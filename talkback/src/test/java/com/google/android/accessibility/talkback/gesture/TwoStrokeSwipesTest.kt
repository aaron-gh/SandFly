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

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_DOUBLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.utils.gestures.GestureManifold
import com.google.android.accessibility.utils.gestures.TwoStrokeSwipeIds
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TwoStrokeSwipesTest {
  private val assigned = mutableSetOf<Int>()
  private val dispatched = mutableListOf<Int>()
  private val swipes =
    TwoStrokeSwipes(
      Handler(Looper.getMainLooper()),
      { it in assigned },
      { dispatched += it.gestureId },
    )

  private fun gesture(id: Int) = AccessibilityGestureEvent(id, 0, emptyList())

  private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

  /** A touch that is the swipe [id], reported before the finger lifts. Returns whether it waits. */
  private fun swipe(id: Int): Boolean {
    swipes.onTouchDown()
    val took = swipes.offer(gesture(id))
    if (!took) {
      dispatched += id
    }
    swipes.onTouchUp()
    return took
  }

  @Test
  fun off_nothingWaits() {
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    assertFalse(swipe(GESTURE_SWIPE_RIGHT))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun on_withNothingAssigned_swipesRunAtOnce() {
    swipes.setTimeMs(1000)
    assertFalse(swipe(GESTURE_SWIPE_RIGHT))
    assertFalse(swipe(GESTURE_SWIPE_UP))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_UP), dispatched)
    assertFalse(swipes.isWaiting())
  }

  @Test
  fun on_aSwipeWithNoAssignedSecondPart_runsAtOnce() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    // Left starts no two-stroke swipe with a command, so it doesn't wait.
    assertFalse(swipe(GESTURE_SWIPE_LEFT))
    assertEquals(listOf(GESTURE_SWIPE_LEFT), dispatched)
    assertFalse(swipes.isWaiting())
  }

  @Test
  fun assignedPair_isOneGesture() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    assertTrue(swipe(GESTURE_SWIPE_RIGHT))
    assertTrue(dispatched.isEmpty())
    idle(900)
    assertTrue(swipe(GESTURE_SWIPE_UP))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP), dispatched)
    idle(2000)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP), dispatched)
  }

  @Test
  fun sameDirectionTwice_isOneGesture() {
    swipes.setTimeMs(500)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT
    swipe(GESTURE_SWIPE_RIGHT)
    swipe(GESTURE_SWIPE_RIGHT)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun timeOut_runsTheFirstSwipe() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    idle(990)
    assertTrue(dispatched.isEmpty())
    idle(20)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
    assertFalse(swipes.isWaiting())
  }

  @Test
  fun quarterSecond_isOfferedAndTimesOut() {
    swipes.setTimeMs(250)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    assertTrue(swipe(GESTURE_SWIPE_RIGHT))
    idle(240)
    assertTrue(dispatched.isEmpty())
    idle(20)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
    assertFalse(swipes.isWaiting())
  }

  @Test
  fun aSecondTouchThatStartsInTime_mayEndLater() {
    swipes.setTimeMs(500)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    idle(400)
    swipes.onTouchDown()
    idle(300)
    assertTrue(swipes.offer(gesture(GESTURE_SWIPE_UP)))
    swipes.onTouchUp()
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP), dispatched)
  }

  @Test
  fun anUnassignedSecondSwipe_runsBothSwipes() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    assertFalse(swipe(GESTURE_SWIPE_DOWN))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_DOWN), dispatched)
  }

  @Test
  fun anUnassignedSecondSwipe_thatStartsAPair_waits() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    // Right then right has no command, but the second right starts right then up.
    assertTrue(swipe(GESTURE_SWIPE_RIGHT))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
    swipe(GESTURE_SWIPE_UP)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP), dispatched)
  }

  @Test
  fun anotherGesture_runsTheFirstSwipeFirst() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    assertFalse(swipe(GESTURE_DOUBLE_TAP))
    assertFalse(swipe(GESTURE_SWIPE_RIGHT_AND_UP))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT, GESTURE_DOUBLE_TAP, GESTURE_SWIPE_RIGHT_AND_UP), dispatched)
  }

  @Test
  fun aTouchThatIsNoGesture_runsTheFirstSwipe() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    swipes.onTouchDown()
    assertTrue(dispatched.isEmpty())
    swipes.onTouchUp()
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun flush_runsTheFirstSwipe_andCancelDropsIt() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    // Touch exploration, or a second finger.
    swipes.flush()
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
    swipe(GESTURE_SWIPE_RIGHT)
    swipes.cancel()
    idle(2000)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun turningOff_runsTheWaitingSwipe() {
    swipes.setTimeMs(2000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP
    swipe(GESTURE_SWIPE_RIGHT)
    swipes.setTimeMs(0)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), dispatched)
    // A time the setting doesn't offer is off.
    swipes.setTimeMs(3)
    assertFalse(swipe(GESTURE_SWIPE_RIGHT))
  }

  @Test
  fun twoStrokeSwipeIds() {
    assertEquals(
      GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP,
      TwoStrokeSwipeIds.twoStrokeSwipeOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_UP),
    )
    assertEquals(0, TwoStrokeSwipeIds.twoStrokeSwipeOf(GESTURE_SWIPE_RIGHT, GESTURE_2_FINGER_SINGLE_TAP))
    assertEquals(
      listOf(
        GestureManifold.GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP,
        GestureManifold.GESTURE_SWIPE_DOWN_LIFT_SWIPE_DOWN,
        GestureManifold.GESTURE_SWIPE_DOWN_LIFT_SWIPE_LEFT,
        GestureManifold.GESTURE_SWIPE_DOWN_LIFT_SWIPE_RIGHT,
        GestureManifold.GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP_AND_DOWN,
      ),
      TwoStrokeSwipeIds.twoStrokeSwipesStartingWith(GESTURE_SWIPE_DOWN).toList().take(5),
    )
    assertEquals(16, TwoStrokeSwipeIds.twoStrokeSwipesStartingWith(GESTURE_SWIPE_UP_AND_LEFT).size)
    assertTrue(TwoStrokeSwipeIds.twoStrokeSwipesStartingWith(GESTURE_DOUBLE_TAP).isEmpty())
    assertTrue(TwoStrokeSwipeIds.twoStrokeSwipesStartingWith(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP).isEmpty())
  }

  @Test
  fun everyTwoStrokeSwipe_hasItsOwnIdAndItsParts() {
    val all = TwoStrokeSwipeIds.twoStrokeSwipes()
    assertEquals(256, all.size)
    assertEquals(256, all.toSet().size)
    for (id in all) {
      val parts = TwoStrokeSwipeIds.twoStrokeSwipeParts(id)!!
      assertEquals(id, TwoStrokeSwipeIds.twoStrokeSwipeOf(parts[0], parts[1]))
      assertTrue(TwoStrokeSwipeIds.isTwoStrokeSwipe(id))
      assertEquals(
        id !in GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT..GestureManifold.GESTURE_SWIPE_UP_LIFT_SWIPE_UP,
        TwoStrokeSwipeIds.isTwoStrokeSwipeWithTurn(id),
      )
    }
    // The 16 one-direction ones keep their numbers.
    assertEquals(
      GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT,
      TwoStrokeSwipeIds.twoStrokeSwipeOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT),
    )
    assertEquals(
      GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT,
      TwoStrokeSwipeIds.twoStrokeSwipeOf(GESTURE_SWIPE_UP_AND_LEFT, GESTURE_SWIPE_RIGHT),
    )
    assertEquals(
      GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP,
      TwoStrokeSwipeIds.twoStrokeSwipeOf(GESTURE_SWIPE_RIGHT, GESTURE_SWIPE_RIGHT_AND_UP),
    )
    // Numbers in the block the one-direction ones would have had are no gestures.
    assertEquals(null, TwoStrokeSwipeIds.twoStrokeSwipeParts(-1000))
    assertFalse(TwoStrokeSwipeIds.isTwoStrokeSwipe(GESTURE_SWIPE_UP_AND_LEFT))
    assertFalse(TwoStrokeSwipeIds.isTwoStrokeSwipe(-1256))
  }

  @Test
  fun angleFirst_makesTheTwoStrokeSwipe() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT
    assertTrue(swipe(GESTURE_SWIPE_UP_AND_LEFT))
    assertTrue(swipe(GESTURE_SWIPE_RIGHT))
    assertEquals(listOf(GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun angleSecond_makesTheTwoStrokeSwipe() {
    swipes.setTimeMs(1000)
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP
    assertTrue(swipe(GESTURE_SWIPE_RIGHT))
    assertTrue(swipe(GESTURE_SWIPE_RIGHT_AND_UP))
    assertEquals(listOf(GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP), dispatched)
  }

  @Test
  fun angleFirstOnly_oneDirectionSwipesRunAtOnce() {
    swipes.setTimeMs(2000)
    assigned += GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT
    assertFalse(swipe(GESTURE_SWIPE_UP))
    assertFalse(swipe(GESTURE_SWIPE_RIGHT))
    assertEquals(listOf(GESTURE_SWIPE_UP, GESTURE_SWIPE_RIGHT), dispatched)
  }

  @Test
  fun makesAssignedTwoStrokeSwipe_asFirstOrSecondSwipe() {
    assigned += GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT
    assigned += GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP
    // Off.
    assertFalse(swipes.makesAssignedTwoStrokeSwipe(GESTURE_SWIPE_UP_AND_LEFT))
    swipes.setTimeMs(1000)
    assertTrue(swipes.makesAssignedTwoStrokeSwipe(GESTURE_SWIPE_UP_AND_LEFT))
    assertFalse(swipes.makesAssignedTwoStrokeSwipe(GESTURE_SWIPE_RIGHT_AND_UP))
    assertFalse(swipes.makesAssignedTwoStrokeSwipe(GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT))
    // After a waiting right swipe, the angle second swipe has one.
    swipe(GESTURE_SWIPE_RIGHT)
    swipes.onTouchDown()
    assertTrue(swipes.makesAssignedTwoStrokeSwipe(GESTURE_SWIPE_RIGHT_AND_UP))
  }
}
