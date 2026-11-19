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

package com.google.android.accessibility.utils

import java.util.function.LongSupplier

/**
 * Decides when the window that keeps the screen on is shown. Requests are made by owner, such as
 * speech or continuous reading, and each lasts until its owner releases it or until its time limit
 * passes. The window is shown while any request lasts. When the last one ends, the window is
 * removed after [HIDE_DELAY_MS], so it isn't removed and added again between items. Screen off,
 * pause and [releaseAll] drop every request and remove the window at once. Requests made while the
 * screen is off or while paused are ignored.
 *
 * Not thread-safe: calls come from one thread.
 */
class ScreenOnKeeper(
  private val window: Window,
  private val timer: Timer,
  private val clock: LongSupplier,
) {
  /** Shows or removes the window. */
  fun interface Window {
    fun setShown(shown: Boolean)
  }

  /** Calls [onTimer] once after a delay. Scheduling again replaces the pending call. */
  interface Timer {
    fun schedule(delayMs: Long)

    fun cancel()
  }

  // The time each request ends, by owner.
  private val deadlines = HashMap<String, Long>()
  private var screenOn = true
  private var paused = false
  private var shown = false
  // When the window is removed after the last request ended, or NONE.
  private var hideAtMs = NONE

  /** Whether the window is shown. */
  val isShown: Boolean
    get() = shown

  /** Keeps the screen on for [owner], for at most [limitMs] from now. Replaces its earlier limit. */
  fun request(owner: String, limitMs: Long) {
    if (!screenOn || paused || limitMs <= 0) {
      return
    }
    deadlines[owner] = clock.asLong + limitMs
    update(immediate = false)
  }

  /** Ends the request of [owner]. */
  fun release(owner: String) {
    if (deadlines.remove(owner) != null) {
      update(immediate = false)
    }
  }

  /** Ends every request and removes the window at once. */
  fun releaseAll() {
    deadlines.clear()
    update(immediate = true)
  }

  /** Takes the screen state. Turning off ends every request. */
  fun setScreenOn(on: Boolean) {
    screenOn = on
    if (!on) {
      releaseAll()
    }
  }

  /** Takes whether SandFly is paused. Pausing ends every request. */
  fun setPaused(paused: Boolean) {
    this.paused = paused
    if (paused) {
      releaseAll()
    }
  }

  /** Called by [Timer] when a request may have ended or the window is due to be removed. */
  fun onTimer() {
    update(immediate = false)
  }

  private fun update(immediate: Boolean) {
    val now = clock.asLong
    deadlines.values.removeIf { it <= now }
    timer.cancel()
    if (deadlines.isNotEmpty()) {
      hideAtMs = NONE
      show(true)
      timer.schedule(deadlines.values.min() - now)
      return
    }
    if (!shown) {
      hideAtMs = NONE
      return
    }
    if (immediate) {
      hideAtMs = NONE
      show(false)
      return
    }
    if (hideAtMs == NONE) {
      hideAtMs = now + HIDE_DELAY_MS
    }
    if (now >= hideAtMs) {
      hideAtMs = NONE
      show(false)
    } else {
      timer.schedule(hideAtMs - now)
    }
  }

  private fun show(shown: Boolean) {
    if (this.shown == shown) {
      return
    }
    this.shown = shown
    window.setShown(shown)
  }

  companion object {
    /** How long the window stays after the last request ends. */
    const val HIDE_DELAY_MS = 2_000L

    private const val NONE = -1L
  }
}
