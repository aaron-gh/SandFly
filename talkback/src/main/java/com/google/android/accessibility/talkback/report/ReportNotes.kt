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
package com.google.android.accessibility.talkback.report

import androidx.annotation.VisibleForTesting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The last notes of one kind for problem reports, whatever the log level, kept in memory only. A
 * note the same as the one before it counts as a repeat of it, so a check that runs on every event
 * fills one line.
 */
class ReportNotes(@VisibleForTesting internal val maxEntries: Int) {
  private class Entry(val time: String, val text: String, var repeats: Int = 0)

  private val entries = ArrayDeque<Entry>()

  fun add(text: String) {
    synchronized(entries) {
      val last = entries.lastOrNull()
      if (last != null && last.text == text) {
        last.repeats++
        return
      }
      val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())
      entries.addLast(Entry(time, text))
      while (entries.size > maxEntries) entries.removeFirst()
    }
  }

  /** The notes, oldest first, or null if there are none. */
  fun read(): String? =
    synchronized(entries) {
      if (entries.isEmpty()) null
      else
        entries.joinToString("\n") {
          "${it.time} ${it.text}" + if (it.repeats > 0) " (${it.repeats + 1} times)" else ""
        }
    }

  @VisibleForTesting
  fun clear() {
    synchronized(entries) { entries.clear() }
  }

  companion object {
    private const val MAX_ENTRIES = 40

    /**
     * Whether a list item activated before Back was kept and focused again after it, and why not.
     */
    @JvmField val focusAfterBack = ReportNotes(MAX_ENTRIES)

    /** How long the tests of macros' Show in Actions only when conditions took, per item. */
    @JvmField val macroConditions = ReportNotes(MAX_ENTRIES)
  }
}
