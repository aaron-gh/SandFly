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

import android.graphics.Rect
import android.os.SystemClock
import android.text.TextUtils
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Items under the finger in app windows whose hover events find nothing, found from the window's
 * nodes.
 *
 * Some apps, such as Threads on its main tabs, report touch exploration's hover only for an empty
 * container as large as the window, while their items are in the node tree with correct bounds.
 * When SandFly detects gestures itself, it knows where the finger was when it requested touch
 * exploration. A hover entering such a container then focuses the item under that point instead.
 *
 * Everything here runs on the main thread.
 */
@MainThread
object HoverlessWindows {
  private const val TAG = "HoverlessWindows"

  /** The share of its window's area a hovered container covers for its hover to count as empty. */
  @VisibleForTesting const val MOST_OF_WINDOW = 0.75f

  /** How long after touch exploration starts its finger position is used for a hover. */
  private const val START_POINT_MS = 1_000L

  private var startX = 0f
  private var startY = 0f
  private var startTimeMs = 0L
  private var hasStart = false

  /**
   * Notes where the finger is as SandFly requests touch exploration for it, from the touch's motion
   * events, which stop once touch exploration starts.
   */
  @JvmStatic
  fun onTouchExplorationRequested(x: Float, y: Float) {
    startX = x
    startY = y
    startTimeMs = SystemClock.uptimeMillis()
    hasStart = true
  }

  /** Forgets the finger position, as a new touch starts or SandFly stops detecting gestures. */
  @JvmStatic
  fun clearStart() {
    hasStart = false
  }

  /**
   * Returns the item to focus for a hover entering [touched], which touch exploration found nothing
   * to focus for, or null. The item is the one at the finger's position as touch exploration
   * started, when [touched] is an empty container covering most of an app window.
   */
  @JvmStatic
  fun itemUnderFinger(touched: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (!hasStart || SystemClock.uptimeMillis() - startTimeMs > START_POINT_MS) {
      return null
    }
    if (
      !TextUtils.isEmpty(touched.text) ||
        !TextUtils.isEmpty(touched.contentDescription) ||
        AccessibilityNodeInfoUtils.isActionableForAccessibility(touched) ||
        touched.isScreenReaderFocusable
    ) {
      return null
    }
    val window = AccessibilityNodeInfoUtils.getWindow(touched) ?: return null
    if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) {
      return null
    }
    val windowBounds = Rect().also(window::getBoundsInScreen)
    val touchedBounds = Rect().also(touched::getBoundsInScreen)
    val x = startX.toInt()
    val y = startY.toInt()
    if (!windowBounds.contains(x, y) || !coversMostOf(touchedBounds, windowBounds)) {
      return null
    }
    val root = window.root ?: return null
    val item = KeyboardExploration.nodeUnder(root, startX, startY)
    if (item == null || item == touched) {
      return null
    }
    LogUtils.d(TAG, "Hover entered an empty container; the item at %d, %d is focused", x, y)
    return item
  }

  @VisibleForTesting
  fun coversMostOf(node: Rect, window: Rect): Boolean {
    val overlap = Rect()
    if (window.isEmpty || !overlap.setIntersect(node, window)) {
      return false
    }
    val overlapArea = overlap.width().toLong() * overlap.height()
    val windowArea = window.width().toLong() * window.height()
    return overlapArea >= MOST_OF_WINDOW * windowArea
  }

  @VisibleForTesting
  fun clear() {
    hasStart = false
  }
}
