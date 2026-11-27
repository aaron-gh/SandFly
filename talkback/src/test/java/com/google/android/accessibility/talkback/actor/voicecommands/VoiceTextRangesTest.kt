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

package com.google.android.accessibility.talkback.actor.voicecommands

import com.google.android.accessibility.talkback.actor.voicecommands.VoiceTextRanges.Plan
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The ranges of text editing voice commands. */
class VoiceTextRangesTest {

  private fun plan(text: String, cursor: Int, operation: TextOperation, target: TextTarget): Plan =
    VoiceTextRanges.plan(text, cursor, cursor, operation, target, Locale.ENGLISH)

  private fun words(count: Int, forward: Boolean) = TextTarget.Units(TextUnit.WORD, count, forward)

  @Test
  fun movesOverWords() {
    val text = "one two three four"
    assertEquals(Plan.Move(0, 7), plan(text, 0, TextOperation.MOVE, words(2, forward = true)))
    assertEquals(Plan.Move(18, 8), plan(text, 18, TextOperation.MOVE, words(2, forward = false)))
    // Inside a word, the rest of it is the first word.
    assertEquals(Plan.Move(5, 7), plan(text, 5, TextOperation.MOVE, words(1, forward = true)))
    // More words than there are go to the last one.
    assertEquals(Plan.Move(8, 18), plan(text, 8, TextOperation.MOVE, words(50, forward = true)))
    // Punctuation isn't a word.
    assertEquals(Plan.Move(13, 7), plan("Hello, world!", 13, TextOperation.MOVE, words(1, forward = false)))
  }

  @Test
  fun nothingPastTheEnds() {
    assertEquals(Plan.AtEdge(end = true), plan("hello world", 11, TextOperation.MOVE, words(1, forward = true)))
    assertEquals(Plan.AtEdge(end = false), plan("hello", 0, TextOperation.DELETE, words(1, forward = false)))
    assertEquals(Plan.AtEdge(end = true), plan("", 0, TextOperation.SELECT, words(1, forward = true)))
  }

  @Test
  fun noCursorIsTheEndOfTheText() {
    assertEquals(
      Plan.Move(11, 6),
      VoiceTextRanges.plan("hello world", -1, -1, TextOperation.MOVE, words(1, forward = false), Locale.ENGLISH),
    )
  }

  @Test
  fun selectionMovesFromItsEnds() {
    // "two" is selected: forward from its end, back from its start.
    val text = "one two three"
    assertEquals(
      Plan.Move(7, 13),
      VoiceTextRanges.plan(text, 4, 7, TextOperation.MOVE, words(1, forward = true), Locale.ENGLISH),
    )
    assertEquals(
      Plan.Move(4, 0),
      VoiceTextRanges.plan(text, 7, 4, TextOperation.MOVE, words(1, forward = false), Locale.ENGLISH),
    )
  }

  @Test
  fun selectedCopiedAndCutTextIsWithoutWhiteSpace() {
    assertEquals(Plan.Edit(6, 11), plan("hello world", 5, TextOperation.SELECT, words(1, forward = true)))
    assertEquals(Plan.Edit(0, 7), plan("one two three", 8, TextOperation.COPY, words(2, forward = false)))
    assertEquals(Plan.Edit(4, 13), plan("one two three", 4, TextOperation.CUT, words(2, forward = true)))
  }

  @Test
  fun deletedWordsTakeTheSpaceLeftOver() {
    // The space after them.
    assertEquals(Plan.Edit(0, 8), plan("one two three four", 0, TextOperation.DELETE, words(2, forward = true)))
    assertEquals(Plan.Edit(4, 8), plan("one two three", 7, TextOperation.DELETE, words(1, forward = false)))
    // At the end of the text, or before punctuation, the space before them.
    assertEquals(Plan.Edit(5, 11), plan("hello world", 11, TextOperation.DELETE, words(1, forward = false)))
    assertEquals(Plan.Edit(5, 11), plan("hello world.", 11, TextOperation.DELETE, words(1, forward = false)))
    // A range with a space at an end already is left as it is.
    assertEquals(Plan.Edit(4, 8), plan("one two three", 8, TextOperation.DELETE, words(1, forward = false)))
    // Part of a word is only that part.
    assertEquals(Plan.Edit(2, 5), plan("hello world", 2, TextOperation.DELETE, words(1, forward = true)))
  }

  @Test
  fun wordsAcrossSentences() {
    // Each word counts once across the end of a sentence.
    val spaced = "one two. Three four five"
    assertEquals(Plan.Edit(4, 20), plan(spaced, 4, TextOperation.DELETE, words(3, forward = true)))
    assertEquals(Plan.Edit(8, 24), plan(spaced, 24, TextOperation.DELETE, words(3, forward = false)))
    // With no space after the full stop, "two.Three" is still two words.
    val joined = "one two.Three four five"
    assertEquals(Plan.Edit(4, 19), plan(joined, 4, TextOperation.DELETE, words(3, forward = true)))
    assertEquals(Plan.Edit(8, 23), plan(joined, 23, TextOperation.DELETE, words(3, forward = false)))
    // Abbreviations, addresses and numbers stay one word.
    assertEquals(Plan.Move(0, 11), plan("example.com is", 0, TextOperation.MOVE, words(1, forward = true)))
    assertEquals(Plan.Move(0, 4), plan("3.14 is", 0, TextOperation.MOVE, words(1, forward = true)))
  }

  @Test
  fun characters() {
    val characters = TextTarget.Units(TextUnit.CHARACTER, 2, forward = true)
    assertEquals(Plan.Edit(0, 2), plan("abc", 0, TextOperation.DELETE, characters))
    // White space is a character, and isn't trimmed.
    assertEquals(Plan.Edit(1, 3), plan("a b", 1, TextOperation.SELECT, characters))
    assertEquals(
      Plan.Move(3, 0),
      plan("abc", 3, TextOperation.MOVE, TextTarget.Units(TextUnit.CHARACTER, 5, forward = false)),
    )
  }

  @Test
  fun sentencesLinesAndParagraphs() {
    assertEquals(
      Plan.Edit(0, 22),
      plan(
        "First one. Second one. Third.",
        0,
        TextOperation.SELECT,
        TextTarget.Units(TextUnit.SENTENCE, 2, forward = true),
      ),
    )
    // A deleted line takes its line break.
    assertEquals(
      Plan.Edit(2, 4),
      plan("a\nb\nc", 2, TextOperation.DELETE, TextTarget.Units(TextUnit.LINE, 1, forward = true)),
    )
    // The last line takes the line break before it.
    assertEquals(
      Plan.Edit(3, 5),
      plan("a\nb\nc", 5, TextOperation.DELETE, TextTarget.Units(TextUnit.LINE, 1, forward = false)),
    )
    assertEquals(
      Plan.Edit(0, 5),
      plan("P1 a.\n\nP2 b.", 0, TextOperation.COPY, TextTarget.Units(TextUnit.PARAGRAPH, 1, forward = true)),
    )
    assertEquals(
      Plan.Edit(0, 7),
      plan("P1 a.\n\nP2 b.", 0, TextOperation.DELETE, TextTarget.Units(TextUnit.PARAGRAPH, 1, forward = true)),
    )
  }

  @Test
  fun startAndEndOfUnits() {
    val text = "hello world"
    val startOfWord = TextTarget.Boundary(TextUnit.WORD, end = false)
    val endOfWord = TextTarget.Boundary(TextUnit.WORD, end = true)
    assertEquals(Plan.Move(8, 6), plan(text, 8, TextOperation.MOVE, startOfWord))
    assertEquals(Plan.Move(8, 11), plan(text, 8, TextOperation.MOVE, endOfWord))
    // At the start already: the start of the word before.
    assertEquals(Plan.Move(6, 0), plan(text, 6, TextOperation.MOVE, startOfWord))
    assertEquals(Plan.Move(0, 2), plan("ab\ncd", 0, TextOperation.MOVE, TextTarget.Boundary(TextUnit.LINE, end = true)))
    assertEquals(Plan.Move(4, 3), plan("ab\ncd", 4, TextOperation.MOVE, TextTarget.Boundary(TextUnit.LINE, end = false)))
    assertEquals(
      Plan.Move(5, 0),
      plan(text, 5, TextOperation.MOVE, TextTarget.Boundary(TextUnit.TEXT, end = false)),
    )
    assertEquals(
      Plan.AtEdge(end = false),
      plan(text, 0, TextOperation.MOVE, TextTarget.Boundary(TextUnit.TEXT, end = false)),
    )
    // Deleting to the end of a unit deletes from the cursor.
    assertEquals(Plan.Edit(8, 11), plan(text, 8, TextOperation.DELETE, endOfWord))
  }

  @Test
  fun phrases() {
    val text = "In sentence crocodile, like to swim"
    assertEquals(Plan.Edit(12, 21), plan(text, 0, TextOperation.SELECT, TextTarget.Phrase("Crocodile")))
    assertEquals(Plan.NotFound, plan(text, 0, TextOperation.DELETE, TextTarget.Phrase("alligator")))
    assertEquals(Plan.NotFound, plan(text, 0, TextOperation.MOVE, TextTarget.Phrase("crocodile")))
  }

  @Test
  fun phraseNearestToTheCursor() {
    assertEquals(8 to 11, VoiceTextRanges.findPhrase("cat dog cat", "cat", 11))
    assertEquals(0 to 3, VoiceTextRanges.findPhrase("cat dog cat", "cat", 0))
    // As near: the one before the cursor.
    assertEquals(0 to 3, VoiceTextRanges.findPhrase("cat xy cat", "cat", 5))
    // Whole words first, else inside a word.
    assertEquals(12 to 15, VoiceTextRanges.findPhrase("concatenate cat", "cat", 0))
    assertEquals(3 to 6, VoiceTextRanges.findPhrase("concatenate", "cat", 0))
    // Any punctuation or spaces between the words, and apostrophes.
    assertEquals(0 to 8, VoiceTextRanges.findPhrase("New-York city", "new york", 0))
    assertEquals(0 to 5, VoiceTextRanges.findPhrase("don't go", "don't", 0))
    assertNull(VoiceTextRanges.findPhrase("hello", "...", 0))
    assertNull(VoiceTextRanges.findPhrase("", "hello", 0))
  }
}
