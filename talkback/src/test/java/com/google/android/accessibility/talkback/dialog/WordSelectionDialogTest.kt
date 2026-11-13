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
import java.util.BitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSelectionDialogTest {

  @Test
  fun charactersModeKeepsSpacesAndGraphemeClusters() {
    val input = listOf("Hello world")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.CHARACTERS).texts()
    assertEquals(listOf("H", "e", "l", "l", "o", " ", "w", "o", "r", "l", "d"), tokens)
  }

  @Test
  fun charactersModeKeepsParagraphBreaks() {
    val input = listOf("Hello", "World")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.CHARACTERS).texts()
    assertEquals(listOf("H", "e", "l", "l", "o", "\n", "\n", "W", "o", "r", "l", "d"), tokens)
  }

  @Test
  fun charactersModeCapsTokensAtMaxLimit() {
    val longString = "a".repeat(2000)
    val tokens = WordSelectionDialog.extractTokens(listOf(longString), Granularity.CHARACTERS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, tokens.items.size)
    assertTrue(tokens.truncated)
  }

  @Test
  fun wordsModeSplitsByWhitespaceAndCaps() {
    val input = listOf("Hello world from SandFly", "Second line")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.WORDS)
    assertEquals(listOf("Hello", "world", "from", "SandFly", "Second", "line"), tokens.texts())
    assertFalse(tokens.truncated)

    val manyWords = List(1500) { "word$it" }
    val capped =
      WordSelectionDialog.extractTokens(listOf(manyWords.joinToString(" ")), Granularity.WORDS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, capped.items.size)
    assertTrue(capped.truncated)
  }

  @Test
  fun paragraphsModeExtractsTrimmedParagraphsAndCaps() {
    val input = listOf("  Paragraph 1  ", "Paragraph 2")
    val tokens = WordSelectionDialog.extractTokens(input, Granularity.PARAGRAPHS)
    assertEquals(listOf("Paragraph 1", "Paragraph 2"), tokens.texts())

    val manyParagraphs = List(1500) { "Para $it" }
    val capped = WordSelectionDialog.extractTokens(manyParagraphs, Granularity.PARAGRAPHS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, capped.items.size)
    assertTrue(capped.truncated)
  }

  @Test
  fun exactlyMaxTokensIsNotTruncated() {
    val words = List(WordSelectionDialog.MAX_TOKENS) { "w$it" }.joinToString(" ")
    val tokens = WordSelectionDialog.extractTokens(listOf(words), Granularity.WORDS)
    assertEquals(WordSelectionDialog.MAX_TOKENS, tokens.items.size)
    assertFalse(tokens.truncated)
  }

  @Test
  fun longParagraphIsSplitAtSpacesAndCopiedWhole() {
    val paragraph = List(200) { "word$it" }.joinToString(" ")
    val tokens = WordSelectionDialog.extractTokens(listOf(paragraph, "Next"), Granularity.PARAGRAPHS)
    assertTrue(tokens.items.size > 2)
    tokens.items.forEach { assertTrue(it.text.length <= WordSelectionDialog.MAX_ITEM_CHARS) }
    tokens.items.forEach { assertFalse(it.text.startsWith("ord")) }
    val all = BitSet().apply { set(0, tokens.items.size) }
    assertEquals(
      paragraph + "\n\nNext",
      WordSelectionDialog.joinSelected(tokens.items, all, Granularity.PARAGRAPHS),
    )
  }

  @Test
  fun longWordIsSplitAndRejoinedWithoutSpaces() {
    val word = "x".repeat(WordSelectionDialog.MAX_ITEM_CHARS * 2 + 5)
    val tokens = WordSelectionDialog.extractTokens(listOf("a $word b"), Granularity.WORDS)
    assertEquals(5, tokens.items.size)
    val all = BitSet().apply { set(0, tokens.items.size) }
    assertEquals("a $word b", WordSelectionDialog.joinSelected(tokens.items, all, Granularity.WORDS))
  }

  @Test
  fun piecesWithAGapAreJoinedWithTheDelimiter() {
    val word = "x".repeat(WordSelectionDialog.MAX_ITEM_CHARS * 2 + 5)
    val tokens = WordSelectionDialog.extractTokens(listOf(word), Granularity.WORDS)
    val firstAndLast = BitSet().apply { set(0); set(2) }
    assertEquals(
      "x".repeat(WordSelectionDialog.MAX_ITEM_CHARS) + " xxxxx",
      WordSelectionDialog.joinSelected(tokens.items, firstAndLast, Granularity.WORDS),
    )
  }

  @Test
  fun splitDoesNotBreakSurrogatePairs() {
    val text = "a".repeat(WordSelectionDialog.MAX_ITEM_CHARS - 1) + "\uD83D\uDE00" + "b"
    val tokens = WordSelectionDialog.extractTokens(listOf(text), Granularity.WORDS)
    tokens.items.forEach {
      assertFalse(Character.isHighSurrogate(it.text.last()))
      assertFalse(Character.isLowSurrogate(it.text.first()))
    }
  }

  @Test
  fun collectedTextStopsAtLimit() {
    val collected = WordSelectionDialog.Collected()
    assertTrue(collected.add("a".repeat(WordSelectionDialog.MAX_TEXT_CHARS - 10)))
    assertFalse(collected.add("b".repeat(20)))
    assertTrue(collected.truncated)
    assertEquals(WordSelectionDialog.MAX_TEXT_CHARS, collected.chars)
    assertFalse(collected.add("c"))
    assertEquals(2, collected.paragraphs.size)
  }

  @Test
  fun entitiesModeListsEachAddressOrNumberAsWritten() {
    val tokens = WordSelectionDialog.entityTokens(listOf("020 7946 0000", "help@example.org"))
    assertEquals(listOf("020 7946 0000", "help@example.org"), tokens.texts())
    assertFalse(tokens.truncated)
  }

  @Test
  fun entitiesModeCapsTokensAtMaxLimit() {
    val tokens = WordSelectionDialog.entityTokens(List(WordSelectionDialog.MAX_TOKENS + 1) { "$it" })
    assertEquals(WordSelectionDialog.MAX_TOKENS, tokens.items.size)
    assertTrue(tokens.truncated)
  }

  @Test
  fun entitiesModeJoinsCheckedItemsWithLineBreaks() {
    val tokens = WordSelectionDialog.entityTokens(listOf("482916", "020 7946 0000", "a@b.org"))
    val checked = BitSet().apply {
      set(0)
      set(2)
    }
    assertEquals(
      "482916\na@b.org",
      WordSelectionDialog.joinSelected(tokens.items, checked, Granularity.ENTITIES),
    )
  }
}
