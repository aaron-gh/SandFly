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

import com.google.android.accessibility.utils.ScreenOnKeeper.Companion.HIDE_DELAY_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOnKeeperTest {
  private var now = 1_000L
  private val shownChanges = mutableListOf<Boolean>()
  // When the pending timer call is due, or null.
  private var timerAt: Long? = null

  private val timer =
    object : ScreenOnKeeper.Timer {
      override fun schedule(delayMs: Long) {
        timerAt = now + delayMs
      }

      override fun cancel() {
        timerAt = null
      }
    }

  private val keeper = ScreenOnKeeper({ shownChanges.add(it) }, timer) { now }

  /** Moves the clock to [time], running the timer call if it is due by then. */
  private fun advanceTo(time: Long) {
    val due = timerAt
    if (due != null && due <= time) {
      now = due
      timerAt = null
      keeper.onTimer()
    }
    now = time
  }

  @Test
  fun request_showsWindowOnce() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.request(KeepScreenOn.READING, 60_000)

    assertTrue(keeper.isShown)
    assertEquals(listOf(true), shownChanges)
  }

  @Test
  fun release_removesWindowAfterDelay() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.release(KeepScreenOn.SPEECH)

    assertTrue(keeper.isShown)
    advanceTo(now + HIDE_DELAY_MS - 1)
    assertTrue(keeper.isShown)
    advanceTo(now + 1)
    assertFalse(keeper.isShown)
    assertNull(timerAt)
    assertEquals(listOf(true, false), shownChanges)
  }

  @Test
  fun requestWithinDelay_keepsWindow() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.release(KeepScreenOn.SPEECH)
    advanceTo(now + 500)
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    advanceTo(now + HIDE_DELAY_MS)

    assertTrue(keeper.isShown)
    assertEquals(listOf(true), shownChanges)
  }

  @Test
  fun windowStaysWhileAnotherOwnerRequests() {
    keeper.request(KeepScreenOn.READING, 60_000)
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.release(KeepScreenOn.SPEECH)
    advanceTo(now + HIDE_DELAY_MS * 2)

    assertTrue(keeper.isShown)
    keeper.release(KeepScreenOn.READING)
    advanceTo(now + HIDE_DELAY_MS)
    assertFalse(keeper.isShown)
  }

  @Test
  fun limit_endsRequest() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)

    advanceTo(now + 59_999)
    assertTrue(keeper.isShown)
    advanceTo(now + 1)
    // The request ended; the window goes after the delay.
    advanceTo(now + HIDE_DELAY_MS)
    assertFalse(keeper.isShown)
    assertNull(timerAt)
  }

  @Test
  fun requestAgain_startsLimitAgain() {
    keeper.request(KeepScreenOn.READING, 60_000)
    advanceTo(now + 50_000)
    keeper.request(KeepScreenOn.READING, 60_000)
    advanceTo(now + 50_000)

    assertTrue(keeper.isShown)
  }

  @Test
  fun limit_ofOneOwnerLeavesTheOther() {
    keeper.request(KeepScreenOn.SPEECH, 10_000)
    keeper.request(KeepScreenOn.READING, 60_000)
    advanceTo(now + 10_000)
    advanceTo(now + HIDE_DELAY_MS)

    assertTrue(keeper.isShown)
    assertEquals(now + 48_000, timerAt)
  }

  @Test
  fun screenOff_removesWindowAtOnce_andIgnoresRequests() {
    keeper.request(KeepScreenOn.READING, 60_000)
    keeper.setScreenOn(false)

    assertFalse(keeper.isShown)
    assertNull(timerAt)
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    assertFalse(keeper.isShown)

    keeper.setScreenOn(true)
    assertFalse(keeper.isShown)
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    assertTrue(keeper.isShown)
  }

  @Test
  fun pause_removesWindowAtOnce_andIgnoresRequests() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.setPaused(true)

    assertFalse(keeper.isShown)
    keeper.request(KeepScreenOn.READING, 60_000)
    assertFalse(keeper.isShown)

    keeper.setPaused(false)
    keeper.request(KeepScreenOn.READING, 60_000)
    assertTrue(keeper.isShown)
  }

  @Test
  fun releaseAll_removesWindowAtOnce() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.request(KeepScreenOn.READING, 60_000)
    keeper.releaseAll()

    assertFalse(keeper.isShown)
    assertNull(timerAt)
    assertEquals(listOf(true, false), shownChanges)
  }

  @Test
  fun releaseAll_duringDelay_removesWindowAtOnce() {
    keeper.request(KeepScreenOn.SPEECH, 60_000)
    keeper.release(KeepScreenOn.SPEECH)
    keeper.releaseAll()

    assertFalse(keeper.isShown)
    assertNull(timerAt)
  }

  @Test
  fun releaseWithoutRequest_doesNothing() {
    keeper.release(KeepScreenOn.SPEECH)
    keeper.releaseAll()

    assertFalse(keeper.isShown)
    assertTrue(shownChanges.isEmpty())
  }

  @Test
  fun nonPositiveLimit_isIgnored() {
    keeper.request(KeepScreenOn.SPEECH, 0)

    assertFalse(keeper.isShown)
  }
}
