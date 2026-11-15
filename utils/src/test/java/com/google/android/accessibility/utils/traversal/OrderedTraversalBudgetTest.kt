/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.traversal

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OrderedTraversalBudgetTest {
  private class Node(label: String, val children: List<Node> = emptyList()) :
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))) {
    var reads = 0
    var delay = 0L
    init { text = label; className = "android.widget.TextView"; isVisibleToUser = true }
    override fun getChildCount() = children.size
    override fun getChild(index: Int): AccessibilityNodeInfoCompat? {
      reads++
      if (delay > 0) ShadowSystemClock.advanceBy(Duration.ofMillis(delay))
      return children.getOrNull(index)
    }
  }

  @Test fun anExpiredBudgetReadsNothingAndPublishesNoPartialOrder() {
    val root = Node("root", listOf(Node("first"), Node("second")))
    val order = OrderedTraversalStrategy(root) { true }
    assertEquals(0, root.reads)
    assertNull(order.findFocus(root, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }

  @Test fun aWideNativeContainerStopsBeforeReadingTheRestOfItsChildren() {
    val root = Node("root", List(100) { Node("item-$it") }).apply { delay = 100 }
    val end = SystemClock.uptimeMillis() + 300
    val order = OrderedTraversalStrategy(root) { SystemClock.uptimeMillis() >= end }
    assertEquals(3, root.reads)
    assertNull(order.findFocus(root, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }

  @Test fun anUnexpiredBudgetPreservesTheNormalNativeReadingOrder() {
    val first = Node("first")
    val second = Node("second")
    val root = Node("root", listOf(first, second))
    val normal = OrderedTraversalStrategy(root)
    val limited = OrderedTraversalStrategy(root) { false }
    assertEquals(normal.findFocus(first, TraversalStrategy.SEARCH_FOCUS_FORWARD),
      limited.findFocus(first, TraversalStrategy.SEARCH_FOCUS_FORWARD))
    assertEquals(second, limited.findFocus(first, TraversalStrategy.SEARCH_FOCUS_FORWARD))
  }
}
