/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.eventprocessor

import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
import android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class ProcessorEventQueueWebFocusTest {
  private val handled = mutableListOf<String>()
  private val queue =
    ProcessorEventQueue { event, _ -> handled.add(event.contentDescription.toString()) }

  private fun event(type: Int, label: String): AccessibilityEvent =
    AccessibilityEvent.obtain(type).apply { contentDescription = label }

  private fun focus(label: String, web: Boolean): AccessibilityEvent {
    val node =
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
        className = "android.view.View"
        text = label
        if (web) {
          addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
          addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT)
        }
      }
    return event(TYPE_VIEW_ACCESSIBILITY_FOCUSED, label).also { shadowOf(it).setSourceNode(node) }
  }

  private fun send(event: AccessibilityEvent) = queue.onAccessibilityEvent(event, null)

  private fun runQueue() = shadowOf(Looper.getMainLooper()).idle()

  @Test
  fun webFocusIsHandledAtOnceAheadOfContentChanges() {
    repeat(50) { send(event(TYPE_WINDOW_CONTENT_CHANGED, "content-$it")) }
    repeat(20) {
      send(focus("item-$it", web = true))
      assertEquals("item-$it", handled.last())
    }
    assertEquals((0 until 20).map { "item-$it" }, handled)

    runQueue()

    assertEquals((0 until 50).map { "content-$it" }, handled.drop(20))
  }

  @Test
  fun queuedEventsWhoseSpeechCouldInterruptWebFocusAreHandledFirst() {
    send(event(TYPE_WINDOW_CONTENT_CHANGED, "content-1"))
    send(event(TYPE_WINDOW_STATE_CHANGED, "window"))
    send(event(TYPE_VIEW_SCROLLED, "scrolled"))
    send(event(TYPE_WINDOW_CONTENT_CHANGED, "content-2"))
    send(event(TYPE_ANNOUNCEMENT, "announcement"))
    send(event(TYPE_NOTIFICATION_STATE_CHANGED, "notification"))
    send(event(TYPE_VIEW_TEXT_CHANGED, "text"))
    send(focus("native", web = false))

    send(focus("web", web = true))

    assertEquals(listOf("window", "announcement", "notification", "native", "web"), handled)
    runQueue()
    assertEquals(
      listOf(
        "window",
        "announcement",
        "notification",
        "native",
        "web",
        "content-1",
        "scrolled",
        "content-2",
        "text",
      ),
      handled,
    )
  }

  @Test
  fun eventsLeftQueuedAreHandledInTimeSlices() {
    // Each event takes 10 ms, so a time slice of 16 ms handles two of them.
    val slowHandled = mutableListOf<String>()
    val slowQueue =
      ProcessorEventQueue { event, _ ->
        slowHandled.add(event.contentDescription.toString())
        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
      }
    repeat(6) { slowQueue.onAccessibilityEvent(event(TYPE_VIEW_CLICKED, "clicked-$it"), null) }
    slowQueue.onAccessibilityEvent(focus("web", web = true), null)
    assertEquals(listOf("web"), slowHandled)

    shadowOf(Looper.getMainLooper()).runOneTask()

    assertEquals(listOf("web", "clicked-0", "clicked-1"), slowHandled)
  }

  @Test
  fun nativeFocusStaysQueued() {
    send(focus("first", web = false))
    send(focus("second", web = false))
    assertTrue(handled.isEmpty())

    runQueue()

    assertEquals(listOf("first", "second"), handled)
  }

  @Test
  fun focusWithoutSourceStaysQueued() {
    send(event(TYPE_VIEW_ACCESSIBILITY_FOCUSED, "no source"))
    assertTrue(handled.isEmpty())

    runQueue()

    assertEquals(listOf("no source"), handled)
  }

  @Test
  fun queuedFocusLimitStillCountsAfterEventsAreTakenOut() {
    // Two native focus events are taken out ahead of the web focus, so the queue's count of them
    // must drop to zero, or later focus events would be pruned below the limit of two.
    send(focus("native-1", web = false))
    send(focus("native-2", web = false))
    send(focus("web", web = true))
    send(focus("native-3", web = false))
    send(focus("native-4", web = false))

    runQueue()

    assertEquals(listOf("native-1", "native-2", "web", "native-3", "native-4"), handled)
  }
}
