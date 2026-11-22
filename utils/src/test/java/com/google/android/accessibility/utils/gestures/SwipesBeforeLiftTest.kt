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
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider
import com.google.common.collect.ImmutableList
import java.util.concurrent.TimeUnit
import java.util.function.IntPredicate
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
  // Whether swipes run before the lift, with the switch off, while nothing starts with them.
  private var whenNothingFollows = false
  // Like TouchInteractionMonitor, the rest of a touch that performed a gesture before the lift
  // goes elsewhere.
  private var followingHeldTouch = false
  private val config =
    object : GestureConfigProvider {
      override fun isGestureAssigned(gestureId: Int) = gestureId in assigned

      override fun holdsSwipeUntilLift() = edgeSwipePossible

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
        "MAPPER_GESTURE_2_FINGER_SWIPE_RIGHT",
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
    // A gesture that completed with the lift leaves no rest of the touch.
    if (action == MotionEvent.ACTION_UP) {
      followingHeldTouch = false
    }
  }

  private fun move(dxCm: Float, dyCm: Float, steps: Int = 4) {
    repeat(steps) {
      ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
      x += dxCm * pixelsPerCm / steps
      y += dyCm * pixelsPerCm / steps
      send(MotionEvent.ACTION_MOVE)
    }
  }

  // Moves of 10 ms after a 40 ms swipe that pass the time a second finger may land.
  private val stepsPastSecondFinger = GestureConfiguration.multiFingerSwipeFingerTimeoutMs(2) / 10 - 3

  /** Swipes 1.5 cm in 40 ms, then 1 cm more, past the time a second finger may land. */
  private fun swipePastSecondFinger(dxCm: Float, dyCm: Float) {
    move(dxCm * 1.5f, dyCm * 1.5f)
    move(dxCm, dyCm, steps = stepsPastSecondFinger)
  }

  /** Sends [action] for two fingers, the second at [secondX]. */
  private fun sendTwo(action: Int, secondX: Float) {
    val properties =
      Array(2) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
    val coords =
      arrayOf(
        MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f },
        MotionEvent.PointerCoords().apply { this.x = secondX; this.y = y; pressure = 1f; size = 1f },
      )
    val event =
      MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 2, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    manifold.onMotionEvent(null, event)
    event.recycle()
  }

  @Test
  fun swipeRight_runsBeforeTheLift() {
    send(MotionEvent.ACTION_DOWN)
    move(1.5f, 0f)
    // A second finger may still land for a two-finger swipe.
    assertTrue(completed.isEmpty())
    move(1f, 0f, steps = stepsPastSecondFinger)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
    move(2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun swipeUp_runsBeforeTheLift() {
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(0f, -1f)
    assertEquals(listOf(GESTURE_SWIPE_UP), completed)
  }

  @Test
  fun rightThenLeft_isTheRightSwipe() {
    assigned += GESTURE_SWIPE_RIGHT_AND_LEFT
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(1f, 0f)
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

  @Test
  fun nothingFollows_theSwipeRunsBeforeTheLift() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(1f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
    move(2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun backAndForthAssigned_theSwipeWaitsAndTheBackAndForthWorks() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    assigned += GESTURE_SWIPE_RIGHT_AND_LEFT
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    move(-2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_AND_LEFT), completed)
  }

  @Test
  fun backAndForthAssigned_theSwipeRunsAtTheLift() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    assigned += GESTURE_SWIPE_RIGHT_AND_LEFT
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun angleAssigned_onlyItsDirectionWaits() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    assigned += GESTURE_SWIPE_LEFT_AND_UP
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(0f, -1f)
    assertEquals(listOf(GESTURE_SWIPE_UP), completed)
    move(0f, -1f)
    send(MotionEvent.ACTION_UP)
    send(MotionEvent.ACTION_DOWN)
    move(-2f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_UP), completed)
    move(0f, -2f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_UP, GESTURE_SWIPE_LEFT_AND_UP), completed)
  }

  @Test
  fun twoDirectionHoldAssigned_theSwipeWaits() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    assigned += GestureManifold.GESTURE_SWIPE_RIGHT_AND_LEFT_AND_HOLD
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertTrue(completed.isEmpty())
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
  }

  @Test
  fun assigningLater_theNextTouchWaits() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(1f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
    assigned += GESTURE_SWIPE_RIGHT_AND_LEFT
    send(MotionEvent.ACTION_DOWN)
    move(2f, 0f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), completed)
    move(-2f, 0f)
    send(MotionEvent.ACTION_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT_AND_LEFT), completed)
    assigned.clear()
    send(MotionEvent.ACTION_DOWN)
    swipePastSecondFinger(1f, 0f)
    assertEquals(
      listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT_AND_LEFT, GESTURE_SWIPE_RIGHT),
      completed,
    )
  }

  @Test
  fun gesturesStartingWith_aSwipe() {
    assertArrayEquals(
      intArrayOf(
        GestureManifold.GESTURE_SWIPE_UP_AND_HOLD,
        AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
        GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD,
        AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT,
        GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_AND_HOLD,
        GESTURE_SWIPE_UP_AND_RIGHT,
        GestureManifold.GESTURE_SWIPE_UP_AND_RIGHT_AND_HOLD,
        GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_UP,
        GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_LEFT,
        GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_RIGHT,
        GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_AND_UP,
        GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_AND_DOWN,
        GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_AND_RIGHT,
        GestureManifold.GESTURE_SWIPE_UP_AND_RIGHT_AND_UP,
        GestureManifold.GESTURE_SWIPE_UP_AND_RIGHT_AND_DOWN,
        GestureManifold.GESTURE_SWIPE_UP_AND_RIGHT_AND_LEFT,
      ),
      GestureConfiguration.gesturesStartingWith(GESTURE_SWIPE_UP),
    )
    val swipes = listOf(GESTURE_SWIPE_UP, GESTURE_SWIPE_DOWN, GESTURE_SWIPE_LEFT, GESTURE_SWIPE_RIGHT)
    for (swipe in swipes) {
      val following = GestureConfiguration.gesturesStartingWith(swipe)
      assertEquals(16, following.size)
      assertEquals(GestureManifold.swipeAndHoldOf(swipe), following[0])
      for (i in 1 until 7) {
        assertTrue(GestureConfiguration.isTwoDirectionSwipe(following[i]))
      }
      for (i in 7 until following.size) {
        assertTrue(GestureConfiguration.isThreeDirectionSwipe(following[i]))
      }
    }
    assertTrue(GestureConfiguration.gesturesStartingWith(GESTURE_SWIPE_DOWN_AND_UP).isEmpty())
    assertTrue(
      GestureConfiguration.gesturesStartingWith(GestureManifold.GESTURE_SWIPE_UP_AND_HOLD).isEmpty()
    )
  }

  @Test
  fun runsBeforeLift_decision() {
    val none = IntPredicate { false }
    // Off and not automatic: every swipe waits for the lift.
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, false, false, none))
    // Automatic with nothing assigned.
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, false, true, none))
    // Only the direction a two-part gesture starts in waits.
    val upThenRight = IntPredicate { id -> id == GESTURE_SWIPE_UP_AND_RIGHT }
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, false, true, upThenRight))
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_RIGHT, false, true, upThenRight))
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_DOWN, false, true, upThenRight))
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_LEFT, false, true, upThenRight))
    val leftThenDown = IntPredicate { id -> id == GESTURE_SWIPE_LEFT_AND_DOWN }
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_LEFT, false, true, leftThenDown))
    val leftThenRight = IntPredicate { id -> id == GESTURE_SWIPE_LEFT_AND_RIGHT }
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_LEFT, false, true, leftThenRight))
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_RIGHT, false, true, leftThenRight))
    // The switch turns two-part gestures off, so only the swipe's own hold makes it wait.
    assertTrue(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, true, false, upThenRight))
    val upHold = IntPredicate { id -> id == GestureManifold.GESTURE_SWIPE_UP_AND_HOLD }
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, true, true, upHold))
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_UP, false, true, upHold))
    // Only one-finger swipes in one direction run before the lift.
    assertFalse(GestureConfiguration.runsBeforeLift(GESTURE_SWIPE_DOWN_AND_UP, true, true, none))
  }

  @Test
  fun secondFingerLandingSoon_isNoOneFingerSwipe() {
    GestureConfiguration.setSwipesBeforeLift(false)
    whenNothingFollows = true
    manifold.setMultiFingerGesturesEnabled(true)
    send(MotionEvent.ACTION_DOWN)
    // The first finger is past 1 cm before the second lands.
    move(1.5f, 0f)
    ShadowLooper.idleMainLooper(20, TimeUnit.MILLISECONDS)
    val pointerDown =
      MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    sendTwo(pointerDown, x + pixelsPerCm)
    repeat(8) {
      ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
      x += 0.25f * pixelsPerCm
      sendTwo(MotionEvent.ACTION_MOVE, x + pixelsPerCm)
    }
    val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    sendTwo(pointerUp, x + pixelsPerCm)
    send(MotionEvent.ACTION_UP)
    assertFalse(GESTURE_SWIPE_RIGHT in completed)
  }
}
