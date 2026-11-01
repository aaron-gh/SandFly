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
package com.google.android.accessibility.talkback.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTextTest {
  private fun item(text: String, left: Int, top: Int, right: Int, bottom: Int) =
    ReviewItem(text, left, top, right, bottom, source = text)

  @Test
  fun itemsSideBySideShareALine() {
    val text =
      ReviewText(
        listOf(
          item("Settings", 100, 0, 300, 50),
          item("Back", 0, 5, 80, 45),
          item("Wi-Fi", 0, 100, 200, 150),
          // Taller than the line, but its centre is on it.
          item("On", 300, 90, 400, 160),
        )
      )
    assertEquals(listOf("Back Settings", "Wi-Fi On"), text.lines.map { it.text })
  }

  @Test
  fun aTallItemIsALineOfItsOwn() {
    val text =
      ReviewText(
        listOf(
          item("Home", 0, 0, 1000, 2000),
          item("Clock", 0, 100, 200, 200),
          item("Phone", 0, 1800, 200, 1900),
          item("Chrome", 300, 1800, 500, 1900),
        )
      )
    assertEquals(listOf("Home", "Clock", "Phone Chrome"), text.lines.map { it.text })
  }

  @Test
  fun lineBreaksSplitAnItem() {
    val text = ReviewText(listOf(item("First\nSecond", 0, 0, 100, 100)))
    assertEquals(listOf("First", "Second"), text.lines.map { it.text })
  }

  @Test
  fun movesByCharacterWordAndLine() {
    val cursor =
      ReviewCursor(
        ReviewText(listOf(item("one two", 0, 0, 70, 10), item("three", 0, 20, 50, 30)))
      )
    assertEquals("one", cursor.currentWord?.text)
    assertTrue(cursor.moveWord(forward = true))
    assertEquals("two", cursor.currentWord?.text)
    assertTrue(cursor.moveWord(forward = true))
    assertEquals(1, cursor.line)
    assertEquals("three", cursor.currentWord?.text)
    assertFalse(cursor.moveWord(forward = true))
    assertTrue(cursor.moveWord(forward = false))
    assertEquals("two", cursor.currentWord?.text)
    assertTrue(cursor.moveCharacter(forward = true))
    assertEquals('w', cursor.currentCharacter)
    // From inside a word, the word before it.
    assertTrue(cursor.moveWord(forward = false))
    assertEquals("one", cursor.currentWord?.text)
    assertTrue(cursor.moveCharacter(forward = false).not())
    assertFalse(cursor.moveLine(-1))
    assertTrue(cursor.moveToBottom())
    cursor.moveToLineEnd()
    assertEquals('e', cursor.currentCharacter)
    assertFalse(cursor.moveCharacter(forward = true))
  }

  @Test
  fun previousCharacterGoesToTheEndOfThePreviousLine() {
    val cursor =
      ReviewCursor(ReviewText(listOf(item("ab", 0, 0, 20, 10), item("cd", 0, 20, 20, 30))), line = 1)
    assertTrue(cursor.moveCharacter(forward = false))
    assertEquals(0, cursor.line)
    assertEquals('b', cursor.currentCharacter)
  }

  @Test
  fun findsWordsByPlaceAndSource() {
    val cursor =
      ReviewCursor(
        ReviewText(
          listOf(item("left right", 0, 0, 100, 10), item("below", 0, 50, 50, 60))
        )
      )
    assertTrue(cursor.moveNear(90, 5))
    assertEquals("right", cursor.currentWord?.text)
    assertTrue(cursor.moveToSource { it == "below" })
    assertEquals("below", cursor.currentWord?.text)
    assertFalse(cursor.moveToSource { it == "missing" })
  }
}
