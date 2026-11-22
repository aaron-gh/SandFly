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

package com.google.android.accessibility.utils.output

import android.text.SpannableStringBuilder
import android.text.Spanned
import java.util.Locale

/**
 * Marks text that is said in place of a capital letter, such as "H hotel" or "Hotel" for H, so that
 * the speech has the higher pitch or the sound that the user chose for capital letters, as a capital
 * letter alone does.
 */
class CapitalLetterSpan {
  companion object {
    /** Returns whether all of [text] is marked as standing for a capital letter. */
    @JvmStatic
    fun covers(text: CharSequence): Boolean =
      text is Spanned &&
        text.getSpans(0, text.length, CapitalLetterSpan::class.java).any {
          text.getSpanStart(it) == 0 && text.getSpanEnd(it) == text.length
        }

    /**
     * Returns whether [text] starts with a capital letter marked as one, as a keyboard key does
     * when more is said after it, such as "A, alpha".
     */
    @JvmStatic
    fun leads(text: CharSequence): Boolean =
      text is Spanned &&
        text.isNotEmpty() &&
        Character.isUpperCase(text[0]) &&
        text.getSpans(0, 1, CapitalLetterSpan::class.java).any { text.getSpanStart(it) == 0 }

    /**
     * Returns [text] with its capital letter in lower case, in [locale], when [text] is a capital
     * letter on its own, or starts with one marked as one followed by other than a letter, such as
     * "A, alpha". Returns null for other text. The spans of [text] are kept.
     */
    @JvmStatic
    fun withLoneCapitalLowercased(text: CharSequence, locale: Locale?): CharSequence? {
      if (text.isEmpty() || !Character.isUpperCase(text[0])) return null
      if (text.length > 1 && (Character.isLetter(text[1]) || !(covers(text) || leads(text)))) {
        return null
      }
      val letter = text[0].toString()
      val lower = letter.lowercase(locale ?: Locale.getDefault())
      // Such as İ, which is two characters in lower case outside Turkish.
      if (lower.length != 1 || lower == letter) return null
      val builder = SpannableStringBuilder(lower).append(text, 1, text.length)
      if (text is Spanned) {
        for (span in text.getSpans(0, text.length, Any::class.java)) {
          val start = text.getSpanStart(span)
          builder.setSpan(span, start, text.getSpanEnd(span), text.getSpanFlags(span))
        }
      }
      return builder
    }
  }
}
