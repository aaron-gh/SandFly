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

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Keeps the screen on while SandFly speaks an item or reads continuously, through a window with
 * FLAG_KEEP_SCREEN_ON that the service shows. Screen wake locks don't keep the screen on on some
 * Samsung phones.
 *
 * Requests can come from any thread; they run on the main thread in the order they were made.
 * Nothing happens while no window is attached.
 */
object KeepScreenOn {
  /** Speech of an item, such as the focused item. */
  const val SPEECH = "speech"

  /** Continuous reading. */
  const val READING = "reading"

  /** The longest one request keeps the screen on. */
  const val LIMIT_MS = 10 * 60 * 1000L

  private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

  @Volatile private var keeper: ScreenOnKeeper? = null
  private var timer: HandlerTimer? = null

  /**
   * Starts keeping the screen on with [window] when asked, while the screen is [screenOn]. Called
   * on the main thread when the service starts.
   */
  @JvmStatic
  fun attach(window: ScreenOnKeeper.Window, screenOn: Boolean) {
    detach()
    val newTimer = HandlerTimer(handler)
    val newKeeper = ScreenOnKeeper(window, newTimer) { SystemClock.uptimeMillis() }
    newTimer.onTimer = Runnable { if (keeper === newKeeper) newKeeper.onTimer() }
    newKeeper.setScreenOn(screenOn)
    timer = newTimer
    keeper = newKeeper
  }

  /** Removes the window and stops keeping the screen on. Called on the main thread. */
  @JvmStatic
  fun detach() {
    val old = keeper ?: return
    keeper = null
    old.releaseAll()
    timer?.cancel()
    timer = null
  }

  /** Keeps the screen on for [owner], for at most [limitMs] from now. */
  @JvmStatic
  @JvmOverloads
  fun request(owner: String, limitMs: Long = LIMIT_MS) {
    post { it.request(owner, limitMs) }
  }

  /** Ends the request of [owner]. */
  @JvmStatic
  fun release(owner: String) {
    post { it.release(owner) }
  }

  /** Takes the screen state. Called on the main thread. */
  @JvmStatic
  fun setScreenOn(on: Boolean) {
    keeper?.setScreenOn(on)
  }

  /** Takes whether SandFly is paused. Called on the main thread. */
  @JvmStatic
  fun setPaused(paused: Boolean) {
    keeper?.setPaused(paused)
  }

  private inline fun post(crossinline action: (ScreenOnKeeper) -> Unit) {
    if (keeper == null) {
      return
    }
    handler.post { keeper?.let(action) }
  }

  /** Runs [onTimer] on the main thread after a delay. */
  private class HandlerTimer(private val handler: Handler) : ScreenOnKeeper.Timer {
    var onTimer: Runnable? = null

    override fun schedule(delayMs: Long) {
      val runnable = onTimer ?: return
      handler.removeCallbacks(runnable)
      handler.postDelayed(runnable, delayMs.coerceAtLeast(0))
    }

    override fun cancel() {
      onTimer?.let { handler.removeCallbacks(it) }
    }
  }
}
