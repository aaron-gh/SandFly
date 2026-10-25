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

package com.google.android.accessibility.talkback.notifications

import androidx.annotation.MainThread

/**
 * The notifications that arrived lately, read or not, with why each was not read. It is kept in
 * memory only, so it is gone when SandFly stops, and holds at most [MAX] notifications.
 */
object NotificationHistory {
  const val MAX = 100

  /** One notification that arrived. */
  data class Entry(
    val info: NotificationInfo,
    val arrivedAtMillis: Long,
    val outcome: Outcome,
    /** What was said, or what would have been. */
    val spoken: String,
    val rules: List<String>,
  )

  private val entries = ArrayDeque<Entry>()
  private val listeners = mutableListOf<() -> Unit>()

  /** The entries, newest first. */
  @MainThread fun all(): List<Entry> = entries.toList().asReversed()

  /** The newest notification that was read. */
  @MainThread
  fun lastRead(): Entry? =
    entries.lastOrNull { it.outcome == Outcome.READ || it.outcome == Outcome.READ_IN_SANDFLY_VOICE }

  @MainThread
  fun add(entry: Entry) {
    entries.addLast(entry)
    while (entries.size > MAX) entries.removeFirst()
    notifyListeners()
  }

  /** Changes the outcome of the newest entry of [info], such as one left unread after all. */
  @MainThread
  fun mark(info: NotificationInfo, outcome: Outcome) {
    val index = entries.indexOfLast { it.info === info }
    if (index < 0) return
    entries[index] = entries[index].copy(outcome = outcome)
    notifyListeners()
  }

  @MainThread
  fun clear() {
    entries.clear()
    notifyListeners()
  }

  @MainThread fun addListener(listener: () -> Unit) {
    listeners += listener
  }

  @MainThread fun removeListener(listener: () -> Unit) {
    listeners -= listener
  }

  private fun notifyListeners() {
    listeners.toList().forEach { it() }
  }
}
