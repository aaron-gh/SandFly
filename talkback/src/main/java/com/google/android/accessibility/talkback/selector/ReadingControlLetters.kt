/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.selector

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.talkback.selector.SelectorController.Setting

/**
 * The letter written with "Write the letter of a reading control" that chooses each reading
 * control. Navigation controls have one by default, mostly NVDA's quick navigation keys: H for
 * Headings, K for Links, L for Lines and A for Landmarks. The user can change or clear each one.
 */
object ReadingControlLetters {
  val DEFAULTS: Map<Setting, Char> =
    mapOf(
      Setting.GRANULARITY_HEADINGS to 'H',
      Setting.GRANULARITY_LINKS to 'K',
      Setting.GRANULARITY_LINES to 'L',
      Setting.GRANULARITY_WEB_LANDMARKS to 'A',
      Setting.GRANULARITY_WORDS to 'W',
      Setting.GRANULARITY_CHARACTERS to 'C',
      Setting.GRANULARITY_PARAGRAPHS to 'P',
      Setting.GRANULARITY_CONTROLS to 'O',
      Setting.GRANULARITY_WEB_BUTTONS to 'B',
      Setting.GRANULARITY_WEB_CHECKBOXES to 'X',
      Setting.GRANULARITY_WEB_EDITFIELDS to 'E',
      Setting.GRANULARITY_WEB_GRAPHICS to 'G',
      Setting.GRANULARITY_WEB_LISTITEMS to 'I',
      Setting.GRANULARITY_WEB_TABLES to 'T',
      Setting.GRANULARITY_WEB_RADIOS to 'R',
      Setting.GRANULARITY_WEB_VISITED_LINKS to 'V',
      Setting.GRANULARITY_WEB_UNVISITED_LINKS to 'U',
      Setting.SPEECH_RATE to 'S',
    )

  /** Kept as text: a letter, or empty for none. Absent means the default. */
  fun prefKey(setting: Setting): String = "pref_reading_control_letter_${setting.name}"

  /** The letter of [setting], in upper case, or null when it has none. */
  @JvmStatic
  fun letterOf(prefs: SharedPreferences, setting: Setting): Char? {
    val chosen = prefs.getString(prefKey(setting), null) ?: return DEFAULTS[setting]
    return chosen.firstOrNull()?.uppercaseChar()
  }

  /** Gives [setting] the letter [letter], or none for null. */
  fun setLetter(prefs: SharedPreferences, setting: Setting, letter: Char?) {
    prefs.edit {
      if (letter == DEFAULTS[setting]) {
        remove(prefKey(setting))
      } else {
        putString(prefKey(setting), letter?.uppercaseChar()?.toString().orEmpty())
      }
    }
  }
}
