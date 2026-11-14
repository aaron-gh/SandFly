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
import android.text.style.TtsSpan
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Report #366: punctuation names in TtsSpans weren't said by engines that ignore spans. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TtsSpanTextTest {
  private fun named(text: String, vararg names: Pair<Int, String>): SpannableStringBuilder =
    SpannableStringBuilder(text).apply {
      for ((at, name) in names) {
        setSpan(TtsSpan.TextBuilder(name).build(), at, at + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
      }
    }

  @Test
  fun punctuationNames_areWrittenIn() {
    val flat = TtsSpanText.flatten(named("Hi, you.", 2 to "comma", 7 to "period"))!!
    assertEquals("Hi comma  you period ", flat.text.toString())
    assertEquals(0, (flat.text as Spanned).getSpans(0, flat.text.length, TtsSpan::class.java).size)
  }

  @Test
  fun positions_mapBackToTheTextGiven() {
    val flat = TtsSpanText.flatten(named("Hi, you.", 2 to "comma", 7 to "period"))!!
    // "you" starts at 10 in "Hi comma  you period " and at 4 in "Hi, you.".
    assertEquals(0, flat.toOriginal(0))
    assertEquals(2, flat.toOriginal(4))
    assertEquals(4, flat.toOriginal(10))
    assertEquals(7, flat.toOriginal(15))
    assertEquals(8, flat.toOriginal(flat.text.length))
  }

  @Test
  fun otherSpans_stay() {
    val text = named("Hi, you", 2 to "comma")
    text.setSpan(LocaleSpan(Locale.FRENCH), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val flat = TtsSpanText.flatten(text)!!.text as Spanned
    val locale = flat.getSpans(0, flat.length, LocaleSpan::class.java).single()
    assertEquals("you", flat.subSequence(flat.getSpanStart(locale), flat.getSpanEnd(locale)).toString())
  }

  @Test
  fun noTextSpans_isNull() {
    assertNull(TtsSpanText.flatten("Hi, you."))
    val verbatim = SpannableStringBuilder("123")
    verbatim.setSpan(TtsSpan.VerbatimBuilder("123").build(), 0, 3, 0)
    assertNull(TtsSpanText.flatten(verbatim))
  }
}
