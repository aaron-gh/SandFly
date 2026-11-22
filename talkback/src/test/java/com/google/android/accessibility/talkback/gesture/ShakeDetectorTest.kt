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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ShakeDetectorTest {
  /** The times in ms that shakes were reported at. */
  private val shakes = mutableListOf<Long>()
  private val detector = ShakeDetector { timeNs -> shakes.add(timeNs / MS) }
  private val random = Random(11)

  /** Movement beyond gravity at a time in ms, along x, y and z. */
  private var movement: (Double) -> DoubleArray = { DoubleArray(3) }

  @Test
  fun shake_runsOnce() {
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 1500, amplitude = 25.0, hz = 4.0)) }
    assertEquals(1, shakes.size)
    // Four swings at 4 Hz: within half a second of the start.
    assertTrue(shakes[0] in 500..1100)
  }

  @Test
  fun shake_at50Hz() {
    feed(until = 3000, periodMs = 20.0) { t ->
      along(1, sine(t, from = 500, to = 1500, amplitude = 25.0, hz = 4.0))
    }
    assertEquals(1, shakes.size)
  }

  @Test
  fun slowShake_threeTimesASecond() {
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 1800, amplitude = 20.0, hz = 3.0)) }
    assertEquals(1, shakes.size)
  }

  @Test
  fun longShake_runsOnce() {
    feed(until = 6000) { t -> along(0, sine(t, from = 500, to = 4500, amplitude = 30.0, hz = 5.0)) }
    assertEquals(1, shakes.size)
  }

  @Test
  fun twoShakesWithAPause_runTwice() {
    feed(until = 6000) { t ->
      along(
        0,
        sine(t, from = 500, to = 1500, amplitude = 25.0, hz = 4.0) +
          sine(t, from = 3500, to = 4500, amplitude = 25.0, hz = 4.0),
      )
    }
    assertEquals(2, shakes.size)
  }

  @Test
  fun shakeJustAfterAShake_runsNothing() {
    // Shakes 0.5 s apart: the second is in the time after the first.
    feed(until = 4000) { t ->
      along(
        0,
        sine(t, from = 500, to = 1200, amplitude = 25.0, hz = 4.0) +
          sine(t, from = 1700, to = 2400, amplitude = 25.0, hz = 4.0),
      )
    }
    assertEquals(1, shakes.size)
  }

  @Test
  fun ellipticalShake_runs() {
    feed(until = 3000) { t ->
      val x = sine(t, from = 500, to = 1500, amplitude = 25.0, hz = 4.0)
      val y = if (t in 500.0..1500.0) 15.0 * cos(2 * PI * 4.0 * (t - 500) / 1000) else 0.0
      doubleArrayOf(x, y, 0.0)
    }
    assertEquals(1, shakes.size)
  }

  @Test
  fun gentleShake_runsOnlyAtHighSensitivity() {
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 1500, amplitude = 12.0, hz = 4.0)) }
    assertEquals(0, shakes.size)

    detector.threshold = ShakeDetector.Sensitivity.HIGH.threshold
    feed(until = 6000) { t ->
      along(0, sine(t, from = 3500, to = 4500, amplitude = 12.0, hz = 4.0))
    }
    assertEquals(1, shakes.size)
  }

  @Test
  fun hardShake_needsLowSensitivity() {
    detector.threshold = ShakeDetector.Sensitivity.LOW.threshold
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 1500, amplitude = 16.0, hz = 4.0)) }
    assertEquals(0, shakes.size)
  }

  @Test
  fun oneSwingBackAndForth_runsNothing() {
    // A single movement out and back: two swings.
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 750, amplitude = 30.0, hz = 4.0)) }
    assertEquals(0, shakes.size)
  }

  @Test
  fun backTaps_runNothing() {
    val taps = listOf(500L, 700L, 900L, 1100L, 1300L)
    feed(until = 3000) { t -> along(2, taps.sumOf { tap(t, at = it, strength = 40.0) }) }
    assertEquals(0, shakes.size)
  }

  @Test
  fun walking_runsNothing() {
    feed(until = 6000) { t ->
      doubleArrayOf(
        sine(t, from = 0, to = 6000, amplitude = 2.0, hz = 1.0),
        sine(t, from = 0, to = 6000, amplitude = 5.0, hz = 2.0),
        sine(t, from = 0, to = 6000, amplitude = 3.0, hz = 2.0),
      )
    }
    assertEquals(0, shakes.size)
  }

  @Test
  fun turningThePhoneOver_runsNothing() {
    // Gravity turns from y to -y in half a second, and back.
    feed(until = 4000) { t ->
      val angle =
        when {
          t < 500 -> 0.0
          t < 1000 -> PI * (t - 500) / 500
          t < 2000 -> PI
          t < 2500 -> PI * (1 - (t - 2000) / 500)
          else -> 0.0
        }
      // Less the gravity that feed() adds along y.
      doubleArrayOf(0.0, G * cos(angle) - G, G * sin(angle))
    }
    assertEquals(0, shakes.size)
  }

  @Test
  fun swingsTooFarApart_runNothing() {
    // One swing a second, each way in turn.
    feed(until = 6000) { t -> along(0, sine(t, from = 500, to = 5500, amplitude = 25.0, hz = 0.5)) }
    assertEquals(0, shakes.size)
  }

  @Test
  fun shakeAtTheStart_waitsForTheFiltersToSettle() {
    feed(until = 3000) { t -> along(0, sine(t, from = 0, to = 300, amplitude = 25.0, hz = 6.0)) }
    assertEquals(0, shakes.size)
  }

  @Test
  fun readingsThatAreNotNumbers_areIgnored() {
    detector.onSample(0, Float.NaN, 0f, 0f)
    feed(until = 3000) { t -> along(0, sine(t, from = 500, to = 1500, amplitude = 25.0, hz = 4.0)) }
    detector.onSample(3_001 * MS, Float.POSITIVE_INFINITY, 0f, 0f)
    assertEquals(1, shakes.size)
  }

  private var nextMs = 0.0

  /** Feeds readings every [periodMs] until [until] ms: gravity along y, [move], and noise. */
  private fun feed(until: Long, periodMs: Double = 2.5, move: (Double) -> DoubleArray) {
    while (nextMs < until) {
      val m = move(nextMs)
      detector.onSample(
        (nextMs * MS).toLong(),
        (m[0] + noise()).toFloat(),
        (G + m[1] + noise()).toFloat(),
        (m[2] + noise()).toFloat(),
      )
      nextMs += periodMs
    }
  }

  private fun along(axis: Int, value: Double): DoubleArray =
    DoubleArray(3).also { it[axis] = value }

  /** A sine of [amplitude] m/s² at [hz] from [from] to [to] ms, else 0. */
  private fun sine(t: Double, from: Long, to: Long, amplitude: Double, hz: Double): Double =
    if (t < from || t >= to) 0.0 else amplitude * sin(2 * PI * hz * (t - from) / 1000)

  /** A tap's ringing jolt at [at] ms: about 10 ms, then dying away. */
  private fun tap(t: Double, at: Long, strength: Double): Double {
    val s = (t - at) / 1000
    if (s < 0 || s > 0.1) return 0.0
    return strength * exp(-s / 0.006) * sin(2 * PI * 120 * s + 0.6)
  }

  private fun noise(): Double = (random.nextDouble() - 0.5) * 0.2

  private companion object {
    const val MS = 1_000_000L
    const val G = 9.81
  }
}
