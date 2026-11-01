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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectTouchSettingsTest {
  private val prefs = FakeSharedPreferences()

  @Test
  fun defaults() {
    assertTrue(DirectTouchSettings.isMasterEnabled(prefs))
    assertTrue(DirectTouchSettings.isSpeechEnabled(prefs))
    assertFalse(DirectTouchSettings.isHapticsEnabled(prefs))
    assertFalse(DirectTouchSettings.isAppEnabled(prefs, "a.b"))
    assertFalse(DirectTouchSettings.isDirectTyping(prefs, "a.b"))
    assertFalse(DirectTouchSettings.isNavBarTapEnabled(prefs))
  }

  @Test
  fun firstSightEnablesCapableAppOnce() {
    DirectTouchSettings.onFirstSight(prefs, "a.b", declaresCapability = true)
    assertTrue(DirectTouchSettings.isAppEnabled(prefs, "a.b"))
    DirectTouchSettings.setAppEnabled(prefs, "a.b", false)
    DirectTouchSettings.onFirstSight(prefs, "a.b", declaresCapability = true)
    assertFalse(DirectTouchSettings.isAppEnabled(prefs, "a.b"))
  }

  @Test
  fun appIsSeenOnceFirstSightRecordsIt() {
    assertFalse(DirectTouchSettings.isSeen(prefs, "a.b"))
    DirectTouchSettings.onFirstSight(prefs, "a.b", declaresCapability = false)
    assertTrue(DirectTouchSettings.isSeen(prefs, "a.b"))
    assertFalse(DirectTouchSettings.isSeen(prefs, "c.d"))
  }

  @Test
  fun firstSightIgnoresIncapableApp() {
    DirectTouchSettings.onFirstSight(prefs, "a.b", declaresCapability = false)
    assertFalse(DirectTouchSettings.isAppEnabled(prefs, "a.b"))
  }

  @Test
  fun backupRoundTrip() {
    DirectTouchSettings.setAppEnabled(prefs, "a.b", true)
    DirectTouchSettings.setDirectTyping(prefs, "a.b", true)
    DirectTouchSettings.setMasterEnabled(prefs, false)
    val json = DirectTouchSettings.exportJson(prefs)
    val other = FakeSharedPreferences()
    assertTrue(DirectTouchSettings.importJson(other, json))
    assertTrue(DirectTouchSettings.isAppEnabled(other, "a.b"))
    assertTrue(DirectTouchSettings.isDirectTyping(other, "a.b"))
    assertFalse(DirectTouchSettings.isMasterEnabled(other))
  }

  @Test
  fun importReplacesExistingTyping() {
    DirectTouchSettings.setDirectTyping(prefs, "old.app", true)
    val json = DirectTouchSettings.exportJson(FakeSharedPreferences())
    assertTrue(DirectTouchSettings.importJson(prefs, json))
    assertFalse(DirectTouchSettings.isDirectTyping(prefs, "old.app"))
  }

  @Test
  fun importRejectsGarbage() {
    assertFalse(DirectTouchSettings.importJson(prefs, "not json"))
    assertFalse(DirectTouchSettings.importJson(prefs, "{\"version\":2}"))
    assertTrue(DirectTouchSettings.isMasterEnabled(prefs))
  }

  @Test
  fun oldNavBarSettingTurnsOnTapToActivateNavigationBarButtons() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean("pref_direct_touch_nav_bar", true).apply()
    DirectTouchSettings.migrateNavBarSetting(prefs)
    assertTrue(DirectTouchSettings.isNavBarTapEnabled(prefs))
    assertFalse(prefs.contains("pref_direct_touch_nav_bar"))
  }

  @Test
  fun oldNavBarSettingOffChangesNothing() {
    val prefs = FakeSharedPreferences()
    prefs.edit().putBoolean("pref_direct_touch_nav_bar", false).apply()
    DirectTouchSettings.migrateNavBarSetting(prefs)
    assertFalse(prefs.contains(DirectTouchSettings.PREF_NAV_BAR_TAP))
    assertFalse(prefs.contains("pref_direct_touch_nav_bar"))
  }
}
