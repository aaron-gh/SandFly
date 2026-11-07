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

package com.google.android.accessibility.talkback.clipboard

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.os.HandlerCompat

/**
 * The Clipboard history's paste into an edit box, tried again every [INTERVAL_MS] for
 * [DURATION_MS]: the edit box's app reads the clipboard only once its window is back and has focus.
 * Starting again cancels the attempts before.
 */
internal object PasteAttempts {
  const val INTERVAL_MS = 200L
  const val DURATION_MS = 3000L

  private val handler = Handler(Looper.getMainLooper())
  private val token = Any()

  /** Runs [step] after [delayMs], cancelling the attempts before. */
  fun after(delayMs: Long, step: () -> Unit) {
    cancel()
    post(delayMs, step)
  }

  /**
   * Runs [attempt] after [delayMs], and again every [INTERVAL_MS] for [DURATION_MS] while it fails,
   * then [failed]. Stops without [failed] once [active] is false.
   */
  fun start(delayMs: Long, active: () -> Boolean, attempt: () -> Boolean, failed: () -> Unit) {
    cancel()
    val deadline = SystemClock.uptimeMillis() + delayMs + DURATION_MS
    post(delayMs) { run(deadline, active, attempt, failed) }
  }

  /** Cancels the attempts not run yet. */
  fun cancel() {
    handler.removeCallbacksAndMessages(token)
  }

  private fun run(deadline: Long, active: () -> Boolean, attempt: () -> Boolean, failed: () -> Unit) {
    if (!active() || attempt()) return
    if (SystemClock.uptimeMillis() + INTERVAL_MS > deadline) {
      failed()
    } else {
      post(INTERVAL_MS) { run(deadline, active, attempt, failed) }
    }
  }

  private fun post(delayMs: Long, block: () -> Unit) {
    HandlerCompat.postDelayed(handler, Runnable { block() }, token, delayMs)
  }
}
