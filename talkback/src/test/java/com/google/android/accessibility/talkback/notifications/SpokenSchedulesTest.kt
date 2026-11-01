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

package com.google.android.accessibility.talkback.notifications

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpokenSchedulesTest {
  private val chicago = TimeZone.getTimeZone("America/Chicago")

  private fun utc(text: String) = Instant.parse(text).toEpochMilli()

  @Test
  fun repeatedHourAfterFallBackGoesToTheNextHour() {
    // 1:00 CST, the second 1:00 of the night.
    assertEquals(
      utc("2026-11-01T08:00:00Z"),
      SpokenSchedules.nextClockTime(utc("2026-11-01T07:00:00.500Z"), 60, chicago),
    )
    // 2:30 CST.
    assertEquals(
      utc("2026-11-01T09:00:00Z"),
      SpokenSchedules.nextClockTime(utc("2026-11-01T08:30:00Z"), 60, chicago),
    )
  }

  @Test
  fun lastQuarterBeforeFallBackGoesToTheRepeatedOneOClock() {
    // 1:45 CDT; the clock shows 1:00 CST 15 minutes later.
    assertEquals(
      utc("2026-11-01T07:00:00Z"),
      SpokenSchedules.nextClockTime(utc("2026-11-01T06:45:00.100Z"), 15, chicago),
    )
  }

  @Test
  fun skippedHourInSpringGoesToThreeOClock() {
    // 1:30 CST; 2:00 does not happen, 3:00 CDT does.
    for (interval in listOf(60, 180)) {
      assertEquals(
        utc("2026-03-08T08:00:00Z"),
        SpokenSchedules.nextClockTime(utc("2026-03-08T07:30:00Z"), interval, chicago),
      )
    }
  }

  @Test
  fun alarmsAcrossClockChangesAlwaysMoveForward() {
    val zones = listOf(chicago, TimeZone.getTimeZone("Europe/London"), TimeZone.getTimeZone("Australia/Lord_Howe"))
    val days = listOf("2026-03-07T00:00:00Z", "2026-03-28T00:00:00Z", "2026-04-04T00:00:00Z",
      "2026-10-03T00:00:00Z", "2026-10-24T00:00:00Z", "2026-10-31T00:00:00Z")
    for (zone in zones) for (day in days) for (interval in listOf(15, 30, 60, 180)) {
      var now = utc(day)
      val end = now + 3 * 24 * 3_600_000L
      while (now < end) {
        val next = SpokenSchedules.nextClockTime(now, interval, zone)
        assertTrue("${zone.id} $interval at $now", next > now)
        assertTrue("${zone.id} $interval at $now", next - now <= (interval + 60) * 60_000L)
        now = next
      }
    }
  }

  @Test
  fun fallBackDayHasTwentyFiveHourlyAlarms() {
    var now = utc("2026-11-01T05:00:00Z") // midnight CDT
    var count = 0
    while (true) {
      now = SpokenSchedules.nextClockTime(now, 60, chicago)
      if (now > utc("2026-11-02T06:00:00Z")) break // midnight CST
      count++
    }
    assertEquals(25, count)
  }
}
