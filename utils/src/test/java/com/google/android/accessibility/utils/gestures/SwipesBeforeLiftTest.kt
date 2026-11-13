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
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider
import com.google.common.collect.ImmutableList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipesBeforeLiftTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val pixelsPerCm = context.resources.displayMetrics.xdpi / 2.54f
  private val completed = mutableListOf<Int>()
  private val assigned = mutableSetOf<Int>()
  private var edgeSwipePossible = false
  // Like TouchInteractionMonitor, the rest of a touch that performed a gesture before the lift
  // goes elsewhere.
  private var followingHeldTouch = false
  private val config =
    object : GestureConfigProvider {
      override fun isGestureAssigned(gestureId: Int) = gestureId in assigned

      override fun holdsSwipeUntilLift() = edgeSwipePossible
    }
  private val manifold: GestureManifold by lazy {
    GestureManifold(
      context,
      object : GestureManifold.Listener {
        override fun onGestureStarted(gestureId: Int) {}

        override fun onGestureCompleted(gestureEvent: AccessibilityGestureEvent) {
          completed += gestureEvent.gestureId
          manifold.clear()
          followingHeldTouch = true
        }

        override fun onGestureCancelled(gestureId: Int) {}
      },
      config,
      {},
      0,
      ImmutableList.of(
        "MAPPER_GESTURE_SWIPE_RIGHT",
        "MAPPER_GESTURE_SWIPE_LEFT",
        "MAPPER_GESTURE_SWIPE_UP",
        "MAPPER_GESTURE_SWIPE_RIGHT_AND_LEFT",
        "MAPPER_GESTURE_SWIPE_LEFT_AND_UP",
      ),
    )
  }
  private val downTime = SystemClock.uptimeMillis()
  private var x = 5 * pixelsPerCm
  private var y = 10 * pixelsPerCm

  @Before fun turnOn() = GestureConfiguration.setSwipesBeforeLift(true)

  @After fun turnOff() = GestureConfiguration.setSwipesBeforeLift(false)

  private fun send(action: Int) {
    if (followingHeldTouch) {
      followingHeldTouch = action != MotionEvent.ACTION_UP
      return
    }
    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  private fun move(dxCm: Float, dyCm: Float) {
    repeat(4) {
      ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
      x += dxCm * pixelsPerCm / 4
      y += dyCm * pixelsPerCm / 4
      send(MotionEvent.ACTION_MOVE)
    }
  }

  @Test
  fun swipeRight_runsBeforeTheLift() {
    send(MotionEvent.ACTION_DOWN)
    move(1.5f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
    move(2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun swipeUp_runsBeforeTheLift() {
    send(MotionEvent.ACTION_DOWN)
    move(0f, -1.5f)
    assertEquals(listOf(GESTURE_SWIPE_UP), completed)
  }

  @Test
  fun rightThenLeft_isTheRightSwipe() {
    assigned += GESTURE_SWIPE_RIGHT_AND_LEFT
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    move(-2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun withTheHoldAssigned_theSwipeWaitsForTheLift() {
    assigned += GestureManifold.GESTURE_SWIPE_RIGHT_AND_HOLD
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun withTheHoldAssigned_anAngleIsNoGesture() {
    assigned += GestureManifold.GESTURE_SWIPE_LEFT_AND_HOLD
    assigned += GESTURE_SWIPE_LEFT_AND_UP
    send(MotionEvent.ACTION_DOWN)
    move(-2f, 0f)
    move(0f, -2f)
    send(MotionEvent.ACTION_UP)
    assertTrue(completed.isEmpty())
  }

  @Test
  fun whileAnEdgeSwipeIsPossible_theSwipeWaitsForTheLift() {
    edgeSwipePossible = true
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun off_theSwipeWaitsForTheLiftAndAnglesWork() {
    GestureConfiguration.setSwipesBeforeLift(false)
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    move(-2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_AND_LEFT), completed)
  }

  @Test
  fun twoDirectionSwipes_includeTheirHolds() {
    assertTrue(GestureConfiguration.isTwoDirectionSwipe(GESTURE_SWIPE_RIGHT_AND_LEFT))
    assertTrue(GestureConfiguration.isTwoDirectionSwipe(GESTURE_SWIPE_LEFT_AND_UP))
    assertTrue(
      GestureConfiguration.isTwoDirectionSwipe(GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD)
    )
    assertTrue(
      GestureConfiguration.isTwoDirectionSwipe(
        GestureManifold.GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD
      )
    )
    assertFalse(GestureConfiguration.isTwoDirectionSwipe(GESTURE_SWIPE_RIGHT))
    assertFalse(
      GestureConfiguration.isTwoDirectionSwipe(GestureManifold.GESTURE_SWIPE_RIGHT_AND_HOLD)
    )
  }
}
