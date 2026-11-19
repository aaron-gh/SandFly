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

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.status.StatusItem
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ScreenAnnouncementsTest {

  @Test
  fun theSettingsScreenMatchesTheKeysAndDefaults() {
    // Unit tests run in the module directory.
    val screen = File("src/main/res/xml/screen_announcement_preferences.xml")
    val switches =
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(screen).getElementsByTagName(
        "com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference"
      )
    val onScreen = mutableMapOf<String, Boolean>()
    for (i in 0 until switches.length) {
      val switch = switches.item(i) as Element
      val key = switch.getAttribute("android:key")
      if (!key.startsWith("@")) {
        onScreen[key] = switch.getAttribute("android:defaultValue").toBooleanStrict()
      }
    }
    assertEquals(ScreenAnnouncementSettings.DEFAULTS, onScreen)
  }

  @Test
  fun byDefaultBacktalkSaysWhatItAlwaysHas() {
    val prefs = FakeSharedPreferences()
    assertTrue(ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SAY_SCREEN_OFF))
    assertTrue(
      ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SCREEN_OFF_RINGER)
    )
    assertTrue(ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SAY_UNLOCKED))
    assertTrue(
      ScreenAnnouncementSettings.isOn(
        prefs,
        ScreenAnnouncementSettings.SCREEN_ON_NOTIFICATION_COUNT,
      )
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ true),
    )
  }

  @Test
  fun aSwitchTurnedOffIsRead() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean(ScreenAnnouncementSettings.SAY_SCREEN_OFF, false).apply()
    assertFalse(ScreenAnnouncementSettings.isOn(prefs, ScreenAnnouncementSettings.SAY_SCREEN_OFF))
  }

  @Test
  fun screenOnStatusIsChosenItemByItemInAFixedOrder() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putBoolean("pref_screen_on_wifi", true)
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_ALWAYS,
      )
      .apply()
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.WIFI),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false),
    )
  }

  @Test
  fun theBatteryOnlyWhileChargingIsSaidOnlyWhileCharging() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_WHILE_CHARGING,
      )
      .apply()
    assertEquals(
      listOf(StatusItem.BATTERY),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ true),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false),
    )
  }

  @Test
  fun aLowBatteryIsSaidBelowTheChosenLevel() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putString(ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY, "20").apply()
    assertEquals(
      listOf(StatusItem.BATTERY),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, 19),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, 20),
    )
    // An unknown level is never low.
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, -1),
    )
  }

  @Test
  fun aLowBatteryIsSaidOnceWithTheBatteryList() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putString(ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY, "20")
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_WHILE_CHARGING,
      )
      .putBoolean("pref_screen_on_wifi", true)
      .apply()
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.WIFI),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ true, 5),
    )
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.WIFI),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, 5),
    )
    assertEquals(
      listOf(StatusItem.WIFI),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, 50),
    )
  }

  @Test
  fun lowBatteryIsOffByDefaultAndForUnknownValues() {
    val prefs = FakeSharedPreferences()
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false, 1),
    )
    prefs.edit().putString(ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY, "99").apply()
    assertEquals(
      ScreenAnnouncementSettings.LOW_BATTERY_OFF,
      ScreenAnnouncementSettings.lowBatteryLevel(prefs),
    )
  }

  @Test
  fun theLowBatteryListMatchesTheLevels() {
    val screen = File("src/main/res/xml/screen_announcement_preferences.xml")
    val values =
      File("src/main/res/values/donottranslate.xml")
        .readText()
        .substringAfter("<string-array name=\"pref_screen_on_low_battery_values\"")
        .substringBefore("</string-array>")
        .let { Regex("<item>(\\d+)</item>").findAll(it).map { m -> m.groupValues[1].toInt() } }
        .toList()
    assertEquals(ScreenAnnouncementSettings.LOW_BATTERY_LEVELS, values)
    assertTrue(screen.readText().contains(ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY))
  }

  @Test
  fun theBatteryOffIsNeverSaid() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_OFF,
      )
      .putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, true)
      .apply()
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ true),
    )
  }

  @Test
  fun theOldBatterySwitchOnBecomesAlways() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, true).apply()
    // Read before the migration, as after importing old settings.
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_ALWAYS,
      ScreenAnnouncementSettings.screenOnBattery(prefs),
    )
    ScreenAnnouncementSettings.migrateBattery(prefs)
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_ALWAYS,
      prefs.getString(ScreenAnnouncementSettings.SCREEN_ON_BATTERY, null),
    )
  }

  @Test
  fun theOldBatterySwitchOffBecomesOff() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, false).apply()
    ScreenAnnouncementSettings.migrateBattery(prefs)
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_OFF,
      prefs.getString(ScreenAnnouncementSettings.SCREEN_ON_BATTERY, null),
    )
  }

  @Test
  fun theMigrationKeepsAChosenBatteryValueAndSkipsNewInstalls() {
    val prefs = FakeSharedPreferences()
    ScreenAnnouncementSettings.migrateBattery(prefs)
    assertFalse(prefs.contains(ScreenAnnouncementSettings.SCREEN_ON_BATTERY))
    prefs
      .edit()
      .putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, true)
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_WHILE_CHARGING,
      )
      .apply()
    ScreenAnnouncementSettings.migrateBattery(prefs)
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_WHILE_CHARGING,
      prefs.getString(ScreenAnnouncementSettings.SCREEN_ON_BATTERY, null),
    )
  }

  @Test
  fun aForcedMigrationReplacesTheBatteryValue() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, true)
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_OFF,
      )
      .apply()
    ScreenAnnouncementSettings.migrateBattery(prefs, force = true)
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_ALWAYS,
      prefs.getString(ScreenAnnouncementSettings.SCREEN_ON_BATTERY, null),
    )
  }

  @Test
  fun theBatteryListOnScreenMatchesTheValues() {
    val screen = File("src/main/res/xml/screen_announcement_preferences.xml")
    val lists =
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(screen).getElementsByTagName(
        "com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference"
      )
    val battery =
      (0 until lists.length)
        .map { lists.item(it) as Element }
        .single { it.getAttribute("android:key") == ScreenAnnouncementSettings.SCREEN_ON_BATTERY }
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_OFF,
      battery.getAttribute("android:defaultValue"),
    )
  }

  @Test
  fun theHiddenScreenIsOffByDefaultAndSaidFirstAfterTheBattery() {
    val prefs = FakeSharedPreferences()
    assertFalse(
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false)
        .contains(StatusItem.HIDDEN_SCREEN)
    )
    prefs
      .edit()
      .putBoolean("pref_screen_on_hidden_screen", true)
      .putBoolean("pref_screen_on_wifi", true)
      .putString(
        ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
        ScreenAnnouncementSettings.BATTERY_ALWAYS,
      )
      .apply()
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.HIDDEN_SCREEN, StatusItem.WIFI),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_ON, /* charging= */ false),
    )
  }

  @Test
  fun statusItemsNeverRepeatTheTime() {
    // The time has its own switch and is spoken first.
    assertFalse(ScreenReadoutItem.entries.any { it.status == StatusItem.TIME })
  }

  @Test
  fun eachEventHasTheSameItemsWithTheSameTitlesOnTheScreen() {
    val screen = File("src/main/res/xml/screen_announcement_preferences.xml")
    val categories =
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(screen).getElementsByTagName(
        "com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory"
      )
    fun rows(categoryKey: String): Map<String, String> {
      val category =
        (0 until categories.length)
          .map { categories.item(it) as Element }
          .single { it.getAttribute("android:key") == categoryKey }
      val children = category.childNodes
      return (0 until children.length)
        .mapNotNull { children.item(it) as? Element }
        .associate { it.getAttribute("android:key") to it.getAttribute("android:title") }
    }
    fun itemKeys(event: ScreenEvent): List<String> =
      listOf(
        if (event == ScreenEvent.SCREEN_ON) "@string/pref_speak_time_key"
        else ScreenAnnouncementSettings.timeKey(event),
        ScreenAnnouncementSettings.notificationCountKey(event),
        ScreenAnnouncementSettings.batteryKey(event),
        ScreenAnnouncementSettings.lowBatteryKey(event),
      ) + ScreenReadoutItem.entries.mapNotNull { ScreenAnnouncementSettings.statusKey(event, it) }
    val off = rows("pref_screen_announcements_screen_off_category")
    val on = rows("pref_screen_announcements_screen_on_category")
    val unlock = rows("pref_screen_announcements_unlock_category")
    val onTitles = itemKeys(ScreenEvent.SCREEN_ON).map { on.getValue(it) }
    assertEquals(onTitles, itemKeys(ScreenEvent.SCREEN_OFF).map { off.getValue(it) })
    assertEquals(onTitles, itemKeys(ScreenEvent.UNLOCK).map { unlock.getValue(it) })
    // Plus the event's own switches.
    assertEquals(
      setOf(ScreenAnnouncementSettings.SAY_SCREEN_OFF, ScreenAnnouncementSettings.SCREEN_OFF_RINGER),
      off.keys - itemKeys(ScreenEvent.SCREEN_OFF).toSet(),
    )
    assertEquals(emptySet<String>(), on.keys - itemKeys(ScreenEvent.SCREEN_ON).toSet())
    assertEquals(
      setOf(ScreenAnnouncementSettings.SAY_UNLOCKED),
      unlock.keys - itemKeys(ScreenEvent.UNLOCK).toSet(),
    )
  }

  @Test
  fun theExistingKeysKeepMeaningTheScreenOnItems() {
    assertEquals(
      "pref_screen_on_notification_count",
      ScreenAnnouncementSettings.notificationCountKey(ScreenEvent.SCREEN_ON),
    )
    assertEquals(
      ScreenAnnouncementSettings.SCREEN_ON_BATTERY,
      ScreenAnnouncementSettings.batteryKey(ScreenEvent.SCREEN_ON),
    )
    assertEquals(
      ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY,
      ScreenAnnouncementSettings.lowBatteryKey(ScreenEvent.SCREEN_ON),
    )
    assertEquals(
      listOf(
        "pref_screen_on_hidden_screen",
        "pref_screen_on_wifi",
        "pref_screen_on_mobile_network",
        "pref_screen_on_ringer",
        "pref_screen_on_airplane_mode",
      ),
      ScreenReadoutItem.entries.mapNotNull {
        ScreenAnnouncementSettings.statusKey(ScreenEvent.SCREEN_ON, it)
      },
    )
  }

  @Test
  fun byDefaultEachEventReadsWhatItAlwaysHas() {
    val prefs = FakeSharedPreferences()
    assertEquals(
      listOf(ScreenReadoutItem.TIME, ScreenReadoutItem.NOTIFICATION_COUNT),
      ScreenAnnouncementSettings.readoutItems(
        prefs,
        ScreenEvent.SCREEN_ON,
        screenOnTime = true,
        charging = true,
        batteryLevel = 5,
      ),
    )
    for (event in listOf(ScreenEvent.SCREEN_OFF, ScreenEvent.UNLOCK)) {
      assertEquals(
        emptyList<ScreenReadoutItem>(),
        ScreenAnnouncementSettings.readoutItems(
          prefs,
          event,
          screenOnTime = true,
          charging = true,
          batteryLevel = 5,
        ),
      )
    }
    assertTrue(ScreenAnnouncementSettings.screenOffRingerMode(prefs))
  }

  @Test
  fun eachEventReadsItsOwnChoice() {
    // The example from report #483.
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putBoolean(ScreenAnnouncementSettings.SCREEN_ON_NOTIFICATION_COUNT, false)
      .putString(
        ScreenAnnouncementSettings.batteryKey(ScreenEvent.SCREEN_ON),
        ScreenAnnouncementSettings.BATTERY_ALWAYS,
      )
      .putBoolean("pref_unlock_mobile_network", true)
      .putBoolean("pref_screen_off_notification_count", true)
      .putBoolean("pref_screen_off_ringer", true)
      .apply()
    assertEquals(
      listOf(ScreenReadoutItem.TIME, ScreenReadoutItem.BATTERY),
      ScreenAnnouncementSettings.readoutItems(
        prefs,
        ScreenEvent.SCREEN_ON,
        screenOnTime = true,
        charging = false,
      ),
    )
    assertEquals(
      listOf(ScreenReadoutItem.MOBILE),
      ScreenAnnouncementSettings.readoutItems(
        prefs,
        ScreenEvent.UNLOCK,
        screenOnTime = true,
        charging = false,
      ),
    )
    assertEquals(
      listOf(ScreenReadoutItem.NOTIFICATION_COUNT, ScreenReadoutItem.RINGER),
      ScreenAnnouncementSettings.readoutItems(
        prefs,
        ScreenEvent.SCREEN_OFF,
        screenOnTime = true,
        charging = false,
      ),
    )
    // The ringer item says the ringer mode, so "Ringer vibrate" isn't said as well.
    assertFalse(ScreenAnnouncementSettings.screenOffRingerMode(prefs))
  }

  @Test
  fun everyItemIsReadInTheSameOrderAtEachEvent() {
    for (event in ScreenEvent.entries) {
      val prefs = FakeSharedPreferences()
      val edit =
        prefs
          .edit()
          .putBoolean(ScreenAnnouncementSettings.timeKey(event), true)
          .putBoolean(ScreenAnnouncementSettings.notificationCountKey(event), true)
          .putString(
            ScreenAnnouncementSettings.batteryKey(event),
            ScreenAnnouncementSettings.BATTERY_ALWAYS,
          )
      for (item in ScreenReadoutItem.entries) {
        ScreenAnnouncementSettings.statusKey(event, item)?.let { edit.putBoolean(it, true) }
      }
      edit.apply()
      assertEquals(
        ScreenReadoutItem.entries,
        ScreenAnnouncementSettings.readoutItems(
          prefs,
          event,
          screenOnTime = true,
          charging = false,
        ),
      )
    }
  }

  @Test
  fun theScreenOnTimeSwitchIsOnlyForTheScreenTurningOn() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean(ScreenAnnouncementSettings.timeKey(ScreenEvent.UNLOCK), true).apply()
    assertEquals(
      listOf(ScreenReadoutItem.TIME),
      ScreenAnnouncementSettings.readoutItems(
        prefs,
        ScreenEvent.UNLOCK,
        screenOnTime = false,
        charging = false,
      ),
    )
    assertFalse(
      ScreenReadoutItem.TIME in
        ScreenAnnouncementSettings.readoutItems(
          prefs,
          ScreenEvent.SCREEN_ON,
          screenOnTime = false,
          charging = false,
        )
    )
    assertFalse(
      ScreenReadoutItem.TIME in
        ScreenAnnouncementSettings.readoutItems(
          prefs,
          ScreenEvent.SCREEN_OFF,
          screenOnTime = true,
          charging = false,
        )
    )
  }

  @Test
  fun theOtherEventsHaveTheirOwnBatteryListsWithoutTheOldSwitch() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putBoolean(ScreenAnnouncementSettings.OLD_SCREEN_ON_BATTERY, true)
      .putString(
        ScreenAnnouncementSettings.batteryKey(ScreenEvent.UNLOCK),
        ScreenAnnouncementSettings.BATTERY_WHILE_CHARGING,
      )
      .putString(ScreenAnnouncementSettings.lowBatteryKey(ScreenEvent.SCREEN_OFF), "20")
      .apply()
    assertEquals(
      ScreenAnnouncementSettings.BATTERY_OFF,
      ScreenAnnouncementSettings.battery(prefs, ScreenEvent.SCREEN_OFF),
    )
    assertEquals(
      listOf(StatusItem.BATTERY),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.UNLOCK, charging = true),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.UNLOCK, charging = false, 10),
    )
    assertEquals(
      listOf(StatusItem.BATTERY),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_OFF, charging = false, 10),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.statusItems(prefs, ScreenEvent.SCREEN_OFF, charging = false, 20),
    )
  }

  @Test
  fun unlockingRightAfterTheScreenTurnsOnLeavesOutWhatWasJustRead() {
    val unlock = listOf(ScreenReadoutItem.TIME, ScreenReadoutItem.MOBILE)
    val screenOn = listOf(ScreenReadoutItem.TIME, ScreenReadoutItem.BATTERY)
    assertEquals(
      listOf(ScreenReadoutItem.MOBILE),
      ScreenAnnouncementSettings.withoutRepeats(unlock, screenOn, screenOnAt = 1_000, now = 2_000),
    )
    val window = ScreenAnnouncementSettings.REPEAT_WINDOW_MS
    assertEquals(
      unlock,
      ScreenAnnouncementSettings.withoutRepeats(unlock, screenOn, 1_000, 1_000 + window),
    )
    assertEquals(
      unlock,
      ScreenAnnouncementSettings.withoutRepeats(unlock, emptyList(), 1_000, 2_000),
    )
  }

  @Test
  fun anyNotificationCountSwitchNeedsTheListener() {
    val prefs = FakeSharedPreferences()
    assertTrue(ScreenAnnouncementSettings.anyNotificationCount(prefs))
    prefs.edit().putBoolean(ScreenAnnouncementSettings.SCREEN_ON_NOTIFICATION_COUNT, false).apply()
    assertFalse(ScreenAnnouncementSettings.anyNotificationCount(prefs))
    prefs.edit().putBoolean("pref_unlock_notification_count", true).apply()
    assertTrue(ScreenAnnouncementSettings.anyNotificationCount(prefs))
    assertEquals(3, ScreenAnnouncementSettings.NOTIFICATION_COUNT_KEYS.size)
  }
}
