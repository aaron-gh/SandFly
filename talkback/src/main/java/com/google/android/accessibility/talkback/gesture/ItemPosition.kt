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
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.scripting.copyToClipboard
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import java.util.function.ToIntFunction

/**
 * The Speak position command: where the focused item is in its list or table, where the cursor is
 * in its text, and how far its scrolling container is scrolled; and the Speak coordinates command:
 * where it is on screen.
 */
object ItemPosition {

  // How far up from the focused item to look for the list item, and from there for the list.
  private const val MAX_ITEM_DEPTH = 6
  private const val MAX_LIST_DEPTH = 3

  /**
   * Returns the position of [focused] to speak: where it is in its list or table, where [text], the
   * cursor in its text, is, and how far its scrolling container is scrolled. [scrollPercent] gives
   * the percentage of a container from its last scroll event, or -1.
   */
  @JvmStatic
  fun describe(
    context: Context,
    focused: AccessibilityNodeInfoCompat?,
    text: TextCursor?,
    scrollPercent: ToIntFunction<AccessibilityNodeInfo>,
  ): String {
    val parts =
      listOfNotNull(
        listPosition(context, focused),
        text?.let { TextPosition.describe(context, it) },
        scrollPosition(focused, scrollPercent)?.let {
          context.getString(R.string.speak_position_scroll, it)
        },
      )
    return if (parts.isEmpty()) {
      context.getString(R.string.speak_position_none)
    } else {
      parts.joinToString(", ")
    }
  }

  /**
   * Returns how far the scrolling container of [focused], or [focused] itself, is scrolled, from 0
   * to 100 percent, or null when nothing around it scrolls or the percentage is unknown. The scroll
   * actions it offers now say whether it is at the start or the end; in between, [scrollPercent]
   * gives the percentage from its last scroll event.
   */
  private fun scrollPosition(
    focused: AccessibilityNodeInfoCompat?,
    scrollPercent: ToIntFunction<AccessibilityNodeInfo>,
  ): Int? {
    val scrollable =
      AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
        focused,
        AccessibilityNodeInfoUtils.FILTER_SCROLLABLE,
      ) ?: return null
    val back =
      AccessibilityNodeInfoUtils.supportsAnyAction(
        scrollable,
        AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
        AccessibilityActionCompat.ACTION_SCROLL_UP,
        AccessibilityActionCompat.ACTION_SCROLL_LEFT,
      )
    val forward =
      AccessibilityNodeInfoUtils.supportsAnyAction(
        scrollable,
        AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
        AccessibilityActionCompat.ACTION_SCROLL_DOWN,
        AccessibilityActionCompat.ACTION_SCROLL_RIGHT,
      )
    return scrollPercentOf(back, forward, scrollPercent.applyAsInt(scrollable.unwrap()))
  }

  /**
   * Returns the scroll percentage of a container that can scroll [back] and [forward], with
   * [cached] the percentage from its last scroll event or -1, or null when it's unknown.
   */
  @JvmStatic
  fun scrollPercentOf(back: Boolean, forward: Boolean, cached: Int): Int? =
    when {
      !back && forward -> 0
      back && !forward -> 100
      !back -> null
      cached < 0 -> null
      // It can scroll both ways, so it's neither at the start nor at the end.
      else -> cached.coerceIn(1, 99)
    }

  /** Returns the position of [focused] in its list or table, or null when it's in neither. */
  private fun listPosition(context: Context, focused: AccessibilityNodeInfoCompat?): String? {
    var node = focused
    var depth = 0
    while (node != null && depth < MAX_ITEM_DEPTH) {
      val item = node.collectionItemInfo
      if (item != null) {
        val collection = collectionOf(node)?.collectionInfo
        if (collection != null) {
          text(
              context,
              item.rowIndex,
              item.columnIndex,
              collection.rowCount,
              collection.columnCount,
            )
            ?.let {
              return it
            }
        }
      }
      node = node.parent
      depth++
    }
    return null
  }

  /**
   * Returns the centre of [focused] on screen, in pixels from the top left, to speak, and copies it.
   * The position is the one a macro's Tap at position step takes.
   */
  @JvmStatic
  fun coordinates(context: Context, focused: AccessibilityNodeInfoCompat?): String {
    focused ?: return context.getString(R.string.script_inspect_no_item)
    val bounds = Rect()
    focused.getBoundsInScreen(bounds)
    val text = coordinatesText(context, bounds)
    return if (context.copyToClipboard(context.getString(R.string.shortcut_speak_coordinates), text)) {
      context.getString(R.string.speak_coordinates_copied, text)
    } else {
      text
    }
  }

  /** Returns the centre of [bounds] as X and Y. */
  @JvmStatic
  fun coordinatesText(context: Context, bounds: Rect): String =
    context.getString(R.string.speak_coordinates_value, bounds.centerX(), bounds.centerY())

  private fun collectionOf(item: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    var node = item.parent
    var depth = 0
    while (node != null && depth < MAX_LIST_DEPTH) {
      if (node.collectionInfo != null) {
        return node
      }
      node = node.parent
      depth++
    }
    return null
  }

  /**
   * Returns the position of the item at [row] and [column] of a collection with [rows] rows and
   * [columns] columns: a row and column in a table, an item in a list, or null when the counts are
   * unknown.
   */
  @JvmStatic
  fun text(context: Context, row: Int, column: Int, rows: Int, columns: Int): String? {
    if (rows > 1 && columns > 1 && row in 0 until rows && column in 0 until columns) {
      return context.getString(R.string.speak_position_row_column, row + 1, rows, column + 1, columns)
    }
    if (rows > 1 && row in 0 until rows) {
      return context.getString(R.string.speak_position_item, row + 1, rows)
    }
    if (columns > 1 && column in 0 until columns) {
      return context.getString(R.string.speak_position_item, column + 1, columns)
    }
    if (rows == 1 && columns == 1) {
      return context.getString(R.string.speak_position_item, 1, 1)
    }
    return null
  }
}
