/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.directtouch

import android.graphics.Rect
import android.graphics.Region
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

object DirectTouchRegions {
  /**
   * Whether touches in a window of [windowType] stay with the screen reader. System windows and
   * accessibility overlays always do, and the keyboard does unless [directTyping] is on.
   */
  fun shouldExcludeWindow(windowType: Int, directTyping: Boolean): Boolean {
    if (
      windowType == AccessibilityWindowInfo.TYPE_SYSTEM ||
        windowType == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
    ) {
      return true
    }
    return !directTyping && windowType == AccessibilityWindowInfo.TYPE_INPUT_METHOD
  }

  /**
   * Whether an event of [eventType] should make direct touch look at the screen again once things
   * settle, and send its region again if it is on. The system keeps one passthrough region per
   * display for all services, and some clear it on every window change (Narwhal does, 50 ms after
   * the event), so the last writer wins.
   */
  fun reassertsRegion(eventType: Int): Boolean =
    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
      eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

  /**
   * Whether a window with these bounds on a display of [displayWidth] by [displayHeight] pixels is
   * a navigation bar: a thin strip along the bottom edge, or along a side edge in landscape, that
   * spans at least half the display. The window can be just the cluster of buttons, so it need not
   * span all of it. The status bar and the notification shade are not navigation bars.
   */
  fun isNavigationBar(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    displayWidth: Int,
    displayHeight: Int,
  ): Boolean {
    val width = right - left
    val height = bottom - top
    if (width <= 0 || height <= 0) {
      return false
    }
    val alongBottom =
      bottom >= displayHeight && height * 5 <= displayHeight && width * 2 >= displayWidth
    val alongSide =
      (left <= 0 || right >= displayWidth) &&
        width * 5 <= displayWidth &&
        height * 2 >= displayHeight
    return alongBottom || alongSide
  }

  /** The whole display minus the [excluded] rectangles. */
  fun passthroughRegion(displayBounds: Rect, excluded: List<Rect>): Region {
    val region = Region(displayBounds)
    excluded.forEach { region.op(it, Region.Op.DIFFERENCE) }
    return region
  }

  /**
   * The [areas] within [window], minus the [excluded] rectangles, or null when nothing is left.
   */
  fun areasRegion(window: Rect, areas: List<Rect>, excluded: List<Rect>): Region? {
    val region = Region()
    areas.forEach { region.op(it, Region.Op.UNION) }
    region.op(window, Region.Op.INTERSECT)
    excluded.forEach { region.op(it, Region.Op.DIFFERENCE) }
    return region.takeUnless { it.isEmpty }
  }
}
