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

package com.google.android.accessibility.talkback.selector

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollTimeout
import com.google.android.accessibility.utils.output.ScrollActionRecord
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils
import com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH

/**
 * Moves accessibility focus to the item below or above the focused one for the Vertical navigation
 * reading control. In a list, it stays in the list and scrolls it a page when no item below or
 * above is on screen.
 */
class VerticalNavigator(
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  private val handler = Handler(Looper.getMainLooper())

  /** Moves down, or up. Plays the end sound when there is no item to move to. */
  fun move(eventId: EventId?, forward: Boolean) {
    handler.removeCallbacksAndMessages(null)
    val focus = focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
    if (focus == null) {
      pipeline.returnFeedback(eventId, Feedback.focusTop(INPUT_MODE_TOUCH))
      return
    }
    val list = VerticalNavigation.listOf(focus)
    if (list != null) {
      VerticalNavigation.target(focus, forward, list)?.let {
        focus(eventId, it)
        return
      }
      if (scroll(eventId, list, forward)) {
        val content = contentOf(focus)
        handler.postDelayed({ moveAfterScroll(eventId, focus, content, list, forward) }, SCROLL_WAIT_MS)
        return
      }
    }
    // At the end of the list, or outside one: the next item below or above on screen.
    val target = VerticalNavigation.target(focus, forward)
    if (target != null) {
      focus(eventId, target)
    } else {
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete))
    }
  }

  /** Stops a move waiting for a list to scroll, such as when the user touches the screen. */
  fun cancel() {
    handler.removeCallbacksAndMessages(null)
  }

  private fun scroll(eventId: EventId?, list: AccessibilityNodeInfoCompat, forward: Boolean): Boolean {
    val action =
      if (forward) AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
      else AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
    return AccessibilityNodeInfoUtils.supportsAction(list, action) &&
      pipeline.returnFeedback(
        eventId,
        // SandFly's own scroll, so focus doesn't follow it as it would the user's scrolling.
        Feedback.scroll(
          list,
          ScrollActionRecord.ACTION_AUTO_SCROLL,
          action,
          SCROLL_SOURCE,
          ScrollTimeout.SCROLL_TIMEOUT_SHORT,
          /* autoScrollAttempt= */ 0,
        ),
      )
  }

  private fun moveAfterScroll(
    eventId: EventId?,
    focus: AccessibilityNodeInfoCompat,
    content: String,
    list: AccessibilityNodeInfoCompat,
    forward: Boolean,
  ) {
    if (!list.refresh()) {
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete))
      return
    }
    // Lists reuse the views of items scrolled away for new ones, so the focused node may now show
    // another item. If it still shows the focused one, the item below or above it is the target;
    // otherwise, the list's first or last row, which follows the focused item.
    val stillShown = focus.refresh() && focus.isVisibleToUser && contentOf(focus) == content
    val target =
      if (stillShown) VerticalNavigation.target(focus, forward, list)
      else VerticalNavigation.edgeRow(list, first = forward)
    if (target != null) {
      focus(eventId, target)
    } else {
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete))
    }
  }

  /**
   * Focuses [target], first scrolling it wholly into view when part of it is off screen, as a swipe
   * does: parts that are off screen aren't spoken, so a row half under a toolbar would be read
   * without its title.
   */
  private fun focus(eventId: EventId?, target: AccessibilityNodeInfoCompat) {
    val list = VerticalNavigation.listOf(target)
    if (list != null &&
        hasPartOffScreen(target) &&
        // Recorded as SandFly's own scroll, so it isn't taken for the user's and focus stays.
        pipeline.returnFeedback(eventId, Feedback.scrollEnsureOnScreen(list, target))
    ) {
      handler.postDelayed(
        {
          if (target.refresh() && target.isVisibleToUser) {
            focusNow(eventId, target)
          } else {
            pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete))
          }
        },
        SHOW_ON_SCREEN_WAIT_MS,
      )
      return
    }
    focusNow(eventId, target)
  }

  private fun focusNow(eventId: EventId?, target: AccessibilityNodeInfoCompat) {
    pipeline.returnFeedback(
      eventId,
      Feedback.focus(
        target,
        FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(),
      ),
    )
  }

  /** Whether the screen cuts off part of [node]: it is shown smaller than it is laid out. */
  private fun hasPartOffScreen(node: AccessibilityNodeInfoCompat): Boolean {
    val onScreen = Rect().also { node.getBoundsInScreen(it) }
    val laidOut = Rect().also { node.getBoundsInParent(it) }
    return onScreen.height() < laidOut.height() || onScreen.width() < laidOut.width()
  }

  private fun contentOf(node: AccessibilityNodeInfoCompat): String =
    TextUtils.concat(node.text ?: "", "\n", node.contentDescription ?: "").toString()

  private companion object {
    const val SCROLL_SOURCE = "VERTICAL_NAVIGATION"

    /** The time a list takes to scroll a page and lay it out. */
    const val SCROLL_WAIT_MS = 300L

    /** The time a list takes to bring an item wholly into view. */
    const val SHOW_ON_SCREEN_WAIT_MS = 150L
  }
}

/**
 * Finds the item below or above the focused one for the Vertical navigation reading control, which
 * leaves out the other items of the same row, such as the buttons beside a video's title or a
 * file's name.
 */
object VerticalNavigation {

  /**
   * Returns the first item after [focus], in reading order, whose middle is below [focus], or the
   * first item of the row above [focus], or null when there is none on screen. With [within], only
   * items in it are found.
   */
  @JvmStatic
  @JvmOverloads
  fun target(
    focus: AccessibilityNodeInfoCompat,
    forward: Boolean,
    within: AccessibilityNodeInfoCompat? = null,
  ): AccessibilityNodeInfoCompat? {
    val root = within ?: AccessibilityNodeInfoUtils.getRoot(focus) ?: return null
    val strategy = strategy(root)
    val focusable = focusable(strategy)
    val from = bounds(focus)
    val direction =
      if (forward) TraversalStrategy.SEARCH_FOCUS_FORWARD
      else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val found =
      TraversalStrategyUtils.searchFocus(
        strategy,
        focus,
        direction,
        filter {
          focusable?.accept(it) == true &&
            isOnOtherRow(bounds(it), from, forward) &&
            // Going up reaches a card before the button in it that has focus.
            !AccessibilityNodeInfoUtils.hasAncestor(focus, it)
        },
      ) ?: return null
    val target = wholeItem(found, root, focus, focusable) { isOnOtherRow(bounds(it), from, forward) }
    if (forward) {
      return target
    }
    // Going up lands on the start of the row, as going down does.
    return rowStart(strategy, focusable, target, root)
  }

  /**
   * The item that [node] is part of: its outermost ancestor below [root] that can be focused too,
   * other than one holding [focus]. Going up reaches a row's parts, such as a setting's summary,
   * before the row itself, which is what going down focuses.
   */
  private fun wholeItem(
    node: AccessibilityNodeInfoCompat,
    root: AccessibilityNodeInfoCompat,
    focus: AccessibilityNodeInfoCompat,
    focusable: Filter<AccessibilityNodeInfoCompat>?,
    fits: (AccessibilityNodeInfoCompat) -> Boolean = { true },
  ): AccessibilityNodeInfoCompat {
    // In web content every container can be focused, so climbing would reach the whole page.
    if (WebInterfaceUtils.supportsWebActions(node)) return node
    var item = node
    var ancestor = node.parent
    while (ancestor != null && ancestor != root) {
      if (ancestor == focus || AccessibilityNodeInfoUtils.hasAncestor(focus, ancestor)) break
      if (focusable?.accept(ancestor) == true && fits(ancestor)) item = ancestor
      ancestor = ancestor.parent
    }
    return item
  }

  /** Returns the start of the first or last row of [list] on screen. */
  @JvmStatic
  fun edgeRow(list: AccessibilityNodeInfoCompat, first: Boolean): AccessibilityNodeInfoCompat? {
    val strategy = strategy(list)
    val focusable = focusable(strategy)
    val direction =
      if (first) TraversalStrategy.SEARCH_FOCUS_FORWARD
      else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val edge =
      TraversalStrategyUtils.findFirstFocusInNodeTree(
        strategy,
        list,
        direction,
        filter { focusable?.accept(it) == true },
      ) ?: return null
    // Nothing in the list has focus, so the list itself is the node to stay out of.
    val item = wholeItem(edge, list, list, focusable)
    return if (first) item else rowStart(strategy, focusable, item, list)
  }

  /**
   * Returns the nearest ancestor of [node] that scrolls up and down, or null. Rows that scroll
   * sideways, such as a carousel in a list, and pagers are passed over.
   */
  @JvmStatic
  fun listOf(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    AccessibilityNodeInfoUtils.getMatchingAncestor(node, filter { scrollsVertically(it) })

  /** Whether [node] is shown and scrolls up and down, unlike a slider, a pager or a carousel. */
  @JvmStatic
  fun scrollsVertically(node: AccessibilityNodeInfoCompat): Boolean {
    if (!node.isVisibleToUser || !AccessibilityNodeInfoUtils.isScrollable(node)) return false
    when (Role.getRole(node)) {
      Role.ROLE_SEEK_CONTROL,
      Role.ROLE_HORIZONTAL_SCROLL_VIEW,
      Role.ROLE_PAGER -> return false
    }
    val actions = node.actionList
    val vertical =
      actions.contains(AccessibilityActionCompat.ACTION_SCROLL_DOWN) ||
        actions.contains(AccessibilityActionCompat.ACTION_SCROLL_UP)
    val sideways =
      actions.contains(AccessibilityActionCompat.ACTION_SCROLL_LEFT) ||
        actions.contains(AccessibilityActionCompat.ACTION_SCROLL_RIGHT)
    if (vertical) return true
    if (sideways) return false
    // A list of one row, such as a horizontal RecyclerView.
    val info = node.collectionInfo
    return info == null || info.rowCount != 1 || info.columnCount <= 1
  }

  private fun rowStart(
    strategy: OrderedTraversalStrategy,
    focusable: Filter<AccessibilityNodeInfoCompat>?,
    target: AccessibilityNodeInfoCompat,
    root: AccessibilityNodeInfoCompat,
  ): AccessibilityNodeInfoCompat {
    val row = bounds(target)
    var start: AccessibilityNodeInfoCompat = target
    while (true) {
      val before =
        TraversalStrategyUtils.searchFocus(
          strategy,
          start,
          TraversalStrategy.SEARCH_FOCUS_BACKWARD,
          filter { focusable?.accept(it) == true },
        ) ?: break
      val item = wholeItem(before, root, start, focusable)
      if (item == start || !isOnSameRow(bounds(item), row)) break
      start = item
    }
    return start
  }

  private fun strategy(root: AccessibilityNodeInfoCompat) =
    OrderedTraversalStrategy(
      root,
      /* includeChildrenOfNodesWithWebActions= */ true,
      /* makeFabFirst= */ false,
    )

  private fun focusable(strategy: OrderedTraversalStrategy): Filter<AccessibilityNodeInfoCompat>? =
    NavigationTarget.createNodeFilter(NavigationTarget.TARGET_DEFAULT, strategy.speakingNodesCache)

  /**
   * Whether an item with [bounds] is below [from], going [forward], or above it: its middle is past
   * the edge of [from].
   */
  @JvmStatic
  fun isOnOtherRow(bounds: Rect, from: Rect, forward: Boolean): Boolean =
    if (forward) bounds.centerY() >= from.bottom else bounds.centerY() <= from.top

  /** Whether an item with [bounds] is in the row of the item with [row]: its middle is within it. */
  @JvmStatic
  fun isOnSameRow(bounds: Rect, row: Rect): Boolean =
    bounds.centerY() > row.top && bounds.centerY() < row.bottom

  private fun bounds(node: AccessibilityNodeInfoCompat): Rect =
    Rect().also { node.getBoundsInScreen(it) }

  private fun filter(accept: (AccessibilityNodeInfoCompat) -> Boolean) =
    object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat?): Boolean =
        node != null && accept(node)
    }
}
