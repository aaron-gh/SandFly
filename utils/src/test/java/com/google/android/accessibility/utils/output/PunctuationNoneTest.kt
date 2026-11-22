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
import android.text.style.TtsSpan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Report #538: Speak punctuation and symbols > None. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PunctuationNoneTest {
  @After
  fun tearDown() {
    SpeechCleanupUtils.setPunctuationVerbosity(SpeechCleanupUtils.ALL)
  }

  /** The text an engine is given. */
  private fun spoken(text: CharSequence): String {
    val hidden = SpeechCleanupUtils.withPunctuationHidden(text) ?: return text.toString()
    return (TtsSpanText.flatten(hidden)?.text ?: hidden).toString()
  }

  @Test
  fun symbols_areSpacesForTheEngine() {
    assertEquals("a  b", spoken("a/b"))
    assertEquals("  50  ", spoken("(50)"))
    assertFalse(spoken("#1 * x @ y & z").contains(Regex("[#*@&]")))
    val symbols = "\"quoted\" [x] {y} <z> 5 + 3 = 8 50% \$5 ~ ^ _ | \\ \u00a9 \u2192"
    assertFalse(spoken(symbols).contains(Regex("[\"\\[\\]{}<>+=%$~^_|\\\\\u00a9\u2192]")))
  }

  @Test
  fun wordsStayApart() {
    val said = spoken("one/two")
    assertTrue(said.startsWith("one "))
    assertTrue(said.endsWith(" two"))
  }

  @Test
  fun textKeepsItsPunctuation() {
    // The text copied from speech is unchanged; only the TtsSpans say spaces.
    val hidden = SpeechCleanupUtils.withPunctuationHidden("a/b")!!
    assertEquals("a/b", hidden.toString())
    val spans = (hidden as Spanned).getSpans(0, hidden.length, TtsSpan::class.java)
    assertEquals(1, spans.size)
    assertEquals(1, hidden.getSpanStart(spans[0]))
  }

  @Test
  fun wordAndNumberJoiners_stay() {
    assertNull(SpeechCleanupUtils.withPunctuationHidden("don't e-mail 3.5 1,000 10:30 1/2"))
    assertNull(SpeechCleanupUtils.withPunctuationHidden("it’s"))
  }

  @Test
  fun punctuationEndingAWord_staysForThePause() {
    assertNull(SpeechCleanupUtils.withPunctuationHidden("Hello, world. Who? Yes! a; b: c…"))
    assertNull(SpeechCleanupUtils.withPunctuationHidden("你好。再见"))
  }

  @Test
  fun punctuationNotEndingAWord_isHidden() {
    assertEquals("a  b", spoken("a.b"))
    assertTrue(SpeechCleanupUtils.isHiddenAt("x .", 2))
    assertTrue(SpeechCleanupUtils.isHiddenAt("x ?y", 2))
  }

  @Test
  fun emojiAndLetters_stay() {
    assertNull(SpeechCleanupUtils.withPunctuationHidden("ok 😀 ☕ ❤️ #️⃣"))
    assertNull(SpeechCleanupUtils.withPunctuationHidden("π µ café بَ"))
  }

  @Test
  fun charactersSaidBySandFly_stay() {
    val text = SpannableStringBuilder("a, b")
    text.setSpan(TtsSpan.TextBuilder("comma").build(), 1, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    text.append(" (c)")
    val hidden = SpeechCleanupUtils.withPunctuationHidden(text)!!
    val spans = (hidden as Spanned).getSpans(0, hidden.length, TtsSpan::class.java)
    assertEquals(3, spans.size)
    assertEquals(1, spans.count { it.args.getString(TtsSpan.ARG_TEXT) == "comma" })
  }

  @Test
  fun singleCharacter_isSaid() {
    assertTrue(SpeechCleanupUtils.isSingleCharacter("/"))
    assertTrue(SpeechCleanupUtils.isSingleCharacter(" + "))
    assertTrue(SpeechCleanupUtils.isSingleCharacter("😀"))
    assertFalse(SpeechCleanupUtils.isSingleCharacter("a/"))
    assertFalse(SpeechCleanupUtils.isSingleCharacter(""))
  }

  @Test
  fun none_hidesEverySymbol() {
    SpeechCleanupUtils.setPunctuationVerbosity(SpeechCleanupUtils.ALL)
    assertFalse(SpeechCleanupUtils.isHiddenByVerbosity('@'))
    assertFalse(SpeechCleanupUtils.isHiddenByVerbosity('+'))
    SpeechCleanupUtils.setPunctuationVerbosity(SpeechCleanupUtils.NONE)
    assertTrue(SpeechCleanupUtils.isHiddenByVerbosity('@'))
    assertTrue(SpeechCleanupUtils.isHiddenByVerbosity('+'))
    assertTrue(SpeechCleanupUtils.isHiddenByVerbosity('.'))
    assertFalse(SpeechCleanupUtils.isHiddenByVerbosity(' '))
    assertFalse(SpeechCleanupUtils.isHiddenByVerbosity('a'))
  }
}
