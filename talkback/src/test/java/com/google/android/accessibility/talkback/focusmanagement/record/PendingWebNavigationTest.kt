/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.focusmanagement.record

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.FormFactorUtils
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingWebNavigationTest {
  @Before fun initializeFormFactor() {
    FormFactorUtils.initialize(RuntimeEnvironment.getApplication())
  }
  private val history = AccessibilityFocusActionHistory(RuntimeEnvironment.getApplication())
  private fun node(label: String, web: Boolean = true): AccessibilityNodeInfoCompat {
    val info = AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      text = label
      className = "android.widget.TextView"
      packageName = "example.web"
      isVisibleToUser = true
      isAccessibilityFocused = true
      if (web) addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      ReflectionHelpers.callInstanceMethod<Any>(this, "setSealed",
        ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType!!, true))
    }
    return AccessibilityNodeInfoCompat.wrap(info)
  }
  private fun info(mute: Boolean = false) = FocusActionInfo.builder()
    .setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).setForceMuteFeedback(mute).build()
  private fun event(node: AccessibilityNodeInfoCompat, time: Long) =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED).apply {
      eventTime = time
      shadowOf(this).setSourceNode(node.unwrap())
    }

  @Test fun theFocusEventCompletesOneDefaultMove() {
    val start = node("start"); val next = node("next"); val time = SystemClock.uptimeMillis()
    history.onPendingWebNavigationAction(info(), time, null, start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(30))
    val record = history.reader.matchFocusActionRecordFromEvent(event(next, time + 5))
    assertNotNull(record)
    assertEquals(next, record!!.focusedNode)
    assertNull(history.pendingWebFocusActionInfo)
  }

  @Test fun anOldPivotEventDoesNotCompleteTheMove() {
    val start = node("start"); val action = info(); val time = SystemClock.uptimeMillis()
    history.onPendingWebNavigationAction(action, time, null, start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(20))
    assertNull(history.reader.matchFocusActionRecordFromEvent(event(start, time + 1)))
    assertSame(action, history.pendingWebFocusActionInfo)
  }

  @Test fun aNewCommandCanCompleteThePreviousMoveBeforeItsEventArrives() {
    val start = node("start"); val middle = node("middle"); val end = node("end")
    val time = SystemClock.uptimeMillis()
    history.onPendingWebNavigationAction(info(), time, null, start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(10))
    history.completePendingWebFocusFromPivot(middle)
    val second = info(mute = true)
    history.onPendingWebNavigationAction(second, time + 10, null, middle)
    ShadowSystemClock.advanceBy(Duration.ofMillis(20))
    val firstRecord = history.reader.matchFocusActionRecordFromEvent(event(middle, time + 4))
    assertNotNull(firstRecord)
    assertFalse(firstRecord!!.extraInfo.forceMuteFeedback)
    assertSame(second, history.pendingWebFocusActionInfo)
    // A still older focus event is different from the new pivot but predates its command.
    history.reader.matchFocusActionRecordFromEvent(event(start, time + 2))
    assertSame(second, history.pendingWebFocusActionInfo)
    val last = history.reader.matchFocusActionRecordFromEvent(event(end, time + 12))
    assertNotNull(last)
    assertTrue(last!!.extraInfo.forceMuteFeedback)
    assertNull(history.pendingWebFocusActionInfo)
  }

  @Test fun aNativeFocusEventDoesNotConsumeWebNavigationMetadata() {
    val action = info(); val time = SystemClock.uptimeMillis()
    history.onPendingWebNavigationAction(action, time, null, node("web"))
    ShadowSystemClock.advanceBy(Duration.ofMillis(20))
    history.reader.matchFocusActionRecordFromEvent(event(node("native", web = false), time + 1))
    assertSame(action, history.pendingWebFocusActionInfo)
  }

  @Test fun aDefaultMoveAcceptsAnEventInTheSameMillisecondAsTheAction() {
    val time = SystemClock.uptimeMillis(); val next = node("next")
    history.onPendingWebNavigationAction(info(), time, null, node("start"))
    assertNotNull(history.reader.matchFocusActionRecordFromEvent(event(next, time)))
  }

  @Test fun theLegacyPendingPathKeepsItsStrictTimestampRule() {
    val time = SystemClock.uptimeMillis(); val action = info()
    history.onPendingAccessibilityFocusActionOnWebElement(action, time, null)
    history.reader.matchFocusActionRecordFromEvent(event(node("next"), time))
    assertSame(action, history.pendingWebFocusActionInfo)
  }
}
