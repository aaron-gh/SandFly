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

import kotlin.math.sqrt

/**
 * Finds shakes of the device in accelerometer readings.
 *
 * Gravity is taken out with a slow low-pass filter, and short jolts such as taps and their ringing
 * with a fast one. A swing is a stretch in which the acceleration that is left is at least
 * [threshold]; it ends when the acceleration falls to half of it or turns away from the swing's
 * strongest direction. A shake is [SWINGS] swings, each in about the opposite direction of the one
 * before, at most [MAX_SWING_GAP_NS] apart and within [SHAKE_NS]: three reversals. After a shake,
 * shakes are ignored for [COOLDOWN_NS] and until the device has been still for [QUIET_NS], so
 * shaking that goes on runs one shake.
 *
 * Not thread-safe: every call must come from the same thread. Times are in the clock of the
 * readings' timestamps.
 */
class ShakeDetector(private val onShake: (timeNs: Long) -> Unit) {

  /** Learns how shaking went, for the gesture trainer. Called on the thread of the readings. */
  interface Observer {
    /** The device moved with [strength] in m/s² beyond gravity, at least half the threshold. */
    fun onMovement(timeNs: Long, strength: Float) {}

    /** A swing ended, and [swings] swings, each the other way, now count toward a shake. */
    fun onSwing(timeNs: Long, swings: Int) {}
  }

  var observer: Observer? = null

  /** How strong each swing must be, in m/s² beyond gravity. */
  var threshold: Float = Sensitivity.MEDIUM.threshold

  /** How strong a swing must be at each sensitivity, in m/s² beyond gravity. */
  enum class Sensitivity(val threshold: Float) {
    LOW(20f),
    MEDIUM(14f),
    HIGH(10f),
  }

  private var started = false
  private var lastNs = 0L
  private var ignoreUntilNs = 0L
  // Low-passed readings: gravity.
  private var gravityX = 0f
  private var gravityY = 0f
  private var gravityZ = 0f
  // The acceleration beyond gravity, without short jolts.
  private var moveX = 0f
  private var moveY = 0f
  private var moveZ = 0f

  // The swing being measured: its strongest point and its direction there.
  private var inSwing = false
  private var swingPeak = 0f
  private var swingPeakNs = 0L
  private var swingDirX = 0f
  private var swingDirY = 0f
  private var swingDirZ = 0f

  // The last swing, and the times of the swings counted towards a shake.
  private var lastSwingNs = 0L
  private var lastDirX = 0f
  private var lastDirY = 0f
  private var lastDirZ = 0f
  private val swingTimesNs = ArrayDeque<Long>()

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
    val gravityAlpha = dt / (GRAVITY_TAU_S + dt)
    gravityX += (x - gravityX) * gravityAlpha
    gravityY += (y - gravityY) * gravityAlpha
    gravityZ += (z - gravityZ) * gravityAlpha
    val moveAlpha = dt / (MOVE_TAU_S + dt)
    moveX += (x - gravityX - moveX) * moveAlpha
    moveY += (y - gravityY - moveY) * moveAlpha
    moveZ += (z - gravityZ - moveZ) * moveAlpha
    val size = sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ)
    if (size >= threshold * SWING_END_RATIO) {
      observer?.onMovement(timeNs, size)
    }

    if (timeNs < ignoreUntilNs) {
      // Settling, or just after a shake: movement keeps the shakes ignored until it stops.
      if (size >= threshold) {
        ignoreUntilNs = maxOf(ignoreUntilNs, timeNs + QUIET_NS)
      }
      inSwing = false
      swingTimesNs.clear()
      return
    }

    if (!inSwing) {
      if (size >= threshold) {
        inSwing = true
        setPeak(timeNs, size)
      }
    } else {
      val along = moveX * swingDirX + moveY * swingDirY + moveZ * swingDirZ
      if (size > swingPeak && along > 0f) {
        setPeak(timeNs, size)
      } else if (size < threshold * SWING_END_RATIO || along <= 0f) {
        inSwing = false
        onSwing()
        if (size >= threshold && along <= 0f) {
          // The acceleration turned around without dropping: the next swing starts at once.
          inSwing = true
          setPeak(timeNs, size)
        }
      }
    }
  }

  private fun setPeak(timeNs: Long, size: Float) {
    swingPeak = size
    swingPeakNs = timeNs
    swingDirX = moveX / size
    swingDirY = moveY / size
    swingDirZ = moveZ / size
  }

  private fun onSwing() {
    val peakNs = swingPeakNs
    if (swingTimesNs.isEmpty() || peakNs - lastSwingNs > MAX_SWING_GAP_NS) {
      startChain(peakNs)
      return
    }
    val gap = peakNs - lastSwingNs
    val dot = swingDirX * lastDirX + swingDirY * lastDirY + swingDirZ * lastDirZ
    when {
      // Too soon after the last swing to be a swing back: ringing or a jolt.
      gap < MIN_SWING_GAP_NS -> return
      dot <= -REVERSAL_COS -> {
        swingTimesNs.addLast(peakNs)
        setLast(peakNs)
      }
      // A swing on in the same direction, as in a jerky shake: neither counted nor breaking.
      dot >= REVERSAL_COS -> setLast(peakNs)
      else -> {
        startChain(peakNs)
        return
      }
    }
    while (swingTimesNs.size > 1 && peakNs - swingTimesNs.first() > SHAKE_NS) {
      swingTimesNs.removeFirst()
    }
    observer?.onSwing(peakNs, swingTimesNs.size)
    if (swingTimesNs.size >= SWINGS) {
      swingTimesNs.clear()
      ignoreUntilNs = peakNs + COOLDOWN_NS
      onShake(peakNs)
    }
  }

  private fun startChain(peakNs: Long) {
    swingTimesNs.clear()
    swingTimesNs.addLast(peakNs)
    setLast(peakNs)
    observer?.onSwing(peakNs, 1)
  }

  private fun setLast(peakNs: Long) {
    lastSwingNs = peakNs
    lastDirX = swingDirX
    lastDirY = swingDirY
    lastDirZ = swingDirZ
  }

  private fun restart(timeNs: Long, x: Float, y: Float, z: Float) {
    started = true
    lastNs = timeNs
    gravityX = x
    gravityY = y
    gravityZ = z
    moveX = 0f
    moveY = 0f
    moveZ = 0f
    inSwing = false
    swingTimesNs.clear()
    ignoreUntilNs = maxOf(ignoreUntilNs, timeNs + SETTLE_NS)
  }

  companion object {
    private const val MS = 1_000_000L

    /** Swings in a shake: three reversals. */
    const val SWINGS = 4
    /** Time constant of the low-pass filter that finds gravity. */
    private const val GRAVITY_TAU_S = 0.4f
    /** Time constant of the low-pass filter that takes out short jolts, such as taps. */
    private const val MOVE_TAU_S = 0.02f
    /** Readings this far apart start the filters again. */
    private const val MAX_SAMPLE_GAP_NS = 200 * MS
    /** Time the filters take to settle after they start. */
    private const val SETTLE_NS = 300 * MS
    /** Part of the threshold below which a swing ends. */
    private const val SWING_END_RATIO = 0.5f
    /** Cosine of the angle within which a swing counts as the opposite or the same direction. */
    private const val REVERSAL_COS = 0.5f
    /** Shortest time between the strongest points of two swings: a shake of 8 per second. */
    private const val MIN_SWING_GAP_NS = 60 * MS
    /** Longest time between the strongest points of two swings. */
    private const val MAX_SWING_GAP_NS = 300 * MS
    /** Longest time from the first to the last swing of a shake. */
    const val SHAKE_NS = 650 * MS
    /** Time after a shake in which no shake counts. */
    private const val COOLDOWN_NS = 1_000 * MS
    /** Time without a swing that ends the shaking after a shake. */
    private const val QUIET_NS = 400 * MS
  }
}
