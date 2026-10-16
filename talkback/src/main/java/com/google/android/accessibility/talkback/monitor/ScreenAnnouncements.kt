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

package com.google.android.accessibility.talkback.monitor

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.status.StatusItem

/**
 * What Backtalk says when the screen turns off or on and when the phone is unlocked. The time
 * when the screen turns on keeps its own older setting, which the Backtalk menu and gestures also
 * change. The keys are also in res/xml/screen_announcement_preferences.xml.
 */
object ScreenAnnouncementSettings {
  const val SAY_SCREEN_OFF = "pref_screen_off_say_screen_off"
  const val SCREEN_OFF_RINGER = "pref_screen_off_ringer_mode"
  const val SAY_UNLOCKED = "pref_unlock_say_unlocked"

  /**
   * The status items that can be spoken after the time as the screen turns on, in the order they
   * are spoken, with their switches. They are separate from the status gesture's, so that each can
   * say different things. The time has its own older switch.
   */
  val SCREEN_ON_STATUS: Map<StatusItem, String> =
    linkedMapOf(
      StatusItem.BATTERY to "pref_screen_on_battery",
      StatusItem.WIFI to "pref_screen_on_wifi",
      StatusItem.MOBILE to "pref_screen_on_mobile_network",
      StatusItem.RINGER to "pref_screen_on_ringer",
      StatusItem.AIRPLANE_MODE to "pref_screen_on_airplane_mode",
    )

  /** Each setting's default, which is what Backtalk said before these settings existed. */
  val DEFAULTS: Map<String, Boolean> =
    mapOf(SAY_SCREEN_OFF to true, SCREEN_OFF_RINGER to true, SAY_UNLOCKED to true) +
      SCREEN_ON_STATUS.values.associateWith { false }

  @JvmStatic
  fun isOn(prefs: SharedPreferences, key: String): Boolean =
    prefs.getBoolean(key, DEFAULTS.getValue(key))

  /** The status items to speak after the time as the screen turns on. */
  @JvmStatic
  fun screenOnStatusItems(prefs: SharedPreferences): List<StatusItem> =
    SCREEN_ON_STATUS.filterValues { isOn(prefs, it) }.keys.toList()
}
