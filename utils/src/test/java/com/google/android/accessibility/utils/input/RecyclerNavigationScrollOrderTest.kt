/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils.input

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.monitor.TouchMonitor
import com.google.android.accessibility.utils.output.ScrollActionRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecyclerNavigationScrollOrderTest {
  @Before fun setUp() { ScrollEventInterpreter.resetNavigation() }
  @After fun tearDown() { ScrollEventInterpreter.resetNavigation() }

  @Test fun lateProgressIsStillFromNavigation() {
    val sample = Sequence()
    sample.newSwipe()
    val progress = sample.scroll(1090L, 550)
    assertTrue(progress.isFromNavigation)
    assertEquals(ScrollActionRecord.ACTION_UNKNOWN, progress.userAction)
    assertEquals(-1, progress.scrollInstanceId)
  }

  @Test fun directTouchKeepsManualScrollHandling() {
    val sample = Sequence()
    sample.initialProgress()
    ScrollEventInterpreter.clearNavigationAt(1060L)
    sample.newSwipe()
    val progress = sample.scroll(1090L, 550)
    assertFalse(progress.isFromNavigation)
    assertEquals(ScrollActionRecord.ACTION_MANUAL_SCROLL, progress.userAction)
  }

  @Test fun anotherListCannotUseAnEarlierListsScroll() {
    val sample = Sequence()
    sample.initialProgress()
    sample.newSwipe()
    val progress = sample.scroll(1090L, 550, recycler())
    assertFalse(progress.isFromNavigation)
    assertEquals(ScrollActionRecord.ACTION_MANUAL_SCROLL, progress.userAction)
  }

  private class Sequence {
    private val node = recycler()
    private val compat = AccessibilityNodeInfoCompat.wrap(node)
    private var active: ScrollActionRecord = swipe(1, 1000L)
    private var superseded: ScrollActionRecord? = null
    private val received = mutableListOf<ScrollEventInterpreter.ScrollEventInterpretation>()
    private val interpreter = object : ScrollEventInterpreter(null, TouchMonitor(), false) {
      override fun readSource(
        event: AccessibilityEvent,
        fromWebNavigation: Boolean,
      ): AccessibilityNodeInfo? = event.source
    }.apply {
      setScrollActorState { active }
      setSupersededScrollActionRecord { superseded }
      addListener { _, interpretation, _ -> received.add(interpretation) }
    }

    private fun swipe(id: Int, start: Long) =
      ScrollActionRecord(id, null, compat, ScrollActionRecord.ACTION_AUTO_SCROLL, start,
        ScrollActionRecord.FOCUS)

    fun initialProgress() {
      assertEquals(ScrollActionRecord.ACTION_AUTO_SCROLL, scroll(1030L, 100).userAction)
    }

    fun newSwipe() {
      superseded = active
      active = swipe(2, 1100L)
    }

    fun scroll(time: Long, y: Int, source: AccessibilityNodeInfo = node):
        ScrollEventInterpreter.ScrollEventInterpretation {
      val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
        eventTime = time
        fromIndex = 3
        toIndex = 4
        itemCount = 30
        scrollY = y
        scrollDeltaY = 30
        shadowOf(this).setSourceNode(source)
      }
      interpreter.onAccessibilityEvent(event, null)
      return received.last()
    }
  }

  private companion object {
    fun recycler(): AccessibilityNodeInfo =
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
        className = "androidx.recyclerview.widget.RecyclerView"
        isScrollable = true
      }
  }
}
