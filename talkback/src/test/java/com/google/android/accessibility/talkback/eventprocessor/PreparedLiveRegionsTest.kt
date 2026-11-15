/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.eventprocessor

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.utils.ConcurrentNodeQueries.LiveRegionRead
import org.junit.Assert.*
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
class PreparedLiveRegionsTest {
  private fun change(label: String) =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply { contentDescription = label }
  private fun focus() = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED).apply {
    contentDescription = "focus"
    val node = AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
    }
    shadowOf(this).setSourceNode(node)
  }

  @Test fun earlyReadsAreJoinedOnceAndTheAlertStillPrecedesFocusSpeech() {
    var starts = 0; var finishes = 0; var fallbacks = 0
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { fallbacks++; false }, { changes ->
        starts++
        object : LiveRegionRead {
          override fun finish(): Map<AccessibilityEvent, Boolean> {
            finishes++
            return changes.associateWith { it.contentDescription == "alert" }
          }
          override fun cancel() { fail("An unchanged batch must be consumed") }
        }
      })
    queue.onAccessibilityEvent(change("alert"), null)
    queue.onAccessibilityEvent(change("polite"), null)
    val focus = focus()
    queue.prepareWebFocus(focus)
    assertEquals(1, starts)
    assertEquals(0, finishes)
    assertTrue(handled.isEmpty())
    queue.onAccessibilityEvent(focus, null)
    assertEquals(listOf("alert", "focus"), handled)
    assertEquals(1, finishes)
    assertEquals(0, fallbacks)
  }

  @Test fun aNewContentChangeCancelsAnEarlierAnswerAndRechecksAlertState() {
    var cancelled = 0; var finishes = 0
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { true }, { changes -> object : LiveRegionRead {
        override fun finish(): Map<AccessibilityEvent, Boolean> {
          finishes++; return changes.associateWith { false }
        }
        override fun cancel() { cancelled++ }
      } })
    queue.onAccessibilityEvent(change("old alert"), null)
    val focus = focus()
    queue.prepareWebFocus(focus)
    queue.onAccessibilityEvent(change("new alert"), null)
    queue.onAccessibilityEvent(focus, null)
    assertEquals(1, cancelled)
    assertEquals(0, finishes)
    assertEquals(listOf("old alert", "new alert", "focus"), handled)
  }

  @Test fun missingPreparedAnswersUseTheOriginalClassifier() {
    var fallbacks = 0
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { fallbacks++; true }, { object : LiveRegionRead {
        override fun finish(): Map<AccessibilityEvent, Boolean> = emptyMap()
        override fun cancel() {}
      } })
    queue.onAccessibilityEvent(change("alert"), null)
    val focus = focus()
    queue.prepareWebFocus(focus)
    queue.onAccessibilityEvent(focus, null)
    assertEquals(1, fallbacks)
    assertEquals(listOf("alert", "focus"), handled)
  }

  @Test fun clearingFeedbackCancelsOutstandingReads() {
    var cancelled = false
    val queue = ProcessorEventQueue({ _, _ -> }, { false }, { object : LiveRegionRead {
      override fun finish(): Map<AccessibilityEvent, Boolean> = error("Cancelled read was used")
      override fun cancel() { cancelled = true }
    } })
    queue.onAccessibilityEvent(change("change"), null)
    queue.prepareWebFocus(focus())
    queue.clearQueue()
    assertTrue(cancelled)
  }

  @Test fun anUnpreparedFocusClassifiesSynchronouslyWithoutStartingOptionalWorkers() {
    var starts = 0
    var classifications = 0
    val handled = mutableListOf<String>()
    val queue = ProcessorEventQueue({ e, _ -> handled.add(e.contentDescription.toString()) },
      { classifications++; true }, { starts++; error("Unprepared focus must not start a batch") })
    queue.onAccessibilityEvent(change("alert"), null)
    queue.onAccessibilityEvent(focus(), null)
    assertEquals(0, starts)
    assertEquals(1, classifications)
    assertEquals(listOf("alert", "focus"), handled)
  }

  @Test @Config(sdk = [26]) fun aWindowChangeOnAndroidEightInvalidatesWithoutNewerApis() {
    val queue = ProcessorEventQueue({ _, _ -> }, { false })
    queue.onAccessibilityEvent(AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED), null)
    queue.clearQueue()
  }
}
