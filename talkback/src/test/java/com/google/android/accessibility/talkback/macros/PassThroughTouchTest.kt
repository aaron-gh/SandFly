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

package com.google.android.accessibility.talkback.macros

import com.google.android.accessibility.talkback.macros.PassThroughTouch.NUDGE_PX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PassThroughTouchTest {
  private val maxX = 719f

  /** The parts as dispatched, each after the one before. */
  private fun dispatched(parts: List<TouchSegment>): List<TouchSegment> {
    var previous: TouchSegment? = null
    return parts.map { part ->
      PassThroughTouch.dispatchedPart(part, previous, maxX).also { previous = it }
    }
  }

  private fun assertContinuous(parts: List<TouchSegment>) {
    parts.zipWithNext { a, b -> assertEquals(a.toX to a.toY, b.fromX to b.fromY) }
  }

  @Test
  fun tapIsDispatchedAsItIs() {
    val tap = TouchSegment(100.5f, 200f, 100.5f, 200f, 100, false)
    assertEquals(listOf(tap), dispatched(listOf(tap)))
  }

  @Test
  fun heldPartsMoveAsideAndBackSoThatEachSendsEvents() {
    val part = TouchSegment(100.5f, 200f, 100.5f, 200f, PassThroughTouch.HOLD_STEP_MS, true)
    val held = dispatched(List(4) { part })
    // A continued part that doesn't move sends Android no event, so every part moves.
    assertTrue(held.all { it.moves && it.willContinue })
    assertEquals(
      listOf(100.5f to 100.5f + NUDGE_PX, 100.5f + NUDGE_PX to 100.5f).let { it + it },
      held.map { it.fromX to it.toX },
    )
    assertTrue(held.all { it.fromY == 200f && it.toY == 200f })
    assertContinuous(held)
    // A stop lifts the touch where it is, without moving.
    val lift = dispatched(held + held.last().lift()).last()
    assertEquals(TouchSegment(100.5f, 200f, 100.5f, 200f, TouchSegment.LIFT_MS, false), lift)
    val midLift = PassThroughTouch.dispatchedPart(held[0].lift(), held[0], maxX)
    assertEquals(100.5f + NUDGE_PX to 200f, midLift.fromX to midLift.fromY)
    assertTrue(!midLift.moves)
  }

  @Test
  fun heldPartAtTheRightEdgeMovesLeft() {
    val part = TouchSegment(maxX, 10f, maxX, 10f, PassThroughTouch.HOLD_STEP_MS, true)
    val first = PassThroughTouch.dispatchedPart(part, null, maxX)
    assertEquals(maxX - NUDGE_PX, first.toX)
    assertEquals(maxX, PassThroughTouch.dispatchedPart(part, first, maxX).toX)
  }

  @Test
  fun macroHoldThenSlideStartsEachPartWhereTheLastEnded() {
    val parts = dispatched(TouchSegment.plan(10f, 300f, 10f, 0f, 2_500))
    assertContinuous(parts)
    assertEquals(listOf(1_000L, 1_000L, 500L, TouchSegment.SLIDE_MS), parts.map { it.durationMs })
    assertTrue(parts.dropLast(1).all { it.willContinue && it.moves })
    val slide = parts.last()
    assertEquals(TouchSegment(10f + NUDGE_PX, 300f, 10f, 0f, TouchSegment.SLIDE_MS, false), slide)
  }

  @Test
  fun macroHoldLiftsWhereItIs() {
    val parts = dispatched(TouchSegment.plan(10f, 20f, 10f, 20f, 2_500))
    assertContinuous(parts)
    assertTrue(parts.dropLast(1).all { it.moves })
    assertEquals(TouchSegment(10f, 20f, 10f, 20f, 500, false), parts.last())
  }
}
