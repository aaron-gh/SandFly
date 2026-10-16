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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundThemesTest {
  @Test
  fun soundsAreNamedAfterWhatTheyReplace() {
    assertEquals("focus" to "wav", SoundThemes.parseSoundName("focus.wav"))
    assertEquals("control_button" to "ogg", SoundThemes.parseSoundName("Control_Button.OGG"))
    assertNull(SoundThemes.parseSoundName("COPYING"))
    assertNull(SoundThemes.parseSoundName(".focus.wav"))
    assertNull(SoundThemes.parseSoundName("notes.txt"))
  }

  @Test
  fun everySoundCanBeNamedInATheme() {
    for (item in SoundThemes.SOUNDS) {
      assertFalse(item.key, item.key.contains('.'))
      assertEquals(item.key to "wav", SoundThemes.parseSoundName("${item.key}.wav"))
    }
  }

  @Test
  fun theBrailleKeyboardTypingSoundsCanBeReplaced() {
    val typing =
      listOf(
        "braille_keyboard_character",
        "braille_keyboard_space",
        "braille_keyboard_delete",
        "braille_keyboard_new_line",
      )
    assertEquals(typing, SoundThemes.BRAILLE_TYPING_EFFECTS.keys.toList())
    assertTrue(SoundThemes.SOUND_KEYS.containsAll(typing))
    val paths = mapOf("braille_keyboard_space" to "/a.wav", "focus" to "/b.wav")
    assertEquals(mapOf("braille_keyboard_space" to "/a.wav"), SoundThemes.brailleTypingSounds(paths))
    // They are not Backtalk's own sounds, so they have no switches of their own.
    assertTrue(IndividualFeedbackSettings.SOUNDS.none { it.key in typing })
  }

  @Test
  fun storedFilesKeepTheirSound() {
    val name = SoundThemes.fileName("control_button", "ogg", 1234L)
    assertEquals("control_button", SoundThemes.keyOfFile(name))
    assertNull(SoundThemes.keyOfFile("control_button.ogg"))
    assertNull(SoundThemes.keyOfFile("theme.json"))
  }

  @Test
  fun licensesAndReadmesTravelWithTheTheme() {
    assertTrue(SoundThemes.isDocument("COPYING.txt"))
    assertTrue(SoundThemes.isDocument("LICENSE"))
    assertTrue(SoundThemes.isDocument("readme.md"))
    assertFalse(SoundThemes.isDocument("focus.wav"))
    assertFalse(SoundThemes.isDocument("notes.txt"))
  }

  @Test
  fun themesAreFiledByName() {
    assertEquals("unspoken", SoundThemes.idFor("Unspoken"))
    assertEquals("my-sounds-2", SoundThemes.idFor("  My Sounds #2! "))
    assertEquals("theme", SoundThemes.idFor("!!!"))
    // Never Backtalk's own.
    assertEquals("backtalk-theme", SoundThemes.idFor("Backtalk"))
  }

  @Test
  fun extensionsOfKnownFormatsOnly() {
    assertEquals("wav", SoundThemes.extensionOf("Click.WAV"))
    assertNull(SoundThemes.extensionOf("notes.txt"))
    assertNull(SoundThemes.extensionOf(null))
  }
}
