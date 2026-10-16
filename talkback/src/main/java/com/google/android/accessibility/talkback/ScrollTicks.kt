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

package com.google.android.accessibility.talkback

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.ScrollTickCounter.Companion.UNKNOWN
import com.google.android.accessibility.utils.AccessibilityEventUtils
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollEventHandler
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollEventInterpretation
import com.google.android.accessibility.utils.output.FeedbackController
import com.google.android.accessibility.utils.output.ScrollActionRecord
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * Plays a light tick for each item that scrolls past while the user scrolls a list, so a slow drag
 * ticks slowly and a fast one quickly, and the movement can be felt as it happens. The scroll sound
 * plays with the ticks, its pitch rising from the top of the list to the bottom.
 *
 * Both are rate limited so a fling cannot flood the output: the scroll sound is about 0.84 s long
 * and SoundPool plays at most 10 streams, stopping the oldest (which may be a focus or window
 * earcon) to start another. So the sound plays at most once per scroll event and at most every
 * 250 ms, as other scrolls do, which keeps at most four scroll sounds playing at once. The ticks
 * follow the items but stay at least 40-50 ms apart.
 *
 * A tick is also felt as soon as a two-finger drag is passed to the app, before the app reports
 * that it scrolled.
 */
class ScrollTicks(private val feedbackController: FeedbackController, density: Float) :
  ScrollEventHandler {

  private val counter = ScrollTickCounter(ITEM_DP * density)
  private val handler = Handler(Looper.getMainLooper())
  private var dragStartTime = -1L
  private val soundLimiter = FeedbackRateLimiter(MIN_SOUND_INTERVAL_MS)
  private val hapticLimiter = FeedbackRateLimiter(MIN_HAPTIC_INTERVAL_MS)

  /** Called on any thread when a two-finger drag is passed to the app. */
  fun onDragStarted() {
    handler.post {
      dragStartTime = SystemClock.uptimeMillis()
      if (hapticLimiter.tryAcquire(dragStartTime)) {
        feedbackController.playHaptic(R.array.scroll_item_pattern, /* eventId= */ null)
      }
    }
  }

  override fun onScrollEvent(
    event: AccessibilityEvent,
    interpretation: ScrollEventInterpretation,
    eventId: EventId?,
  ) {
    if (
      event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED ||
        interpretation.userAction != ScrollActionRecord.ACTION_MANUAL_SCROLL ||
        !interpretation.isFromScrollable ||
        interpretation.isMediaPlayerAutoScroll
    ) {
      return
    }
    var deltaX = UNKNOWN
    var deltaY = UNKNOWN
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      deltaX = event.scrollDeltaX
      deltaY = event.scrollDeltaY
    }
    val now = SystemClock.uptimeMillis()
    val ticks =
      counter.onScroll(
        now,
        event.windowId,
        event.className,
        // Tells apart two lists of the same class in one window, such as the panes of a two-pane
        // layout. AccessibilityNodeInfo equality compares the window and the view.
        event.source,
        if (event.itemCount > 0) event.fromIndex else UNKNOWN,
        event.scrollX,
        event.scrollY,
        deltaX,
        deltaY,
      )
    if (ticks == 0) {
      return
    }
    // The same pitch and volume as the scroll sound for other scrolls.
    val rate = 2f.pow(AccessibilityEventUtils.getScrollPercent(event, 50f) / 50f - 1)
    // The drag already ticked when it started.
    val feltAlready = dragStartTime >= 0 && now - dragStartTime < DRAG_TICK_COVER_MS
    dragStartTime = -1L
    // The sound plays once for the event, not once per item.
    if (soundLimiter.tryAcquire(now)) {
      feedbackController.playAuditoryWithoutHaptic(R.raw.scroll_tone, rate, rate, eventId)
    }
    // Spread the ticks over the time until the next scroll event, no closer than
    // MIN_TICK_SPACING_MS, so a fling ticks at most twice per event.
    handler.removeCallbacksAndMessages(null)
    val count = min(ticks, (EVENT_INTERVAL_MS / MIN_TICK_SPACING_MS).toInt())
    val spacing = EVENT_INTERVAL_MS / count
    for (i in 0 until count) {
      if (i == 0 && feltAlready) {
        continue
      }
      handler.postDelayed({ vibrate(eventId) }, i * spacing)
    }
  }

  private fun vibrate(eventId: EventId?) {
    if (hapticLimiter.tryAcquire(SystemClock.uptimeMillis())) {
      feedbackController.playHaptic(R.array.scroll_item_pattern, eventId)
    }
  }

  private companion object {
    // About the height of one list row.
    const val ITEM_DP = 48f
    // Android sends scroll events at most this often.
    const val EVENT_INTERVAL_MS = 100L
    // How long after a drag starts its tick stands for the first scroll event's tick. Apps report
    // the first scroll about 100 ms after it starts.
    const val DRAG_TICK_COVER_MS = 300L
    // The scroll sound plays at most this often, the same as for other scrolls (see Mappers).
    const val MIN_SOUND_INTERVAL_MS = 250L
    // The ticks of one scroll event are spread at least this far apart.
    const val MIN_TICK_SPACING_MS = 50L
    // No tick plays sooner than this after the last, whatever the events do. A little less than
    // MIN_TICK_SPACING_MS, as the handler may run a tick late.
    const val MIN_HAPTIC_INTERVAL_MS = 40L
  }
}

/** Lets feedback through at most once every [minIntervalMs]. */
class FeedbackRateLimiter(private val minIntervalMs: Long) {
  private var lastTime = Long.MIN_VALUE

  /** Returns whether feedback may play at [time], and if so, counts it as played. */
  fun tryAcquire(time: Long): Boolean {
    if (lastTime != Long.MIN_VALUE && time - lastTime < minIntervalMs) {
      return false
    }
    lastTime = time
    return true
  }
}

/** Counts the items that scrolled past since the last scroll event of the same scroll. */
class ScrollTickCounter(private val itemPx: Float) {
  private var lastTime = -1L
  private var lastWindowId = UNKNOWN
  private var lastClassName: CharSequence? = null
  private var lastSource: Any? = null
  private var lastFromIndex = UNKNOWN
  private var lastScrollX = UNKNOWN
  private var lastScrollY = UNKNOWN
  // Distance scrolled that has not made up a whole item yet.
  private var pixels = 0f

  /**
   * Returns how many ticks to play for a scroll event: one for the first event of a scroll, then
   * one for each item scrolled past, at most [MAX_TICKS]. Pass [UNKNOWN] for values the event does
   * not have. [source] identifies the scrolled view by equality, such as the event's
   * AccessibilityNodeInfo, or is null when unknown.
   */
  fun onScroll(
    time: Long,
    windowId: Int,
    className: CharSequence?,
    source: Any?,
    fromIndex: Int,
    scrollX: Int,
    scrollY: Int,
    deltaX: Int,
    deltaY: Int,
  ): Int {
    val sameScroll =
      lastTime >= 0 &&
        time - lastTime <= SCROLL_GAP_MS &&
        windowId == lastWindowId &&
        className == lastClassName &&
        source == lastSource
    val previousFromIndex = lastFromIndex
    val previousScrollX = lastScrollX
    val previousScrollY = lastScrollY
    lastTime = time
    lastWindowId = windowId
    lastClassName = className
    lastSource = source
    lastFromIndex = fromIndex
    lastScrollX = scrollX
    lastScrollY = scrollY
    if (!sameScroll) {
      // One tick to say the scroll started, however far it went.
      pixels = 0f
      return 1
    }

    val items: Int
    if (fromIndex >= 0 && previousFromIndex >= 0) {
      // The list says which item is first on screen.
      items = abs(fromIndex - previousFromIndex)
    } else {
      pixels +=
        if (deltaX != UNKNOWN || deltaY != UNKNOWN) {
          abs(known(deltaX)) + abs(known(deltaY)).toFloat()
        } else {
          abs(change(previousScrollX, scrollX)) + abs(change(previousScrollY, scrollY)).toFloat()
        }
      items = (pixels / itemPx).toInt()
      pixels -= items * itemPx
    }
    return min(items, MAX_TICKS)
  }

  private fun known(value: Int) = if (value == UNKNOWN) 0 else value

  private fun change(from: Int, to: Int) = if (from < 0 || to < 0) 0 else to - from

  companion object {
    const val UNKNOWN = -1

    /** The most ticks for one scroll event, so a fling still ticks as separate taps. */
    const val MAX_TICKS = 4

    /** Scroll events further apart than this belong to different scrolls. */
    const val SCROLL_GAP_MS = 250L
  }
}
