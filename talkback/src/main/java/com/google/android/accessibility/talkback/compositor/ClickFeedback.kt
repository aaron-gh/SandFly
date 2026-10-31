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

package com.google.android.accessibility.talkback.compositor

import android.content.Context
import android.os.SystemClock
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.compositor.rule.EventTypeViewClickedFeedbackRule
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Performance.EventId

/**
 * Feedback for the clicks SandFly sends itself, with a double tap, the activation key or a braille
 * display.
 *
 * The click sound and vibration are given as the click is sent. A new state, such as "On", is said
 * as soon as the app has carried the click out, interrupting what was said about the item. Without
 * this, both wait for the app's click event, which is handled 150 ms after it arrives so that the
 * item has time to update. The app's reports of the same click and state then give nothing more.
 *
 * Apps that update the item later than the click still have its state said from their own events.
 */
class ClickFeedback(
  private val compositor: Compositor,
  private val context: Context,
  private val globalVariables: GlobalVariables,
) {

  /**
   * Sends a click on [target] with [click] and gives its feedback. Returns what [click] returned.
   */
  fun click(target: AccessibilityNodeInfoCompat, eventId: EventId?, click: () -> Boolean): Boolean {
    compositor.giveClickCues(eventId)
    // Read after the cues, since a child that isn't cached waits for the app.
    val items = itemsWithState(target)
    val before = items?.let { stateOf(it) }
    val start = SystemClock.uptimeMillis()
    val success = click()
    val sent = SENT.add(start, SystemClock.uptimeMillis())
    if (!success || items == null) {
      return success
    }
    // The copies are only updated by the app's events, which wait until this returns.
    if (!items.all { it.refresh() }) {
      return true
    }
    val after = stateOf(items) ?: return true
    if (TextUtils.equals(after.text, before?.text) || Scripts.isSilenced(after.node)) {
      return true
    }
    sent.said = after.text.toString()
    compositor.sayClickState(after.node, after.text, eventId)
    return true
  }

  /**
   * [feedback] for an app's [event], without what was already given for a click SandFly sent.
   * [type] is the compositor's event type.
   */
  fun filter(
    type: Int,
    event: AccessibilityEvent,
    source: AccessibilityNodeInfoCompat?,
    feedback: EventFeedback,
  ): EventFeedback {
    if (
      (type != Compositor.EVENT_TYPE_VIEW_CLICKED &&
        type != Compositor.EVENT_TYPE_WINDOW_CONTENT_CHANGED) || SENT.isEmpty()
    ) {
      return feedback
    }
    val time = event.eventTime
    val cuesGiven = type == Compositor.EVENT_TYPE_VIEW_CLICKED && SENT.isSentDuring(time)
    val tts = feedback.ttsOutput().orElse(null)
    val said =
      !TextUtils.isEmpty(tts) &&
        (SENT.isSaid(time, tts.toString()) ||
          (source != null && SENT.isSaid(time, stateDescriptionOf(source))))
    if (!cuesGiven && !said) {
      return feedback
    }
    val builder = feedback.toBuilder()
    if (cuesGiven) {
      builder.setEarcon(-1).setSecondEarcon(-1).setHaptic(-1)
    }
    if (said) {
      builder.setTtsOutput(java.util.Optional.of(""))
    }
    return builder.build()
  }

  /**
   * [target] and its children whose state a click says, as copies, or null if the click says no
   * state for any of them.
   */
  private fun itemsWithState(target: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat>? {
    val items = ArrayList<AccessibilityNodeInfoCompat>()
    items += copy(target)
    var any = hasState(target)
    for (i in 0 until minOf(target.childCount, MAX_CHILDREN)) {
      val child = target.getChild(i) ?: continue
      if (child.isCheckable || !TextUtils.isEmpty(AccessibilityNodeInfoUtils.getState(child))) {
        items += child
        any = true
      }
    }
    return if (any) items else null
  }

  private class State(val node: AccessibilityNodeInfoCompat, val text: CharSequence)

  /**
   * What a click says of [items], the target first: a checked state, as the app's click event says
   * it, or else a state description, as the app's state change event says it.
   */
  private fun stateOf(items: List<AccessibilityNodeInfoCompat>): State? {
    for (node in items) {
      val checked =
        EventTypeViewClickedFeedbackRule.getNodeCheckedStateText(node, context, globalVariables)
      if (!TextUtils.isEmpty(checked)) {
        return State(node, checked)
      }
    }
    val opened = AccessibilityNodeFeedbackUtils.getCollapsedOrExpandedStateText(items[0], context)
    if (!TextUtils.isEmpty(opened)) {
      return State(items[0], opened)
    }
    for (node in items) {
      val description = stateDescriptionOf(node)
      if (!TextUtils.isEmpty(description)) {
        return State(node, description)
      }
    }
    return null
  }

  private fun stateDescriptionOf(node: AccessibilityNodeInfoCompat): String =
    AccessibilityNodeFeedbackUtils.getNodeStateDescription(node, context, globalVariables)
      .toString()

  private fun hasState(node: AccessibilityNodeInfoCompat): Boolean =
    node.isCheckable ||
      AccessibilityNodeInfoUtils.isExpandable(node) ||
      AccessibilityNodeInfoUtils.isCollapsible(node) ||
      !TextUtils.isEmpty(AccessibilityNodeInfoUtils.getState(node))

  @Suppress("DEPRECATION") // A copy, so that refreshing it leaves other holders' nodes alone.
  private fun copy(node: AccessibilityNodeInfoCompat) = AccessibilityNodeInfoCompat.obtain(node)

  companion object {
    // Each child not in the cache waits for the app before the click.
    private const val MAX_CHILDREN = 8

    private val SENT = SentClicks(SystemClock::uptimeMillis)

    /**
     * Whether [stateDescription], reported by an app at [eventTime], was already said for a click
     * SandFly sent.
     */
    @JvmStatic
    fun isSaid(eventTime: Long, stateDescription: CharSequence?): Boolean =
      !TextUtils.isEmpty(stateDescription) && SENT.isSaid(eventTime, stateDescription.toString())

    /** Forgets every click, such as when SandFly turns off. */
    @JvmStatic
    fun clear() = SENT.clear()
  }
}

/**
 * The clicks SandFly sent, by the [clock] time they were sent and carried out, with the state said
 * for each. Apps send a click's events with the time they sent them, and send its click event while
 * carrying it out.
 */
class SentClicks(private val clock: () -> Long) {
  class Click(val start: Long, val end: Long) {
    /** The state said as soon as the click was carried out, or null. */
    var said: String? = null
  }

  private val clicks = RecentItems<Click>(MAX_CLICKS, MAX_AGE_MS, clock)

  fun add(start: Long, end: Long): Click = Click(start, end).also { clicks.add(it) }

  fun isEmpty(): Boolean = clicks.isEmpty()

  /** Whether an event sent at [eventTime] was sent while a click was carried out. */
  fun isSentDuring(eventTime: Long): Boolean =
    clicks.find { eventTime >= it.start - CLOCK_SLACK_MS && eventTime <= it.end + CLOCK_SLACK_MS } !=
      null

  /**
   * Whether [text], reported at [eventTime] by an event sent while or soon after a click was
   * carried out, is the state said for that click.
   */
  fun isSaid(eventTime: Long, text: String): Boolean =
    clicks.find {
      val said = it.said
      said != null &&
        eventTime >= it.start - CLOCK_SLACK_MS &&
        eventTime <= it.end + REPORT_MS &&
        said.trim().equals(text.trim(), ignoreCase = true)
    } != null

  fun clear() = clicks.removeAll { true }

  private companion object {
    // Event times and the clock are both whole milliseconds of uptime.
    const val CLOCK_SLACK_MS = 1L
    // Apps report a state change up to 100 ms after it, and some a frame or two after that.
    const val REPORT_MS = 600L
    const val MAX_CLICKS = 8
    // The click event is handled 150 ms after it arrives, and a busy app holds it back further.
    const val MAX_AGE_MS = 10_000L
  }
}
