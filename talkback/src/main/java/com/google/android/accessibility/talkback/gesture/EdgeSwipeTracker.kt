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
import kotlin.math.hypot

/**
 * Recognizes one finger sliding in from the top or bottom edge of the screen, as on iOS: up from
 * the bottom edge, or down from the left or right half of the top edge.
 *
 * The touch must start within [edgeZonePx] of the edge and slide inward. Once it has slid
 * [stageDistancesPx]`[0]` inward, mostly straight, the slide takes over the touch and reaches its
 * first stage. The bottom edge has a second stage at [stageDistancesPx]`[1]`. Each change of stage,
 * forward or back, is reported to the listener as it happens, and the stage the finger is at when
 * it lifts is the gesture. Sliding back to the edge before lifting cancels it.
 *
 * A slide that takes too long to reach the first stage, or goes sideways or outward, is left to
 * the other gestures and touch exploration, and so is a slide from an edge that [isEdgeInUse]
 * turns down, such as one whose gestures do nothing. So is a touch that has not moved at all by
 * [STILL_MILLIS]. Touch exploration waits while a slide may still start ([isPossible]), and
 * [rejectIfStalled] ends that wait when no further motion event comes.
 *
 * A finger that lands partly off the screen is reported at the screen's edge until enough of it is
 * on the screen, so a slide can stay at the edge, drifting sideways, for its first 100 ms or so.
 */
class EdgeSwipeTracker(
  private val edgeZonePx: Float,
  private val stageDistancesPx: FloatArray,
  // Movement, in pixels, after which a slide that is not going inward is rejected.
  private val slopPx: Float,
  private val isEdgeInUse: (Edge) -> Boolean,
  private val listener: Listener,
) {

  /** The edges a slide can start from. */
  enum class Edge(val stages: Int) {
    BOTTOM(2),
    TOP_LEFT(1),
    TOP_RIGHT(1),
  }

  /** Receives the stages and the end of a slide. */
  interface Listener {
    /** The slide took over the touch. No other gesture or touch exploration may use it. */
    fun onEdgeSwipeStarted(edge: Edge)

    /**
     * The slide moved from [previousStage] to [stage]. Stage 0 is back at the edge, where lifting
     * cancels the slide.
     */
    fun onEdgeSwipeStage(edge: Edge, stage: Int, previousStage: Int)

    /** The finger lifted at [stage], or at 0 when it was slid back to the edge. */
    fun onEdgeSwipeEnded(edge: Edge, stage: Int)
  }

  private enum class State {
    IDLE,
    // The touch started at an edge, and the slide may still start.
    POSSIBLE,
    // The slide owns the touch.
    ACTIVE,
    // The slide was cancelled after it took over; the rest of the touch is ignored.
    CANCELLED,
    // Not an edge slide.
    REJECTED,
  }

  private var state = State.IDLE
  private var edge = Edge.BOTTOM
  private var pointerId = INVALID_ID
  private var startX = 0f
  private var startY = 0f
  private var startTime = 0L
  private var inward = 0f
  // Whether the finger has moved since it landed.
  private var moved = false
  private var stage = 0

  /** Whether a slide owns the current touch. */
  val isActive: Boolean
    get() = state == State.ACTIVE || state == State.CANCELLED

  /** Whether the touch started at an edge and may still become a slide. */
  val isPossible: Boolean
    get() = state == State.POSSIBLE

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    pointerId = INVALID_ID
    inward = 0f
    moved = false
    stage = 0
  }

  /**
   * Processes a motion event. Returns true if the event belongs to an edge slide and must not be
   * handled as anything else.
   *
   * @param screenWidth the width of the screen, in the event's coordinates
   * @param screenHeight the height of the screen, in the event's coordinates
   */
  fun onMotionEvent(event: MotionEvent, screenWidth: Int, screenHeight: Int): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        clear()
        startTracking(event, screenWidth, screenHeight)
      }
      MotionEvent.ACTION_POINTER_DOWN ->
        when (state) {
          State.POSSIBLE -> state = State.REJECTED
          State.ACTIVE -> {
            // A second finger is not part of the slide: cancel it, and keep the rest of the
            // touch from becoming another gesture.
            debugLog("cancelled by a second finger")
            state = State.CANCELLED
            listener.onEdgeSwipeEnded(edge, 0)
          }
          else -> {}
        }
      MotionEvent.ACTION_MOVE ->
        when (state) {
          State.POSSIBLE -> onPossibleMove(event)
          State.ACTIVE -> onActiveMove(event)
          else -> {}
        }
      MotionEvent.ACTION_UP -> {
        val wasActive = state == State.ACTIVE
        val owned = isActive
        if (wasActive) {
          debugLog("ended at stage $stage")
          listener.onEdgeSwipeEnded(edge, stage)
        }
        clear()
        return owned
      }
      MotionEvent.ACTION_CANCEL -> {
        val owned = isActive
        if (state == State.ACTIVE) {
          listener.onEdgeSwipeEnded(edge, 0)
        }
        clear()
        return owned
      }
      else -> {}
    }
    return isActive
  }

  private fun startTracking(event: MotionEvent, screenWidth: Int, screenHeight: Int) {
    val x = event.x
    val y = event.y
    edge =
      when {
        y >= screenHeight - edgeZonePx -> Edge.BOTTOM
        y <= edgeZonePx -> if (x < screenWidth / 2f) Edge.TOP_LEFT else Edge.TOP_RIGHT
        else -> {
          state = State.REJECTED
          return
        }
      }
    if (!isEdgeInUse(edge)) {
      state = State.REJECTED
      return
    }
    pointerId = event.getPointerId(0)
    startX = x
    startY = y
    startTime = event.eventTime
    state = State.POSSIBLE
    debugLog("possible: start=(${x.toInt()},${y.toInt()}) screen=${screenWidth}x$screenHeight")
  }

  /**
   * The time, in [MotionEvent.getEventTime] terms, at which [rejectIfStalled] rejects the touch if
   * it is still possible and has not moved on.
   */
  fun stallTime(): Long = startTime + if (moved) MAX_START_MILLIS else STILL_MILLIS

  /**
   * Rejects a possible slide that has not moved by [STILL_MILLIS], or not reached the first stage
   * by [MAX_START_MILLIS], at [now].
   */
  fun rejectIfStalled(now: Long) {
    if (state != State.POSSIBLE) {
      return
    }
    if (now - startTime > MAX_START_MILLIS) {
      debugLog("rejected, too slow")
      state = State.REJECTED
    } else if (now - startTime >= STILL_MILLIS && !moved) {
      debugLog("rejected, not moving after ${now - startTime} ms")
      state = State.REJECTED
    }
  }

  /** How far the finger has moved from the edge, along the slide. Negative is outward. */
  private fun inward(y: Float): Float = if (edge == Edge.BOTTOM) startY - y else y - startY

  private fun onPossibleMove(event: MotionEvent) {
    val index = event.findPointerIndex(pointerId)
    if (event.pointerCount != 1 || index < 0) {
      state = State.REJECTED
      return
    }
    inward = inward(event.getY(index))
    val sideways = abs(event.getX(index) - startX)
    if (hypot(inward, sideways) >= slopPx / STILL_SLOP_DIVISOR) {
      moved = true
    }
    if (
      inward < -slopPx ||
        (sideways > slopPx * SIDEWAYS_SLOP_FACTOR && sideways > inward * MAX_SIDEWAYS_RATIO)
    ) {
      debugLog(
        "rejected, not sliding inward: start=(${startX.toInt()},${startY.toInt()})" +
          " inward=${inward.toInt()} sideways=${sideways.toInt()} slop=${slopPx.toInt()}"
      )
      state = State.REJECTED
      return
    }
    rejectIfStalled(event.eventTime)
    if (state != State.POSSIBLE) {
      return
    }
    if (inward >= stageDistancesPx[0]) {
      debugLog("started")
      state = State.ACTIVE
      listener.onEdgeSwipeStarted(edge)
      updateStage(inward)
    }
  }

  private fun onActiveMove(event: MotionEvent) {
    val index = event.findPointerIndex(pointerId)
    if (index >= 0) {
      updateStage(inward(event.getY(index)))
    }
  }

  private fun updateStage(inward: Float) {
    var reached = 0
    for (i in 0 until edge.stages) {
      if (inward >= stageDistancesPx[i]) reached = i + 1
    }
    if (reached == stage) {
      return
    }
    val previous = stage
    stage = reached
    listener.onEdgeSwipeStage(edge, stage, previous)
  }

  private fun debugLog(message: String) {
    if (BuildConfig.DEBUG) {
      Log.d(DEBUG_TAG, "Edge swipe ${edge.name}: $message")
    }
  }

  companion object {
    private const val INVALID_ID = -1

    /**
     * Time from the touch to the first stage, in milliseconds. A slower slide is left to touch
     * exploration.
     */
    private const val MAX_START_MILLIS = 500L

    /**
     * Time, in milliseconds, by which a touch at an edge must have moved. A touch that stays put
     * longer is touch exploration.
     */
    private const val STILL_MILLIS = 200L

    /** A finger has moved once it is a third of the slop from where it landed. */
    private const val STILL_SLOP_DIVISOR = 3f

    /**
     * How far a slide may move sideways, in slops, before its direction counts. While the finger is
     * still reported at the edge, it can only move sideways.
     */
    private const val SIDEWAYS_SLOP_FACTOR = 3f

    /** How far a slide may move sideways for each pixel inward before the first stage. */
    private const val MAX_SIDEWAYS_RATIO = 1f

    private const val DEBUG_TAG = "BacktalkGesture"
  }
}
