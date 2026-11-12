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

import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks Read line breaks as spaces. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LineBreaksTest {

  private fun spaced(text: CharSequence) = LineBreaks.toSpaces(text)?.toString()

  @Test
  fun eachKindOfLineBreakIsASpace() {
    assertEquals("Ronan Owner Hi", spaced("Ronan\nOwner\nHi"))
    assertEquals("Ronan Owner Hi", spaced("Ronan\r\nOwner\rHi"))
    assertEquals("Ronan Owner Hi", spaced("Ronan Owner Hi"))
  }

  @Test
  fun aRunOfLineBreaksIsOneSpace() {
    assertEquals("First paragraph Second", spaced("First paragraph\n\n\r\n Second"))
    assertEquals(" Hi ", spaced("\n\nHi\n"))
  }

  @Test
  fun spacesNextToALineBreakStay() {
    assertEquals("a   b", spaced("a \n b"))
  }

  @Test
  fun textWithoutLineBreaksIsLeftAlone() {
    assertNull(spaced("Ronan Owner Hi"))
    assertNull(spaced(""))
  }

  @Test
  fun lineBreaksOnTheirOwnAreLeftAlone() {
    assertNull(spaced("\n"))
    assertNull(spaced("\r\n"))
    assertNull(spaced(" \n "))
  }

  @Test
  fun spansKeepTheirText() {
    val text = SpannableString("Hallo\nWelt\nHello")
    val german = LocaleSpan(Locale.GERMAN)
    text.setSpan(german, 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val result = LineBreaks.toSpaces(text)
    assertTrue(result is Spanned)
    result as Spanned
    assertEquals("Hallo Welt Hello", result.toString())
    assertEquals(0, result.getSpanStart(german))
    assertEquals(10, result.getSpanEnd(german))
  }
}
