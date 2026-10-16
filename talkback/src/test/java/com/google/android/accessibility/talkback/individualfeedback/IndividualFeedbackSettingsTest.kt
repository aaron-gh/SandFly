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

package com.google.android.accessibility.talkback.individualfeedback

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndividualFeedbackSettingsTest {
  private val prefs = FakeSharedPreferences()
  private val focus = IndividualFeedbackSettings.SOUNDS.first { it.key == "focus" }
  private val radialMenu = IndividualFeedbackSettings.SOUNDS.first { it.key == "radial_menu" }
  private val click =
    IndividualFeedbackSettings.VIBRATIONS.first { it.key == "view_clicked_pattern" }

  @Test
  fun everythingIsOnByDefault() {
    IndividualFeedbackSettings.SOUNDS.forEach {
      assertTrue(IndividualFeedbackSettings.isSoundOn(prefs, it))
    }
    IndividualFeedbackSettings.VIBRATIONS.forEach {
      assertTrue(IndividualFeedbackSettings.isVibrationOn(prefs, it))
    }
    assertTrue(IndividualFeedbackSettings.mutedSoundResources(prefs).isEmpty())
    assertTrue(IndividualFeedbackSettings.mutedVibrationResources(prefs).isEmpty())
  }

  @Test
  fun turningASoundOffMutesOnlyThatSound() {
    IndividualFeedbackSettings.setSoundOn(prefs, focus, false)
    assertFalse(IndividualFeedbackSettings.isSoundOn(prefs, focus))
    assertEquals(setOf("focus"), IndividualFeedbackSettings.mutedSoundResources(prefs))
    assertTrue(IndividualFeedbackSettings.mutedVibrationResources(prefs).isEmpty())

    IndividualFeedbackSettings.setSoundOn(prefs, focus, true)
    assertTrue(IndividualFeedbackSettings.isSoundOn(prefs, focus))
    assertTrue(IndividualFeedbackSettings.mutedSoundResources(prefs).isEmpty())
  }

  @Test
  fun soundsAndVibrationsAreSeparate() {
    IndividualFeedbackSettings.setVibrationOn(prefs, click, false)
    assertFalse(IndividualFeedbackSettings.isVibrationOn(prefs, click))
    assertEquals(
      setOf("view_clicked_pattern"),
      IndividualFeedbackSettings.mutedVibrationResources(prefs),
    )
    assertTrue(IndividualFeedbackSettings.mutedSoundResources(prefs).isEmpty())
  }

  @Test
  fun groupedItemMutesEveryResource() {
    IndividualFeedbackSettings.setSoundOn(prefs, radialMenu, false)
    assertEquals(
      (1..8).map { "radial_menu_$it" }.toSet(),
      IndividualFeedbackSettings.mutedSoundResources(prefs),
    )
  }

  @Test
  fun keysAreUnique() {
    assertEquals(
      IndividualFeedbackSettings.SOUNDS.size,
      IndividualFeedbackSettings.SOUNDS.map { it.key }.toSet().size,
    )
    assertEquals(
      IndividualFeedbackSettings.VIBRATIONS.size,
      IndividualFeedbackSettings.VIBRATIONS.map { it.key }.toSet().size,
    )
  }

  @Test
  fun theOldSelectionSwitchCarriesOverToTheVibrationSelectionNowPlays() {
    prefs
      .edit()
      .putStringSet(
        IndividualFeedbackSettings.PREF_MUTED_VIBRATIONS,
        setOf("view_focused_or_selected_pattern", "typo_pattern"),
      )
      .apply()
    IndividualFeedbackSettings.migrate(prefs)
    assertEquals(
      setOf("view_actionable_pattern", "typo_pattern"),
      IndividualFeedbackSettings.mutedVibrationResources(prefs),
    )
  }

  @Test
  fun migratingLeavesOtherChoicesAlone() {
    val typo = IndividualFeedbackSettings.VIBRATIONS.first { it.key == "typo_pattern" }
    IndividualFeedbackSettings.setVibrationOn(prefs, typo, false)
    IndividualFeedbackSettings.migrate(prefs)
    IndividualFeedbackSettings.migrate(prefs)
    assertEquals(setOf("typo_pattern"), IndividualFeedbackSettings.mutedVibrationResources(prefs))
  }

  @Test
  fun screenOffTakesOverTheVolumeChangeSwitchesThatWereOff() {
    // Screen off used to play the volume beep, under switches named Volume change.
    prefs
      .edit()
      .putStringSet(IndividualFeedbackSettings.PREF_MUTED_SOUNDS, setOf("volume_beep", "focus"))
      .putStringSet(IndividualFeedbackSettings.PREF_MUTED_VIBRATIONS, setOf("volume_pattern"))
      .apply()
    val screenOff = IndividualFeedbackSettings.SOUNDS.first { it.key == "screen_off" }
    val screenOffVibration =
      IndividualFeedbackSettings.VIBRATIONS.first { it.key == "screen_off_pattern" }

    IndividualFeedbackSettings.migrate(prefs)
    assertEquals(
      setOf("screen_off", "focus"),
      IndividualFeedbackSettings.mutedSoundResources(prefs),
    )
    assertEquals(
      setOf("screen_off_pattern"),
      IndividualFeedbackSettings.mutedVibrationResources(prefs),
    )

    // Only once, so screen off stays on after the user turns it back on.
    IndividualFeedbackSettings.setSoundOn(prefs, screenOff, true)
    IndividualFeedbackSettings.setVibrationOn(prefs, screenOffVibration, true)
    IndividualFeedbackSettings.migrate(prefs)
    assertTrue(IndividualFeedbackSettings.isSoundOn(prefs, screenOff))
    assertTrue(IndividualFeedbackSettings.isVibrationOn(prefs, screenOffVibration))
  }

  @Test
  fun screenOffStaysOnWhenVolumeChangeWasOn() {
    val screenOff = IndividualFeedbackSettings.SOUNDS.first { it.key == "screen_off" }
    IndividualFeedbackSettings.migrate(prefs)
    assertTrue(IndividualFeedbackSettings.isSoundOn(prefs, screenOff))
  }
}
