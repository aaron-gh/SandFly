/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.input

import android.os.SystemClock
import java.time.Duration
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CompletedWebNavigationTest {
  @Before fun reset() { ScrollEventInterpreter.resetNavigation() }

  private fun navigationEvent(time: Long) = android.view.accessibility.AccessibilityEvent.obtain(
    android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = time
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
    }

  @Test fun aNewTouchAndCommandDoNotTurnAnOldAutomaticScrollIntoAManualOne() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(40))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ScrollEventInterpreter.clearNavigation()
    ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.onWebNavigation(12, start + 60)
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 20))
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 70))
  }

  @Test fun aManualScrollAfterTheCommandReturnedIsNotClaimed() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(40))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ScrollEventInterpreter.clearNavigation()
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 41))
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(13, start + 20))
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start - 1))
    ScrollEventInterpreter.clearCompletedWebNavigation()
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 20))
  }

  @Test fun aSubsequentFlickPreservesAnAsynchronousScrollFromThePreviousMove() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(20))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ScrollEventInterpreter.onTouchInteractionStart()
    ShadowSystemClock.advanceBy(Duration.ofMillis(140))
    ScrollEventInterpreter.onNavigation(12)
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 130))
  }

  @Test fun directTouchEndsAtTheMotionTimeInsteadOfTheDelayedCallbackTime() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(20))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(180))
    ScrollEventInterpreter.clearNavigationAt(start + 80)
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 70))
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 81))
  }

  @Test fun oldCommandsAreBoundedWithoutRemovingTheRecentCommand() {
    val first = SystemClock.uptimeMillis()
    repeat(33) {
      ShadowSystemClock.advanceBy(Duration.ofMillis(10))
      ScrollEventInterpreter.onWebNavigation(12, first + it * 10)
    }
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, first))
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, first + 325))
  }

  @Test fun aDirectTouchDuringTheActionIsNotOverwrittenByItsLateCompletion() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.clearNavigationAt(start + 50)
    ScrollEventInterpreter.onWebNavigation(12, start)
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 40))
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 70))
  }

  @Test fun severalDirectMotionsDuringAnActionKeepTheFirstTouchAsTheBoundary() {
    val start = SystemClock.uptimeMillis()
    ScrollEventInterpreter.startWebNavigation(start)
    ScrollEventInterpreter.clearNavigationAt(start + 50)
    ScrollEventInterpreter.clearNavigationAt(start + 80)
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.finishWebNavigationAction(12, start, true)
    assertTrue(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start + 40)))
    assertFalse(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start + 70)))
  }

  @Test fun anOldMotionCapsAnOlderSpanWithoutTakingAwayTheNewCommand() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.startWebNavigation(start + 180)
    ScrollEventInterpreter.finishWebNavigationAction(12, start + 180, true)
    ScrollEventInterpreter.clearNavigationAt(start + 50)
    assertFalse(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start + 70)))
    assertTrue(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start + 190)))
  }

  @Test fun aSlowActionStillAttributesItsAutomaticScrollAfterTheActionReturns() {
    val start = SystemClock.uptimeMillis()
    ScrollEventInterpreter.startWebNavigation(start)
    ShadowSystemClock.advanceBy(Duration.ofMillis(1500))
    ScrollEventInterpreter.finishWebNavigationAction(12, start, true)
    assertTrue(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start + 1600)))
  }

  @Test fun aFailedActionLeavesNoPendingOrCompletedNavigationToClaimAScroll() {
    val start = SystemClock.uptimeMillis()
    ScrollEventInterpreter.startWebNavigation(start)
    ScrollEventInterpreter.finishWebNavigationAction(12, start, false)
    assertFalse(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(start)))
  }

  @Test fun aLateDirectTouchCapsTheSpanAtItsMotionTimeEvenIfTheActionReturnedLater() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.onWebNavigation(12, start)
    ScrollEventInterpreter.clearNavigationAt(start + 50)
    assertTrue(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 40))
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start + 70))
  }

  @Test fun anOldQueuedMotionDoesNotClearANewerNavigation() {
    val start = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.onWebNavigation(12, start + 80)
    ScrollEventInterpreter.clearNavigationAt(start + 50)
    val event = android.view.accessibility.AccessibilityEvent.obtain(
      android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED).apply { eventTime = start + 120 }
    org.robolectric.Shadows.shadowOf(event).setWindowId(12)
    assertTrue(ScrollEventInterpreter.isWebNavigationEvent(event))
  }

  @Test fun shutdownForgetsBothTheActiveCommandAndItsCompletedTimeSpans() {
    val start = SystemClock.uptimeMillis()
    ScrollEventInterpreter.onWebNavigation(12, start)
    ScrollEventInterpreter.resetNavigation()
    assertFalse(ScrollEventInterpreter.wasCompletedWebNavigation(12, start))
    val event = android.view.accessibility.AccessibilityEvent.obtain(
      android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED).apply { eventTime = start }
    org.robolectric.Shadows.shadowOf(event).setWindowId(12)
    assertFalse(ScrollEventInterpreter.isWebNavigationEvent(event))
  }

  @Test fun aWindowStateChangeForgetsWebNavigationButKeepsNativeNavigation() {
    val interpreter = ScrollEventInterpreter(
      null, com.google.android.accessibility.utils.monitor.TouchMonitor(), false)
    val stateChange = android.view.accessibility.AccessibilityEvent.obtain(
      android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    ScrollEventInterpreter.onNavigation(5)
    val now = SystemClock.uptimeMillis()
    interpreter.onAccessibilityEvent(stateChange, null)
    assertTrue(ScrollEventInterpreter.followsCurrentNavigation(5, now))

    ScrollEventInterpreter.onWebNavigation(12, now)
    assertTrue(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(now)))
    interpreter.onAccessibilityEvent(stateChange, null)
    assertFalse(ScrollEventInterpreter.isWebNavigationEvent(navigationEvent(now)))
    assertFalse(ScrollEventInterpreter.followsCurrentNavigation(12, now))
  }
}
