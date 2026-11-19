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
 * The three times SandFly can read status items: when the screen turns off, when it turns on, and
 * when the phone is unlocked. Each has the same items, with keys that start with [prefix].
 */
enum class ScreenEvent(val prefix: String) {
  SCREEN_OFF("pref_screen_off_"),
  SCREEN_ON("pref_screen_on_"),
  UNLOCK("pref_unlock_"),
}

/** An item that SandFly can read at a [ScreenEvent], in the order they are read. */
enum class ScreenReadoutItem(val status: StatusItem?) {
  TIME(null),
  NOTIFICATION_COUNT(null),
  BATTERY(StatusItem.BATTERY),
  HIDDEN_SCREEN(StatusItem.HIDDEN_SCREEN),
  WIFI(StatusItem.WIFI),
  MOBILE(StatusItem.MOBILE),
  RINGER(StatusItem.RINGER),
  AIRPLANE_MODE(StatusItem.AIRPLANE_MODE),
}

/**
 * What SandFly says when the screen turns off or on and when the phone is unlocked. Each event can
 * read the same items, chosen separately. The time when the screen turns on keeps its own older
 * setting, which the SandFly menu and gestures also change. The keys are also in
 * res/xml/screen_announcement_preferences.xml.
 */
object ScreenAnnouncementSettings {
  const val SAY_SCREEN_OFF = "pref_screen_off_say_screen_off"

  /**
   * Says "Ringer vibrate" or "Ringer silent" after "Screen off". The screen-off [RINGER] item, which
   * also says Do Not Disturb, replaces it while that is on.
   */
  const val SCREEN_OFF_RINGER = "pref_screen_off_ringer_mode"
  const val SAY_UNLOCKED = "pref_unlock_say_unlocked"

  /** The screen-on time switch, which is older than the other events' time switches. */
  const val SCREEN_ON_TIME = "pref_speak_time_key"

  /**
   * The number of notifications when the screen turns on. It needs notification access, and nothing
   * is said without it or with no notifications.
   */
  const val SCREEN_ON_NOTIFICATION_COUNT = "pref_screen_on_notification_count"

  /**
   * When the battery is spoken as the screen turns on: [BATTERY_OFF], [BATTERY_ALWAYS] or
   * [BATTERY_WHILE_CHARGING]. It replaced the on/off switch [OLD_SCREEN_ON_BATTERY], whose on is
   * [BATTERY_ALWAYS]. The other events have no old switch.
   */
  const val SCREEN_ON_BATTERY = "pref_screen_on_battery_mode"
  const val OLD_SCREEN_ON_BATTERY = "pref_screen_on_battery"
  const val BATTERY_OFF = "off"
  const val BATTERY_ALWAYS = "always"
  const val BATTERY_WHILE_CHARGING = "while_charging"
  private val BATTERY_VALUES = setOf(BATTERY_OFF, BATTERY_ALWAYS, BATTERY_WHILE_CHARGING)

  /**
   * The battery level in percent below which the battery is also spoken as the screen turns on,
   * whatever [SCREEN_ON_BATTERY] says, or 0 for never. It is saved as a string for the list on the
   * settings screen.
   */
  const val SCREEN_ON_LOW_BATTERY = "pref_screen_on_low_battery"
  const val LOW_BATTERY_OFF = 0
  @JvmField val LOW_BATTERY_LEVELS = listOf(LOW_BATTERY_OFF, 10, 15, 20, 30, 50)

  /**
   * How long after the screen-on readout the unlock readout leaves out the items it read. With face
   * or fingerprint unlock, the phone is unlocked right after the screen turns on.
   */
  const val REPEAT_WINDOW_MS = 5_000L

  /**
   * The key ends of the status items with a switch, in the order they are spoken, after the time,
   * the number of notifications and the battery. The hidden screen comes first of these, as it
   * decides whether the screen can be shown to someone. They are separate from the status gesture's,
   * so that each can say different things.
   */
  private val STATUS_SWITCHES: Map<ScreenReadoutItem, String> =
    linkedMapOf(
      ScreenReadoutItem.HIDDEN_SCREEN to "hidden_screen",
      ScreenReadoutItem.WIFI to "wifi",
      ScreenReadoutItem.MOBILE to "mobile_network",
      ScreenReadoutItem.RINGER to "ringer",
      ScreenReadoutItem.AIRPLANE_MODE to "airplane_mode",
    )

  /** The time switch of [event]. */
  @JvmStatic
  fun timeKey(event: ScreenEvent): String =
    if (event == ScreenEvent.SCREEN_ON) SCREEN_ON_TIME else event.prefix + "time"

  /** The number of notifications switch of [event]. */
  @JvmStatic
  fun notificationCountKey(event: ScreenEvent): String = event.prefix + "notification_count"

  /** The battery list of [event]. */
  @JvmStatic fun batteryKey(event: ScreenEvent): String = event.prefix + "battery_mode"

  /** The low battery list of [event]. */
  @JvmStatic fun lowBatteryKey(event: ScreenEvent): String = event.prefix + "low_battery"

  /** The switch of the status [item] at [event], or null for the items without a switch. */
  @JvmStatic
  fun statusKey(event: ScreenEvent, item: ScreenReadoutItem): String? =
    STATUS_SWITCHES[item]?.let { event.prefix + it }

  /** Every number of notifications switch, which each need the notification listener. */
  @JvmField
  val NOTIFICATION_COUNT_KEYS: Set<String> =
    ScreenEvent.entries.map { notificationCountKey(it) }.toSet()

  /**
   * Each switch's default, which is what SandFly said before these settings existed, except the
   * number of notifications when the screen turns on, which is on. The screen-on time has its own
   * default in the resources.
   */
  val DEFAULTS: Map<String, Boolean> =
    mapOf(SAY_SCREEN_OFF to true, SCREEN_OFF_RINGER to true, SAY_UNLOCKED to true) +
      ScreenEvent.entries.flatMap { event ->
        listOfNotNull(
          timeKey(event).takeUnless { event == ScreenEvent.SCREEN_ON },
          notificationCountKey(event),
        ) + STATUS_SWITCHES.keys.mapNotNull { statusKey(event, it) }
      }.associateWith { it == SCREEN_ON_NOTIFICATION_COUNT }

  @JvmStatic
  fun isOn(prefs: SharedPreferences, key: String): Boolean =
    prefs.getBoolean(key, DEFAULTS.getValue(key))

  /** Whether any event reads the number of notifications. */
  @JvmStatic
  fun anyNotificationCount(prefs: SharedPreferences): Boolean =
    NOTIFICATION_COUNT_KEYS.any { isOn(prefs, it) }

  /**
   * Reads when the battery is spoken as the screen turns on, or the old switch's matching value
   * while [migrateBattery] hasn't run, as after importing settings from before the list.
   */
  @JvmStatic
  fun screenOnBattery(prefs: SharedPreferences): String = battery(prefs, ScreenEvent.SCREEN_ON)

  /** Reads when the battery is spoken at [event]. */
  @JvmStatic
  fun battery(prefs: SharedPreferences, event: ScreenEvent): String =
    prefs.getString(batteryKey(event), null)?.takeIf { it in BATTERY_VALUES }
      ?: if (event == ScreenEvent.SCREEN_ON) {
        oldBatteryValue(prefs.getBoolean(OLD_SCREEN_ON_BATTERY, false))
      } else {
        BATTERY_OFF
      }

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

  /** The level chosen for the screen-on [SCREEN_ON_LOW_BATTERY], or [LOW_BATTERY_OFF]. */
  @JvmStatic
  fun lowBatteryLevel(prefs: SharedPreferences): Int =
    lowBatteryLevel(prefs, ScreenEvent.SCREEN_ON)

  /** The low battery level chosen for [event], or [LOW_BATTERY_OFF]. */
  @JvmStatic
  fun lowBatteryLevel(prefs: SharedPreferences, event: ScreenEvent): Int =
    prefs.getString(lowBatteryKey(event), null)?.toIntOrNull()?.takeIf {
      it in LOW_BATTERY_LEVELS
    } ?: LOW_BATTERY_OFF

  /**
   * The items to read at [event], in the order they are read. [screenOnTime] is the screen-on time
   * switch, which only that event uses. [charging] is whether the charger is connected, for
   * [BATTERY_WHILE_CHARGING], and [batteryLevel] the battery level in percent, or a negative number
   * when it is unknown, for the low battery list.
   */
  @JvmStatic
  @JvmOverloads
  fun readoutItems(
    prefs: SharedPreferences,
    event: ScreenEvent,
    screenOnTime: Boolean,
    charging: Boolean,
    batteryLevel: Int = -1,
  ): List<ScreenReadoutItem> {
    val time =
      if (event == ScreenEvent.SCREEN_ON) screenOnTime else isOn(prefs, timeKey(event))
    val battery =
      when (battery(prefs, event)) {
        BATTERY_ALWAYS -> true
        BATTERY_WHILE_CHARGING -> charging
        else -> false
      } || batteryLevel in 0 until lowBatteryLevel(prefs, event)
    return buildList {
      if (time) add(ScreenReadoutItem.TIME)
      if (isOn(prefs, notificationCountKey(event))) add(ScreenReadoutItem.NOTIFICATION_COUNT)
      if (battery) add(ScreenReadoutItem.BATTERY)
      for ((item, end) in STATUS_SWITCHES) {
        if (isOn(prefs, event.prefix + end)) add(item)
      }
    }
  }

  /**
   * The status items to read at [event] after the time and the number of notifications, in the
   * order they are read. See [readoutItems].
   */
  @JvmStatic
  @JvmOverloads
  fun statusItems(
    prefs: SharedPreferences,
    event: ScreenEvent,
    charging: Boolean,
    batteryLevel: Int = -1,
  ): List<StatusItem> =
    readoutItems(prefs, event, screenOnTime = false, charging, batteryLevel).mapNotNull {
      it.status
    }

  /**
   * Whether to say "Ringer vibrate" or "Ringer silent" after "Screen off": [SCREEN_OFF_RINGER] is
   * on and the screen-off ringer status item, which says the same and Do Not Disturb, is off.
   */
  @JvmStatic
  fun screenOffRingerMode(prefs: SharedPreferences): Boolean =
    isOn(prefs, SCREEN_OFF_RINGER) &&
      !isOn(prefs, statusKey(ScreenEvent.SCREEN_OFF, ScreenReadoutItem.RINGER)!!)

  /**
   * [items] for the unlock readout without those that [screenOnItems] read less than
   * [REPEAT_WINDOW_MS] before, at [screenOnAt]; [now] and [screenOnAt] are in the same clock.
   */
  @JvmStatic
  fun withoutRepeats(
    items: List<ScreenReadoutItem>,
    screenOnItems: Collection<ScreenReadoutItem>,
    screenOnAt: Long,
    now: Long,
  ): List<ScreenReadoutItem> =
    if (screenOnItems.isEmpty() || now - screenOnAt !in 0 until REPEAT_WINDOW_MS) items
    else items.filterNot { it in screenOnItems }
}
