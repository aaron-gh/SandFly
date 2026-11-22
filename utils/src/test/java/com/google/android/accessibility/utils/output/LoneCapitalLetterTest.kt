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

package com.google.android.accessibility.utils.output

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LocaleSpan
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Report #549: engines said "capital" for a capital letter with any Capital letters choice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoneCapitalLetterTest {
  private val russian = Locale.forLanguageTag("ru-RU")

  private fun lowered(text: CharSequence, locale: Locale? = russian): String? =
    CapitalLetterSpan.withLoneCapitalLowercased(text, locale)?.toString()

  private fun marked(text: String, end: Int): SpannableStringBuilder =
    SpannableStringBuilder(text).apply {
      setSpan(CapitalLetterSpan(), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

  @Test
  fun loneCapital_isLowerCase() {
    assertEquals("з", lowered("З"))
    assertEquals("a", lowered("A", Locale.US))
    assertEquals("ı", lowered("I", Locale.forLanguageTag("tr-TR")))
  }

  @Test
  fun markedCapitalBeforeMore_isLowerCase() {
    assertEquals("a, alpha", lowered(marked("A, alpha", 1), Locale.US))
    assertEquals("h hotel", lowered(marked("H hotel", 7), Locale.US))
  }

  @Test
  fun otherText_isLeft() {
    assertNull(lowered("Заглавная"))
    assertNull(lowered("A, alpha", Locale.US))
    assertNull(lowered(marked("Hotel", 5), Locale.US))
    assertNull(lowered("а"))
    assertNull(lowered("5"))
    assertNull(lowered(""))
  }

  @Test
  fun spans_areKept() {
    val text = SpannableStringBuilder("Ж")
    val language = LocaleSpan(russian)
    text.setSpan(language, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val result = CapitalLetterSpan.withLoneCapitalLowercased(text, russian) as Spanned
    assertEquals("ж", result.toString())
    assertEquals(0, result.getSpanStart(language))
    assertEquals(1, result.getSpanEnd(language))
  }
}
