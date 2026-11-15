/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** An icon's container can be read before its child during an automatic scroll. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EarlyScrollOrderTest {
  private var nextId = 1L

  private inner class Node(label: String?, bounds: Rect) :
    AccessibilityNodeInfoCompat(
      AccessibilityNodeInfo.obtain().apply {
        ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
        className = "android.view.View"
        text = label
        isVisibleToUser = true
        isFocusable = label != null
        isClickable = label != null
        setBoundsInScreen(bounds)
        setBoundsInParent(bounds)
      }
    ) {
    val children = mutableListOf<AccessibilityNodeInfoCompat?>()
    var parentNode: Node? = null

    override fun refresh() = true

    override fun getParent(): AccessibilityNodeInfoCompat? = parentNode

    override fun getChildCount() = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = children[index]

    fun add(vararg nodes: Node?): Node {
      nodes.forEach {
        it?.parentNode = this
        children.add(it)
      }
      return this
    }
  }

  private fun checkDirection(forward: Boolean, unreadChild: Boolean = true) {
    val first = Node("First", Rect(0, 0, 100, 100))
    val middle = Node("Middle", Rect(0, 100, 100, 200))
    val middleContainer = Node(null, Rect(0, 100, 100, 200))
    if (unreadChild) middleContainer.add(null)
    val last = Node("Last", Rect(0, 200, 100, 300))
    val list = Node(null, Rect(0, 0, 100, 300)).add(first, middleContainer, last)
    list.addAction(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION)
    val pivot = if (forward) first else last
    val direction = if (forward) SEARCH_FOCUS_FORWARD else SEARCH_FOCUS_BACKWARD
    val filter = Filter.node { it.isClickable }
    val incomplete = OrderedTraversalStrategy(list)
    assertEquals(unreadChild, incomplete.missesChildren())

    // Moving past the empty container would skip Middle and focus the other end of the list.
    assertNull(
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        incomplete, list, pivot, direction, filter, false, list.children.filterNotNull()
      )
    )

    if (unreadChild) middleContainer.children[0] = middle else middleContainer.children.add(middle)
    middle.parentNode = middleContainer
    val complete = OrderedTraversalStrategy(list)
    assertFalse(complete.missesChildren())
    assertEquals(
      middle,
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        complete, list, pivot, direction, filter, false, list.children.filterNotNull()
      ),
    )
  }

  @Test fun unreadNextIconWaitsForTheNextCompleteOrder() = checkDirection(true)

  @Test fun unreadPreviousIconWaitsForTheNextCompleteOrder() = checkDirection(false)

  @Test fun emptyNextItemWaitsForItsIcon() = checkDirection(true, unreadChild = false)

  @Test fun emptyPreviousItemWaitsForItsIcon() = checkDirection(false, unreadChild = false)

  @Test
  fun anEmptyFocusableIndexedItemIsFocusedImmediately() {
    val first = Node("First", Rect(0, 0, 100, 100))
    val control = Node(null, Rect(0, 100, 100, 200)).apply { isClickable = true }
    val list = Node(null, Rect(0, 0, 100, 200)).add(first, control)
    list.addAction(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION)
    assertEquals(
      control,
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        OrderedTraversalStrategy(list), list, first, SEARCH_FOCUS_FORWARD,
        Filter.node { it.isClickable }, false, list.children.filterNotNull()
      ),
    )
  }

  @Test
  fun anEmptyNonIndexedDecorationIsSkippedAsBefore() {
    val first = Node("First", Rect(0, 0, 100, 100))
    val decoration = Node(null, Rect(0, 100, 100, 200))
    val last = Node("Last", Rect(0, 200, 100, 300))
    val list = Node(null, Rect(0, 0, 100, 300)).add(first, decoration, last)
    assertEquals(
      last,
      FocusProcessorForLogicalNavigation.searchFocusRecoveringMissingPivot(
        OrderedTraversalStrategy(list), list, first, SEARCH_FOCUS_FORWARD,
        Filter.node { it.isClickable }, false, list.children.filterNotNull()
      ),
    )
  }
}
