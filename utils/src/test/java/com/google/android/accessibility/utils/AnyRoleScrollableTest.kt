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

package com.google.android.accessibility.utils

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Moving to the next item scrolls a container of any role, such as a NestedScrollView or a
 * Compose scrollable, when no list, grid or scroll view can scroll, but never a pager or a
 * container that scrolls only sideways.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnyRoleScrollableTest {
  private var nextId = 1L

  /** A node whose parent and children are set by the test. */
  private inner class Node(className: String, vararg actions: AccessibilityAction) :
    AccessibilityNodeInfoCompat(info(className, actions)) {
    var parentNode: Node? = null
    val children = mutableListOf<Node>()

    override fun getParent(): AccessibilityNodeInfoCompat? = parentNode

    override fun getChildCount(): Int = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = children[index]

    fun add(vararg nodes: Node): Node {
      nodes.forEach {
        it.parentNode = this
        children.add(it)
      }
      return this
    }
  }

  private fun info(className: String, actions: Array<out AccessibilityAction>) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = true
      setBoundsInScreen(Rect(0, 0, 100, 100))
      actions.forEach { addAction(it) }
      isScrollable = actions.any { it.id != AccessibilityNodeInfo.ACTION_CLICK }
    }

  private fun item() =
    Node("android.widget.TextView", AccessibilityAction.ACTION_CLICK).apply {
      isFocusable = true
      isClickable = true
      text = "item"
    }

  private fun verticalScroller(className: String = "android.widget.FrameLayout") =
    Node(
      className,
      AccessibilityAction.ACTION_SCROLL_FORWARD,
      AccessibilityAction.ACTION_SCROLL_DOWN,
    )

  private fun findAnyRole(pivot: AccessibilityNodeInfoCompat, direction: Int) =
    ScrollableNodeInfo.findAnyRoleScrollableNodeForDirection(
      direction,
      pivot,
      /* includeSelf= */ false,
      /* rtl= */ false,
    )

  private fun findList(pivot: AccessibilityNodeInfoCompat, direction: Int) =
    ScrollableNodeInfo.findScrollableNodeForDirection(
      direction,
      pivot,
      /* includeSelf= */ false,
      /* rtl= */ false,
    )

  @Test
  fun aViewGroupThatScrollsDownIsFoundOnlyByTheAnyRoleSearch() {
    val pivot = item()
    val scroller = verticalScroller().add(pivot)

    assertNull(findList(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD))
    val found = findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD)
    assertNotNull(found)
    assertEquals(scroller, found!!.node)
    assertTrue(found.isAnyRole)
    assertSame(AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL_ANY_ROLE, found.containerFilter)
  }

  @Test
  fun aComposeScrollableIsFoundForDown() {
    val pivot = item()
    val scroller = verticalScroller("android.view.View").add(pivot)

    assertEquals(scroller, findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_DOWN)?.node)
  }

  @Test
  fun aListIsStillFoundByTheListSearch() {
    val pivot = item()
    val list =
      Node("android.widget.ListView", AccessibilityAction.ACTION_SCROLL_FORWARD).add(pivot)

    val found = findList(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD)
    assertEquals(list, found?.node)
    assertFalse(found!!.isAnyRole)
    assertSame(AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL, found.containerFilter)
  }

  @Test
  fun leftAndRightFindNothing() {
    val pivot = item()
    Node(
        "android.widget.FrameLayout",
        AccessibilityAction.ACTION_SCROLL_FORWARD,
        AccessibilityAction.ACTION_SCROLL_RIGHT,
        AccessibilityAction.ACTION_SCROLL_DOWN,
      )
      .add(pivot)

    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_RIGHT))
    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_LEFT))
  }

  @Test
  fun aPagerAndASidewaysCarouselArePassedForTheVerticalScrollerAroundThem() {
    val pivot = item()
    val carousel =
      Node(
          "android.widget.FrameLayout",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_SCROLL_RIGHT,
        )
        .add(pivot)
    val pager =
      Node(
          "android.widget.FrameLayout",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_PAGE_DOWN,
        )
        .add(carousel)
    val outer = verticalScroller().add(pager)

    assertEquals(outer, findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD)?.node)
  }

  @Test
  fun aPagerAloneIsNotScrolled() {
    val pivot = item()
    Node(
        "android.widget.FrameLayout",
        AccessibilityAction.ACTION_SCROLL_FORWARD,
        AccessibilityAction.ACTION_PAGE_RIGHT,
      )
      .add(pivot)

    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }

  @Test
  fun aOneRowCollectionIsNotScrolled() {
    val pivot = item()
    val row =
      Node("android.widget.FrameLayout", AccessibilityAction.ACTION_SCROLL_FORWARD).add(pivot)
    row.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, 6, false))

    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }

  @Test
  fun aScrollerOffScreenIsNotScrolled() {
    val pivot = item()
    verticalScroller().add(pivot).isVisibleToUser = false

    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }

  @Test
  fun aScrollerThatCannotScrollBackIsNotFoundForBackward() {
    val pivot = item()
    verticalScroller().add(pivot)

    assertNull(findAnyRole(pivot, TraversalStrategy.SEARCH_FOCUS_BACKWARD))
  }

  @Test
  fun theAnyRoleFilterLeavesOutSeekBarsAndPickers() {
    val filter = AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL_ANY_ROLE
    assertTrue(filter.accept(verticalScroller()))
    assertFalse(filter.accept(verticalScroller("android.widget.SeekBar")))
    assertFalse(filter.accept(verticalScroller("android.widget.NumberPicker")))
    assertFalse(filter.accept(verticalScroller("android.widget.EditText")))
    assertFalse(filter.accept(verticalScroller("android.webkit.WebView")))
    assertFalse(filter.accept(item()))
  }

  @Test
  fun theLastItemInAnAnyRoleScrollerIsAtItsEdge() {
    val first = item()
    val last = item()
    val scroller = verticalScroller().add(first, last)
    val root = Node("android.widget.FrameLayout").add(scroller)
    val strategy = OrderedTraversalStrategy(root)

    val forward = TraversalStrategy.SEARCH_FOCUS_FORWARD
    val found = findAnyRole(last, forward)!!
    assertTrue(
      TraversalStrategyUtils.isAutoScrollEdgeListItem(
        last,
        found,
        /* ignoreDescendantsOfPivot= */ false,
        forward,
        /* focusFinder= */ null,
        strategy,
      )
    )
    // The next item is in the same scroller, so the move needn't scroll.
    assertFalse(
      TraversalStrategyUtils.isAutoScrollEdgeListItem(
        first,
        findAnyRole(first, forward)!!,
        /* ignoreDescendantsOfPivot= */ false,
        forward,
        /* focusFinder= */ null,
        strategy,
      )
    )
    assertEquals(scroller, found.node)
  }
}
