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

package com.google.android.accessibility.talkback.compositor

import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The announcements of the nodes that a swipe from the focused node would most likely reach,
 * worked out while the user listens, so that a swipe can speak one without working it out then.
 *
 * They hold for the focused node only, and are thrown away when the focus moves, when the window
 * shows another screen, and after [MAX_AGE_MS]. Any other event that can change a node in their
 * window, even a text, marks them as changed: one is then used only if the node, read again from
 * the app, has the same [NodeFingerprint] as when it was prepared.
 *
 * Filter logcat by the "PreparedFocusSpeech" tag in debug builds to see how often they are used.
 */
object PreparedFocusSpeech {
  private const val TAG = "PreparedFocusSpeech"

  /** How long a prepared announcement is kept, in case a change came without an event. */
  const val MAX_AGE_MS = 5_000L

  /**
   * How long before a swipe an announcement must have been prepared to be used. One prepared just
   * before may predate a change whose event has not arrived yet, so the swipe reads the node again.
   */
  const val MIN_AGE_MS = 100L

  private const val NO_WINDOW_ID = -1

  /**
   * An announcement of focusing [node], which was read from the app to prepare it, and
   * [containerTitle], the title of the container that working it out left focus in, which giving it
   * must leave focus in too. [fingerprint] is the node's when it was prepared, or null if it has
   * none, see [NodeFingerprint.of].
   */
  class Prepared
  internal constructor(
    val node: AccessibilityNodeInfoCompat,
    val feedback: EventFeedback,
    val containerTitle: CharSequence,
    internal val fingerprint: List<Any?>?,
    internal val time: Long,
    /** Whether [node] was read again after a change and found to say the same. */
    val isCheckedAfterChange: Boolean,
  ) {
    constructor(
      node: AccessibilityNodeInfoCompat,
      feedback: EventFeedback,
      containerTitle: CharSequence,
      fingerprint: List<Any?>?,
    ) : this(
      node,
      feedback,
      containerTitle,
      fingerprint,
      SystemClock.uptimeMillis(),
      isCheckedAfterChange = false,
    )

    internal fun isFor(other: AccessibilityNodeInfoCompat): Boolean = node == other
  }

  private val prepared = ArrayList<Prepared>(2)
  private var windowId = NO_WINDOW_ID

  /** What changed in the window since the announcements were prepared, or null if nothing has. */
  private var changedBy: String? = null

  /** Why the announcements were last thrown away, for the log. */
  private var lastClearReason = "start"
  private var used = 0
  private var missed = 0

  /** Keeps [feedback] as the announcement of focusing [node]; see [Prepared]. */
  @JvmStatic
  fun put(
    node: AccessibilityNodeInfoCompat,
    feedback: EventFeedback,
    containerTitle: CharSequence,
    fingerprint: List<Any?>?,
  ) {
    if (node.windowId != windowId) {
      clear("window changed")
      windowId = node.windowId
    }
    if (changedBy != null) {
      // This one is read after the change, so it needs no check. Those prepared before still do,
      // and are thrown away, so that the change holds for all that are kept.
      prepared.clear()
      changedBy = null
    }
    prepared.removeAll { it.isFor(node) }
    prepared += Prepared(node, feedback, containerTitle, fingerprint)
  }

  /**
   * Returns whether an announcement of focusing [node] is kept that needs no check, so that it
   * need not be prepared again.
   */
  @JvmStatic
  fun has(node: AccessibilityNodeInfoCompat): Boolean = changedBy == null && find(node) != null

  /**
   * Returns the announcement of focusing [node], or null if there is none, it was prepared less
   * than [MIN_AGE_MS] ago, or its node, read again after a change in its window, no longer says
   * the same. Keeps them all: the caller clears them once the focus moves.
   */
  @JvmStatic
  fun peek(node: AccessibilityNodeInfoCompat): Prepared? = usable(node)

  /** Whether an announcement prepared at [preparedTime] may be used at [now]; see [MIN_AGE_MS]. */
  @JvmStatic
  fun isOldEnough(preparedTime: Long, now: Long): Boolean = now - preparedTime >= MIN_AGE_MS

  /** Forgets all announcements, for [reason], which the log gives. */
  @JvmStatic
  fun clear(reason: String) {
    if (prepared.isNotEmpty()) {
      lastClearReason = reason
      LogUtils.d(TAG, "Prepared speech thrown away: %s", reason)
    }
    prepared.clear()
    changedBy = null
    windowId = NO_WINDOW_ID
  }

  /**
   * Forgets all announcements if [event] shows another screen, and marks them as changed if it can
   * change what a node in their window says, see [canChange].
   */
  @JvmStatic
  fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (prepared.isEmpty()) {
      return
    }
    val type = event.eventType
    if (!canChange(type, event.windowId, windowId)) {
      return
    }
    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      clear(if (LogUtils.shouldLog(Log.DEBUG)) describe(event) else "")
      return
    }
    if (changedBy == null) {
      val reason = if (LogUtils.shouldLog(Log.DEBUG)) describe(event) else ""
      changedBy = reason
      LogUtils.d(TAG, "Prepared speech to be checked before use: %s", reason)
    }
  }

  private fun describe(event: AccessibilityEvent): String =
    AccessibilityEvent.eventTypeToString(event.eventType) +
      " 0x" +
      Integer.toHexString(event.contentChangeTypes)

  /**
   * Whether an event of [type] from the window [eventWindowId] can change what a node in the window
   * [preparedWindowId] says. Unlike the saved reading order, any content change counts, even of
   * text or state alone.
   */
  @JvmStatic
  fun canChange(type: Int, eventWindowId: Int, preparedWindowId: Int): Boolean =
    type and TraversalTreeCache.IGNORED_EVENT_TYPES == 0 &&
      (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
        type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventWindowId == NO_WINDOW_ID ||
        eventWindowId == preparedWindowId)

  /** Forgets all announcements before SandFly performs [actionId], if it can change the nodes. */
  @JvmStatic
  fun onNodeAction(actionId: Int) {
    if (prepared.isNotEmpty() && TraversalTreeCache.changesNodes(actionId)) {
      clear(
        if (LogUtils.shouldLog(Log.DEBUG)) AccessibilityNodeInfoUtils.actionToString(actionId)
        else ""
      )
    }
  }

  /**
   * Notes that a swipe to [node] does not use a prepared announcement, for [reason], such as
   * moving to another row of a grid, whose sounds were not prepared.
   */
  @JvmStatic
  fun noteNotUsed(node: AccessibilityNodeInfoCompat, reason: String) {
    if (find(node) != null) {
      logNotUsed(reason)
    }
  }

  private fun usable(node: AccessibilityNodeInfoCompat): Prepared? {
    val found = find(node)
    if (found == null) {
      if (prepared.isEmpty()) {
        logNotUsed("none kept, last thrown away: ", lastClearReason)
      } else {
        logNotUsed("kept for other nodes")
      }
      return null
    }
    val now = SystemClock.uptimeMillis()
    if (!isOldEnough(found.time, now)) {
      logNotUsed("prepared ms before: ", now - found.time)
      return null
    }
    val change = changedBy
    if (change == null) {
      used++
      LogUtils.d(TAG, "Prepared speech used (%d used, %d not)", used, missed)
      return found
    }
    val checked = checked(found)
    if (checked == null) {
      logNotUsed("changed: ", change)
      return null
    }
    used++
    LogUtils.d(TAG, "Prepared speech used, same after %s (%d used, %d not)", change, used, missed)
    return checked
  }

  /** Counts and logs an announcement not used, for [reason], followed by [detail] if any. */
  private fun logNotUsed(reason: String, detail: Any = "") {
    missed++
    LogUtils.d(TAG, "Prepared speech not used: %s%s (%d used, %d not)", reason, detail, used, missed)
  }

  /**
   * Returns [found] for its node read again from the app, if that node has the fingerprint it had
   * when it was prepared, or else null.
   */
  private fun checked(found: Prepared): Prepared? {
    val fingerprint = found.fingerprint ?: return null
    @Suppress("DEPRECATION") // obtain(): copy, so that the kept node is left as it was.
    val node = AccessibilityNodeInfoCompat.obtain(found.node)
    val now =
      try {
        if (!node.refresh()) {
          return null
        }
        NodeFingerprint.of(node)
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Cannot check prepared speech: %s", e)
        return null
      }
    if (now != fingerprint) {
      return null
    }
    return Prepared(
      node,
      found.feedback,
      found.containerTitle,
      fingerprint,
      found.time,
      isCheckedAfterChange = true,
    )
  }

  private fun find(node: AccessibilityNodeInfoCompat): Prepared? {
    val now = SystemClock.uptimeMillis()
    prepared.removeAll { now - it.time > MAX_AGE_MS }
    return prepared.firstOrNull { it.isFor(node) }
  }
}
