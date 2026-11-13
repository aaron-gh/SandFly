/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.input

import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.utils.monitor.TouchMonitor
import com.google.android.accessibility.utils.output.ScrollActionRecord
import java.time.Duration
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class WebNavigationScrollOrderTest {
  @Before fun reset() { ScrollEventInterpreter.resetNavigation() }
  @After fun clear() { ScrollEventInterpreter.resetNavigation() }

  private fun scroll(time: Long, source: AccessibilityNodeInfo? = null): AccessibilityEvent =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      eventTime = time
      fromIndex = 3; toIndex = 5; itemCount = 50
      scrollDeltaY = 100
      shadowOf(this).setSourceNode(source ?: AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
        isScrollable = true
        addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      })
      org.robolectric.Shadows.shadowOf(this).setWindowId(12)
    }

  // Robolectric's source-node setter handles getSource(), not the framework's flags overload.
  private fun interpreter() = object : ScrollEventInterpreter(null, TouchMonitor(), false) {
    override fun readSource(event: AccessibilityEvent, fromWebNavigation: Boolean): AccessibilityNodeInfo? = event.source
  }.apply { setScrollActorState { null } }

  @Test fun navigationAttributionIsDeliveredBeforeLaterFocusWithNoLatePositionFeedback() {
    val interpreter = interpreter()
    val order = mutableListOf<String>()
    interpreter.addListener { _, interpretation, _ ->
      assertTrue(interpretation.isFromNavigation)
      assertFalse(interpretation.hasValidIndex)
      assertEquals(ScrollActionRecord.ACTION_UNKNOWN, interpretation.userAction)
      order.add("navigation scroll")
    }
    val time = SystemClock.uptimeMillis()
    ScrollEventInterpreter.onWebNavigation(12, time)
    interpreter.onAccessibilityEvent(scroll(time), null)
    order.add("next focus speech")
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    assertEquals(listOf("navigation scroll", "next focus speech"), order)
  }

  @Test fun aScrollWithNoNavigationAttributionKeepsNormalManualHandlingWithoutATouch() {
    val interpreter = interpreter()
    val received = mutableListOf<ScrollEventInterpreter.ScrollEventInterpretation>()
    interpreter.addListener { _, interpretation, _ -> received.add(interpretation) }
    interpreter.onAccessibilityEvent(scroll(SystemClock.uptimeMillis()), null)
    assertEquals(1, received.size)
    assertFalse(received.single().isFromNavigation)
    assertTrue(received.single().hasValidIndex)
    assertEquals(ScrollActionRecord.ACTION_MANUAL_SCROLL, received.single().userAction)
  }

  @Test fun navigationUpdatesTheCacheBeforeAManualReverseScrollWithoutDeltas() {
    val interpreter = interpreter()
    val seen = mutableListOf<ScrollEventInterpreter.ScrollEventInterpretation>()
    interpreter.addListener { _, interpretation, _ -> seen.add(interpretation) }
    val time = SystemClock.uptimeMillis()
    val source = scroll(time).source!!
    fun event(offset: Long, index: Int, delta: Int) = scroll(time + offset, source).apply {
      fromIndex = index; toIndex = index + 2; scrollDeltaY = delta
    }
    interpreter.onAccessibilityEvent(event(0, 0, 100), null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.onWebNavigation(12, time + 100)
    interpreter.onAccessibilityEvent(event(100, 10, 100), null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.clearNavigationAt(time + 150)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    interpreter.onAccessibilityEvent(event(200, 5, 0), null)
    assertFalse(seen[1].hasValidIndex)
    assertTrue(seen[1].isFromNavigation)
    assertEquals(com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD,
      seen.last().scrollDirection)
    assertEquals(ScrollActionRecord.ACTION_MANUAL_SCROLL, seen.last().userAction)
  }

  @Test fun anOldNavigationEventCannotReplaceANewerManualCachePosition() {
    val interpreter = interpreter()
    val seen = mutableListOf<ScrollEventInterpreter.ScrollEventInterpretation>()
    interpreter.addListener { _, interpretation, _ -> seen.add(interpretation) }
    val time = SystemClock.uptimeMillis()
    val source = scroll(time).source!!
    fun event(offset: Long, index: Int, delta: Int) = scroll(time + offset, source).apply {
      fromIndex = index; toIndex = index + 2; scrollDeltaY = delta
    }
    interpreter.onAccessibilityEvent(event(0, 0, 100), null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    ScrollEventInterpreter.onWebNavigation(12, time + 100)
    val delayed = event(100, 10, 100)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.clearNavigationAt(time + 150)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    interpreter.onAccessibilityEvent(event(200, 20, 100), null)
    interpreter.onAccessibilityEvent(delayed, null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(100))
    interpreter.onAccessibilityEvent(event(300, 15, 0), null)
    assertTrue(seen[2].isFromNavigation)
    assertEquals(com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD,
      seen.last().scrollDirection)
  }

  @Test fun aDuplicatePositionStillKeepsItsNewerTimeAgainstALateNavigationEvent() {
    val interpreter = interpreter()
    val seen = mutableListOf<ScrollEventInterpreter.ScrollEventInterpretation>()
    interpreter.addListener { _, interpretation, _ -> seen.add(interpretation) }
    val time = SystemClock.uptimeMillis()
    val source = scroll(time).source!!
    fun event(offset: Long, index: Int, delta: Int) = scroll(time + offset, source).apply {
      fromIndex = index; toIndex = index + 2; scrollDeltaY = delta
    }
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(200))
    interpreter.onAccessibilityEvent(event(200, 20, 100), null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.onWebNavigation(12, time + 250)
    val delayed = event(250, 10, 100)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.startWebNavigation(time + 300)
    ScrollEventInterpreter.finishWebNavigationAction(12, time + 300, true)
    interpreter.onAccessibilityEvent(event(300, 20, 100), null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    ScrollEventInterpreter.clearNavigationAt(time + 350)
    interpreter.onAccessibilityEvent(delayed, null)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    interpreter.onAccessibilityEvent(event(400, 15, 0), null)
    assertTrue(seen[1].isDuplicateEvent)
    assertEquals(com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD,
      seen.last().scrollDirection)
  }
}
