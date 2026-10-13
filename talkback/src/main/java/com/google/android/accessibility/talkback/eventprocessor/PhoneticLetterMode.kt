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

package com.google.android.accessibility.talkback.eventprocessor

import android.content.SharedPreferences
import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.VerbosityPreferences

/**
 * How Backtalk says the phonetic word for a letter, such as "hotel" for h. This replaced an on/off
 * setting, whose on is [AFTER_LETTER_DELAY].
 */
object PhoneticLetterMode {
  /** Only the letter is said. */
  const val OFF = "off"

  /**
   * The letter, then its word after a pause: "h, hotel". Moving by character, the word comes after
   * a short wait, and moving on skips it.
   */
  const val AFTER_LETTER_DELAY = "after_letter_delay"

  /** The letter and its word together, with no pause: "h hotel". */
  const val AFTER_LETTER_NO_DELAY = "after_letter_no_delay"

  /** The word in place of the letter: "hotel". */
  const val INSTEAD_OF_LETTER = "instead_of_letter"

  /** Reads the mode for the current verbosity preset. */
  @JvmStatic
  fun read(prefs: SharedPreferences, res: Resources): String =
    VerbosityPreferences.getPreferenceValueString(
      prefs,
      res,
      res.getString(R.string.pref_phonetic_letters_mode_key),
      res.getString(R.string.pref_phonetic_letters_mode_default),
    ) ?: AFTER_LETTER_DELAY

  /**
   * Gives each copy of the old on/off setting the matching mode, unless that copy has a mode
   * already. There is a copy for each verbosity preset, and one with no preset from before presets.
   */
  @JvmStatic
  fun migrate(prefs: SharedPreferences, res: Resources) {
    val oldKey = res.getString(R.string.pref_phonetic_letters_key)
    val newKey = res.getString(R.string.pref_phonetic_letters_mode_key)
    val editor = prefs.edit()
    var changed = false
    fun keyFor(preset: String?, key: String) =
      if (preset == null) key else VerbosityPreferences.toVerbosityPrefKey(preset, key)
    for (preset in listOf(null) + res.getStringArray(R.array.pref_verbosity_preset_values)) {
      val from = keyFor(preset, oldKey)
      val to = keyFor(preset, newKey)
      if (prefs.contains(from) && !prefs.contains(to)) {
        editor.putString(to, if (prefs.getBoolean(from, true)) AFTER_LETTER_DELAY else OFF)
        changed = true
      }
    }
    if (changed) {
      editor.apply()
    }
  }
}
