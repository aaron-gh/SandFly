/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.input

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.utils.monitor.TouchMonitor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScrollPercentTest {

  private fun percent(
    scrollX: Int = -1,
    scrollY: Int = -1,
    maxScrollX: Int = -1,
    maxScrollY: Int = -1,
    fromIndex: Int = -1,
    toIndex: Int = -1,
    itemCount: Int = -1,
  ) =
    ScrollEventInterpreter.scrollPercent(
      scrollX,
      scrollY,
      maxScrollX,
      maxScrollY,
      fromIndex,
      toIndex,
      itemCount,
    )

  @Test
  fun scrollOffsetsVerticalBeforeHorizontal() {
    assertEquals(25, percent(scrollY = 250, maxScrollY = 1000))
    assertEquals(100, percent(scrollY = 1200, maxScrollY = 1000))
    assertEquals(50, percent(scrollX = 50, maxScrollX = 100))
    assertEquals(10, percent(scrollX = 50, maxScrollX = 100, scrollY = 100, maxScrollY = 1000))
  }

  @Test
  fun shownItemsWithoutScrollOffsets() {
    assertEquals(0, percent(fromIndex = 0, toIndex = 9, itemCount = 100))
    assertEquals(100, percent(fromIndex = 90, toIndex = 99, itemCount = 100))
    // Items 45 to 54 of 100: 45 of the 90 places the first shown item can be in.
    assertEquals(50, percent(fromIndex = 45, toIndex = 54, itemCount = 100))
    assertEquals(-1, percent())
    assertEquals(-1, percent(fromIndex = 5, toIndex = 9, itemCount = 3))
  }

  @Test
  fun keepsThePercentOfTheLastScrollOfEachNode() {
    val list = AccessibilityNodeInfo()
    val interpreter =
      object : ScrollEventInterpreter(null, TouchMonitor(), false) {
        override fun readSource(e: AccessibilityEvent, navigation: Boolean) = list
      }.apply { setScrollActorState { null } }
    assertEquals(-1, interpreter.scrollPercent(list))
    interpreter.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
        scrollY = 300
        maxScrollY = 1200
      },
      null,
    )
    assertEquals(25, interpreter.scrollPercent(list))
  }
}
