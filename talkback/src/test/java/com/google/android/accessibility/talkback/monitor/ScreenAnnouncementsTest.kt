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
    assertEquals(emptyList<StatusItem>(), ScreenAnnouncementSettings.screenOnStatusItems(prefs))
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
      .putBoolean("pref_screen_on_battery", true)
      .apply()
    assertEquals(
      listOf(StatusItem.BATTERY, StatusItem.WIFI),
      ScreenAnnouncementSettings.screenOnStatusItems(prefs),
    )
  }

  @Test
  fun screenOnStatusNeverRepeatsTheTime() {
    // The time has its own switch and is spoken first.
    assertFalse(StatusItem.TIME in ScreenAnnouncementSettings.SCREEN_ON_STATUS)
  }
}
