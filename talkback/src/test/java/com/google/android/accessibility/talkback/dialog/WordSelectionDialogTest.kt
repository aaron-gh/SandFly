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

package com.google.android.accessibility.talkback.dialog

import com.google.android.accessibility.talkback.dialog.WordSelectionDialog.Granularity
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSelectionDialogTest {

  @Test
  fun charactersModeKeepsSpacesAndGraphemeClusters() {
    val input = listOf("Hello world")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.CHARACTERS)
    assertEquals(listOf("H", "e", "l", "l", "o", " ", "w", "o", "r", "l", "d"), tokens)
  }

  @Test
  fun charactersModeKeepsParagraphBreaks() {
    val input = listOf("Hello", "World")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.CHARACTERS)
    assertEquals(listOf("H", "e", "l", "l", "o", "\n", "\n", "W", "o", "r", "l", "d"), tokens)
  }

  @Test
  fun charactersModeCapsTokensAtMaxLimit() {
    val longString = "a".repeat(2000)
    val tokens = WordSelectionDialog.extractTokens(listOf(longString), Granularity.CHARACTERS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, tokens.size)
  }

  @Test
  fun wordsModeSplitsByWhitespaceAndCaps() {
    val input = listOf("Hello world from SandFly", "Second line")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.WORDS)
    assertEquals(listOf("Hello", "world", "from", "SandFly", "Second", "line"), tokens)

    val manyWords = List(1500) { "word$it" }
    val capped = WordSelectionDialog.extractTokens(listOf(manyWords.joinToString(" ")), Granularity.WORDS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, capped.size)
  }

  @Test
  fun paragraphsModeExtractsTrimmedParagraphsAndCaps() {
    val input = listOf("  Paragraph 1  ", "Paragraph 2")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.PARAGRAPHS)
    assertEquals(listOf("Paragraph 1", "Paragraph 2"), tokens)

    val manyParagraphs = List(1500) { "Para $it" }
    val capped = WordSelectionDialog.extractTokens(manyParagraphs, Granularity.PARAGRAPHS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, capped.size)
  }
}
