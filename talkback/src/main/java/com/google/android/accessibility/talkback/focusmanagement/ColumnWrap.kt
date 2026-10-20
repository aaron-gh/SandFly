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

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import kotlin.math.abs
import kotlin.math.max

/**
 * Finds when moving to the next or previous item goes from one row of a grid or table to another:
 * forward from the end of a row to the start of the next, or back from the start of a row to the
 * end of the one before.
 *
 * Items with collection item info, such as table cells and the items of grid lists, are compared
 * by their row and column. Grids without it, such as the icons of a home screen or a keypad, are
 * compared by where the items are: items of the same size, in the same container or in rows of the
 * same container, one below the other and back towards the start of the row.
 */
object ColumnWrap {
  const val NONE = 0
  const val FORWARD = 1
  const val BACK = 2

  /** How far up from a focused item its cell may be, as in a web table cell holding text. */
  private const val MAX_CELL_DEPTH = 3

  /** How far up from a cell its table or grid may be. */
  private const val MAX_COLLECTION_DEPTH = 3

  /** How far up from two items, laid out as a grid without collection info, their container is. */
  private const val MAX_CONTAINER_DEPTH = 2

  /** How much two items of a grid may differ in width or height, as a part of the larger. */
  private const val SIZE_TOLERANCE = 0.25f

  /** How long a web page has to move focus after being asked to, in milliseconds. */
  private const val WEB_FOCUS_TIMEOUT_MS = 1000L

  private var webPivot: AccessibilityNodeInfoCompat? = null
  private var webForward = false
  private var webRtl = false
  private var webRequestTime = 0L

  /** The sound of [wrap], or 0 for [NONE]. */
  @JvmStatic
  fun sound(wrap: Int): Int =
    when (wrap) {
      FORWARD -> R.id.column_wrap
      BACK -> R.id.column_wrap_back
      else -> 0
    }

  /**
   * Whether moving [forward] or back from [from] to [to] goes to another row of a grid: [FORWARD],
   * [BACK] or [NONE].
   */
  @JvmStatic
  fun detect(
    from: AccessibilityNodeInfoCompat?,
    to: AccessibilityNodeInfoCompat?,
    forward: Boolean,
    rtl: Boolean,
  ): Int {
    if (from == null || to == null || from == to) {
      return NONE
    }
    return try {
      val fromCell = cellOf(from)
      val toCell = cellOf(to)
      if (fromCell != null && toCell != null) {
        byCells(fromCell, toCell, forward)
      } else {
        byPlace(from, to, forward, rtl)
      }
    } catch (e: RuntimeException) {
      // A node from an app that went away.
      NONE
    }
  }

  /**
   * Remembers that a web page was asked to move focus [forward] or back from [pivot], so that the
   * focus event that follows can be compared with it by [takeWebFocus].
   */
  @JvmStatic
  fun expectWebFocus(pivot: AccessibilityNodeInfoCompat, forward: Boolean, rtl: Boolean) {
    webPivot = pivot
    webForward = forward
    webRtl = rtl
    webRequestTime = SystemClock.uptimeMillis()
  }

  /**
   * Whether the focus [event], the first after [expectWebFocus], went to another row of a grid.
   * Any focus event ends the wait.
   */
  @JvmStatic
  fun takeWebFocus(event: AccessibilityEvent): Int {
    val pivot = webPivot ?: return NONE
    webPivot = null
    if (SystemClock.uptimeMillis() - webRequestTime > WEB_FOCUS_TIMEOUT_MS) {
      return NONE
    }
    return detect(pivot, AccessibilityNodeInfoUtils.toCompat(event.source), webForward, webRtl)
  }

  private class Cell(val collection: AccessibilityNodeInfoCompat, val row: Int, val column: Int)

  private fun byCells(from: Cell, to: Cell, forward: Boolean): Int {
    if (from.collection != to.collection) {
      return NONE
    }
    return when {
      forward && to.row > from.row && to.column < from.column -> FORWARD
      !forward && to.row < from.row && to.column > from.column -> BACK
      else -> NONE
    }
  }

  /** The cell that [node] is, or is in, with the table or grid it is in. */
  private fun cellOf(node: AccessibilityNodeInfoCompat): Cell? {
    var current: AccessibilityNodeInfoCompat? = node
    for (depth in 0..MAX_CELL_DEPTH) {
      val item = current?.collectionItemInfo
      if (current != null && item != null) {
        val collection = collectionOf(current) ?: return null
        return Cell(collection, item.rowIndex, item.columnIndex)
      }
      current = current?.parent ?: return null
    }
    return null
  }

  private fun collectionOf(cell: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    var current = cell.parent
    for (depth in 0 until MAX_COLLECTION_DEPTH) {
      if (current == null) {
        return null
      }
      val info = current.collectionInfo
      if (info != null) {
        // A list, one item per row, has no rows to wrap.
        return if (info.columnCount == 1) null else current
      }
      current = current.parent
    }
    return null
  }

  private fun byPlace(
    from: AccessibilityNodeInfoCompat,
    to: AccessibilityNodeInfoCompat,
    forward: Boolean,
    rtl: Boolean,
  ): Int {
    val a = Rect().also { from.getBoundsInScreen(it) }
    val b = Rect().also { to.getBoundsInScreen(it) }
    if (
      a.isEmpty ||
        b.isEmpty ||
        !similar(a.width(), b.width()) ||
        !similar(a.height(), b.height())
    ) {
      return NONE
    }
    // Rows may overlap a little.
    val slack = minOf(a.height(), b.height()) / 4
    val below = b.top >= a.bottom - slack
    val above = b.bottom <= a.top + slack
    val towardsRowStart = if (rtl) b.centerX() > a.right else b.centerX() < a.left
    val towardsRowEnd = if (rtl) b.centerX() < a.left else b.centerX() > a.right
    val wrap =
      when {
        forward && below && towardsRowStart -> FORWARD
        !forward && above && towardsRowEnd -> BACK
        else -> NONE
      }
    return if (wrap != NONE && shareContainer(from, to)) wrap else NONE
  }

  private fun similar(a: Int, b: Int): Boolean = abs(a - b) <= max(a, b) * SIZE_TOLERANCE

  /**
   * Whether [a] and [b] are in the same container, as many levels down: siblings, or items of rows
   * that are siblings.
   */
  private fun shareContainer(
    a: AccessibilityNodeInfoCompat,
    b: AccessibilityNodeInfoCompat,
  ): Boolean {
    var parentA = a.parent
    var parentB = b.parent
    for (depth in 0 until MAX_CONTAINER_DEPTH) {
      if (parentA == null || parentB == null) {
        return false
      }
      if (parentA == parentB) {
        return true
      }
      parentA = parentA.parent
      parentB = parentB.parent
    }
    return false
  }
}
