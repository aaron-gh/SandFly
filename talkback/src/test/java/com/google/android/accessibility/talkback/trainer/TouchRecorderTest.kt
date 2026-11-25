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

package com.google.android.accessibility.talkback.trainer

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchRecorderTest {
  private val recorder = TouchRecorder()

  @Test
  fun fingerLiftedFirst_endsAtItsPointerUp() {
    recorder.onMotionEvent(event(ACTION_DOWN, 0, 0 to (100f to 100f)))
    recorder.onMotionEvent(event(ACTION_POINTER_DOWN, 20, 0 to (100f to 100f), 1 to (200f to 100f), index = 1))
    recorder.onMotionEvent(event(ACTION_MOVE, 40, 0 to (100f to 150f), 1 to (200f to 150f)))
    recorder.onMotionEvent(event(ACTION_POINTER_UP, 60, 0 to (100f to 160f), 1 to (200f to 160f), index = 0))
    // Pointer 1 is now the only finger, at index 0.
    recorder.onMotionEvent(event(ACTION_MOVE, 80, 1 to (200f to 200f)))
    assertTrue(recorder.fingersDown)
    recorder.onMotionEvent(event(ACTION_UP, 100, 1 to (200f to 210f)))

    assertFalse(recorder.fingersDown)
    val touch = recorder.touches.single()
    assertEquals(2, touch.mostFingersAtOnce)
    val (first, second) = touch.fingers
    assertEquals(0, first.pointerId)
    assertEquals(60L, first.upMs)
    assertEquals(160f, first.end.y)
    assertEquals(1, second.pointerId)
    assertEquals(100L, second.upMs)
    assertEquals(210f, second.end.y)
    assertEquals(20L, touch.landingSpreadMs)
    assertEquals(100L, touch.durationMs)
  }

  @Test
  fun cancel_liftsEveryFinger() {
    recorder.onMotionEvent(event(ACTION_DOWN, 0, 0 to (100f to 100f)))
    recorder.onMotionEvent(event(ACTION_POINTER_DOWN, 10, 0 to (100f to 100f), 1 to (200f to 100f), index = 1))
    recorder.onMotionEvent(event(ACTION_CANCEL, 50, 0 to (100f to 120f), 1 to (200f to 120f)))

    assertFalse(recorder.fingersDown)
    val touch = recorder.touches.single()
    assertTrue(touch.fingers.all { it.upMs == 50L })
  }

  @Test
  fun endUnseenTouch_liftsTheFingersWhereTheyWereLastSeen() {
    recorder.onMotionEvent(event(ACTION_DOWN, 0, 0 to (100f to 100f)))
    recorder.onMotionEvent(event(ACTION_MOVE, 30, 0 to (110f to 100f)))
    recorder.endUnseenTouch(timeMs = 500, explored = true)

    assertFalse(recorder.fingersDown)
    val touch = recorder.touches.single()
    assertTrue(touch.becameExploration)
    assertEquals(500L, touch.upMs)
    assertEquals(110f, touch.fingers.single().end.x)
    // Events of the ended touch that still arrive change nothing.
    recorder.onMotionEvent(event(ACTION_MOVE, 600, 0 to (300f to 100f)))
    assertEquals(110f, touch.fingers.single().end.x)
  }

  @Test
  fun endUnseenTouch_withNoFingerDown_doesNothing() {
    recorder.onMotionEvent(event(ACTION_DOWN, 0, 0 to (100f to 100f)))
    recorder.onMotionEvent(event(ACTION_UP, 50, 0 to (100f to 100f)))
    recorder.endUnseenTouch(timeMs = 500, explored = true)

    val touch = recorder.touches.single()
    assertFalse(touch.becameExploration)
    assertEquals(50L, touch.upMs)
  }

  @Test
  fun fingerHeldDown_keepsAtMostTheCapAndItsLatestPoint() {
    recorder.onMotionEvent(event(ACTION_DOWN, 0, 0 to (0f to 0f)))
    for (t in 1..FingerPath.MAX_POINTS + 500) {
      recorder.onMotionEvent(event(ACTION_MOVE, t.toLong(), 0 to (t.toFloat() to 0f)))
    }
    val finger = recorder.touches.single().fingers.single()
    assertEquals(FingerPath.MAX_POINTS, finger.points.size)
    assertEquals((FingerPath.MAX_POINTS + 500).toFloat(), finger.end.x)
  }

  /** A motion event at [timeMs] with each finger as pointer id to (x to y). */
  private fun event(
    action: Int,
    timeMs: Long,
    vararg fingers: Pair<Int, Pair<Float, Float>>,
    index: Int = 0,
  ): MotionEvent {
    val properties =
      Array(fingers.size) { i ->
        MotionEvent.PointerProperties().apply {
          id = fingers[i].first
          toolType = MotionEvent.TOOL_TYPE_FINGER
        }
      }
    val coords =
      Array(fingers.size) { i ->
        MotionEvent.PointerCoords().apply {
          x = fingers[i].second.first
          y = fingers[i].second.second
          pressure = 1f
          size = 1f
        }
      }
    val fullAction =
      if (action == ACTION_POINTER_DOWN || action == ACTION_POINTER_UP) {
        action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      } else {
        action
      }
    return MotionEvent.obtain(0, timeMs, fullAction, fingers.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
  }
}
