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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.VerbosityPreferences
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.input.TextEventFilter
import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput

/**
 * The keyboard echo settings, one for each [TypingInput]. Each holds a
 * [TextEventFilter.KeyboardEchoType] as a string.
 */
object KeyboardEchoSettings {
  // Where the braille keyboard kept its own echo setting, in its own settings file.
  private const val OLD_BRAILLE_PREFS_FILE = "braille_keyboard"
  private const val OLD_BRAILLE_KEYBOARD_ECHO_KEY = "pref_brailleime_keyboard_echo"

  /** Returns the key of the echo setting for [input]. */
  @JvmStatic
  fun key(res: Resources, input: TypingInput): String =
    res.getString(
      when (input) {
        TypingInput.ON_SCREEN_KEYBOARD -> R.string.pref_echo_on_screen_keyboard_key
        TypingInput.PHYSICAL_KEYBOARD -> R.string.pref_echo_physical_keyboard_key
        TypingInput.BRAILLE_KEYBOARD -> R.string.pref_echo_braille_keyboard_key
        TypingInput.BRAILLE_DISPLAY -> R.string.pref_echo_braille_display_key
      }
    )

  /** Reads the echo for [input], a [TextEventFilter.KeyboardEchoType]. */
  @JvmStatic
  @TextEventFilter.KeyboardEchoType
  fun read(prefs: SharedPreferences, res: Resources, input: TypingInput): Int =
    prefs.getString(key(res, input), null)?.toIntOrNull()
      ?: res.getString(R.string.pref_keyboard_echo_default).toInt()

  /** Returns the name of the echo setting for [input], such as "Physical keyboard echo". */
  @JvmStatic
  fun title(res: Resources, input: TypingInput): String =
    res.getString(
      when (input) {
        TypingInput.ON_SCREEN_KEYBOARD -> R.string.title_keyboard_echo_on_screen
        TypingInput.PHYSICAL_KEYBOARD -> R.string.title_keyboard_echo_physical
        TypingInput.BRAILLE_KEYBOARD -> R.string.title_keyboard_echo_braille_keyboard
        TypingInput.BRAILLE_DISPLAY -> R.string.title_keyboard_echo_braille_display
      }
    )

  /** Returns the echo choices, as [TextEventFilter.KeyboardEchoType] values, in settings order. */
  @JvmStatic
  fun choices(res: Resources): IntArray =
    res.getStringArray(R.array.pref_keyboard_echo_values).map { it.toInt() }.toIntArray()

  /** Returns the name of [echo], a [TextEventFilter.KeyboardEchoType], such as "Words". */
  @JvmStatic
  fun choiceName(res: Resources, @TextEventFilter.KeyboardEchoType echo: Int): String {
    val index = choices(res).indexOf(echo).coerceAtLeast(0)
    return res.getStringArray(R.array.pref_keyboard_echo_entries)[index]
  }

  /** Returns the setting's name and [echo]'s name, such as "Physical keyboard echo: Words". */
  @JvmStatic
  fun state(
    res: Resources,
    input: TypingInput,
    @TextEventFilter.KeyboardEchoType echo: Int,
  ): String =
    res.getString(R.string.template_keyboard_echo_state, title(res, input), choiceName(res, echo))

  /** Sets the echo for [input] to [echo], a [TextEventFilter.KeyboardEchoType]. */
  @JvmStatic
  fun write(
    prefs: SharedPreferences,
    res: Resources,
    input: TypingInput,
    @TextEventFilter.KeyboardEchoType echo: Int,
  ) {
    prefs.edit().putString(key(res, input), echo.toString()).apply()
  }

  /**
   * Moves the echo for [input] to the next or previous choice, wrapping around, and returns the
   * new echo.
   */
  @JvmStatic
  @TextEventFilter.KeyboardEchoType
  fun cycle(prefs: SharedPreferences, res: Resources, input: TypingInput, next: Boolean): Int {
    val choices = choices(res)
    val index = choices.indexOf(read(prefs, res, input)).coerceAtLeast(0)
    val echo = choices[(index + if (next) 1 else choices.size - 1) % choices.size]
    write(prefs, res, input, echo)
    return echo
  }

  /** Returns whether [echo], a [TextEventFilter.KeyboardEchoType], speaks each character typed. */
  @JvmStatic
  fun echoesCharacters(@TextEventFilter.KeyboardEchoType echo: Int): Boolean =
    echo == TextEventFilter.PREF_ECHO_CHARACTERS ||
      echo == TextEventFilter.PREF_ECHO_CHARACTERS_AND_WORDS

  /**
   * Gives each echo setting that has no value the echo the user had before: the on-screen and
   * physical keyboard echo were verbosity preset settings, the braille keyboard had its own, which
   * started as the on-screen keyboard's, and a braille display spoke characters as the physical
   * keyboard echo said.
   */
  @JvmStatic
  fun migrate(context: Context, prefs: SharedPreferences) {
    val res = context.resources
    val default = res.getString(R.string.pref_keyboard_echo_default)
    fun presetValue(keyResId: Int): String =
      VerbosityPreferences.getPreferenceValueString(prefs, res, res.getString(keyResId), default)
        ?: default
    val editor = prefs.edit()
    var changed = false
    fun moveIfUnset(input: TypingInput, value: () -> String) {
      val key = key(res, input)
      if (!prefs.contains(key)) {
        editor.putString(key, value())
        changed = true
      }
    }
    val onScreen = { presetValue(R.string.pref_keyboard_echo_on_screen_key) }
    val physical = { presetValue(R.string.pref_keyboard_echo_physical_key) }
    moveIfUnset(TypingInput.ON_SCREEN_KEYBOARD, onScreen)
    moveIfUnset(TypingInput.PHYSICAL_KEYBOARD, physical)
    moveIfUnset(TypingInput.BRAILLE_KEYBOARD) {
      SharedPreferencesUtils.getSharedPreferences(context, OLD_BRAILLE_PREFS_FILE)
        .getString(OLD_BRAILLE_KEYBOARD_ECHO_KEY, null) ?: onScreen()
    }
    moveIfUnset(TypingInput.BRAILLE_DISPLAY, physical)
    if (changed) {
      editor.apply()
    }
  }
}
