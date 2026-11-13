/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.eventprocessor

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class ProcessorEventQueueMemoTest {
  private fun change(label: String) =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply { contentDescription = label }
  private fun focus(label: String): AccessibilityEvent {
    val node = AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    }
    return AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED).apply {
      contentDescription = label
      shadowOf(this).setSourceNode(node)
    }
  }

  @Test fun repeatedFocusDoesNotReadTheSameQueuedChangesAgain() {
    var checks = 0
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { checks++; false })
    repeat(20) { queue.onAccessibilityEvent(change("change-$it"), null) }
    repeat(10) { queue.onAccessibilityEvent(focus("focus-$it"), null) }
    assertEquals(ProcessorEventQueue.MAX_CONTENT_CHANGES_CHECKED, checks)
    assertEquals((0 until 10).map { "focus-$it" }, handled)
  }

  @Test fun aNewChangeRechecksLiveRegionStateOfOlderQueuedChanges() {
    var assertive = false
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { assertive })
    queue.onAccessibilityEvent(change("notice"), null)
    queue.onAccessibilityEvent(focus("first focus"), null)
    assertive = true
    queue.onAccessibilityEvent(change("notice"), null)
    queue.onAccessibilityEvent(focus("second focus"), null)
    assertEquals(listOf("first focus", "notice", "notice", "second focus"), handled)
  }
  @Test fun windowFocusMetadataDoesNotInvalidateQueuedLiveRegionAnswers() {
    var checks = 0
    val queue = ProcessorEventQueue({ _, _ -> }, { checks++; false })
    repeat(20) { queue.onAccessibilityEvent(change("change-$it"), null) }
    queue.onAccessibilityEvent(focus("first"), null)
    repeat(10) {
      val window = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED)
      ReflectionHelpers.callInstanceMethod<Any>(window, "setWindowChanges",
        ClassParameter.from(Int::class.javaPrimitiveType, AccessibilityEvent.WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED))
      queue.onAccessibilityEvent(window, null)
      queue.onAccessibilityEvent(focus("focus-$it"), null)
    }
    assertEquals(ProcessorEventQueue.MAX_CONTENT_CHANGES_CHECKED, checks)
    val structural = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED)
    ReflectionHelpers.callInstanceMethod<Any>(structural, "setWindowChanges",
      ClassParameter.from(Int::class.javaPrimitiveType, AccessibilityEvent.WINDOWS_CHANGE_CHILDREN))
    queue.onAccessibilityEvent(structural, null)
    queue.onAccessibilityEvent(focus("after layout change"), null)
    assertEquals(2 * ProcessorEventQueue.MAX_CONTENT_CHANGES_CHECKED, checks)
  }

}
