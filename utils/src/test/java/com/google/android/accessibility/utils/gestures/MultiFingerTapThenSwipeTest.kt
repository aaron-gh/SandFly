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

package com.google.android.accessibility.utils.gestures

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_DOWN
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider
import com.google.common.collect.ImmutableList
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Taps with several fingers followed by a swipe with as many fingers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiFingerTapThenSwipeTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val pixelsPerCm = context.resources.displayMetrics.ydpi / 2.54f
  private val completed = mutableListOf<Int>()
  private val assigned = mutableSetOf<Int>()
  private val manifold by lazy {
    GestureManifold(
        context,
        object : GestureManifold.Listener {
          override fun onGestureStarted(gestureId: Int) {}

          override fun onGestureCompleted(gestureEvent: AccessibilityGestureEvent) {
            completed += gestureEvent.gestureId
          }

          override fun onGestureCancelled(gestureId: Int) {}
        },
        object : GestureConfigProvider {
          override fun isGestureAssigned(gestureId: Int): Boolean = gestureId in assigned

          override fun tapsRunWhenNothingFollows(): Boolean = true
        },
        {},
        0,
        ImmutableList.of(
          "MAPPER_GESTURE_2_FINGER_SINGLE_TAP",
          "MAPPER_GESTURE_2_FINGER_SINGLE_TAP_AND_HOLD",
          "MAPPER_GESTURE_2_FINGER_DOUBLE_TAP",
          "MAPPER_GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD",
          "MAPPER_GESTURE_2_FINGER_TRIPLE_TAP",
          "MAPPER_GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD",
          "MAPPER_GESTURE_3_FINGER_SINGLE_TAP",
          "MAPPER_GESTURE_3_FINGER_DOUBLE_TAP",
          "MAPPER_GESTURE_3_FINGER_TRIPLE_TAP",
          "MAPPER_GESTURE_2_FINGER_SWIPE_UP",
          "MAPPER_GESTURE_2_FINGER_SWIPE_LEFT",
          "MAPPER_GESTURE_3_FINGER_SWIPE_DOWN",
          "MAPPER_GESTURE_2_FINGER_SINGLE_TAP_SWIPE_UP",
          "MAPPER_GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT",
          "MAPPER_GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP",
          "MAPPER_GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_DOWN",
          "MAPPER_GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_DOWN",
        ),
      )
      .apply { setMultiFingerGesturesEnabled(true) }
  }
  private val startX = 4 * pixelsPerCm
  private val startY = 8 * pixelsPerCm
  private val fingerGap = pixelsPerCm
  private var downTime = 0L

  private fun wait(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

  /** Sends [action] for the first [count] fingers, each moved by [dx] and [dy] pixels. */
  private fun send(action: Int, actionIndex: Int, count: Int, dx: Float = 0f, dy: Float = 0f) {
    val properties =
      Array(count) { i ->
        MotionEvent.PointerProperties().apply {
          id = i
          toolType = MotionEvent.TOOL_TYPE_FINGER
        }
      }
    val coords =
      Array(count) { i ->
        MotionEvent.PointerCoords().apply {
          x = startX + i * fingerGap + dx
          y = startY + dy
          pressure = 1f
          size = 1f
        }
      }
    val fullAction =
      if (action == ACTION_POINTER_DOWN || action == ACTION_POINTER_UP) {
        action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      } else {
        action
      }
    val event =
      MotionEvent.obtain(
        downTime,
        SystemClock.uptimeMillis(),
        fullAction,
        count,
        properties,
        coords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0,
      )
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  private fun fingersDown(fingers: Int) {
    downTime = SystemClock.uptimeMillis()
    send(ACTION_DOWN, 0, 1)
    for (i in 1 until fingers) {
      wait(5)
      send(ACTION_POINTER_DOWN, i, i + 1)
    }
  }

  private fun fingersUp(fingers: Int, dx: Float = 0f, dy: Float = 0f) {
    for (i in fingers - 1 downTo 1) {
      send(ACTION_POINTER_UP, i, i + 1, dx, dy)
      wait(5)
    }
    send(ACTION_UP, 0, 1, dx, dy)
  }

  /** [fingers] fingers tap together [taps] times, 100 ms apart, and 100 ms pass. */
  private fun taps(fingers: Int, taps: Int) {
    repeat(taps) {
      fingersDown(fingers)
      wait(50)
      fingersUp(fingers)
      wait(100)
    }
  }

  /** [fingers] fingers land and move 3 cm by [dxCm] and [dyCm] steps of 0.3 cm, and lift. */
  private fun swipe(fingers: Int, dxCm: Float, dyCm: Float) {
    fingersDown(fingers)
    var dx = 0f
    var dy = 0f
    repeat(10) {
      wait(10)
      dx += dxCm * 0.3f * pixelsPerCm
      dy += dyCm * 0.3f * pixelsPerCm
      send(ACTION_MOVE, 0, fingers, dx, dy)
    }
    fingersUp(fingers, dx, dy)
  }

  @Test
  fun doubleTapThenSwipeUpWithTwoFingers() {
    assigned += GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP
    taps(fingers = 2, taps = 2)
    swipe(fingers = 2, dxCm = 0f, dyCm = -1f)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP), completed)
  }

  @Test
  fun tapThenSwipeLeftWithTwoFingers() {
    assigned += GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT
    taps(fingers = 2, taps = 1)
    swipe(fingers = 2, dxCm = -1f, dyCm = 0f)
    assertEquals(listOf(GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT), completed)
  }

  @Test
  fun tripleTapThenSwipeDownWithThreeFingers() {
    assigned += GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_DOWN
    taps(fingers = 3, taps = 3)
    swipe(fingers = 3, dxCm = 0f, dyCm = 1f)
    assertEquals(listOf(GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_DOWN), completed)
  }

  @Test
  fun swipeInAnotherDirectionIsNotTheGesture() {
    assigned += GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP
    taps(fingers = 2, taps = 2)
    swipe(fingers = 2, dxCm = 1f, dyCm = 0f)
    wait(1000)
    assertEquals(listOf<Int>(), completed)
  }

  @Test
  fun swipeAfterOtherTapsIsNotTheGesture() {
    assigned += GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP
    taps(fingers = 2, taps = 1)
    swipe(fingers = 2, dxCm = 0f, dyCm = -1f)
    wait(1000)
    assertEquals(false, GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP in completed)
  }

  @Test
  fun lateSwipeIsNotTheGesture() {
    assigned += listOf(GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP, GESTURE_2_FINGER_DOUBLE_TAP)
    taps(fingers = 2, taps = 2)
    wait(1000)
    swipe(fingers = 2, dxCm = 0f, dyCm = -1f)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun doubleTapWaitsForASwipeThatHasAnAction() {
    assigned += listOf(GESTURE_2_FINGER_DOUBLE_TAP, GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP)
    fingersDown(2)
    wait(50)
    fingersUp(2)
    wait(100)
    fingersDown(2)
    wait(50)
    fingersUp(2)
    assertEquals(listOf<Int>(), completed)
    wait(1000)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun doubleTapRunsAtItsLastTapWhenNoSwipeAfterItHasAnAction() {
    // Only a swipe after a single tap has an action, which can't follow a double tap.
    assigned += listOf(GESTURE_2_FINGER_DOUBLE_TAP, GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT)
    fingersDown(2)
    wait(50)
    fingersUp(2)
    wait(100)
    fingersDown(2)
    wait(50)
    fingersUp(2)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun singleTapWaitsForASwipeThatHasAnAction() {
    assigned += listOf(GESTURE_2_FINGER_SINGLE_TAP, GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT)
    fingersDown(2)
    wait(50)
    fingersUp(2)
    assertEquals(listOf<Int>(), completed)
    wait(1000)
    assertEquals(listOf(GESTURE_2_FINGER_SINGLE_TAP), completed)
  }
}
