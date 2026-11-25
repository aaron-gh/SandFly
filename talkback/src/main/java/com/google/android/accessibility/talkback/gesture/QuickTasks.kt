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

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.R

/**
 * Quick Tasks, as in the BlackBerry 10 Screen Reader: for [TIMEOUT_MS] after the Quick Tasks
 * command, a few gestures change the speech or the screen instead of running their commands. One
 * finger up and down changes the speech volume, right and left the speech rate; two fingers up
 * repeat the last thing spoken, down spell it; a two-finger tap hides or shows the screen. Each of
 * them gives another [TIMEOUT_MS]. Any other gesture ends Quick Tasks and runs its own command;
 * running out of time ends them with a vibration.
 */
object QuickTasks {

  const val TIMEOUT_MS = 3000L

  private val TASKS =
    listOf(
      R.string.pref_shortcut_up_key to R.string.shortcut_value_increase_volume,
      R.string.pref_shortcut_down_key to R.string.shortcut_value_decrease_volume,
      R.string.pref_shortcut_right_key to R.string.shortcut_value_speech_rate_increase,
      R.string.pref_shortcut_left_key to R.string.shortcut_value_speech_rate_decrease,
      R.string.pref_shortcut_2finger_swipe_up_key to R.string.shortcut_value_repeat_last_spoken_phrase,
      R.string.pref_shortcut_2finger_swipe_down_key to R.string.shortcut_value_spell_last_spoken_phrase,
      R.string.pref_shortcut_2finger_1tap_key to R.string.shortcut_value_show_hide_screen,
    )

  private val handler by lazy { Handler(Looper.getMainLooper()) }

  @Volatile private var tasks: Map<String, String> = emptyMap()
  @Volatile private var until = 0L
  private var onTimeout: Runnable? = null
  private val timeout = Runnable {
    if (until != 0L) {
      until = 0L
      val callback = onTimeout
      onTimeout = null
      callback?.run()
    }
  }

  /** Starts Quick Tasks; [onTimeout] runs on the main thread if they end by running out of time. */
  @JvmStatic
  fun start(context: Context, onTimeout: Runnable) {
    if (tasks.isEmpty()) {
      tasks = TASKS.associate { (key, value) -> context.getString(key) to context.getString(value) }
    }
    this.onTimeout = onTimeout
    extend(SystemClock.uptimeMillis())
  }

  /** Whether Quick Tasks are on. */
  @VisibleForTesting internal fun isActive(): Boolean = isActive(SystemClock.uptimeMillis())

  private fun isActive(now: Long): Boolean = until != 0L && now < until

  /**
   * Returns the command that the gesture with preference key [key] runs in Quick Tasks, which gives
   * them more time, or null when Quick Tasks are off or it isn't one of theirs, which ends them.
   */
  @JvmStatic fun action(key: String?): String? = action(key, SystemClock.uptimeMillis())

  @VisibleForTesting
  internal fun action(key: String?, now: Long): String? {
    if (!isActive(now)) return null
    val task = key?.let { tasks[it] }
    if (task == null) {
      stop()
      return null
    }
    extend(now)
    return task
  }

  /** Ends Quick Tasks without the vibration, as when the service stops. */
  @JvmStatic
  fun stop() {
    until = 0L
    onTimeout = null
    handler.removeCallbacks(timeout)
  }

  private fun extend(now: Long) {
    until = now + TIMEOUT_MS
    handler.removeCallbacks(timeout)
    handler.postDelayed(timeout, TIMEOUT_MS)
  }
}
