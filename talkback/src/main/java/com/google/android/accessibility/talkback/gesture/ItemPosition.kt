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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R

/** The Speak position command: where the focused item is in its list or table. */
object ItemPosition {

  // How far up from the focused item to look for the list item, and from there for the list.
  private const val MAX_ITEM_DEPTH = 6
  private const val MAX_LIST_DEPTH = 3

  /** Returns the position of [focused] in its list or table, to speak. */
  @JvmStatic
  fun describe(context: Context, focused: AccessibilityNodeInfoCompat?): String {
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
    return context.getString(R.string.speak_position_none)
  }

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
