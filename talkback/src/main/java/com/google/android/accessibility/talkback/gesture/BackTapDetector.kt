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
 * Finds taps on the back of the device in accelerometer readings and counts them into double and
 * triple taps.
 *
 * A tap is a short jolt, mostly along the axis through the screen, after a moment with little
 * movement. Gravity and slow movement are taken out with a low-pass filter. A touch on the screen
 * also jolts the device, so taps while the screen is touched, and shortly before or after, are
 * dropped.
 *
 * Not thread-safe: every call must come from the same thread. All times are in the clock of the
 * readings' timestamps.
 */
class BackTapDetector(private val listener: Listener) {

  interface Listener {
    /** Runs when [count] taps, 2 or 3, have ended. */
    fun onTaps(count: Int)

    /** Describes a jolt, taken as a tap or not, for the debug log. */
    fun onJolt(description: String) {}
  }

  /** How strong a jolt must be along the axis through the screen, in m/s², to count as a tap. */
  var threshold: Float = Sensitivity.MEDIUM.threshold

  /**
   * Whether a double tap waits to see whether a third tap follows. Without a command for the triple
   * tap, the double tap runs sooner.
   */
  var waitForTriple: Boolean = true

  /** How strong a jolt must be at each sensitivity, in m/s². */
  enum class Sensitivity(val threshold: Float) {
    LOW(6f),
    MEDIUM(3.5f),
    HIGH(2f),
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

  /** Takes in a reading of the accelerometer: [timeNs] and acceleration in m/s². */
  fun onSample(timeNs: Long, x: Float, y: Float, z: Float) {
    if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
      return
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
      if (abs(fastZ) >= threshold && timeNs >= nextJoltNs) {
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

  /** The screen is touched: drops the taps so far and any until the touch ends. */
  fun onTouchStart() {
    touching = true
    taps = 0
  }

  /** The touch on the screen ended. */
  fun onTouchEnd() {
    touching = false
    touchEndNs = lastNs
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
    // The device rings for a moment after a jolt.
    nextJoltNs = startNs + RING_NS
    val stillnessRatio = if (taps == 0) STILL_BEFORE_FIRST_TAP else STILL_BEFORE_NEXT_TAP
    val rejection =
      when {
        touching || (touchEndNs != NONE && timeNs - touchEndNs < TOUCH_GUARD_NS) -> "screen touched"
        startNs < ignoreTapsUntilNs -> "just after a run"
        joltZ < SCREEN_AXIS_RATIO * joltSideways -> "sideways"
        movementBeforeJolt > stillnessRatio * joltZ -> "moving"
        else -> null
      }
    listener.onJolt(
      String.format(
        java.util.Locale.ROOT,
        "Back tap jolt %.1f, sideways %.1f, movement before %.2f: %s",
        joltZ,
        joltSideways,
        movementBeforeJolt,
        rejection ?: "tap ${taps + 1}",
      )
    )
    if (rejection != null) {
      // A jolt that isn't a tap between taps means the device is being handled.
      if (rejection != "just after a run") {
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
        taps == MAX_TAPS -> MAX_TAPS
        taps == 2 && !waitForTriple -> 2
        sinceLastTap > MAX_TAP_INTERVAL_NS -> taps
        else -> return
      }
    taps = 0
    // Taps that follow at once belong to this run.
    ignoreTapsUntilNs = timeNs + MAX_TAP_INTERVAL_NS
    if (count >= 2) {
      listener.onTaps(count)
    }
  }

  private companion object {
    const val NONE = -1L
    const val MAX_TAPS = 3
    const val MS = 1_000_000L

    /** Time constant of the low-pass filter that takes out gravity and slow movement. */
    const val SLOW_TAU_S = 0.02f
    /** Time constant of the average of the fast movement before a jolt. */
    const val MOVEMENT_TAU_S = 0.1f
    /** Readings this far apart start the filters again. */
    const val MAX_SAMPLE_GAP_NS = 100 * MS
    /** Time the filters take to settle after they start. */
    const val SETTLE_NS = 100 * MS
    /** How long the strongest part of a jolt is looked for. */
    const val JOLT_NS = 25 * MS
    /** Time after the start of a jolt in which the device still rings. */
    const val RING_NS = 90 * MS
    /** Longest time between the taps of a double or triple tap. */
    const val MAX_TAP_INTERVAL_NS = 450 * MS
    /** Time before and after a touch on the screen in which jolts are dropped. */
    const val TOUCH_GUARD_NS = 150 * MS
    /** How much stronger a tap is along the axis through the screen than along the others. */
    const val SCREEN_AXIS_RATIO = 1.2f
    /** Most movement before the first tap, as a part of the tap's strength. */
    const val STILL_BEFORE_FIRST_TAP = 0.25f
    /** Most movement before a later tap, which may include the ringing of the one before. */
    const val STILL_BEFORE_NEXT_TAP = 0.45f
  }
}
