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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The swipe Scroll left and Scroll right send to a pager that has no scroll actions, such as the
 * folder tabs of Solid Explorer (report 540), sent to the app as it is; see [SyntheticTouches].
 * Only a pager around the focus gets it: a sideways swipe elsewhere could dismiss or archive a list
 * item.
 */
object SidewaysSwipe {
  private val handler = Handler(Looper.getMainLooper())
  private var pending: Runnable? = null

  /**
   * Sends [line], from [swipeLine], once the gesture that asked for it has ended. Returns false
   * when SandFly can't send touches.
   */
  @JvmStatic
  fun send(service: AccessibilityService, line: FloatArray): Boolean {
    if (!SyntheticTouches.isAvailable()) return false
    cancel()
    val path =
      Path().apply {
        moveTo(line[1], line[0])
        lineTo(line[2], line[0])
      }
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
        .build()
    val send = Runnable {
      pending = null
      SyntheticTouches.dispatch(service, gesture, SWIPE_DURATION_MS)
    }
    pending = send
    handler.postDelayed(send, GESTURE_END_WAIT_MS)
    return true
  }

  /** Stops a swipe that is waiting, such as when the user touches the screen or presses a key. */
  @JvmStatic
  fun cancel() {
    pending?.let {
      handler.removeCallbacks(it)
      pending = null
    }
  }

  /**
   * The closest ancestor of [node] that shows one page and holds others, as a pager does: the
   * child that holds [node] fills it, and another child of the same class isn't on the screen.
   */
  @JvmStatic
  fun pagerHolding(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    var child = node
    var parent = node.parent
    val parentBounds = Rect()
    val childBounds = Rect()
    var depth = 0
    while (parent != null && depth < MAX_DEPTH) {
      parent.getBoundsInScreen(parentBounds)
      child.getBoundsInScreen(childBounds)
      if (
        !parentBounds.isEmpty &&
          childBounds.width() * 10 >= parentBounds.width() * 9 &&
          childBounds.height() * 10 >= parentBounds.height() * 9 &&
          holdsHiddenPage(parent, child)
      ) {
        return parent
      }
      child = parent
      parent = parent.parent
      depth++
    }
    return null
  }

  private fun holdsHiddenPage(
    parent: AccessibilityNodeInfoCompat,
    page: AccessibilityNodeInfoCompat,
  ): Boolean {
    val className = page.className ?: return false
    for (i in 0 until minOf(parent.childCount, MAX_PAGES)) {
      val other = parent.getChild(i) ?: continue
      if (other != page && !other.isVisibleToUser && className == other.className) return true
    }
    return false
  }

  /**
   * The swipe over [pagerBounds] on [screen], kept away from the screen edges: its y, then the x
   * it starts and ends at, or null when too little of the pager is on the screen. Scroll left moves
   * the content left to show the page on the right, as the page action does, so the finger moves
   * left.
   */
  @JvmStatic
  fun swipeLine(pagerBounds: Rect, screen: Rect, left: Boolean): FloatArray? {
    val margin = screen.width() * EDGE_FRACTION
    val area = Rect(screen)
    area.inset(margin.toInt(), 0)
    if (area.isEmpty || !area.intersect(pagerBounds) || area.width() < margin) return null
    val y = area.exactCenterY()
    val right = area.left + area.width() * RIGHT_FRACTION
    val leftX = area.left + area.width() * LEFT_FRACTION
    return if (left) floatArrayOf(y, right, leftX) else floatArrayOf(y, leftX, right)
  }

  /** How many levels above the focus a pager is looked for. */
  private const val MAX_DEPTH = 30

  /** How many children of a pager are looked at for another page. */
  private const val MAX_PAGES = 30

  /** The part of the screen width on each side the swipe stays out of, for system gestures. */
  private const val EDGE_FRACTION = 0.08f

  /** Where the swipe starts and ends, as shares of the width it goes over. */
  private const val LEFT_FRACTION = 0.25f
  private const val RIGHT_FRACTION = 0.75f

  private const val SWIPE_DURATION_MS = 250L

  /** How long the gesture that asked for the swipe has to end. */
  private const val GESTURE_END_WAIT_MS = 150L
}
