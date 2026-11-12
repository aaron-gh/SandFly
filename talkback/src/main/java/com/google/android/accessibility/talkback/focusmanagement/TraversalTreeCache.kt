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

package com.google.android.accessibility.talkback.focusmanagement

import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Keeps the reading order of the window that the user last swiped in, so that swiping through a
 * screen that has not changed does not ask the app for every node again. Any event that can change
 * that window throws the order away, the same way the framework's own node cache is kept current.
 *
 * Filter logcat by the "BacktalkTreeCache" tag in debug builds to see how often the order is reused.
 */
object TraversalTreeCache {
  private const val TAG = "BacktalkTreeCache"

  /** Events that never change the nodes or their order. */
  internal const val IGNORED_EVENT_TYPES =
    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED or
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED or
      AccessibilityEvent.TYPE_VIEW_HOVER_ENTER or
      AccessibilityEvent.TYPE_VIEW_HOVER_EXIT or
      AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START or
      AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END or
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_END or
      AccessibilityEvent.TYPE_GESTURE_DETECTION_START or
      AccessibilityEvent.TYPE_GESTURE_DETECTION_END or
      AccessibilityEvent.TYPE_ANNOUNCEMENT or
      AccessibilityEvent.TYPE_SPEECH_STATE_CHANGE or
      AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY or
      AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT or
      AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED

  /**
   * Content changes that leave the nodes and their order as they are, such as a progress bar or a
   * clock that ticks. An app that shows one sends these events all the time, which would otherwise
   * throw the order away before every swipe.
   */
  private const val NON_STRUCTURAL_CHANGES =
    AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT or
      AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION

  /**
   * How long the order is kept after one of those changes. A text change can, rarely, make a node
   * speak that did not before, so the order is not trusted for long after one.
   */
  private const val MAX_AGE_AFTER_TEXT_CHANGE_MS = 1500L

  /** Node actions that only move focus, and so do not change the nodes or their order. */
  internal val FOCUS_ACTIONS =
    setOf(
      AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
      AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
      AccessibilityNodeInfoCompat.ACTION_FOCUS,
      AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS,
    )

  /**
   * Showing a node on screen only scrolls when the node is near the edge, which on a round watch
   * screen is nearly every swipe, and a scroll sends its own event, which throws the order away if
   * it arrives. Throwing it away for the action as well rebuilt the order on every swipe there.
   */
  private val SHOW_ON_SCREEN = AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN.id

  private const val NO_WINDOW_ID = -1

  private var root: AccessibilityNodeInfoCompat? = null
  private var strategy: OrderedTraversalStrategy? = null
  private var lastClearReason = "start"
  private var firstIgnoredChangeTime = 0L
  private var hits = 0
  private var misses = 0

  /** The window whose incomplete order was last read again, until an event changes that window. */
  private var rereadWindowId = NO_WINDOW_ID

  /**
   * The root of the window whose order was last built, if every node in that order that set its
   * own place in it moved next to a sibling. Kept when the order is thrown away, until the window
   * shows another screen, so that navigation there can work out the next item around the focused
   * node; see [LocalTraversalOrder].
   */
  private var localRoot: AccessibilityNodeInfoCompat? = null

  /**
   * The nodes last read around a focused node while no order was saved, see [LocalTraversalOrder].
   * They are held as the saved order's nodes are, until an event changes their window; see
   * [holds].
   */
  private var localTree: LocalTraversalOrder.Tree? = null
  private var localTreeWindowId = NO_WINDOW_ID

  /** Whether a text or state in the window of [localTree] has changed since it was read. */
  private var localTreeChanged = false

  /** Where the last navigation started, so a removed node does not send the user to the top. */
  private var lastPivotTop = Int.MIN_VALUE
  private var lastPivotWindowId = NO_WINDOW_ID

  /**
   * Set when scrolling ahead moved the focused item off screen: its bounds and its list's before
   * the page, the sum of scroll deltas then (see
   * [FocusProcessorForLogicalNavigation.scrollDeltaSumY]) and which way the page went. The next
   * navigation goes on from the item next to it.
   */
  private var scrolledAwayBounds: Rect? = null
  private val scrolledAwayListBounds = Rect()
  private var scrolledAwayDeltaSum = 0
  private var scrolledAwayForward = true

  /** Returns the saved order of [root] if it has one and it contains [pivot]. */
  @JvmStatic
  fun get(
    root: AccessibilityNodeInfoCompat,
    pivot: AccessibilityNodeInfoCompat,
  ): OrderedTraversalStrategy? {
    rememberPivot(pivot)
    if (
      firstIgnoredChangeTime != 0L &&
        SystemClock.uptimeMillis() - firstIgnoredChangeTime > MAX_AGE_AFTER_TEXT_CHANGE_MS
    ) {
      clear("text changes")
    }
    val saved = strategy
    val result = saved?.takeIf { root == this.root && it.containsNode(pivot) }
    if (LogUtils.shouldLog(Log.DEBUG)) {
      if (result != null) {
        hits++
        Log.d(TAG, "Reused order ($hits reused, $misses built)")
      } else {
        misses++
        val reason =
          when {
            saved == null -> "cleared by $lastClearReason"
            root != this.root -> "different window"
            else -> "focused node not in order"
          }
        Log.d(TAG, "Building order, $reason ($hits reused, $misses built)")
      }
    }
    return result
  }

  /**
   * Whether the saved order holds [node]. Nothing that could remove a node from the window has
   * happened since the order was saved, so the node is still there.
   */
  @JvmStatic
  fun holds(node: AccessibilityNodeInfoCompat): Boolean =
    strategy?.containsNode(node) == true || localTree?.holds(node) == true

  /**
   * Whether the saved order holds [node] as the app has it now: nothing in its window has changed
   * since the order was read from the app, not even a text or state, so the node needs no second
   * read.
   */
  @JvmStatic
  fun holdsCurrent(node: AccessibilityNodeInfoCompat): Boolean =
    (firstIgnoredChangeTime == 0L && strategy?.containsNode(node) == true) ||
      (!localTreeChanged && localTree?.holds(node) == true)

  /**
   * The root of the window that [node] is in, if the saved order holds [node]: the root the order
   * was read from, as nothing that could change the window has happened since. Asking the window
   * for its root instead always waits for the app, because Android never answers that from its
   * node cache.
   */
  @JvmStatic
  fun rootFor(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    root?.takeIf { it.windowId == node.windowId && strategy?.containsNode(node) == true }

  /**
   * Remembers where a navigation started. When the focused node is later removed — an app
   * re-laying-out its content, such as a blocked ad slot collapsing, does this — navigation carries
   * on from the node nearest this place instead of falling back to the top of the window.
   */
  @JvmStatic
  fun rememberPivot(pivot: AccessibilityNodeInfoCompat) {
    val bounds = Rect()
    pivot.getBoundsInScreen(bounds)
    lastPivotTop = bounds.top
    lastPivotWindowId = pivot.windowId
    scrolledAwayBounds = null
  }

  /**
   * Records that scrolling ahead moved the focused node of window [windowId] off screen. It was at
   * [bounds] in a list at [listBounds] when the sum of scroll deltas was [deltaSum], and the page
   * went [forward] or back.
   */
  @JvmStatic
  fun rememberScrolledAway(
    windowId: Int,
    bounds: Rect,
    listBounds: Rect,
    deltaSum: Int,
    forward: Boolean,
  ) {
    lastPivotWindowId = windowId
    scrolledAwayBounds = Rect(bounds)
    scrolledAwayListBounds.set(listBounds)
    scrolledAwayDeltaSum = deltaSum
    scrolledAwayForward = forward
  }

  /** Forgets the item that scrolling ahead moved off screen, once something else has focus. */
  @JvmStatic
  fun forgetScrolledAway() {
    scrolledAwayBounds = null
  }

  /**
   * Whether scrolling ahead moved the focused node of window [windowId] off screen and nothing has
   * had focus since: the next navigation goes on from it, so focus isn't put elsewhere meanwhile.
   */
  @JvmStatic
  fun continuesAfterScrolledAway(windowId: Int): Boolean =
    scrolledAwayBounds != null && windowId == lastPivotWindowId

  /**
   * Where the next item comes after a node at [top] to [bottom] has scrolled [shift] px up off
   * screen ([shift] is negative for a scroll down): the top of the item after it, going [forward],
   * or the bottom of the item before it.
   */
  @JvmStatic
  fun placeAfterScroll(top: Int, bottom: Int, shift: Int, forward: Boolean): Int =
    if (forward) bottom - shift else top - shift

  /**
   * How far an item at [item] in a list at [list] scrolled off it, given [shift] from the list's
   * scroll deltas: at least far enough to leave the list, as the deltas of some lists, such as
   * Compose lazy lists, aren't in pixels.
   */
  @JvmStatic
  fun shiftOffList(shift: Int, item: Rect, list: Rect, forward: Boolean): Int =
    if (list.isEmpty) {
      shift
    } else if (forward) {
      maxOf(shift, item.bottom - list.top)
    } else {
      minOf(shift, item.top - list.bottom)
    }

  /**
   * The node to search on from when the focused node has been removed: the last node above the
   * place the last navigation started, so a swipe forward lands on whatever now occupies that
   * place, rather than on the top of the page. Falls back to the first node when the removal was
   * above everything. Null when nothing was remembered, or the window has changed.
   *
   * The content change that removes a node usually throws the saved order away as well — an ad slot
   * collapsing sends a content change of the undefined type, which is structural here — so a fresh
   * order is built when there is none. That build is on the swiping thread, but it happens only on
   * the rare path where the focused node disappeared, and a swipe would have built an order anyway.
   */
  @JvmStatic
  fun pivotAfterRemoval(root: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (lastPivotWindowId == NO_WINDOW_ID || root.windowId != lastPivotWindowId) {
      return null
    }
    val order =
      strategy?.takeIf { root == this.root }
        ?: runCatching { OrderedTraversalStrategy(root) }.getOrNull()
        ?: return null
    val bounds = Rect()
    val away = scrolledAwayBounds
    if (away != null) {
      val shift =
        shiftOffList(
          FocusProcessorForLogicalNavigation.scrollDeltaSumY() - scrolledAwayDeltaSum,
          away,
          scrolledAwayListBounds,
          scrolledAwayForward,
        )
      val place = placeAfterScroll(away.top, away.bottom, shift, scrolledAwayForward)
      if (!scrolledAwayForward) {
        // The item before the one scrolled away ends at the place; a swipe back from the first
        // node below it lands there.
        for (node in order.dumpTree()) {
          if (node.windowId != lastPivotWindowId) {
            continue
          }
          node.getBoundsInScreen(bounds)
          if (bounds.top >= place) {
            return node
          }
        }
        return null
      }
      return lastNodeAbove(order, place, bounds)
    }
    return lastNodeAbove(order, lastPivotTop, bounds)
  }

  /** The last node of [order] above [place], or the first node if none is above it. */
  private fun lastNodeAbove(
    order: OrderedTraversalStrategy,
    place: Int,
    bounds: Rect,
  ): AccessibilityNodeInfoCompat? {
    var above: AccessibilityNodeInfoCompat? = null
    for (node in order.dumpTree()) {
      if (node.windowId != lastPivotWindowId) {
        continue
      }
      node.getBoundsInScreen(bounds)
      if (bounds.top >= place) {
        return above ?: node
      }
      above = node
    }
    return above
  }

  /** Saves the order of [root], replacing any saved order. */
  @JvmStatic
  fun put(root: AccessibilityNodeInfoCompat, strategy: OrderedTraversalStrategy) {
    this.root = root
    this.strategy = strategy
    firstIgnoredChangeTime = 0L
    localTree = null
    localRoot = if (strategy.movesOnlyAmongSiblings()) root else null
  }

  /**
   * The root of the window of [node], if the next item there can be worked out around the focused
   * node: in the last order built of that window, nodes set their own place only next to a sibling,
   * and the window has shown no other screen since.
   */
  @JvmStatic
  fun localRootFor(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    localRoot?.takeIf { it.windowId == node.windowId }

  /**
   * Keeps [tree], read around a focused node, so that its nodes count as held, and later navigation
   * carries on with it, until an event changes its window.
   */
  @JvmStatic
  fun putLocal(tree: LocalTraversalOrder.Tree) {
    if (tree !== localTree) {
      localTree = tree
      localTreeWindowId = tree.root.windowId
      localTreeChanged = false
    }
  }

  /** The nodes kept by [putLocal] for the window of [root], if nothing there has changed since. */
  @JvmStatic
  fun localTreeFor(root: AccessibilityNodeInfoCompat): LocalTraversalOrder.Tree? =
    localTree?.takeIf { !localTreeChanged && !it.broken && it.root == root }

  /**
   * Whether an incomplete order of window [windowId] may be read from the app again with one bulk
   * request now. Allowed once until an event or a node action can have changed that window, so that
   * a window that stays incomplete is not read in bulk on every swipe.
   */
  @JvmStatic
  fun startReread(windowId: Int): Boolean {
    if (windowId == rereadWindowId) {
      return false
    }
    rereadWindowId = windowId
    return true
  }

  /** Throws away the saved order. */
  @JvmStatic
  fun clear(reason: String) {
    rereadWindowId = NO_WINDOW_ID
    localTree = null
    if (strategy == null) {
      return
    }
    root = null
    strategy = null
    firstIgnoredChangeTime = 0L
    lastClearReason = reason
  }

  /**
   * Throws away the saved order before TalkBack performs [actionId] on a node, unless the action
   * only moves focus. A scroll or click changes the screen before the app's change event reaches
   * us, and TalkBack can navigate again in between, such as right after it scrolls a list.
   */
  @JvmStatic
  fun onNodeAction(actionId: Int) {
    if (changesNodes(actionId)) {
      clear(if (LogUtils.shouldLog(Log.DEBUG)) AccessibilityNodeInfoUtils.actionToString(actionId) else "")
    }
  }

  /** Whether [actionId] can change the nodes before the app's event about it arrives. */
  @JvmStatic
  fun changesNodes(actionId: Int): Boolean = actionId !in FOCUS_ACTIONS && actionId != SHOW_ON_SCREEN

  /** Throws away the saved order if [event] can change its window. */
  @JvmStatic
  fun onAccessibilityEvent(event: AccessibilityEvent) {
    val type = event.eventType
    if (type and IGNORED_EVENT_TYPES != 0) {
      return
    }
    if (
      rereadWindowId != NO_WINDOW_ID &&
        affectsWindow(event, rereadWindowId) &&
        !isNonStructural(event)
    ) {
      rereadWindowId = NO_WINDOW_ID
    }
    localRoot?.let {
      if (
        type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
          (event.windowId == NO_WINDOW_ID || event.windowId == it.windowId)
      ) {
        localRoot = null
      }
    }
    if (localTree != null && affectsWindow(event, localTreeWindowId)) {
      if (isNonStructural(event)) {
        localTreeChanged = true
      } else {
        localTree = null
      }
    }
    val savedRoot = root ?: return
    if (affectsWindow(event, savedRoot.windowId)) {
      if (isNonStructural(event)) {
        if (firstIgnoredChangeTime == 0L) {
          firstIgnoredChangeTime = SystemClock.uptimeMillis()
        }
        return
      }
      clear(
        if (LogUtils.shouldLog(Log.DEBUG)) {
          AccessibilityEvent.eventTypeToString(type) +
            " 0x" +
            Integer.toHexString(event.contentChangeTypes)
        } else {
          ""
        }
      )
    }
  }

  private fun affectsWindow(event: AccessibilityEvent, windowId: Int): Boolean {
    val type = event.eventType
    return type == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
      type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
      event.windowId == NO_WINDOW_ID ||
      event.windowId == windowId
  }

  private fun isNonStructural(event: AccessibilityEvent): Boolean {
    val changes = event.contentChangeTypes
    return event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
      changes != 0 &&
      changes and NON_STRUCTURAL_CHANGES.inv() == 0
  }
}
