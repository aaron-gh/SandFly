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

package com.google.android.accessibility.talkback.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Counts the network requests that need [NetworkForegroundService] and follows the service's state,
 * so that it starts with the first request and stops when the last one ends.
 */
internal class ForegroundLeases {
  enum class Phase {
    /** Not running, or no longer in the foreground. */
    IDLE,
    /** Asked to start, and not yet in the foreground. */
    STARTING,
    FOREGROUND,
  }

  enum class Acquired {
    /** Not needed: the request runs as it is. */
    NONE,
    /** The caller starts the service, then waits for [awaitStarted]. */
    START,
    /** The service is starting or running; the caller waits for [awaitStarted]. */
    JOIN,
  }

  private var count = 0
  private var phase = Phase.IDLE
  private var started = CountDownLatch(0)

  @get:Synchronized
  val phaseNow: Phase
    get() = phase

  @get:Synchronized
  val countNow: Int
    get() = count

  /**
   * Counts a request. With none running and the service idle, the service is needed only while the
   * network is [blocked].
   */
  @Synchronized
  fun acquire(blocked: Boolean): Acquired {
    if (count == 0 && phase == Phase.IDLE) {
      if (!blocked) return Acquired.NONE
      count = 1
      phase = Phase.STARTING
      started = CountDownLatch(1)
      return Acquired.START
    }
    count++
    return Acquired.JOIN
  }

  /** The service could not be started at all. The request that counted it still holds its count. */
  @Synchronized
  fun onStartFailed() {
    if (phase == Phase.STARTING) phase = Phase.IDLE
    started.countDown()
  }

  /** Ends a request. Returns true when the service should stop. */
  @Synchronized
  fun release(): Boolean {
    if (count == 0) return false
    count--
    return count == 0 && phase == Phase.FOREGROUND
  }

  /** The service started: returns whether it should go into the foreground. */
  @Synchronized
  fun onServiceStarted(): Boolean {
    if (count > 0) return true
    phase = Phase.IDLE
    started.countDown()
    return false
  }

  /**
   * The service is in the foreground, or failed to be. Returns true when it should stop at once,
   * because the requests that needed it ended while it was starting.
   */
  @Synchronized
  fun onForeground(success: Boolean): Boolean {
    val stop = success && count == 0
    phase = if (success && !stop) Phase.FOREGROUND else Phase.IDLE
    started.countDown()
    return stop
  }

  /** Returns true, and marks the service idle, when no request needs it any more. */
  @Synchronized
  fun stopIfIdle(): Boolean {
    if (count > 0 || phase != Phase.FOREGROUND) return false
    phase = Phase.IDLE
    return true
  }

  /**
   * The service left the foreground or was destroyed. A start asked for meanwhile stays pending:
   * Android runs it in a new instance.
   */
  @Synchronized
  fun onServiceGone() {
    if (phase == Phase.FOREGROUND) phase = Phase.IDLE
  }

  /** Waits up to [timeoutMs] for the service to be in the foreground or to have failed. */
  fun awaitStarted(timeoutMs: Long): Boolean {
    val latch = synchronized(this) { started }
    return try {
      latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      false
    }
  }
}
