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

package com.google.android.accessibility.talkback.gesture

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks the text and scroll parts of Speak position. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextPositionTest {

  private val context = RuntimeEnvironment.getApplication()

  @Test
  fun speaksCharacterAndPercentInOneParagraph() {
    assertEquals("Character 1 of 10, 0%", TextPosition.describe(context, TextCursor("0123456789", 0)))
    assertEquals("Character 4 of 10, 30%", TextPosition.describe(context, TextCursor("0123456789", 3)))
    // At the end, the cursor is after the last character.
    assertEquals(
      "Character 10 of 10, 100%",
      TextPosition.describe(context, TextCursor("0123456789", 10)),
    )
    assertNull(TextPosition.describe(context, TextCursor("", 0)))
  }

  @Test
  fun speaksParagraphWhenThereIsMoreThanOne() {
    val text = "First line.\nSecond line.\n\nThird."
    assertEquals(
      "Character 13 of 32, paragraph 2 of 3, 37%",
      TextPosition.describe(context, TextCursor(text, 12)),
    )
  }

  @Test
  fun countsParagraphsBetweenLineBreaks() {
    val text = "ab\n\ncd\nef\n"
    assertEquals(1 to 3, TextPosition.paragraphs(text, 0))
    // A line break belongs to the paragraph before it.
    assertEquals(1 to 3, TextPosition.paragraphs(text, 3))
    assertEquals(2 to 3, TextPosition.paragraphs(text, 4))
    assertEquals(3 to 3, TextPosition.paragraphs(text, 8))
    assertEquals(1 to 0, TextPosition.paragraphs("\n\n", 1))
  }

  @Test
  fun editFieldWithInputFocusUsesItsCursor() {
    val field = node(EditText::class.java.name, "Hello world").apply {
      isEditable = true
      isFocused = true
      setTextSelection(6, 6)
    }
    val other = TextCursor("Other", 2)
    assertCursor("Hello world", 6, TextPosition.cursorOf(field, other, other))
    field.setTextSelection(-1, -1)
    assertNull(TextPosition.cursorOf(field, other, other))
  }

  @Test
  fun otherTextUsesSandFlysCursorBeforeTheAppsCursor() {
    val text = node(TextView::class.java.name, "Some text")
    val own = TextCursor("Some text", 4)
    val traversed = TextCursor("Some text", 2)
    assertSame(own, TextPosition.cursorOf(text, own, traversed))
    assertSame(traversed, TextPosition.cursorOf(text, null, traversed))
    assertNull(TextPosition.cursorOf(null, own, traversed))
  }

  @Test
  fun trackerKeepsTheAppsCursorUntilFocusMoves() {
    val tracker = TextTraversalTracker()
    tracker.onAccessibilityEvent(traversal(5, 10, back = false), null)
    assertCursor("Hello world", 10, tracker.cursor())
    tracker.onAccessibilityEvent(traversal(6, 11, back = true), null)
    assertCursor("Hello world", 6, tracker.cursor())
    // Indices past the text are not kept.
    tracker.onAccessibilityEvent(traversal(6, 40, back = false), null)
    assertNull(tracker.cursor())
    tracker.onAccessibilityEvent(traversal(0, 5, back = false), null)
    tracker.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED),
      null,
    )
    assertNull(tracker.cursor())
  }

  @Test
  fun scrollPercentFromActionsAndLastScroll() {
    assertEquals(0, ItemPosition.scrollPercentOf(back = false, forward = true, cached = 50))
    assertEquals(100, ItemPosition.scrollPercentOf(back = true, forward = false, cached = 50))
    assertEquals(40, ItemPosition.scrollPercentOf(back = true, forward = true, cached = 40))
    // Scrollable both ways is neither the start nor the end.
    assertEquals(1, ItemPosition.scrollPercentOf(back = true, forward = true, cached = 0))
    assertEquals(99, ItemPosition.scrollPercentOf(back = true, forward = true, cached = 100))
    assertNull(ItemPosition.scrollPercentOf(back = true, forward = true, cached = -1))
    assertNull(ItemPosition.scrollPercentOf(back = false, forward = false, cached = 50))
  }

  private fun assertCursor(text: String, offset: Int, cursor: TextCursor?) {
    assertEquals(text, cursor?.text?.toString())
    assertEquals(offset, cursor?.offset)
  }

  private fun node(className: String, text: String): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo()).apply {
      this.className = className
      this.text = text
    }

  private fun traversal(from: Int, to: Int, back: Boolean): AccessibilityEvent =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY)
      .apply {
        text.add("Hello world")
        fromIndex = from
        toIndex = to
        action =
          if (back) {
            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
          } else {
            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
          }
      }
}
