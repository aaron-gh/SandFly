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
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * [FocusProcessorForLogicalNavigation.findNodeNearPivotPlace] finds a node to resume a search
 * from when a pivot is missing from a freshly-built local tree, read live by its own on-screen
 * bounds. The tree is laid out with the pivot's place sitting inside a container -- as a
 * RecyclerView item row normally would -- rather than directly under the tree's root, since the
 * container's bounds also span the pivot's place and must not be picked instead of the real
 * nearest node.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FindNodeNearPivotPlaceTest {
  private var nextId = 1L

  private inner class Node(bounds: Rect, focusable: Boolean = true) :
    AccessibilityNodeInfoCompat(info(bounds, focusable)) {
    val kids = ArrayList<Node>()
    var up: Node? = null

    fun add(vararg children: Node): Node {
      for (child in children) {
        kids.add(child)
        child.up = this
      }
      return this
    }

    override fun getChildCount(): Int = kids.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = kids[index]

    override fun getParent(): AccessibilityNodeInfoCompat? = up
  }

  private fun info(bounds: Rect, focusable: Boolean = true): AccessibilityNodeInfo {
    val info = AccessibilityNodeInfo.obtain()
    ReflectionHelpers.setField(info, "mSourceNodeId", nextId++)
    info.setBoundsInScreen(bounds)
    info.isVisibleToUser = true
    info.isClickable = focusable
    info.isFocusable = focusable
    info.className = "android.widget.TextView"
    return info
  }

  /** A standalone node, not part of any tree, read live only for its on-screen bounds. */
  private fun pivotAt(bounds: Rect): Node = Node(bounds)

  /** A plain layout wrapper (e.g. a grid row): not itself an accessibility-focusable unit. */
  private fun container(bounds: Rect): Node = Node(bounds, focusable = false)

  @Test
  fun backward_skipsContainerThatSpansThePivotsPlace() {
    val itemAbove = Node(Rect(0, 100, 200, 200))
    val itemBelow = Node(Rect(0, 400, 200, 500))
    val row = Node(Rect(0, 100, 200, 500)).add(itemAbove, itemBelow)
    val root = Node(Rect(0, 0, 200, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    // The pivot's place is between itemAbove and itemBelow, inside row -- row isn't in the
    // tree built from root once the pivot's own view has been recycled, but row's bounds still
    // span the pivot's place, the same way row's would if the pivot were still a child of it.
    val pivot = pivotAt(Rect(0, 200, 200, 300))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_BACKWARD, /* rtl= */ false)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_BACKWARD, null)
    assertEquals(itemAbove, resumed)
  }

  @Test
  fun forward_skipsContainerThatSpansThePivotsPlace() {
    val itemAbove = Node(Rect(0, 100, 200, 200))
    val itemBelow = Node(Rect(0, 400, 200, 500))
    val row = Node(Rect(0, 100, 200, 500)).add(itemAbove, itemBelow)
    val root = Node(Rect(0, 0, 200, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    val pivot = pivotAt(Rect(0, 200, 200, 300))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD, /* rtl= */ false)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_FORWARD, null)
    assertEquals(itemBelow, resumed)
  }

  /**
   * A horizontal row (a carousel, or a grid row) has siblings that share the same top/bottom, so
   * comparing bounds on the vertical axis alone can't tell which side of the pivot's place they're
   * on. Rotates the nested-container scenario above by 90 degrees: the pivot's place sits in the
   * gap between two side-by-side items instead of two stacked ones.
   */
  @Test
  fun backward_ordersRowSiblingsHorizontallyInsteadOfOnlyByVerticalOverlap() {
    val itemLeft = Node(Rect(0, 0, 100, 200))
    val itemRight = Node(Rect(300, 0, 400, 200))
    val row = Node(Rect(0, 0, 400, 200)).add(itemLeft, itemRight)
    val root = Node(Rect(0, 0, 400, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    // The pivot's place is between itemLeft and itemRight, inside row -- same top/bottom as both,
    // so only their horizontal position (not just whether their bounds reach the pivot's) tells
    // them apart.
    val pivot = pivotAt(Rect(150, 0, 250, 200))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_BACKWARD, /* rtl= */ false)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_BACKWARD, null)
    assertEquals(itemLeft, resumed)
  }

  @Test
  fun forward_ordersRowSiblingsHorizontallyInsteadOfOnlyByVerticalOverlap() {
    val itemLeft = Node(Rect(0, 0, 100, 200))
    val itemRight = Node(Rect(300, 0, 400, 200))
    val row = Node(Rect(0, 0, 400, 200)).add(itemLeft, itemRight)
    val root = Node(Rect(0, 0, 400, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    val pivot = pivotAt(Rect(150, 0, 250, 200))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD, /* rtl= */ false)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_FORWARD, null)
    assertEquals(itemRight, resumed)
  }

  /**
   * An ordinary single-column item can vertically overlap the pivot's stale place (e.g. mid-
   * scroll, before the list settles) without being a real row-mate: it shares the pivot's column
   * instead of sitting beside it. The row/grid tie-break must not mistake it for one -- that would
   * stop the forward scan right there and lose both this item and the real next item after it.
   */
  @Test
  fun forward_treatsAColumnOverlapAsAnOrdinarySiblingNotARowMate() {
    val itemOverlap = Node(Rect(0, 150, 200, 250))
    val itemBelow = Node(Rect(0, 400, 200, 500))
    val row = Node(Rect(0, 100, 200, 500)).add(itemOverlap, itemBelow)
    val root = Node(Rect(0, 0, 200, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    // The pivot's place vertically overlaps itemOverlap's -- both span the same column -- rather
    // than sitting cleanly between two stacked items as in the tests above.
    val pivot = pivotAt(Rect(0, 200, 200, 300))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD, /* rtl= */ false)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_FORWARD, null)
    assertEquals(itemBelow, resumed)
  }

  /**
   * A real grid has more than one row. The pivot's own row-mate ends that row, so recovering
   * from it must cross into the next row's first item instead of stopping at the row boundary --
   * exercising the same path the single-row tests above don't: the row container (not the
   * pivot's own ancestor) is what the ordinary top/bottom check has to clear.
   */
  @Test
  fun forward_transitionsFromOneGridRowToTheNextWhenThePivotIsMissing() {
    val itemA = Node(Rect(0, 0, 100, 100))
    val row1 = container(Rect(0, 0, 200, 100)).add(itemA)
    val itemC = Node(Rect(0, 100, 100, 200))
    val itemD = Node(Rect(100, 100, 200, 200))
    val row2 = container(Rect(0, 100, 200, 200)).add(itemC, itemD)
    val root = container(Rect(0, 0, 200, 200)).add(row1, row2)
    val strategy = OrderedTraversalStrategy(root)
    // The pivot's place is row1's second cell -- missing from the tree, so row1 only has itemA.
    val pivot = pivotAt(Rect(100, 0, 200, 100))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD, /* rtl= */ false)

    // itemA is the real anchor: the caller's own further search from it (with its real node
    // filter, not exercised by this lower-level test) is what actually crosses into row2.
    assertEquals(itemA, nearest)
  }

  /** Same grid, recovering backward across the row boundary instead. */
  @Test
  fun backward_transitionsFromOneGridRowToThePreviousWhenThePivotIsMissing() {
    val itemA = Node(Rect(0, 0, 100, 100))
    val itemB = Node(Rect(100, 0, 200, 100))
    val row1 = container(Rect(0, 0, 200, 100)).add(itemA, itemB)
    val itemD = Node(Rect(100, 100, 200, 200))
    val row2 = container(Rect(0, 100, 200, 200)).add(itemD)
    val root = container(Rect(0, 0, 200, 200)).add(row1, row2)
    val strategy = OrderedTraversalStrategy(root)
    // The pivot's place is row2's first cell -- missing from the tree, so row2 only has itemD.
    val pivot = pivotAt(Rect(0, 100, 100, 200))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_BACKWARD, /* rtl= */ false)

    // itemD is the real anchor: the caller's own further search from it (with its real node
    // filter, not exercised by this lower-level test) is what actually crosses back into row1.
    assertEquals(itemD, nearest)
  }

  /**
   * Same row, but in an RTL layout: reading order runs right-to-left, so the item with the
   * larger X coordinate is the one that comes first.
   */
  @Test
  fun backward_mirrorsRowOrderForRtl() {
    val itemFirst = Node(Rect(300, 0, 400, 200))
    val itemSecond = Node(Rect(0, 0, 100, 200))
    val row = Node(Rect(0, 0, 400, 200)).add(itemFirst, itemSecond)
    val root = Node(Rect(0, 0, 400, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    val pivot = pivotAt(Rect(150, 0, 250, 200))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_BACKWARD, /* rtl= */ true)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_BACKWARD, null)
    assertEquals(itemFirst, resumed)
  }

  /** Same row and RTL layout, recovering forward instead. */
  @Test
  fun forward_mirrorsRowOrderForRtl() {
    val itemFirst = Node(Rect(300, 0, 400, 200))
    val itemSecond = Node(Rect(0, 0, 100, 200))
    val row = Node(Rect(0, 0, 400, 200)).add(itemFirst, itemSecond)
    val root = Node(Rect(0, 0, 400, 1000)).add(row)
    val strategy = OrderedTraversalStrategy(root)
    val pivot = pivotAt(Rect(150, 0, 250, 200))

    val nearest =
        FocusProcessorForLogicalNavigation.findNodeNearPivotPlace(
            strategy, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD, /* rtl= */ true)

    val resumed =
        TraversalStrategyUtils.searchFocus(
            strategy, nearest, TraversalStrategy.SEARCH_FOCUS_FORWARD, null)
    assertEquals(itemSecond, resumed)
  }
}
