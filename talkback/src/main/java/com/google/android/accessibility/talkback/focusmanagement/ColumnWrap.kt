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
 * to an item below it, or back to an item above it. Rows are found by where items are on the
 * screen, so each item of a list is a row of its own, and the items of a table, a grid or a row
 * with buttons beside a title share one.
 */
object ColumnWrap {
  const val NONE = 0
  const val FORWARD = 1
  const val BACK = 2

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
      if (a.isEmpty || b.isEmpty || from.windowId != to.windowId || !leavesRow(a, b, forward)) NONE
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

  private fun bounds(node: AccessibilityNodeInfoCompat): Rect =
    Rect().also { node.getBoundsInScreen(it) }
}
