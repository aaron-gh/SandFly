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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.SharedPreferences
import android.text.SpannableStringBuilder

/**
 * Symbol dictionary: symbols and symbol sequences replaced in speech and on a braille display. An
 * entry matches its exact text anywhere, with no whole-word rule or regular expression, so a period
 * directly after a word matches. Entries apply in order, each to the text the one before it left,
 * and after the speech dictionary or braille dictionary. Those of one app apply only in that app.
 */
object SymbolDictionary {
  const val KEY = "pref_symbol_dictionary"

  const val MAX_ENTRIES = 500

  /** An entry that would make the text longer than this is skipped. */
  internal const val MAX_RESULT_LENGTH = 100_000

  @Volatile private var entries: List<DictionaryEntry> = emptyList()

  val isEmpty: Boolean
    get() = entries.isEmpty()

  /** Reads the entries from [prefs]. Call before the speech and braille dictionaries load. */
  @JvmStatic
  fun load(prefs: SharedPreferences) {
    entries = read(prefs)
  }

  fun read(prefs: SharedPreferences): List<DictionaryEntry> =
    SpeechDictionary.fromJson(prefs.getString(KEY, null)).take(MAX_ENTRIES)

  fun write(prefs: SharedPreferences, list: List<DictionaryEntry>) {
    prefs.edit().putString(KEY, SpeechDictionary.toJson(list.take(MAX_ENTRIES))).apply()
  }

  /** Returns [text] with the entries applied for [app], or null when none changed it. */
  fun apply(text: CharSequence, app: String): CharSequence? = apply(text, entries, app)

  internal fun apply(text: CharSequence, entries: List<DictionaryEntry>, app: String): CharSequence? {
    if (entries.isEmpty()) return null
    var builder: SpannableStringBuilder? = null
    var subject = text.toString()
    for (entry in entries) {
      if (entry.app.isNotEmpty() && entry.app != app) continue
      val find = entry.replacement.find
      val replace = entry.replacement.replace
      if (find.isEmpty()) continue
      val starts = ArrayList<Int>()
      var index = subject.indexOf(find)
      while (index >= 0) {
        starts += index
        index = subject.indexOf(find, index + find.length)
      }
      if (starts.isEmpty()) continue
      val resultLength = subject.length + starts.size.toLong() * (replace.length - find.length)
      if (resultLength > maxOf(subject.length, MAX_RESULT_LENGTH)) continue
      val target = builder ?: SpannableStringBuilder(text).also { builder = it }
      // From the end, so that earlier positions stay where they were.
      for (start in starts.asReversed()) {
        target.replace(start, start + find.length, replace)
      }
      subject = target.toString()
    }
    return builder
  }
}
