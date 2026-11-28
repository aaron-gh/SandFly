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
import org.junit.Assert.assertFalse
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
      pieces.forEach { assertTrue(it.start <= it.coreStart && it.coreStart <= it.coreEnd && it.coreEnd <= it.end) }
      if (granularity != Granularity.LINES) {
        pieces.forEach {
          assertTrue(it.start == it.coreStart && !it.isBlank)
          val after = text.substring(it.coreEnd, it.end)
          assertTrue(after.isEmpty() || after == granularity.separator)
        }
      }
    }
  }

  @Test
  fun fromNameFindsEveryGranularityAndFallsBackToWords() {
    for (granularity in Granularity.values()) {
      assertEquals(granularity, Granularity.fromName(granularity.name))
    }
    assertEquals(Granularity.WORDS, Granularity.fromName(null))
    assertEquals(Granularity.WORDS, Granularity.fromName(""))
    assertEquals(Granularity.WORDS, Granularity.fromName("NOT_A_GRANULARITY"))
  }

  @Test
  fun wordsShowWhiteSpaceOtherThanOneSpaceAsPieces() {
    assertEquals(
      listOf(
        "  ", "Hello", ",", "big", "world", ".", "  ", "How", "are", "you", "?", "\n",
        "Second", "line", "\n\n\n", "New", "paragraph", ".", " ",
      ),
      shown(text, Granularity.WORDS),
    )
    // A space before closing punctuation or after opening punctuation is shown.
    assertEquals(listOf("word", " ", ",", "  ", "next"), shown("word ,  next", Granularity.WORDS))
    assertEquals(listOf("(", " ", "a", ")", "b"), shown("( a) b", Granularity.WORDS))
    assertEquals(listOf("a", "\t", "b"), shown("a\tb", Granularity.WORDS))
  }

  @Test
  fun linesKeepBlankLinesAndSpaces() {
    assertEquals(
      listOf("  Hello, big world.  How are you?", "Second line", "", "", "New paragraph. "),
      shown(text, Granularity.LINES),
    )
    assertEquals(listOf("a", "", "b", ""), shown("a\r\n\nb\n", Granularity.LINES))
    assertEquals(listOf("", ""), shown("\n", Granularity.LINES))
  }

  @Test
  fun sentencesAndParagraphs() {
    assertEquals(
      listOf("  ", "Hello, big world.  How are you?\nSecond line", "\n\n\n", "New paragraph.", " "),
      shown(text, Granularity.PARAGRAPHS),
    )
    assertEquals(listOf("P1", "P2"), shown("P1\n\nP2", Granularity.PARAGRAPHS))
    assertEquals(listOf("One.", "Two."), shown("One. Two.", Granularity.SENTENCES))
    assertEquals(
      listOf("  ", "Hello, big world.", "  ", "How are you?", "\n", "Second line", "\n\n\n", "New paragraph.", " "),
      shown(text, Granularity.SENTENCES),
    )
  }

  @Test
  fun charactersIncludeSpaces() {
    assertEquals(listOf("a", " ", "b"), shown("a b", Granularity.CHARACTERS))
  }

  @Test
  fun whiteSpaceRunsInOrder() {
    assertEquals(
      listOf(WhiteSpace.SPACE to 2, WhiteSpace.NEW_LINE to 2, WhiteSpace.TAB to 1, WhiteSpace.SPACE to 1),
      TextPieces.whiteSpaceRuns("  \r\n\n\t\u00A0"),
    )
    val words = TextPieces.split("a  b", Granularity.WORDS, Locale.US)
    assertTrue(TextPieces.isWhiteSpace("a  b", words[1]))
    assertFalse(TextPieces.isWhiteSpace("a  b", words[0]))
  }

  @Test
  fun deleteRemovesOnlyThePieces() {
    val t = "one two three"
    val words = TextPieces.split(t, Granularity.WORDS, Locale.US)
    assertEquals("two three", TextPieces.delete(t, listOf(words[0]), Granularity.WORDS))
    assertEquals("one three", TextPieces.delete(t, listOf(words[1]), Granularity.WORDS))
    assertEquals("one two", TextPieces.delete(t, listOf(words[2]), Granularity.WORDS))
    assertEquals("one", TextPieces.delete(t, listOf(words[1], words[2]), Granularity.WORDS))
    assertEquals("", TextPieces.delete(t, words, Granularity.WORDS))
    // White space shown as a piece of its own stays when the word after it is deleted.
    val spaced = "one  two"
    val spacedWords = TextPieces.split(spaced, Granularity.WORDS, Locale.US)
    assertEquals("one  ", TextPieces.delete(spaced, listOf(spacedWords[2]), Granularity.WORDS))
    assertEquals("One.", TextPieces.delete("One. Two.", listOf(TextPieces.split("One. Two.", Granularity.SENTENCES, Locale.US)[1]), Granularity.SENTENCES))
  }

  @Test
  fun deleteLines() {
    val t = "a\n\nb\nc"
    val lines = TextPieces.split(t, Granularity.LINES, Locale.US)
    assertEquals("a\nb\nc", TextPieces.delete(t, listOf(lines[1]), Granularity.LINES))
    assertEquals("a\n\nb", TextPieces.delete(t, listOf(lines[3]), Granularity.LINES))
    assertEquals("a\n", TextPieces.delete(t, listOf(lines[2], lines[3]), Granularity.LINES))
    val trailing = TextPieces.split("a\r\n", Granularity.LINES, Locale.US)
    assertEquals("a", TextPieces.delete("a\r\n", listOf(trailing[1]), Granularity.LINES))
    assertEquals("", TextPieces.delete("a\r\n", trailing, Granularity.LINES))
  }

  @Test
  fun editsKeepTheRestOfTheText() {
    val t = "one two three"
    val words = TextPieces.split(t, Granularity.WORDS, Locale.US)
    assertEquals("one 2 three", TextPieces.replace(t, words[1], "2"))
    assertEquals("one two and three", TextPieces.insertAfter(t, words[1], "and", Granularity.WORDS))
    assertEquals("one two and three", TextPieces.insertBefore(t, words[2], "and", Granularity.WORDS))
    assertEquals("zero one two three", TextPieces.insertBefore(t, words[0], "zero", Granularity.WORDS))
    assertEquals("one two three four", TextPieces.append(t, "four", Granularity.WORDS))
    assertEquals("one three", TextPieces.join(t, listOf(words[2], words[0]), Granularity.WORDS))
    assertEquals("one two", TextPieces.join(t, listOf(words[0], words[1]), Granularity.WORDS))
  }

  @Test
  fun wordsInsertNextToPunctuation() {
    val t = "(one), two"
    val words = TextPieces.split(t, Granularity.WORDS, Locale.US)
    assertEquals(listOf("(", "one", ")", ",", "two"), shown(t, Granularity.WORDS))
    assertEquals("(one and), two", TextPieces.insertAfter(t, words[1], "and", Granularity.WORDS))
    assertEquals("(and one), two", TextPieces.insertBefore(t, words[1], "and", Granularity.WORDS))
  }

  @Test
  fun linesInsertOnANewLine() {
    val lines = TextPieces.split("a\nc", Granularity.LINES, Locale.US)
    assertEquals("a\nb\nc", TextPieces.insertAfter("a\nc", lines[0], "b", Granularity.LINES))
    assertEquals("b\na\nc", TextPieces.insertBefore("a\nc", lines[0], "b", Granularity.LINES))
    val blank = TextPieces.split("a\n\nc", Granularity.LINES, Locale.US)
    assertEquals("a\n\nb\nc", TextPieces.insertAfter("a\n\nc", blank[1], "b", Granularity.LINES))
    assertEquals("a\nb", TextPieces.replace("a\n", TextPieces.split("a\n", Granularity.LINES, Locale.US)[1], "b"))
  }

  @Test
  fun lineReplacedWithALineBreakLeavesOneBlankLine() {
    val text = "one\ntwo\nthree"
    val lines = TextPieces.split(text, Granularity.LINES, Locale.US)
    assertEquals("one\n\nthree", TextPieces.replace(text, lines[1], "\n", Granularity.LINES))
    assertEquals("one\nb\nthree", TextPieces.replace(text, lines[1], "b\r\n", Granularity.LINES))
    // The last line has no line break of its own, so a typed one is kept.
    assertEquals("one\ntwo\nend\n", TextPieces.replace(text, lines[2], "end\n", Granularity.LINES))
  }

  @Test
  fun paragraphsInsertBetweenBlankLines() {
    val t = "P1\n\nP2"
    val paragraphs = TextPieces.split(t, Granularity.PARAGRAPHS, Locale.US)
    assertEquals("P1\n\nnew\n\nP2", TextPieces.insertAfter(t, paragraphs[0], "new", Granularity.PARAGRAPHS))
    assertEquals("P1\n\nnew\n\nP2", TextPieces.insertBefore(t, paragraphs[1], "new", Granularity.PARAGRAPHS))
    assertEquals("P1\n\nP2\n\nnew", TextPieces.insertAfter(t, paragraphs[1], "new", Granularity.PARAGRAPHS))
  }

  @Test
  fun replaceAllIgnoresCaseAndCounts() {
    assertEquals("x b x" to 2, TextPieces.replaceAll("a b A", "a", "x"))
    assertEquals("a $1" to 1, TextPieces.replaceAll("a b", "b", "$1"))
  }

  @Test
  fun whiteSpaceOnlyTextIsShown() {
    assertEquals(listOf("   \n "), shown("   \n ", Granularity.WORDS))
    assertTrue(TextPieces.split("", Granularity.CHARACTERS, Locale.US).isEmpty())
    assertTrue(TextPieces.split("", Granularity.LINES, Locale.US).isEmpty())
  }

  @Test
  fun findIndexSearchesForwardAndBackwardWithWrapping() {
    val text = "one two one three one"
    val pieces = TextPieces.split(text, Granularity.WORDS, Locale.US)
    val ones = pieces.indices.filter { text.substring(pieces[it].coreStart, pieces[it].coreEnd) == "one" }
    assertEquals(3, ones.size)
    // Nothing focused yet: forward starts at the first piece, backward at the last.
    assertEquals(ones[0], TextPieces.findIndex(text, pieces, "ONE", -1))
    assertEquals(ones[2], TextPieces.findIndex(text, pieces, "one", -1, backwards = true))
    // From a match, the next and previous matches.
    assertEquals(ones[2], TextPieces.findIndex(text, pieces, "one", ones[1]))
    assertEquals(ones[0], TextPieces.findIndex(text, pieces, "one", ones[1], backwards = true))
    // Wrapping around both ends.
    assertEquals(ones[0], TextPieces.findIndex(text, pieces, "one", ones[2]))
    assertEquals(ones[2], TextPieces.findIndex(text, pieces, "one", ones[0], backwards = true))
    // A single match is found again from itself.
    val three = TextPieces.findIndex(text, pieces, "three", -1)
    assertEquals(three, TextPieces.findIndex(text, pieces, "three", three, backwards = true))
    // An index past the end starts over.
    assertEquals(ones[0], TextPieces.findIndex(text, pieces, "one", pieces.size + 3))
    assertEquals(-1, TextPieces.findIndex(text, pieces, "four", 0))
    assertEquals(-1, TextPieces.findIndex(text, pieces, "", 0))
    assertEquals(-1, TextPieces.findIndex(text, emptyList(), "one", 0, backwards = true))
  }
}
