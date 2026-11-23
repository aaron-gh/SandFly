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

package com.google.android.accessibility.talkback.doubletaptaps

import android.content.SharedPreferences

/**
 * The apps where activating an item, such as with a double tap, taps it on the screen instead of
 * sending a click, for apps that report a click as handled and then ignore it.
 */
object DoubleTapTapsApps {
  const val PREF_APPS = "pref_double_tap_taps_apps"

  @JvmStatic
  fun isOn(prefs: SharedPreferences, pkg: CharSequence?): Boolean =
    pkg != null && pkg.toString() in apps(prefs)

  fun setOn(prefs: SharedPreferences, pkg: String, on: Boolean) {
    val apps = apps(prefs)
    prefs.edit().putStringSet(PREF_APPS, if (on) apps + pkg else apps - pkg).apply()
  }

  /** Turns the setting on or off for [pkg]. Returns whether it is now on. */
  @JvmStatic
  fun toggle(prefs: SharedPreferences, pkg: String): Boolean {
    val on = !isOn(prefs, pkg)
    setOn(prefs, pkg, on)
    return on
  }

  /**
   * Whether activating an item taps it on the screen: when a script rule has it touched, or when
   * the setting is on for the item's app.
   */
  @JvmStatic
  fun tapsToActivate(
    prefs: SharedPreferences,
    scriptTouches: Boolean,
    pkg: CharSequence?,
  ): Boolean = scriptTouches || isOn(prefs, pkg)

  // A copy: the set that SharedPreferences returns must not be changed.
  private fun apps(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(PREF_APPS, emptySet()).orEmpty().toSet()
}
