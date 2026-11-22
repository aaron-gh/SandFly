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

package com.google.android.accessibility.utils.gestures;

import android.accessibilityservice.AccessibilityService;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The gesture IDs of two-stroke swipes, such as {@link
 * GestureManifold#GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP}: two one-finger swipes with the finger lifted
 * between them. Only SandFly's own gesture detection, from Android 13, reports them, but their IDs
 * and names are used on every version.
 */
public final class TwoStrokeSwipeIds {

  private TwoStrokeSwipeIds() {}

  /**
   * The strokes of two-stroke swipes: the one-direction swipes up, down, left and right, then the
   * back-and-forth swipes, then the angle swipes.
   */
  private static final int[] TWO_STROKE_PARTS = {
    AccessibilityService.GESTURE_SWIPE_UP,
    AccessibilityService.GESTURE_SWIPE_DOWN,
    AccessibilityService.GESTURE_SWIPE_LEFT,
    AccessibilityService.GESTURE_SWIPE_RIGHT,
    AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
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
    AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN
  };

  // The one-direction swipes come first in TWO_STROKE_PARTS.
  private static final int ONE_DIRECTION_PARTS = 4;

  // The two-stroke swipes with a back-and-forth or angle stroke are numbered down from this.
  private static final int TWO_STROKE_SWIPE_WITH_TURN_BASE = -1000;

  /**
   * Returns the two-stroke swipe of the one-finger swipe {@code firstGestureId} and then the
   * one-finger swipe {@code secondGestureId}, or 0 when either is no one-direction, back-and-forth
   * or angle swipe.
   */
  public static int twoStrokeSwipeOf(int firstGestureId, int secondGestureId) {
    int first = twoStrokePartIndex(firstGestureId);
    int second = twoStrokePartIndex(secondGestureId);
    if (first < 0 || second < 0) {
      return 0;
    }
    if (first < ONE_DIRECTION_PARTS && second < ONE_DIRECTION_PARTS) {
      // Numbered down from the first, four for each first direction, in the order up, down, left,
      // right.
      return GestureManifold.GESTURE_SWIPE_UP_LIFT_SWIPE_UP - first * ONE_DIRECTION_PARTS - second;
    }
    return TWO_STROKE_SWIPE_WITH_TURN_BASE - first * TWO_STROKE_PARTS.length - second;
  }

  /**
   * Returns the two-stroke swipes that start with the one-finger swipe {@code swipeGestureId}, one
   * for each stroke that can follow it, or none for every gesture that is no stroke of one.
   */
  public static int[] twoStrokeSwipesStartingWith(int swipeGestureId) {
    if (twoStrokePartIndex(swipeGestureId) < 0) {
      return new int[0];
    }
    int[] gestures = new int[TWO_STROKE_PARTS.length];
    for (int i = 0; i < gestures.length; i++) {
      gestures[i] = twoStrokeSwipeOf(swipeGestureId, TWO_STROKE_PARTS[i]);
    }
    return gestures;
  }

  /** Returns every two-stroke swipe. */
  public static int[] twoStrokeSwipes() {
    int[] gestures = new int[TWO_STROKE_PARTS.length * TWO_STROKE_PARTS.length];
    for (int first = 0; first < TWO_STROKE_PARTS.length; first++) {
      for (int second = 0; second < TWO_STROKE_PARTS.length; second++) {
        gestures[first * TWO_STROKE_PARTS.length + second] =
            twoStrokeSwipeOf(TWO_STROKE_PARTS[first], TWO_STROKE_PARTS[second]);
      }
    }
    return gestures;
  }

  /**
   * Returns the first and second swipe of the two-stroke swipe {@code gestureId}, or null for every
   * other gesture.
   */
  public static int @Nullable [] twoStrokeSwipeParts(int gestureId) {
    int first;
    int second;
    if (gestureId <= GestureManifold.GESTURE_SWIPE_UP_LIFT_SWIPE_UP
        && gestureId >= GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT) {
      int index = GestureManifold.GESTURE_SWIPE_UP_LIFT_SWIPE_UP - gestureId;
      first = index / ONE_DIRECTION_PARTS;
      second = index % ONE_DIRECTION_PARTS;
    } else {
      int index = TWO_STROKE_SWIPE_WITH_TURN_BASE - gestureId;
      if (index < 0 || index >= TWO_STROKE_PARTS.length * TWO_STROKE_PARTS.length) {
        return null;
      }
      first = index / TWO_STROKE_PARTS.length;
      second = index % TWO_STROKE_PARTS.length;
      if (first < ONE_DIRECTION_PARTS && second < ONE_DIRECTION_PARTS) {
        // These have the numbers above.
        return null;
      }
    }
    return new int[] {TWO_STROKE_PARTS[first], TWO_STROKE_PARTS[second]};
  }

  /** Returns whether {@code gestureId} is a two-stroke swipe. */
  public static boolean isTwoStrokeSwipe(int gestureId) {
    return twoStrokeSwipeParts(gestureId) != null;
  }

  /**
   * Returns whether {@code gestureId} is a two-stroke swipe with a back-and-forth or angle swipe as
   * either stroke.
   */
  public static boolean isTwoStrokeSwipeWithTurn(int gestureId) {
    int[] parts = twoStrokeSwipeParts(gestureId);
    return parts != null
        && (twoStrokePartIndex(parts[0]) >= ONE_DIRECTION_PARTS
            || twoStrokePartIndex(parts[1]) >= ONE_DIRECTION_PARTS);
  }

  /** The place of a swipe in TWO_STROKE_PARTS, or -1 for other gestures. */
  private static int twoStrokePartIndex(int gestureId) {
    for (int i = 0; i < TWO_STROKE_PARTS.length; i++) {
      if (TWO_STROKE_PARTS[i] == gestureId) {
        return i;
      }
    }
    return -1;
  }
}
