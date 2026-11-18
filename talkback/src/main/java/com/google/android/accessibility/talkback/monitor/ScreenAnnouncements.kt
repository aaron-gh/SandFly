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
   * The number of notifications, said after the time as the screen turns on. It needs notification
   * access, and nothing is said without it or with no notifications.
   */
  const val SCREEN_ON_NOTIFICATION_COUNT = "pref_screen_on_notification_count"

  /**
   * When the battery is spoken first after the time as the screen turns on: [BATTERY_OFF],
   * [BATTERY_ALWAYS] or [BATTERY_WHILE_CHARGING]. It replaced the on/off switch
   * [OLD_SCREEN_ON_BATTERY], whose on is [BATTERY_ALWAYS].
   */
  const val SCREEN_ON_BATTERY = "pref_screen_on_battery_mode"
  const val OLD_SCREEN_ON_BATTERY = "pref_screen_on_battery"
  const val BATTERY_OFF = "off"
  const val BATTERY_ALWAYS = "always"
  const val BATTERY_WHILE_CHARGING = "while_charging"
  private val BATTERY_VALUES = setOf(BATTERY_OFF, BATTERY_ALWAYS, BATTERY_WHILE_CHARGING)

  /**
   * The battery level in percent below which the battery is also spoken after the time as the
   * screen turns on, whatever [SCREEN_ON_BATTERY] says, or 0 for never. It is saved as a string
   * for the list on the settings screen.
   */
  const val SCREEN_ON_LOW_BATTERY = "pref_screen_on_low_battery"
  const val LOW_BATTERY_OFF = 0
  @JvmField val LOW_BATTERY_LEVELS = listOf(LOW_BATTERY_OFF, 10, 15, 20, 30, 50)

  /**
   * The status items with a switch that can be spoken after the time and the battery as the screen
   * turns on, in the order they are spoken. They are separate from the status gesture's, so that
   * each can say different things. The time has its own older switch.
   */
  val SCREEN_ON_STATUS: Map<StatusItem, String> =
    linkedMapOf(
      StatusItem.WIFI to "pref_screen_on_wifi",
      StatusItem.MOBILE to "pref_screen_on_mobile_network",
      StatusItem.RINGER to "pref_screen_on_ringer",
      StatusItem.AIRPLANE_MODE to "pref_screen_on_airplane_mode",
    )

  /**
   * Each switch's default, which is what Backtalk said before these settings existed, except the
   * number of notifications, which is new and on.
   */
  val DEFAULTS: Map<String, Boolean> =
    mapOf(
      SAY_SCREEN_OFF to true,
      SCREEN_OFF_RINGER to true,
      SAY_UNLOCKED to true,
      SCREEN_ON_NOTIFICATION_COUNT to true,
    ) +
      SCREEN_ON_STATUS.values.associateWith { false }

  @JvmStatic
  fun isOn(prefs: SharedPreferences, key: String): Boolean =
    prefs.getBoolean(key, DEFAULTS.getValue(key))

  /**
   * Reads when the battery is spoken, or the old switch's matching value while [migrateBattery]
   * hasn't run, as after importing settings from before the list.
   */
  @JvmStatic
  fun screenOnBattery(prefs: SharedPreferences): String =
    prefs.getString(SCREEN_ON_BATTERY, null)?.takeIf { it in BATTERY_VALUES }
      ?: oldBatteryValue(prefs.getBoolean(OLD_SCREEN_ON_BATTERY, false))

  /**
   * Gives the battery list the old switch's matching value, unless the list has a value or
   * [force], as after importing a file that holds the old switch.
   */
  @JvmStatic
  @JvmOverloads
  fun migrateBattery(prefs: SharedPreferences, force: Boolean = false) {
    if (!prefs.contains(OLD_SCREEN_ON_BATTERY) || (!force && prefs.contains(SCREEN_ON_BATTERY))) {
      return
    }
    val value = oldBatteryValue(prefs.getBoolean(OLD_SCREEN_ON_BATTERY, false))
    prefs.edit().putString(SCREEN_ON_BATTERY, value).apply()
  }

  /** The battery list's value for the old switch [on]. */
  @JvmStatic
  fun oldBatteryValue(on: Boolean): String = if (on) BATTERY_ALWAYS else BATTERY_OFF

  /** The level chosen for [SCREEN_ON_LOW_BATTERY], or [LOW_BATTERY_OFF]. */
  @JvmStatic
  fun lowBatteryLevel(prefs: SharedPreferences): Int =
    prefs.getString(SCREEN_ON_LOW_BATTERY, null)?.toIntOrNull()?.takeIf {
      it in LOW_BATTERY_LEVELS
    } ?: LOW_BATTERY_OFF

  /**
   * The status items to speak after the time as the screen turns on. [charging] is whether the
   * charger is connected, for [BATTERY_WHILE_CHARGING], and [batteryLevel] the battery level in
   * percent, or a negative number when it is unknown, for [SCREEN_ON_LOW_BATTERY].
   */
  @JvmStatic
  @JvmOverloads
  fun screenOnStatusItems(
    prefs: SharedPreferences,
    charging: Boolean,
    batteryLevel: Int = -1,
  ): List<StatusItem> {
    val battery =
      when (screenOnBattery(prefs)) {
        BATTERY_ALWAYS -> true
        BATTERY_WHILE_CHARGING -> charging
        else -> false
      } || batteryLevel in 0 until lowBatteryLevel(prefs)
    return (if (battery) listOf(StatusItem.BATTERY) else emptyList()) +
      SCREEN_ON_STATUS.filterValues { isOn(prefs, it) }.keys
  }
}
