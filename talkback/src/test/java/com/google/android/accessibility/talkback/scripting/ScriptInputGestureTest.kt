/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.scripting

import com.google.android.accessibility.utils.gestures.GestureManifold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptInputGestureTest {
  @Test
  fun edgePinchAndSwipeAndHoldGesturesHaveNames() {
    assertTrue(ScriptInput.isGesture("edgeSwipeDownFromTopLeft"))
    assertTrue(ScriptInput.isGesture("edgeLongSwipeUpFromBottom"))
    assertEquals("twoFingerPinch", ScriptInput.gestureName(GestureManifold.GESTURE_2_FINGER_PINCH))
    assertEquals("twoFingerSpread", ScriptInput.gestureName(GestureManifold.GESTURE_2_FINGER_SPREAD))
    assertEquals(
      "swipeRightThenDownAndHold",
      ScriptInput.gestureName(GestureManifold.GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD),
    )
  }

  @Test
  fun cornerGesturesHaveNames() {
    assertTrue(ScriptInput.isGesture("cornerTapTopLeft"))
    assertEquals(
      "cornerTripleTapAndHoldBottomRight",
      ScriptInput.gestureName(GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP_AND_HOLD),
    )
  }

  @Test
  fun oneFingerTripleAndQuadrupleTapsHaveNames() {
    assertTrue(ScriptInput.isGesture("tripleTap"))
    assertEquals("tripleTap", ScriptInput.gestureName(GestureManifold.GESTURE_TRIPLE_TAP))
    assertEquals("quadrupleTap", ScriptInput.gestureName(GestureManifold.GESTURE_QUADRUPLE_TAP))
  }

  @Test
  fun tapThenSwipeGesturesHaveNames() {
    assertTrue(ScriptInput.isGesture("twoFingerTapThenSwipeUp"))
    assertEquals(
      "fourFingerTripleTapThenSwipeRight",
      ScriptInput.gestureName(GestureManifold.GESTURE_4_FINGER_TRIPLE_TAP_SWIPE_RIGHT),
    )
  }

  @Test
  fun tapThenOneFingerSwipeGesturesHaveNames() {
    assertTrue(ScriptInput.isGesture("twoFingerTapThenOneFingerSwipeUp"))
    assertEquals(
      "twoFingerTripleTapThenOneFingerSwipeRight",
      ScriptInput.gestureName(GestureManifold.GESTURE_2_FINGER_TRIPLE_TAP_1_FINGER_SWIPE_RIGHT),
    )
  }

  @Test
  fun twoFingerAngleGesturesHaveNames() {
    assertTrue(ScriptInput.isGesture("twoFingerSwipeUpThenRight"))
    assertEquals(
      "twoFingerSwipeRightThenDown",
      ScriptInput.gestureName(GestureManifold.GESTURE_2_FINGER_SWIPE_RIGHT_AND_DOWN),
    )
  }

  @Test
  fun threePartSwipesHaveNames() {
    assertTrue(ScriptInput.isGesture("swipeUpThenDownThenUp"))
    assertEquals(
      "swipeRightThenUpThenRight",
      ScriptInput.gestureName(GestureManifold.GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT),
    )
  }

  @Test
  fun twoStrokeSwipesHaveNames() {
    assertTrue(ScriptInput.isGesture("swipeDownLiftSwipeDown"))
    assertEquals(
      "swipeRightLiftSwipeUp",
      ScriptInput.gestureName(GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP),
    )
    assertEquals(
      "swipeUpThenLeftLiftSwipeRightThenDown",
      ScriptInput.gestureName(GestureManifold.GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT_AND_DOWN),
    )
    assertTrue(ScriptInput.isGesture("swipeLeftLiftSwipeDownThenUp"))
  }

  @Test
  fun everyGestureNameIsUnique() {
    assertEquals(ScriptInput.gestureNames.size, ScriptInput.gestureNames.toSet().size)
  }
}
