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
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider
import com.google.common.collect.ImmutableList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureStartTimeTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val pixelsPerCm = context.resources.displayMetrics.xdpi / 2.54f
  private val completed = mutableListOf<Int>()
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
      object : GestureConfigProvider {},
      {},
      0,
      ImmutableList.of("MAPPER_GESTURE_SWIPE_RIGHT", "MAPPER_GESTURE_SWIPE_LEFT"),
    )
  }
  private val downTime = SystemClock.uptimeMillis()
  private var x = 5 * pixelsPerCm
  private var y = 10 * pixelsPerCm

  @After
  fun resetStartTime() =
    GestureConfiguration.setSwipeStartTimeMs(GestureConfiguration.DEFAULT_SWIPE_START_TIME_MS)

  private fun send(action: Int) {
    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  private fun move(dxCm: Float, steps: Int, stepMs: Long) {
    repeat(steps) {
      ShadowLooper.idleMainLooper(stepMs, TimeUnit.MILLISECONDS)
      x += dxCm * pixelsPerCm / steps
      send(MotionEvent.ACTION_MOVE)
    }
  }

  // Moves 0.9 cm in 0.225 seconds, then 1.5 cm quickly: a slow start, then a swipe.
  private fun slowStartSwipeRight() {
    send(MotionEvent.ACTION_DOWN)
    move(0.9f, 9, 25)
    move(1.5f, 3, 10)
    send(MotionEvent.ACTION_UP)
  }

  @Test
  fun defaultTime_isUnchanged() {
    assertEquals(150, GestureConfiguration.scaleSwipeStartThresholdMs(150))
    assertEquals(170, GestureConfiguration.scaleSwipeStartThresholdMs(170))
  }

  @Test
  fun longerTime_scalesTheThreshold() {
    GestureConfiguration.setSwipeStartTimeMs(300)
    assertEquals(300, GestureConfiguration.scaleSwipeStartThresholdMs(150))
    assertEquals(340, GestureConfiguration.scaleSwipeStartThresholdMs(170))
  }

  @Test
  fun valuesOutsideTheChoices_fallBackToTheDefault() {
    GestureConfiguration.setSwipeStartTimeMs(0)
    assertEquals(150, GestureConfiguration.getSwipeStartTimeMs())
    GestureConfiguration.setSwipeStartTimeMs(100)
    assertEquals(150, GestureConfiguration.getSwipeStartTimeMs())
    GestureConfiguration.setSwipeStartTimeMs(1_000)
    assertEquals(150, GestureConfiguration.getSwipeStartTimeMs())
    GestureConfiguration.setSwipeStartTimeMs(250)
    assertEquals(250, GestureConfiguration.getSwipeStartTimeMs())
  }

  @Test
  fun slowStart_withTheDefaultTime_isNoSwipe() {
    slowStartSwipeRight()
    assertTrue(completed.isEmpty())
  }

  @Test
  fun slowStart_withALongerTime_isASwipe() {
    GestureConfiguration.setSwipeStartTimeMs(250)
    slowStartSwipeRight()
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }
}
