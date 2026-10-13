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

package com.google.android.accessibility.talkback.directtouch

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.focusmanagement.LiftToActivateMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Settings for direct touch: which apps get raw touch, and how Backtalk reports changes. Apps that
 * declare [CAPABILITY_KEY] are turned on the first time they are seen, and after that the user's
 * choice is kept.
 */
object DirectTouchSettings {
  /** Manifest meta-data key a game sets to ask for direct touch. */
  const val CAPABILITY_KEY = "dev.nvgt.capability.DIRECT_TOUCH"

  const val PREF_MASTER = "pref_direct_touch_master"
  const val PREF_SPEECH = "pref_direct_touch_speech"
  const val PREF_HAPTICS = "pref_direct_touch_haptics"
  /** Whether a tap on a navigation bar button that takes touches directly says the button. */
  const val PREF_NAV_BAR_SPEECH = "pref_speak_nav_bar_buttons"
  private const val PREF_APPS = "pref_direct_touch_apps"
  private const val PREF_SEEN = "pref_direct_touch_seen"
  private const val PREF_TYPING_PREFIX = "pref_direct_touch_typing_"
  private const val BACKUP_VERSION = 1

  /**
   * The old "Navigation bar always direct" setting. Lift to activate on the navigation bar
   * replaced it, since that also lets a single tap press the buttons, and still says them.
   */
  private const val OLD_PREF_NAV_BAR = "pref_direct_touch_nav_bar"

  /**
   * Turns lift to activate on for the navigation bar, stored under [liftToActivateKey], for users
   * who had the old navigation bar setting on and lift to activate off. Runs once.
   */
  fun migrateNavBarSetting(prefs: SharedPreferences, liftToActivateKey: String) {
    if (!prefs.contains(OLD_PREF_NAV_BAR)) {
      return
    }
    val editor = prefs.edit().remove(OLD_PREF_NAV_BAR)
    val liftToActivate = LiftToActivateMode.fromPrefValue(prefs.getString(liftToActivateKey, null))
    if (prefs.getBoolean(OLD_PREF_NAV_BAR, false) && liftToActivate == LiftToActivateMode.DISABLED) {
      editor.putString(liftToActivateKey, LiftToActivateMode.NAVIGATION_BAR.prefValue)
    }
    editor.apply()
  }

  fun isMasterEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_MASTER, true)

  fun setMasterEnabled(prefs: SharedPreferences, enabled: Boolean) {
    prefs.edit().putBoolean(PREF_MASTER, enabled).apply()
  }

  fun isSpeechEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_SPEECH, true)

  fun isHapticsEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_HAPTICS, false)

  fun isNavBarSpeechEnabled(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREF_NAV_BAR_SPEECH, true)

  fun isAppEnabled(prefs: SharedPreferences, pkg: String): Boolean = pkg in stringSet(prefs, PREF_APPS)

  /** Whether direct touch is on and turned on for any app, so that it can turn on at all. */
  fun hasEnabledApps(prefs: SharedPreferences): Boolean =
    isMasterEnabled(prefs) && stringSet(prefs, PREF_APPS).isNotEmpty()

  fun setAppEnabled(prefs: SharedPreferences, pkg: String, enabled: Boolean) {
    val apps = stringSet(prefs, PREF_APPS)
    prefs.edit().putStringSet(PREF_APPS, if (enabled) apps + pkg else apps - pkg).apply()
  }

  fun isDirectTyping(prefs: SharedPreferences, pkg: String): Boolean =
    prefs.getBoolean(PREF_TYPING_PREFIX + pkg, false)

  fun setDirectTyping(prefs: SharedPreferences, pkg: String, enabled: Boolean) {
    prefs.edit().putBoolean(PREF_TYPING_PREFIX + pkg, enabled).apply()
  }

  /**
   * Records that [pkg] has been seen. The first time, an app that [declaresCapability] is turned
   * on. Later calls change nothing, so an app the user turned off stays off.
   */
  fun onFirstSight(prefs: SharedPreferences, pkg: String, declaresCapability: Boolean) {
    val seen = stringSet(prefs, PREF_SEEN)
    if (pkg in seen) {
      return
    }
    val editor = prefs.edit().putStringSet(PREF_SEEN, seen + pkg)
    if (declaresCapability) {
      editor.putStringSet(PREF_APPS, stringSet(prefs, PREF_APPS) + pkg)
    }
    editor.apply()
  }

  fun exportJson(prefs: SharedPreferences): String =
    JSONObject()
      .put("version", BACKUP_VERSION)
      .put("master", isMasterEnabled(prefs))
      .put("speech", isSpeechEnabled(prefs))
      .put("haptics", isHapticsEnabled(prefs))
      .put("apps", JSONArray(stringSet(prefs, PREF_APPS).sorted()))
      .put("seen", JSONArray(stringSet(prefs, PREF_SEEN).sorted()))
      .put("typing", JSONArray(typingPackages(prefs).sorted()))
      .toString()

  /** Replaces the settings with a backup from [exportJson]. Returns false and changes nothing if [json] is not one. */
  fun importJson(prefs: SharedPreferences, json: String): Boolean {
    val backup: JSONObject
    val apps: Set<String>
    val seen: Set<String>
    val typing: Set<String>
    try {
      backup = JSONObject(json)
      if (backup.getInt("version") != BACKUP_VERSION) {
        return false
      }
      apps = strings(backup.getJSONArray("apps"))
      seen = strings(backup.getJSONArray("seen"))
      typing = strings(backup.getJSONArray("typing"))
      backup.getBoolean("master")
      backup.getBoolean("speech")
      backup.getBoolean("haptics")
    } catch (_: JSONException) {
      return false
    }
    val editor = prefs.edit()
    typingPackages(prefs).forEach { editor.remove(PREF_TYPING_PREFIX + it) }
    typing.forEach { editor.putBoolean(PREF_TYPING_PREFIX + it, true) }
    editor
      .putBoolean(PREF_MASTER, backup.getBoolean("master"))
      .putBoolean(PREF_SPEECH, backup.getBoolean("speech"))
      .putBoolean(PREF_HAPTICS, backup.getBoolean("haptics"))
      .putStringSet(PREF_APPS, apps)
      .putStringSet(PREF_SEEN, seen)
      .apply()
    return true
  }

  private fun stringSet(prefs: SharedPreferences, key: String): Set<String> =
    prefs.getStringSet(key, emptySet()).orEmpty().toSet()

  private fun typingPackages(prefs: SharedPreferences): Set<String> =
    prefs.all
      .filter { (key, value) -> key.startsWith(PREF_TYPING_PREFIX) && value == true }
      .keys
      .map { it.removePrefix(PREF_TYPING_PREFIX) }
      .toSet()

  private fun strings(array: JSONArray): Set<String> =
    (0 until array.length()).map { array.getString(it) }.toSet()
}
