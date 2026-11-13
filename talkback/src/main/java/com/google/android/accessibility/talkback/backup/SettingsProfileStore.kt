/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.backup

import java.io.File
import java.io.IOException
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Settings profiles in [directory]: an index file with each profile's ID, name and triggers, in
 * order, and the profile chosen to be used everywhere; a file for each profile with the settings it
 * sets; and the state of the profiles applied. Settings are kept by [SettingsBackup.profileKey],
 * with null for a setting that a profile leaves unset. The store only keeps the data;
 * [SettingsProfiles] applies it.
 *
 * An index of version 1 lists profiles that each hold every setting, as a settings file; see
 * [isLegacy].
 */
class SettingsProfileStore(
  private val directory: File,
  /** The name of the [number]th profile recovered from its file when the index can't be read. */
  private val recoveredName: (number: Int) -> String = { "Profile $it" },
) {

  /** When a profile is used without being chosen: in [apps], and in the contexts switched on. */
  data class Triggers(
    val apps: Set<String> = emptySet(),
    val web: Boolean = false,
    val reading: Boolean = false,
    val editing: Boolean = false,
  ) {
    fun isEmpty(): Boolean = apps.isEmpty() && !web && !reading && !editing
  }

  /** A settings profile. */
  data class Profile(val id: String, val name: String, val triggers: Triggers = Triggers())

  /**
   * The profiles' settings in the preferences: for each setting a profile sets, its value without
   * profiles ([base]), the value written ([written]) and the ID of the profile it came from
   * ([from]).
   */
  data class Applied(
    val base: Map<String, Any?> = emptyMap(),
    val written: Map<String, Any?> = emptyMap(),
    val from: Map<String, String> = emptyMap(),
  ) {
    fun isEmpty(): Boolean = written.isEmpty()
  }

  private var profiles = mutableListOf<Profile>()
  private var manualId: String? = null
  private var legacy = false
  private var legacyActiveId: String? = null

  init {
    load()
  }

  @Synchronized fun profiles(): List<Profile> = profiles.toList()

  @Synchronized fun isEmpty(): Boolean = profiles.isEmpty()

  @Synchronized fun profile(id: String): Profile? = profiles.firstOrNull { it.id == id }

  /** Whether any profile is used in an app or context. */
  @Synchronized fun hasTriggers(): Boolean = profiles.any { !it.triggers.isEmpty() }

  /** The profile chosen to be used everywhere, or null for none. */
  @Synchronized fun manual(): Profile? = profiles.firstOrNull { it.id == manualId }

  /**
   * The profile after the chosen one, or null for none, which comes after the last and before the
   * first.
   */
  fun nextManual(): Profile? = offset(1)

  /** The profile before the chosen one, or null for none, as [nextManual]. */
  fun previousManual(): Profile? = offset(-1)

  @Synchronized
  private fun offset(by: Int): Profile? {
    // Position 0 is none; the profiles follow.
    val count = profiles.size + 1
    val index = profiles.indexOfFirst { it.id == manualId } + 1
    return profiles.getOrNull((index + by).mod(count) - 1)
  }

  /** Whether a profile other than [exceptId] is called [name], ignoring case and outer spaces. */
  @Synchronized
  fun isNameTaken(name: String, exceptId: String? = null): Boolean =
    profiles.any { it.id != exceptId && it.name.equals(name.trim(), ignoreCase = true) }

  /**
   * Adds a profile called [name] that sets [settings], last. Returns the profile, or null when the
   * name is empty or taken, or the files can't be written.
   */
  @Synchronized
  fun add(name: String, settings: Map<String, Any?> = emptyMap()): Profile? {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH || isNameTaken(trimmed)) return null
    val profile = Profile(UUID.randomUUID().toString(), trimmed)
    if (!write(settingsFile(profile.id), profileText(settings))) return null
    profiles += profile
    if (!saveIndex()) {
      profiles.remove(profile)
      settingsFile(profile.id).delete()
      return null
    }
    return profile
  }

  /** Renames the profile [id]. Returns false when there is none, or the name is empty or taken. */
  @Synchronized
  fun rename(id: String, name: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) return false
    if (isNameTaken(trimmed, exceptId = id)) return false
    return change(id) { it.copy(name = trimmed) }
  }

  /** Sets when the profile [id] is used. Returns false when there is none or on failure. */
  @Synchronized
  fun setTriggers(id: String, triggers: Triggers): Boolean =
    change(id) { it.copy(triggers = triggers) }

  private fun change(id: String, change: (Profile) -> Profile): Boolean {
    val index = profiles.indexOfFirst { it.id == id }
    if (index < 0) return false
    val old = profiles[index]
    profiles[index] = change(old)
    if (!saveIndex()) {
      profiles[index] = old
      return false
    }
    return true
  }

  /** Moves the profile [id] [by] places, up for negative. Returns false when it can't move. */
  @Synchronized
  fun move(id: String, by: Int): Boolean {
    val index = profiles.indexOfFirst { it.id == id }
    val target = index + by
    if (index < 0 || target !in profiles.indices) return false
    val old = profiles.toMutableList()
    profiles.add(target, profiles.removeAt(index))
    if (!saveIndex()) {
      profiles = old
      return false
    }
    return true
  }

  /** Deletes the profile [id]. */
  @Synchronized
  fun delete(id: String): Boolean {
    val index = profiles.indexOfFirst { it.id == id }
    if (index < 0) return false
    val old = profiles.removeAt(index)
    val oldManual = manualId
    if (manualId == id) manualId = null
    if (!saveIndex()) {
      profiles.add(index, old)
      manualId = oldManual
      return false
    }
    settingsFile(id).delete()
    return true
  }

  /** Makes the profile [id], or none for null, the one used everywhere. */
  @Synchronized
  fun setManual(id: String?): Boolean {
    if (id != null && profiles.none { it.id == id }) return false
    if (id == manualId) return true
    val old = manualId
    manualId = id
    if (!saveIndex()) {
      manualId = old
      return false
    }
    return true
  }

  /**
   * The settings that the profile [id] sets, or null when there is none or its file can't be read.
   */
  @Synchronized
  fun settings(id: String): Map<String, Any?>? {
    if (legacy || profiles.none { it.id == id }) return null
    val json = read(settingsFile(id)) ?: return null
    return try {
      decodeSettings(JSONObject(json).getJSONObject("settings"))
    } catch (e: JSONException) {
      null
    }
  }

  /** Replaces the settings of the profile [id]. Returns false when there is none or on failure. */
  @Synchronized
  fun saveSettings(id: String, settings: Map<String, Any?>): Boolean =
    !legacy && profiles.any { it.id == id } && write(settingsFile(id), profileText(settings))

  /** The state of the profiles applied, empty when none is. */
  @Synchronized
  fun applied(): Applied {
    val json = read(File(directory, APPLIED)) ?: return Applied()
    return try {
      val root = JSONObject(json)
      val from = mutableMapOf<String, String>()
      val fromJson = root.optJSONObject("from") ?: JSONObject()
      for (key in fromJson.keys()) from[key] = fromJson.optString(key)
      Applied(
        decodeSettings(root.optJSONObject("base") ?: JSONObject()),
        decodeSettings(root.optJSONObject("written") ?: JSONObject()),
        from,
      )
    } catch (e: JSONException) {
      Applied()
    }
  }

  /** Saves the state of the profiles applied. */
  @Synchronized
  fun saveApplied(applied: Applied): Boolean {
    val file = File(directory, APPLIED)
    if (applied.isEmpty()) return !file.exists() || file.delete()
    val from = JSONObject()
    for ((key, id) in applied.from) from.put(key, id)
    val root =
      JSONObject()
        .put("base", encodeSettings(applied.base))
        .put("written", encodeSettings(applied.written))
        .put("from", from)
    return write(file, root.toString())
  }

  /**
   * The profile that gets the settings changed while its settings are being changed, and the
   * settings as they were when that started, or null.
   */
  @Synchronized
  fun editing(): Pair<String, Map<String, Any?>>? {
    val json = read(File(directory, EDITING)) ?: return null
    return try {
      val root = JSONObject(json)
      val id = root.getString("id")
      if (profiles.none { it.id == id }) null
      else id to decodeSettings(root.optJSONObject("start") ?: JSONObject())
    } catch (e: JSONException) {
      null
    }
  }

  /** Saves that the profile [id], or none for null, gets changed settings, and the [start]. */
  @Synchronized
  fun saveEditing(id: String?, start: Map<String, Any?> = emptyMap()): Boolean {
    val file = File(directory, EDITING)
    if (id == null) return !file.exists() || file.delete()
    return write(file, JSONObject().put("id", id).put("start", encodeSettings(start)).toString())
  }

  /**
   * Whether the profiles are of version 1, each with every setting. Read them with
   * [legacySettings] and make them of this version with [finishLegacy].
   */
  @Synchronized fun isLegacy(): Boolean = legacy

  /** The profile in use in version 1. */
  @Synchronized fun legacyActive(): String? = legacyActiveId

  /** The settings file of the version 1 profile [id], or null. */
  @Synchronized
  fun legacySettings(id: String): String? =
    if (profiles.none { it.id == id }) null else read(settingsFile(id))

  /**
   * Makes the version 1 profiles profiles that set [settings], by ID; profiles without settings
   * there are deleted. Returns false on failure, and then keeps them.
   */
  @Synchronized
  fun finishLegacy(settings: Map<String, Map<String, Any?>>): Boolean {
    if (!legacy) return true
    // The version 1 files stay until the new index is written, so a failure keeps them.
    val kept = profiles.filter { it.id in settings }
    val written = mutableListOf<File>()
    for (profile in kept) {
      val file = File(directory, "${profile.id}$NEW_SUFFIX")
      written += file
      if (!write(file, profileText(settings.getValue(profile.id)))) {
        written.forEach { it.delete() }
        return false
      }
    }
    val old = profiles
    profiles = kept.toMutableList()
    manualId = null
    if (!saveIndex()) {
      profiles = old
      written.forEach { it.delete() }
      return false
    }
    for (profile in old) {
      if (profile.id !in settings) settingsFile(profile.id).delete()
    }
    for (profile in kept) {
      File(directory, "${profile.id}$NEW_SUFFIX").renameTo(settingsFile(profile.id))
    }
    legacy = false
    legacyActiveId = null
    return true
  }

  private fun settingsFile(id: String) = File(directory, "$id$SUFFIX")

  private fun profileText(settings: Map<String, Any?>): String =
    JSONObject().put("version", VERSION).put("settings", encodeSettings(settings)).toString()

  private fun read(file: File): String? =
    try {
      if (!file.isFile || file.length() > MAX_BYTES) null else file.readText()
    } catch (e: IOException) {
      null
    }

  private fun load() {
    val file = File(directory, INDEX)
    if (!file.isFile) return
    if (file.length() > MAX_BYTES) {
      recover(file)
      return
    }
    try {
      val root = JSONObject(file.readText())
      val array = root.optJSONArray("profiles") ?: JSONArray()
      val read = mutableListOf<Profile>()
      for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val id = item.optString("id")
        val name = item.optString("name").trim()
        if (!ID.matches(id) || name.isEmpty() || read.any { it.id == id }) continue
        read += Profile(id, name, decodeTriggers(item))
      }
      profiles = read
      if (root.optInt("version", 1) < VERSION) {
        legacy = read.isNotEmpty()
        val active = root.optString("active")
        legacyActiveId = if (read.any { it.id == active }) active else read.firstOrNull()?.id
      } else {
        val manual = root.optString("manual")
        manualId = if (read.any { it.id == manual }) manual else null
      }
    } catch (e: IOException) {
      recover(file)
    } catch (e: JSONException) {
      recover(file)
    }
  }

  /**
   * Moves the unreadable [index] aside, so that writing a new one doesn't lose it, and makes a
   * profile of each profile file in [directory], oldest first. The names are not known and are
   * numbered, and none is used everywhere.
   */
  private fun recover(index: File) {
    index.renameTo(File(directory, "$INDEX.corrupt-${System.currentTimeMillis()}"))
    val files =
      directory
        .listFiles { file ->
          file.isFile && file.name.endsWith(SUFFIX) && ID.matches(file.name.removeSuffix(SUFFIX))
        }
        ?.sortedBy { it.lastModified() }
        .orEmpty()
    if (files.isEmpty()) return
    profiles =
      files.mapIndexedTo(mutableListOf()) { i, file ->
        Profile(file.name.removeSuffix(SUFFIX), recoveredName(i + 1))
      }
    // A file of version 1 holds a whole settings file.
    legacy = files.any { read(it)?.let(::isLegacyText) == true }
    if (legacy) {
      // The file saved last is most likely that of the profile in use.
      legacyActiveId = profiles.last().id
    } else {
      // Without a new index, the next change writes one.
      saveIndex()
    }
  }

  private fun saveIndex(): Boolean {
    val array = JSONArray()
    for (profile in profiles) {
      val item = JSONObject().put("id", profile.id).put("name", profile.name)
      encodeTriggers(profile.triggers, item)
      array.put(item)
    }
    val root = JSONObject().put("version", VERSION).put("profiles", array)
    manualId?.let { root.put("manual", it) }
    return write(File(directory, INDEX), root.toString())
  }

  /** Writes [text] to a file beside [file] and then moves it there, so a failure keeps the old. */
  private fun write(file: File, text: String): Boolean {
    if (!directory.isDirectory && !directory.mkdirs()) return false
    val temporary = File(directory, "${file.name}.tmp")
    return try {
      temporary.writeText(text)
      temporary.renameTo(file) || run {
        temporary.delete()
        false
      }
    } catch (e: IOException) {
      temporary.delete()
      false
    }
  }

  companion object {
    private const val INDEX = "profiles.json"
    private const val APPLIED = "applied.state"
    private const val EDITING = "editing.state"
    private const val SUFFIX = ".json"
    private const val NEW_SUFFIX = ".new"
    private const val VERSION = 2
    private const val MAX_BYTES = 5L * 1024 * 1024
    const val MAX_NAME_LENGTH = 100

    /** IDs are UUIDs, which also keeps each profile's file in [directory]. */
    private val ID = Regex("[0-9a-fA-F-]{1,64}")

    private fun isLegacyText(text: String): Boolean =
      try {
        JSONObject(text).has("files")
      } catch (e: JSONException) {
        false
      }

    private fun encodeTriggers(triggers: Triggers, item: JSONObject) {
      if (triggers.apps.isNotEmpty()) item.put("apps", JSONArray(triggers.apps.sorted()))
      if (triggers.web) item.put("web", true)
      if (triggers.reading) item.put("reading", true)
      if (triggers.editing) item.put("editing", true)
    }

    private fun decodeTriggers(item: JSONObject): Triggers {
      val array = item.optJSONArray("apps") ?: JSONArray()
      val apps = (0 until array.length()).mapNotNull { array.optString(it).ifEmpty { null } }
      return Triggers(
        apps.toSet(),
        item.optBoolean("web"),
        item.optBoolean("reading"),
        item.optBoolean("editing"),
      )
    }

    /** [settings] as JSON, with null as a value of type "unset". */
    private fun encodeSettings(settings: Map<String, Any?>): JSONObject {
      val json = JSONObject()
      for ((key, value) in settings) {
        val encoded =
          if (value == null) JSONObject().put("type", UNSET)
          else SettingsBackup.encode(value) ?: continue
        json.put(key, encoded)
      }
      return json
    }

    private fun decodeSettings(json: JSONObject): Map<String, Any?> {
      val settings = mutableMapOf<String, Any?>()
      for (key in json.keys()) {
        val value = json.optJSONObject(key) ?: continue
        if (value.optString("type") == UNSET) {
          settings[key] = null
        } else {
          settings[key] = SettingsBackup.decode(value) ?: continue
        }
      }
      return settings
    }

    private const val UNSET = "unset"
  }
}
