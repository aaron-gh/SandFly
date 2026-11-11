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

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * Recognizes one-finger taps in a corner of the screen: a tap, double tap, triple tap, double tap
 * and hold, and triple tap and hold, in each of the four corners. A corner is a square of
 * [cornerSizePx] at each corner of the screen.
 *
 * Only a touch that lands in a corner with at least one gesture that [assigned] gives an action
 * starts a sequence; every other touch is left alone. While the taps go on, the tracker keeps their
 * motion events, so that no other gesture, touch exploration or passthrough sees them. A tap must
 * lift within [tapTimeoutMs] and [touchSlop] of where it landed, and the next tap must land within
 * [multiTapTimeoutMs] of the lift and [doubleTapSlop] of the previous tap, as for the double tap
 * gesture. A double tap waits for a third tap only while a triple tap gesture of that corner has an
 * action, and a tap always waits for a second, so that a double tap keeps activating. When the taps
 * end:
 * - with a gesture of that tap count that has an action, it is reported as [Result.Gesture];
 * - otherwise they are reported as [Result.Taps], which act as ordinary taps;
 * - a tap held past [tapTimeoutMs] becomes a hold, reported after [holdTimeoutMs] from its landing,
 *   or at once when [tapTimeoutMs] is longer, when its hold gesture has an action.
 *
 * Any other touch, such as a finger that moves, lands a second finger, holds without a hold
 * gesture or lands away from the previous tap, isn't a corner gesture: [Result.Released] hands back
 * the kept events, in order, to be handled as usual. A held tap that lifts before its hold gesture
 * is one tap, [Result.Taps] of 1, so that the taps before it don't make a double tap.
 *
 * A touch that lands in a corner as the second tap of a double tap whose first tap was outside the
 * corner, reported by [onLift], is left alone.
 *
 * Time passes in [onTimeout], which the caller runs at [deadline]; motion events never complete a
 * sequence on time alone.
 */
class CornerTapTracker(
  private var cornerSizePx: Float,
  private val touchSlop: Float,
  private val doubleTapSlop: Float,
  private var tapTimeoutMs: Long,
  private val holdTimeoutMs: Long,
  // The longest time between a tap's lift and the next tap's landing.
  private val multiTapTimeoutMs: () -> Long,
  // Whether the gesture of the corner has an action.
  private val assigned: (Corner, Kind) -> Boolean,
) {

  enum class Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
  }

  /** The gestures of each corner, by number of taps and whether the last one is held. */
  enum class Kind(val taps: Int, val hold: Boolean) {
    TAP(1, false),
    DOUBLE_TAP(2, false),
    TRIPLE_TAP(3, false),
    DOUBLE_TAP_AND_HOLD(2, true),
    TRIPLE_TAP_AND_HOLD(3, true);

    companion object {
      fun of(taps: Int, hold: Boolean): Kind? =
        entries.firstOrNull { it.taps == taps && it.hold == hold }
    }
  }

  /** Says whether a touch at a point may start a sequence, such as when it isn't on the keyboard. */
  fun interface StartCheck {
    fun allows(x: Float, y: Float): Boolean
  }

  /** What became of an event or a timeout. */
  sealed interface Result {
    /** The event is no part of a corner sequence: handle it as usual. */
    data object Ignored : Result

    /** The tracker keeps the event, or the sequence goes on after a timeout. */
    data object Kept : Result

    /** The corner gesture [kind] in [corner]. */
    data class Gesture(val corner: Corner, val kind: Kind) : Result

    /** [taps] taps that no corner gesture takes, with the finger up. */
    data class Taps(val taps: Int) : Result

    /**
     * No corner gesture: [events], copies of those the tracker kept, go on as usual, in order. They
     * are the receiver's, and are left to the garbage collector, as gesture matchers may keep them.
     */
    class Released(val events: List<MotionEvent>) : Result
  }

  private var corner: Corner? = null
  // Taps that have lifted.
  private var taps = 0
  // The most taps the corner's gestures need.
  private var maxTaps = 0
  private var fingerDown = false
  // Past the tap timeout, waiting for the hold.
  private var holding = false
  private var downX = 0f
  private var downY = 0f
  private var downTime = 0L
  private val kept = ArrayList<MotionEvent>()
  // The last lift of a touch that wasn't corner taps, or NO_LIFT.
  private var liftTime = NO_LIFT
  private var liftX = 0f
  private var liftY = 0f

  /** When [onTimeout] is due, in uptime milliseconds, or [NO_DEADLINE]. */
  var deadline = NO_DEADLINE
    private set

  /** Whether a sequence of corner taps is going on. */
  val isActive: Boolean
    get() = corner != null

  fun setCornerSize(px: Float) {
    cornerSizePx = px
  }

  /** Sets how long a tap may stay down before it is a hold; takes effect from the next landing. */
  fun setTapTimeout(ms: Long) {
    tapTimeoutMs = ms
  }

  /** Returns the corner of the screen of [width] by [height] that ([x], [y]) is in, or null. */
  fun cornerAt(x: Float, y: Float, width: Int, height: Int): Corner? {
    if (width <= 0 || height <= 0) {
      return null
    }
    val left = x <= cornerSizePx
    val right = !left && x >= width - cornerSizePx
    val top = y <= cornerSizePx
    val bottom = !top && y >= height - cornerSizePx
    return when {
      top && left -> Corner.TOP_LEFT
      top && right -> Corner.TOP_RIGHT
      bottom && left -> Corner.BOTTOM_LEFT
      bottom && right -> Corner.BOTTOM_RIGHT
      else -> null
    }
  }

  /** Follows [event] on a screen of [width] by [height]. */
  fun onMotionEvent(event: MotionEvent, width: Int, height: Int, check: StartCheck): Result {
    val current = corner
    if (current == null) {
      return if (event.actionMasked == MotionEvent.ACTION_DOWN) {
        start(event, width, height, check)
      } else {
        Result.Ignored
      }
    }
    kept += MotionEvent.obtain(event)
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        if (
          fingerDown ||
            event.eventTime > deadline ||
            hypot(event.x - downX, event.y - downY) > doubleTapSlop
        ) {
          return release()
        }
        land(event)
      }
      MotionEvent.ACTION_MOVE ->
        if (hypot(event.x - downX, event.y - downY) > touchSlop) {
          return release()
        }
      MotionEvent.ACTION_UP -> {
        if (hypot(event.x - downX, event.y - downY) > touchSlop) {
          return release()
        }
        if (holding) {
          // Lifted past the tap timeout but before the hold gesture: as elsewhere, the taps before
          // it and the held tap make no double tap, and the held tap is explored.
          reset()
          return Result.Taps(1)
        }
        taps++
        fingerDown = false
        if (taps >= maxTaps) {
          return resolve(current)
        }
        deadline = event.eventTime + multiTapTimeoutMs()
      }
      else -> return release()
    }
    return Result.Kept
  }

  /** Lets time pass up to [now], in uptime milliseconds. */
  fun onTimeout(now: Long): Result {
    val current = corner ?: return Result.Ignored
    if (deadline == NO_DEADLINE || now < deadline) {
      return Result.Kept
    }
    if (!fingerDown) {
      return resolve(current)
    }
    val hold = Kind.of(taps + 1, hold = true)
    if (holding && hold != null) {
      reset()
      return Result.Gesture(current, hold)
    }
    if (hold != null && assigned(current, hold)) {
      if (now >= downTime + holdTimeoutMs) {
        // The tap timeout is as long as the hold timeout or longer.
        reset()
        return Result.Gesture(current, hold)
      }
      holding = true
      deadline = downTime + holdTimeoutMs
      return Result.Kept
    }
    return release()
  }

  /**
   * Ends a sequence whose finger is up at once, as when the touch interaction ends, and returns
   * what its taps make. Returns [Result.Kept] while the finger is down.
   */
  fun resolveNow(): Result {
    val current = corner ?: return Result.Ignored
    return if (fingerDown) Result.Kept else resolve(current)
  }

  /**
   * Notes the lift of a one-finger touch that isn't corner taps, at [event], so that a touch landing
   * in a corner as the next tap of a double tap is left alone.
   */
  fun onLift(event: MotionEvent) {
    if (isActive) return
    liftTime = event.eventTime
    liftX = event.x
    liftY = event.y
  }

  /** Ends any sequence, dropping the events it kept, and forgets the last lift. */
  fun clear() {
    reset()
    liftTime = NO_LIFT
  }

  private fun start(event: MotionEvent, width: Int, height: Int, check: StartCheck): Result {
    val landed = cornerAt(event.x, event.y, width, height) ?: return Result.Ignored
    if (Kind.entries.none { assigned(landed, it) } || !check.allows(event.x, event.y)) {
      return Result.Ignored
    }
    if (
      liftTime != NO_LIFT &&
        event.eventTime - liftTime <= multiTapTimeoutMs() &&
        hypot(event.x - liftX, event.y - liftY) <= doubleTapSlop
    ) {
      // The second tap of a double tap that started outside the corner.
      return Result.Ignored
    }
    corner = landed
    taps = 0
    maxTaps =
      if (assigned(landed, Kind.TRIPLE_TAP) || assigned(landed, Kind.TRIPLE_TAP_AND_HOLD)) 3 else 2
    kept += MotionEvent.obtain(event)
    land(event)
    return Result.Kept
  }

  private fun land(event: MotionEvent) {
    fingerDown = true
    holding = false
    downX = event.x
    downY = event.y
    downTime = event.eventTime
    deadline = downTime + tapTimeoutMs
  }

  private fun resolve(current: Corner): Result {
    val tapped = taps
    reset()
    val kind = Kind.of(tapped, hold = false)
    return if (kind != null && assigned(current, kind)) {
      Result.Gesture(current, kind)
    } else {
      Result.Taps(tapped)
    }
  }

  private fun release(): Result {
    val events = ArrayList(kept)
    kept.clear()
    reset()
    return Result.Released(events)
  }

  private fun reset() {
    kept.forEach { it.recycle() }
    kept.clear()
    corner = null
    taps = 0
    maxTaps = 0
    fingerDown = false
    holding = false
    deadline = NO_DEADLINE
  }

  companion object {
    const val NO_DEADLINE = Long.MAX_VALUE
    private const val NO_LIFT = Long.MIN_VALUE
  }
}
