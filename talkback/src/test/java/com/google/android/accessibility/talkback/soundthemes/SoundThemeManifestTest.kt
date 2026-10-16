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
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundThemeManifestTest {
  private val keys =
    SoundVibrations.themeNames(IndividualFeedbackSettings.SOUNDS.map { it.key })

  private fun parse(json: String) = SoundThemeManifest.parse(json, keys, "fallback")

  @Test
  fun readsWhoMadeItAndItsSettings() {
    val manifest =
      parse(
        """
        {"name": "Unspoken", "author": "Bryan Smart", "description": "Sounds for controls",
         "license": "GPL-2.0", "website": "https://example.com",
         "settings": {"control_sounds": true, "3d_audio": "always"}}
        """
      )
    assertEquals("Unspoken", manifest.name)
    assertEquals("Bryan Smart", manifest.author)
    assertEquals("GPL-2.0", manifest.license)
    assertEquals(true, manifest.controlSounds)
    assertEquals("always", manifest.audio3d)
    assertTrue(manifest.warnings.isEmpty())
  }

  @Test
  fun missingSettingsAreLeftToTheDefaults() {
    val manifest = parse("{}")
    assertEquals("fallback", manifest.name)
    assertNull(manifest.controlSounds)
    assertNull(manifest.audio3d)
  }

  @Test
  fun badSettingsAreWarnedAboutAndLeftOut() {
    val manifest = parse("""{"settings": {"control_sounds": "yes", "3d_audio": "loud"}}""")
    assertNull(manifest.controlSounds)
    assertNull(manifest.audio3d)
    assertEquals(2, manifest.warnings.size)
  }

  @Test
  fun aVibrationBecomesAllThreeFormsOfAPattern() {
    val pattern =
      SoundThemeManifest.toPatternOrThrow(
        JSONObject(
          """{"pattern": [0, 15], "strength": [[12, 110]], "effects": [["tick", 140, 0]]}"""
        )
      )
    assertArrayEquals(intArrayOf(0, 15, -9998, 12, 110, -9999, 7, 140, 0), pattern)
  }

  @Test
  fun strengthAloneGivesThePatternForSimpleVibrators() {
    val pattern =
      SoundThemeManifest.toPatternOrThrow(
        JSONObject("""{"strength": [[10, 0], [20, 200], [5, 100], [30, 0], [10, 255]]}""")
      )
    assertArrayEquals(
      intArrayOf(10, 25, 30, 10, -9998, 10, 0, 20, 200, 5, 100, 30, 0, 10, 255),
      pattern,
    )
  }

  @Test
  fun noneIsAnEmptyPattern() {
    assertEquals(0, SoundThemeManifest.toPatternOrThrow("none").size)
  }

  @Test
  fun badVibrationsAreRefused() {
    for (bad in
      listOf(
        """{"effects": [["click", 255, 0]]}""",
        """{"pattern": []}""",
        """{"pattern": [0, 9000]}""",
        """{"pattern": [0, -5]}""",
        """{"pattern": [0, 1.5]}""",
        """{"pattern": [0, 20], "strength": [[20, 300]]}""",
        """{"pattern": [0, 20], "effects": [["buzz", 100, 0]]}""",
        """{"pattern": [0, 20], "effects": [["click", 100]]}""",
        // Each form may last at most 5 s in all, not 5 s a step.
        """{"pattern": [0, 20], "strength": [[5000, 100], [5000, 100]]}""",
        """{"pattern": [0, 20], "effects": [["click", 100, 5000], ["click", 100, 5000]]}""",
      )) {
      assertThrows(bad, IllegalArgumentException::class.java) {
        SoundThemeManifest.toPatternOrThrow(JSONObject(bad))
      }
    }
  }

  @Test
  fun vibrationsForUnknownSoundsOrBadOnesAreWarnedAbout() {
    val manifest =
      parse(
        """
        {"vibrations": {"focus": {"pattern": [0, 20]}, "beep": {"pattern": [0, 20]},
                        "tick": {"pattern": []}, "control_button": "none"}}
        """
      )
    assertEquals(setOf("focus", "control_button"), manifest.vibrations.keys)
    assertEquals(2, manifest.warnings.size)
    val patterns = manifest.vibrationPatterns()
    assertArrayEquals(intArrayOf(0, 20), patterns["focus"])
    assertEquals(0, patterns.getValue("control_button").size)
  }

  @Test
  fun savingAndReadingAgainKeepsEverything() {
    val manifest =
      parse(
        """
        {"name": "Mine", "author": "Me", "settings": {"control_sounds": false, "3d_audio": "never"},
         "vibrations": {"focus": {"pattern": [0, 20], "effects": [["click", 200, 0]]},
                        "tick": "none"}}
        """
      )
    val again = parse(manifest.toJson())
    assertEquals(manifest.name, again.name)
    assertEquals(manifest.author, again.author)
    assertEquals(false, again.controlSounds)
    assertEquals("never", again.audio3d)
    assertEquals(manifest.vibrations.keys, again.vibrations.keys)
    assertArrayEquals(
      manifest.vibrationPatterns().getValue("focus"),
      again.vibrationPatterns().getValue("focus"),
    )
  }

  @Test
  fun vibrationsWithoutASoundCanBeReplacedButNotBrailleDisplaySounds() {
    val manifest =
      parse(
        """
        {"vibrations": {"announcement": {"pattern": [0, 20]},
                        "braille_keyboard_character": {"pattern": [0, 10]},
                        "direct_touch_on": "none",
                        "display_connected": {"pattern": [0, 20]}}}
        """
      )
    assertEquals(
      setOf("announcement", "braille_keyboard_character", "direct_touch_on"),
      manifest.vibrations.keys,
    )
    assertEquals(1, manifest.warnings.size)
  }

  @Test
  fun notJsonIsRefused() {
    assertThrows(org.json.JSONException::class.java) { parse("not json") }
  }
}
