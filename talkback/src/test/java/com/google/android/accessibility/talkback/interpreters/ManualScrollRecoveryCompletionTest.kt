/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.Interpretation
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor
import com.google.android.accessibility.talkback.monitor.InputMethodMonitor
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord
import com.google.android.accessibility.talkback.interpreters.ManualScrollInterpreter.ManualScrollInterpretation
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.monitor.TouchMonitor
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.concurrent.FutureTask

/** The page scrolling a newly focused card sideways must not replay the old vertical scroll. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualScrollRecoveryCompletionTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun node(sideways: Boolean) = AccessibilityNodeInfo.obtain(View(context)).apply {
    addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    addAction(if (sideways) AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT
              else AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN)
    ReflectionHelpers.callInstanceMethod<Unit>(this, "setSealed",
      ClassParameter.from(Boolean::class.javaPrimitiveType, true))
  }

  private fun scroll(source: AccessibilityNodeInfo, time: Long): ManualScrollInterpretation {
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = time
      shadowOf(this).setSourceNode(source)
    }
    return ManualScrollInterpretation.create(null, event,
      TraversalStrategy.SEARCH_FOCUS_FORWARD, true)
  }

  private fun checkCompletion(outcome: Int, focusSucceeded: Boolean, shouldRetry: Boolean,
    waitingRead: Boolean = false) {
    FormFactorUtils.initialize(context)
    val source = node(false)
    val focus = AccessibilityNodeInfoCompat.wrap(node(false))
    val history = AccessibilityFocusActionHistory(context)
    val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.MANUAL_SCROLL).build()
    val record = FocusActionRecord(focus, info, 1)
    ReflectionHelpers.getField<java.util.ArrayDeque<FocusActionRecord>>(history,
      "focusActionRecordList").add(record)
    val actorState = ActorState(ActorStateWritable(null, null, null, null, history.reader,
      null, null, null, null, null, null, null, null, null))
    val touch = TouchMonitor()
    touch.onAccessibilityEvent(AccessibilityEvent.obtain(
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_START).apply { eventTime = 10 })
    touch.onAccessibilityEvent(AccessibilityEvent.obtain(
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_END).apply { eventTime = 20 })
    val monitor = ReflectionHelpers.callConstructor(ScreenStateMonitor::class.java,
      ClassParameter.from(android.accessibilityservice.AccessibilityService::class.java, null),
      ClassParameter.from(InputMethodMonitor::class.java, null))
    ReflectionHelpers.setField(monitor, "areMainWindowsStable", true)
    val interpreter = AccessibilityFocusInterpreter(context, null, monitor.state, touch, null)
    interpreter.setActorState(actorState)
    var focusCalls = 0
    interpreter.setPipeline(Pipeline.InterpretationReceiver { _, _, _, _ ->
      focusCalls++; focusSucceeded
    })
    interpreter.onManualWebScroll(scroll(source, 20))
    val original = ReflectionHelpers.getField<ManualScrollInterpretation>(interpreter,
      "pendingManualScroll")
    assertNotNull(original)
    ReflectionHelpers.setField(interpreter, "pendingManualScroll", null)
    val recovery = WebFocusRecovery(focus, null, AccessibilityNodeInfoCompat.wrap(source),
      true, true, null, false)
    ReflectionHelpers.setField(recovery, "outcome", outcome)
    ReflectionHelpers.setField(recovery, "item", focus)
    ReflectionHelpers.setField(interpreter, "webFocusRecovery", recovery)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryScroll", original)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryFocusTime", 1L)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryStartTime",
      SystemClock.uptimeMillis() - if (waitingRead) 501 else 0)
    if (waitingRead) {
      val work = FutureTask<Boolean> { true }
      ReflectionHelpers.setField(interpreter, "webFocusRecoveryWork", work)
      ReflectionHelpers.callInstanceMethod<Unit>(interpreter, "runWebFocusRecovery",
        ClassParameter.from(Boolean::class.javaPrimitiveType, true))
      assertSame(recovery, ReflectionHelpers.getField(interpreter, "webFocusRecovery"))
      assertFalse(work.isCancelled)
      assertEquals(0, focusCalls)
      work.run()
    }
    ReflectionHelpers.callInstanceMethod<Unit>(interpreter, "runWebFocusRecovery",
      ClassParameter.from(Boolean::class.javaPrimitiveType, true))
    assertEquals(if (outcome == WebFocusRecovery.FOUND) 1 else 0, focusCalls)

    // Chrome can scroll a horizontal carousel to show the card SandFly just focused.
    // It is not a new user scroll, and must not send focus to a fixed page header.
    interpreter.onManualScroll(scroll(node(true), 30))
    val repeated = ReflectionHelpers.getField<ManualScrollInterpretation?>(interpreter,
      "pendingManualScroll")
    assertEquals(shouldRetry, repeated != null)

    // A real continuation of the user's vertical scroll must still be handled.
    interpreter.onManualWebScroll(scroll(source, 40))
    assertNotNull(ReflectionHelpers.getField<ManualScrollInterpretation?>(interpreter,
      "pendingManualScroll"))
    ReflectionHelpers.getField<android.os.Handler>(interpreter, "handler")
      .removeCallbacksAndMessages(null)
  }

  /**
   * Runs a recovery whose worker read is still outstanding [elapsedMs] after it started, with the
   * item [edge] across an edge of the view already published by the worker.
   */
  private fun runWithOutstandingRead(yieldToInput: Boolean, elapsedMs: Long,
    published: Boolean = true): Triple<AccessibilityFocusInterpreter, FutureTask<Boolean>,
      List<AccessibilityNodeInfoCompat?>> {
    FormFactorUtils.initialize(context)
    val source = node(false)
    val focus = AccessibilityNodeInfoCompat.wrap(node(false))
    val edge = AccessibilityNodeInfoCompat.wrap(node(false))
    val history = AccessibilityFocusActionHistory(context)
    val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.MANUAL_SCROLL).build()
    ReflectionHelpers.getField<java.util.ArrayDeque<FocusActionRecord>>(history,
      "focusActionRecordList").add(FocusActionRecord(focus, info, 1))
    val actorState = ActorState(ActorStateWritable(null, null, null, null, history.reader,
      null, null, null, null, null, null, null, null, null))
    val monitor = ReflectionHelpers.callConstructor(ScreenStateMonitor::class.java,
      ClassParameter.from(android.accessibilityservice.AccessibilityService::class.java, null),
      ClassParameter.from(InputMethodMonitor::class.java, null))
    ReflectionHelpers.setField(monitor, "areMainWindowsStable", true)
    // Focus was lost: nothing to move away from when no item is found.
    val focusMonitor = object : AccessibilityFocusMonitor(null, null, null) {
      override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean): AccessibilityNodeInfoCompat? = null
    }
    val interpreter =
      AccessibilityFocusInterpreter(context, focusMonitor, monitor.state, TouchMonitor(), null)
    interpreter.setActorState(actorState)
    val items = mutableListOf<AccessibilityNodeInfoCompat?>()
    interpreter.setPipeline(Pipeline.InterpretationReceiver { _, _, interpretation, _ ->
      items.add((interpretation as Interpretation.ManualScroll).itemInView()); true
    })
    val recovery = WebFocusRecovery(focus, null, AccessibilityNodeInfoCompat.wrap(source),
      true, true, null, false)
    if (published) ReflectionHelpers.setField(recovery, "publishedPartial", edge)
    // Never run: the worker's read does not return.
    val work = FutureTask<Boolean> { true }
    ReflectionHelpers.setField(interpreter, "webFocusRecovery", recovery)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryScroll", scroll(source, 20))
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryWork", work)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryFocusTime", 1L)
    ReflectionHelpers.setField(interpreter, "webFocusRecoveryStartTime",
      SystemClock.uptimeMillis() - elapsedMs)
    ReflectionHelpers.callInstanceMethod<Unit>(interpreter, "runWebFocusRecovery",
      ClassParameter.from(Boolean::class.javaPrimitiveType, yieldToInput))
    if (published && items.isNotEmpty()) assertSame(edge, items[0])
    return Triple(interpreter, work, items)
  }

  @Test fun aGestureAtTheDeadlineUsesTheEdgeItemFoundWhileAReadIsOutstanding() {
    val (interpreter, work, items) = runWithOutstandingRead(yieldToInput = false, elapsedMs = 501)
    assertEquals(1, items.size)
    assertTrue("The outstanding worker read must be stopped", work.isCancelled)
    assertNull(ReflectionHelpers.getField(interpreter, "webFocusRecovery"))
    assertFalse(interpreter.isWebScrollFocusPending)
  }

  @Test fun aGestureAtTheDeadlineWithoutAnEdgeItemMovesNoFocusToAnItem() {
    val (interpreter, work, items) =
      runWithOutstandingRead(yieldToInput = false, elapsedMs = 501, published = false)
    assertTrue(items.isEmpty())
    assertTrue(work.isCancelled)
    assertFalse(interpreter.isWebScrollFocusPending)
  }

  @Test fun pollingWaitsForAnOutstandingReadOnlyWithinTheGraceTime() {
    val (waiting, waitingWork, none) = runWithOutstandingRead(yieldToInput = true, elapsedMs = 600)
    assertTrue(none.isEmpty())
    assertFalse(waitingWork.isCancelled)
    assertTrue(waiting.isWebScrollFocusPending)
    ReflectionHelpers.getField<android.os.Handler>(waiting, "handler")
      .removeCallbacksAndMessages(null)

    val (expired, work, items) = runWithOutstandingRead(yieldToInput = true, elapsedMs = 651)
    assertEquals(1, items.size)
    assertTrue(work.isCancelled)
    assertFalse(expired.isWebScrollFocusPending)
  }

  @Test fun scrollingTheRecoveredCardSidewaysDoesNotRecoverTheSameVerticalScrollAgain() =
    checkCompletion(WebFocusRecovery.FOUND, focusSucceeded = true, shouldRetry = false)

  @Test fun anOutstandingReadAtTheDeadlineDoesNotDiscardThePartialResult() =
    checkCompletion(WebFocusRecovery.FOUND, focusSucceeded = true, shouldRetry = false,
      waitingRead = true)

  @Test fun aFailedFocusStillAllowsTheStripToRequestRecovery() =
    checkCompletion(WebFocusRecovery.FOUND, focusSucceeded = false, shouldRetry = true)

  @Test fun aUserSelectedTabStillFollowsThePageScrollingItSideways() =
    checkCompletion(WebFocusRecovery.IN_VIEW, focusSucceeded = true, shouldRetry = true)
}
