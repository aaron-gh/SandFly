/*
 * Copyright 2026 Backtalk contributors
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

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.keyboard.KeyCombo
import org.json.JSONException
import org.json.JSONObject

/**
 * Settings > Gestures > Gestures in apps, and Keyboard shortcuts in apps: gestures and keyboard
 * shortcuts that run another command in one app, while that app's window is the last one shown.
 * Each gesture is named by its preference key on the Gestures screen, without a gesture set, each
 * keyboard shortcut by [keyName], and the command by its value.
 */
object AppGestures {

  const val KEY = "pref_app_gestures"

  /**
   * The gestures and keyboard shortcuts of an app: what each runs there, and the app's name for
   * the settings.
   */
  data class App(
    val name: String,
    val gestures: Map<String, String>,
    val keys: Map<String, String> = emptyMap(),
  )

  @Volatile private var apps: Map<String, App> = emptyMap()

  // The keyboard shortcuts of each app, as key combinations.
  @Volatile private var appKeys: Map<String, List<Pair<KeyCombo, String>>> = emptyMap()

  // The package of the window shown last.
  @Volatile private var app: String = ""

  @JvmStatic
  fun load(prefs: SharedPreferences) {
    val read = read(prefs)
    apps = read
    appKeys =
      read
        .mapValues { (_, app) -> app.keys.mapNotNull { (name, value) -> keyCombo(name)?.let { it to value } } }
        .filterValues { it.isNotEmpty() }
  }

  /** The keyboard shortcuts of the app shown, with the command each runs there. */
  @JvmStatic fun keys(): List<Pair<KeyCombo, String>> = appKeys[app].orEmpty()

  /** The name a keyboard shortcut is kept under. */
  @JvmStatic
  fun keyName(keyCombo: KeyCombo): String =
    "${keyCombo.keyComboCode}:${keyCombo.isTriggerModifierUsed}"

  /** The key combination kept under [name], or null if it isn't one. */
  @JvmStatic
  fun keyCombo(name: String): KeyCombo? {
    val parts = name.split(':')
    val code = parts.getOrNull(0)?.toLongOrNull() ?: return null
    val trigger = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: return null
    return KeyCombo(code, trigger)
  }

  /** Notes the app whose window is shown. */
  @JvmStatic
  fun onWindowShown(packageName: CharSequence?) {
    app = packageName?.toString().orEmpty()
  }

  /**
   * Returns the command that the gesture of preference key [gestureKey] runs in the app shown, or
   * null when it runs what it runs everywhere.
   */
  @JvmStatic
  fun action(gestureKey: String?): String? {
    if (gestureKey == null) return null
    val apps = apps
    if (apps.isEmpty()) return null
    return apps[app]?.gestures?.get(gestureKey)
  }

  fun read(prefs: SharedPreferences): Map<String, App> = fromJson(prefs.getString(KEY, null))

  fun write(prefs: SharedPreferences, apps: Map<String, App>) {
    prefs.edit().putString(KEY, toJson(apps)).apply()
  }

  fun fromJson(json: String?): Map<String, App> {
    if (json.isNullOrEmpty()) return emptyMap()
    return try {
      val root = JSONObject(json)
      root.keys().asSequence().associateWith { packageName ->
        val app = root.getJSONObject(packageName)
        val gestures = app.optJSONObject("gestures") ?: JSONObject()
        val keys = app.optJSONObject("keys") ?: JSONObject()
        App(
          app.optString("name", packageName),
          gestures.keys().asSequence().associateWith { gestures.getString(it) },
          keys.keys().asSequence().associateWith { keys.getString(it) },
        )
      }
    } catch (e: JSONException) {
      emptyMap()
    }
  }

  fun toJson(apps: Map<String, App>): String {
    val root = JSONObject()
    for ((packageName, app) in apps) {
      val gestures = JSONObject()
      app.gestures.forEach { (key, value) -> gestures.put(key, value) }
      val keys = JSONObject()
      app.keys.forEach { (key, value) -> keys.put(key, value) }
      root.put(
        packageName,
        JSONObject().put("name", app.name).put("gestures", gestures).put("keys", keys),
      )
    }
    return root.toString()
  }
}
