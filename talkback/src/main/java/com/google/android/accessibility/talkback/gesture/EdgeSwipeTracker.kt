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

/**
 * Recognizes one finger sliding in from the top or bottom edge of the screen, as on iOS: up from
 * the bottom edge, or down from the left or right half of the top edge.
 *
 * The touch must start within [edgeZonePx] of the edge and slide inward. Once it has slid
 * [stageDistancesPx]`[0]` inward, mostly straight, the slide takes over the touch and reaches its
 * first stage. The bottom edge has a second stage at [stageDistancesPx]`[1]`. Each stage reached
 * is reported to the listener as it happens, and the stage the finger is at when it lifts is the
 * gesture. Sliding back to the edge before lifting cancels it.
 *
 * A slide that takes too long to reach the first stage, or goes sideways or outward, is left to
 * the other gestures and touch exploration, and so is a slide from an edge that [isEdgeInUse]
 * turns down, such as one whose gestures do nothing.
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

    /** The slide reached a further stage, starting at 1. */
    fun onEdgeSwipeStage(edge: Edge, stage: Int)

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
  private var stage = 0
  private var highestStage = 0

  /** Whether a slide owns the current touch. */
  val isActive: Boolean
    get() = state == State.ACTIVE || state == State.CANCELLED

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    pointerId = INVALID_ID
    stage = 0
    highestStage = 0
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
  }

  /** How far the finger has moved from the edge, along the slide. Negative is outward. */
  private fun inward(y: Float): Float = if (edge == Edge.BOTTOM) startY - y else y - startY

  private fun onPossibleMove(event: MotionEvent) {
    val index = event.findPointerIndex(pointerId)
    if (event.pointerCount != 1 || index < 0) {
      state = State.REJECTED
      return
    }
    val inward = inward(event.getY(index))
    val sideways = abs(event.getX(index) - startX)
    if (event.eventTime - startTime > MAX_START_MILLIS) {
      debugLog("rejected, too slow")
      state = State.REJECTED
    } else if (inward < -slopPx || (sideways > slopPx && sideways > inward * MAX_SIDEWAYS_RATIO)) {
      debugLog("rejected, not sliding inward")
      state = State.REJECTED
    } else if (inward >= stageDistancesPx[0]) {
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
    stage = reached
    // Each stage is felt once on the way in; coming back to a stage already felt is quiet.
    if (stage > highestStage) {
      highestStage = stage
      listener.onEdgeSwipeStage(edge, stage)
    }
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
     * exploration, which starts about this soon.
     */
    private const val MAX_START_MILLIS = 300L

    /** How far a slide may move sideways for each pixel inward before the first stage. */
    private const val MAX_SIDEWAYS_RATIO = 1f

    private const val DEBUG_TAG = "BacktalkGesture"
  }
}
