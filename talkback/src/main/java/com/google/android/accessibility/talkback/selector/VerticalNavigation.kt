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

package com.google.android.accessibility.talkback.selector

import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils

/**
 * Finds the item below or above the focused one for the Vertical navigation reading control, which
 * leaves out the other items of the same row, such as the buttons beside a video's title or a
 * file's name.
 */
object VerticalNavigation {

  /**
   * Returns the first item after [focus], in reading order, whose middle is below [focus], or the
   * first item of the row above [focus], or null when there is none on screen.
   */
  @JvmStatic
  fun target(focus: AccessibilityNodeInfoCompat, forward: Boolean): AccessibilityNodeInfoCompat? {
    val root = AccessibilityNodeInfoUtils.getRoot(focus) ?: return null
    val strategy =
      OrderedTraversalStrategy(
        root,
        /* includeChildrenOfNodesWithWebActions= */ true,
        /* makeFabFirst= */ false,
      )
    val focusable: Filter<AccessibilityNodeInfoCompat>? =
      NavigationTarget.createNodeFilter(
        NavigationTarget.TARGET_DEFAULT,
        strategy.speakingNodesCache,
      )
    val from = bounds(focus)
    val direction =
      if (forward) TraversalStrategy.SEARCH_FOCUS_FORWARD
      else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val target =
      TraversalStrategyUtils.searchFocus(
        strategy,
        focus,
        direction,
        filter { focusable?.accept(it) == true && isOnOtherRow(bounds(it), from, forward) },
      ) ?: return null
    if (forward) {
      return target
    }
    // Going up lands on the start of the row, as going down does.
    val row = bounds(target)
    var start: AccessibilityNodeInfoCompat = target
    while (true) {
      val before =
        TraversalStrategyUtils.searchFocus(
          strategy,
          start,
          TraversalStrategy.SEARCH_FOCUS_BACKWARD,
          filter { focusable?.accept(it) == true },
        ) ?: break
      if (!isOnSameRow(bounds(before), row)) break
      start = before
    }
    return start
  }

  /**
   * Whether an item with [bounds] is below [from], going [forward], or above it: its middle is past
   * the edge of [from].
   */
  @JvmStatic
  fun isOnOtherRow(bounds: Rect, from: Rect, forward: Boolean): Boolean =
    if (forward) bounds.centerY() >= from.bottom else bounds.centerY() <= from.top

  /** Whether an item with [bounds] is in the row of the item with [row]: its middle is within it. */
  @JvmStatic
  fun isOnSameRow(bounds: Rect, row: Rect): Boolean =
    bounds.centerY() > row.top && bounds.centerY() < row.bottom

  private fun bounds(node: AccessibilityNodeInfoCompat): Rect =
    Rect().also { node.getBoundsInScreen(it) }

  private fun filter(accept: (AccessibilityNodeInfoCompat) -> Boolean) =
    object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat?): Boolean =
        node != null && accept(node)
    }
}
