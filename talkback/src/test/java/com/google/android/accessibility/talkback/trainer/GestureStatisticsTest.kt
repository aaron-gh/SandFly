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

import android.accessibilityservice.AccessibilityService
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureStatisticsTest {
  private val swipeRight =
    Exercise(AccessibilityService.GESTURE_SWIPE_RIGHT, Shape.Swipe(1, listOf(Direction.RIGHT)))
  private val twoFingerSwipeUp =
    Exercise(AccessibilityService.GESTURE_2_FINGER_SWIPE_UP, Shape.Swipe(2, listOf(Direction.UP)))
  private val doubleTap = Exercise(AccessibilityService.GESTURE_DOUBLE_TAP, Shape.Taps(1, 2, hold = false))

  /** A touch of [fingers] fingers, all landing at [downMs] and lifting at [upMs]. */
  private fun touch(downMs: Long, upMs: Long, fingers: Int = 1): Touch =
    Touch().apply {
      for (id in 0 until fingers) {
        this.fingers +=
          FingerPath(id).apply {
            add(TouchPoint(downMs, 100f * id, 0f))
            add(TouchPoint(upMs, 100f * id + 500f, 0f))
            this.upMs = upMs
          }
      }
      mostFingersAtOnce = fingers
    }

  private fun sample(kind: StatKind, fingers: Int, wholeMs: Long, recognized: Boolean = true) =
    TrySample(kind, fingers, wholeMs, listOf(wholeMs), recognized)

  @Test
  fun durations_oddCount_medianIsMiddle() {
    assertEquals(Durations(120, 160, 210), Durations.of(listOf(210L, 120L, 160L)))
  }

  @Test
  fun durations_evenCount_medianIsMeanOfMiddleTwo() {
    assertEquals(Durations(100, 151, 300), Durations.of(listOf(300L, 100L, 150L, 152L)))
    assertEquals(Durations(100, 125, 150), Durations.of(listOf(150L, 100L)))
  }

  @Test
  fun durations_empty_isNull() {
    assertNull(Durations.of(emptyList()))
  }

  @Test
  fun summaries_groupByKindAndFingers_inKindOrder() {
    val stats = GestureStatistics()
    stats.add(sample(StatKind.SWIPE, 2, 300))
    stats.add(sample(StatKind.SWIPE, 1, 150))
    stats.add(sample(StatKind.TAP, 1, 80, recognized = false))
    stats.add(sample(StatKind.SWIPE, 1, 200, recognized = false))
    stats.add(sample(StatKind.SWIPE, 1, 170))

    val summaries = stats.summaries()

    assertEquals(
      listOf(StatKind.TAP to 1, StatKind.SWIPE to 1, StatKind.SWIPE to 2),
      summaries.map { it.kind to it.fingers },
    )
    val oneFingerSwipes = summaries[1]
    assertEquals(3, oneFingerSwipes.tries)
    assertEquals(2, oneFingerSwipes.recognized)
    assertEquals(Durations(150, 170, 200), oneFingerSwipes.whole)
    assertEquals(0, summaries[0].recognized)
  }

  @Test
  fun summaries_singleTouchTries_haveNoTouchDurations() {
    val stats = GestureStatistics()
    stats.add(swipeRight, listOf(touch(1000, 1150)), recognized = true)
    assertNull(stats.summaries().single().touches)
  }

  @Test
  fun sampleOf_doubleTap_wholeFromFirstLandingToLastLifting_andEachTouch() {
    val taps = listOf(touch(1000, 1080), touch(1200, 1300))

    val sample = GestureStatistics.sampleOf(doubleTap, taps, recognized = true)!!

    assertEquals(StatKind.DOUBLE_TAP, sample.kind)
    assertEquals(1, sample.fingers)
    assertEquals(300L, sample.wholeMs)
    assertEquals(listOf(80L, 100L), sample.touchMs)

    val stats = GestureStatistics()
    stats.add(sample)
    stats.add(GestureStatistics.sampleOf(doubleTap, listOf(touch(0, 60), touch(150, 220)), true)!!)
    val summary = stats.summaries().single()
    assertEquals(Durations(220, 260, 300), summary.whole)
    assertEquals(Durations(60, 75, 100), summary.touches)
  }

  @Test
  fun sampleOf_multiFingerSwipe_usesTheExercisesFingers() {
    val sample = GestureStatistics.sampleOf(twoFingerSwipeUp, listOf(touch(0, 190, fingers = 2)), true)!!
    assertEquals(StatKind.SWIPE, sample.kind)
    assertEquals(2, sample.fingers)
    assertEquals(190L, sample.wholeMs)
  }

  @Test
  fun sampleOf_unfinishedTouchesAreLeftOut() {
    val open = touch(0, 100).apply { fingers.first().upMs = -1 }
    assertNull(GestureStatistics.sampleOf(swipeRight, listOf(open), true))
    assertEquals(
      100L,
      GestureStatistics.sampleOf(swipeRight, listOf(touch(500, 600), open), true)!!.wholeMs,
    )
  }

  @Test
  fun sampleOf_backTapsAndShake_haveNoSample() {
    val touches = listOf(touch(0, 100))
    assertNull(GestureStatistics.sampleOf(Exercise(TrainerExercises.SHAKE_ID, Shape.Shake), touches, true))
    assertNull(
      GestureStatistics.sampleOf(Exercise(TrainerExercises.BACK_DOUBLE_TAP_ID, Shape.BackTaps(2)), touches, true)
    )
  }

  @Test
  fun kindOf_mapsShapes() {
    assertEquals(StatKind.TAP to 3, GestureStatistics.kindOf(Shape.Taps(3, 1, hold = false)))
    assertEquals(StatKind.QUADRUPLE_TAP to 3, GestureStatistics.kindOf(Shape.Taps(3, 4, hold = false)))
    assertEquals(StatKind.TAP_HOLD to 2, GestureStatistics.kindOf(Shape.Taps(2, 1, hold = true)))
    assertEquals(StatKind.DOUBLE_TAP_HOLD to 1, GestureStatistics.kindOf(Shape.Taps(1, 2, hold = true)))
    assertEquals(StatKind.TRIPLE_TAP_HOLD to 4, GestureStatistics.kindOf(Shape.Taps(4, 3, hold = true)))
    assertEquals(
      StatKind.SWIPE_HOLD to 1,
      GestureStatistics.kindOf(Shape.Swipe(1, listOf(Direction.UP), hold = true)),
    )
    assertEquals(
      StatKind.TWO_PART_SWIPE to 2,
      GestureStatistics.kindOf(Shape.Swipe(2, listOf(Direction.UP, Direction.RIGHT))),
    )
    assertEquals(
      StatKind.THREE_PART_SWIPE to 1,
      GestureStatistics.kindOf(Shape.Swipe(1, listOf(Direction.UP, Direction.RIGHT, Direction.DOWN))),
    )
    assertEquals(StatKind.SPREAD to 3, GestureStatistics.kindOf(Shape.Pinch(3, spread = true)))
    assertEquals(StatKind.EDGE_SWIPE to 1, GestureStatistics.kindOf(Shape.EdgeSlide(Edge.BOTTOM, long = false)))
    assertEquals(
      StatKind.ALONG_EDGE to 1,
      GestureStatistics.kindOf(Shape.EdgeSlide(Edge.ALONG_LEFT_UP, long = false)),
    )
  }

  @Test
  fun samplesAreBounded_oldestDropped() {
    val stats = GestureStatistics()
    repeat(GestureStatistics.MAX_SAMPLES) { stats.add(sample(StatKind.SWIPE, 1, 100)) }
    stats.add(sample(StatKind.SWIPE, 1, 900))
    val summary = stats.summaries().single()
    assertEquals(GestureStatistics.MAX_SAMPLES, summary.tries)
    assertEquals(900L, summary.whole.longestMs)
    assertEquals(100L, summary.whole.shortestMs)
  }

  @Test
  fun sampleOf_keepsAtMostMaxTouches() {
    val taps = (0 until 20).map { touch(it * 200L, it * 200L + 50) }
    val sample = GestureStatistics.sampleOf(doubleTap, taps, recognized = false)!!
    assertEquals(GestureStatistics.MAX_TOUCHES, sample.touchMs.size)
    assertEquals(19 * 200L + 50, sample.wholeMs)
  }
}
