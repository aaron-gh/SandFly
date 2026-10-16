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
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityEventUtils

/** What Backtalk already did for an app's focus event before it arrived. */
enum class EarlyFocusMatch {
  /** Nothing: the event is handled as usual. */
  NONE,

  /** Its focus was spoken early, and the state that follows focus updated, so it is not spoken. */
  SPOKEN,

  /**
   * The state that follows focus was updated for it, but speaking it early failed, so it is spoken
   * without updating the state again.
   */
  STATE_UPDATED,

  /**
   * It was sent before the latest focus handled early was set, so it is out of date, and speaking
   * it would cut off the newer focus.
   */
  STALE,
}

/**
 * Remembers the nodes whose accessibility focus was spoken as soon as Backtalk focused them, so
 * that the app's focus events for them, which come later, are not spoken again.
 *
 * Each node is remembered for one focus event, for at most [MAX_WAIT_MS].
 */
class EarlyFocusSpeech(clock: () -> Long = SystemClock::uptimeMillis) {
  private val records =
    EarlyFocusRecords<AccessibilityNodeInfoCompat>(MAX_NODES, MAX_WAIT_MS, clock)

  /**
   * Remembers that [node]'s focus has been spoken, after the focus action that started at
   * [actionTime], in [SystemClock.uptimeMillis] time.
   */
  fun addSpoken(node: AccessibilityNodeInfoCompat, actionTime: Long) =
    records.addSpoken(node, actionTime)

  /**
   * Remembers that the state that follows focus was updated for [node], after the focus action that
   * started at [actionTime], but speaking it failed.
   */
  fun addStateUpdated(node: AccessibilityNodeInfoCompat, actionTime: Long) =
    records.addStateUpdated(node, actionTime)

  /**
   * Forgets [node], which Backtalk has focused again without speaking it early, such as by touch,
   * so that its next focus event is spoken.
   */
  fun forget(node: AccessibilityNodeInfoCompat) = records.forget(node)

  /** Forgets every node, such as when the events waiting to be spoken are thrown away. */
  fun clear() = records.clear()

  /** Whether the state that follows focus has already been updated for [event], or it is stale. */
  fun isHandled(event: AccessibilityEvent): Boolean =
    match(event, consume = false) != EarlyFocusMatch.NONE

  /**
   * Returns what was already done for [event], and forgets the node it matched, so that a later
   * focus event for the same node is handled as usual.
   */
  fun consume(event: AccessibilityEvent): EarlyFocusMatch = match(event, consume = true)

  private fun match(event: AccessibilityEvent, consume: Boolean): EarlyFocusMatch {
    if (event.eventType != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      return EarlyFocusMatch.NONE
    }
    // Only ask the event for its source, a round trip to the app, when a node could match it.
    val source = if (records.isEmpty()) null else AccessibilityEventUtils.sourceCompat(event)
    return records.match(source, event.eventTime, consume)
  }

  companion object {
    /** How long the app's focus event may take to arrive. */
    const val MAX_WAIT_MS = 2_000L

    /** How many nodes are remembered at once, for swipes faster than the app's events. */
    private const val MAX_NODES = 4
  }
}

/** The rules of [EarlyFocusSpeech], for any kind of [item], so that they can be tested. */
class EarlyFocusRecords<T>(maxItems: Int, maxAgeMs: Long, clock: () -> Long) {
  private class Entry<T>(val item: T, val spoken: Boolean, val actionTime: Long)

  private val entries = RecentItems<Entry<T>>(maxItems, maxAgeMs, clock)

  /** When the latest focus action handled early started, or 0 if none has. */
  private var lastActionTime = 0L

  fun addSpoken(item: T, actionTime: Long) = add(Entry(item, spoken = true, actionTime))

  fun addStateUpdated(item: T, actionTime: Long) = add(Entry(item, spoken = false, actionTime))

  /**
   * Each item has at most one entry, for its latest focus. The event of an earlier focus of it is
   * then stale, as that focus moved away before the latest one.
   */
  private fun add(entry: Entry<T>) {
    forget(entry.item)
    entries.add(entry)
    lastActionTime = maxOf(lastActionTime, entry.actionTime)
  }

  fun forget(item: T) = entries.removeAll { it.item == item }

  fun clear() = entries.clear()

  fun isEmpty(): Boolean = entries.isEmpty()

  /**
   * Returns what was already done for the focus event of [item] (null when not looked up) sent at
   * [eventTime], and, if [consume], forgets the entry it matched.
   *
   * The app sends its focus event while Backtalk performs the focus action, so after the action
   * started. An event sent before the latest focus action handled early started is stale: that
   * action took the focus away from its node. An [eventTime] of 0 is unknown.
   */
  fun match(item: T?, eventTime: Long, consume: Boolean): EarlyFocusMatch {
    if (item != null) {
      val matches: (Entry<T>) -> Boolean = {
        it.item == item && (eventTime <= 0 || eventTime >= it.actionTime)
      }
      val entry = if (consume) entries.removeFirst(matches) else entries.find(matches)
      if (entry != null) {
        return if (entry.spoken) EarlyFocusMatch.SPOKEN else EarlyFocusMatch.STATE_UPDATED
      }
    }
    return if (eventTime > 0 && eventTime < lastActionTime) {
      EarlyFocusMatch.STALE
    } else {
      EarlyFocusMatch.NONE
    }
  }
}

/**
 * Items remembered for at most [maxAgeMs] each, and at most [maxItems] at once, the oldest
 * forgotten first.
 */
class RecentItems<T>(
  private val maxItems: Int,
  private val maxAgeMs: Long,
  private val clock: () -> Long,
) {
  private class Entry<T>(val item: T, val time: Long)

  private val entries = ArrayDeque<Entry<T>>()

  fun add(item: T) {
    removeExpired()
    entries.addLast(Entry(item, clock()))
    while (entries.size > maxItems) entries.removeFirst()
  }

  fun isEmpty(): Boolean {
    removeExpired()
    return entries.isEmpty()
  }

  fun contains(item: T): Boolean = find { it == item } != null

  /** Returns the oldest item that matches [predicate], or null. */
  fun find(predicate: (T) -> Boolean): T? {
    removeExpired()
    return entries.firstOrNull { predicate(it.item) }?.item
  }

  /** Forgets the oldest entry equal to [item], and returns whether there was one. */
  fun remove(item: T): Boolean = removeFirst { it == item } != null

  /** Forgets the oldest item that matches [predicate], and returns it, or null if none did. */
  fun removeFirst(predicate: (T) -> Boolean): T? {
    removeExpired()
    val index = entries.indexOfFirst { predicate(it.item) }
    if (index < 0) return null
    return entries.removeAt(index).item
  }

  /** Forgets every item that matches [predicate]. */
  fun removeAll(predicate: (T) -> Boolean) {
    entries.removeAll { predicate(it.item) }
  }

  fun clear() = entries.clear()

  private fun removeExpired() {
    val now = clock()
    while (entries.isNotEmpty() && now - entries.first().time > maxAgeMs) entries.removeFirst()
  }
}
