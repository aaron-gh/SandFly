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
 * The middle of the two fingers is followed from the moment the second finger lands. Each stroke
 * is a movement of [strokePx] across the screen, and the next stroke goes back the other way.
 * The second stroke completes the scrub, which is reported at once, while the fingers are still
 * down. Movement up or down, a pinch, a third
 * finger, a stroke that doesn't turn back within [MAX_STROKE_MILLIS], or a scrub that takes longer
 * than [MAX_MILLIS] is not a scrub.
 */
class TwoFingerScrubTracker(
  private val strokePx: Float,
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
  // 1 while moving right, -1 while moving left, 0 before the first stroke.
  private var direction = 0
  // The furthest point of the current stroke.
  private var extreme = 0f
  private var strokes = 0
  private var strokeTime = 0L

  /** Whether a scrub was recognized and owns the current touch. */
  val isScrubbed: Boolean
    get() = state == State.SCRUBBED

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    firstId = INVALID_ID
    secondId = INVALID_ID
    direction = 0
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
          State.TRACKING -> state = State.REJECTED
          else -> {}
        }
      MotionEvent.ACTION_MOVE -> if (state == State.TRACKING) onTrackingMove(event)
      MotionEvent.ACTION_POINTER_UP -> if (state == State.TRACKING) state = State.REJECTED
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
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
    direction = 0
    strokes = 0
    state = State.TRACKING
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
    val x = (x0 + x1) / 2
    val y = (y0 + y1) / 2
    val time = event.eventTime
    if (time - startTime > MAX_MILLIS) {
      reject("too slow")
      return
    }
    if (abs(hypot(x1 - x0, y1 - y0) - startDistance) > strokePx) {
      reject("pinch")
      return
    }
    if (direction == 0) {
      val dx = x - startX
      if (abs(dx) >= strokePx) {
        direction = sign(dx).toInt()
        extreme = x
        strokes = 1
        strokeTime = time
      } else if (abs(y - startY) > strokePx) {
        reject("moving up or down")
      }
      return
    }
    if ((x - extreme) * direction > 0) {
      extreme = x
    } else if ((extreme - x) * direction >= strokePx) {
      direction = -direction
      extreme = x
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
    }
    if (time - strokeTime > MAX_STROKE_MILLIS) {
      reject("stroke didn't turn back")
    }
  }

  private fun reject(reason: String) {
    if (GestureLog.enabled) {
      Log.d(DEBUG_TAG, "Two-finger scrub rejected: $reason after $strokes strokes")
    }
    state = State.REJECTED
  }

  companion object {
    private const val INVALID_ID = -1

    /** Strokes in a scrub. */
    private const val STROKES = 2

    /** Longest time for the whole scrub, in milliseconds. */
    private const val MAX_MILLIS = 1200L

    /** Longest time for one stroke before it turns back, in milliseconds. */
    private const val MAX_STROKE_MILLIS = 450L

    private const val DEBUG_TAG = "BacktalkGesture"
  }
}
