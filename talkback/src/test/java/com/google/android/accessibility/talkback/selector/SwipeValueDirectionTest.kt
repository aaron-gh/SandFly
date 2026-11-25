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

package com.google.android.accessibility.talkback.selector

import android.accessibilityservice.AccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeValueDirectionTest {

  @Test
  fun swipeUpIncreasesAndSwipeDownDecreases() {
    for (rtl in listOf(false, true)) {
      assertEquals(true, SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_UP, rtl))
      assertEquals(
        false,
        SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_DOWN, rtl),
      )
    }
  }

  @Test
  fun swipeRightIncreasesAndSwipeLeftDecreases() {
    assertEquals(
      true,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_RIGHT, /* rtl= */ false),
    )
    assertEquals(
      false,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_LEFT, /* rtl= */ false),
    )
  }

  @Test
  fun rightToLeft_swipeLeftIncreasesAndSwipeRightDecreases() {
    assertEquals(
      true,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_LEFT, /* rtl= */ true),
    )
    assertEquals(
      false,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_SWIPE_RIGHT, /* rtl= */ true),
    )
  }

  @Test
  fun multiFingerSwipesFollowTheirDirection() {
    assertEquals(
      true,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT, false),
    )
    assertEquals(
      false,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT, false),
    )
    assertEquals(
      true,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_4_FINGER_SWIPE_UP, false),
    )
    assertEquals(
      false,
      SwipeValueDirection.increases(AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN, false),
    )
  }

  @Test
  fun otherGesturesLeaveTheDirectionOpen() {
    for (gesture in
      listOf(
        AccessibilityService.GESTURE_DOUBLE_TAP,
        AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
      )) {
      assertNull(SwipeValueDirection.increases(gesture, /* rtl= */ false))
      assertNull(SwipeValueDirection.increases(gesture, /* rtl= */ true))
    }
  }

  @Test
  fun withoutSwipe_moveUpOrBackwardIncreases() {
    // Default gestures, keyboard and braille display: swipe up runs Move reading control up or
    // backward, which increases a slider.
    assertTrue(SwipeValueDirection.commandIncreases(/* isNext= */ false, null))
    assertFalse(SwipeValueDirection.commandIncreases(/* isNext= */ true, null))
  }

  @Test
  fun reassignedSwipes_directionFollowsTheSwipe() {
    // Move reading control down or forward on swipe right (report #623) increases a slider.
    assertTrue(SwipeValueDirection.commandIncreases(/* isNext= */ true, true))
    // Move reading control up or backward on swipe left decreases it.
    assertFalse(SwipeValueDirection.commandIncreases(/* isNext= */ false, false))
  }
}
