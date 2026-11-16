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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DotTrackingTest {
  // Six dots in a row, as in tabletop mode: indexes 0-2 one hand, 3-5 the other.
  private val xs = floatArrayOf(100f, 200f, 300f, 500f, 600f, 700f)
  private val ys = FloatArray(6) { 500f }
  private val hands = intArrayOf(0, 0, 0, 1, 1, 1)

  private fun follow(
    dots: IntArray,
    touchXs: FloatArray,
    touchYs: FloatArray,
    fromXs: FloatArray = xs,
    fromYs: FloatArray = ys,
    minSpacing: Float = 50f,
  ) = DotTracking.follow(fromXs, fromYs, hands, dots, touchXs, touchYs, minSpacing, 1000f, 1000f)

  @Test
  fun touch_movesItsHandAndMostlyItsDot() {
    val moved = follow(intArrayOf(4), floatArrayOf(610f), floatArrayOf(520f))!!
    // The hand moves by 0.2 of the offset, (2, 4).
    assertEquals(502f, moved[0][3], 0.01f)
    assertEquals(504f, moved[1][3], 0.01f)
    assertEquals(702f, moved[0][5], 0.01f)
    // The touched dot then moves 0.2 of what is left.
    assertEquals(603.6f, moved[0][4], 0.01f)
    assertEquals(507.2f, moved[1][4], 0.01f)
    // The other hand stays.
    for (dot in 0..2) {
      assertEquals(xs[dot], moved[0][dot], 0f)
      assertEquals(ys[dot], moved[1][dot], 0f)
    }
  }

  @Test
  fun eachHandMovesByItsOwnTouches() {
    val moved =
      follow(intArrayOf(0, 1, 4), floatArrayOf(90f, 190f, 620f), floatArrayOf(500f, 500f, 500f))!!
    assertEquals(298f, moved[0][2], 0.01f)
    assertEquals(504f, moved[0][3], 0.01f)
  }

  @Test
  fun touchBetweenTwoDots_movesNothing() {
    assertNull(follow(intArrayOf(4), floatArrayOf(645f), floatArrayOf(500f)))
  }

  @Test
  fun touchCloserToAnotherDot_movesNothing() {
    // Matched to dot index 4 because another finger took index 5.
    assertNull(follow(intArrayOf(4), floatArrayOf(690f), floatArrayOf(500f)))
  }

  @Test
  fun onlyClearTouchesCount() {
    val moved =
      follow(intArrayOf(3, 4), floatArrayOf(510f, 645f), floatArrayOf(500f, 500f))!!
    // Only the touch on index 3 counts: the hand moves by 2, index 3 by 0.2 of the remaining 8.
    assertEquals(503.6f, moved[0][3], 0.01f)
    assertEquals(602f, moved[0][4], 0.01f)
  }

  @Test
  fun moveOffThePlane_movesNothing() {
    val edgeXs = floatArrayOf(1f, 200f, 300f, 500f, 600f, 700f)
    assertNull(follow(intArrayOf(0), floatArrayOf(-20f), floatArrayOf(500f), fromXs = edgeXs))
  }

  @Test
  fun dotsComingTooClose_moveNothing() {
    assertNull(follow(intArrayOf(4), floatArrayOf(620f), floatArrayOf(500f), minSpacing = 99f))
  }

  @Test
  fun dotsAlreadyTooClose_mayMoveApart() {
    val closeXs = floatArrayOf(100f, 200f, 300f, 500f, 530f, 700f)
    val moved =
      follow(intArrayOf(4), floatArrayOf(540f), floatArrayOf(500f), fromXs = closeXs)
    assertNotNull(moved)
  }

  @Test
  fun repeatedTouches_bringTheDotToTheFinger() {
    var fromXs = xs
    var fromYs = ys
    repeat(30) {
      val moved = follow(intArrayOf(4), floatArrayOf(630f), floatArrayOf(510f), fromXs, fromYs)!!
      fromXs = moved[0]
      fromYs = moved[1]
    }
    assertEquals(630f, fromXs[4], 0.5f)
    assertEquals(510f, fromYs[4], 0.5f)
  }

  @Test
  fun handOf_splitsDotsByColumn() {
    for (dot in intArrayOf(1, 2, 3, 7)) {
      assertEquals(DotTracking.FIRST_HAND, DotTracking.handOf(dot))
    }
    for (dot in intArrayOf(4, 5, 6, 8)) {
      assertEquals(DotTracking.SECOND_HAND, DotTracking.handOf(dot))
    }
  }
}
