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
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.selector.VerticalNavigation
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollTimeout
import com.google.android.accessibility.utils.output.ScrollActionRecord

/**
 * Refreshes a list as pulling it down from its top does: SandFly scrolls the list to its top and
 * sends a downward swipe to it, which apps take as a pull to refresh.
 */
class PullToRefresh(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  private val handler = Handler(Looper.getMainLooper())
  private var list: AccessibilityNodeInfoCompat? = null
  private var scrolls = 0
  private var eventId: EventId? = null
  private var pendingPull: Runnable? = null

  private val step = Runnable { scrollOnce() }

  /**
   * Scrolls the list with accessibility focus, or else the first list on screen, to its top, then
   * pulls it down. Returns false if there is no list.
   */
  fun refresh(eventId: EventId?): Boolean {
    cancel()
    val found = findList() ?: return false
    this.eventId = eventId
    list = found
    scrolls = 0
    scrollOnce()
    return true
  }

  /**
   * Pulls down the list with accessibility focus, or else the first list on screen, if it is at its
   * top. Returns false if there is no such list.
   */
  fun pullIfAtTop(eventId: EventId?): Boolean {
    cancel()
    val found = findList() ?: return false
    if (AccessibilityNodeInfoUtils.supportsAction(found, SCROLL_BACKWARD)) return false
    return pull(eventId, found)
  }

  /** Stops scrolling to the top, such as when the user touches the screen or presses a key. */
  fun cancel() {
    handler.removeCallbacksAndMessages(null)
    list = null
    if (pendingPull != null) {
      pendingPull = null
      endPassThrough()
    }
  }

  private fun scrollOnce() {
    val current = list ?: return
    val scrolled =
      scrolls < MAX_SCROLLS &&
        current.refresh() &&
        AccessibilityNodeInfoUtils.supportsAction(current, SCROLL_BACKWARD) &&
        pipeline.returnFeedback(
          eventId,
          Feedback.scroll(
            current,
            ScrollActionRecord.ACTION_AUTO_SCROLL,
            SCROLL_BACKWARD,
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
    // Waits for the top to be laid out, since some apps only refresh from the very top.
    handler.postDelayed({ pull(eventId, current) }, SCROLL_INTERVAL_MS)
  }

  private fun pull(eventId: EventId?, list: AccessibilityNodeInfoCompat): Boolean {
    // Before Android 11, there is no way to pass the swipe through touch exploration.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val bounds = Rect()
    list.refresh()
    list.getBoundsInScreen(bounds)
    if (bounds.isEmpty) return false
    val screenHeight = service.resources.displayMetrics.heightPixels
    val startY = bounds.top + bounds.height() / 10f
    val endY = minOf(bounds.bottom - 1f, startY + screenHeight * PULL_FRACTION)
    if (endY <= startY) return false
    val path =
      Path().apply {
        moveTo(bounds.exactCenterX(), startY)
        lineTo(bounds.exactCenterX(), endY)
      }
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, PULL_DURATION_MS))
        .build()
    // Touch exploration would take the swipe as a SandFly gesture, so the screen passes touch
    // through to the app until the swipe ends.
    val metrics = service.resources.displayMetrics
    pipeline.returnFeedback(
      eventId,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    val ended =
      object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) = endPassThrough()

        override fun onCancelled(description: GestureDescription) = endPassThrough()
      }
    // Android applies the region a moment later, and a gesture that started this may still be
    // ending.
    val send = Runnable {
      pendingPull = null
      if (!service.dispatchGesture(gesture, ended, handler)) endPassThrough()
    }
    pendingPull = send
    handler.postDelayed(send, PASS_THROUGH_WAIT_MS)
    pipeline.returnFeedback(eventId, Feedback.sound(R.raw.scroll_tone))
    return true
  }

  private fun endPassThrough() {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, /* region= */ null),
    )
  }

  /**
   * The list that scrolls up and down with accessibility focus in it, or else the first one in the
   * active window.
   */
  private fun findList(): AccessibilityNodeInfoCompat? {
    val focus = focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
    focus?.let { VerticalNavigation.listOf(it) }?.let {
      return it
    }
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

  private companion object {
    const val SCROLL_BACKWARD = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
    const val SCROLL_SOURCE = "PULL_TO_REFRESH"

    /** The time an app takes to move its list a page, before the next scroll. */
    const val SCROLL_INTERVAL_MS = 150L

    /** Lists longer than this many pages are left part way. */
    const val MAX_SCROLLS = 100

    /** How far the pull goes, as a share of the screen's height. */
    const val PULL_FRACTION = 0.4f

    const val PULL_DURATION_MS = 400L

    const val PASS_THROUGH_WAIT_MS = 350L
  }
}
