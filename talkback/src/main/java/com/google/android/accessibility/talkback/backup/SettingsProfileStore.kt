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
 * Settings profiles in [directory]: an index file with each profile's ID and name, in order, and
 * the profile in use, and a file for each profile with its settings, as [SettingsBackup] writes
 * them. The store only keeps the texts; [SettingsProfiles] makes and applies them.
 */
class SettingsProfileStore(private val directory: File) {

  /** A settings profile. */
  data class Profile(val id: String, val name: String)

  private var profiles = mutableListOf<Profile>()
  private var activeId: String? = null

  init {
    load()
  }

  @Synchronized fun profiles(): List<Profile> = profiles.toList()

  @Synchronized fun isEmpty(): Boolean = profiles.isEmpty()

  @Synchronized fun profile(id: String): Profile? = profiles.firstOrNull { it.id == id }

  /** The profile in use, or null before the first profile is added. */
  @Synchronized fun active(): Profile? = profiles.firstOrNull { it.id == activeId }

  /** The profile after the one in use, wrapping around, or null when there are none. */
  @Synchronized
  fun next(): Profile? {
    if (profiles.isEmpty()) return null
    val index = profiles.indexOfFirst { it.id == activeId }
    return profiles[(index + 1).mod(profiles.size)]
  }

  /** Whether a profile other than [exceptId] is called [name], ignoring case and outer spaces. */
  @Synchronized
  fun isNameTaken(name: String, exceptId: String? = null): Boolean =
    profiles.any { it.id != exceptId && it.name.equals(name.trim(), ignoreCase = true) }

  /**
   * Adds a profile called [name] with [settings], last. The first profile becomes the one in use.
   * Returns the profile, or null when the name is empty or taken, or the files can't be written.
   */
  @Synchronized
  fun add(name: String, settings: String): Profile? {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH || isNameTaken(trimmed)) return null
    val profile = Profile(UUID.randomUUID().toString(), trimmed)
    if (!write(settingsFile(profile.id), settings)) return null
    val oldActive = activeId
    profiles += profile
    if (activeId == null) activeId = profile.id
    if (!saveIndex()) {
      profiles.remove(profile)
      activeId = oldActive
      settingsFile(profile.id).delete()
      return null
    }
    return profile
  }

  /** Renames the profile [id]. Returns false when there is none, or the name is empty or taken. */
  @Synchronized
  fun rename(id: String, name: String): Boolean {
    val trimmed = name.trim()
    val index = profiles.indexOfFirst { it.id == id }
    if (index < 0 || trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) return false
    if (isNameTaken(trimmed, exceptId = id)) return false
    val old = profiles[index]
    profiles[index] = old.copy(name = trimmed)
    if (!saveIndex()) {
      profiles[index] = old
      return false
    }
    return true
  }

  /** Deletes the profile [id]. The profile in use can't be deleted. */
  @Synchronized
  fun delete(id: String): Boolean {
    if (id == activeId) return false
    val index = profiles.indexOfFirst { it.id == id }
    if (index < 0) return false
    val old = profiles.removeAt(index)
    if (!saveIndex()) {
      profiles.add(index, old)
      return false
    }
    settingsFile(id).delete()
    return true
  }

  /** Makes the profile [id] the one in use. Returns false when there is none. */
  @Synchronized
  fun setActive(id: String): Boolean {
    if (profiles.none { it.id == id }) return false
    if (id == activeId) return true
    val old = activeId
    activeId = id
    if (!saveIndex()) {
      activeId = old
      return false
    }
    return true
  }

  /** The settings of the profile [id], or null when there is none or its file can't be read. */
  @Synchronized
  fun settings(id: String): String? {
    if (profiles.none { it.id == id }) return null
    val file = settingsFile(id)
    return try {
      if (!file.isFile || file.length() > MAX_BYTES) null else file.readText()
    } catch (e: IOException) {
      null
    }
  }

  /** Replaces the settings of the profile [id]. Returns false when there is none or on failure. */
  @Synchronized
  fun saveSettings(id: String, settings: String): Boolean =
    profiles.any { it.id == id } && write(settingsFile(id), settings)

  private fun settingsFile(id: String) = File(directory, "$id$SUFFIX")

  private fun load() {
    val file = File(directory, INDEX)
    if (!file.isFile || file.length() > MAX_BYTES) return
    try {
      val root = JSONObject(file.readText())
      val array = root.optJSONArray("profiles") ?: JSONArray()
      val read = mutableListOf<Profile>()
      for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val id = item.optString("id")
        val name = item.optString("name").trim()
        if (!ID.matches(id) || name.isEmpty() || read.any { it.id == id }) continue
        read += Profile(id, name)
      }
      profiles = read
      val active = root.optString("active")
      // An index without a valid profile in use uses the first.
      activeId = if (read.any { it.id == active }) active else read.firstOrNull()?.id
    } catch (e: IOException) {
      // Starts without profiles.
    } catch (e: JSONException) {
      // Starts without profiles.
    }
  }

  private fun saveIndex(): Boolean {
    val array = JSONArray()
    for (profile in profiles) {
      array.put(JSONObject().put("id", profile.id).put("name", profile.name))
    }
    val root = JSONObject().put("version", VERSION).put("profiles", array)
    activeId?.let { root.put("active", it) }
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
    private const val SUFFIX = ".json"
    private const val VERSION = 1
    private const val MAX_BYTES = 5L * 1024 * 1024
    const val MAX_NAME_LENGTH = 100

    /** IDs are UUIDs, which also keeps each profile's file in [directory]. */
    private val ID = Regex("[0-9a-fA-F-]{1,64}")
  }
}
