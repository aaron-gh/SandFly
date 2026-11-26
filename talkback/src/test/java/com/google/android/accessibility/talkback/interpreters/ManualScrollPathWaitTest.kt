/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord
import com.google.android.accessibility.talkback.focusmanagement.record.NodePathDescription
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.input.ScrollEventInterpreter
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollEventInterpretation
import com.google.android.accessibility.utils.monitor.TouchMonitor
import com.google.android.accessibility.utils.output.ScrollActionRecord
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import java.util.ArrayDeque
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** A web snapshot can be unfinished while the user is already scrolling again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualScrollPathWaitTest {
  private val context = RuntimeEnvironment.getApplication()

  @Before fun start() {
    FormFactorUtils.initialize(context)
    ScrollEventInterpreter.resetNavigation()
  }

  private fun node(web: Boolean = true, sideways: Boolean = false) =
    AccessibilityNodeInfo.obtain(View(context)).apply {
      if (web) addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      addAction(if (sideways) AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT
                else AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN)
      ReflectionHelpers.callInstanceMethod<Unit>(this, "setSealed",
        ClassParameter.from(Boolean::class.javaPrimitiveType, true))
    }

  private fun checkPathUse(
    userTouch: Boolean = true,
    sideways: Boolean = false,
    webSource: Boolean = true,
    webFocus: Boolean = true,
    commandTail: Boolean = false,
    expectPending: Boolean = true,
    expectedReads: Int,
  ) {
    val source = node(webSource, sideways)
    val focus = AccessibilityNodeInfoCompat.wrap(node(webFocus))
    val history = AccessibilityFocusActionHistory(context)
    var pathReads = 0
    val record = object : FocusActionRecord(focus, FocusActionInfo.builder().build(), 1) {
      override fun getAncestorIdentityPath(): NodePathDescription {
        pathReads++
        // Asking for a pending Future here would block the event thread.
        assertTrue("The manual web scroll must not wait for its snapshot", expectedReads > 0)
        return NodePathDescription.obtain(AccessibilityNodeInfoCompat.wrap(source))
      }
    }
    ReflectionHelpers.getField<ArrayDeque<FocusActionRecord>>(history, "focusActionRecordList").add(record)
    val actorState = ActorState(ActorStateWritable(null, null, null, null, history.reader,
      null, null, null, null, null, null, null, null, null))
    val touch = TouchMonitor()
    if (userTouch) touch.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_TOUCH_INTERACTION_START).apply { eventTime = 10 })
    val listener = AccessibilityFocusInterpreter(context, null, null, touch, null)
    listener.setActorState(actorState)
    val interpreter = ManualScrollInterpreter().apply {
      setActorState(actorState)
      setListener(listener)
    }
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = 20
      shadowOf(this).setSourceNode(source)
    }
    val userAction = if (commandTail) ScrollActionRecord.ACTION_UNKNOWN
      else ScrollActionRecord.ACTION_MANUAL_SCROLL
    interpreter.onScrollEvent(event, ScrollEventInterpretation(
      userAction, TraversalStrategy.SEARCH_FOCUS_FORWARD, true, false, true, false, -1,
      false, false, commandTail), null)
    assertEquals(expectedReads, pathReads)
    val pending = ReflectionHelpers.getField<ManualScrollInterpreter.ManualScrollInterpretation>(
      listener, "pendingManualScroll")
    if (!expectPending) {
      assertNull(pending)
      return
    }
    assertNotNull(pending)
    assertEquals(AccessibilityNodeInfoCompat.wrap(source),
      AccessibilityNodeInfoCompat.wrap(pending.event().source!!))
    // The normal path must keep the verified ancestry, especially for sideways focus strips.
    assertEquals(expectedReads > 0, pending.scrolledHoldsFocus())
  }

  @Test fun aFingerDrivenWebScrollDoesNotReadThePendingSnapshot() = checkPathUse(expectedReads = 0)

  @Test fun aSidewaysStripStillUsesItsVerifiedFocusPath() =
    checkPathUse(sideways = true, expectedReads = 1)

  @Test fun aKeyboardOrPageScriptScrollKeepsItsFocusPathHandling() =
    checkPathUse(userTouch = false, expectedReads = 1)

  @Test fun aNativeScrollSourceKeepsItsFocusPathHandling() =
    checkPathUse(webSource = false, expectedReads = 1)

  @Test fun aNativeFocusDoesNotUseTheWebShortcut() =
    checkPathUse(webFocus = false, expectedReads = 1)

  @Test fun aFinishedNativeScrollCommandCanStillRecoverItsFocus() =
    checkPathUse(webSource = false, webFocus = false, commandTail = true, expectedReads = 1)

  @Test fun aFinishedWebScrollCommandDoesNotStartManualRecovery() =
    checkPathUse(commandTail = true, expectPending = false, expectedReads = 0)
}
