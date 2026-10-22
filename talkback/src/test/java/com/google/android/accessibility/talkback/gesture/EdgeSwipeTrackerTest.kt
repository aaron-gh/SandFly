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
  private val ended = mutableListOf<Pair<Edge, Int>>()
  private var edgesInUse = Edge.entries.toSet()
  private var stagesInUse = listOf(1, 2)
  private val tracker =
    EdgeSwipeTracker(
      edgeZonePx = 30f,
      stageDistancesPx = floatArrayOf(100f, 400f),
      slopPx = 15f,
      edgeStages = { if (it in edgesInUse) stagesInUse else emptyList() },
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
            ended.add(edge to stage)
          }
        },
    )
  private var time = 0L

  private fun touch(action: Int, y: Float, x: Float = 150f) {
    val event = MotionEvent.obtain(0, time, action, x, y, 0)
    tracker.onMotionEvent(event, 300, 1000)
    event.recycle()
    time += 10
  }

  /** Slides from ([x], [y]) through each x in [xs], 10 ms apart, and lifts. */
  private fun slideSideways(x: Float, y: Float, vararg xs: Float) {
    touch(MotionEvent.ACTION_DOWN, y, x)
    for (toX in xs) {
      touch(MotionEvent.ACTION_MOVE, y, toX)
    }
    touch(MotionEvent.ACTION_UP, y, xs.last())
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

  @Test
  fun topEdgeHasTwoStages() {
    touch(MotionEvent.ACTION_DOWN, 5f, 50f)
    touch(MotionEvent.ACTION_MOVE, 50f, 50f)
    touch(MotionEvent.ACTION_MOVE, 450f, 50f)
    touch(MotionEvent.ACTION_UP, 450f, 50f)
    assertEquals(listOf(Edge.TOP_LEFT to 2), ended)
  }

  @Test
  fun sideEdgesAreByHalf() {
    slideSideways(5f, 200f, 50f, 150f)
    slideSideways(5f, 800f, 50f, 450f)
    slideSideways(295f, 200f, 250f, 150f)
    slideSideways(295f, 800f, 250f, -150f)
    assertEquals(
      listOf(Edge.LEFT_TOP to 1, Edge.LEFT_BOTTOM to 2, Edge.RIGHT_TOP to 1, Edge.RIGHT_BOTTOM to 2),
      ended,
    )
  }

  @Test
  fun slidingUpFromTheSideEdgeIsNotAnEdgeSwipe() {
    touch(MotionEvent.ACTION_DOWN, 500f, 5f)
    touch(MotionEvent.ACTION_MOVE, 420f, 8f)
    touch(MotionEvent.ACTION_MOVE, 300f, 10f)
    touch(MotionEvent.ACTION_UP, 300f, 10f)
    assertEquals(listOf<Pair<Edge, Int>>(), ended)
  }

  @Test
  fun cornerIsTheSideEdgeWhenTheBottomEdgeIsNotInUse() {
    edgesInUse = setOf(Edge.LEFT_BOTTOM)
    slideSideways(5f, 995f, 50f, 150f)
    assertEquals(listOf(Edge.LEFT_BOTTOM to 1), ended)
  }

  @Test
  fun edgeWithOneStageHasNoSecondStage() {
    stagesInUse = listOf(1)
    slide(950f, 880f, 500f)
    assertEquals(listOf("start", "0>1", "end 1"), events)
  }

  @Test
  fun edgeWithOnlyTheSecondStageStartsThere() {
    stagesInUse = listOf(2)
    slide(950f, 800f)
    slide(950f, 880f, 500f)
    slide(950f, 880f, 500f, 800f)
    assertEquals(listOf("start", "0>2", "end 2", "start", "0>2", "2>0", "end 0"), events)
  }
}
