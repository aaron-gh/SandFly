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

package com.google.android.accessibility.talkback.imagecaption

import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RequestListTest {
  private val pending = mutableListOf<Boolean>()

  /** A request that the test ends, as its result, its error or its timeout would. */
  private inner class FakeRequest(
    private val list: RequestList<FakeRequest>,
    timeout: Duration?,
    private val failAtOnce: Boolean,
  ) : Request({ scheduled, _ -> pending += scheduled }, timeout) {
    var performed = 0
    var results = 0
    val errors = mutableListOf<Int>()

    override fun perform() {
      performed++
      setStartTimestamp()
      if (failAtOnce) {
        onError(Request.ERROR_UNKNOWN)
      }
      runTimeoutRunnable()
    }

    override fun onError(errorCode: Int) {
      if (finish()) {
        setEndTimestamp()
        errors += errorCode
        list.performNextRequest(this)
      }
    }

    fun succeed() {
      if (finish()) {
        setEndTimestamp()
        results++
        list.performNextRequest(this)
      }
    }
  }

  private fun RequestList<FakeRequest>.request(
    timeout: Duration? = null,
    failAtOnce: Boolean = false,
  ) = FakeRequest(this, timeout, failAtOnce)

  private fun advance(duration: Duration) {
    shadowOf(Looper.getMainLooper()).idleFor(duration)
  }

  @Test
  fun aLateResultEndsNeitherItsRequestAgainNorTheNextOne() {
    val list = RequestList<FakeRequest>(/* capacity= */ 1)
    val a = list.request(timeout = Duration.ofSeconds(3))
    val b = list.request()
    val c = list.request()
    list.addRequest(a)
    list.addRequest(b)

    advance(Duration.ofSeconds(3))
    assertEquals(listOf(Request.ERROR_TIMEOUT), a.errors)
    assertEquals(1, b.performed)

    list.addRequest(c)
    a.succeed()
    list.performNextRequest(a)
    assertEquals(0, a.results)
    assertEquals("C started before B ended", 0, c.performed)

    b.succeed()
    assertEquals(1, c.performed)
  }

  @Test
  fun clearCancelsTheRequests_soTheirTimeoutsAndResultsAreIgnored() {
    val list = RequestList<FakeRequest>(/* capacity= */ 10)
    val a = list.request(timeout = Duration.ofSeconds(3))
    val b = list.request()
    list.addRequest(a)
    list.addRequest(b)

    list.clear()
    advance(Duration.ofSeconds(3))
    a.succeed()
    assertTrue(a.errors.isEmpty())
    assertEquals(0, a.results)

    val d = list.request()
    list.addRequest(d)
    assertEquals(1, d.performed)
    assertEquals(0, b.performed)
  }

  @Test
  fun aRequestThatFailsAtOnceSetsNoTimeout() {
    val list = RequestList<FakeRequest>(/* capacity= */ 1)
    val a = list.request(timeout = Duration.ofSeconds(3), failAtOnce = true)
    list.addRequest(a)

    advance(Duration.ofSeconds(3))
    assertEquals(listOf(Request.ERROR_UNKNOWN), a.errors)
  }

  @Test
  fun waitsTheIntervalInUptime() {
    val list = RequestList<FakeRequest>(/* capacity= */ 1, Duration.ofSeconds(1))
    val a = list.request()
    val b = list.request()
    list.addRequest(a)
    a.succeed()

    list.addRequest(b)
    assertEquals(listOf(true), pending)
    advance(Duration.ofMillis(999))
    assertEquals(0, b.performed)
    advance(Duration.ofMillis(1))
    assertEquals(1, b.performed)
  }

  @Test
  fun keepsTheNewestWaitingRequests_andCancelsTheOthers() {
    val list = RequestList<FakeRequest>(/* capacity= */ 1)
    val a = list.request()
    val b = list.request()
    val c = list.request()
    list.addRequest(a)
    list.addRequest(b)
    list.addRequest(c)

    a.succeed()
    assertEquals(0, b.performed)
    assertEquals(1, c.performed)

    b.succeed()
    assertEquals(0, b.results)
    val d = list.request()
    list.addRequest(d)
    assertEquals("D started before C ended", 0, d.performed)
  }
}
