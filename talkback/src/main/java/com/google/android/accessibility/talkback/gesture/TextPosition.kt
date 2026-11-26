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

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityEventListener
import com.google.android.accessibility.utils.AccessibilityEventUtils
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.Role

/**
 * A place in [text]: [offset] is a cursor between characters, from 0 before the first character to
 * the length of the text after the last. After a move forward the cursor is after the text moved
 * over, and after a move back before it, as in an edit field.
 */
data class TextCursor(val text: CharSequence, val offset: Int)

/** The text part of the Speak position command: where the cursor is in the focused text. */
object TextPosition {

  /**
   * Returns the cursor of [focused]: its own cursor when it is an edit field with input focus,
   * otherwise [own], the cursor SandFly keeps in text it moves through itself, otherwise
   * [traversed], the cursor the app keeps after a move through its text.
   */
  @JvmStatic
  fun cursorOf(
    focused: AccessibilityNodeInfoCompat?,
    own: TextCursor?,
    traversed: TextCursor?,
  ): TextCursor? {
    focused ?: return null
    if (
      (focused.isEditable || Role.getRole(focused) == Role.ROLE_EDIT_TEXT) &&
        focused.isFocused
    ) {
      val text = focused.text
      if (text == null || text.isEmpty() || focused.isShowingHintText) {
        return null
      }
      val cursor = focused.textSelectionEnd
      return if (cursor in 0..text.length) TextCursor(text, cursor) else null
    }
    return own ?: traversed
  }

  /**
   * Returns the position of [cursor] to speak, such as "Character 120 of 800, paragraph 2 of 5,
   * 15%", or null for empty text. The character is the one after the cursor, or the last one at
   * the end. Paragraphs are separated by line breaks, as when moving by paragraph.
   */
  @JvmStatic
  fun describe(context: Context, cursor: TextCursor): String? {
    val length = cursor.text.length
    if (length == 0) {
      return null
    }
    val offset = cursor.offset.coerceIn(0, length)
    val character = minOf(offset + 1, length)
    val percent = (offset.toLong() * 100 / length).toInt()
    val (paragraph, paragraphs) = paragraphs(cursor.text, minOf(offset, length - 1))
    return if (paragraphs > 1) {
      context.getString(
        R.string.speak_position_text_paragraph,
        character,
        length,
        paragraph,
        paragraphs,
        percent,
      )
    } else {
      context.getString(R.string.speak_position_text, character, length, percent)
    }
  }

  /**
   * Returns the number of the paragraph that [index] is in, from 1, and the number of paragraphs:
   * runs of text between line breaks, so blank lines start no paragraph. An index on a line break
   * is in the paragraph before it.
   */
  @JvmStatic
  fun paragraphs(text: CharSequence, index: Int): Pair<Int, Int> {
    var count = 0
    var current = 0
    var inParagraph = false
    for (i in 0 until text.length) {
      val lineBreak = text[i] == '\n'
      if (!lineBreak && !inParagraph) {
        count++
      }
      inParagraph = !lineBreak
      if (i == index) {
        current = count
      }
    }
    return maxOf(current, 1) to count
  }
}

/**
 * Keeps the cursor that the app keeps after the last move by character, word, line or paragraph
 * through its text, for Speak position, until accessibility focus moves. Text that SandFly moves
 * through itself sends no such event; its cursor is kept by the granularity code.
 */
class TextTraversalTracker : AccessibilityEventListener {

  private var last: TextCursor? = null

  override fun getEventTypes(): Int =
    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY or
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED

  override fun onAccessibilityEvent(event: AccessibilityEvent, eventId: EventId?) {
    last = null
    if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY) {
      return
    }
    // The text that the indices are in. One text is kept as it is, without a copy.
    val text: CharSequence? =
      event.contentDescription?.takeIf { it.isNotEmpty() }
        ?: event.text.singleOrNull()
        ?: AccessibilityEventUtils.getEventAggregateText(event)
    val from = minOf(event.fromIndex, event.toIndex)
    val to = maxOf(event.fromIndex, event.toIndex)
    if (text == null || text.isEmpty() || from < 0 || to > text.length) {
      return
    }
    val back = event.action == AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
    last = TextCursor(text, if (back) from else to)
  }

  /** Returns the cursor after the last move through text since focus moved, or null. */
  fun cursor(): TextCursor? = last
}
