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

/**
 * Finds when moving to the next or previous item leaves the row of items the focus is in: forward
 * to an item below it, or back to an item above it, when the row has another item beside the
 * focused one. Rows are found by where items are on the screen, so this works for tables, grids,
 * home screens and lists whose rows hold several items, such as a title with buttons beside it. A
 * list with one item in each row has no row to leave.
 */
object ColumnWrap {
  const val NONE = 0
  const val FORWARD = 1
  const val BACK = 2

  /** How far up from the focused item the container of its row may be. */
  private const val MAX_ROW_DEPTH = 3

  /** The most items looked at to find another item in the focused item's row. */
  private const val MAX_ROW_NODES = 200

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
   * Whether moving [forward] or back from [from] to [to] leaves the row [from] is in: [FORWARD],
   * [BACK] or [NONE].
   */
  @JvmStatic
  @Suppress("UNUSED_PARAMETER")
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
      val a = bounds(from)
      val b = bounds(to)
      if (a.isEmpty || b.isEmpty || !leavesRow(a, b, forward) || !hasItemBeside(from, a)) NONE
      else if (forward) FORWARD
      else BACK
    } catch (e: RuntimeException) {
      // A node from an app that went away.
      NONE
    }
  }

  /**
   * Whether an item at [to] is past the row of an item at [from]: below it going [forward], or
   * above it going back. Its middle is past the edge of [from].
   */
  @JvmStatic
  fun leavesRow(from: Rect, to: Rect, forward: Boolean): Boolean =
    if (forward) to.centerY() >= from.bottom else to.centerY() <= from.top

  /** Whether an item at [other] is beside an item at [item], in its row. */
  @JvmStatic
  fun isBeside(other: Rect, item: Rect): Boolean =
    other.centerY() > item.top &&
      other.centerY() < item.bottom &&
      (other.right <= item.left || other.left >= item.right)

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

  /**
   * Whether the row of [node], at [place], has another item that can take focus beside it, in the
   * containers of [node] up to [MAX_ROW_DEPTH] levels up.
   */
  private fun hasItemBeside(node: AccessibilityNodeInfoCompat, place: Rect): Boolean {
    var searched: AccessibilityNodeInfoCompat = node
    var container = node.parent
    var looked = 0
    for (depth in 0 until MAX_ROW_DEPTH) {
      if (container == null) return false
      val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
      // The container searched one level down is left out.
      for (i in 0 until container.childCount) {
        container.getChild(i)?.takeIf { it != searched }?.let(queue::add)
      }
      while (queue.isNotEmpty() && looked < MAX_ROW_NODES) {
        val candidate = queue.removeFirst()
        looked++
        if (candidate == node || !candidate.isVisibleToUser) continue
        val bounds = bounds(candidate)
        if (bounds.isEmpty || bounds.bottom <= place.top || bounds.top >= place.bottom) continue
        if (
          isBeside(bounds, place) && AccessibilityNodeInfoUtils.isAccessibilityFocusable(candidate)
        ) {
          return true
        }
        for (i in 0 until candidate.childCount) candidate.getChild(i)?.let(queue::add)
      }
      if (looked >= MAX_ROW_NODES) return false
      searched = container
      container = container.parent
    }
    return false
  }

  private fun bounds(node: AccessibilityNodeInfoCompat): Rect =
    Rect().also { node.getBoundsInScreen(it) }
}
