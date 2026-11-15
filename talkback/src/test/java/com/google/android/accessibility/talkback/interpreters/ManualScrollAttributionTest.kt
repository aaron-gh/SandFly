/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.interpreters.ManualScrollInterpreter.ManualScrollInterpretation
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.input.ScrollEventInterpreter
import com.google.android.accessibility.utils.monitor.TouchMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualScrollAttributionTest {
  @Before fun start() {
    FormFactorUtils.initialize(RuntimeEnvironment.getApplication())
    ScrollEventInterpreter.resetNavigation()
  }
  @After fun stop() { ScrollEventInterpreter.resetNavigation() }

  @Test fun noRecentTouchDoesNotMakeAWebScrollAnAutomaticNavigationScroll() {
    val context = RuntimeEnvironment.getApplication()
    val interpreter = AccessibilityFocusInterpreter(context, null, null, TouchMonitor(), null)
    val web = AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(View(context)).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    })
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = SystemClock.uptimeMillis()
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
    }
    assertFalse(interpreter.shouldIgnoreManualScroll(event, web))
    ScrollEventInterpreter.onWebNavigation(12, event.eventTime)
    assertTrue(interpreter.shouldIgnoreManualScroll(event, web))
    ScrollEventInterpreter.resetNavigation()
    assertFalse(interpreter.shouldIgnoreManualScroll(event, web))
  }

  @Test fun aScrollWithoutTouchAfterFocusWasLostStartsNoWebFocusRecovery() {
    val context = RuntimeEnvironment.getApplication()
    val interpreter = AccessibilityFocusInterpreter(context, null, null, TouchMonitor(), null)
    interpreter.setActorState(ActorState(ActorStateWritable(null, null, null, null,
      AccessibilityFocusActionHistory(context).reader, null, null, null, null, null, null, null,
      null, null)))
    val web = AccessibilityNodeInfo.obtain(View(context)).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    }
    // A Page Down key or a page script scrolls the page with no touch and no navigation.
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = SystemClock.uptimeMillis()
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
      org.robolectric.Shadows.shadowOf(this).setSourceNode(web)
    }
    val interpretation = ManualScrollInterpretation.create(
      null, event, TraversalStrategy.SEARCH_FOCUS_FORWARD, false)
    val handled: Boolean = ReflectionHelpers.callInstanceMethod(interpreter,
      "startWebFocusRecovery",
      ClassParameter.from(ManualScrollInterpretation::class.java, interpretation),
      ClassParameter.from(AccessibilityNodeInfoCompat::class.java, null),
      ClassParameter.from(Boolean::class.javaPrimitiveType, false))
    assertFalse(handled)
  }
}
