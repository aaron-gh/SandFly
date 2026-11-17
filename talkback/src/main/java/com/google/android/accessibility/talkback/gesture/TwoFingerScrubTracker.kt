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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

/**
 * Recognizes a two-finger scrub: two fingers moving quickly one way across the screen and back.
 *
 * The middle of the two fingers is followed from the moment the second finger lands. The first
 * stroke is a movement of [strokePx] across the screen, and the stroke back the other way needs
 * [RETURN_FRACTION] of that. The second stroke completes the scrub, which is reported at once, while
 * the fingers are still down. A pinch, a third finger, a stroke that stops moving out for longer
 * than [MAX_STROKE_MILLIS] before it turns back, or a scrub that takes longer than [MAX_MILLIS] from
 * when the fingers start to move is not a scrub.
 *
 * Strokes go left and right, and movement up or down first is not a scrub, unless [anyDirection]
 * returns true when the fingers land. Then strokes may go any way, such as up and down or
 * diagonally. A first stroke that turns aside by more than 45 degrees, such as a slide in from the
 * top edge followed by a stroke sideways, starts the scrub again once from the turn, the way it
 * goes from there. A stroke that only curves or drifts keeps its direction.
 */
class TwoFingerScrubTracker(
  private val strokePx: Float,
  private val anyDirection: () -> Boolean = { false },
  private val listener: Listener,
) {

  /** Receives the scrub. */
  fun interface Listener {
    fun onScrub()
  }

  private enum class State {
    IDLE,
    TRACKING,
    SCRUBBED,
    REJECTED,
  }

  private var state = State.IDLE
  private var firstId = INVALID_ID
  private var secondId = INVALID_ID
  private var startTime = 0L
  private var startX = 0f
  private var startY = 0f
  private var startDistance = 0f
  // Whether strokes may go any way for the current touch, not only left and right.
  private var anyWay = false
  // Whether the first stroke already turned aside and started again, which it does once.
  private var turned = false
  // The unit vector of the current stroke's direction, set from the first stroke.
  private var directionX = 0f
  private var directionY = 0f
  // The furthest point of the current stroke along its direction.
  private var extremeX = 0f
  private var extremeY = 0f
  private var strokes = 0
  private var strokeTime = 0L

  /** Whether two fingers are being followed and may still make a scrub. */
  val isTracking: Boolean
    get() = state == State.TRACKING

  /** Whether a scrub was recognized and owns the current touch. */
  val isScrubbed: Boolean
    get() = state == State.SCRUBBED

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    firstId = INVALID_ID
    secondId = INVALID_ID
    strokes = 0
  }

  /**
   * Processes a motion event. Returns true if the event belongs to a scrub and must not be handled
   * as anything else.
   */
  fun onMotionEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> clear()
      MotionEvent.ACTION_POINTER_DOWN ->
        when (state) {
          State.IDLE -> if (event.pointerCount == 2) startTracking(event)
          State.TRACKING -> reject("third finger")
          else -> {}
        }
      MotionEvent.ACTION_MOVE -> if (state == State.TRACKING) onTrackingMove(event)
      MotionEvent.ACTION_POINTER_UP -> if (state == State.TRACKING) reject("finger lifted")
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (state == State.TRACKING) reject("touch ended")
        val scrubbed = state == State.SCRUBBED
        clear()
        return scrubbed
      }
      else -> {}
    }
    return state == State.SCRUBBED
  }

  private fun startTracking(event: MotionEvent) {
    firstId = event.getPointerId(0)
    secondId = event.getPointerId(1)
    startTime = event.eventTime
    startX = (event.getX(0) + event.getX(1)) / 2
    startY = (event.getY(0) + event.getY(1)) / 2
    startDistance = hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
    anyWay = anyDirection()
    turned = false
    strokes = 0
    state = State.TRACKING
  }

  /** Starts the first stroke at ([x], [y]), going the way of ([dx], [dy]). */
  private fun startFirstStroke(dx: Float, dy: Float, x: Float, y: Float, time: Long) {
    val length = hypot(dx, dy)
    directionX = dx / length
    directionY = dy / length
    extremeX = x
    extremeY = y
    strokes = 1
    strokeTime = time
  }

  private fun onTrackingMove(event: MotionEvent) {
    val firstIndex = event.findPointerIndex(firstId)
    val secondIndex = event.findPointerIndex(secondId)
    if (event.pointerCount != 2 || firstIndex < 0 || secondIndex < 0) {
      reject("${event.pointerCount} fingers")
      return
    }
    val x0 = event.getX(firstIndex)
    val y0 = event.getY(firstIndex)
    val x1 = event.getX(secondIndex)
    val y1 = event.getY(secondIndex)
    val x = (x0 + x1) / 2
    val y = (y0 + y1) / 2
    val time = event.eventTime
    // The time limit starts when the fingers start to move, not when they land.
    if (strokes == 0 && hypot(x - startX, y - startY) < strokePx * START_FRACTION) {
      startTime = time
    }
    if (time - startTime > MAX_MILLIS) {
      reject("too slow")
      return
    }
    if (abs(hypot(x1 - x0, y1 - y0) - startDistance) > strokePx) {
      reject("pinch")
      return
    }
    if (strokes == 0) {
      val dx = x - startX
      val dy = y - startY
      if (anyWay) {
        if (hypot(dx, dy) >= strokePx) {
          startFirstStroke(dx, dy, x, y, time)
        }
      } else if (abs(dx) >= strokePx) {
        startFirstStroke(sign(dx), 0f, x, y, time)
      } else if (abs(dy) > strokePx) {
        reject("moving up or down")
      }
      return
    }
    // How far the fingers are from the furthest point of the stroke, along the stroke and across it.
    val along = (x - extremeX) * directionX + (y - extremeY) * directionY
    val across = abs((x - extremeX) * directionY - (y - extremeY) * directionX)
    // When strokes may go any way, the stroke goes on while the fingers move within 45 degrees of
    // it, and the furthest point is where they are, so that drift and curves don't add up to a turn.
    // Otherwise the furthest point stays on the line of the stroke.
    val advancing = along > 0 && (!anyWay || along >= across)
    if (advancing) {
      if (anyWay) {
        extremeX = x
        extremeY = y
      } else {
        extremeX += along * directionX
        extremeY += along * directionY
      }
      strokeTime = time
    } else if (-along >= strokePx * RETURN_FRACTION) {
      directionX = -directionX
      directionY = -directionY
      extremeX = x
      extremeY = y
      strokes++
      strokeTime = time
      if (strokes >= STROKES) {
        if (GestureLog.enabled) {
          Log.d(DEBUG_TAG, "Two-finger scrub in ${time - startTime} ms")
        }
        state = State.SCRUBBED
        listener.onScrub()
        return
      }
    } else if (
      anyWay &&
        !turned &&
        strokes == 1 &&
        across >= strokePx &&
        -along < strokePx * TURN_FRACTION
    ) {
      // The fingers turned aside at the furthest point without coming back, so the scrub starts
      // again from there, the way they went from it.
      if (GestureLog.enabled) {
        Log.d(DEBUG_TAG, "Two-finger scrub: first stroke turned aside, starting again")
      }
      turned = true
      startFirstStroke(x - extremeX, y - extremeY, x, y, time)
    }
    if (time - strokeTime > MAX_STROKE_MILLIS) {
      reject("stroke didn't turn back")
    }
  }

  private fun reject(reason: String) {
    if (GestureLog.enabled) {
      Log.d(
        DEBUG_TAG,
        "Two-finger scrub rejected: $reason after $strokes strokes, stroke ${strokePx.toInt()} px",
      )
    }
    state = State.REJECTED
  }

  companion object {
    private const val INVALID_ID = -1

    /** Strokes in a scrub. */
    private const val STROKES = 2

    /** Longest time for the whole scrub, in milliseconds. */
    private const val MAX_MILLIS = 1200L

    /** Longest time a stroke may stop moving out before it turns back, in milliseconds. */
    private const val MAX_STROKE_MILLIS = 450L

    /** The part of a stroke the fingers must move back. */
    private const val RETURN_FRACTION = 0.6f

    /** The part of a stroke the fingers move before the time limit starts. */
    private const val START_FRACTION = 0.25f

    /**
     * The part of a stroke the fingers may come back while turning aside, when strokes may go any
     * way, for the turn to start the scrub again rather than be a stroke back.
     */
    private const val TURN_FRACTION = 0.3f

    private const val DEBUG_TAG = "BacktalkGesture"
  }
}
