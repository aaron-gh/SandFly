/* Copyright 2026 Backtalk contributors. Licensed under the Apache License, Version 2.0. */

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusAfterScrollTest {
  @Test
  fun invalidPivotFocusesFirstVisibleItemInScrollDirection() {
    val root = node("root")
    val first = node("first", root)
    node("second", root)
    val strategy = OrderedTraversalStrategy(root)
    val filter = object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat): Boolean = node.isFocusable
    }

    val result = FocusProcessorForLogicalNavigation.findFocusAfterInvalidPivot(
      strategy,
      root,
      TraversalStrategy.SEARCH_FOCUS_FORWARD,
      filter,
    )

    assertEquals(first.text, result?.text)
  }

  @Test
  fun invalidPivotFocusesLastVisibleItemWhenMovingBackward() {
    val root = node("root")
    node("first", root)
    val last = node("last", root)
    val strategy = OrderedTraversalStrategy(root)
    val filter = object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat): Boolean = node.isFocusable
    }

    val result = FocusProcessorForLogicalNavigation.findFocusAfterInvalidPivot(
      strategy,
      root,
      TraversalStrategy.SEARCH_FOCUS_BACKWARD,
      filter,
    )

    assertEquals(last.text, result?.text)
  }

  private fun node(
    text: String,
    parent: AccessibilityNodeInfoCompat? = null,
  ): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    raw.text = text
    raw.className = "android.widget.TextView"
    raw.isFocusable = text != "root"
    raw.isVisibleToUser = true
    raw.setBoundsInScreen(Rect(0, 0, 100, 100))
    val node = AccessibilityNodeInfoCompat.wrap(raw)
    if (parent != null) {
      shadowOf(parent.unwrap()).addChild(raw)
    }
    return node
  }
}
