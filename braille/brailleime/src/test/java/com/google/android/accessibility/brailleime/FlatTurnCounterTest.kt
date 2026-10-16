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

package com.google.android.accessibility.brailleime

import org.junit.Assert.assertEquals
import org.junit.Test

class FlatTurnCounterTest {
  /** Feeds headings in order, and returns the quarter turns each one reported. */
  private fun FlatTurnCounter.feed(vararg headings: Float) = headings.map { onHeading(it) }

  @Test
  fun firstHeadingIsTheStart() {
    assertEquals(listOf(0), FlatTurnCounter().feed(170f))
  }

  @Test
  fun nudgesDoNotCount() {
    assertEquals(listOf(0, 0, 0, 0), FlatTurnCounter().feed(0f, 40f, 59f, -50f))
  }

  @Test
  fun aQuarterTurnCountsTwoThirdsOfTheWay() {
    assertEquals(listOf(0, 0, 1), FlatTurnCounter().feed(0f, 50f, 61f))
    assertEquals(listOf(0, -1), FlatTurnCounter().feed(0f, -65f))
  }

  @Test
  fun turnedOnlyOncePerQuarterTurn() {
    assertEquals(listOf(0, 1, 0, 0), FlatTurnCounter().feed(0f, 65f, 90f, 140f))
  }

  @Test
  fun aFullTurnIsFourQuarters() {
    assertEquals(listOf(0, 1, 1, 1, 1), FlatTurnCounter().feed(0f, 90f, 180f, -90f, 0f))
  }

  @Test
  fun turningBackCountsFromTheTurnedDirection() {
    // Turned a quarter, then back to roughly where it started: both count, though the first was
    // only noticed at 65 degrees.
    assertEquals(listOf(0, 1, -1), FlatTurnCounter().feed(0f, 65f, 25f))
  }

  @Test
  fun acrossTheSouthernWrap() {
    // From 170 to -120 degrees is 70 degrees clockwise, through south.
    assertEquals(listOf(0, 1), FlatTurnCounter().feed(170f, -120f))
    assertEquals(listOf(0, -1), FlatTurnCounter().feed(-170f, 120f))
  }

  @Test
  fun reset_takesTheNextHeadingAsTheStart() {
    val counter = FlatTurnCounter()
    counter.feed(0f, 50f)
    counter.reset()
    assertEquals(listOf(0, 0), counter.feed(50f, 100f))
  }

  @Test
  fun angleBetween_isSignedAndWraps() {
    assertEquals(10f, FlatTurnCounter.angleBetween(10f, 0f), 0.001f)
    assertEquals(-10f, FlatTurnCounter.angleBetween(-10f, 0f), 0.001f)
    assertEquals(20f, FlatTurnCounter.angleBetween(-170f, 170f), 0.001f)
    assertEquals(-20f, FlatTurnCounter.angleBetween(170f, -170f), 0.001f)
  }
}
