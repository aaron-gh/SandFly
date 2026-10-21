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

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FocusFinder
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollTimeout
import com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH
import com.google.android.accessibility.utils.output.ScrollActionRecord
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils

/**
 * Scrolls the list with accessibility focus, or else the first list in the window, to its start or
 * end, a page at a time, then focuses the list's first or last item. With no list to scroll, it
 * focuses the first or last item on screen.
 */
class ListEdgeScroller(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  private val handler = Handler(Looper.getMainLooper())
  private val focusFinder = FocusFinder(service)
  private var list: AccessibilityNodeInfoCompat? = null
  private var toStart = true
  private var scrolls = 0
  private var eventId: EventId? = null

  private val step = Runnable { scrollOnce() }

  /** Starts scrolling to the start, or to the end. Returns false if there is nothing to focus. */
  fun scroll(toStart: Boolean, eventId: EventId?): Boolean {
    cancel()
    this.toStart = toStart
    this.eventId = eventId
    val found = findList() ?: return focusOnScreen()
    list = found
    isActive = true
    scrolls = 0
    scrollOnce()
    return true
  }

  /** Stops scrolling and focusing, such as when the user touches the screen or presses a key. */
  fun cancel() {
    handler.removeCallbacksAndMessages(null)
    list = null
    isActive = false
  }

  private fun scrollOnce() {
    val current = list ?: return
    val action = scrollAction()
    val scrolled =
      scrolls < MAX_SCROLLS &&
        current.refresh() &&
        AccessibilityNodeInfoUtils.supportsAction(current, action) &&
        pipeline.returnFeedback(
          eventId,
          // Recorded as Backtalk's own scroll, so focus doesn't follow it as it would a user's, and
          // the item focused at the end is spoken rather than how far the list scrolled.
          Feedback.scroll(
            current,
            ScrollActionRecord.ACTION_AUTO_SCROLL,
            action,
            SCROLL_SOURCE,
            ScrollTimeout.SCROLL_TIMEOUT_SHORT,
            /* autoScrollAttempt= */ 0,
          ),
        )
    if (scrolled) {
      scrolls++
      handler.postDelayed(step, SCROLL_INTERVAL_MS)
      return
    }
    list = null
    // Waits for the last page to be laid out before looking for its first or last item.
    handler.postDelayed(
      {
        isActive = false
        focusEdgeOf(current)
      },
      SCROLL_INTERVAL_MS,
    )
  }

  private fun scrollAction(): Int =
    if (toStart) AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
    else AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD

  /**
   * The scrollable ancestor of accessibility focus, which may already be at the edge, or else the
   * first list in the window that can scroll that way. Sliders adjust with scroll actions, so they
   * are left out.
   */
  private fun findList(): AccessibilityNodeInfoCompat? {
    val focus = focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(focus, filter { isList(it) })?.let {
      return it
    }
    val root =
      focus?.let { AccessibilityNodeInfoUtils.getRoot(it) }
        ?: service.rootInActiveWindow?.let { AccessibilityNodeInfoCompat.wrap(it) }
        ?: return null
    return AccessibilityNodeInfoUtils.searchFromBfs(
      root,
      filter {
        it != null && isList(it) && AccessibilityNodeInfoUtils.supportsAction(it, scrollAction())
      },
    )
  }

  private fun isList(node: AccessibilityNodeInfoCompat?): Boolean =
    node != null &&
      node.isVisibleToUser &&
      Role.getRole(node) != Role.ROLE_SEEK_CONTROL &&
      AccessibilityNodeInfoUtils.isScrollable(node)

  private fun filter(accept: (AccessibilityNodeInfoCompat?) -> Boolean) =
    object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat?): Boolean = accept(node)
    }

  private fun focusEdgeOf(list: AccessibilityNodeInfoCompat) {
    val direction =
      if (toStart) TraversalStrategy.SEARCH_FOCUS_FORWARD
      else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val target =
      if (list.refresh()) {
        val strategy = TraversalStrategyUtils.getTraversalStrategy(list, focusFinder, direction)
        TraversalStrategyUtils.findFirstFocusInNodeTree(
          strategy,
          list,
          direction,
          NavigationTarget.createNodeFilter(
            NavigationTarget.TARGET_DEFAULT,
            strategy.speakingNodesCache,
          ),
        )
      } else {
        null
      }
    if (target == null) {
      focusOnScreen()
      return
    }
    pipeline.returnFeedback(
      eventId,
      Feedback.focus(
        target,
        FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(),
      ),
    )
  }

  private fun focusOnScreen(): Boolean =
    pipeline.returnFeedback(
      eventId,
      if (toStart) Feedback.focusTop(INPUT_MODE_TOUCH) else Feedback.focusBottom(INPUT_MODE_TOUCH),
    )

  companion object {
    private const val SCROLL_SOURCE = "LIST_EDGE"

    /** The time an app takes to move its list a page, before the next scroll. */
    private const val SCROLL_INTERVAL_MS = 150L

    /** Lists longer than this many pages are left part way. */
    private const val MAX_SCROLLS = 100

    /**
     * Whether a list is scrolling to its start or end. Scroll events of a page can come after the
     * next page's scroll has started, which makes them look like the user's own scrolling, so
     * focus mustn't follow them meanwhile.
     */
    @JvmStatic
    @Volatile
    var isActive = false
      private set
  }
}
