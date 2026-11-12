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

import android.os.Looper
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Checks when the Clipboard history's paste is tried again, and when it stops. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class PasteAttemptsTest {

  private var attempts = 0
  private var failures = 0

  @After
  fun cancel() {
    PasteAttempts.cancel()
  }

  private fun idle(millis: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
  }

  private fun start(delayMs: Long = 0L, active: () -> Boolean = { true }, succeedOn: Int = -1) {
    PasteAttempts.start(
      delayMs,
      active,
      {
        attempts++
        attempts == succeedOn
      },
      { failures++ },
    )
  }

  @Test
  fun waitsForTheDelay() {
    start(delayMs = 1000L, succeedOn = 1)
    idle(999L)
    assertEquals(0, attempts)
    idle(1L)
    assertEquals(1, attempts)
  }

  @Test
  fun stopsOnSuccess() {
    start(succeedOn = 3)
    idle(10_000L)
    assertEquals(3, attempts)
    assertEquals(0, failures)
  }

  @Test
  fun triesEveryIntervalForTheDurationThenFails() {
    start(delayMs = 500L)
    idle(500L + PasteAttempts.DURATION_MS - PasteAttempts.INTERVAL_MS)
    assertEquals(0, failures)
    idle(10_000L)
    assertEquals((PasteAttempts.DURATION_MS / PasteAttempts.INTERVAL_MS + 1).toInt(), attempts)
    assertEquals(1, failures)
  }

  @Test
  fun stopsWithoutFailingWhenNoLongerActive() {
    var active = true
    start(active = { active })
    idle(PasteAttempts.INTERVAL_MS)
    assertEquals(2, attempts)
    active = false
    idle(10_000L)
    assertEquals(2, attempts)
    assertEquals(0, failures)
  }

  @Test
  fun stopsWhenCancelled() {
    start()
    idle(PasteAttempts.INTERVAL_MS)
    PasteAttempts.cancel()
    idle(10_000L)
    assertEquals(2, attempts)
    assertEquals(0, failures)
  }

  @Test
  fun startingAgainCancelsTheAttemptsBefore() {
    var before = 0
    PasteAttempts.start(0L, { true }, { before++ == -1 }, { failures++ })
    idle(PasteAttempts.INTERVAL_MS)
    start(succeedOn = 1)
    idle(10_000L)
    assertEquals(2, before)
    assertEquals(1, attempts)
    assertEquals(0, failures)
  }
}
