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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.braille.brailledisplay.controller.BrailleTextFilter

/**
 * Braille > Braille dictionary: text that a braille display shows differently, such as a symbol as
 * a word, kept apart from the speech dictionary, whose entries are for how text sounds. Entries
 * apply as the speech dictionary's do: in order, and those of one app only in that app.
 */
object BrailleDictionary {
  const val KEY = "pref_braille_dictionary"

  @Volatile private var entries: List<DictionaryEntry> = emptyList()

  /** Reads the entries from [prefs], and applies them to braille output while there are any. */
  @JvmStatic
  fun load(context: Context, prefs: SharedPreferences) {
    entries = read(prefs)
    BrailleTextFilter.set(
      if (entries.isEmpty()) null
      else BrailleTextFilter.Filter { SpeechDictionary.applyInCurrentApp(it, entries) }
    )
  }

  fun read(prefs: SharedPreferences): List<DictionaryEntry> =
    SpeechDictionary.fromJson(prefs.getString(KEY, null))

  fun write(prefs: SharedPreferences, list: List<DictionaryEntry>) {
    prefs.edit().putString(KEY, SpeechDictionary.toJson(list)).apply()
  }
}
