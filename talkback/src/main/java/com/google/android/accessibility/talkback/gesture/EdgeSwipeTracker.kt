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

import android.util.Log
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Recognizes one finger sliding in from an edge of the screen, as on iOS: up from the bottom edge,
 * down from the left or right half of the top edge, or in from the top or bottom half of the left
 * or right edge. Also recognizes one finger sliding along an edge: up or down the left or right
 * edge, and left or right along the top or bottom edge.
 *
 * The touch must start within [edgeZonePx] of the edge and slide inward, or along the edge. Stage 1
 * is [stageDistancesPx]`[0]` from the start and stage 2 is [stageDistancesPx]`[1]`, but a slide has
 * only the stages that [edgeStages] gives it, such as those whose gestures have an action, and a
 * slide along an edge has only stage 1. Once the finger has slid, mostly straight, to the slide's
 * first stage, the slide takes over the touch; until then, the touch is still an ordinary swipe.
 * Each change of stage, forward or back, is reported to the listener as it happens, and the stage
 * the finger is at when it lifts is the gesture. Sliding back to the start, or to before the
 * slide's first stage, and lifting cancels it.
 *
 * A touch at an edge may become any of the slides from there that have stages; each is dropped once
 * the finger goes the wrong way for it. A touch whose slides are all dropped, or that takes too
 * long to reach a first stage, is left to the other gestures and touch exploration, and so is a
 * touch at an edge whose slides have no stages, and one that has not moved at all by
 * [STILL_MILLIS]. Touch exploration waits while a slide may still start ([isPossible]), and
 * [rejectIfStalled] ends that wait when no further motion event comes.
 *
 * A slide along an edge for which [edgeScrolls] is true scrolls the app instead: once it has moved a
 * slop along the edge, the listener passes the rest of the touch to the app. Slides up and down the
 * left and right edges scroll only away from the corners, which belong to the top and bottom edges.
 *
 * A left or right edge for which [menuItemCount] gives items is a menu instead: once a slide up or
 * down it, away from the corners, has moved a slop, the slide owns the touch and goes through the
 * menu. The items divide the edge's length equally, from the top, and the item is the one level with
 * the finger. Moving more than [MENU_ZONE_FACTOR] edge zones away from the edge leaves the menu,
 * where lifting does nothing, and coming back returns to the items. Slides along that edge, and
 * scrolling along it, are not used, including from its corners.
 *
 * A finger that lands partly off the screen is reported at the screen's edge until enough of it is
 * on the screen, so a slide can stay at the edge, drifting sideways, for its first 100 ms or so.
 */
class EdgeSwipeTracker(
  private val edgeZonePx: Float,
  private val stageDistancesPx: FloatArray,
  // Movement, in pixels, after which a slide that is not going inward is rejected.
  private val slopPx: Float,
  // The stages, 1 and 2, that a slide from an edge has, in order. None leaves the edge to other
  // gestures.
  private val edgeStages: (Edge) -> List<Int>,
  // Whether a slide along an edge, such as ALONG_LEFT_UP, scrolls the app instead of being a
  // gesture.
  private val edgeScrolls: (Edge) -> Boolean,
  private val listener: Listener,
  // The number of items in the menu along the left or right edge, or 0 when that edge is no menu.
  // Asked once for each touch that lands at the left or right edge.
  private val menuItemCount: (MenuEdge) -> Int = { 0 },
) {

  /** The side edges, which can be menus. */
  enum class MenuEdge {
    LEFT,
    RIGHT,
  }

  /**
   * The slides: in from an edge, or, when [along] is true, along an edge in the direction the name
   * ends with.
   */
  enum class Edge(val along: Boolean = false) {
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    LEFT_TOP,
    LEFT_BOTTOM,
    RIGHT_TOP,
    RIGHT_BOTTOM,
    ALONG_LEFT_UP(along = true),
    ALONG_LEFT_DOWN(along = true),
    ALONG_RIGHT_UP(along = true),
    ALONG_RIGHT_DOWN(along = true),
    ALONG_TOP_LEFT(along = true),
    ALONG_TOP_RIGHT(along = true),
    ALONG_BOTTOM_LEFT(along = true),
    ALONG_BOTTOM_RIGHT(along = true),
  }

  /** A slide the touch may still become, with its stages. */
  private class Candidate(
    val edge: Edge,
    val stages: List<Int>,
    val scroll: Boolean = false,
    // The menu that this slide along a side edge goes through.
    val menu: MenuEdge? = null,
  )

  /** Receives the stages and the end of a slide. */
  interface Listener {
    /**
     * A finger landed at [edge], where a slide in may start. Not reported when only slides along
     * the edge may start there.
     */
    fun onEdgeTouched(edge: Edge)

    /** The slide took over the touch. No other gesture or touch exploration may use it. */
    fun onEdgeSwipeStarted(edge: Edge)

    /**
     * The slide moved from [previousStage] to [stage]. Stage 0 is back at the edge, where lifting
     * cancels the slide.
     */
    fun onEdgeSwipeStage(edge: Edge, stage: Int, previousStage: Int)

    /**
     * The finger is sliding up or down the left or right edge, which scrolls the app: pass the rest
     * of the touch to the app.
     */
    fun onEdgeScroll(edge: Edge)

    /** The finger lifted at [stage], or at 0 when it was slid back to the edge. */
    fun onEdgeSwipeEnded(edge: Edge, stage: Int)

    /**
     * The finger moved to [item] of the menu along [edge], from [previousItem], counting from 0 at
     * the top. [NO_ITEM] is away from the edge, where lifting does nothing. The first report, when
     * the menu takes over the touch, has [NO_ITEM] as [previousItem].
     */
    fun onEdgeMenuItem(edge: MenuEdge, item: Int, previousItem: Int)

    /**
     * The finger lifted on [item] of the menu along [edge], or the menu ended with [NO_ITEM] away
     * from the edge or when a second finger landed.
     */
    fun onEdgeMenuEnded(edge: MenuEdge, item: Int)
  }

  private enum class State {
    IDLE,
    // The touch started at an edge, and the slide may still start.
    POSSIBLE,
    // The slide owns the touch.
    ACTIVE,
    // The slide was cancelled after it took over; the rest of the touch is ignored.
    CANCELLED,
    // Not an edge slide.
    REJECTED,
    // The slide scrolls the app; the rest of the touch is the app's.
    SCROLLING,
  }

  private var state = State.IDLE
  private var edge = Edge.BOTTOM
  private var pointerId = INVALID_ID
  private var startX = 0f
  private var startY = 0f
  private var startTime = 0L
  // Whether the finger has moved since it landed.
  private var moved = false
  private var stage = 0
  private var stages = emptyList<Int>()
  // The slides the touch may still become, first the one that wins when two reach their first
  // stage together.
  private val candidates = mutableListOf<Candidate>()
  // The menu the slide goes through, while it owns the touch, with its item count and the item the
  // finger is on.
  private var menuEdge: MenuEdge? = null
  private var menuItems = 0
  private var menuItem = NO_ITEM
  private var screenWidth = 0f
  private var screenHeight = 0f

  /** Whether a slide owns the current touch. */
  val isActive: Boolean
    get() = state == State.ACTIVE || state == State.CANCELLED

  /** Whether the touch started at an edge and may still become a slide. */
  val isPossible: Boolean
    get() = state == State.POSSIBLE

  /** Forgets the current touch. */
  fun clear() {
    state = State.IDLE
    pointerId = INVALID_ID
    moved = false
    stage = 0
    stages = emptyList()
    candidates.clear()
    menuEdge = null
    menuItems = 0
    menuItem = NO_ITEM
  }

  /**
   * Processes a motion event. Returns true if the event belongs to an edge slide and must not be
   * handled as anything else.
   *
   * @param screenWidth the width of the screen, in the event's coordinates
   * @param screenHeight the height of the screen, in the event's coordinates
   */
  fun onMotionEvent(event: MotionEvent, screenWidth: Int, screenHeight: Int): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        clear()
        startTracking(event, screenWidth, screenHeight)
      }
      MotionEvent.ACTION_POINTER_DOWN ->
        when (state) {
          State.POSSIBLE -> state = State.REJECTED
          State.ACTIVE -> {
            // A second finger is not part of the slide: cancel it, and keep the rest of the
            // touch from becoming another gesture.
            debugLog("cancelled by a second finger")
            state = State.CANCELLED
            endSlide(lifted = false)
          }
          else -> {}
        }
      MotionEvent.ACTION_MOVE ->
        when (state) {
          State.POSSIBLE -> {
            onPossibleMove(event)
            if (state == State.SCROLLING) {
              return true
            }
          }
          State.ACTIVE -> onActiveMove(event)
          else -> {}
        }
      MotionEvent.ACTION_UP -> {
        val wasActive = state == State.ACTIVE
        val owned = isActive
        if (wasActive) {
          debugLog("ended at stage $stage, menu item $menuItem")
          endSlide(lifted = true)
        }
        clear()
        return owned
      }
      MotionEvent.ACTION_CANCEL -> {
        val owned = isActive
        if (state == State.ACTIVE) {
          endSlide(lifted = false)
        }
        clear()
        return owned
      }
      else -> {}
    }
    return isActive
  }

  /** Reports the end of the active slide: lifted where it is, or cancelled. */
  private fun endSlide(lifted: Boolean) {
    val menu = menuEdge
    if (menu != null) {
      listener.onEdgeMenuEnded(menu, if (lifted) menuItem else NO_ITEM)
    } else {
      listener.onEdgeSwipeEnded(edge, if (lifted) stage else 0)
    }
  }

  private fun startTracking(event: MotionEvent, screenWidth: Int, screenHeight: Int) {
    val x = event.x
    val y = event.y
    val bottom = y >= screenHeight - edgeZonePx
    val top = !bottom && y <= edgeZonePx
    val left = x <= edgeZonePx
    val right = !left && x >= screenWidth - edgeZonePx
    this.screenWidth = screenWidth.toFloat()
    this.screenHeight = screenHeight.toFloat()
    val sideEdge =
      when {
        left -> MenuEdge.LEFT
        right -> MenuEdge.RIGHT
        else -> null
      }
    // A side edge that is a menu has no slides along it, not even from its corners.
    val sideMenuItems = sideEdge?.let(menuItemCount) ?: 0
    // In a corner, the top or bottom edge comes first, then slides in before slides along.
    val edges =
      listOfNotNull(
        when {
          bottom -> Edge.BOTTOM
          top -> if (x < screenWidth / 2f) Edge.TOP_LEFT else Edge.TOP_RIGHT
          else -> null
        },
        when {
          left -> if (y < screenHeight / 2f) Edge.LEFT_TOP else Edge.LEFT_BOTTOM
          right -> if (y < screenHeight / 2f) Edge.RIGHT_TOP else Edge.RIGHT_BOTTOM
          else -> null
        },
      ) +
        when {
          bottom -> listOf(Edge.ALONG_BOTTOM_LEFT, Edge.ALONG_BOTTOM_RIGHT)
          top -> listOf(Edge.ALONG_TOP_LEFT, Edge.ALONG_TOP_RIGHT)
          else -> emptyList()
        } +
        when {
          left -> listOf(Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN)
          right -> listOf(Edge.ALONG_RIGHT_UP, Edge.ALONG_RIGHT_DOWN)
          else -> emptyList()
        }
    for (candidate in edges) {
      if (candidate.along && candidate.alongSideEdge() && sideMenuItems > 0) {
        // Like scrolling, the menu is only away from the corners, which belong to the top and
        // bottom edges.
        if (!top && !bottom) {
          candidates.add(Candidate(candidate, listOf(1), menu = sideEdge))
        }
        continue
      }
      // Slides up and down a side edge scroll only away from the corners, which belong to the top
      // and bottom edges.
      if (
        candidate.along &&
          (!candidate.alongSideEdge() || (!top && !bottom)) &&
          edgeScrolls(candidate)
      ) {
        candidates.add(Candidate(candidate, listOf(1), scroll = true))
        continue
      }
      val lastStage = if (candidate.along) 1 else stageDistancesPx.size
      val candidateStages = edgeStages(candidate).filter { it in 1..lastStage }
      if (candidateStages.isNotEmpty()) {
        candidates.add(Candidate(candidate, candidateStages))
      }
    }
    if (candidates.isEmpty()) {
      state = State.REJECTED
      return
    }
    edge = candidates.first().edge
    menuItems = sideMenuItems
    pointerId = event.getPointerId(0)
    startX = x
    startY = y
    startTime = event.eventTime
    state = State.POSSIBLE
    debugLog("possible: start=(${x.toInt()},${y.toInt()}) screen=${screenWidth}x$screenHeight")
    candidates.firstOrNull { !it.edge.along }?.let { listener.onEdgeTouched(it.edge) }
  }

  /**
   * The time, in [MotionEvent.getEventTime] terms, at which [rejectIfStalled] rejects the touch if
   * it is still possible and has not moved on.
   */
  fun stallTime(): Long = startTime + if (moved) maxStartMillis() else STILL_MILLIS

  /** The time from the touch by which the slide must reach its first stage. */
  private fun maxStartMillis(): Long =
    if (candidates.any { it.stages.first() != 1 }) MAX_LONG_START_MILLIS else MAX_START_MILLIS

  /**
   * Rejects a possible slide that has not moved by [STILL_MILLIS], or not reached the edge's first
   * stage in time, at [now].
   */
  fun rejectIfStalled(now: Long) {
    if (state != State.POSSIBLE) {
      return
    }
    if (now - startTime > maxStartMillis()) {
      debugLog("rejected, too slow")
      state = State.REJECTED
    } else if (now - startTime >= STILL_MILLIS && !moved) {
      debugLog("rejected, not moving after ${now - startTime} ms")
      state = State.REJECTED
    }
  }

  /**
   * How far the finger has moved from the start in the direction of [slide]: inward for a slide in
   * from an edge, or along the edge. Negative is the opposite way.
   */
  private fun forward(slide: Edge, x: Float, y: Float): Float =
    when (slide) {
      Edge.BOTTOM,
      Edge.ALONG_LEFT_UP,
      Edge.ALONG_RIGHT_UP -> startY - y
      Edge.TOP_LEFT,
      Edge.TOP_RIGHT,
      Edge.ALONG_LEFT_DOWN,
      Edge.ALONG_RIGHT_DOWN -> y - startY
      Edge.LEFT_TOP,
      Edge.LEFT_BOTTOM,
      Edge.ALONG_TOP_RIGHT,
      Edge.ALONG_BOTTOM_RIGHT -> x - startX
      Edge.RIGHT_TOP,
      Edge.RIGHT_BOTTOM,
      Edge.ALONG_TOP_LEFT,
      Edge.ALONG_BOTTOM_LEFT -> startX - x
    }

  /** How far the finger has moved across the direction of [slide]. */
  private fun sideways(slide: Edge, x: Float, y: Float): Float =
    when (slide) {
      Edge.BOTTOM,
      Edge.TOP_LEFT,
      Edge.TOP_RIGHT,
      Edge.ALONG_LEFT_UP,
      Edge.ALONG_LEFT_DOWN,
      Edge.ALONG_RIGHT_UP,
      Edge.ALONG_RIGHT_DOWN -> abs(x - startX)
      Edge.LEFT_TOP,
      Edge.LEFT_BOTTOM,
      Edge.RIGHT_TOP,
      Edge.RIGHT_BOTTOM,
      Edge.ALONG_TOP_LEFT,
      Edge.ALONG_TOP_RIGHT,
      Edge.ALONG_BOTTOM_LEFT,
      Edge.ALONG_BOTTOM_RIGHT -> abs(y - startY)
    }

  private fun onPossibleMove(event: MotionEvent) {
    val index = event.findPointerIndex(pointerId)
    if (event.pointerCount != 1 || index < 0) {
      state = State.REJECTED
      return
    }
    val x = event.getX(index)
    val y = event.getY(index)
    if (hypot(x - startX, y - startY) >= slopPx / STILL_SLOP_DIVISOR) {
      moved = true
    }
    var started: Candidate? = null
    val iterator = candidates.iterator()
    while (iterator.hasNext()) {
      val candidate = iterator.next()
      val forward = forward(candidate.edge, x, y)
      val sideways = sideways(candidate.edge, x, y)
      val maxRatio = if (candidate.edge.along) MAX_ALONG_SIDEWAYS_RATIO else MAX_SIDEWAYS_RATIO
      if (
        forward < -slopPx ||
          (sideways > slopPx * SIDEWAYS_SLOP_FACTOR && sideways > forward * maxRatio)
      ) {
        debugLog(
          "${candidate.edge.name} rejected, wrong direction:" +
            " start=(${startX.toInt()},${startY.toInt()})" +
            " forward=${forward.toInt()} sideways=${sideways.toInt()} slop=${slopPx.toInt()}"
        )
        iterator.remove()
        continue
      }
      val firstStage =
        if (candidate.scroll || candidate.menu != null) {
          slopPx
        } else {
          stageDistancesPx[candidate.stages.first() - 1]
        }
      if (started == null && forward >= firstStage) {
        started = candidate
      }
    }
    if (candidates.isEmpty()) {
      state = State.REJECTED
      return
    }
    rejectIfStalled(event.eventTime)
    if (state != State.POSSIBLE || started == null) {
      return
    }
    if (started.scroll) {
      edge = started.edge
      debugLog("scrolling the app")
      state = State.SCROLLING
      listener.onEdgeScroll(edge)
      return
    }
    edge = started.edge
    stages = started.stages
    menuEdge = started.menu
    debugLog(if (menuEdge != null) "started the menu" else "started")
    state = State.ACTIVE
    listener.onEdgeSwipeStarted(edge)
    if (menuEdge != null) {
      updateMenuItem(x, y)
    } else {
      updateStage(forward(edge, x, y))
    }
  }

  private fun onActiveMove(event: MotionEvent) {
    val index = event.findPointerIndex(pointerId)
    if (index < 0) {
      return
    }
    if (menuEdge != null) {
      updateMenuItem(event.getX(index), event.getY(index))
    } else {
      updateStage(forward(edge, event.getX(index), event.getY(index)))
    }
  }

  /**
   * Returns the menu item level with ([x], [y]): the edge's length divided equally among the items,
   * from the top. [NO_ITEM] once the finger is more than [MENU_ZONE_FACTOR] edge zones from the
   * edge.
   */
  private fun menuItemAt(x: Float, y: Float): Int {
    val fromEdge = if (menuEdge == MenuEdge.LEFT) x else screenWidth - x
    if (fromEdge > edgeZonePx * MENU_ZONE_FACTOR || screenHeight <= 0f) {
      return NO_ITEM
    }
    return (y * menuItems / screenHeight).toInt().coerceIn(0, menuItems - 1)
  }

  private fun updateMenuItem(x: Float, y: Float) {
    val menu = menuEdge ?: return
    val item = menuItemAt(x, y)
    if (item == menuItem) {
      return
    }
    val previous = menuItem
    menuItem = item
    listener.onEdgeMenuItem(menu, item, previous)
  }

  /** Sets how far a slide goes in to its [first] stage, and on from there to its [second]. */
  fun setStageDistances(first: Float, second: Float) {
    stageDistancesPx[0] = first
    stageDistancesPx[1] = first + second
  }

  private fun updateStage(forward: Float) {
    var reached = 0
    for (s in stages) {
      if (forward >= stageDistancesPx[s - 1]) reached = s
    }
    if (reached == stage) {
      return
    }
    val previous = stage
    stage = reached
    listener.onEdgeSwipeStage(edge, stage, previous)
  }

  private fun debugLog(message: String) {
    if (GestureLog.enabled) {
      Log.d(DEBUG_TAG, "Edge swipe ${edge.name}: $message")
    }
  }

  companion object {
    private const val INVALID_ID = -1

    /** No menu item: the finger is away from the edge. */
    const val NO_ITEM = -1

    /**
     * How many edge zones from its edge the finger may move and stay in a menu. Further in, lifting
     * does nothing.
     */
    private const val MENU_ZONE_FACTOR = 3f

    private fun Edge.alongSideEdge(): Boolean =
      this == Edge.ALONG_LEFT_UP ||
        this == Edge.ALONG_LEFT_DOWN ||
        this == Edge.ALONG_RIGHT_UP ||
        this == Edge.ALONG_RIGHT_DOWN

    /**
     * Time from the touch to the first stage, in milliseconds. A slower slide is left to touch
     * exploration.
     */
    private const val MAX_START_MILLIS = 500L

    /** Time from the touch to stage 2, in milliseconds, for an edge whose first stage it is. */
    private const val MAX_LONG_START_MILLIS = 1000L

    /**
     * Time, in milliseconds, by which a touch at an edge must have moved. A touch that stays put
     * longer is touch exploration.
     */
    private const val STILL_MILLIS = 200L

    /** A finger has moved once it is a third of the slop from where it landed. */
    private const val STILL_SLOP_DIVISOR = 3f

    /**
     * How far a slide may move sideways, in slops, before its direction counts. While the finger is
     * still reported at the edge, it can only move sideways.
     */
    private const val SIDEWAYS_SLOP_FACTOR = 3f

    /** How far a slide may move sideways for each pixel inward before the first stage. */
    private const val MAX_SIDEWAYS_RATIO = 1f

    /**
     * How far a slide along an edge may move away from it for each pixel along it before the first
     * stage.
     */
    private const val MAX_ALONG_SIDEWAYS_RATIO = 0.5f

    private const val DEBUG_TAG = "BacktalkGesture"
  }
}
