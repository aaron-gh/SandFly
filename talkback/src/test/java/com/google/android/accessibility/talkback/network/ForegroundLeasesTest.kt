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

import com.google.android.accessibility.talkback.network.ForegroundLeases.Acquired
import com.google.android.accessibility.talkback.network.ForegroundLeases.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForegroundLeasesTest {
  private val leases = ForegroundLeases()

  @Test
  fun unblockedNetwork_needsNoService() {
    assertEquals(Acquired.NONE, leases.acquire(blocked = false))
    assertEquals(0, leases.countNow)
    assertEquals(Phase.IDLE, leases.phaseNow)
    assertFalse(leases.release())
  }

  @Test
  fun firstBlockedRequest_startsService_laterOnesJoin() {
    assertEquals(Acquired.START, leases.acquire(blocked = true))
    assertEquals(Phase.STARTING, leases.phaseNow)
    // A later request joins whatever the network state, since the service may have lifted it.
    assertEquals(Acquired.JOIN, leases.acquire(blocked = false))
    assertEquals(2, leases.countNow)
  }

  @Test
  fun serviceStops_whenLastRequestEnds() {
    leases.acquire(blocked = true)
    leases.acquire(blocked = true)
    assertTrue(leases.onServiceStarted())
    assertFalse(leases.onForeground(true))
    assertTrue(leases.awaitStarted(0))
    assertEquals(Phase.FOREGROUND, leases.phaseNow)
    assertFalse(leases.release())
    assertTrue(leases.release())
    assertTrue(leases.stopIfIdle())
    assertEquals(Phase.IDLE, leases.phaseNow)
    // A stop that comes again does nothing.
    assertFalse(leases.stopIfIdle())
  }

  @Test
  fun requestDuringPendingStop_keepsService() {
    leases.acquire(blocked = true)
    leases.onServiceStarted()
    leases.onForeground(true)
    assertTrue(leases.release())
    // A request comes before the stop posted to the main thread runs.
    assertEquals(Acquired.JOIN, leases.acquire(blocked = false))
    assertFalse(leases.stopIfIdle())
    assertEquals(Phase.FOREGROUND, leases.phaseNow)
  }

  @Test
  fun requestsEndedBeforeServiceStarted_serviceDoesNotGoForeground() {
    leases.acquire(blocked = true)
    // The request gave up waiting and ended.
    assertFalse(leases.release())
    assertFalse(leases.onServiceStarted())
    assertEquals(Phase.IDLE, leases.phaseNow)
    assertTrue(leases.awaitStarted(0))
  }

  @Test
  fun requestsEndedWhileGoingForeground_serviceStopsAtOnce() {
    leases.acquire(blocked = true)
    assertTrue(leases.onServiceStarted())
    leases.release()
    assertTrue(leases.onForeground(true))
    assertEquals(Phase.IDLE, leases.phaseNow)
  }

  @Test
  fun foregroundRefused_requestsRunAnyway_nextBurstTriesAgain() {
    leases.acquire(blocked = true)
    leases.onServiceStarted()
    assertFalse(leases.onForeground(false))
    assertTrue(leases.awaitStarted(0))
    assertEquals(Phase.IDLE, leases.phaseNow)
    // Requests in the same burst don't start it again.
    assertEquals(Acquired.JOIN, leases.acquire(blocked = true))
    assertFalse(leases.release())
    assertFalse(leases.release())
    assertEquals(Acquired.START, leases.acquire(blocked = true))
  }

  @Test
  fun serviceNotStartable_releasesWaiters() {
    leases.acquire(blocked = true)
    leases.onStartFailed()
    assertTrue(leases.awaitStarted(0))
    assertEquals(Phase.IDLE, leases.phaseNow)
    assertFalse(leases.release())
    assertEquals(0, leases.countNow)
  }

  @Test
  fun startNotAnsweredYet_waitTimesOut() {
    leases.acquire(blocked = true)
    assertFalse(leases.awaitStarted(1))
  }

  @Test
  fun timeoutWhileForeground_goesIdle_requestsStillCounted() {
    leases.acquire(blocked = true)
    leases.onServiceStarted()
    leases.onForeground(true)
    leases.onServiceGone()
    assertEquals(Phase.IDLE, leases.phaseNow)
    assertEquals(1, leases.countNow)
    assertFalse(leases.release())
  }

  @Test
  fun oldInstanceDestroyed_doesNotCancelPendingStart() {
    leases.acquire(blocked = true)
    leases.onServiceStarted()
    leases.onForeground(true)
    leases.release()
    leases.stopIfIdle()
    // A new burst starts before the stopped instance's onDestroy.
    assertEquals(Acquired.START, leases.acquire(blocked = true))
    leases.onServiceGone()
    assertEquals(Phase.STARTING, leases.phaseNow)
    assertFalse(leases.awaitStarted(0))
  }

  @Test
  fun extraRelease_isIgnored() {
    assertFalse(leases.release())
    assertEquals(0, leases.countNow)
  }

  @Test
  fun run_withoutInitialize_runsBlock() {
    assertEquals(42, NetworkForeground.run { 42 })
    NetworkForeground.acquire().release()
  }
}
