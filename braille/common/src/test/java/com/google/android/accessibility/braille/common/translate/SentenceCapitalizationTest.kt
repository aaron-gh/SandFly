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

package com.google.android.accessibility.braille.common.translate

import android.text.InputType
import com.google.android.accessibility.braille.common.translate.SentenceCapitalization.capitalizeFirstLetter
import com.google.android.accessibility.braille.common.translate.SentenceCapitalization.fieldAllowsCapitalization
import com.google.android.accessibility.braille.common.translate.SentenceCapitalization.isSentenceStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceCapitalizationTest {

  @Test
  fun emptyField_startsSentence() {
    assertTrue(isSentenceStart(""))
    assertTrue(isSentenceStart("   "))
  }

  @Test
  fun unknownText_doesNotStartSentence() {
    assertFalse(isSentenceStart(null))
  }

  @Test
  fun afterSentenceEndAndSpace_startsSentence() {
    assertTrue(isSentenceStart("Hello. "))
    assertTrue(isSentenceStart("Really? "))
    assertTrue(isSentenceStart("Stop! "))
    assertTrue(isSentenceStart("Wait\u2026 "))
    assertTrue(isSentenceStart("Done.  "))
    assertTrue(isSentenceStart("Done.\u00A0"))
  }

  @Test
  fun afterFullWidthAndOtherScriptSentenceEnds_startsSentence() {
    assertTrue(isSentenceStart("\u3002 "))
    assertTrue(isSentenceStart("\uFF1F "))
    assertTrue(isSentenceStart("\uFF01 "))
    assertTrue(isSentenceStart("\u061F "))
    assertTrue(isSentenceStart("\u0964 "))
  }

  @Test
  fun afterClosingQuoteOrBracket_startsSentence() {
    assertTrue(isSentenceStart("He said \"Go.\" "))
    assertTrue(isSentenceStart("(See above.) "))
    assertTrue(isSentenceStart("\u201CYes!\u201D "))
  }

  @Test
  fun afterLineBreak_startsSentence() {
    assertTrue(isSentenceStart("Dear Sam,\n"))
    assertTrue(isSentenceStart("Dear Sam,\n  "))
    assertTrue(isSentenceStart("line\r\n"))
  }

  @Test
  fun midSentence_doesNotStartSentence() {
    assertFalse(isSentenceStart("Hello "))
    assertFalse(isSentenceStart("Hello, "))
    assertFalse(isSentenceStart("3 "))
  }

  @Test
  fun punctuationWithoutSpace_doesNotStartSentence() {
    assertFalse(isSentenceStart("e.g"))
    assertFalse(isSentenceStart("Hello."))
    assertFalse(isSentenceStart("word"))
  }

  @Test
  fun plainTextFields_allowCapitalization() {
    assertTrue(fieldAllowsCapitalization(InputType.TYPE_CLASS_TEXT))
    assertTrue(
      fieldAllowsCapitalization(
        InputType.TYPE_CLASS_TEXT or
          InputType.TYPE_TEXT_FLAG_MULTI_LINE or
          InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
      )
    )
    assertTrue(
      fieldAllowsCapitalization(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
      )
    )
  }

  @Test
  fun passwordEmailUriAndNumberFields_disallowCapitalization() {
    val text = InputType.TYPE_CLASS_TEXT
    assertFalse(fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
    assertFalse(fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
    assertFalse(fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    assertFalse(fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
    assertFalse(
      fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
    )
    assertFalse(fieldAllowsCapitalization(text or InputType.TYPE_TEXT_VARIATION_URI))
    assertFalse(fieldAllowsCapitalization(InputType.TYPE_CLASS_NUMBER))
    assertFalse(fieldAllowsCapitalization(InputType.TYPE_CLASS_PHONE))
    assertFalse(fieldAllowsCapitalization(InputType.TYPE_CLASS_DATETIME))
    assertFalse(fieldAllowsCapitalization(InputType.TYPE_NULL))
  }

  @Test
  fun capitalizeFirstLetter_changesOnlyFirstLetter() {
    assertEquals("Hello", capitalizeFirstLetter("hello"))
    assertEquals("Knowledge", capitalizeFirstLetter("knowledge"))
    assertEquals("A", capitalizeFirstLetter("a"))
    assertEquals("\u00C9t\u00E9", capitalizeFirstLetter("\u00E9t\u00E9"))
    assertEquals("\u0416\u0443\u043A", capitalizeFirstLetter("\u0436\u0443\u043A"))
  }

  @Test
  fun capitalizeFirstLetter_skipsOpeningQuotesAndBrackets() {
    assertEquals("\"Yes", capitalizeFirstLetter("\"yes"))
    assertEquals("(Maybe", capitalizeFirstLetter("(maybe"))
    assertEquals("\u00BFQu\u00E9", capitalizeFirstLetter("\u00BFqu\u00E9"))
  }

  @Test
  fun capitalizeFirstLetter_leavesCapitalsDigitsAndUncasedTextUnchanged() {
    assertEquals("Hello", capitalizeFirstLetter("Hello"))
    assertEquals("IPHONE", capitalizeFirstLetter("IPHONE"))
    assertEquals("3rd", capitalizeFirstLetter("3rd"))
    assertEquals("\uD55C\uAE00", capitalizeFirstLetter("\uD55C\uAE00"))
    assertEquals("", capitalizeFirstLetter(""))
    assertEquals("\"", capitalizeFirstLetter("\""))
  }

  @Test
  fun apply_onlyWhenAsked() {
    assertEquals("hello", SentenceCapitalization.apply(false, "hello"))
    assertEquals("Hello", SentenceCapitalization.apply(true, "hello"))
  }
}
