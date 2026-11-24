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

package com.google.android.accessibility.braille.translate

import com.google.android.accessibility.braille.interfaces.BrailleWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SplitTranslatorTest {
  private val calls = mutableListOf<String>()

  private inner class Named(private val name: String) : BrailleTranslator {
    override fun translateToPrint(brailleWord: BrailleWord): String = name

    override fun translateToPrintPartial(brailleWord: BrailleWord): String = "$name partial"

    override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult? {
      calls += "$name $text $cursorPosition"
      return null
    }
  }

  private val typing = Named("typing")
  private val reading = Named("reading")

  @Test
  fun cellsAreTranslatedWithTheTypingTable() {
    val translator = SplitTranslator(typing, reading)
    assertEquals("typing", translator.translateToPrint(BrailleWord()))
    assertEquals("typing partial", translator.translateToPrintPartial(BrailleWord()))
  }

  @Test
  fun textIsTranslatedWithTheReadingTable() {
    SplitTranslator(typing, reading).translate("ab", 1)
    assertEquals(listOf("reading ab 1"), calls)
  }

  @Test
  fun equalWithTheSameTranslators() {
    assertEquals(SplitTranslator(typing, reading), SplitTranslator(typing, reading))
    assertEquals(
      SplitTranslator(typing, reading).hashCode(),
      SplitTranslator(typing, reading).hashCode(),
    )
    assertNotEquals(SplitTranslator(typing, reading), SplitTranslator(reading, typing))
  }
}
