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
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BackTapDetectorTest {
  /** The tap counts reported, with the time in ms they were reported at. */
  private val runs = mutableListOf<Pair<Int, Long>>()
  private val detector =
    BackTapDetector(
      object : BackTapDetector.Listener {
        override fun onTaps(count: Int) {
          runs.add(count to nowMs)
        }
      }
    )
  private val random = Random(7)
  private var nowMs = 0L
  private var sampleIndex = 0L

  /** Jolts along each axis at each sample, added to gravity and noise. */
  private val jolts = mutableMapOf<Long, FloatArray>()

  @Test
  fun doubleTap_runsAfterTheTimeForAThirdTap() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    still(1500)
    assertEquals(listOf(2), runs.map { it.first })
    // Waits for a third tap that doesn't come.
    assertTrue(runs[0].second >= 700 + 450)
  }

  @Test
  fun doubleTap_withoutTripleTapCommand_runsSooner() {
    detector.waitForTriple = false
    still(500)
    tap(at = 500)
    tap(at = 700)
    still(1500)
    assertEquals(listOf(2), runs.map { it.first })
    assertTrue(runs[0].second < 700 + 300)
  }

  @Test
  fun tripleTap() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    tap(at = 900)
    still(2000)
    assertEquals(listOf(3), runs.map { it.first })
  }

  @Test
  fun fourTaps_runOnlyTheTripleTap() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    tap(at = 900)
    tap(at = 1100)
    still(2500)
    assertEquals(listOf(3), runs.map { it.first })
  }

  @Test
  fun lighterSecondTap_counts() {
    still(500)
    tap(at = 500)
    tap(at = 700, strength = 14f)
    still(1500)
    assertEquals(listOf(2), runs.map { it.first })
  }

  @Test
  fun muchLighterSecondTap_runsNothing() {
    still(500)
    tap(at = 500)
    tap(at = 700, strength = 10f)
    still(1500)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun lightFirstTap_runsNothing() {
    still(500)
    tap(at = 500, strength = 14f)
    tap(at = 700)
    still(1500)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun singleTap_runsNothing() {
    still(500)
    tap(at = 500)
    still(2000)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun tapsTooFarApart_runNothing() {
    still(500)
    tap(at = 500)
    tap(at = 1200)
    still(2500)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun twoDoubleTaps() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    tap(at = 2000)
    tap(at = 2200)
    still(3500)
    assertEquals(listOf(2, 2), runs.map { it.first })
  }

  @Test
  fun weakTaps_countOnlyAtHighSensitivity() {
    still(500)
    tap(at = 500, strength = 14f)
    tap(at = 700, strength = 14f)
    still(1500)
    assertEquals(emptyList<Int>(), runs.map { it.first })

    detector.threshold = BackTapDetector.Sensitivity.HIGH.threshold
    tap(at = 2000, strength = 14f)
    tap(at = 2200, strength = 14f)
    still(3500)
    assertEquals(listOf(2), runs.map { it.first })
  }

  @Test
  fun touchOnTheScreen_dropsTheTaps() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    still(750)
    detector.onTouchStart(nowMs * 1_000_000L)
    still(800)
    detector.onTouchEnd(nowMs * 1_000_000L)
    still(2000)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun tapsWhileTheScreenIsTouched_runNothing() {
    still(500)
    detector.onTouchStart(nowMs * 1_000_000L)
    tap(at = 500)
    tap(at = 700)
    still(1000)
    detector.onTouchEnd(nowMs * 1_000_000L)
    still(2000)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun touchReportedBeforeItsBatchOfReadings_dropsTheTaps() {
    still(500)
    tap(at = 500)
    tap(at = 700)
    // The touch from 750 to 800 ms is reported before the readings around it arrive.
    detector.onTouchStart(750 * MS)
    detector.onTouchEnd(800 * MS)
    still(2000)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun touchReportedBeforeItsBatchOfReadings_leavesLaterTapsAlone() {
    still(200)
    detector.onTouchStart(250 * MS)
    detector.onTouchEnd(300 * MS)
    tap(at = 700)
    tap(at = 900)
    still(2000)
    assertEquals(listOf(2), runs.map { it.first })
  }

  @Test
  fun sidewaysJolts_runNothing() {
    still(500)
    tap(at = 500, axis = 0)
    tap(at = 700, axis = 0)
    still(1500)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun joltsWhileShaking_runNothing() {
    still(500)
    shake(from = 500, to = 1500)
    tap(at = 1300)
    tap(at = 1450)
    still(2500)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  @Test
  fun readingsThatStop_startAgain() {
    still(500)
    tap(at = 500)
    // No readings for a second, as when the sensor pauses.
    nowMs = 1500
    sampleIndex = indexAt(nowMs)
    tap(at = 1800)
    still(3000)
    assertEquals(emptyList<Int>(), runs.map { it.first })
  }

  /** Adds a tap's jolt at [at] ms along [axis] (2 is through the screen). */
  private fun tap(at: Long, strength: Float = 28f, axis: Int = 2) {
    // A short ringing pulse: about 10 ms, then dying away.
    for (i in 0 until 40) {
      val t = i / 400f
      val value = strength * exp(-t / 0.006f) * sin(2 * PI.toFloat() * 120f * t + 0.6f)
      val values = jolts.getOrPut(indexAt(at) + i) { FloatArray(3) }
      values[axis] += value
    }
    still(at + 1)
  }

  /** Adds strong movement in all directions from [from] to [to] ms. */
  private fun shake(from: Long, to: Long) {
    for (index in indexAt(from) until indexAt(to)) {
      val t = index / 400f
      val values = jolts.getOrPut(index) { FloatArray(3) }
      values[0] += 12f * sin(2 * PI.toFloat() * 6f * t)
      values[1] += 9f * sin(2 * PI.toFloat() * 9f * t)
      values[2] += 10f * sin(2 * PI.toFloat() * 13f * t)
    }
  }

  /** Feeds readings at 400 Hz until [untilMs]. */
  private fun still(untilMs: Long) {
    while (sampleIndex < indexAt(untilMs)) {
      val jolt = jolts.remove(sampleIndex)
      nowMs = sampleIndex * 5 / 2
      // Held at an angle, with a little tremor.
      detector.onSample(
        sampleIndex * 2_500_000L,
        0.3f + noise() + (jolt?.get(0) ?: 0f),
        6.9f + noise() + (jolt?.get(1) ?: 0f),
        6.9f + noise() + (jolt?.get(2) ?: 0f),
      )
      sampleIndex++
    }
    nowMs = untilMs
  }

  private companion object {
    const val MS = 1_000_000L
  }

  private fun noise(): Float = (random.nextFloat() - 0.5f) * 0.1f

  /** The index of the reading at [ms]: readings come every 2.5 ms. */
  private fun indexAt(ms: Long): Long = ms * 2 / 5
}
