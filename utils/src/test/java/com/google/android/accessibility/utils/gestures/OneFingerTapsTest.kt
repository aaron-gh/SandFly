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
import android.accessibilityservice.AccessibilityService.GESTURE_DOUBLE_TAP
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_QUADRUPLE_TAP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_TRIPLE_TAP
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
 * One-finger double and triple taps run at their last tap when no one-finger gesture of more taps
 * has an action, and otherwise wait for a further tap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OneFingerTapsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
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
      },
      {},
      0,
      ImmutableList.of(
        "MAPPER_GESTURE_DOUBLE_TAP",
        "MAPPER_GESTURE_DOUBLE_TAP_AND_HOLD",
        "MAPPER_GESTURE_TRIPLE_TAP",
        "MAPPER_GESTURE_QUADRUPLE_TAP",
      ),
    )
  }
  private var downTime = 0L

  private fun idle(ms: Long) = ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)

  private fun send(action: Int) {
    val now = SystemClock.uptimeMillis()
    if (action == ACTION_DOWN) {
      downTime = now
    }
    val event = MotionEvent.obtain(downTime, now, action, 160f, 240f, 0)
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  /** One finger taps [taps] times in one place, 100 ms apart. */
  private fun taps(taps: Int) {
    repeat(taps) { tap ->
      if (tap > 0) {
        idle(100)
      }
      send(ACTION_DOWN)
      idle(50)
      send(ACTION_UP)
    }
  }

  @Test
  fun doubleTapRunsAtItsLastTapWhenNoGestureOfMoreTapsHasAnAction() {
    taps(2)
    assertEquals(listOf(GESTURE_DOUBLE_TAP), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_DOUBLE_TAP), completed)
  }

  @Test
  fun doubleTapWaitsWhenTheTripleTapHasAnAction() {
    assigned += GESTURE_TRIPLE_TAP
    taps(2)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_DOUBLE_TAP), completed)
  }

  @Test
  fun doubleTapWaitsWhenTheQuadrupleTapHasAnAction() {
    assigned += GESTURE_QUADRUPLE_TAP
    taps(2)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_DOUBLE_TAP), completed)
  }

  @Test
  fun tripleTapRunsAtItsLastTapWhenTheQuadrupleTapHasNoAction() {
    assigned += GESTURE_TRIPLE_TAP
    taps(3)
    assertEquals(listOf(GESTURE_TRIPLE_TAP), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_TRIPLE_TAP), completed)
  }

  @Test
  fun tripleTapWaitsWhenTheQuadrupleTapHasAnAction() {
    assigned += listOf(GESTURE_TRIPLE_TAP, GESTURE_QUADRUPLE_TAP)
    taps(3)
    assertEquals(listOf<Int>(), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_TRIPLE_TAP), completed)
  }

  @Test
  fun quadrupleTapRunsAtItsLastTap() {
    assigned += listOf(GESTURE_TRIPLE_TAP, GESTURE_QUADRUPLE_TAP)
    taps(4)
    assertEquals(listOf(GESTURE_QUADRUPLE_TAP), completed)
    idle(1000)
    assertEquals(listOf(GESTURE_QUADRUPLE_TAP), completed)
  }

  @Test
  fun threeTapsAreADoubleTapWhenNoGestureOfMoreTapsHasAnAction() {
    taps(3)
    assertEquals(listOf(GESTURE_DOUBLE_TAP), completed)
  }
}
