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
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.google.android.accessibility.utils.gestures.GestureManifold
import com.google.android.accessibility.utils.gestures.TwoStrokeSwipeIds
import java.util.function.Consumer
import java.util.function.IntPredicate

/**
 * Two-stroke swipes: a one-finger swipe, the finger lifted, and a second one-finger swipe that
 * starts within the time set in Time between two-stroke swipes, reported as one gesture such as
 * [GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP]. Either swipe is a one-direction,
 * back-and-forth or angle swipe.
 *
 * A swipe waits for a second swipe only while the time is set and a two-stroke swipe that starts
 * with it has a command; otherwise it runs at once, so a two-stroke swipe that starts with a
 * back-and-forth or angle swipe makes no one-direction swipe wait. A waiting swipe runs as it is
 * when the time passes before the next touch, or when the next touch is anything other than a swipe
 * that makes a two-stroke swipe with a command.
 *
 * @param isAssigned whether a gesture has a command
 * @param dispatch runs a gesture
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class TwoStrokeSwipes(
  private val handler: Handler,
  private val isAssigned: IntPredicate,
  private val dispatch: Consumer<AccessibilityGestureEvent>,
) {
  private var timeMs = 0
  private var waiting: AccessibilityGestureEvent? = null
  // Whether a touch has started since the waiting swipe's.
  private var nextTouchStarted = false
  private val timeout = Runnable { flush() }

  /** Sets the time the second swipe may take to start after the first lifts, or 0 for off. */
  fun setTimeMs(timeMs: Int) {
    this.timeMs = if (timeMs in TIMES_MS) timeMs else 0
    if (this.timeMs == 0) {
      flush()
    }
  }

  /** Returns whether a swipe is waiting for a second swipe. */
  fun isWaiting(): Boolean = waiting != null

  /**
   * Takes the completed gesture [event]. Runs a waiting swipe first unless [event] makes a
   * two-stroke swipe with it. Returns true when this took [event]: it made a two-stroke swipe, or it
   * waits for a second swipe. Otherwise the caller runs it.
   */
  fun offer(event: AccessibilityGestureEvent): Boolean {
    val first = waiting
    if (first != null) {
      clearWaiting()
      val twoStroke = TwoStrokeSwipeIds.twoStrokeSwipeOf(first.gestureId, event.gestureId)
      if (twoStroke != 0 && isAssigned.test(twoStroke)) {
        dispatch.accept(AccessibilityGestureEvent(twoStroke, event.displayId, event.motionEvents))
        return true
      }
      dispatch.accept(first)
    }
    if (!waitsAfter(event.gestureId)) {
      return false
    }
    waiting = event
    nextTouchStarted = false
    // Counted again from the lift; this covers a touch that never reports one.
    handler.postDelayed(timeout, timeMs.toLong())
    return true
  }

  /**
   * Returns whether the swipe [gestureId] makes a two-stroke swipe with a command, as the second
   * swipe after the waiting swipe or as a first swipe. Gesture matching treats such a swipe as one
   * with a command: a back-and-forth or angle swipe is matched, and a one-direction swipe that it
   * starts with waits for the lift.
   */
  fun makesAssignedTwoStrokeSwipe(gestureId: Int): Boolean {
    if (timeMs == 0) {
      return false
    }
    val first = waiting
    if (first != null) {
      val twoStroke = TwoStrokeSwipeIds.twoStrokeSwipeOf(first.gestureId, gestureId)
      if (twoStroke != 0 && isAssigned.test(twoStroke)) {
        return true
      }
    }
    return waitsAfter(gestureId)
  }

  /** Returns whether the gesture [gestureId] waits for a second swipe. */
  fun waitsAfter(gestureId: Int): Boolean =
    timeMs > 0 && TwoStrokeSwipeIds.twoStrokeSwipesStartingWith(gestureId).any { isAssigned.test(it) }

  /** The first finger of a touch went down. A second swipe that starts now is in time. */
  fun onTouchDown() {
    if (waiting == null) {
      return
    }
    handler.removeCallbacks(timeout)
    nextTouchStarted = true
  }

  /** The last finger of a touch lifted, after the touch was matched to a gesture or not. */
  fun onTouchUp() {
    if (waiting == null) {
      return
    }
    if (nextTouchStarted) {
      // The touch after the waiting swipe made no two-stroke swipe.
      flush()
      return
    }
    // The waiting swipe's own touch: the time counts from its lift.
    handler.removeCallbacks(timeout)
    handler.postDelayed(timeout, timeMs.toLong())
  }

  /** Runs the waiting swipe now, as a swipe. */
  fun flush() {
    val first = waiting ?: return
    clearWaiting()
    dispatch.accept(first)
  }

  /** Drops the waiting swipe, for when the service stops. */
  fun cancel() {
    clearWaiting()
  }

  private fun clearWaiting() {
    handler.removeCallbacks(timeout)
    waiting = null
    nextTouchStarted = false
  }

  companion object {
    /** The times the setting offers, in ms. Any other is off. */
    val TIMES_MS = setOf(250, 500, 1000, 1500, 2000)
  }
}
