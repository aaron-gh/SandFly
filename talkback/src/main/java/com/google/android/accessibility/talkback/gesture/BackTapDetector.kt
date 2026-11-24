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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Finds taps on the back of the device in accelerometer readings and counts them into double,
 * triple and quadruple taps.
 *
 * A tap is a short jolt, mostly along the axis through the screen, after a moment with little
 * movement. Gravity and slow movement are taken out with a low-pass filter. A touch on the screen
 * also jolts the device, so taps while the screen is touched, and shortly before or after, are
 * dropped.
 *
 * Not thread-safe: every call must come from the same thread. All times are in the clock of the
 * readings' timestamps. Readings may come in batches, after the touches on the screen around them:
 * a touch is applied when the readings reach its time.
 */
class BackTapDetector(private val listener: Listener) {

  interface Listener {
    /** Runs when [count] taps, 2 to 4, have ended. */
    fun onTaps(count: Int)

    /** Describes a jolt, taken as a tap or not, for the debug log. */
    fun onJolt(description: String) {}

    /** A jolt strong enough to be measured was taken as a tap or not, as [jolt] says. */
    fun onJoltJudged(jolt: Jolt) {}

    /** A run of [count] taps ended, 1 to 4, whether or not it runs a command. */
    fun onRunEnded(count: Int) {}
  }

  /**
   * A jolt at [timeNs] of [strength] along the axis through the screen, in m/s², where [needed] was
   * needed for a tap. [rejection] says why it wasn't taken as a tap, or is null for a tap.
   */
  class Jolt(val timeNs: Long, val strength: Float, val needed: Float, val rejection: Rejection?)

  enum class Rejection {
    TOO_LIGHT,
    SCREEN_TOUCHED,
    JUST_AFTER_RUN,
    SIDEWAYS,
    MOVING,
  }

  /**
   * How strong a jolt must be along the axis through the screen, in m/s², to count as the first tap.
   * Later taps count from [FOLLOWING_TAP_RATIO] of it: a second tap is mostly weaker than the first.
   */
  var threshold: Float = Sensitivity.MEDIUM.threshold

  /**
   * The most taps that run a command, 2 to 4. Fewer taps wait to see whether another tap follows;
   * this many run as soon as the last one is measured.
   */
  var mostTaps: Int = MAX_TAPS

  /** How strong a jolt must be at each sensitivity, in m/s². */
  enum class Sensitivity(val threshold: Float) {
    LOW(14f),
    MEDIUM(9f),
    HIGH(6f),
  }

  private var started = false
  private var lastNs = 0L
  // Low-passed readings: gravity and slow movement.
  private var slowX = 0f
  private var slowY = 0f
  private var slowZ = 0f
  // Average size of the fast movement, the readings less the low-passed ones.
  private var movement = 0f

  // The jolt being measured: when it started, or -1 for none.
  private var joltStartNs = NONE
  private var joltZ = 0f
  private var joltSideways = 0f
  private var movementBeforeJolt = 0f
  private var nextJoltNs = 0L

  private var taps = 0
  private var lastTapNs = 0L
  private var ignoreTapsUntilNs = 0L

  private var touching = false
  private var touchEndNs = NONE
  // Touches on the screen that the readings haven't reached yet, in time order.
  private val pendingTouchNs = ArrayDeque<Long>()
  private val pendingTouchStarts = ArrayDeque<Boolean>()

  /** Takes in a reading of the accelerometer: [timeNs] and acceleration in m/s². */
  fun onSample(timeNs: Long, x: Float, y: Float, z: Float) {
    if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
      return
    }
    while (pendingTouchNs.isNotEmpty() && pendingTouchNs.first() <= timeNs) {
      applyTouch(pendingTouchNs.removeFirst(), pendingTouchStarts.removeFirst())
    }
    if (!started || timeNs <= lastNs || timeNs - lastNs > MAX_SAMPLE_GAP_NS) {
      // The first reading, or readings stopped for a while: start again from this one.
      restart(timeNs, x, y, z)
      return
    }
    val dt = (timeNs - lastNs) / 1e9f
    lastNs = timeNs
    val fastX = x - slowX
    val fastY = y - slowY
    val fastZ = z - slowZ
    val slowAlpha = dt / (SLOW_TAU_S + dt)
    slowX += fastX * slowAlpha
    slowY += fastY * slowAlpha
    slowZ += fastZ * slowAlpha

    if (joltStartNs == NONE) {
      if (abs(fastZ) >= threshold * MEASURED_RATIO && timeNs >= nextJoltNs) {
        joltStartNs = timeNs
        joltZ = abs(fastZ)
        joltSideways = max(abs(fastX), abs(fastY))
        movementBeforeJolt = movement
      }
    } else {
      joltZ = max(joltZ, abs(fastZ))
      joltSideways = max(joltSideways, max(abs(fastX), abs(fastY)))
      if (timeNs - joltStartNs >= JOLT_NS) {
        onJoltMeasured(timeNs)
      }
    }

    val size = sqrt(fastX * fastX + fastY * fastY + fastZ * fastZ)
    movement += (size - movement) * (dt / (MOVEMENT_TAU_S + dt))

    finishTaps(timeNs)
  }

  /** The screen is touched at [timeNs]: drops the taps so far and any until the touch ends. */
  fun onTouchStart(timeNs: Long) {
    onTouch(timeNs, start = true)
  }

  /** The touch on the screen ended at [timeNs]. */
  fun onTouchEnd(timeNs: Long) {
    onTouch(timeNs, start = false)
  }

  private fun onTouch(timeNs: Long, start: Boolean) {
    when {
      // A time far from the readings' is in another clock: the touch counts from the last reading.
      !started || abs(timeNs - lastNs) > MAX_TOUCH_OFFSET_NS -> applyTouch(lastNs, start)
      timeNs <= lastNs && pendingTouchNs.isEmpty() -> applyTouch(timeNs, start)
      else -> {
        pendingTouchNs.addLast(maxOf(timeNs, pendingTouchNs.lastOrNull() ?: timeNs))
        pendingTouchStarts.addLast(start)
      }
    }
  }

  private fun applyTouch(timeNs: Long, start: Boolean) {
    touching = start
    if (start) {
      taps = 0
    } else {
      touchEndNs = timeNs
    }
  }

  /** Drops the taps so far and any in the next [MAX_TAP_INTERVAL_NS] from [timeNs], as after a shake. */
  fun dropTaps(timeNs: Long) {
    taps = 0
    ignoreTapsUntilNs = maxOf(ignoreTapsUntilNs, timeNs + MAX_TAP_INTERVAL_NS)
  }

  private fun restart(timeNs: Long, x: Float, y: Float, z: Float) {
    started = true
    lastNs = timeNs
    slowX = x
    slowY = y
    slowZ = z
    movement = 0f
    joltStartNs = NONE
    taps = 0
    nextJoltNs = timeNs + SETTLE_NS
  }

  private fun onJoltMeasured(timeNs: Long) {
    val startNs = joltStartNs
    joltStartNs = NONE
    val following = taps > 0 && startNs - lastTapNs <= MAX_TAP_INTERVAL_NS
    val needed = if (following) threshold * FOLLOWING_TAP_RATIO else threshold
    if (joltZ < needed) {
      // Too light to be a tap: measured only to be described, and changes nothing.
      listener.onJolt(
        String.format(java.util.Locale.ROOT, "Back tap jolt %.1f: too light, %.1f needed", joltZ, needed)
      )
      listener.onJoltJudged(Jolt(startNs, joltZ, needed, Rejection.TOO_LIGHT))
      return
    }
    // The device rings for a moment after a jolt.
    nextJoltNs = startNs + RING_NS
    val stillnessRatio = if (taps == 0) STILL_BEFORE_FIRST_TAP else STILL_BEFORE_NEXT_TAP
    val rejection =
      when {
        touching || (touchEndNs != NONE && timeNs - touchEndNs < TOUCH_GUARD_NS) ->
          Rejection.SCREEN_TOUCHED
        startNs < ignoreTapsUntilNs -> Rejection.JUST_AFTER_RUN
        joltZ < SCREEN_AXIS_RATIO * joltSideways -> Rejection.SIDEWAYS
        movementBeforeJolt > stillnessRatio * joltZ -> Rejection.MOVING
        else -> null
      }
    listener.onJolt(
      String.format(
        java.util.Locale.ROOT,
        "Back tap jolt %.1f, sideways %.1f, movement before %.2f: %s",
        joltZ,
        joltSideways,
        movementBeforeJolt,
        rejection?.let { it.name.lowercase().replace('_', ' ') } ?: "tap ${taps + 1}",
      )
    )
    listener.onJoltJudged(Jolt(startNs, joltZ, needed, rejection))
    if (rejection != null) {
      // A jolt that isn't a tap between taps means the device is being handled.
      if (rejection != Rejection.JUST_AFTER_RUN) {
        taps = 0
      }
      return
    }
    if (taps > 0 && startNs - lastTapNs > MAX_TAP_INTERVAL_NS) {
      taps = 0
    }
    if (taps < MAX_TAPS) {
      taps++
      lastTapNs = startNs
    }
  }

  private fun finishTaps(timeNs: Long) {
    if (taps == 0) {
      return
    }
    val sinceLastTap = timeNs - lastTapNs
    val count =
      when {
        // Waits long enough for a touch on the screen to be reported.
        sinceLastTap < TOUCH_GUARD_NS -> return
        taps >= mostTaps.coerceIn(2, MAX_TAPS) -> taps
        sinceLastTap > MAX_TAP_INTERVAL_NS -> taps
        else -> return
      }
    taps = 0
    // Taps that follow at once belong to this run.
    ignoreTapsUntilNs = timeNs + MAX_TAP_INTERVAL_NS
    listener.onRunEnded(count)
    if (count >= 2) {
      listener.onTaps(count)
    }
  }

  companion object {
    private const val NONE = -1L
    private const val MAX_TAPS = 4
    private const val MS = 1_000_000L

    /** Time constant of the low-pass filter that takes out gravity and slow movement. */
    private const val SLOW_TAU_S = 0.02f
    /** Time constant of the average of the fast movement before a jolt. */
    private const val MOVEMENT_TAU_S = 0.1f
    /** Readings this far apart start the filters again. */
    private const val MAX_SAMPLE_GAP_NS = 100 * MS
    /** Time the filters take to settle after they start. */
    private const val SETTLE_NS = 100 * MS
    /** Part of the threshold from which jolts are measured, so that ones too light are logged. */
    private const val MEASURED_RATIO = 0.5f
    /** Part of the threshold a later tap must reach. */
    private const val FOLLOWING_TAP_RATIO = 0.75f
    /** How long the strongest part of a jolt is looked for. */
    private const val JOLT_NS = 25 * MS
    /** Time after the start of a jolt in which the device still rings. */
    private const val RING_NS = 90 * MS
    /** Longest time between the taps of a double, triple or quadruple tap. */
    const val MAX_TAP_INTERVAL_NS = 450 * MS
    /** Time before and after a touch on the screen in which jolts are dropped. */
    private const val TOUCH_GUARD_NS = 150 * MS
    /**
     * How far a touch's time may be from the last reading's and still be in the same clock. Batched
     * readings come up to [AccelerometerGestures]' report latency late.
     */
    private const val MAX_TOUCH_OFFSET_NS = 2_000 * MS
    /** How much stronger a tap is along the axis through the screen than along the others. */
    private const val SCREEN_AXIS_RATIO = 1.2f
    /** Most movement before the first tap, as a part of the tap's strength. */
    private const val STILL_BEFORE_FIRST_TAP = 0.25f
    /** Most movement before a later tap, which may include the ringing of the one before. */
    private const val STILL_BEFORE_NEXT_TAP = 0.45f
  }
}
