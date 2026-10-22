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
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Recognizes a pinch or a spread with two, three or four fingers: the fingers moving toward their
 * middle, or away from it.
 *
 * Tracking starts again each time a finger lands, with the fingers then down. The gesture is
 * recognized as soon as the fingers' average distance from their middle has changed by
 * [minChangePx] and by [MIN_RATIO] of where it started, while the middle has moved and the fingers
 * have turned around it less than that. It is reported at once, while the fingers are still down,
 * and the rest of the touch then belongs to it. A finger lifting first, or more than four fingers,
 * is not a pinch. Only finger counts that [isCountInUse] accepts are tracked.
 *
 * While [isPossiblePinch] is true, the caller should hold back passing a two-finger movement to the
 * app.
 */
class PinchTracker(
  private val minChangePx: Float,
  private val isCountInUse: (Int) -> Boolean,
  private val listener: Listener,
) {

  /** Receives the pinches and spreads. */
  fun interface Listener {
    fun onPinch(fingerCount: Int, spread: Boolean)
  }

  private enum class State {
    IDLE,
    TRACKING,
    RECOGNIZED,
    REJECTED,
  }

  private var state = State.IDLE
  private var fingerCount = 0
  private val ids = IntArray(MAX_FINGERS)
  private val startAngles = FloatArray(MAX_FINGERS)
  private val lastAngles = FloatArray(MAX_FINGERS)
  private val turned = FloatArray(MAX_FINGERS)
  private var startX = 0f
  private var startY = 0f
  private var startRadius = 0f
  private var radialChange = 0f
  private var translation = 0f

  /** Whether a pinch or spread was recognized and owns the current touch. */
  val isRecognized: Boolean
    get() = state == State.RECOGNIZED

  /** Whether the fingers are moving toward or away from each other more than together. */
  val isPossiblePinch: Boolean
    get() = state == State.TRACKING && abs(radialChange) >= translation

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    fingerCount = 0
    radialChange = 0f
    translation = 0f
  }

  /**
   * Processes a motion event. Returns true if the event belongs to a pinch or spread and must not
   * be handled as anything else.
   */
  fun onMotionEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> clear()
      MotionEvent.ACTION_POINTER_DOWN ->
        if (state == State.IDLE || state == State.TRACKING) {
          val count = event.pointerCount
          when {
            count > MAX_FINGERS -> state = State.REJECTED
            isCountInUse(count) -> startTracking(event)
            // Wait for more fingers, which land one at a time.
            else -> state = State.IDLE
          }
        }
      MotionEvent.ACTION_MOVE -> if (state == State.TRACKING) onTrackingMove(event)
      MotionEvent.ACTION_POINTER_UP -> if (state == State.TRACKING) state = State.REJECTED
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        val recognized = state == State.RECOGNIZED
        clear()
        return recognized
      }
      else -> {}
    }
    return state == State.RECOGNIZED
  }

  private fun startTracking(event: MotionEvent) {
    fingerCount = event.pointerCount
    var sumX = 0f
    var sumY = 0f
    for (i in 0 until fingerCount) {
      sumX += event.getX(i)
      sumY += event.getY(i)
    }
    startX = sumX / fingerCount
    startY = sumY / fingerCount
    var sumRadius = 0f
    for (i in 0 until fingerCount) {
      ids[i] = event.getPointerId(i)
      val angle = angle(event.getX(i), event.getY(i), startX, startY)
      startAngles[i] = angle
      lastAngles[i] = angle
      turned[i] = 0f
      sumRadius += hypot(event.getX(i) - startX, event.getY(i) - startY)
    }
    startRadius = sumRadius / fingerCount
    radialChange = 0f
    translation = 0f
    state = State.TRACKING
  }

  private fun onTrackingMove(event: MotionEvent) {
    if (event.pointerCount != fingerCount) {
      state = State.REJECTED
      return
    }
    var sumX = 0f
    var sumY = 0f
    for (i in 0 until fingerCount) {
      val index = event.findPointerIndex(ids[i])
      if (index < 0) {
        state = State.REJECTED
        return
      }
      sumX += event.getX(index)
      sumY += event.getY(index)
    }
    val x = sumX / fingerCount
    val y = sumY / fingerCount
    var sumRadius = 0f
    var sumTurn = 0f
    for (i in 0 until fingerCount) {
      val index = event.findPointerIndex(ids[i])
      val fx = event.getX(index)
      val fy = event.getY(index)
      sumRadius += hypot(fx - x, fy - y)
      val angle = angle(fx, fy, x, y)
      var delta = angle - lastAngles[i]
      while (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
      while (delta <= -Math.PI) delta += (2 * Math.PI).toFloat()
      turned[i] += delta
      lastAngles[i] = angle
      sumTurn += turned[i]
    }
    val radius = sumRadius / fingerCount
    radialChange = radius - startRadius
    translation = hypot(x - startX, y - startY)
    // How far the fingers moved around the middle, as in a rotation.
    val arc = abs(sumTurn / fingerCount) * radius
    val change = abs(radialChange)
    if (change < minChangePx) {
      return
    }
    val ratio = if (startRadius > 0f) radius / startRadius else Float.MAX_VALUE
    val spread = radialChange > 0
    val farEnough = if (spread) ratio >= MIN_RATIO else ratio <= 1f / MIN_RATIO
    if (farEnough && translation <= change && arc <= change) {
      if (GestureLog.enabled) {
        val name = if (spread) "Spread" else "Pinch"
        Log.d(
          DEBUG_TAG,
          "$name with $fingerCount fingers: change=%.0f ratio=%.2f translation=%.0f arc=%.0f"
            .format(radialChange, ratio, translation, arc),
        )
      }
      state = State.RECOGNIZED
      listener.onPinch(fingerCount, spread)
    } else if (translation > change * GIVE_UP_FACTOR || arc > change * GIVE_UP_FACTOR) {
      if (GestureLog.enabled) {
        Log.d(DEBUG_TAG, "Pinch rejected: translation=%.0f arc=%.0f".format(translation, arc))
      }
      state = State.REJECTED
    }
  }

  companion object {
    private const val MAX_FINGERS = 4

    /**
     * How much the fingers' distance from their middle must grow, or shrink by the inverse, for a
     * spread or a pinch.
     */
    private const val MIN_RATIO = 1.3f

    /** A movement that moves or turns this much more than it pinches is something else. */
    private const val GIVE_UP_FACTOR = 2f

    private const val DEBUG_TAG = "BacktalkGesture"

    /** The angle of a finger around the middle, in radians. */
    private fun angle(x: Float, y: Float, middleX: Float, middleY: Float): Float =
      atan2((y - middleY).toDouble(), (x - middleX).toDouble()).toFloat()
  }
}
