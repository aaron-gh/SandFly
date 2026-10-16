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

package com.google.android.accessibility.talkback.gesture

import android.util.Log
import android.view.MotionEvent
import com.google.android.accessibility.talkback.BuildConfig
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Recognizes two fingers turning around each other, like turning a dial, and reports one step for
 * each [setStepDegrees] of rotation.
 *
 * The angle of the line between the two fingers is followed from the moment the second finger
 * lands. The rotation is accepted once that angle has changed by [COMMIT_DEGREES] while the
 * movement along the circle is larger than the movement of the midpoint (a two-finger scroll) and
 * the change of the distance between the fingers (a pinch). Until then the two-finger movement is
 * left to the caller. While [isPossibleRotation] is true, the caller should hold back scrolling or
 * passing the touch to the app. Once accepted, every event up to the last finger lifting belongs
 * to the rotation, and [onMotionEvent] returns true for them.
 *
 * Clockwise is clockwise as seen on the screen.
 */
class TwoFingerRotationTracker(
  // Movement of one finger, in pixels, after which the movement must look like a rotation.
  private val decisionDistancePx: Float,
  // The smallest movement along the circle, in pixels, that can start a rotation.
  private val minArcPx: Float,
  private val listener: Listener,
) {

  /** Receives the rotation steps. */
  fun interface Listener {
    /** Called for each step of rotation. The first step is reported when the rotation starts. */
    fun onRotationStep(clockwise: Boolean)
  }

  private enum class State {
    // Waiting for exactly two fingers.
    IDLE,
    // Two fingers are down, and the movement may still become a rotation.
    TRACKING,
    // The fingers are rotating; the rest of the touch belongs to the rotation.
    ROTATING,
    // Not a rotation; ignore the rest of the touch.
    REJECTED,
  }

  private var state = State.IDLE
  private var firstId = INVALID_ID
  private var secondId = INVALID_ID
  private var startX0 = 0f
  private var startY0 = 0f
  private var startX1 = 0f
  private var startY1 = 0f
  // How far each finger has moved since tracking started, for the debug log.
  private var moved0X = 0f
  private var moved0Y = 0f
  private var moved1X = 0f
  private var moved1Y = 0f
  private var startMidX = 0f
  private var startMidY = 0f
  private var startDistance = 0f
  private var lastAngle = 0f
  // Signed rotation since the second finger landed, in degrees. Positive is clockwise.
  private var accumulated = 0f
  // Where the last step was reported.
  private var anchor = 0f
  // The furthest point reached in [direction] since the last step.
  private var peak = 0f
  // 1 if the last step was clockwise, -1 if counterclockwise.
  private var direction = 0
  // When the rotation was accepted, in event time.
  private var acceptedTime = 0L
  // False when one of the two rotating fingers was lifted. The rotation then waits for the rest.
  private var rotatingFingersDown = false

  /** Whether a rotation has started and owns the current touch. */
  val isRotating: Boolean
    get() = state == State.ROTATING

  /** Whether two fingers are moving in a way that may still turn into a rotation. */
  val isPossibleRotation: Boolean
    get() = state == State.TRACKING

  /** Whether both fingers moved the same way, so the touch is a two-finger scroll. */
  var isScroll = false
    private set

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    isScroll = false
    firstId = INVALID_ID
    secondId = INVALID_ID
    rotatingFingersDown = false
  }

  /**
   * Processes a motion event. Returns true if the event belongs to a rotation and must not be
   * handled as anything else.
   */
  fun onMotionEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> clear()
      MotionEvent.ACTION_POINTER_DOWN ->
        when (state) {
          State.IDLE -> if (event.pointerCount == 2) startTracking(event)
          State.TRACKING -> state = State.REJECTED
          else -> {}
        }
      MotionEvent.ACTION_MOVE ->
        when (state) {
          State.TRACKING -> onTrackingMove(event)
          State.ROTATING -> if (rotatingFingersDown) onRotatingMove(event)
          else -> {}
        }
      MotionEvent.ACTION_POINTER_UP ->
        when (state) {
          State.TRACKING -> state = State.REJECTED
          State.ROTATING -> {
            val id = event.getPointerId(event.actionIndex)
            if (id == firstId || id == secondId) rotatingFingersDown = false
            return true
          }
          else -> {}
        }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        val wasRotating = state == State.ROTATING
        clear()
        return wasRotating
      }
      else -> {}
    }
    return state == State.ROTATING
  }

  private fun startTracking(event: MotionEvent) {
    firstId = event.getPointerId(0)
    secondId = event.getPointerId(1)
    val x0 = event.getX(0)
    val y0 = event.getY(0)
    val x1 = event.getX(1)
    val y1 = event.getY(1)
    startX0 = x0
    startY0 = y0
    startX1 = x1
    startY1 = y1
    startMidX = (x0 + x1) / 2
    startMidY = (y0 + y1) / 2
    startDistance = hypot(x1 - x0, y1 - y0)
    lastAngle = angleDegrees(x0, y0, x1, y1)
    accumulated = 0f
    state = State.TRACKING
    if (BuildConfig.DEBUG) {
      Log.d(DEBUG_TAG, "Rotation tracking started: distance=%.0f".format(startDistance))
    }
  }

  private fun onTrackingMove(event: MotionEvent) {
    val firstIndex = event.findPointerIndex(firstId)
    val secondIndex = event.findPointerIndex(secondId)
    if (event.pointerCount != 2 || firstIndex < 0 || secondIndex < 0) {
      state = State.REJECTED
      return
    }
    val x0 = event.getX(firstIndex)
    val y0 = event.getY(firstIndex)
    val x1 = event.getX(secondIndex)
    val y1 = event.getY(secondIndex)
    accumulate(angleDegrees(x0, y0, x1, y1))

    val distance = hypot(x1 - x0, y1 - y0)
    // How far each finger moved along the circle, around the midpoint, apart, and in all.
    val arc = (distance / 2) * Math.toRadians(abs(accumulated).toDouble()).toFloat()
    val translation = hypot((x0 + x1) / 2 - startMidX, (y0 + y1) / 2 - startMidY)
    val radial = abs(distance - startDistance) / 2
    val looksLikeRotation =
      translation <= arc * MAX_TRANSLATION_RATIO && radial <= arc * MAX_RADIAL_RATIO

    moved0X = x0 - startX0
    moved0Y = y0 - startY0
    moved1X = x1 - startX1
    moved1Y = y1 - startY1
    val moved0 = hypot(moved0X, moved0Y)
    val moved1 = hypot(moved1X, moved1Y)
    if (
      moved0 >= minArcPx &&
        moved1 >= minArcPx &&
        (moved0X * moved1X + moved0Y * moved1Y) / (moved0 * moved1) > SAME_DIRECTION_COSINE
    ) {
      // In a rotation the fingers move in opposite directions. Both moving the same way is a
      // scroll, even when one finger started first and the line between them turned.
      debugLog("rejected as scroll, fingers moving the same way", arc, translation, radial)
      state = State.REJECTED
      isScroll = true
      return
    }
    // With one finger nearly still, a turn looks the same as the start of a scroll whose other
    // finger has not moved yet, so it must turn further before it counts.
    val pivot = min(moved0, moved1) < max(moved0, moved1) * PIVOT_STILL_RATIO
    val commitDegrees = if (pivot) PIVOT_COMMIT_DEGREES else COMMIT_DEGREES

    if (looksLikeRotation && abs(accumulated) >= commitDegrees && arc >= minArcPx) {
      debugLog("accepted", arc, translation, radial)
      state = State.ROTATING
      rotatingFingersDown = true
      direction = if (accumulated > 0) 1 else -1
      // The first step covers a whole step of rotation, even when the turn was accepted sooner, so
      // the second step always comes at the same angle. Otherwise a quick turn with both fingers,
      // accepted at a smaller angle than a pivot, gives two steps where a pivot gives one.
      anchor = direction * max(stepDegrees, abs(accumulated))
      acceptedTime = event.eventTime
      peak = accumulated
      listener.onRotationStep(direction > 0)
    } else if (translation > decisionDistancePx && abs(accumulated) < SCROLL_MAX_DEGREES) {
      // Both fingers moved the same way without turning: a scroll.
      debugLog("rejected as scroll", arc, translation, radial)
      state = State.REJECTED
      isScroll = true
    } else if (radial > decisionDistancePx && radial > arc) {
      // The fingers moved apart or together more than they turned: a pinch.
      debugLog("rejected as pinch", arc, translation, radial)
      state = State.REJECTED
    } else if (
      !looksLikeRotation &&
        max(arc, max(translation, radial)) > decisionDistancePx * GIVE_UP_DISTANCE_FACTOR
    ) {
      debugLog("rejected", arc, translation, radial)
      state = State.REJECTED
    }
  }

  private fun debugLog(result: String, arc: Float, translation: Float, radial: Float) {
    if (BuildConfig.DEBUG) {
      Log.d(
        DEBUG_TAG,
        ("Rotation $result: angle=%.1f arc=%.0f translation=%.0f radial=%.0f slop=%.0f " +
            "finger1=(%.0f,%.0f) finger2=(%.0f,%.0f)")
          .format(
            accumulated,
            arc,
            translation,
            radial,
            decisionDistancePx,
            moved0X,
            moved0Y,
            moved1X,
            moved1Y,
          ),
      )
    }
  }

  private fun onRotatingMove(event: MotionEvent) {
    val firstIndex = event.findPointerIndex(firstId)
    val secondIndex = event.findPointerIndex(secondId)
    if (firstIndex < 0 || secondIndex < 0) {
      return
    }
    accumulate(
      angleDegrees(
        event.getX(firstIndex),
        event.getY(firstIndex),
        event.getX(secondIndex),
        event.getY(secondIndex),
      )
    )
    if ((accumulated - peak) * direction > 0) {
      peak = accumulated
    }
    if (event.eventTime - acceptedTime < FLICK_MILLIS) {
      // A quick flick turns far in a moment. It is one step, so the next step is counted from
      // where the fingers are when the flick is over.
      if ((accumulated - anchor) * direction > 0) anchor = accumulated
      return
    }
    while (true) {
      val step = stepDegrees
      if ((accumulated - anchor) * direction >= step) {
        // Further in the same direction: steps are counted from the last step.
        anchor += direction * step
        peak = anchor
        if ((accumulated - peak) * direction > 0) peak = accumulated
        listener.onRotationStep(direction > 0)
      } else if ((peak - accumulated) * direction >= step) {
        // Turned back: the step is counted from where the fingers turned.
        direction = -direction
        anchor = peak + direction * step
        peak = accumulated
        listener.onRotationStep(direction > 0)
      } else {
        break
      }
    }
  }

  /** Adds the change from the last angle, taking the shorter way around the circle. */
  private fun accumulate(angle: Float) {
    var delta = angle - lastAngle
    while (delta > 180f) delta -= 360f
    while (delta <= -180f) delta += 360f
    accumulated += delta
    lastAngle = angle
  }

  companion object {
    private const val INVALID_ID = -1

    /** Rotation for each step after the first, in degrees, unless the setting chooses another. */
    private const val DEFAULT_STEP_DEGREES = 30f

    /** Set from the "Rotor turn per step" setting. */
    @Volatile private var stepDegrees = DEFAULT_STEP_DEGREES

    /** Sets the rotation for each step after the first, in degrees. */
    @JvmStatic
    fun setStepDegrees(degrees: Float) {
      stepDegrees = degrees
    }

    /** Rotation that starts the rotation and reports the first step, in degrees. */
    private const val COMMIT_DEGREES = 15f

    /** Time after the first step, in milliseconds, during which the rotation reports no steps. */
    private const val FLICK_MILLIS = 200L

    /** Rotation that starts a rotation where one finger stays nearly still, in degrees. */
    private const val PIVOT_COMMIT_DEGREES = 30f

    /** A finger that moved less than this share of the other's movement counts as still. */
    private const val PIVOT_STILL_RATIO = 0.35f

    /**
     * Fingers whose movements point within about 60 degrees of each other move the same way, which
     * is a scroll, not a rotation.
     */
    private const val SAME_DIRECTION_COSINE = 0.5f

    /** A two-finger movement that turns less than this, in degrees, is a scroll. */
    private const val SCROLL_MAX_DEGREES = 8f

    /**
     * How far past the decision distance to keep waiting for a movement that is neither clearly a
     * scroll nor a pinch, since the start of a turn often drifts a little.
     */
    private const val GIVE_UP_DISTANCE_FACTOR = 3f

    /**
     * Largest movement of the midpoint, relative to the movement along the circle. Turning one
     * finger around the other, which stays still, moves the midpoint almost as far as the circle.
     */
    private const val MAX_TRANSLATION_RATIO = 2f

    /** Largest change of the distance from the midpoint, relative to the movement along the circle. */
    private const val MAX_RADIAL_RATIO = 0.75f

    private const val DEBUG_TAG = "BacktalkGesture"

    /** The angle of the line from the first finger to the second. Y grows down the screen. */
    private fun angleDegrees(x0: Float, y0: Float, x1: Float, y1: Float): Float =
      Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())).toFloat()
  }
}
