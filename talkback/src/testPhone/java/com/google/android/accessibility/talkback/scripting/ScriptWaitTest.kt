/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.scripting

import android.app.Application
import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ScriptWaitTest {
  private val handler = Handler(Looper.getMainLooper())

  @Test
  fun speechRequestRunsBeforeQueuedBackgroundWork() {
    val order = mutableListOf<String>()
    handler.post { order.add("background") }
    var result: String? = null
    val caller = Thread {
      result = handler.await(2000, "speech", atFrontOfQueue = true) {
        order.add("speech")
        "rewritten"
      }
    }
    caller.start()
    awaitWaiting(caller)
    shadowOf(handler.looper).idle()
    caller.join(2000)
    assertFalse(caller.isAlive)
    assertEquals("rewritten", result)
    assertEquals(listOf("speech", "background"), order)
  }

  @Test
  fun ordinaryRequestsKeepQueueOrder() {
    val order = mutableListOf<String>()
    handler.post { order.add("background") }
    val caller = Thread { handler.await(2000, "actions") { order.add("actions") } }
    caller.start()
    awaitWaiting(caller)
    shadowOf(handler.looper).idle()
    caller.join(2000)
    assertFalse(caller.isAlive)
    assertEquals(listOf("background", "actions"), order)
  }

  @Test
  fun timedOutRequestDoesNotRunWhenTheQueueResumes() {
    var called = false
    var result: String? = "pending"
    val caller = Thread {
      result = handler.await(10, "speech", atFrontOfQueue = true) {
        called = true
        "late"
      }
    }
    caller.start()
    caller.join(2000)
    assertFalse(caller.isAlive)
    shadowOf(handler.looper).idle()
    assertNull(result)
    assertFalse(called)
  }

  @Test
  fun sameThreadRequestDoesNotReenterTheRuntime() {
    var called = false
    assertNull(handler.await(30, "speech", atFrontOfQueue = true) { called = true })
    shadowOf(handler.looper).idle()
    assertFalse(called)
  }

  private fun awaitWaiting(thread: Thread) {
    val deadline = System.nanoTime() + 2_000_000_000L
    while (thread.isAlive && thread.state != Thread.State.TIMED_WAITING &&
      System.nanoTime() < deadline) {
      Thread.yield()
    }
    assertTrue("Caller must have queued its task and entered the bounded wait",
      thread.state == Thread.State.TIMED_WAITING)
  }
}
