/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor
import com.google.android.accessibility.talkback.monitor.InputMethodMonitor
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

  @Test fun shutdownCancelsARecoveryBeforeAQueuedWorkerCanReadTheApp() {
    val context = RuntimeEnvironment.getApplication()
    val interpreter = AccessibilityFocusInterpreter(context, null, null, TouchMonitor(), null)
    val node = AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(View(context)))
    val recovery = WebFocusRecovery(null, null, node, true, true, null, false)
    var pending: java.util.function.Supplier<Boolean>? = null
    val task = java.util.concurrent.FutureTask<Boolean> { true }
    recovery.startInBackground(SystemClock.uptimeMillis() + 500) { read ->
      pending = read
      task
    }
    ReflectionHelpers.setField(interpreter, "webFocusRecovery", recovery)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryWork", task)
    interpreter.shutdown()
    assertTrue(task.isCancelled)
    assertFalse(pending!!.get())
    assertFalse(interpreter.isWebScrollFocusPending)
  }

  @Test fun aPendingVerticalScrollCannotDiscardASidewaysScrollFromAnotherWindow() {
    val context = RuntimeEnvironment.getApplication()
    val interpreter = AccessibilityFocusInterpreter(context, null, null, TouchMonitor(), null)
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = 100
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
    }
    val pending = ManualScrollInterpretation.create(null, event,
      TraversalStrategy.SEARCH_FOCUS_FORWARD, false)
    ReflectionHelpers.setField(interpreter, "webScroll", pending)
    ReflectionHelpers.setField(interpreter, "pendingManualScroll", pending)
    val other = AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(View(context)).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT)
      ReflectionHelpers.setField(this, "mWindowId", 13)
    })
    assertFalse(interpreter.shouldIgnoreManualScroll(event, other))
  }

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
      ClassParameter.from(Boolean::class.javaPrimitiveType, false),
      ClassParameter.from(AccessibilityNodeInfoCompat::class.java, AccessibilityNodeInfoCompat.wrap(web)))
    assertFalse(handled)
  }
  /** Runs a delayed web scroll at time 100 after a swipe at [focusTime]; returns focus queries and pipeline inputs. */
  private fun delayedWebScrollAfterSwipeAt(focusTime: Long): Pair<Int, Int> {
    val context = RuntimeEnvironment.getApplication()
    val web = AccessibilityNodeInfo.obtain(View(context)).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      ReflectionHelpers.callInstanceMethod<Unit>(this, "setSealed",
        ClassParameter.from(Boolean::class.javaPrimitiveType, true))
    }
    val history = AccessibilityFocusActionHistory(context)
    val record = FocusActionRecord(AccessibilityNodeInfoCompat.wrap(web),
      FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(), focusTime)
    ReflectionHelpers.getField<java.util.ArrayDeque<FocusActionRecord>>(history,
      "focusActionRecordList").add(record)
    val monitor = ReflectionHelpers.callConstructor(ScreenStateMonitor::class.java,
      ClassParameter.from(android.accessibilityservice.AccessibilityService::class.java, null),
      ClassParameter.from(InputMethodMonitor::class.java, null))
    ReflectionHelpers.setField(monitor, "areMainWindowsStable", true)
    var focusQueries = 0
    val focusMonitor = object : AccessibilityFocusMonitor(null, null, null) {
      override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean): AccessibilityNodeInfoCompat? {
        focusQueries++
        return null
      }
    }
    var inputs = 0
    val interpreter =
      AccessibilityFocusInterpreter(context, focusMonitor, monitor.state, TouchMonitor(), null)
    interpreter.setActorState(ActorState(ActorStateWritable(null, null, null, null,
      history.reader, null, null, null, null, null, null, null, null, null)))
    interpreter.setPipeline(Pipeline.InterpretationReceiver { _, _, _, _ -> inputs++; true })
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = 100
      org.robolectric.Shadows.shadowOf(this).setSourceNode(web)
    }
    ReflectionHelpers.callInstanceMethod<Unit>(interpreter, "handleManualScroll",
      ClassParameter.from(ManualScrollInterpretation::class.java,
        ManualScrollInterpretation.create(null, event, TraversalStrategy.SEARCH_FOCUS_FORWARD, true)),
      ClassParameter.from(Boolean::class.javaPrimitiveType, false))
    assertNull(ReflectionHelpers.getField(interpreter, "webFocusRecovery"))
    return focusQueries to inputs
  }

  @Test fun aDelayedWebScrollAfterNewerUserNavigationDoesNotReadNativeFocusability() {
    val (focusQueries, inputs) = delayedWebScrollAfterSwipeAt(focusTime = 200)
    assertEquals("An obsolete scroll must return before any focus query", 0, focusQueries)
    assertEquals("An obsolete scroll must not move focus", 0, inputs)
  }

  @Test fun aWebScrollAfterAnOlderSwipeStillChecksTheFocus() {
    val (focusQueries, _) = delayedWebScrollAfterSwipeAt(focusTime = 50)
    assertEquals(1, focusQueries)
  }
}
