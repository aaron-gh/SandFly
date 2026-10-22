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

package com.google.android.accessibility.talkback.granularediting

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPiecesTest {
  private val text = "  Hello, big world.  How are you?\nSecond line\n\n\nNew paragraph. "

  private fun shown(text: String, granularity: Granularity) =
    TextPieces.split(text, granularity, Locale.US).map { text.substring(it.coreStart, it.coreEnd) }

  @Test
  fun piecesCoverTheWholeTextInOrder() {
    for (granularity in Granularity.values()) {
      val pieces = TextPieces.split(text, granularity, Locale.US)
      assertEquals(granularity.name, 0, pieces.first().start)
      assertEquals(granularity.name, text.length, pieces.last().end)
      pieces.zipWithNext().forEach { (a, b) -> assertEquals(granularity.name, a.end, b.start) }
      pieces.forEach { assertTrue(it.start <= it.coreStart && it.coreStart < it.coreEnd && it.coreEnd <= it.end) }
    }
  }

  @Test
  fun wordsLeaveOutWhiteSpaceAndKeepPunctuation() {
    assertEquals(
      listOf("Hello", ",", "big", "world", ".", "How", "are", "you", "?", "Second", "line", "New", "paragraph", "."),
      shown(text, Granularity.WORDS),
    )
  }

  @Test
  fun linesSentencesAndParagraphs() {
    assertEquals(listOf("Hello, big world.  How are you?", "Second line", "New paragraph."), shown(text, Granularity.LINES))
    assertEquals(
      listOf("Hello, big world.  How are you?\nSecond line", "New paragraph."),
      shown(text, Granularity.PARAGRAPHS),
    )
    assertEquals(listOf("Hello, big world.", "How are you?", "Second line", "New paragraph."), shown(text, Granularity.SENTENCES))
  }

  @Test
  fun charactersIncludeSpaces() {
    assertEquals(listOf("a", " ", "b"), shown("a b", Granularity.CHARACTERS))
  }

  @Test
  fun editsKeepTheRestOfTheText() {
    val words = TextPieces.split("one two three", Granularity.WORDS, Locale.US)
    assertEquals("one 2 three", TextPieces.replace("one two three", words[1], "2"))
    assertEquals("one three", TextPieces.delete("one two three", listOf(words[1])))
    assertEquals("one", TextPieces.delete("one two three", listOf(words[1], words[2])))
    assertEquals("one two and three", TextPieces.insertAfter("one two three", words[1], "and", Granularity.WORDS))
    assertEquals("zero one two three", TextPieces.insertBefore("one two three", words[0], "zero", Granularity.WORDS))
    assertEquals("one two three four", TextPieces.append("one two three", "four", Granularity.WORDS))
    assertEquals("one three", TextPieces.join("one two three", listOf(words[2], words[0])))
  }

  @Test
  fun linesInsertOnANewLine() {
    val lines = TextPieces.split("a\nc", Granularity.LINES, Locale.US)
    assertEquals("a\nb\nc", TextPieces.insertAfter("a\nc", lines[0], "b", Granularity.LINES))
  }

  @Test
  fun replaceAllIgnoresCaseAndCounts() {
    assertEquals("x b x" to 2, TextPieces.replaceAll("a b A", "a", "x"))
    assertEquals("a $1" to 1, TextPieces.replaceAll("a b", "b", "$1"))
  }

  @Test
  fun whiteSpaceOnlyTextHasNoPieces() {
    assertTrue(TextPieces.split("   \n ", Granularity.WORDS, Locale.US).isEmpty())
    assertTrue(TextPieces.split("", Granularity.CHARACTERS, Locale.US).isEmpty())
  }
}
