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

/**
 * The direction a swipe that runs Move reading control down or forward, or up or backward, changes
 * a value in, such as a slider's or Speech rate's: swipes up and towards the end of a line
 * increase it, swipes down and towards the start of a line decrease it, whichever of the two
 * commands the swipe runs.
 */
object SwipeValueDirection {

  /**
   * Whether [gestureId] increases a value, or null for a gesture that isn't a straight swipe. With
   * [rtl], lines start on the right, so swipes left increase a value.
   */
  @JvmStatic
  fun increases(gestureId: Int, rtl: Boolean): Boolean? =
    when (gestureId) {
      AccessibilityService.GESTURE_SWIPE_UP,
      AccessibilityService.GESTURE_2_FINGER_SWIPE_UP,
      AccessibilityService.GESTURE_3_FINGER_SWIPE_UP,
      AccessibilityService.GESTURE_4_FINGER_SWIPE_UP -> true
      AccessibilityService.GESTURE_SWIPE_DOWN,
      AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN,
      AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN,
      AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN -> false
      AccessibilityService.GESTURE_SWIPE_RIGHT,
      AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT,
      AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT,
      AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT -> !rtl
      AccessibilityService.GESTURE_SWIPE_LEFT,
      AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT,
      AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT,
      AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT -> rtl
      else -> null
    }

  /**
   * Whether a command increases a value: as [swipeIncreases] says, or otherwise, for the commands'
   * other uses, when [isNext] is false, as Move reading control up or backward, on swipe up by
   * default, increases it.
   */
  @JvmStatic
  fun commandIncreases(isNext: Boolean, swipeIncreases: Boolean?): Boolean =
    swipeIncreases ?: !isNext
}
