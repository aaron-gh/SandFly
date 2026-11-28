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

package com.google.android.accessibility.utils.traversal

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.ScrollableNodeInfo
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * The edge check keeps the node its search found in [TraversalStrategyUtils.EdgeSearch], for a
 * search of the same kind from the same pivot to take.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdgeSearchTest {
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

  private fun list() =
    Node(
      "android.widget.ListView",
      AccessibilityAction.ACTION_SCROLL_FORWARD,
      AccessibilityAction.ACTION_SCROLL_BACKWARD,
    )

  private fun isEdge(
    pivot: AccessibilityNodeInfoCompat,
    strategy: OrderedTraversalStrategy,
    edgeSearch: TraversalStrategyUtils.EdgeSearch,
    direction: Int = SEARCH_FOCUS_FORWARD,
    ignoreDescendants: Boolean = false,
  ): Boolean =
    TraversalStrategyUtils.isAutoScrollEdgeListItem(
      pivot,
      ScrollableNodeInfo.findScrollableNodeForDirection(
        direction,
        pivot,
        /* includeSelf= */ false,
        /* rtl= */ false,
      )!!,
      ignoreDescendants,
      direction,
      /* focusFinder= */ null,
      strategy,
      edgeSearch,
    )

  @Test
  fun keepsTheNextItemAwayFromTheEdge() {
    val first = item()
    val second = item()
    val root = Node("android.widget.FrameLayout").add(list().add(first, second, item()))
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(/* alsoAccepted= */ null)

    assertFalse(isEdge(first, strategy, edgeSearch))

    assertTrue(edgeSearch.holds(strategy, first, SEARCH_FOCUS_FORWARD, false))
    assertEquals(second, edgeSearch.found())
  }

  @Test
  fun holdsOnlyTheSameSearch() {
    val first = item()
    val second = item()
    val root = Node("android.widget.FrameLayout").add(list().add(first, second, item()))
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(/* alsoAccepted= */ null)

    assertFalse(isEdge(first, strategy, edgeSearch))

    val otherOrder = OrderedTraversalStrategy(root)
    assertFalse(edgeSearch.holds(otherOrder, first, SEARCH_FOCUS_FORWARD, false))
    assertFalse(edgeSearch.holds(strategy, second, SEARCH_FOCUS_FORWARD, false))
    assertFalse(edgeSearch.holds(strategy, first, SEARCH_FOCUS_BACKWARD, false))
    assertFalse(edgeSearch.holds(strategy, first, SEARCH_FOCUS_FORWARD, true))
    edgeSearch.clear()
    assertFalse(edgeSearch.holds(strategy, first, SEARCH_FOCUS_FORWARD, false))
  }

  @Test
  fun keepsNoNodeAtTheEndOfTheWindow() {
    val last = item()
    val root = Node("android.widget.FrameLayout").add(list().add(item(), last))
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(/* alsoAccepted= */ null)

    assertTrue(isEdge(last, strategy, edgeSearch))

    assertTrue(edgeSearch.holds(strategy, last, SEARCH_FOCUS_FORWARD, false))
    assertNull(edgeSearch.found())
  }

  @Test
  fun passingANodeTheCallerAlsoAcceptsHoldsNothing() {
    val first = item()
    // Takes no focus, so the edge check passes it, where the caller's search would stop.
    val stop = Node("android.view.View")
    val root = Node("android.widget.FrameLayout").add(list().add(first, stop, item()))
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(Filter.node { it == stop })

    assertFalse(isEdge(first, strategy, edgeSearch))

    assertFalse(edgeSearch.holds(strategy, first, SEARCH_FOCUS_FORWARD, false))
  }

  @Test
  fun aNodeTheCallerAlsoAcceptsAfterTheFoundOneHoldsTheFoundOne() {
    val first = item()
    val second = item()
    val stop = Node("android.view.View")
    val root = Node("android.widget.FrameLayout").add(list().add(first, second, stop))
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(Filter.node { it == stop })

    assertFalse(isEdge(first, strategy, edgeSearch))

    assertTrue(edgeSearch.holds(strategy, first, SEARCH_FOCUS_FORWARD, false))
    assertEquals(second, edgeSearch.found())
  }

  @Test
  fun aPivotOutsideTheListSearchesNothing() {
    val outside = item()
    val inList = item()
    val list = list().add(inList)
    Node("android.widget.FrameLayout").add(outside, list)
    val root = list.parentNode!!
    val strategy = OrderedTraversalStrategy(root)
    val edgeSearch = TraversalStrategyUtils.EdgeSearch(/* alsoAccepted= */ null)

    assertFalse(
      TraversalStrategyUtils.isAutoScrollEdgeListItem(
        outside,
        ScrollableNodeInfo.findScrollableNodeForDirection(
          SEARCH_FOCUS_FORWARD,
          inList,
          /* includeSelf= */ false,
          /* rtl= */ false,
        )!!,
        /* ignoreDescendantsOfPivot= */ false,
        SEARCH_FOCUS_FORWARD,
        /* focusFinder= */ null,
        strategy,
        edgeSearch,
      )
    )
    assertFalse(edgeSearch.holds(strategy, outside, SEARCH_FOCUS_FORWARD, false))
  }
}
