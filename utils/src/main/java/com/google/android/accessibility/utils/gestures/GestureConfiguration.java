/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils.gestures;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.ViewConfiguration;
import androidx.annotation.RequiresApi;
import java.util.function.IntPredicate;

/** This class contains timeout values that control the behavior of gesture detection. */
public final class GestureConfiguration {

  /**
   * The maximum number of milliseconds that can be between two taps for them to count as a
   * multi-tap gesture.
   */
  public static final int DOUBLE_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout() - 50;

  // A gesture waits a bit longer than the tap gap before it completes, so a late tap still counts.
  private static final int MULTI_TAP_COMPLETION_MARGIN_MS =
      ViewConfiguration.getDoubleTapTimeout() - DOUBLE_TAP_TIMEOUT_MS;

  private static volatile int multiTapTimeoutMs = DOUBLE_TAP_TIMEOUT_MS;

  private GestureConfiguration() {}

  /** Returns the user-set maximum number of milliseconds between two taps of a multi-tap gesture. */
  public static int getMultiTapTimeoutMs() {
    return multiTapTimeoutMs;
  }

  /** Returns how long a tap gesture waits for another tap before it completes. */
  public static int getMultiTapCompletionTimeoutMs() {
    return multiTapTimeoutMs + MULTI_TAP_COMPLETION_MARGIN_MS;
  }

  /** How long two fingers must be held for TalkBack's recovery gesture, two-finger tap and hold. */
  public static final int TWO_FINGER_LONG_HOLD_MS = 5000;

  /**
   * Returns the maximum number of milliseconds between two taps of a multi-finger multi-tap gesture:
   * the user-set time, but never shorter than the default, as lifting and placing several fingers
   * takes longer than one.
   */
  public static int getMultiFingerTapTimeoutMs() {
    return Math.max(multiTapTimeoutMs, DOUBLE_TAP_TIMEOUT_MS);
  }

  /** Returns how long a multi-finger tap gesture waits for another tap before it completes. */
  public static int getMultiFingerTapCompletionTimeoutMs() {
    return getMultiFingerTapTimeoutMs() + MULTI_TAP_COMPLETION_MARGIN_MS;
  }

  public static void setMultiTapTimeoutMs(int timeoutMs) {
    multiTapTimeoutMs = timeoutMs;
  }

  /** The default time the finger rests at the end of a swipe for a swipe and hold. */
  public static final int DEFAULT_SWIPE_HOLD_TIME_MS = 250;

  private static volatile int swipeHoldTimeMs = DEFAULT_SWIPE_HOLD_TIME_MS;

  /** Returns how long the finger rests at the end of a swipe before lifting for a swipe and hold. */
  public static int getSwipeHoldTimeMs() {
    return swipeHoldTimeMs;
  }

  /** The shortest and longest hold times the setting offers. */
  private static final int MIN_SWIPE_HOLD_TIME_MS = 100;

  private static final int MAX_SWIPE_HOLD_TIME_MS = 500;

  /**
   * Sets the hold time. A value outside the setting's choices, such as 0 from a restored settings
   * file, would make every swipe a swipe and hold, so it falls back to the default.
   */
  public static void setSwipeHoldTimeMs(int timeMs) {
    swipeHoldTimeMs =
        timeMs >= MIN_SWIPE_HOLD_TIME_MS && timeMs <= MAX_SWIPE_HOLD_TIME_MS
            ? timeMs
            : DEFAULT_SWIPE_HOLD_TIME_MS;
  }

  /**
   * The default time a one-finger swipe has to move the gesture confirm distance before touch
   * exploration starts: config_max_time_to_start_swipe_ms_per_cm times
   * config_gesture_confirm_distance_cm on a phone.
   */
  public static final int DEFAULT_SWIPE_START_TIME_MS = 150;

  private static final int MAX_SWIPE_START_TIME_MS = 300;

  private static volatile int swipeStartTimeMs = DEFAULT_SWIPE_START_TIME_MS;

  /** Returns the user-set gesture start time. */
  public static int getSwipeStartTimeMs() {
    return swipeStartTimeMs;
  }

  /**
   * Sets the gesture start time. A value outside the setting's choices, such as 0 from a restored
   * settings file, falls back to the default.
   */
  public static void setSwipeStartTimeMs(int timeMs) {
    swipeStartTimeMs =
        timeMs >= DEFAULT_SWIPE_START_TIME_MS && timeMs <= MAX_SWIPE_START_TIME_MS
            ? timeMs
            : DEFAULT_SWIPE_START_TIME_MS;
  }

  /**
   * Returns how long after a finger goes down the next finger of a {@code fingerCount}-finger swipe
   * may land.
   */
  public static int multiFingerSwipeFingerTimeoutMs(int fingerCount) {
    return fingerCount * ViewConfiguration.getTapTimeout();
  }

  private static volatile boolean swipesBeforeLift = false;

  /**
   * Returns whether a one-finger swipe runs once it has moved the gesture confirm distance, before
   * the finger lifts, with back-and-forth and angle gestures off.
   */
  public static boolean swipesBeforeLift() {
    return swipesBeforeLift;
  }

  public static void setSwipesBeforeLift(boolean on) {
    swipesBeforeLift = on;
  }

  /**
   * Returns whether {@code gestureId} is a one-finger swipe in two directions, back-and-forth or
   * angle, or the swipe and hold version of one.
   */
  public static boolean isTwoDirectionSwipe(int gestureId) {
    return switch (gestureId) {
      case AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
          AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP,
          AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
          AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
          AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT,
          AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT,
          AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT,
          AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT,
          AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP,
          AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN,
          AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP,
          AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN ->
          true;
      default ->
          gestureId <= GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD
              && gestureId >= GestureManifold.GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD;
    };
  }

  /** Returns whether {@code gestureId} is a one-finger swipe in three directions. */
  public static boolean isThreeDirectionSwipe(int gestureId) {
    return gestureId <= GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_UP
        && gestureId >= GestureManifold.GESTURE_SWIPE_RIGHT_AND_LEFT_AND_RIGHT;
  }

  /**
   * Returns the one-finger swipes in three directions whose first direction is that of the
   * one-finger swipe {@code swipeGestureId}, or none for every other gesture.
   */
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  public static int[] threeDirectionSwipesStartingWith(int swipeGestureId) {
    // The three-direction swipes are numbered down from the first, nine for each first direction,
    // in the order up, down, left, right.
    int first =
        switch (swipeGestureId) {
          case AccessibilityService.GESTURE_SWIPE_UP -> 0;
          case AccessibilityService.GESTURE_SWIPE_DOWN -> 1;
          case AccessibilityService.GESTURE_SWIPE_LEFT -> 2;
          case AccessibilityService.GESTURE_SWIPE_RIGHT -> 3;
          default -> -1;
        };
    if (first < 0) {
      return new int[0];
    }
    int[] gestures = new int[THREE_DIRECTION_SWIPES_PER_FIRST_DIRECTION];
    for (int i = 0; i < gestures.length; i++) {
      gestures[i] =
          GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_UP
              - first * THREE_DIRECTION_SWIPES_PER_FIRST_DIRECTION
              - i;
    }
    return gestures;
  }

  // Three second directions, each with three third directions.
  private static final int THREE_DIRECTION_SWIPES_PER_FIRST_DIRECTION = 9;

  /** Returns whether {@code gestureId} is one of the two-finger angle gestures. */
  public static boolean isTwoFingerAngle(int gestureId) {
    return gestureId <= GestureManifold.GESTURE_2_FINGER_SWIPE_UP_AND_LEFT
        && gestureId >= GestureManifold.GESTURE_2_FINGER_SWIPE_RIGHT_AND_DOWN;
  }

  /**
   * Returns the gestures that start with the one-finger swipe {@code swipeGestureId}: its swipe and
   * hold, the back-and-forth and angle gestures that start in its direction, their swipe and hold
   * versions, and the swipes in three directions that start in its direction. Empty for every other
   * gesture. Swipe and hold gestures are SandFly's own
   * detection's, from Android 13.
   */
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  public static int[] gesturesStartingWith(int swipeGestureId) {
    int[] twoDirection =
        switch (swipeGestureId) {
          case AccessibilityService.GESTURE_SWIPE_UP ->
              new int[] {
                AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
                AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT,
                AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT
              };
          case AccessibilityService.GESTURE_SWIPE_DOWN ->
              new int[] {
                AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP,
                AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT,
                AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT
              };
          case AccessibilityService.GESTURE_SWIPE_LEFT ->
              new int[] {
                AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
                AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP,
                AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN
              };
          case AccessibilityService.GESTURE_SWIPE_RIGHT ->
              new int[] {
                AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
                AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP,
                AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN
              };
          default -> new int[0];
        };
    if (twoDirection.length == 0) {
      return twoDirection;
    }
    int[] threeDirection = threeDirectionSwipesStartingWith(swipeGestureId);
    int[] gestures = new int[twoDirection.length * 2 + 1 + threeDirection.length];
    gestures[0] = GestureManifold.swipeAndHoldOf(swipeGestureId);
    for (int i = 0; i < twoDirection.length; i++) {
      gestures[1 + 2 * i] = twoDirection[i];
      gestures[2 + 2 * i] = GestureManifold.swipeAndHoldOf(twoDirection[i]);
    }
    System.arraycopy(
        threeDirection, 0, gestures, twoDirection.length * 2 + 1, threeDirection.length);
    return gestures;
  }

  /**
   * Returns whether the one-finger swipe {@code swipeGestureId} runs once it has moved the gesture
   * confirm distance, before the finger lifts. A swipe whose swipe and hold has an action waits for
   * the lift. Otherwise the swipe runs before the lift when {@code twoDirectionSwipesOff}, which is
   * Recognize swipes before lifting, or, when {@code whenNothingFollows}, while no gesture that
   * starts with it has an action.
   *
   * @param isAssigned whether a gesture has an action
   */
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  public static boolean runsBeforeLift(
      int swipeGestureId,
      boolean twoDirectionSwipesOff,
      boolean whenNothingFollows,
      IntPredicate isAssigned) {
    int[] following = gesturesStartingWith(swipeGestureId);
    if (following.length == 0 || isAssigned.test(following[0])) {
      return false;
    }
    if (twoDirectionSwipesOff) {
      return true;
    }
    if (!whenNothingFollows) {
      return false;
    }
    for (int gestureId : following) {
      if (isAssigned.test(gestureId)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Scales a swipe start threshold computed from the resources to the user-set gesture start time.
   * With the default time the threshold is returned unchanged.
   */
  public static int scaleSwipeStartThresholdMs(int thresholdMs) {
    int timeMs = swipeStartTimeMs;
    if (timeMs == DEFAULT_SWIPE_START_TIME_MS) {
      return thresholdMs;
    }
    return (int) ((long) thresholdMs * timeMs / DEFAULT_SWIPE_START_TIME_MS);
  }
}
