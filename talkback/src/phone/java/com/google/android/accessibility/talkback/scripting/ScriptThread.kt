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

import android.os.SystemClock

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
  private val depth = ThreadLocal<Int>()

  fun <T> busy(work: () -> T): T {
    val outer = depth.get() ?: 0
    val thread = Thread.currentThread()
    if (outer == 0) {
      owner = thread
      busySince = SystemClock.uptimeMillis()
    }
    depth.set(outer + 1)
    try {
      return work()
    } finally {
      depth.set(outer)
      if (outer == 0 && owner === thread) {
        busySince = 0L
        owner = null
      }
    }
  }

  fun heldUpFor(ms: Long): Boolean {
    val since = busySince
    return since != 0L && SystemClock.uptimeMillis() - since > ms
  }
}
