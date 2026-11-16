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

package com.google.android.accessibility.brailleime.input

import kotlin.math.hypot

/**
 * Moves the braille keyboard dots toward where the fingers land while typing, so the dots follow
 * hands that shift during a typing session.
 *
 * Each typed cell moves every dot of a hand by part of the mean offset of that hand's touches, then
 * moves each touched dot by part of its remaining offset. A touch counts only when it is clearly
 * closer to its own dot than to any other, so a touch that may have hit the wrong dot never pulls
 * the dots its way.
 */
object DotTracking {
  /** Share of a hand's mean touch offset that moves all the dots of that hand. */
  const val HAND_WEIGHT = 0.2f

  /** Share of a touch's remaining offset that moves its own dot. */
  const val FINGER_WEIGHT = 0.2f

  /** A touch counts when its distance to its dot is at most this share of that to the next dot. */
  const val MAX_DISTANCE_RATIO = 0.6f

  /** Hand of dots 1, 2, 3 and 7. */
  const val FIRST_HAND = 0

  /** Hand of dots 4, 5, 6 and 8. */
  const val SECOND_HAND = 1

  /** Hand that types the given dot number. */
  @JvmStatic
  fun handOf(dotNumber: Int): Int =
    when (dotNumber) {
      1,
      2,
      3,
      7 -> FIRST_HAND
      else -> SECOND_HAND
    }

  /**
   * Returns the dot centres moved toward the touches, as `[xs, ys]`, or null when no touch counts or
   * the move would take a dot off the plane or bring two dots closer than [minSpacing].
   *
   * @param xs x of each dot centre
   * @param ys y of each dot centre
   * @param hands hand of each dot, [FIRST_HAND] or [SECOND_HAND]
   * @param touchedDots index of the dot each touch matched, each dot at most once
   * @param touchXs x of each touch
   * @param touchYs y of each touch
   */
  @JvmStatic
  fun follow(
    xs: FloatArray,
    ys: FloatArray,
    hands: IntArray,
    touchedDots: IntArray,
    touchXs: FloatArray,
    touchYs: FloatArray,
    minSpacing: Float,
    width: Float,
    height: Float,
  ): Array<FloatArray>? {
    val dotCount = xs.size
    if (dotCount < 2 || ys.size != dotCount || hands.size != dotCount) {
      return null
    }
    val counts = BooleanArray(touchedDots.size)
    var anyCounts = false
    for (t in touchedDots.indices) {
      val dot = touchedDots[t]
      if (dot !in 0 until dotCount) {
        continue
      }
      val own = hypot(touchXs[t] - xs[dot], touchYs[t] - ys[dot])
      var nearestOther = Float.MAX_VALUE
      for (other in 0 until dotCount) {
        if (other != dot) {
          nearestOther = minOf(nearestOther, hypot(touchXs[t] - xs[other], touchYs[t] - ys[other]))
        }
      }
      counts[t] = own <= MAX_DISTANCE_RATIO * nearestOther
      anyCounts = anyCounts || counts[t]
    }
    if (!anyCounts) {
      return null
    }

    val newXs = xs.copyOf()
    val newYs = ys.copyOf()
    for (hand in intArrayOf(FIRST_HAND, SECOND_HAND)) {
      var sumX = 0f
      var sumY = 0f
      var touches = 0
      for (t in touchedDots.indices) {
        if (counts[t] && hands[touchedDots[t]] == hand) {
          sumX += touchXs[t] - xs[touchedDots[t]]
          sumY += touchYs[t] - ys[touchedDots[t]]
          touches++
        }
      }
      if (touches == 0) {
        continue
      }
      val shiftX = HAND_WEIGHT * sumX / touches
      val shiftY = HAND_WEIGHT * sumY / touches
      for (dot in 0 until dotCount) {
        if (hands[dot] == hand) {
          newXs[dot] += shiftX
          newYs[dot] += shiftY
        }
      }
    }
    for (t in touchedDots.indices) {
      if (counts[t]) {
        val dot = touchedDots[t]
        newXs[dot] += FINGER_WEIGHT * (touchXs[t] - newXs[dot])
        newYs[dot] += FINGER_WEIGHT * (touchYs[t] - newYs[dot])
      }
    }

    for (dot in 0 until dotCount) {
      if (newXs[dot] !in 0f..width || newYs[dot] !in 0f..height) {
        return null
      }
    }
    for (a in 0 until dotCount) {
      for (b in a + 1 until dotCount) {
        val after = hypot(newXs[a] - newXs[b], newYs[a] - newYs[b])
        // Dots already closer than the minimum, as calibrated, may still move apart.
        if (after < minSpacing && after < hypot(xs[a] - xs[b], ys[a] - ys[b])) {
          return null
        }
      }
    }
    return arrayOf(newXs, newYs)
  }
}
