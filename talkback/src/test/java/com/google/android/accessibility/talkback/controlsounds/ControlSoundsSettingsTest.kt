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

package com.google.android.accessibility.talkback.controlsounds

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.individualfeedback.IndividualFeedbackSettings
import com.google.android.accessibility.utils.output.FeedbackController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlSoundsSettingsTest {
  private val prefs = FakeSharedPreferences()

  private val allChosen = ControlSounds.SOUNDS.keys

  private fun turnOn() = prefs.edit().putBoolean(ControlSoundsSettings.PREF_ON, true).apply()

  @Test
  fun offByDefault() {
    assertFalse(ControlSoundsSettings.isOn(prefs))
    assertFalse(ControlSoundsSettings.speakRoles(prefs))
    assertTrue(ControlSoundsSettings.playingSounds(prefs, true, emptySet(), allChosen).isEmpty())
  }

  @Test
  fun everyChosenSoundPlaysWhenOn() {
    turnOn()
    assertEquals(
      ControlSounds.SOUNDS.values.toSet(),
      ControlSoundsSettings.playingSounds(prefs, true, emptySet(), allChosen),
    )
  }

  @Test
  fun onlyControlsWithAChosenSoundPlay() {
    turnOn()
    assertEquals(
      setOf(R.id.control_button),
      ControlSoundsSettings.playingSounds(prefs, true, emptySet(), setOf("control_button", "focus")),
    )
    assertTrue(ControlSoundsSettings.playingSounds(prefs, true, emptySet(), emptySet()).isEmpty())
  }

  @Test
  fun noSoundPlaysWithSoundFeedbackOff() {
    turnOn()
    assertTrue(ControlSoundsSettings.playingSounds(prefs, false, emptySet(), allChosen).isEmpty())
  }

  @Test
  fun aSoundTurnedOffOnItsOwnDoesNotPlay() {
    turnOn()
    val playing = ControlSoundsSettings.playingSounds(prefs, true, setOf("control_button"), allChosen)
    assertFalse(R.id.control_button in playing)
    assertTrue(R.id.control_checkbox in playing)
  }

  @Test
  fun onlyControlsTheThemeGivesAVibrationAreFelt() {
    turnOn()
    assertEquals(
      setOf(R.id.control_button),
      ControlSoundsSettings.vibratingSounds(prefs, true, emptySet(), setOf("control_button")),
    )
    assertTrue(ControlSoundsSettings.vibratingSounds(prefs, true, emptySet(), emptySet()).isEmpty())
  }

  @Test
  fun controlVibrationsAreFeltWithSoundFeedbackOffButNotWithVibrationOff() {
    turnOn()
    assertTrue(ControlSoundsSettings.playingSounds(prefs, false, emptySet(), allChosen).isEmpty())
    assertEquals(
      ControlSounds.SOUNDS.values.toSet(),
      ControlSoundsSettings.vibratingSounds(prefs, true, emptySet(), allChosen),
    )
    assertTrue(ControlSoundsSettings.vibratingSounds(prefs, false, emptySet(), allChosen).isEmpty())
  }

  @Test
  fun aControlVibrationTurnedOffOnItsOwnIsNotFelt() {
    turnOn()
    val felt =
      ControlSoundsSettings.vibratingSounds(prefs, true, setOf("control_button_pattern"), allChosen)
    assertFalse(R.id.control_button in felt)
    assertTrue(R.id.control_checkbox in felt)
    // Links share the switch of the link vibration.
    val withoutLinks =
      ControlSoundsSettings.vibratingSounds(prefs, true, setOf("hyperlink_pattern"), allChosen)
    assertFalse(R.id.control_link in withoutLinks)
  }

  @Test
  fun noControlVibrationsWithControlSoundsOff() {
    assertTrue(ControlSoundsSettings.vibratingSounds(prefs, true, emptySet(), allChosen).isEmpty())
  }

  @Test
  fun everySoundHasAnIndividualSwitch() {
    val switches = IndividualFeedbackSettings.SOUNDS.flatMap { it.resourceNames }
    assertTrue(switches.containsAll(ControlSounds.SOUNDS.keys))
  }

  @Test
  fun threeDWithHeadphonesByDefault() {
    assertEquals(
      FeedbackController.SPATIAL_3D_WITH_HEADPHONES,
      ControlSoundsSettings.spatialMode(prefs),
    )
    prefs.edit().putString(ControlSoundsSettings.PREF_3D, ControlSoundsSettings.VALUE_3D_ALWAYS).apply()
    assertEquals(FeedbackController.SPATIAL_3D, ControlSoundsSettings.spatialMode(prefs))
    prefs.edit().putString(ControlSoundsSettings.PREF_3D, ControlSoundsSettings.VALUE_3D_NEVER).apply()
    assertEquals(FeedbackController.SPATIAL_STEREO, ControlSoundsSettings.spatialMode(prefs))
  }
}
