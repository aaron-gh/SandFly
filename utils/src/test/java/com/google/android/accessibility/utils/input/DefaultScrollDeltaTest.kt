/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.input

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.utils.AccessibilityEventUtils
import com.google.android.accessibility.utils.monitor.TouchMonitor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultScrollDeltaTest {
  private fun contentChange() = AccessibilityEvent.obtain(
    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
    contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
    scrollDeltaX = -1; scrollDeltaY = -1
  }

  @Test fun ordinarySubtreeChangesDoNotReadNodesAsIfTheyWereScrolls() {
    var reads = 0
    var deliveries = 0
    val interpreter = object : ScrollEventInterpreter(null, TouchMonitor(), false) {
      override fun readSource(e: AccessibilityEvent, navigation: Boolean): AccessibilityNodeInfo? {
        reads++
        return null
      }
    }.apply { setScrollActorState { null }; addListener { _, _, _ -> deliveries++ } }
    repeat(50) { interpreter.onAccessibilityEvent(contentChange(), null) }
    assertEquals(0, reads)
    assertEquals(0, deliveries)
    // Fine-grained position-only changes still enter scroll interpretation, with no deltas.
    interpreter.onAccessibilityEvent(contentChange().apply { scrollY = 100; maxScrollY = 1000 }, null)
    interpreter.onAccessibilityEvent(contentChange().apply {
      fromIndex = 1; toIndex = 5; itemCount = 40
    }, null)
    // A native WebView's real scroll event is never discarded by this content-change filter.
    interpreter.onAccessibilityEvent(contentChange().apply {
      eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED
    }, null)
    for ((x, y) in listOf(-1 to 0, 0 to -1)) {
      interpreter.onAccessibilityEvent(contentChange().apply {
        scrollDeltaX = x; scrollDeltaY = y
      }, null)
    }
    assertEquals(5, reads)
    assertEquals(5, deliveries)
  }

  @Test fun actualSmallAndNegativeDeltasRemainAvailable() {
    for ((x, y) in listOf(-1 to 0, 0 to -1, 0 to -40, 0 to 40, 2 to 3)) {
      assertTrue(AccessibilityEventUtils.hasValidScrollDelta(contentChange().apply {
        scrollDeltaX = x; scrollDeltaY = y
      }))
    }
    assertFalse(AccessibilityEventUtils.hasValidScrollDelta(contentChange().apply {
      scrollDeltaX = 0; scrollDeltaY = 0
    }))
    assertFalse(AccessibilityEventUtils.hasValidScrollDelta(contentChange().apply {
      scrollDeltaX = AccessibilityEventUtils.DELTA_UNDEFINED
      scrollDeltaY = AccessibilityEventUtils.DELTA_UNDEFINED
    }))
  }
}
