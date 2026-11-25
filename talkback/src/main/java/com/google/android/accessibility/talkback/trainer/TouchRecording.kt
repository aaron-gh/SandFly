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

package com.google.android.accessibility.talkback.trainer

import android.view.MotionEvent
import kotlin.math.hypot

/** Where a finger was at [timeMs], in screen pixels. */
class TouchPoint(val timeMs: Long, val x: Float, val y: Float) {
  fun distanceTo(other: TouchPoint): Float = hypot(other.x - x, other.y - y)
}

/** The path of one finger from landing to lifting. */
class FingerPath(val pointerId: Int) {
  val points = ArrayList<TouchPoint>()

  /** When the finger lifted, or -1 while it is down. */
  var upMs = -1L

  val start: TouchPoint
    get() = points.first()

  val end: TouchPoint
    get() = points.last()

  val downMs: Long
    get() = start.timeMs

  /** How far the finger got from where it landed, at most. */
  fun farthestFromStart(): Float = points.maxOf { it.distanceTo(start) }

  /**
   * Adds [point] to the path. A finger held on the screen keeps sending points: past
   * [MAX_POINTS] the last point is replaced, so that the path still ends where the finger is.
   */
  fun add(point: TouchPoint) {
    if (points.size < MAX_POINTS) points += point else points[points.lastIndex] = point
  }

  companion object {
    const val MAX_POINTS = 2000
  }
}

/** Fingers on the screen from the first landing to the last lifting. */
class Touch {
  val fingers = ArrayList<FingerPath>()

  /** Whether the touch became touch exploration, after which its motion events stopped. */
  var becameExploration = false

  /** The most fingers that were on the screen at once. */
  var mostFingersAtOnce = 0

  val downMs: Long
    get() = fingers.first().downMs

  val upMs: Long
    get() = fingers.maxOf { it.upMs }

  val durationMs: Long
    get() = upMs - downMs

  /** The time between the first finger landing and the last. */
  val landingSpreadMs: Long
    get() = fingers.maxOf { it.downMs } - downMs

  /** Whether a finger lifted before the last finger landed. */
  val liftedBeforeAllLanded: Boolean
    get() = fingers.any { it.upMs in 0 until fingers.maxOf { finger -> finger.downMs } }
}

/** Builds the touches of one attempt from motion events. */
class TouchRecorder {
  val touches = ArrayList<Touch>()
  private var current: Touch? = null
  private val down = HashMap<Int, FingerPath>()

  val fingersDown: Boolean
    get() = current != null

  fun onMotionEvent(event: MotionEvent) {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        val touch = Touch()
        touches += touch
        current = touch
        down.clear()
        land(event, event.actionIndex)
      }
      MotionEvent.ACTION_POINTER_DOWN -> land(event, event.actionIndex)
      MotionEvent.ACTION_MOVE -> move(event)
      MotionEvent.ACTION_POINTER_UP -> lift(event, event.actionIndex)
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        for (index in 0 until event.pointerCount) lift(event, index)
        current = null
        down.clear()
      }
    }
    current?.let { it.mostFingersAtOnce = maxOf(it.mostFingersAtOnce, event.pointerCount) }
  }

  /**
   * Ends the touch whose lifting the service never saw: once a touch becomes touch exploration or
   * goes to the app, its motion events stop. The fingers count as lifted at [timeMs], where they
   * were last seen.
   */
  fun endUnseenTouch(timeMs: Long, explored: Boolean) {
    val touch = current ?: return
    touch.becameExploration = explored
    for (path in down.values) path.upMs = timeMs
    current = null
    down.clear()
  }

  private fun land(event: MotionEvent, index: Int) {
    val touch = current ?: return
    val path = FingerPath(event.getPointerId(index))
    path.add(TouchPoint(event.eventTime, event.getX(index), event.getY(index)))
    touch.fingers += path
    down[path.pointerId] = path
  }

  private fun move(event: MotionEvent) {
    for (index in 0 until event.pointerCount) {
      val path = down[event.getPointerId(index)] ?: continue
      for (h in 0 until event.historySize) {
        path.add(
          TouchPoint(
            event.getHistoricalEventTime(h),
            event.getHistoricalX(index, h),
            event.getHistoricalY(index, h),
          )
        )
      }
      path.add(TouchPoint(event.eventTime, event.getX(index), event.getY(index)))
    }
  }

  private fun lift(event: MotionEvent, index: Int) {
    val path = down.remove(event.getPointerId(index)) ?: return
    path.add(TouchPoint(event.eventTime, event.getX(index), event.getY(index)))
    path.upMs = event.eventTime
  }
}
