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
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
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
  private val touched = mutableListOf<Edge>()
  private val scrolled = mutableListOf<Edge>()
  private var scrollingSlides = emptySet<Edge>()
  private var owned = mutableListOf<Boolean>()
  private var edgesInUse = Edge.entries.filter { !it.along }.toSet()
  private var stagesInUse = listOf(1, 2)
  private var menus = emptyMap<MenuEdge, Int>()
  private val tracker =
    EdgeSwipeTracker(
      edgeZonePx = 30f,
      stageDistancesPx = floatArrayOf(100f, 400f),
      slopPx = 15f,
      edgeStages = { if (it in edgesInUse) stagesInUse else emptyList() },
      edgeScrolls = { it in scrollingSlides },
      listener =
        object : EdgeSwipeTracker.Listener {
          override fun onEdgeSwipeStarted(edge: Edge) {
            events.add("start")
          }

          override fun onEdgeTouched(edge: Edge) {
            touched.add(edge)
          }

          override fun onEdgeSwipeStage(edge: Edge, stage: Int, previousStage: Int) {
            events.add("$previousStage>$stage")
          }

          override fun onEdgeScroll(edge: Edge) {
            events.add("scroll")
            scrolled.add(edge)
          }

          override fun onEdgeSwipeEnded(edge: Edge, stage: Int) {
            events.add("end $stage")
            ended.add(edge to stage)
          }

          override fun onEdgeMenuItem(edge: MenuEdge, item: Int, previousItem: Int) {
            events.add("$edge $previousItem>$item")
          }

          override fun onEdgeMenuEnded(edge: MenuEdge, item: Int) {
            events.add("$edge chose $item")
          }
        },
      menuItemCount = { menus[it] ?: 0 },
    )
  private var time = 0L

  private fun touch(action: Int, y: Float, x: Float = 150f) {
    val event = MotionEvent.obtain(0, time, action, x, y, 0)
    owned.add(tracker.onMotionEvent(event, 300, 1000))
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

  /** Slides from ([x], [y]) through each point in [points], as x and y pairs, and lifts. */
  private fun slideThrough(x: Float, y: Float, vararg points: Float) {
    touch(MotionEvent.ACTION_DOWN, y, x)
    for (i in points.indices step 2) {
      touch(MotionEvent.ACTION_MOVE, points[i + 1], points[i])
    }
    touch(MotionEvent.ACTION_UP, points[points.size - 1], points[points.size - 2])
  }

  @Test
  fun slidesAlongEachEdge() {
    edgesInUse = Edge.entries.filter { it.along }.toSet()
    slideThrough(5f, 500f, 8f, 450f, 10f, 380f)
    slideThrough(5f, 500f, 8f, 550f, 10f, 620f)
    slideThrough(295f, 500f, 292f, 450f, 290f, 380f)
    slideThrough(295f, 500f, 292f, 550f, 290f, 620f)
    slideThrough(150f, 5f, 100f, 8f, 30f, 10f)
    slideThrough(150f, 5f, 200f, 8f, 270f, 10f)
    slideThrough(150f, 995f, 100f, 992f, 30f, 990f)
    slideThrough(150f, 995f, 200f, 992f, 270f, 990f)
    assertEquals(
      listOf(
        Edge.ALONG_LEFT_UP to 1,
        Edge.ALONG_LEFT_DOWN to 1,
        Edge.ALONG_RIGHT_UP to 1,
        Edge.ALONG_RIGHT_DOWN to 1,
        Edge.ALONG_TOP_LEFT to 1,
        Edge.ALONG_TOP_RIGHT to 1,
        Edge.ALONG_BOTTOM_LEFT to 1,
        Edge.ALONG_BOTTOM_RIGHT to 1,
      ),
      ended,
    )
  }

  @Test
  fun touchIsReportedOnlyWhereASlideInMayStart() {
    edgesInUse = setOf(Edge.ALONG_LEFT_UP)
    slideThrough(5f, 500f, 8f, 450f, 10f, 380f)
    edgesInUse = setOf(Edge.ALONG_LEFT_UP, Edge.LEFT_TOP)
    slideThrough(5f, 300f, 8f, 250f, 10f, 180f)
    assertEquals(listOf(Edge.LEFT_TOP), touched)
    assertEquals(listOf(Edge.ALONG_LEFT_UP to 1, Edge.ALONG_LEFT_UP to 1), ended)
  }

  @Test
  fun slideAlongAnEdgeThatScrollsScrollsTheApp() {
    scrollingSlides = setOf(Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN)
    edgesInUse = setOf(Edge.ALONG_LEFT_UP, Edge.ALONG_RIGHT_UP)
    slideThrough(5f, 500f, 7f, 490f, 8f, 470f, 9f, 300f)
    assertEquals(listOf("scroll"), events)
    // The touch belongs to the slide from the move that started the scroll, and to the app after.
    assertEquals(listOf(false, false, true, false, false), owned)
    slideThrough(5f, 500f, 7f, 530f, 9f, 700f)
    // The right edge does not scroll, so its slide up is still its gesture.
    slideThrough(295f, 500f, 293f, 450f, 290f, 380f)
    assertEquals(listOf(Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN), scrolled)
    assertEquals(listOf(Edge.ALONG_RIGHT_UP to 1), ended)
    assertEquals(listOf<Edge>(), touched)
  }

  @Test
  fun slideLeftOrRightAlongTheTopOrBottomScrollsTheApp() {
    scrollingSlides =
      setOf(Edge.ALONG_TOP_LEFT, Edge.ALONG_TOP_RIGHT, Edge.ALONG_BOTTOM_LEFT, Edge.ALONG_BOTTOM_RIGHT)
    slideThrough(150f, 5f, 130f, 7f, 100f, 8f)
    slideThrough(150f, 995f, 170f, 993f, 200f, 992f)
    // From a corner too.
    slideThrough(5f, 995f, 25f, 993f, 60f, 992f)
    assertEquals(
      listOf(Edge.ALONG_TOP_LEFT, Edge.ALONG_BOTTOM_RIGHT, Edge.ALONG_BOTTOM_RIGHT),
      scrolled,
    )
  }

  @Test
  fun slideInFromASideEdgeStillWorksWhenTheEdgeScrolls() {
    scrollingSlides = setOf(Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN)
    slideSideways(5f, 200f, 50f, 150f)
    assertEquals(listOf(Edge.LEFT_TOP to 1), ended)
    assertEquals(listOf<Edge>(), scrolled)
  }

  @Test
  fun cornersDoNotScrollAlongSideEdges() {
    scrollingSlides = setOf(Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN)
    edgesInUse = setOf(Edge.BOTTOM)
    slideThrough(5f, 995f, 8f, 950f, 10f, 880f)
    assertEquals(listOf(Edge.BOTTOM to 1), ended)
    assertEquals(listOf<Edge>(), scrolled)
  }

  @Test
  fun slideAlongAnEdgeHasOneStage() {
    edgesInUse = setOf(Edge.ALONG_LEFT_UP)
    slideThrough(5f, 900f, 8f, 850f, 10f, 750f, 12f, 300f)
    assertEquals(listOf("start", "0>1", "end 1"), events)
  }

  @Test
  fun slideAlongAnEdgeThatLeavesTheEdgeIsNotAnEdgeSwipe() {
    edgesInUse = setOf(Edge.ALONG_LEFT_UP)
    slideThrough(5f, 500f, 60f, 450f, 120f, 380f)
    assertEquals(listOf<Pair<Edge, Int>>(), ended)
  }

  @Test
  fun cornerSlideTakesTheEdgeItsDirectionFits() {
    edgesInUse = setOf(Edge.BOTTOM, Edge.ALONG_LEFT_UP, Edge.ALONG_BOTTOM_RIGHT)
    slideThrough(5f, 995f, 8f, 950f, 10f, 880f)
    slideThrough(5f, 995f, 50f, 993f, 150f, 990f)
    assertEquals(listOf(Edge.BOTTOM to 1, Edge.ALONG_BOTTOM_RIGHT to 1), ended)
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
  @Test
  fun slidingAlongAMenuEdgeGoesThroughItsItems() {
    // Four items of 250 pixels each.
    menus = mapOf(MenuEdge.LEFT to 4)
    slideThrough(5f, 100f, 5f, 120f, 6f, 300f, 4f, 600f, 5f, 520f)
    assertEquals(
      listOf("start", "LEFT -1>0", "LEFT 0>1", "LEFT 1>2", "LEFT chose 2"),
      events,
    )
  }

  @Test
  fun menuGoesUpTheEdgeToo() {
    menus = mapOf(MenuEdge.RIGHT to 2)
    slideThrough(295f, 700f, 295f, 680f, 295f, 300f)
    assertEquals(listOf("start", "RIGHT -1>1", "RIGHT 1>0", "RIGHT chose 0"), events)
  }

  @Test
  fun leavingTheEdgeLeavesTheMenuAndComingBackReturns() {
    menus = mapOf(MenuEdge.LEFT to 4)
    slideThrough(5f, 100f, 5f, 130f, 60f, 130f, 120f, 130f, 20f, 140f, 120f, 150f)
    assertEquals(
      listOf("start", "LEFT -1>0", "LEFT 0>-1", "LEFT -1>0", "LEFT 0>-1", "LEFT chose -1"),
      events,
    )
  }

  @Test
  fun secondFingerCancelsTheMenu() {
    menus = mapOf(MenuEdge.LEFT to 4)
    touch(MotionEvent.ACTION_DOWN, 100f, 5f)
    touch(MotionEvent.ACTION_MOVE, 130f, 5f)
    owned.add(tracker.onMotionEvent(twoFingers(MotionEvent.ACTION_POINTER_DOWN), 300, 1000))
    touch(MotionEvent.ACTION_UP, 400f, 5f)
    assertEquals(listOf("start", "LEFT -1>0", "LEFT chose -1"), events)
    assertEquals(true, owned.last())
  }

  @Test
  fun menuEdgeReplacesSlidesAlongItAndScrolling() {
    edgesInUse = setOf(Edge.ALONG_LEFT_UP, Edge.ALONG_RIGHT_UP)
    scrollingSlides = setOf(Edge.ALONG_LEFT_DOWN)
    menus = mapOf(MenuEdge.LEFT to 2)
    slideThrough(5f, 300f, 5f, 200f)
    slideThrough(5f, 300f, 5f, 400f)
    slideThrough(295f, 600f, 295f, 400f)
    assertEquals(listOf<Edge>(), scrolled)
    assertEquals(listOf(Edge.ALONG_RIGHT_UP to 1), ended)
    assertEquals(
      listOf(
        "start", "LEFT -1>0", "LEFT chose 0",
        "start", "LEFT -1>0", "LEFT chose 0",
        "start", "0>1", "end 1",
      ),
      events,
    )
  }

  @Test
  fun cornerOfAMenuEdgeHasNoSlidesAlongIt() {
    edgesInUse = setOf(Edge.ALONG_LEFT_DOWN)
    menus = mapOf(MenuEdge.LEFT to 2)
    slideThrough(5f, 10f, 5f, 200f)
    assertEquals(listOf<String>(), events)
  }

  @Test
  fun slideInFromAMenuEdgeIsStillAnEdgeGesture() {
    menus = mapOf(MenuEdge.LEFT to 2)
    slideSideways(5f, 200f, 50f, 150f)
    assertEquals(listOf(Edge.LEFT_TOP to 1), ended)
  }

  @Test
  fun restingOnAMenuEdgeIsLeftToTouchExploration() {
    menus = mapOf(MenuEdge.LEFT to 2)
    touch(MotionEvent.ACTION_DOWN, 300f, 5f)
    time += 250
    tracker.rejectIfStalled(time)
    touch(MotionEvent.ACTION_MOVE, 400f, 5f)
    touch(MotionEvent.ACTION_UP, 400f, 5f)
    assertEquals(listOf<String>(), events)
    assertEquals(false, tracker.isActive)
  }

  private fun twoFingers(action: Int): MotionEvent {
    val properties =
      Array(2) { i ->
        MotionEvent.PointerProperties().apply {
          id = i
          toolType = MotionEvent.TOOL_TYPE_FINGER
        }
      }
    val coords =
      Array(2) { i ->
        MotionEvent.PointerCoords().apply {
          x = 5f + i * 100f
          y = 130f
        }
      }
    val event =
      MotionEvent.obtain(
        0,
        time,
        action or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
        2,
        properties,
        coords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0,
      )
    time += 10
    return event
  }
}
