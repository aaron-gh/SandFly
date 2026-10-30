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

package com.google.android.accessibility.talkback.scripting

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Knows whether the script thread is in the middle of something. The main thread waits a short
 * time for scripts before it speaks, and that wait is wasted when the thread is already held up by
 * a long command, timer or reply, so callers skip it then.
 */
object ScriptThread {
  @Volatile private var busySince = 0L
  // Restarting the engine starts a new script thread while the old one may still be finishing, so
  // only the thread that set the time clears it.
  @Volatile private var owner: Thread? = null
  /** The script the thread is busy for, if it is one script's work. */
  @Volatile private var script: String? = null
  private val depth = ThreadLocal<Int>()

  /**
   * Told on the main thread when the script thread has been busy with one piece of work for
   * [STUCK_MS], with the script it was for and the thread. The prelude's hook can't stop Lua inside
   * one C function, such as a pattern match that backtracks for good, so the thread can't be
   * stopped, only given up on.
   */
  @Volatile var onStuck: ((script: String?, thread: Thread) -> Unit)? = null

  /** Checks for a stuck thread only while it is busy. */
  private val main = Handler(Looper.getMainLooper())
  private val watching = AtomicBoolean(false)
  private val check =
    object : Runnable {
      override fun run() {
        val since = busySince
        val thread = owner
        if (since == 0L || thread == null) {
          watching.set(false)
          // Busy again since the check was found idle.
          if (busySince != 0L) watch()
          return
        }
        val elapsed = SystemClock.uptimeMillis() - since
        if (elapsed < STUCK_MS) {
          main.postDelayed(this, STUCK_MS - elapsed)
          return
        }
        watching.set(false)
        onStuck?.invoke(script, thread)
      }
    }

  private fun watch() {
    if (watching.compareAndSet(false, true)) main.postDelayed(check, STUCK_MS)
  }

  fun <T> busy(scriptId: String? = null, work: () -> T): T {
    val outer = depth.get() ?: 0
    val thread = Thread.currentThread()
    if (outer == 0) {
      owner = thread
      script = scriptId
      busySince = SystemClock.uptimeMillis()
      watch()
    }
    depth.set(outer + 1)
    try {
      return work()
    } finally {
      depth.set(outer)
      if (outer == 0 && owner === thread) {
        busySince = 0L
        owner = null
        script = null
      }
    }
  }

  fun heldUpFor(ms: Long): Boolean {
    val since = busySince
    return since != 0L && SystemClock.uptimeMillis() - since > ms
  }

  /** How long one piece of work may hold up the script thread before its script is turned off. */
  const val STUCK_MS = 20_000L
}
