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
import org.json.JSONException
import org.json.JSONObject

/**
 * Settings > Gestures > Gestures in apps: gestures that run another command in one app, while
 * that app's window is the last one shown. Each gesture is named by its preference key on the
 * Gestures screen, without a gesture set, and the command by its value.
 */
object AppGestures {

  const val KEY = "pref_app_gestures"

  /** The gestures of an app: what each runs there, and the app's name for the settings. */
  data class App(val name: String, val gestures: Map<String, String>)

  @Volatile private var apps: Map<String, App> = emptyMap()

  // The package of the window shown last.
  @Volatile private var app: String = ""

  @JvmStatic
  fun load(prefs: SharedPreferences) {
    apps = read(prefs)
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
        App(
          app.optString("name", packageName),
          gestures.keys().asSequence().associateWith { gestures.getString(it) },
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
      root.put(packageName, JSONObject().put("name", app.name).put("gestures", gestures))
    }
    return root.toString()
  }
}
