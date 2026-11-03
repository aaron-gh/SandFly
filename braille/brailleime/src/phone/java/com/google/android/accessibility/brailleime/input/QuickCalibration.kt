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

/**
 * Decides whether a touch is the quick calibration gesture: the fingers of one hand held on the
 * screen, then the fingers of the other hand added, as dots 4-5-6 then 1-2-3. A full cell typed
 * with all fingers at once lands too close together in time to match.
 */
object QuickCalibration {
  /** The first hand's fingers stay down this long, with no finger added, before the second's. */
  const val FIRST_HAND_HOLD_MIN_MS = 400L

  /** The fingers of each hand land within this span of each other. */
  const val HAND_SPREAD_MAX_MS = 400L

  /** All fingers stay down this long, without moving, before the dots are calibrated. */
  const val SETTLE_MS = 250L

  /**
   * Whether fingers that landed at [downTimes], all still down, were put down one hand and then
   * the other, with half of [dotCount] fingers each.
   */
  @JvmStatic
  fun isOneHandThenOther(downTimes: LongArray, dotCount: Int): Boolean {
    if (dotCount < 2 || dotCount % 2 != 0 || downTimes.size != dotCount) {
      return false
    }
    val sorted = downTimes.sorted()
    val half = dotCount / 2
    return sorted[half - 1] - sorted[0] <= HAND_SPREAD_MAX_MS &&
      sorted[dotCount - 1] - sorted[half] <= HAND_SPREAD_MAX_MS &&
      sorted[half] - sorted[half - 1] >= FIRST_HAND_HOLD_MIN_MS
  }

  /** Indices into [downTimes] of the first half of the fingers to land. */
  @JvmStatic
  fun firstHand(downTimes: LongArray): Set<Int> =
    downTimes.indices.sortedBy { downTimes[it] }.take(downTimes.size / 2).toSet()

  /**
   * Whether the [firstHand] fingers are the fingers on one side of the others, given the indices of
   * all the fingers in [sideOrder], their order across the screen from one hand to the other.
   */
  @JvmStatic
  fun isOneSide(sideOrder: List<Int>, firstHand: Set<Int>): Boolean {
    if (sideOrder.size % 2 != 0 || firstHand.size * 2 != sideOrder.size) {
      return false
    }
    val half = sideOrder.size / 2
    return firstHand == sideOrder.take(half).toSet() || firstHand == sideOrder.drop(half).toSet()
  }
}
