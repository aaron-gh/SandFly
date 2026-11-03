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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCalibrationTest {
  @Test
  fun isOneHandThenOther_firstHandHeldThenSecondHand() {
    assertTrue(
      QuickCalibration.isOneHandThenOther(longArrayOf(1000, 1030, 1060, 1600, 1620, 1650), 6)
    )
  }

  @Test
  fun isOneHandThenOther_ignoresTheOrderOfTheFingers() {
    assertTrue(
      QuickCalibration.isOneHandThenOther(longArrayOf(1620, 1000, 1650, 1060, 1600, 1030), 6)
    )
  }

  @Test
  fun isOneHandThenOther_fullCellTypedTogetherIsTyping() {
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 20, 40, 60, 80, 100), 6))
  }

  @Test
  fun isOneHandThenOther_firstHandHeldTooShortlyIsTyping() {
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 20, 40, 300, 320, 340), 6))
  }

  @Test
  fun isOneHandThenOther_firstHandHeldExactlyTheMinimum() {
    val gap = QuickCalibration.FIRST_HAND_HOLD_MIN_MS
    assertTrue(
      QuickCalibration.isOneHandThenOther(longArrayOf(0, 10, 20, 20 + gap, 30 + gap, 40 + gap), 6)
    )
  }

  @Test
  fun isOneHandThenOther_fingersOfAHandSpreadOutIsTyping() {
    // The first hand's fingers land one at a time over a second.
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 500, 1000, 1500, 1510, 1520), 6))
    // The second hand's fingers land one at a time over a second.
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 10, 20, 500, 1000, 1500), 6))
  }

  @Test
  fun isOneHandThenOther_fourFingersThenFourForEightDots() {
    assertTrue(
      QuickCalibration.isOneHandThenOther(
        longArrayOf(0, 10, 20, 30, 600, 610, 620, 630),
        8,
      )
    )
    // Three and then five isn't one hand and then the other.
    assertFalse(
      QuickCalibration.isOneHandThenOther(
        longArrayOf(0, 10, 20, 600, 610, 620, 630, 640),
        8,
      )
    )
  }

  @Test
  fun isOneHandThenOther_needsAFingerForEachDot() {
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 10, 20, 600, 610), 6))
    assertFalse(QuickCalibration.isOneHandThenOther(longArrayOf(0, 10, 20, 600, 610, 620), 8))
  }

  @Test
  fun firstHand_isTheFingersThatLandedFirst() {
    assertEquals(
      setOf(1, 3, 5),
      QuickCalibration.firstHand(longArrayOf(900, 10, 910, 0, 920, 20)),
    )
  }

  @Test
  fun isOneSide_firstHandOnEitherSide() {
    // Fingers 3, 4 and 5 are across the screen from fingers 0, 1 and 2.
    val sideOrder = listOf(3, 4, 5, 0, 1, 2)
    assertTrue(QuickCalibration.isOneSide(sideOrder, setOf(0, 1, 2)))
    assertTrue(QuickCalibration.isOneSide(sideOrder, setOf(5, 3, 4)))
  }

  @Test
  fun isOneSide_firstHandMixedWithTheOtherIsTyping() {
    assertFalse(QuickCalibration.isOneSide(listOf(3, 4, 5, 0, 1, 2), setOf(0, 1, 3)))
  }

  @Test
  fun isOneSide_needsHalfOfTheFingers() {
    assertFalse(QuickCalibration.isOneSide(listOf(3, 4, 5, 0, 1, 2), setOf(0, 1)))
    assertFalse(QuickCalibration.isOneSide(listOf(3, 4, 5, 0, 1, 2), setOf(0, 1, -1)))
  }
}
