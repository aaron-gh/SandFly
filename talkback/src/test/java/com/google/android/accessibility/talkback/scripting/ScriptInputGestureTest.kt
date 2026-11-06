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
  fun everyGestureNameIsUnique() {
    assertEquals(ScriptInput.gestureNames.size, ScriptInput.gestureNames.toSet().size)
  }
}
