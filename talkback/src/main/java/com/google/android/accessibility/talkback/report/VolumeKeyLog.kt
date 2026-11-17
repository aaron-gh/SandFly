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

import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The last volume key events the screen reader received, whether each was kept from Android, and
 * why presses passed, for problem reports. On some phones the log reaches back only a few seconds
 * at Debug level, so a report sent after the volume key commands stopped working has none of the
 * presses in it. Kept in memory only.
 */
object VolumeKeyLog {
  @VisibleForTesting internal const val MAX_ENTRIES = 60

  private val entries = ArrayDeque<String>()

  /**
   * Records [event], a volume key event, and whether it was [kept] from Android, with [state], the
   * state of the volume key presses after it.
   */
  @JvmStatic
  fun onKeyEvent(event: KeyEvent, kept: Boolean, state: String) {
    val action =
      when (event.action) {
        KeyEvent.ACTION_DOWN -> if (event.repeatCount > 0) "repeat ${event.repeatCount}" else "down"
        KeyEvent.ACTION_UP -> "up"
        else -> "action ${event.action}"
      }
    add(
      "${KeyEvent.keyCodeToString(event.keyCode)} $action " +
        "${if (kept) "kept" else "passed to Android"}; $state"
    )
  }

  /** Records [text], such as why presses passed to Android or the command a press ran. */
  @JvmStatic
  fun note(text: String) {
    add(text)
  }

  @VisibleForTesting
  internal fun add(text: String) {
    val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())
    synchronized(entries) {
      entries.addLast("$time $text")
      while (entries.size > MAX_ENTRIES) entries.removeFirst()
    }
  }

  /** The entries, oldest first, or null if there are none. */
  @JvmStatic
  fun read(): String? =
    synchronized(entries) { if (entries.isEmpty()) null else entries.joinToString("\n") }

  @VisibleForTesting
  internal fun clear() {
    synchronized(entries) { entries.clear() }
  }
}
