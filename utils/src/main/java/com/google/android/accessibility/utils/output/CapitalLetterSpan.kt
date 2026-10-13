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

import android.text.Spanned

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
  }
}
