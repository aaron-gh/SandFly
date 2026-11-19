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

package com.google.android.accessibility.talkback.braillecommands

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.ValueSteps
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Braille > Braille commands: the braille cells that run commands while braille commands are
 * shown. Each cell is named by its dot numbers in order, such as "4" or "1245", and the command by
 * its value. A cell runs what the profile in use gives it, else what the app in front gives it,
 * else what Default gives it.
 */
object BrailleCommands {

  const val KEY = "pref_braille_commands"

  /** The ID of the profile in use, or "" for Default. */
  const val ACTIVE_KEY = "pref_braille_command_profile"

  /** Starts the value of the command that changes to a profile. It must never change once released. */
  private const val PREFIX = "BRAILLE_COMMAND_PROFILE:"

  /** Names the set of an app, by package name. */
  private const val APP_SET = "app:"

  /** Names the set of a profile, by ID. */
  private const val PROFILE_SET = "profile:"

  /** The set named "" is Default's. */
  const val DEFAULT_SET = ""

  /** A profile: its name and the cells it gives commands. */
  data class Profile(val id: String, val name: String, val cells: Map<String, String>)

  /** The cells an app gives commands, and the app's name for the settings. */
  data class App(val name: String, val cells: Map<String, String>)

  /**
   * Everything saved. [defaults] is null until Default's cells are changed, while the cells of
   * [builtIn] apply.
   */
  data class Config(
    val defaults: Map<String, String>?,
    val apps: Map<String, App>,
    val profiles: List<Profile>,
  )

  /** Default's cells before they are changed, by dot numbers. */
  @JvmStatic
  fun builtIn(context: Context): Map<String, String> {
    fun value(id: Int) = context.getString(id)
    return linkedMapOf(
      "4" to value(R.string.shortcut_value_next),
      "1" to value(R.string.shortcut_value_previous),
      "56" to value(R.string.shortcut_value_select_next_setting),
      "23" to value(R.string.shortcut_value_select_previous_setting),
      "5" to value(R.string.shortcut_value_selected_setting_next_action),
      "2" to value(R.string.shortcut_value_selected_setting_previous_action),
      "36" to value(R.string.shortcut_value_perform_click_action),
      "12" to value(R.string.shortcut_value_back),
      "125" to value(R.string.shortcut_value_home),
      "1345" to value(R.string.shortcut_value_notifications),
      "1235" to value(R.string.shortcut_value_overview),
      "12345" to value(R.string.shortcut_value_quick_settings),
      "2345" to value(R.string.shortcut_value_first_in_screen),
      "15" to value(R.string.shortcut_value_last_in_screen),
      "134" to value(R.string.shortcut_value_talkback_breakout),
    )
  }

  /** Returns the name of the set of the app [packageName]. */
  @JvmStatic fun appSet(packageName: String): String = APP_SET + packageName

  /** Returns the name of the set of the profile [id]. */
  @JvmStatic fun profileSet(id: String): String = PROFILE_SET + id

  /** Returns the package name of an app's [set], or null when it isn't an app's. */
  @JvmStatic
  fun packageOf(set: String): String? = set.takeIf { it.startsWith(APP_SET) }?.removePrefix(APP_SET)

  /** Returns the profile ID of a profile's [set], or null when it isn't a profile's. */
  @JvmStatic
  fun profileOf(set: String): String? =
    set.takeIf { it.startsWith(PROFILE_SET) }?.removePrefix(PROFILE_SET)

  /** Returns the cells of [set] in [config], by dot numbers. */
  fun cells(context: Context, config: Config, set: String): Map<String, String> =
    when {
      set == DEFAULT_SET -> config.defaults ?: builtIn(context)
      packageOf(set) != null -> config.apps[packageOf(set)]?.cells.orEmpty()
      else -> config.profiles.firstOrNull { it.id == profileOf(set) }?.cells.orEmpty()
    }

  /** Returns [config] with the cells of [set] replaced by [cells]. */
  fun withCells(config: Config, set: String, cells: Map<String, String>): Config {
    packageOf(set)?.let { packageName ->
      val app = config.apps[packageName] ?: App(packageName, emptyMap())
      return config.copy(apps = config.apps + (packageName to app.copy(cells = cells)))
    }
    profileOf(set)?.let { id ->
      return config.copy(profiles = config.profiles.map { if (it.id == id) it.copy(cells = cells) else it })
    }
    return config.copy(defaults = cells)
  }

  /**
   * Returns the command the cell of dot numbers [cell] runs: the profile in use's, else [app]'s,
   * else Default's, or null when none gives it one.
   */
  @JvmStatic
  fun action(context: Context, prefs: SharedPreferences, cell: String, app: String?): String? {
    val config = read(prefs)
    val activeId = prefs.getString(ACTIVE_KEY, "").orEmpty()
    config.profiles.firstOrNull { it.id == activeId }?.cells?.get(cell)?.let { return it }
    if (app != null) config.apps[app]?.cells?.get(cell)?.let { return it }
    return cells(context, config, DEFAULT_SET)[cell]
  }

  /**
   * Returns the command that the cell [cell] already runs in [set], or null when it runs none
   * there, for telling before the cell is given another command.
   */
  @JvmStatic
  fun collision(context: Context, prefs: SharedPreferences, set: String, cell: String): String? =
    cells(context, read(prefs), set)[cell]

  /** Makes [cell] run [value] in [set], replacing what it ran there. */
  @JvmStatic
  fun assign(context: Context, prefs: SharedPreferences, set: String, cell: String, value: String) {
    val config = read(prefs)
    write(prefs, withCells(config, set, cells(context, config, set) + (cell to value)))
  }

  /** Takes [cell] out of [set]. */
  @JvmStatic
  fun remove(context: Context, prefs: SharedPreferences, set: String, cell: String) {
    val config = read(prefs)
    write(prefs, withCells(config, set, cells(context, config, set) - cell))
  }

  /** Takes the command [value] off every cell that runs it, in Default, apps and profiles. */
  @JvmStatic
  fun removeCommand(context: Context, prefs: SharedPreferences, value: String) {
    val config = read(prefs)
    val defaults = cells(context, config, DEFAULT_SET)
    if (
      value !in defaults.values &&
        config.apps.values.none { value in it.cells.values } &&
        config.profiles.none { value in it.cells.values }
    ) {
      return
    }
    write(
      prefs,
      Config(
        if (value in defaults.values) defaults.filterValues { it != value } else config.defaults,
        config.apps.mapValues { (_, app) -> app.copy(cells = app.cells.filterValues { it != value }) },
        config.profiles.map { it.copy(cells = it.cells.filterValues { cell -> cell != value }) },
      ),
    )
  }

  /** Puts Default's cells back as they were before they were changed. */
  @JvmStatic
  fun resetDefaults(prefs: SharedPreferences) {
    write(prefs, read(prefs).copy(defaults = null))
  }

  /** Returns the cells that run [value] in Default, in order of their dot numbers. */
  @JvmStatic
  fun cellsOf(context: Context, prefs: SharedPreferences, value: String): List<String> =
    cells(context, read(prefs), DEFAULT_SET).filterValues { it == value }.keys.sorted()

  /** Returns [cell] with its dots in order, or null when it has none or a dot that isn't 1 to 8. */
  @JvmStatic
  fun normalize(cell: String): String? {
    if (cell.isEmpty() || cell.any { it !in '1'..'8' }) return null
    return cell.toSet().sorted().joinToString("")
  }

  // Profiles.

  /** Whether there is a profile to change to besides Default. */
  @JvmStatic fun hasProfiles(prefs: SharedPreferences): Boolean = read(prefs).profiles.isNotEmpty()

  /** The ID of the profile in use, or "" for Default, also when the profile was deleted. */
  @JvmStatic
  fun activeId(prefs: SharedPreferences): String {
    val id = prefs.getString(ACTIVE_KEY, "").orEmpty()
    return if (read(prefs).profiles.any { it.id == id }) id else ""
  }

  /** The name of the profile in use, or null for Default. */
  @JvmStatic
  fun activeName(prefs: SharedPreferences): String? {
    val id = activeId(prefs)
    return read(prefs).profiles.firstOrNull { it.id == id }?.name
  }

  /**
   * Starts using the profile [id], or Default for "", and returns its name to speak, or null when
   * there is no such profile.
   */
  @JvmStatic
  fun select(context: Context, prefs: SharedPreferences, id: String): String? {
    val profile = read(prefs).profiles.firstOrNull { it.id == id }
    if (id.isNotEmpty() && profile == null) return null
    prefs.edit().putString(ACTIVE_KEY, id).apply()
    return profile?.name ?: context.getString(R.string.gesture_profile_default)
  }

  /** The ID of the profile after or before the one in use, with Default first. */
  @JvmStatic
  fun adjacentId(prefs: SharedPreferences, next: Boolean): String {
    val ids = listOf("") + read(prefs).profiles.map { it.id }
    val index = ids.indexOf(activeId(prefs))
    return ids[ValueSteps.step(index, ids.size, next)]
  }

  /** The IDs and names of Default and each profile, in order. */
  @JvmStatic
  fun choices(context: Context, prefs: SharedPreferences): List<Pair<String, String>> =
    listOf("" to context.getString(R.string.gesture_profile_default)) +
      read(prefs).profiles.map { it.id to it.name }

  /** Returns the command that changes to the profile [id], or to Default for "". */
  @JvmStatic fun valueOf(id: String): String = PREFIX + id

  /** Returns whether [value] is a command that changes profile. */
  @JvmStatic fun isAction(value: String?): Boolean = value != null && value.startsWith(PREFIX)

  /** Returns the ID of the profile that the command [value] changes to. */
  @JvmStatic fun idOf(value: String): String = value.removePrefix(PREFIX)

  /**
   * Returns the name of the command [value], or null when it isn't one of these commands or its
   * profile was deleted.
   */
  @JvmStatic
  fun label(context: Context, prefs: SharedPreferences, value: String?): String? {
    if (!isAction(value)) return null
    val id = idOf(value!!)
    val name =
      if (id.isEmpty()) context.getString(R.string.gesture_profile_default)
      else read(prefs).profiles.firstOrNull { it.id == id }?.name ?: return null
    return context.getString(R.string.shortcut_use_braille_command_profile, name)
  }

  /** Returns a new profile's ID. */
  fun newId(): String = UUID.randomUUID().toString()

  // Storage.

  @JvmStatic fun read(prefs: SharedPreferences): Config = fromJson(prefs.getString(KEY, null))

  @JvmStatic
  fun write(prefs: SharedPreferences, config: Config) {
    prefs.edit().putString(KEY, toJson(config)).apply()
  }

  private fun cellsFrom(json: JSONObject?): Map<String, String> {
    if (json == null) return emptyMap()
    val cells = LinkedHashMap<String, String>()
    for (key in json.keys()) {
      val cell = normalize(key) ?: continue
      val value = json.optString(key)
      if (value.isNotEmpty()) cells[cell] = value
    }
    return cells
  }

  private fun cellsTo(cells: Map<String, String>): JSONObject {
    val json = JSONObject()
    cells.forEach { (cell, value) -> json.put(cell, value) }
    return json
  }

  fun fromJson(json: String?): Config {
    val empty = Config(null, emptyMap(), emptyList())
    if (json.isNullOrEmpty()) return empty
    return try {
      val root = JSONObject(json)
      val defaults = root.optJSONObject("default")?.let { cellsFrom(it) }
      val appsJson = root.optJSONObject("apps") ?: JSONObject()
      val apps =
        appsJson.keys().asSequence().associateWith { packageName ->
          val app = appsJson.optJSONObject(packageName) ?: JSONObject()
          App(app.optString("name", packageName), cellsFrom(app.optJSONObject("cells")))
        }
      val profilesJson = root.optJSONArray("profiles") ?: JSONArray()
      val profiles =
        (0 until profilesJson.length()).mapNotNull { index ->
          val profile = profilesJson.optJSONObject(index) ?: return@mapNotNull null
          val id = profile.optString("id")
          if (id.isEmpty()) return@mapNotNull null
          Profile(id, profile.optString("name", id), cellsFrom(profile.optJSONObject("cells")))
        }
      Config(defaults, apps, profiles)
    } catch (e: JSONException) {
      empty
    }
  }

  fun toJson(config: Config): String {
    val root = JSONObject()
    config.defaults?.let { root.put("default", cellsTo(it)) }
    val apps = JSONObject()
    for ((packageName, app) in config.apps) {
      apps.put(packageName, JSONObject().put("name", app.name).put("cells", cellsTo(app.cells)))
    }
    root.put("apps", apps)
    val profiles = JSONArray()
    for (profile in config.profiles) {
      profiles.put(
        JSONObject()
          .put("id", profile.id)
          .put("name", profile.name)
          .put("cells", cellsTo(profile.cells))
      )
    }
    root.put("profiles", profiles)
    return root.toString()
  }
}
