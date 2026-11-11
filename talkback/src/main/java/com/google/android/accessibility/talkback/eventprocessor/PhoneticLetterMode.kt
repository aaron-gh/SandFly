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
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.VerbosityPreferences

/**
 * How Backtalk says the phonetic word for a letter, such as "hotel" for h. There is a setting for
 * letters reviewed (moved over by character or spelled) and one for letters typed or deleted and
 * keyboard keys. Both replaced one setting for all letters, which replaced an on/off setting, whose
 * on is [AFTER_LETTER_DELAY].
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

  /** Reads the mode for letters reviewed. */
  @JvmStatic
  fun readReview(prefs: SharedPreferences, res: Resources): String =
    read(
      prefs,
      res,
      R.string.pref_phonetic_letters_review_mode_key,
      R.string.pref_phonetic_letters_review_mode_default,
    )

  /** Reads the mode for letters typed or deleted and keyboard keys. */
  @JvmStatic
  fun readTyping(prefs: SharedPreferences, res: Resources): String =
    read(
      prefs,
      res,
      R.string.pref_phonetic_letters_typing_mode_key,
      R.string.pref_phonetic_letters_typing_mode_default,
    )

  /**
   * Reads the mode under [keyId], or the one setting for all letters while [split] hasn't copied it,
   * as after importing settings from before the split.
   */
  private fun read(
    prefs: SharedPreferences,
    res: Resources,
    @StringRes keyId: Int,
    @StringRes defaultId: Int,
  ): String {
    val values = res.getStringArray(R.array.pref_phonetic_letters_mode_values)
    return prefs.getString(res.getString(keyId), null)?.takeIf { it in values }
      ?: prefs
        .getString(res.getString(R.string.pref_phonetic_letters_mode_key), null)
        ?.takeIf { it in values }
      ?: res.getString(defaultId)
  }

  /**
   * Gives the settings for letters reviewed and letters typed the value of the one setting for all
   * letters that they replaced, unless they have a value. Runs after [migrate] and
   * [VerbosityPreferences.removePresets], which put the old values into that setting.
   */
  @JvmStatic
  fun split(prefs: SharedPreferences, res: Resources) {
    val old =
      prefs
        .getString(res.getString(R.string.pref_phonetic_letters_mode_key), null)
        ?.takeIf { it in res.getStringArray(R.array.pref_phonetic_letters_mode_values) }
        ?: return
    val editor = prefs.edit()
    var changed = false
    for (keyId in
      listOf(
        R.string.pref_phonetic_letters_review_mode_key,
        R.string.pref_phonetic_letters_typing_mode_key,
      )) {
      val key = res.getString(keyId)
      if (!prefs.contains(key)) {
        editor.putString(key, old)
        changed = true
      }
    }
    if (changed) {
      editor.apply()
    }
  }

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
