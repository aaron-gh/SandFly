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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ true),
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false),
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ true),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false),
    )
  }

  @Test
  fun aLowBatteryIsSaidBelowTheChosenLevel() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putString(ScreenAnnouncementSettings.SCREEN_ON_LOW_BATTERY, "20").apply()
    assertEquals(
      listOf(StatusItem.BATTERY),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, 19),
    )
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, 20),
    )
    // An unknown level is never low.
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, -1),
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ true, 5),
    )
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.WIFI),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, 5),
    )
    assertEquals(
      listOf(StatusItem.WIFI),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, 50),
    )
  }

  @Test
  fun lowBatteryIsOffByDefaultAndForUnknownValues() {
    val prefs = FakeSharedPreferences()
    assertEquals(
      emptyList<StatusItem>(),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false, 1),
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ true),
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false)
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
      ScreenAnnouncementSettings.screenOnStatusItems(prefs, /* charging= */ false),
    )
  }

  @Test
  fun screenOnStatusNeverRepeatsTheTime() {
    // The time has its own switch and is spoken first.
    assertFalse(StatusItem.TIME in ScreenAnnouncementSettings.SCREEN_ON_STATUS)
  }
}
