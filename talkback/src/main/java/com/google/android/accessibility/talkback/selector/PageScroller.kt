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

package com.google.android.accessibility.talkback.selector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_PAGE
import com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_PAGE
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForLogicalNavigation
import com.google.android.accessibility.talkback.gesture.SyntheticTouches
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.Performance.EventId

/**
 * Scrolls a page for the Scroll reading control. The list gets its scroll action, as before. Where
 * no list takes the action, or the list doesn't move, a swipe is sent to the list, or to the middle
 * of the window when no list is found, as a two-finger swipe passed to the app does. Some lists
 * move only for touches.
 */
class PageScroller(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  private val handler = Handler(Looper.getMainLooper())

  /** Scrolls forward, toward the end of the list, or back. */
  fun scroll(eventId: EventId?, forward: Boolean) {
    cancel()
    val direction = if (forward) NEXT_PAGE else PREVIOUS_PAGE
    val action =
      if (forward) AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
      else AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
    val focus = focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
    // Sliders, pickers and carousels adjust or scroll sideways with the action; no swipe follows.
    if (
      !SyntheticTouches.isAvailable() ||
        (focus != null &&
          AccessibilityNodeInfoUtils.supportsAction(focus, action) &&
          !VerticalNavigation.scrollsVertically(focus))
    ) {
      pipeline.returnFeedback(eventId, Feedback.focusDirection(direction))
      return
    }
    val focusList = focus?.let { VerticalNavigation.listOf(it) }
    val list = focusList ?: windowList(focus)
    val listTakesAction = list != null && AccessibilityNodeInfoUtils.supportsAction(list, action)
    val before = list?.let { contentOf(it) }
    val scrollEvents = FocusProcessorForLogicalNavigation.scrollEventCount()
    if (!pipeline.returnFeedback(eventId, Feedback.focusDirection(direction))) {
      if (swipesAfterFailedAction(list != null, listTakesAction)) {
        // The gesture that adjusted the reading control may still be ending.
        handler.postDelayed({ swipe(focusList, focus, forward) }, GESTURE_END_WAIT_MS)
      }
      return
    }
    handler.postDelayed(
      {
        val moved =
          FocusProcessorForLogicalNavigation.scrollEventCount() != scrollEvents ||
            (list != null && (!list.refresh() || contentOf(list) != before))
        if (!moved) {
          swipe(focusList, focus, forward)
        }
      },
      CHECK_DELAY_MS,
    )
  }

  /** Stops a swipe that is waiting, such as when the user touches the screen or presses a key. */
  fun cancel() {
    handler.removeCallbacksAndMessages(null)
  }

  /** The first list that scrolls up and down in the window of [focus], or else the active one. */
  private fun windowList(focus: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
    val root =
      focus?.let { AccessibilityNodeInfoUtils.getRoot(it) }
        ?: service.rootInActiveWindow?.let { AccessibilityNodeInfoCompat.wrap(it) }
        ?: return null
    return AccessibilityNodeInfoUtils.searchFromBfs(
      root,
      object : Filter<AccessibilityNodeInfoCompat>() {
        override fun accept(node: AccessibilityNodeInfoCompat?): Boolean =
          node != null && VerticalNavigation.scrollsVertically(node)
      },
    )
  }

  /**
   * What the list shows: where its first items are, and which they are. A list that scrolled
   * shows other items or the same ones elsewhere.
   */
  private fun contentOf(list: AccessibilityNodeInfoCompat): List<Any?> {
    val content = ArrayList<Any?>()
    val bounds = Rect()
    val count = list.childCount
    content.add(count)
    for (i in 0 until minOf(count, MAX_CHILDREN)) {
      val child = list.getChild(i)
      if (child == null || !child.refresh()) {
        content.add(null)
        continue
      }
      child.getBoundsInScreen(bounds)
      content.add(bounds.top)
      content.add(bounds.bottom)
      content.add(child.collectionItemInfo?.rowIndex)
      content.add(firstText(child, TEXT_DEPTH))
    }
    return content
  }

  private fun firstText(node: AccessibilityNodeInfoCompat, depth: Int): String? {
    node.text?.takeIf { it.isNotEmpty() }?.let {
      return it.toString()
    }
    node.contentDescription?.takeIf { it.isNotEmpty() }?.let {
      return it.toString()
    }
    if (depth == 0) return null
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      firstText(child, depth - 1)?.let {
        return it
      }
    }
    return null
  }

  /**
   * Swipes over [focusList], the list with accessibility focus, or else over the middle of the
   * window of [focus].
   */
  private fun swipe(
    focusList: AccessibilityNodeInfoCompat?,
    focus: AccessibilityNodeInfoCompat?,
    forward: Boolean,
  ) {
    if (!SyntheticTouches.isAvailable()) return
    val node =
      focusList?.takeIf { it.refresh() }
        ?: focus?.let { AccessibilityNodeInfoUtils.getRoot(it) }
        ?: service.rootInActiveWindow?.let { AccessibilityNodeInfoCompat.wrap(it) }
    val area = Rect()
    node?.getBoundsInScreen(area)
    val metrics = service.resources.displayMetrics
    val screen = Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    if (screen.isEmpty) return
    if (area.isEmpty || !area.intersect(screen) || area.isEmpty) {
      area.set(screen)
    }
    val line = swipeLine(area, forward)
    val path =
      Path().apply {
        moveTo(line[0], line[1])
        lineTo(line[0], line[2])
      }
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
        .build()
    SyntheticTouches.dispatch(service, gesture, SWIPE_DURATION_MS)
  }

  companion object {
    /** How long a list has to start moving after its scroll action. */
    private const val CHECK_DELAY_MS = 600L

    /** How long the gesture that adjusted the reading control has to end. */
    private const val GESTURE_END_WAIT_MS = 150L

    private const val SWIPE_DURATION_MS = 300L

    /** The list's items compared to tell whether it moved. */
    private const val MAX_CHILDREN = 12

    /** How deep in an item its text is looked for. */
    private const val TEXT_DEPTH = 2

    /** Where the swipe starts and ends, as shares of the height of what it goes over. */
    private const val LOWER_FRACTION = 0.7f
    private const val UPPER_FRACTION = 0.3f

    /**
     * Whether to swipe when the scroll action couldn't be sent. A list that doesn't take the action
     * is at its start or end, where a swipe would only pull it to refresh or stretch it.
     */
    @JvmStatic
    fun swipesAfterFailedAction(listFound: Boolean, listTakesAction: Boolean): Boolean =
      !listFound || listTakesAction

    /**
     * The swipe over [area]: its x, then the y it starts and ends at. Scrolling forward shows what
     * comes after, so the finger moves up, as two fingers swiping up do.
     */
    @JvmStatic
    fun swipeLine(area: Rect, forward: Boolean): FloatArray {
      val x = area.exactCenterX()
      val lower = area.top + area.height() * LOWER_FRACTION
      val upper = area.top + area.height() * UPPER_FRACTION
      return if (forward) floatArrayOf(x, lower, upper) else floatArrayOf(x, upper, lower)
    }
  }
}
