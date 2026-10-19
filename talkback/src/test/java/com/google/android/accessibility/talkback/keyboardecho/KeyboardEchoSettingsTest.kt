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

package com.google.android.accessibility.talkback.keyboardecho

import android.app.Application
import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.utils.VerbosityPreferences
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_CHARACTERS
import com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_CHARACTERS_AND_WORDS
import com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_NONE
import com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_WORDS
import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardEchoSettingsTest {
  private val prefs = FakeSharedPreferences()
  private lateinit var app: Application
  private lateinit var res: Resources
  private lateinit var custom: String

  @Before
  fun setUp() {
    app = RuntimeEnvironment.getApplication()
    res = app.resources
    custom = res.getString(R.string.pref_verbosity_preset_value_custom)
  }

  private fun setPreset(preset: String) {
    prefs.edit().putString(res.getString(R.string.pref_verbosity_preset_key), preset).apply()
  }

  private fun setCustomEcho(keyResId: Int, echo: Int) {
    prefs
      .edit()
      .putString(
        VerbosityPreferences.toVerbosityPrefKey(custom, res.getString(keyResId)),
        echo.toString(),
      )
      .apply()
  }

  private fun read(input: TypingInput) = KeyboardEchoSettings.read(prefs, res, input)

  @Test
  fun defaultIsCharactersAndWords() {
    for (input in TypingInput.values()) {
      assertEquals(PREF_ECHO_CHARACTERS_AND_WORDS, read(input))
    }
  }

  @Test
  fun migrateMovesTheCustomPresetEcho() {
    setPreset(custom)
    setCustomEcho(R.string.pref_keyboard_echo_on_screen_key, PREF_ECHO_WORDS)
    setCustomEcho(R.string.pref_keyboard_echo_physical_key, PREF_ECHO_NONE)

    KeyboardEchoSettings.migrate(app, prefs)

    assertEquals(PREF_ECHO_WORDS, read(TypingInput.ON_SCREEN_KEYBOARD))
    assertEquals(PREF_ECHO_NONE, read(TypingInput.PHYSICAL_KEYBOARD))
    // The braille keyboard started from the on-screen keyboard's echo, and a braille display spoke
    // characters as the physical keyboard echo said.
    assertEquals(PREF_ECHO_WORDS, read(TypingInput.BRAILLE_KEYBOARD))
    assertEquals(PREF_ECHO_NONE, read(TypingInput.BRAILLE_DISPLAY))
  }

  @Test
  fun migrateMovesTheLowPresetEcho() {
    setPreset(res.getString(R.string.pref_verbosity_preset_value_low))

    KeyboardEchoSettings.migrate(app, prefs)

    for (input in TypingInput.values()) {
      assertEquals(PREF_ECHO_NONE, read(input))
    }
  }

  @Test
  fun migrateMovesTheBrailleKeyboardsOwnEcho() {
    SharedPreferencesUtils.getSharedPreferences(app, "braille_keyboard")
      .edit()
      .putString("pref_brailleime_keyboard_echo", PREF_ECHO_CHARACTERS.toString())
      .commit()

    KeyboardEchoSettings.migrate(app, prefs)

    assertEquals(PREF_ECHO_CHARACTERS, read(TypingInput.BRAILLE_KEYBOARD))
  }

  @Test
  fun migrateKeepsEchoAlreadySet() {
    prefs
      .edit()
      .putString(
        KeyboardEchoSettings.key(res, TypingInput.PHYSICAL_KEYBOARD),
        PREF_ECHO_WORDS.toString(),
      )
      .apply()
    setPreset(res.getString(R.string.pref_verbosity_preset_value_low))

    KeyboardEchoSettings.migrate(app, prefs)

    assertEquals(PREF_ECHO_WORDS, read(TypingInput.PHYSICAL_KEYBOARD))
    assertEquals(PREF_ECHO_NONE, read(TypingInput.ON_SCREEN_KEYBOARD))
  }

  @Test
  fun cycleWrapsAroundTheChoicesInSettingsOrder() {
    // The choices are None, Characters, Words, Characters and words.
    assertEquals(
      PREF_ECHO_NONE,
      KeyboardEchoSettings.cycle(prefs, res, TypingInput.BRAILLE_DISPLAY, /* next= */ true),
    )
    assertEquals(
      PREF_ECHO_CHARACTERS,
      KeyboardEchoSettings.cycle(prefs, res, TypingInput.BRAILLE_DISPLAY, /* next= */ true),
    )
    assertEquals(
      PREF_ECHO_NONE,
      KeyboardEchoSettings.cycle(prefs, res, TypingInput.BRAILLE_DISPLAY, /* next= */ false),
    )
    assertEquals(
      PREF_ECHO_CHARACTERS_AND_WORDS,
      KeyboardEchoSettings.cycle(prefs, res, TypingInput.BRAILLE_DISPLAY, /* next= */ false),
    )
    assertEquals(PREF_ECHO_CHARACTERS_AND_WORDS, read(TypingInput.BRAILLE_DISPLAY))
    assertEquals(PREF_ECHO_CHARACTERS_AND_WORDS, read(TypingInput.ON_SCREEN_KEYBOARD))
  }

  @Test
  fun stateNamesTheSettingAndItsValue() {
    assertEquals(
      "Physical keyboard echo: Words",
      KeyboardEchoSettings.state(res, TypingInput.PHYSICAL_KEYBOARD, PREF_ECHO_WORDS),
    )
  }
}
