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
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD
import android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_3_FINGER_QUADRUPLE_TAP
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

/**
 * Multi-finger multi-taps run at their last tap when no gesture that goes on after their taps has
 * an action, and otherwise wait for a further tap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiFingerTapsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val completed = mutableListOf<Int>()
  private val assigned = mutableSetOf<Int>()
  private var runWhenNothingFollows = true
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

          override fun tapsRunWhenNothingFollows(): Boolean = runWhenNothingFollows
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
          "MAPPER_GESTURE_3_FINGER_QUADRUPLE_TAP",
        ),
      )
      .apply { setMultiFingerGesturesEnabled(true) }
  }
  private val xs = floatArrayOf(100f, 160f, 220f, 280f)
  private val y = 300f
  private var downTime = 0L

  private fun idle(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

  private fun send(action: Int, actionIndex: Int, count: Int) {
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
          x = xs[i]
          y = this@MultiFingerTapsTest.y
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

  /** [fingers] fingers tap together [taps] times, 100 ms apart. */
  private fun taps(fingers: Int, taps: Int) {
    repeat(taps) { tap ->
      if (tap > 0) {
        idle(100)
      }
      downTime = SystemClock.uptimeMillis()
      send(ACTION_DOWN, 0, 1)
      for (i in 1 until fingers) {
        idle(5)
        send(ACTION_POINTER_DOWN, i, i + 1)
      }
      idle(50)
      for (i in fingers - 1 downTo 1) {
        send(ACTION_POINTER_UP, i, i + 1)
        idle(5)
      }
      send(ACTION_UP, 0, 1)
    }
  }

  @Test
  fun twoFingerDoubleTapRunsAtItsLastTapWhenNoLongerTapHasAnAction() {
    assigned += GESTURE_2_FINGER_DOUBLE_TAP
    taps(fingers = 2, taps = 2)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun twoFingerDoubleTapWaitsWhenTheTripleTapHasAnAction() {
    assigned += listOf(GESTURE_2_FINGER_DOUBLE_TAP, GESTURE_2_FINGER_TRIPLE_TAP)
    taps(fingers = 2, taps = 2)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun twoFingerDoubleTapWaitsWhenTheTripleTapAndHoldHasAnAction() {
    assigned += listOf(GESTURE_2_FINGER_DOUBLE_TAP, GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD)
    taps(fingers = 2, taps = 2)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }

  @Test
  fun twoFingerTripleTapRunsAtItsLastTap() {
    assigned +=
      listOf(
        GESTURE_2_FINGER_DOUBLE_TAP,
        GESTURE_2_FINGER_TRIPLE_TAP,
        GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD,
      )
    taps(fingers = 2, taps = 3)
    assertEquals(listOf(GESTURE_2_FINGER_TRIPLE_TAP), completed)
  }

  @Test
  fun threeFingerQuadrupleTapRunsAtItsLastTap() {
    assigned += listOf(GESTURE_3_FINGER_TRIPLE_TAP, GESTURE_3_FINGER_QUADRUPLE_TAP)
    taps(fingers = 3, taps = 4)
    assertEquals(listOf(GESTURE_3_FINGER_QUADRUPLE_TAP), completed)
  }

  @Test
  fun threeFingerTripleTapWaitsWhenTheQuadrupleTapHasAnAction() {
    assigned += listOf(GESTURE_3_FINGER_TRIPLE_TAP, GESTURE_3_FINGER_QUADRUPLE_TAP)
    taps(fingers = 3, taps = 3)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_3_FINGER_TRIPLE_TAP), completed)
  }

  @Test
  fun tapsWaitWhenTheProviderKeepsTheWait() {
    runWhenNothingFollows = false
    assigned += GESTURE_2_FINGER_DOUBLE_TAP
    taps(fingers = 2, taps = 2)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_2_FINGER_DOUBLE_TAP), completed)
  }
}
