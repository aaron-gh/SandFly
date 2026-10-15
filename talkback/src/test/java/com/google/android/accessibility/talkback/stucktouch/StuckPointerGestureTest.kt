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

package com.google.android.accessibility.talkback.stucktouch

import com.google.android.accessibility.talkback.stucktouch.StuckPointerGesture.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replays [StuckPointerGesture.plan] through models of Android's gesture injector
 * (MotionEventInjector) and the input dispatcher's verifier for injected events (InputVerifier).
 */
class StuckPointerGestureTest {

  private companion object {
    /** The highest pointer id MotionEventInjector gives a stroke. */
    const val MAX_ID = 10
  }

  /** The pointers the verifier holds down for device -1, or null for none. */
  private var held: MutableSet<Int>? = null

  /** Events the verifier accepted that start a touch, which a window could receive. */
  private var acceptedDowns = 0

  private fun verifyDown(pointers: List<Int>, actionIndex: Int) {
    val current = held
    if (pointers.size == 1) {
      if (current == null) {
        held = mutableSetOf(pointers[0])
        acceptedDowns++
      }
    } else if (
      current != null &&
        current.size == pointers.size - 1 &&
        pointers[actionIndex] !in current
    ) {
      current.add(pointers[actionIndex])
      acceptedDowns++
    }
  }

  private fun verifyUp(pointers: List<Int>, actionIndex: Int) {
    val current = held ?: return
    if (pointers.size == 1) {
      if (current.size == 1 && pointers[0] in current) {
        held = null
      }
    } else if (current == pointers.toSet()) {
      current.remove(pointers[actionIndex])
    }
  }

  /** Strokes with no movement and distinct key times, as the injector turns them into events. */
  private fun inject(strokes: List<Stroke>) {
    val keys = sortedMapOf<Long, Pair<Int, Boolean>>()
    strokes.forEachIndexed { stroke, (start, end) ->
      assertTrue(end > start)
      assertNull(keys.put(start, stroke to true))
      assertNull(keys.put(end, stroke to false))
    }
    val strokeToId = mutableMapOf<Int, Int>()
    val open = mutableListOf<Int>()
    for ((stroke, starts) in keys.values) {
      if (starts) {
        open.add(stroke)
        // MotionEventInjector.getUnusedPointerId: lowest unused, but never above 10.
        strokeToId[stroke] =
          generateSequence(0) { it + 1 }.first { it !in strokeToId.values || it == MAX_ID }
        val ids = open.map { strokeToId.getValue(it) }
        assertEquals("duplicate pointer ids $ids", ids.size, ids.toSet().size)
        verifyDown(ids, open.size - 1)
      } else {
        val index = open.indexOf(stroke)
        verifyUp(open.map { strokeToId.getValue(it) }, index)
        open.removeAt(index)
        if (open.isEmpty()) {
          strokeToId.clear()
        }
      }
    }
  }

  private fun lift(stuck: Set<Int>) {
    held = stuck.toMutableSet()
    acceptedDowns = 0
    val plan = StuckPointerGesture.plan(stuck)
    assertTrue("no plan for $stuck", plan.isNotEmpty())
    inject(plan)
  }

  @Test
  fun liftsEverySetOfPointerIdsTheInjectorCanReach() {
    for (mask in 1 until (1 shl (MAX_ID + 1))) {
      val stuck = (0..MAX_ID).filter { mask and (1 shl it) != 0 }.toSet()
      lift(stuck)
      assertNull("$stuck left down", held)
    }
  }

  @Test
  fun startsNoTouchWhenTheIdsAreHeldInOrder() {
    lift(setOf(1, 4))
    assertNull(held)
    assertEquals(0, acceptedDowns)
  }

  @Test
  fun leavesNothingDownWhenNothingIsStuck() {
    held = null
    inject(StuckPointerGesture.plan(setOf(1, 4)))
    assertNull(held)
  }

  @Test
  fun hasNoPlanForIdsAGestureCantReach() {
    assertTrue(StuckPointerGesture.plan(emptySet()).isEmpty())
    assertTrue(StuckPointerGesture.plan(setOf(MAX_ID + 1)).isEmpty())
    assertTrue(StuckPointerGesture.plan(setOf(-1)).isEmpty())
  }

  @Test
  fun staysWithinTheStrokeLimit() {
    // Android 8 to 10 allow 10 strokes in a gesture.
    assertTrue(StuckPointerGesture.plan(setOf(10), maxStrokes = 10).isEmpty())
    assertEquals(10, StuckPointerGesture.plan(setOf(9), maxStrokes = 10).size)
  }
}
