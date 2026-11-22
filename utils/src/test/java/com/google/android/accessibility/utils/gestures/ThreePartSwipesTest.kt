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
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP
import android.content.Context
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_DOWN_AND_LEFT_AND_DOWN
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_LEFT_AND_RIGHT_AND_LEFT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider
import com.google.common.collect.ImmutableList
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** One-finger swipes in three directions, through the gesture matchers as SandFly sets them up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThreePartSwipesTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val pixelsPerCm = context.resources.displayMetrics.xdpi / 2.54f
  private val completed = mutableListOf<Int>()
  private val assigned = mutableSetOf<Int>()
  private var whenNothingFollows = false
  private val config =
    object : GestureConfigProvider {
      override fun isGestureAssigned(gestureId: Int) = gestureId in assigned

      override fun swipesBeforeLiftWhenNothingFollows() = whenNothingFollows
    }
  private val manifold: GestureManifold by lazy {
    GestureManifold(
      context,
      object : GestureManifold.Listener {
        override fun onGestureStarted(gestureId: Int) {}

        override fun onGestureCompleted(gestureEvent: AccessibilityGestureEvent) {
          completed += gestureEvent.gestureId
          manifold.clear()
        }

        override fun onGestureCancelled(gestureId: Int) {}
      },
      config,
      {},
      0,
      ImmutableList.copyOf(swipeMatchers()),
    )
  }

  /** The names of the matchers of every one-finger swipe in one, two and three directions. */
  private fun swipeMatchers(): List<String> {
    val directions = listOf("UP", "DOWN", "LEFT", "RIGHT")
    val names = mutableListOf<String>()
    for (a in directions) {
      names += "MAPPER_GESTURE_SWIPE_$a"
      for (b in directions - a) {
        names += "MAPPER_GESTURE_SWIPE_${a}_AND_$b"
        for (c in directions - b) {
          names += "MAPPER_GESTURE_SWIPE_${a}_AND_${b}_AND_$c"
        }
      }
    }
    return names
  }

  private val downTime = SystemClock.uptimeMillis()
  private var x = 5 * pixelsPerCm
  private var y = 10 * pixelsPerCm

  private fun send(action: Int) {
    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  private fun move(dxCm: Float, dyCm: Float, steps: Int = 4) {
    repeat(steps) {
      ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
      x += dxCm * pixelsPerCm / steps
      y += dyCm * pixelsPerCm / steps
      send(MotionEvent.ACTION_MOVE)
    }
  }

  /** One touch that moves by each pair of [cm], in cm, in turn, and lifts. */
  private fun stroke(vararg cm: Float) {
    send(MotionEvent.ACTION_DOWN)
    for (i in cm.indices step 2) {
      move(cm[i], cm[i + 1])
    }
    send(MotionEvent.ACTION_UP)
  }

  @Test
  fun rightUpRight_isItsGestureWhenAssigned() {
    assigned += GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
    stroke(2f, 0f, 0f, -2f, 2f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT), completed)
  }

  @Test
  fun leftRightLeft_isItsGestureWhenAssigned() {
    assigned += GESTURE_SWIPE_LEFT_AND_RIGHT_AND_LEFT
    stroke(-2f, 0f, 2f, 0f, -2f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_LEFT_AND_RIGHT_AND_LEFT), completed)
  }

  @Test
  fun downLeftDown_isItsGestureWhenAssigned() {
    assigned += GESTURE_SWIPE_DOWN_AND_LEFT_AND_DOWN
    stroke(0f, 2f, -2f, 0f, 0f, 2f)
    assertEquals(listOf(GESTURE_SWIPE_DOWN_AND_LEFT_AND_DOWN), completed)
  }

  @Test
  fun unassigned_threeParts_areNoGesture() {
    stroke(2f, 0f, 0f, -2f, 2f, 0f)
    assertTrue(completed.isEmpty())
  }

  @Test
  fun withAThreePartSwipeAssigned_oneAndTwoPartSwipesAreAsBefore() {
    assigned += GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
    stroke(2f, 0f)
    stroke(2f, 0f, 0f, -2f)
    stroke(-2f, 0f, 2f, 0f)
    assertEquals(
      listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT_AND_UP, GESTURE_SWIPE_LEFT_AND_RIGHT),
      completed,
    )
  }

  @Test
  fun aTinyThirdMove_staysATwoPartSwipe() {
    assigned += GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
    // Less than the distance between sampled points, so not a third part.
    stroke(2f, 0f, 0f, -2f, 0.1f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_AND_UP), completed)
  }

  @Test
  fun withAThreePartSwipeAssigned_theFirstSwipeWaitsForTheLift() {
    whenNothingFollows = true
    assigned += GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    move(1f, 0f, steps = 20)
    assertTrue(completed.isEmpty())
    move(0f, -2f)
    move(2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT), completed)
  }

  @Test
  fun whileSwipesRunBeforeTheLift_threePartsAreOff() {
    GestureConfiguration.setSwipesBeforeLift(true)
    try {
      assigned += GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT
      stroke(2f, 0f, 0f, -2f, 2f, 0f)
      assertTrue(GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT !in completed)
    } finally {
      GestureConfiguration.setSwipesBeforeLift(false)
    }
  }

  @Test
  fun segments_ofThreeStrokes_areThreeParts() {
    val cm = pixelsPerCm
    val points = mutableListOf<PointF>()
    var px = 0f
    var py = 0f
    for ((dx, dy) in listOf(1f to 0f, 0f to -1f, 1f to 0f)) {
      repeat(8) {
        px += dx * cm / 4
        py += dy * cm / 4
        points += PointF(px, py)
      }
    }
    points.add(0, PointF(0f, 0f))
    val path = Swipe.segments(points)
    assertEquals(4, path.size)
    val directions =
      (0 until path.size - 1).map {
        val dx = path[it + 1].x - path[it].x
        val dy = path[it + 1].y - path[it].y
        if (Math.abs(dx) > Math.abs(dy)) if (dx < 0) "left" else "right"
        else if (dy < 0) "up" else "down"
      }
    assertEquals(listOf("right", "up", "right"), directions)
  }

  @Test
  fun threeDirectionSwipes_startingWithASwipe() {
    val following =
      GestureConfiguration.threeDirectionSwipesStartingWith(
        android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
      )
    assertEquals(9, following.size)
    assertTrue(GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT in following)
    assertTrue(following.all { GestureConfiguration.isThreeDirectionSwipe(it) })
  }
}
