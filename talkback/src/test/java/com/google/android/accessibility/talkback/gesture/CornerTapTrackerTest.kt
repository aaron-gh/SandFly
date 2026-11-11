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
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Corner
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Kind
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the taps a corner tap tracker reports, on a 300 by 1000 pixel screen with corners of 30
 * pixels, taps of up to 100 ms, 250 ms between taps and holds of 400 ms.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CornerTapTrackerTest {
  private var inUse = setOf<Pair<Corner, Kind>>()
  private val tracker =
    CornerTapTracker(
      cornerSizePx = 30f,
      touchSlop = 8f,
      doubleTapSlop = 100f,
      tapTimeoutMs = 100,
      holdTimeoutMs = 400,
      multiTapTimeoutMs = { 250L },
      assigned = { corner, kind -> (corner to kind) in inUse },
    )
  private val anywhere = CornerTapTracker.StartCheck { _, _ -> true }

  private fun event(action: Int, timeMs: Long, x: Float = 10f, y: Float = 10f): MotionEvent =
    MotionEvent.obtain(0, timeMs, action, x, y, 0)

  private fun send(action: Int, timeMs: Long, x: Float = 10f, y: Float = 10f): Result =
    tracker.onMotionEvent(event(action, timeMs, x, y), 300, 1000, anywhere)

  /** Taps once at each time in [downMs], each tap 50 ms long, and returns the last result. */
  private fun taps(vararg downMs: Long): Result {
    var result: Result = Result.Ignored
    for (time in downMs) {
      send(MotionEvent.ACTION_DOWN, time)
      result = send(MotionEvent.ACTION_UP, time + 50)
    }
    return result
  }

  @Test
  fun cornersAreSquaresAtTheCornersOfTheScreen() {
    assertEquals(Corner.TOP_LEFT, tracker.cornerAt(5f, 5f, 300, 1000))
    assertEquals(Corner.TOP_RIGHT, tracker.cornerAt(295f, 20f, 300, 1000))
    assertEquals(Corner.BOTTOM_LEFT, tracker.cornerAt(20f, 990f, 300, 1000))
    assertEquals(Corner.BOTTOM_RIGHT, tracker.cornerAt(290f, 975f, 300, 1000))
    assertNull(tracker.cornerAt(5f, 500f, 300, 1000))
    assertNull(tracker.cornerAt(150f, 5f, 300, 1000))
  }

  @Test
  fun touchInACornerWithoutGesturesIsIgnored() {
    inUse = setOf(Corner.TOP_RIGHT to Kind.TAP)
    assertEquals(Result.Ignored, send(MotionEvent.ACTION_DOWN, 0))
    assertFalse(tracker.isActive)
  }

  @Test
  fun touchThatMayNotStartIsIgnored() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TAP)
    val result =
      tracker.onMotionEvent(event(MotionEvent.ACTION_DOWN, 0), 300, 1000) { _, _ -> false }
    assertEquals(Result.Ignored, result)
  }

  @Test
  fun tapWaitsForASecondTapThenIsTheGesture() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TAP)
    assertEquals(Result.Kept, taps(0))
    assertEquals(300L, tracker.deadline)
    assertEquals(Result.Kept, tracker.onTimeout(299))
    assertEquals(Result.Gesture(Corner.TOP_LEFT, Kind.TAP), tracker.onTimeout(300))
    assertFalse(tracker.isActive)
  }

  @Test
  fun doubleTapWithoutItsGestureIsTwoTaps() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TAP)
    assertEquals(Result.Taps(2), taps(0, 150))
  }

  @Test
  fun doubleTapEndsAtOnceWithoutATripleTap() {
    inUse = setOf(Corner.TOP_LEFT to Kind.DOUBLE_TAP)
    assertEquals(Result.Gesture(Corner.TOP_LEFT, Kind.DOUBLE_TAP), taps(0, 150))
  }

  @Test
  fun doubleTapWaitsForAThirdTapWithATripleTap() {
    inUse = setOf(Corner.TOP_LEFT to Kind.DOUBLE_TAP, Corner.TOP_LEFT to Kind.TRIPLE_TAP)
    assertEquals(Result.Kept, taps(0, 150))
    assertEquals(Result.Gesture(Corner.TOP_LEFT, Kind.DOUBLE_TAP), tracker.onTimeout(450))
  }

  @Test
  fun tripleTapIsItsGesture() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TRIPLE_TAP)
    assertEquals(Result.Gesture(Corner.TOP_LEFT, Kind.TRIPLE_TAP), taps(0, 150, 300))
  }

  @Test
  fun heldSecondTapIsTheDoubleTapAndHold() {
    inUse = setOf(Corner.TOP_LEFT to Kind.DOUBLE_TAP_AND_HOLD)
    taps(0)
    assertEquals(Result.Kept, send(MotionEvent.ACTION_DOWN, 150))
    assertEquals(Result.Kept, tracker.onTimeout(250))
    assertEquals(550L, tracker.deadline)
    assertEquals(Result.Gesture(Corner.TOP_LEFT, Kind.DOUBLE_TAP_AND_HOLD), tracker.onTimeout(550))
  }

  @Test
  fun heldTapWithoutAHoldGestureIsReleased() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TAP)
    send(MotionEvent.ACTION_DOWN, 0)
    val result = tracker.onTimeout(100)
    assertTrue(result is Result.Released)
    assertEquals(1, (result as Result.Released).events.size)
  }

  @Test
  fun movingFingerIsReleasedWithItsEvents() {
    inUse = setOf(Corner.TOP_LEFT to Kind.TAP)
    send(MotionEvent.ACTION_DOWN, 0)
    send(MotionEvent.ACTION_MOVE, 10, x = 12f)
    val result = send(MotionEvent.ACTION_MOVE, 20, x = 30f)
    assertTrue(result is Result.Released)
    assertEquals(
      listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_MOVE),
      (result as Result.Released).events.map { it.actionMasked },
    )
    assertFalse(tracker.isActive)
  }

  @Test
  fun tapAwayFromTheFirstIsReleased() {
    inUse = setOf(Corner.TOP_LEFT to Kind.DOUBLE_TAP)
    taps(0)
    assertTrue(send(MotionEvent.ACTION_DOWN, 150, x = 200f, y = 500f) is Result.Released)
  }
}
