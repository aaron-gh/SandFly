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

/** The kinds of gesture the statistics group tries by, in the order they are shown. */
enum class StatKind {
  TAP,
  DOUBLE_TAP,
  TRIPLE_TAP,
  QUADRUPLE_TAP,
  TAP_HOLD,
  DOUBLE_TAP_HOLD,
  TRIPLE_TAP_HOLD,
  SWIPE,
  SWIPE_HOLD,
  TWO_PART_SWIPE,
  THREE_PART_SWIPE,
  TAP_SWIPE,
  TWO_STROKE_SWIPE,
  PINCH,
  SPREAD,
  ROTATE,
  SCRUB,
  EDGE_SWIPE,
  ALONG_EDGE,
  EDGE_MENU,
  CORNER,
}

/** One try: its whole time, from the first finger landing to the last lifting, and each touch's. */
class TrySample(
  val kind: StatKind,
  val fingers: Int,
  val wholeMs: Long,
  val touchMs: List<Long>,
  val recognized: Boolean,
)

/** Shortest, median and longest of some durations, in ms. */
data class Durations(val shortestMs: Long, val medianMs: Long, val longestMs: Long) {
  companion object {
    /** The durations of [values], or null if there are none. */
    fun of(values: List<Long>): Durations? {
      if (values.isEmpty()) return null
      val sorted = values.sorted()
      val middle = sorted.size / 2
      val median =
        if (sorted.size % 2 == 1) sorted[middle]
        else Math.round((sorted[middle - 1] + sorted[middle]) / 2.0)
      return Durations(sorted.first(), median, sorted.last())
    }
  }
}

/**
 * The tries of one kind of gesture with one number of fingers. [touches] is set when a try had
 * more than one touch, such as the taps of a double tap.
 */
data class StatSummary(
  val kind: StatKind,
  val fingers: Int,
  val tries: Int,
  val recognized: Int,
  val whole: Durations,
  val touches: Durations?,
)

/**
 * The durations of the trainer's tries while it is open, by kind of gesture and number of fingers.
 * Kept in memory only; past [MAX_SAMPLES] tries the oldest are dropped.
 */
class GestureStatistics {
  private val samples = ArrayDeque<TrySample>()

  val isEmpty: Boolean
    get() = samples.isEmpty()

  fun add(sample: TrySample) {
    if (samples.size >= MAX_SAMPLES) samples.removeFirst()
    samples.addLast(sample)
  }

  /** Adds a try at [exercise] made of [touches], if it is a touch gesture with finished touches. */
  fun add(exercise: Exercise, touches: List<Touch>, recognized: Boolean) {
    sampleOf(exercise, touches, recognized)?.let { add(it) }
  }

  fun summaries(): List<StatSummary> =
    samples
      .groupBy { it.kind to it.fingers }
      .map { (key, tries) ->
        val touchMs = tries.flatMap { it.touchMs }
        StatSummary(
          kind = key.first,
          fingers = key.second,
          tries = tries.size,
          recognized = tries.count { it.recognized },
          whole = Durations.of(tries.map { it.wholeMs })!!,
          touches = if (tries.any { it.touchMs.size > 1 }) Durations.of(touchMs) else null,
        )
      }
      .sortedWith(compareBy({ it.kind.ordinal }, { it.fingers }))

  companion object {
    const val MAX_SAMPLES = 1000
    /** The most touches of a try kept: a try of too many taps keeps its first ones. */
    const val MAX_TOUCHES = 8

    /**
     * The sample of a try at [exercise], or null if the exercise has no touches, such as back taps
     * and shakes, or no touch of the try finished.
     */
    fun sampleOf(exercise: Exercise, touches: List<Touch>, recognized: Boolean): TrySample? {
      val (kind, fingers) = kindOf(exercise.shape) ?: return null
      val usable =
        touches.filter { touch -> touch.fingers.isNotEmpty() && touch.fingers.all { it.upMs >= 0 } }
      if (usable.isEmpty()) return null
      val wholeMs = usable.maxOf { it.upMs } - usable.minOf { it.downMs }
      return TrySample(kind, fingers, wholeMs, usable.take(MAX_TOUCHES).map { it.durationMs }, recognized)
    }

    /** The kind and number of fingers of [shape], or null for gestures that aren't touches. */
    fun kindOf(shape: Shape): Pair<StatKind, Int>? =
      when (shape) {
        is Shape.Taps ->
          when {
            shape.hold && shape.taps >= 3 -> StatKind.TRIPLE_TAP_HOLD
            shape.hold && shape.taps == 2 -> StatKind.DOUBLE_TAP_HOLD
            shape.hold -> StatKind.TAP_HOLD
            shape.taps >= 4 -> StatKind.QUADRUPLE_TAP
            shape.taps == 3 -> StatKind.TRIPLE_TAP
            shape.taps == 2 -> StatKind.DOUBLE_TAP
            else -> StatKind.TAP
          } to shape.fingers
        is Shape.Swipe ->
          when {
            shape.hold -> StatKind.SWIPE_HOLD
            shape.directions.size >= 3 -> StatKind.THREE_PART_SWIPE
            shape.directions.size == 2 -> StatKind.TWO_PART_SWIPE
            else -> StatKind.SWIPE
          } to shape.fingers
        is Shape.TapSwipe -> StatKind.TAP_SWIPE to shape.fingers
        is Shape.TwoStroke -> StatKind.TWO_STROKE_SWIPE to 1
        is Shape.Pinch -> (if (shape.spread) StatKind.SPREAD else StatKind.PINCH) to shape.fingers
        is Shape.Rotate -> StatKind.ROTATE to 2
        Shape.Scrub -> StatKind.SCRUB to 2
        is Shape.EdgeSlide ->
          (if (shape.edge.along) StatKind.ALONG_EDGE else StatKind.EDGE_SWIPE) to 1
        is Shape.EdgeMenu -> StatKind.EDGE_MENU to 1
        is Shape.CornerTaps -> StatKind.CORNER to 1
        is Shape.BackTaps,
        Shape.Shake -> null
      }
  }
}
