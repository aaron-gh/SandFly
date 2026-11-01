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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Gestures > Gesture profiles: sets of gestures that run other commands while the profile is in
 * use, before gestures in apps, scripts and the Gestures screen. A gesture a profile leaves out
 * runs what it runs without the profile. Each gesture is named by its preference key on the
 * Gestures screen, without a gesture set, and the command by its value. Default, with ID "", is no
 * profile.
 */
object GestureProfiles {

  const val KEY = "pref_gesture_profiles"

  /** The ID of the profile in use, or "" for Default. */
  const val ACTIVE_KEY = "pref_gesture_profile"

  /** Starts the value of the command that changes to a profile. It must never change once released. */
  private const val PREFIX = "GESTURE_PROFILE:"

  /** A gesture profile: its name and what each of its gestures runs. */
  data class Profile(val id: String, val name: String, val gestures: Map<String, String>)

  @Volatile private var profiles: List<Profile> = emptyList()

  // The gestures of the profile in use, or null for Default.
  @Volatile private var active: Map<String, String>? = null

  @JvmStatic
  fun load(prefs: SharedPreferences) {
    val read = read(prefs)
    profiles = read
    val id = prefs.getString(ACTIVE_KEY, "").orEmpty()
    active = read.firstOrNull { it.id == id }?.gestures
  }

  /**
   * Returns the command that the gesture of preference key [gestureKey] runs in the profile in
   * use, or null when it runs what it runs without the profile.
   */
  @JvmStatic
  fun action(gestureKey: String?): String? {
    if (gestureKey == null) return null
    return active?.get(gestureKey)
  }

  /** Whether there is a profile to change to besides Default. */
  @JvmStatic fun hasProfiles(): Boolean = profiles.isNotEmpty()

  /** The ID of the profile in use, or "" for Default, also when the profile was deleted. */
  @JvmStatic
  fun activeId(prefs: SharedPreferences): String {
    val id = prefs.getString(ACTIVE_KEY, "").orEmpty()
    return if (read(prefs).any { it.id == id }) id else ""
  }

  /**
   * Starts using the profile [id], or Default for "", and returns its name to speak, or null when
   * there is no such profile.
   */
  @JvmStatic
  fun select(context: Context, prefs: SharedPreferences, id: String): String? {
    val all = read(prefs)
    val profile = all.firstOrNull { it.id == id }
    if (id.isNotEmpty() && profile == null) return null
    prefs.edit().putString(ACTIVE_KEY, id).apply()
    // The service reloads it from the preference too, but the next gesture may come first.
    profiles = all
    active = profile?.gestures
    return profile?.name ?: context.getString(R.string.gesture_profile_default)
  }

  /** The ID of the profile after or before the one in use, with Default first, wrapping around. */
  @JvmStatic
  fun adjacentId(prefs: SharedPreferences, next: Boolean): String {
    val ids = listOf("") + read(prefs).map { it.id }
    val index = ids.indexOf(activeId(prefs))
    return ids[Math.floorMod(index + if (next) 1 else -1, ids.size)]
  }

  /** The IDs and names of Default and each profile, in order. */
  @JvmStatic
  fun choices(context: Context, prefs: SharedPreferences): List<Pair<String, String>> =
    listOf("" to context.getString(R.string.gesture_profile_default)) +
      read(prefs).map { it.id to it.name }

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
      else read(prefs).firstOrNull { it.id == id }?.name ?: return null
    return context.getString(R.string.shortcut_use_gesture_profile, name)
  }

  /** Returns a new profile's ID. */
  fun newId(): String = UUID.randomUUID().toString()

  @JvmStatic
  fun read(prefs: SharedPreferences): List<Profile> = fromJson(prefs.getString(KEY, null))

  @JvmStatic
  fun write(prefs: SharedPreferences, profiles: List<Profile>) {
    prefs.edit().putString(KEY, toJson(profiles)).apply()
  }

  fun fromJson(json: String?): List<Profile> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).mapNotNull { index ->
        val profile = array.optJSONObject(index) ?: return@mapNotNull null
        val id = profile.optString("id")
        if (id.isEmpty()) return@mapNotNull null
        val gestures = profile.optJSONObject("gestures") ?: JSONObject()
        Profile(
          id,
          profile.optString("name", id),
          gestures.keys().asSequence().associateWith { gestures.getString(it) },
        )
      }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  fun toJson(profiles: List<Profile>): String {
    val array = JSONArray()
    for (profile in profiles) {
      val gestures = JSONObject()
      profile.gestures.forEach { (key, value) -> gestures.put(key, value) }
      array.put(
        JSONObject().put("id", profile.id).put("name", profile.name).put("gestures", gestures)
      )
    }
    return array.toString()
  }
}
