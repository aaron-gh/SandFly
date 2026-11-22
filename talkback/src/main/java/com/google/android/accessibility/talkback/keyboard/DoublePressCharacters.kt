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

package com.google.android.accessibility.talkback.keyboard

import android.content.SharedPreferences
import java.util.Locale

/**
 * Double presses of the SandFly keyboard: a key pressed twice puts its double-press characters in
 * place of the character the first press typed, such as G G for ğ, and Space pressed twice after a
 * letter or digit types a period and a space, with Double space types a period.
 */
object DoublePressCharacters {
  const val KEY = "pref_sandfly_keyboard_double_press"

  const val MAX_ENTRIES = 200

  /** The longest text that a key's double press types. */
  const val MAX_LENGTH = 16

  /** How soon after a press the second one makes it a double press. */
  const val TWICE_MS = KeyPresses.TWICE_MS

  /** The characters that the double press of [key], a lowercase character, types. */
  data class Entry(val key: Char, val characters: String)

  fun read(prefs: SharedPreferences): List<Entry> = parse(prefs.getString(KEY, null))

  fun write(prefs: SharedPreferences, entries: List<Entry>) {
    prefs.edit().putString(KEY, format(entries)).apply()
  }

  /** Reads entries saved as lines of the key, a tab and its characters. */
  fun parse(text: String?): List<Entry> {
    if (text.isNullOrEmpty()) return emptyList()
    val entries = LinkedHashMap<Char, Entry>()
    for (line in text.lineSequence()) {
      if (entries.size >= MAX_ENTRIES) break
      val tab = line.indexOf('\t')
      if (tab != 1) continue
      val key = normalizeKey(line.substring(0, 1)) ?: continue
      val characters = line.substring(2)
      if (!isValidCharacters(characters)) continue
      entries[key] = Entry(key, characters)
    }
    return entries.values.toList()
  }

  fun format(entries: List<Entry>): String =
    entries.take(MAX_ENTRIES).joinToString("\n") { it.key + "\t" + it.characters }

  /** The key of [text], one character that isn't a space, in lowercase, or null. */
  fun normalizeKey(text: String): Char? {
    if (text.length != 1) return null
    val key = text[0]
    if (Character.isWhitespace(key) || Character.isISOControl(key) || Character.isSurrogate(key)) {
      return null
    }
    return key.lowercaseChar()
  }

  /** Whether [characters] can be typed by a double press: not empty, short, no control characters. */
  fun isValidCharacters(characters: String): Boolean =
    characters.isNotEmpty() &&
      characters.length <= MAX_LENGTH &&
      characters.none { Character.isISOControl(it) }

  /** Whether a second press of the key that typed [typed] can do something. */
  fun applies(typed: Char, entries: Map<Char, String>, doubleSpacePeriod: Boolean): Boolean =
    if (typed == ' ') doubleSpacePeriod else typed.lowercaseChar() in entries

  /**
   * What a second press of the key whose first press typed [typed] puts in place of that
   * character, with [before] the text before the cursor, or null when it types as a single press.
   * A capital [typed] gives the capital of the key's characters.
   */
  fun replacement(
    typed: Char,
    before: CharSequence?,
    entries: Map<Char, String>,
    doubleSpacePeriod: Boolean,
    locale: Locale = Locale.getDefault(),
  ): String? {
    if (before.isNullOrEmpty() || before[before.length - 1] != typed) return null
    if (typed == ' ') {
      if (!doubleSpacePeriod || before.length < 2) return null
      // Only after a word, as on phone keyboards, so that a third press types a space.
      return if (Character.isLetterOrDigit(before[before.length - 2])) ". " else null
    }
    val characters = entries[typed.lowercaseChar()] ?: return null
    return if (Character.isUpperCase(typed)) characters.uppercase(locale) else characters
  }
}
