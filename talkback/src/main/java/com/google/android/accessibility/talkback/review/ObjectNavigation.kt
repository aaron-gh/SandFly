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
package com.google.android.accessibility.talkback.review

import android.accessibilityservice.AccessibilityService
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut as Key
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FocusFinder
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils

/**
 * Moves SandFly focus through the tree of items as apps build it: to the item that contains the
 * focused one, the first item it contains, and the items beside it, including containers that
 * focus otherwise skips. Also moves input focus and SandFly focus together to the next or previous
 * item that can take input focus, as Tab does on a computer.
 */
class ObjectNavigation(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val focusMonitor: AccessibilityFocusMonitor,
) {
  /** Runs an object navigation keyboard action. Returns false for actions it doesn't have. */
  fun perform(action: TalkBackPhysicalKeyboardShortcut, eventId: EventId?): Boolean {
    when (action) {
      Key.OBJECT_PARENT -> parent(eventId)
      Key.OBJECT_FIRST_CHILD -> firstChild(eventId)
      Key.OBJECT_NEXT -> sibling(forward = true, eventId)
      Key.OBJECT_PREVIOUS -> sibling(forward = false, eventId)
      Key.NAVIGATE_NEXT_FOCUSABLE -> focusable(forward = true, eventId)
      Key.NAVIGATE_PREVIOUS_FOCUSABLE -> focusable(forward = false, eventId)
      else -> return false
    }
    return true
  }

  /** SandFly focus, or input focus when there is none, or the active window. */
  private fun current(): AccessibilityNodeInfoCompat? =
    focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
      ?: focusMonitor.inputFocus
      ?: service.rootInActiveWindow?.let { AccessibilityNodeInfoCompat.wrap(it) }

  private fun parent(eventId: EventId?) {
    var node = current()?.parent
    while (node != null && !node.isVisibleToUser) {
      node = node.parent
    }
    if (node == null) {
      speak(R.string.object_navigation_no_parent, eventId)
      return
    }
    focus(node, eventId)
  }

  private fun firstChild(eventId: EventId?) {
    val node = current()
    val child = node?.let { visibleChildren(it).firstOrNull() }
    if (child == null) {
      speak(R.string.object_navigation_no_child, eventId)
      return
    }
    focus(child, eventId)
  }

  private fun sibling(forward: Boolean, eventId: EventId?) {
    val node = current()
    val siblings = node?.parent?.let { visibleChildren(it) }.orEmpty()
    val index = siblings.indexOf(node)
    val target = if (index < 0) null else siblings.getOrNull(if (forward) index + 1 else index - 1)
    if (target == null) {
      speak(
        if (forward) R.string.object_navigation_no_next else R.string.object_navigation_no_previous,
        eventId,
      )
      return
    }
    focus(target, eventId)
  }

  private fun visibleChildren(node: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> =
    (0 until node.childCount).mapNotNull { node.getChild(it) }.filter { it.isVisibleToUser }

  /**
   * Moves to the next or previous item that can take input focus, in reading order, going round
   * the window at its ends.
   */
  private fun focusable(forward: Boolean, eventId: EventId?) {
    val start = current() ?: return
    val root = AccessibilityNodeInfoUtils.getRoot(start) ?: return
    val direction =
      if (forward) TraversalStrategy.SEARCH_FOCUS_FORWARD else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val strategy = TraversalStrategyUtils.getTraversalStrategy(root, FocusFinder(service), direction)
    val filter =
      object : Filter<AccessibilityNodeInfoCompat>() {
        override fun accept(node: AccessibilityNodeInfoCompat?): Boolean =
          node != null &&
            node.isVisibleToUser &&
            node.isEnabled &&
            AccessibilityNodeInfoUtils.isFocusable(node)
      }
    val target =
      TraversalStrategyUtils.searchFocus(strategy, start, direction, filter)
        ?: (strategy as? OrderedTraversalStrategy)?.dumpTree()?.let { order ->
          if (forward) order.firstOrNull { filter.accept(it) } else order.lastOrNull { filter.accept(it) }
        }
    if (target == null || target == start && !filter.accept(start)) {
      speak(R.string.no_focusable_item, eventId)
      return
    }
    // SandFly focus last, so that what it says interrupts what input focus says.
    target.performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS)
    focus(target, eventId)
  }

  private fun focus(node: AccessibilityNodeInfoCompat, eventId: EventId?) {
    pipeline.returnFeedback(
      eventId,
      Feedback.focus(
          node,
          FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(),
        )
        .setForceRefocus(true),
    )
    // A container with no name of its own says nothing else, so it says what it is.
    val role = Role.getRole(node)
    if (
      node.text.isNullOrBlank() &&
        node.contentDescription.isNullOrBlank() &&
        (role == Role.ROLE_NONE || role == Role.ROLE_VIEW_GROUP)
    ) {
      val count = visibleChildren(node).size
      if (count == 0) return
      pipeline.returnFeedback(
        eventId,
        Feedback.speech(
          service.resources.getQuantityString(R.plurals.object_navigation_group, count, count),
          QUEUED_OPTIONS,
        ),
      )
    }
  }

  private fun speak(textRes: Int, eventId: EventId?) {
    pipeline.returnFeedback(eventId, Feedback.speech(service.getString(textRes), SPEAK_OPTIONS))
  }

  private companion object {
    val QUEUED_OPTIONS: SpeakOptions =
      SpeakOptions.create()
        .setQueueMode(SpeechController.QUEUE_MODE_QUEUE)
        .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)

    val SPEAK_OPTIONS: SpeakOptions =
      SpeakOptions.create()
        .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
        .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)
  }
}
