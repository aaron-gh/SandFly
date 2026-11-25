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
 * The last Enter presses on a physical keyboard, whether the screen reader took each or passed it
 * to the app, and the state that decided it, for problem reports. Records no typed text, only the
 * kinds and view IDs of the items with focus. Kept in memory only.
 */
object EnterKeyLog {
  @VisibleForTesting internal const val MAX_ENTRIES = 40

  private val entries = ArrayDeque<String>()

  /** Whether [keyCode] is logged: Enter, Numpad Enter or the centre of a D-pad. */
  @JvmStatic
  fun isLogged(keyCode: Int): Boolean =
    keyCode == KeyEvent.KEYCODE_ENTER ||
      keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
      keyCode == KeyEvent.KEYCODE_DPAD_CENTER

  /**
   * Records the press of [event], an Enter key, whether it was [taken] from the app, and [state],
   * the state after it.
   */
  @JvmStatic
  fun onKeyDown(event: KeyEvent, taken: Boolean, state: String) {
    add(
      "${KeyEvent.keyCodeToString(event.keyCode)} " +
        "${if (taken) "taken" else "passed to the app"}; $state"
    )
  }

  /** Records [text], such as why the user counts as typing again. */
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
