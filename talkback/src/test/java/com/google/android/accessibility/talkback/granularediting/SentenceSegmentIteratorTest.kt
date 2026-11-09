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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SentenceSegmentIteratorTest {
  private fun forward(text: String, locale: Locale = Locale.US): List<String> {
    val iterator = SentenceSegmentIterator(text, locale)
    val sentences = ArrayList<String>()
    var position = 0
    while (true) {
      val range = iterator.following(position) ?: return sentences
      sentences += text.substring(range[0], range[1])
      position = range[1]
    }
  }

  private fun backward(text: String, locale: Locale = Locale.US): List<String> {
    val iterator = SentenceSegmentIterator(text, locale)
    val sentences = ArrayList<String>()
    var position = text.length
    while (true) {
      val range = iterator.preceding(position) ?: return sentences
      sentences += text.substring(range[0], range[1])
      position = range[0]
    }
  }

  @Test
  fun latinSentences() {
    val sentences = listOf("One.", "Two?", "Three!")
    assertEquals(sentences, forward("One. Two? Three!"))
    assertEquals(sentences.reversed(), backward("One. Two? Three!"))
  }

  @Test
  fun chineseSentences() {
    val text = "你好。今天怎么样？很好！再见"
    val sentences =
      listOf("你好。", "今天怎么样？", "很好！", "再见")
    assertEquals(sentences, forward(text, Locale.CHINESE))
    assertEquals(sentences.reversed(), backward(text, Locale.CHINESE))
    // The device locale need not be Chinese for Chinese punctuation to end a sentence.
    assertEquals(sentences, forward(text, Locale.US))
  }

  @Test
  fun aSentenceEndsAtTheEndOfALine() {
    assertEquals(
      listOf("Heading", "First sentence.", "Second sentence"),
      forward("Heading\nFirst sentence. Second sentence"),
    )
  }

  @Test
  fun whiteSpaceIsNotPartOfASentence() {
    assertEquals(listOf("One.", "Two."), forward("  One.   \n\n Two.  "))
  }

  @Test
  fun movingBackAfterMovingForwardRepeatsTheSentence() {
    val iterator = SentenceSegmentIterator("One. Two.", Locale.US)
    assertArrayEquals(intArrayOf(0, 4), iterator.following(0))
    assertArrayEquals(intArrayOf(5, 9), iterator.following(4))
    assertArrayEquals(intArrayOf(5, 9), iterator.preceding(9))
    assertArrayEquals(intArrayOf(0, 4), iterator.preceding(5))
  }

  @Test
  fun anOffsetInsideASentenceMovesOverTheRestOfIt() {
    val iterator = SentenceSegmentIterator("One. Two.", Locale.US)
    assertArrayEquals(intArrayOf(6, 9), iterator.following(6))
    assertArrayEquals(intArrayOf(5, 6), iterator.preceding(6))
  }

  @Test
  fun movingPastEitherEndGivesNothing() {
    val iterator = SentenceSegmentIterator("One. Two.", Locale.US)
    assertNull(iterator.following(9))
    assertNull(iterator.preceding(0))
  }

  @Test
  fun offsetsOutsideTheTextAreClamped() {
    val iterator = SentenceSegmentIterator("One. Two.", Locale.US)
    assertArrayEquals(intArrayOf(0, 4), iterator.following(-5))
    assertNull(iterator.following(100))
    assertNull(iterator.preceding(-5))
    assertArrayEquals(intArrayOf(5, 9), iterator.preceding(100))
  }

  @Test
  fun textWithoutSentencesGivesNothing() {
    for (text in listOf("", "   \n ")) {
      val iterator = SentenceSegmentIterator(text, Locale.US)
      assertNull(iterator.following(0))
      assertNull(iterator.preceding(text.length))
    }
  }
}
