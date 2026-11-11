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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.TouchInteractionMonitor
import com.google.android.accessibility.utils.AccessibilityEventUtils
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.input.ScrollEventInterpreter
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Scrolls a list for a move to the next or previous item by dragging it with one finger, sent to
 * the app as it is; see [SyntheticTouches]. The content follows the drag at once, and the drag
 * stands still before it lifts, so the list doesn't fling. The next item can then be looked for as
 * soon as the drag ends, where a scroll action makes many lists animate for half a second.
 */
class DragScroll(private val service: AccessibilityService) {

  /** Receives the end of a drag, one frame after it lifts or is cancelled. */
  fun interface Listener {
    /**
     * [completed] is false when the drag was cancelled, such as by a touch. [scrollEvents] counts
     * the scroll events of the list's window during the drag, and [scrollDeltaY] adds up their
     * vertical deltas.
     */
    fun onDragEnded(completed: Boolean, scrollEvents: Int, scrollDeltaY: Int)
  }

  /** Where a drag goes down, from [startY] to [endY], at [x]. */
  data class DragPath(val x: Float, val startY: Float, val endY: Float) {
    /** Whether the drag moves the list's content up, toward the top of the screen. */
    val movesContentUp: Boolean
      get() = endY < startY
  }

  private val handler = Handler(Looper.getMainLooper())
  private var current: Any? = null

  /** Lists that a drag left where they were, which scroll by action from then on. */
  private val failedLists = BoundedMap<AccessibilityNodeInfoCompat, Boolean>(MAX_REMEMBERED_LISTS)

  /** Which way a forward scroll moved the content of lists seen scrolling: true for up. */
  private val forwardMovesUp = BoundedMap<AccessibilityNodeInfoCompat, Boolean>(MAX_REMEMBERED_LISTS)

  /**
   * Returns the path to drag [list] along so that it scrolls as [scrollAction] would, or null when
   * the list should scroll by action instead. [pivot] is the focused item.
   */
  fun plan(
    list: AccessibilityNodeInfoCompat,
    pivot: AccessibilityNodeInfoCompat,
    scrollAction: Int,
  ): DragPath? {
    val reason =
      when {
        !enabled -> "turned off"
        !SyntheticTouches.isAvailable() -> "touches can't be sent"
        isFingerDown() -> "a finger is down"
        failedLists[list] == true -> "an earlier drag left this list where it was"
        pivot.isEditable || Role.getRole(pivot) == Role.ROLE_EDIT_TEXT -> "the focused item is editable"
        isWebContent(list) -> "the list is web content"
        !isVerticalList(list) -> "the list isn't known to scroll up and down only"
        else -> null
      }
    if (reason != null) {
      LogUtils.d(TAG, "Not dragging: %s.", reason)
      return null
    }
    val contentUp =
      contentMovesUp(scrollAction, list.className, forwardMovesUp[list])
        ?: run {
          LogUtils.d(TAG, "Not dragging: which way this list's scroll action moves isn't known.")
          return null
        }
    val listBounds = Rect()
    list.getBoundsInScreen(listBounds)
    val screen = screenSize()
    val blockers = ArrayList<Rect>()
    if (!addWindowsAbove(list, listBounds, screen, blockers) ||
      !addNodesOver(list, listBounds, blockers)
    ) {
      LogUtils.d(TAG, "Not dragging: what covers the list couldn't be read in time.")
      return null
    }
    val density = service.resources.displayMetrics.density
    val path =
      dragPath(
        listBounds,
        screen.x,
        screen.y,
        blockers,
        interactiveParts(pivot),
        contentUp,
        sideMarginPx = (SIDE_MARGIN_DP * density).toInt(),
        topMarginPx = (TOP_MARGIN_DP * density).toInt(),
        bottomMarginPx = (BOTTOM_MARGIN_DP * density).toInt(),
        minDistancePx = (MIN_DISTANCE_DP * density).toInt(),
      )
    if (path == null) {
      LogUtils.d(TAG, "Not dragging: too little of the list %s is uncovered.", listBounds)
    }
    return path
  }

  /**
   * Drags [list] along [path], then calls [listener]. Returns false if the drag couldn't be sent,
   * and then doesn't call [listener].
   */
  fun drag(list: AccessibilityNodeInfoCompat, path: DragPath, listener: Listener): Boolean {
    cancel()
    val token = Any()
    current = token
    val move =
      GestureDescription.StrokeDescription(
        Path().apply {
          moveTo(path.x, path.startY)
          lineTo(path.x, path.endY)
        },
        0,
        MOVE_MS,
        /* willContinue= */ true,
      )
    watchedWindowId = list.windowId
    watchedScrollEvents = 0
    watchedScrollDeltaY = 0
    val start = SystemClock.uptimeMillis()
    watchUntil = start + MOVE_MS + HOLD_MS + START_WAIT_MS
    ScrollEventInterpreter.setOwnTouchScrollUntil(watchUntil + LATE_EVENT_MS)
    val finish = { completed: Boolean ->
      if (current === token) {
        val end = SystemClock.uptimeMillis()
        watchUntil = end + LATE_EVENT_MS
        ScrollEventInterpreter.setOwnTouchScrollUntil(end + LATE_EVENT_MS)
        handler.postDelayed(
          {
            if (current === token) {
              current = null
              LogUtils.d(
                TAG,
                "Drag %s after %d ms, %d scroll events, deltaY=%d.",
                if (completed) "ended" else "cancelled",
                SystemClock.uptimeMillis() - start,
                watchedScrollEvents,
                watchedScrollDeltaY,
              )
              listener.onDragEnded(completed, watchedScrollEvents, watchedScrollDeltaY)
            }
          },
          FRAME_MS,
        )
      }
    }
    val sent =
      SyntheticTouches.dispatch(
        service,
        GestureDescription.Builder().addStroke(move).build(),
        MOVE_MS + HOLD_MS,
      ) { moved ->
        if (current !== token) return@dispatch
        if (!moved) {
          finish(false)
          return@dispatch
        }
        // The finger stays where the drag ended before it lifts, so the list has no speed left
        // to fling with.
        val hold =
          move.continueStroke(
            Path().apply { moveTo(path.x, path.endY) },
            0,
            HOLD_MS,
            /* willContinue= */ false,
          )
        val heldSent =
          SyntheticTouches.dispatch(
            service,
            GestureDescription.Builder().addStroke(hold).build(),
            HOLD_MS,
            startsTouch = false,
          ) { held ->
            finish(held)
          }
        if (!heldSent) finish(false)
      }
    if (!sent) {
      current = null
      watchUntil = 0L
      ScrollEventInterpreter.setOwnTouchScrollUntil(0L)
      return false
    }
    LogUtils.d(
      TAG,
      "Dragging the list from (%.0f, %.0f) to (%.0f, %.0f).",
      path.x,
      path.startY,
      path.x,
      path.endY,
    )
    return true
  }

  /** Stops waiting for the drag in progress, which then calls no listener. */
  fun cancel() {
    current = null
    handler.removeCallbacksAndMessages(null)
  }

  /** Makes [list] scroll by action from now on, as a drag didn't move it. */
  fun markFailed(list: AccessibilityNodeInfoCompat) {
    failedLists[list] = true
  }

  /**
   * Records which way [scrollAction] moved the content of [list] on screen, so that later scrolls
   * of the list can drag the same way.
   */
  fun learn(list: AccessibilityNodeInfoCompat, scrollAction: Int, contentMovedUp: Boolean) {
    val forwardUp =
      when (scrollAction) {
        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD -> contentMovedUp
        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD -> !contentMovedUp
        else -> return
      }
    if (forwardMovesUp[list] != forwardUp) {
      LogUtils.d(TAG, "A forward scroll moves this list's content %s.", if (forwardUp) "up" else "down")
      forwardMovesUp[list] = forwardUp
    }
  }

  private fun screenSize(): Point {
    val size = Point()
    val display =
      service.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
    if (display != null) {
      @Suppress("DEPRECATION") display.getRealSize(size)
    } else {
      val metrics = service.resources.displayMetrics
      size.set(metrics.widthPixels, metrics.heightPixels)
    }
    return size
  }

  /**
   * Adds the bounds of windows in front of the list's window that cover part of the list, such as
   * the keyboard or system bars. Overlays that cover the whole screen, such as a screen curtain,
   * don't take touches and are left out. Returns false if the list's window isn't found.
   */
  private fun addWindowsAbove(
    list: AccessibilityNodeInfoCompat,
    listBounds: Rect,
    screen: Point,
    blockers: MutableList<Rect>,
  ): Boolean {
    val windows = service.windows ?: return false
    val listWindow = windows.firstOrNull { it.id == list.windowId } ?: return false
    val bounds = Rect()
    for (window in windows) {
      if (window.id == listWindow.id || window.layer <= listWindow.layer) continue
      window.getBoundsInScreen(bounds)
      if (!Rect.intersects(bounds, listBounds)) continue
      if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
        bounds.width() >= screen.x * 9 / 10 &&
        bounds.height() >= screen.y * 9 / 10
      ) {
        continue
      }
      blockers.add(Rect(bounds))
    }
    return true
  }

  /**
   * Adds the bounds of items of the list's window that aren't in the list but are drawn over it,
   * such as a message field or a button that floats over a chat. Returns false if the window has
   * too many items to read in time.
   */
  private fun addNodesOver(
    list: AccessibilityNodeInfoCompat,
    listBounds: Rect,
    blockers: MutableList<Rect>,
  ): Boolean {
    val root = AccessibilityNodeInfoUtils.getRoot(list) ?: return false
    val deadline = SystemClock.uptimeMillis() + MAX_READ_MS
    val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
    queue.add(root)
    var read = 0
    val bounds = Rect()
    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      if (node == list) continue
      if (node.isVisibleToUser) {
        node.getBoundsInScreen(bounds)
        if (Rect.intersects(bounds, listBounds) &&
          !bounds.contains(listBounds) &&
          isSomething(node)
        ) {
          blockers.add(Rect(bounds))
        }
      }
      for (i in 0 until node.childCount) {
        if (++read > MAX_WINDOW_NODES || SystemClock.uptimeMillis() > deadline) return false
        node.getChild(i)?.let { queue.add(it) }
      }
    }
    return true
  }

  /** Bounds of the parts of [pivot] that react to touch, which the drag goes around if it can. */
  private fun interactiveParts(pivot: AccessibilityNodeInfoCompat): List<Rect> {
    val parts = ArrayList<Rect>()
    val deadline = SystemClock.uptimeMillis() + MAX_READ_MS
    val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
    queue.add(pivot)
    var read = 0
    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      if (node != pivot && node.isVisibleToUser && reactsToTouch(node)) {
        parts.add(Rect().also { node.getBoundsInScreen(it) })
      }
      for (i in 0 until node.childCount) {
        if (++read > MAX_ITEM_NODES || SystemClock.uptimeMillis() > deadline) return parts
        node.getChild(i)?.let { queue.add(it) }
      }
    }
    return parts
  }

  private fun isWebContent(list: AccessibilityNodeInfoCompat): Boolean =
    Role.getRole(list) == Role.ROLE_WEB_VIEW ||
      WebInterfaceUtils.supportsWebActions(list) ||
      WebInterfaceUtils.isWebContainer(list)

  private class BoundedMap<K, V>(private val max: Int) : LinkedHashMap<K, V>(max, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > max
  }

  companion object {
    private const val TAG = "DragScroll"

    /** How long the drag moves. */
    @VisibleForTesting const val MOVE_MS = 100L

    /**
     * How long the drag stands still before it lifts. Android and Compose take a finger that hasn't
     * moved for 40 ms as stopped, so the list doesn't fling.
     */
    @VisibleForTesting const val HOLD_MS = 60L

    /** How long after the drag is sent its first scroll events count. */
    private const val START_WAIT_MS = 300L

    /** How long after the drag a scroll event of its list may still arrive. */
    private const val LATE_EVENT_MS = 250L

    private const val FRAME_MS = 16L

    /** Distance from the screen's sides, clear of the back gesture. */
    private const val SIDE_MARGIN_DP = 48f

    /** Distance from the screen's top, clear of the status bar and the notification shade. */
    private const val TOP_MARGIN_DP = 56f

    /** Distance from the screen's bottom, clear of the navigation bar and its gestures. */
    private const val BOTTOM_MARGIN_DP = 80f

    /** The shortest drag worth sending. */
    private const val MIN_DISTANCE_DP = 96f

    /** Share of the uncovered part of the list kept between its edges and the drag's ends. */
    @VisibleForTesting const val END_INSET = 0.08f

    private const val MAX_REMEMBERED_LISTS = 8
    private const val MAX_WINDOW_NODES = 500
    private const val MAX_ITEM_NODES = 100
    private const val MAX_READ_MS = 30L

    /** View-based lists whose forward scroll action always moves their content up. */
    private val VIEW_LISTS =
      setOf(
        "androidx.recyclerview.widget.RecyclerView",
        "android.support.v7.widget.RecyclerView",
        "android.widget.ListView",
        "android.widget.GridView",
        "android.widget.ExpandableListView",
        "androidx.core.widget.NestedScrollView",
        "android.support.v4.widget.NestedScrollView",
      )

    @Volatile private var enabled = true

    // Scroll events of the dragged list's window, read and written on the main thread.
    private var watchedWindowId = -1
    private var watchUntil = 0L
    private var watchedScrollEvents = 0
    private var watchedScrollDeltaY = 0

    /** Returns whether a finger is on the screen, as far as SandFly's gesture detection has seen. */
    @JvmStatic
    fun isFingerDown(): Boolean =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && TouchInteractionMonitor.isFingerDown()

    /** Sets whether swipes scroll lists by dragging them. */
    @JvmStatic
    fun setEnabled(on: Boolean) {
      enabled = on
    }

    /** Returns how many scroll events the list's window has sent since the last drag started. */
    @JvmStatic fun scrollEventsOfDrag(): Int = watchedScrollEvents

    /** Returns the vertical deltas of the scroll events counted by [scrollEventsOfDrag], added up. */
    @JvmStatic fun scrollDeltaYOfDrag(): Int = watchedScrollDeltaY

    /** Counts a scroll event of the window being dragged. Called on the main thread. */
    @JvmStatic
    fun onScrollEvent(event: AccessibilityEvent) {
      if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED ||
        event.windowId != watchedWindowId ||
        SystemClock.uptimeMillis() > watchUntil
      ) {
        return
      }
      watchedScrollEvents++
      val delta = AccessibilityEventUtils.getScrollDeltaY(event)
      if (delta != AccessibilityEventUtils.DELTA_UNDEFINED) {
        watchedScrollDeltaY += delta
      }
    }

    /**
     * Returns whether [scrollAction] moves the content of a list up the screen, or null if that
     * isn't known. Scroll down and up say it; forward and back depend on the list: lists made of
     * views always scroll forward by moving their content up, even when their items run up the
     * screen, as in a chat. Compose lists laid out in reverse scroll forward the other way, so they
     * need [learnedForwardUp], which a scroll action of the list showed earlier.
     */
    @JvmStatic
    @VisibleForTesting
    fun contentMovesUp(
      scrollAction: Int,
      className: CharSequence?,
      learnedForwardUp: Boolean?,
    ): Boolean? {
      val forwardUp =
        learnedForwardUp ?: if (className != null && className.toString() in VIEW_LISTS) true else null
      return when (scrollAction) {
        AccessibilityAction.ACTION_SCROLL_DOWN.id -> true
        AccessibilityAction.ACTION_SCROLL_UP.id -> false
        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD -> forwardUp
        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD -> forwardUp?.not()
        else -> null
      }
    }

    /**
     * Returns where to drag a list with [list] bounds to move its content up or down by about the
     * height of its uncovered part, or null if too little of it is uncovered. [blockers] are the
     * bounds of what is in front of the list: those in its upper half shorten it from the top,
     * the others from the bottom. The drag stays [sideMarginPx] from the screen's sides,
     * [topMarginPx] from its top and [bottomMarginPx] from its bottom, where system gestures
     * start. Of the places across the list, the first whose path crosses none of [avoid] is taken.
     */
    @JvmStatic
    @VisibleForTesting
    fun dragPath(
      list: Rect,
      screenWidth: Int,
      screenHeight: Int,
      blockers: List<Rect>,
      avoid: List<Rect>,
      contentUp: Boolean,
      sideMarginPx: Int,
      topMarginPx: Int,
      bottomMarginPx: Int,
      minDistancePx: Int,
    ): DragPath? {
      if (list.isEmpty) return null
      val left = max(list.left + list.width() / 10, sideMarginPx)
      val right = min(list.right - list.width() / 10, screenWidth - sideMarginPx)
      if (right <= left) return null
      var top = max(list.top, topMarginPx)
      var bottom = min(list.bottom, screenHeight - bottomMarginPx)
      val middle = (list.top + list.bottom) / 2
      for (blocker in blockers) {
        if (blocker.isEmpty ||
          blocker.right <= list.left ||
          blocker.left >= list.right ||
          blocker.bottom <= list.top ||
          blocker.top >= list.bottom
        ) {
          continue
        }
        if (blocker.centerY() < middle) {
          top = max(top, blocker.bottom)
        } else {
          bottom = min(bottom, blocker.top)
        }
      }
      if (bottom <= top) return null
      val inset = (bottom - top) * END_INSET
      val upperY = top + inset
      val lowerY = bottom - inset
      if (lowerY - upperY < minDistancePx) return null
      val x = across(left, right, upperY, lowerY, avoid)
      return if (contentUp) DragPath(x, lowerY, upperY) else DragPath(x, upperY, lowerY)
    }

    /** Picks the place between [left] and [right] whose path crosses the fewest of [avoid]. */
    private fun across(left: Int, right: Int, upperY: Float, lowerY: Float, avoid: List<Rect>): Float {
      var best = left + (right - left) * PLACES[0]
      var bestHits = Int.MAX_VALUE
      for (place in PLACES) {
        val x = left + (right - left) * place
        val hits =
          avoid.count {
            !it.isEmpty && x >= it.left && x < it.right && it.top < lowerY && it.bottom > upperY
          }
        if (hits < bestHits) {
          best = x
          bestHits = hits
          if (hits == 0) break
        }
      }
      return best
    }

    /** Places across the list to drag, as shares of its width, in order of preference. */
    private val PLACES = floatArrayOf(0.5f, 0.35f, 0.65f, 0.2f, 0.8f)

    /**
     * Whether [list] scrolls only up and down: it says so with its scroll actions, has one column,
     * is a kind of view that scrolls only up and down, or has items as wide as itself, such as a
     * chat whose layout groups photos in columns.
     */
    @JvmStatic
    fun isVerticalList(list: AccessibilityNodeInfoCompat): Boolean {
      if (!list.isVisibleToUser) return false
      when (Role.getRole(list)) {
        Role.ROLE_PAGER,
        Role.ROLE_SEEK_CONTROL,
        Role.ROLE_HORIZONTAL_SCROLL_VIEW -> return false
      }
      val actions = list.actionList
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_LEFT) ||
        actions.contains(AccessibilityActionCompat.ACTION_SCROLL_RIGHT)
      ) {
        return false
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_UP) ||
        actions.contains(AccessibilityActionCompat.ACTION_SCROLL_DOWN)
      ) {
        return true
      }
      val info = list.collectionInfo
      if (info != null) {
        if (info.rowCount == 1) return false
        if (info.columnCount == 1 && info.rowCount > 1) return true
      } else {
        when (list.className?.toString()) {
          "android.widget.ListView",
          "android.widget.ExpandableListView",
          "androidx.core.widget.NestedScrollView",
          "android.support.v4.widget.NestedScrollView" -> return true
        }
      }
      return hasFullWidthItem(list)
    }

    /** Whether one of the first items of [list] on screen is nearly as wide as the list. */
    private fun hasFullWidthItem(list: AccessibilityNodeInfoCompat): Boolean {
      val listBounds = Rect()
      list.getBoundsInScreen(listBounds)
      if (listBounds.isEmpty) return false
      val bounds = Rect()
      for (i in 0 until min(list.childCount, MAX_ITEMS_CHECKED)) {
        val child = list.getChild(i) ?: continue
        if (!child.isVisibleToUser) continue
        child.getBoundsInScreen(bounds)
        if (isFullWidth(bounds, listBounds)) return true
      }
      return false
    }

    /** Whether an item with [item] bounds is at least nine tenths as wide as a list with [list]. */
    @JvmStatic
    @VisibleForTesting
    fun isFullWidth(item: Rect, list: Rect): Boolean =
      !list.isEmpty && item.width() * 10 >= list.width() * 9

    private const val MAX_ITEMS_CHECKED = 20

    /** Whether [node] is something shown or used, rather than only a container. */
    private fun isSomething(node: AccessibilityNodeInfoCompat): Boolean =
      reactsToTouch(node) ||
        node.isFocusable ||
        !node.text.isNullOrEmpty() ||
        !node.contentDescription.isNullOrEmpty()

    private fun reactsToTouch(node: AccessibilityNodeInfoCompat): Boolean =
      node.isClickable ||
        node.isLongClickable ||
        node.isEditable ||
        AccessibilityNodeInfoUtils.isScrollable(node) ||
        Role.getRole(node) == Role.ROLE_SEEK_CONTROL
  }
}
