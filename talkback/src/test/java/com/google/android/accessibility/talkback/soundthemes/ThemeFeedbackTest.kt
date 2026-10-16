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

package com.google.android.accessibility.talkback.soundthemes

import com.google.android.accessibility.talkback.individualfeedback.IndividualFeedbackSettings
import com.google.android.accessibility.talkback.individualfeedback.SoundVibrations
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFeedbackTest {
  private val feedback =
    ThemeFeedback(
      soundPaths = emptyMap(),
      vibrations =
        mapOf(
          "focus" to intArrayOf(0, 20),
          "control_button" to intArrayOf(0, 30),
          "control_link" to intArrayOf(0, 40),
          "tick" to IntArray(0),
        ),
    )

  @Test
  fun vibrationsTurnedOffPlayNothingRatherThanBacktalks() {
    val playing =
      feedback.vibrationsPlaying(setOf("view_hovered_pattern", "control_button_pattern"))
    assertEquals(0, playing.getValue("focus").size)
    assertEquals(0, playing.getValue("control_button").size)
    assertArrayEquals(intArrayOf(0, 40), playing["control_link"])
  }

  @Test
  fun linksAreTurnedOffWithTheLinkVibration() {
    assertEquals(0, feedback.vibrationsPlaying(setOf("hyperlink_pattern")).getValue("control_link").size)
  }

  @Test
  fun brailleKeyboardVibrationsTurnedOffPlayNothingWithoutATheme() {
    val playing = feedback.vibrationsPlaying(setOf("braille_keyboard_space"))
    assertEquals(0, playing.getValue("braille_keyboard_space").size)
    assertEquals(null, playing["braille_keyboard_character"])
  }

  @Test
  fun brailleKeyboardVibrationsTurnedOffPlayNothingWithATheme() {
    val themed =
      ThemeFeedback(emptyMap(), mapOf("braille_keyboard_space" to intArrayOf(0, 20)))
    val playing = themed.vibrationsPlaying(setOf("braille_keyboard_space"))
    assertEquals(0, playing.getValue("braille_keyboard_space").size)
    assertArrayEquals(
      intArrayOf(0, 20),
      themed.vibrationsPlaying(emptySet())["braille_keyboard_space"],
    )
  }

  @Test
  fun everyBrailleKeyboardVibrationHasASwitch() {
    assertEquals(
      SoundVibrations.BRAILLE_KEYBOARD,
      IndividualFeedbackSettings.BRAILLE_KEYBOARD_VIBRATIONS.map { it.key }.toSet(),
    )
    assertTrue(
      SoundVibrations.BRAILLE_KEYBOARD.all { it in SoundVibrations.VIBRATION_ONLY.keys }
    )
  }

  @Test
  fun onlyVibrationsThatCanBeFeltCount() {
    assertEquals(setOf("focus", "control_button", "control_link"), feedback.felt())
  }
}
