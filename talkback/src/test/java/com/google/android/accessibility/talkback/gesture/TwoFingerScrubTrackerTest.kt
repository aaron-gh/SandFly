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

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TwoFingerScrubTrackerTest {
  private var scrubs = 0
  private val tracker = TwoFingerScrubTracker(STROKE) { scrubs++ }
  private var time = 0L

  /** Lands two fingers 100 px apart around [x]. */
  private fun land(x: Float) {
    tracker.onMotionEvent(event(MotionEvent.ACTION_DOWN, x, 1))
    tracker.onMotionEvent(event(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), x, 2))
  }

  private fun moveTo(x: Float, afterMillis: Long = 20) {
    time += afterMillis
    tracker.onMotionEvent(event(MotionEvent.ACTION_MOVE, x, 2))
  }

  /** Moves from [from] to [to] in steps of 10 px, [stepMillis] apart. */
  private fun slide(from: Float, to: Float, stepMillis: Long = 10) {
    val step = if (to > from) 10f else -10f
    var x = from
    while ((to - x) * step > 0) {
      x += step
      moveTo(x, stepMillis)
    }
  }

  private fun event(action: Int, x: Float, count: Int): MotionEvent {
    val ids = IntArray(count) { it }
    val coords =
      Array(count) {
        MotionEvent.PointerCoords().apply {
          this.x = x - 50 + it * 100
          y = 500f
        }
      }
    val props = Array(count) { i -> MotionEvent.PointerProperties().apply { id = ids[i] } }
    return MotionEvent.obtain(0, time, action, count, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
  }

  @Test
  fun outAndBack_scrubs() {
    land(500f)
    slide(500f, 650f)
    slide(650f, 500f)
    assertEquals(1, scrubs)
    assertTrue(tracker.isScrubbed)
  }

  @Test
  fun shortWayBack_scrubs() {
    land(500f)
    slide(500f, 620f)
    slide(620f, 550f)
    assertEquals(1, scrubs)
  }

  @Test
  fun slowFirstStrokeThatKeepsMoving_scrubs() {
    land(500f)
    // 100 px out in 300 ms, then 300 more px over 600 ms, still moving out.
    slide(500f, 900f, stepMillis = 25)
    slide(900f, 800f)
    assertEquals(1, scrubs)
  }

  @Test
  fun restBeforeMoving_doesNotCountTowardsTheLimit() {
    land(500f)
    moveTo(505f, afterMillis = 1500)
    slide(505f, 650f)
    slide(650f, 500f)
    assertEquals(1, scrubs)
  }

  @Test
  fun pauseAtTheTurn_isNotAScrub() {
    land(500f)
    slide(500f, 650f)
    moveTo(650f, afterMillis = 600)
    slide(650f, 500f)
    assertEquals(0, scrubs)
  }

  @Test
  fun oneWayOnly_isNotAScrub() {
    land(500f)
    slide(500f, 800f)
    assertEquals(0, scrubs)
    assertTrue(tracker.isTracking)
    tracker.onMotionEvent(event(MotionEvent.ACTION_UP, 800f, 1))
    assertFalse(tracker.isTracking)
  }

  private companion object {
    const val STROKE = 100f
  }
}
