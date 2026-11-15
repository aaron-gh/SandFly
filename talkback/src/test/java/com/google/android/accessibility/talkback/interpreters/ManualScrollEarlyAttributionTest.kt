/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollEventInterpretation
import com.google.android.accessibility.utils.output.ScrollActionRecord
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualScrollEarlyAttributionTest {
  @Test fun attributedNavigationDoesNotAskForThePotentiallyUnfinishedHistoryPath() {
    val interpreter = ManualScrollInterpreter()
    var checked = false
    interpreter.setListener(object : ManualScrollInterpreter.ScrolledViewChangeListener {
      override fun onManualScroll(i: ManualScrollInterpreter.ManualScrollInterpretation) {
        fail("A navigation scroll must not trigger manual recovery")
      }
      override fun shouldIgnoreManualScroll(
        event: AccessibilityEvent, node: AccessibilityNodeInfoCompat
      ): Boolean { checked = true; return true }
    })
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED)
    val source = AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))
    shadowOf(event).setSourceNode(source)
    // No ActorState is installed: touching history before attribution would fail this test.
    interpreter.onScrollEvent(event, ScrollEventInterpretation(
      ScrollActionRecord.ACTION_MANUAL_SCROLL, TraversalStrategy.SEARCH_FOCUS_FORWARD,
      true, false, true, false, -1), null)
    assertTrue(checked)
  }
}
