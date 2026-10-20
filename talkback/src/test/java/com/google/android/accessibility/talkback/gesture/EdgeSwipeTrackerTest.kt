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

package com.google.android.accessibility.talkback.gesture

import android.view.MotionEvent
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks the stage changes an edge swipe reports, on a 300 by 1000 pixel screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdgeSwipeTrackerTest {
  private val events = mutableListOf<String>()
  private val tracker =
    EdgeSwipeTracker(
      edgeZonePx = 30f,
      stageDistancesPx = floatArrayOf(100f, 400f),
      slopPx = 15f,
      isEdgeInUse = { true },
      listener =
        object : EdgeSwipeTracker.Listener {
          override fun onEdgeSwipeStarted(edge: Edge) {
            events.add("start")
          }

          override fun onEdgeTouched(edge: Edge) {}

          override fun onEdgeSwipeStage(edge: Edge, stage: Int, previousStage: Int) {
            events.add("$previousStage>$stage")
          }

          override fun onEdgeSwipeEnded(edge: Edge, stage: Int) {
            events.add("end $stage")
          }
        },
    )
  private var time = 0L

  private fun touch(action: Int, y: Float) {
    val event = MotionEvent.obtain(0, time, action, 150f, y, 0)
    tracker.onMotionEvent(event, 300, 1000)
    event.recycle()
    time += 10
  }

  /** Slides from the bottom edge through each y in [ys], 10 ms apart, and lifts. */
  private fun slide(vararg ys: Float) {
    touch(MotionEvent.ACTION_DOWN, 995f)
    for (y in ys) {
      touch(MotionEvent.ACTION_MOVE, y)
    }
    touch(MotionEvent.ACTION_UP, ys.last())
  }

  @Test
  fun everyStageChangeIsReported() {
    slide(950f, 880f, 500f, 880f, 500f, 970f)
    assertEquals(listOf("start", "0>1", "1>2", "2>1", "1>2", "2>0", "end 0"), events)
  }

  @Test
  fun liftingAtTheSecondStageEndsThere() {
    slide(950f, 880f, 500f)
    assertEquals(listOf("start", "0>1", "1>2", "end 2"), events)
  }
}
