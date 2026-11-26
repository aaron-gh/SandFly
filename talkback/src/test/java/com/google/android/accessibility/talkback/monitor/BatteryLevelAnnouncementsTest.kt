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

package com.google.android.accessibility.talkback.monitor

import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements.Companion.intervalEndedBetween
import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements.Companion.nextTickDelay
import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements.Companion.parse
import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements.Companion.reachesStep
import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements.Interval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryLevelAnnouncementsTest {
  private val minute = 60_000L

  @Test
  fun parse_readsEveryValueOfTheList() {
    assertNull(parse("off"))
    assertNull(parse(null))
    assertNull(parse("unknown"))
    assertEquals(Interval(percent = 5), parse("percent_5"))
    assertEquals(Interval(percent = 10), parse("percent_10"))
    assertEquals(Interval(minutes = 15), parse("minutes_15"))
    assertEquals(Interval(minutes = 30), parse("minutes_30"))
    assertEquals(Interval(minutes = 60), parse("minutes_60"))
  }

  @Test
  fun reachesStep_whileDischarging_atEachMultiple() {
    assertTrue(reachesStep(21, 20, 10))
    assertFalse(reachesStep(20, 19, 10))
    assertFalse(reachesStep(29, 21, 10))
    assertTrue(reachesStep(1, 0, 10))
    assertTrue(reachesStep(6, 5, 5))
    assertFalse(reachesStep(7, 6, 5))
  }

  @Test
  fun reachesStep_whileCharging_atEachMultiple() {
    assertTrue(reachesStep(19, 20, 10))
    assertFalse(reachesStep(20, 21, 10))
    assertTrue(reachesStep(99, 100, 10))
    assertTrue(reachesStep(94, 95, 5))
    assertFalse(reachesStep(95, 99, 5))
  }

  @Test
  fun reachesStep_aJumpOverAMultiple() {
    assertTrue(reachesStep(21, 19, 10))
    assertTrue(reachesStep(18, 22, 10))
    assertTrue(reachesStep(34, 26, 5))
  }

  @Test
  fun reachesStep_noChangeOrNoStep_isFalse() {
    assertFalse(reachesStep(20, 20, 10))
    assertFalse(reachesStep(21, 20, 0))
  }

  @Test
  fun nextTickDelay_countsFromWhenTheSettingStarted() {
    val since = 1_000_000L
    val interval = 15 * minute
    assertEquals(interval, nextTickDelay(since, since, interval))
    assertEquals(10 * minute, nextTickDelay(since + 5 * minute, since, interval))
    // A tick that ran a little late still keeps to the intervals.
    assertEquals(interval - 50, nextTickDelay(since + interval + 50, since, interval))
    // After the screen was off for a while, the next interval end.
    assertEquals(4 * minute, nextTickDelay(since + 41 * minute, since, interval))
  }

  @Test
  fun nextTickDelay_beforeTheStartOrWithNoInterval() {
    assertEquals(15 * minute, nextTickDelay(0L, 1_000L, 15 * minute))
    assertEquals(0L, nextTickDelay(1_000L, 0L, 0L))
  }

  @Test
  fun intervalEndedBetween_whileTheScreenWasOff() {
    val since = 1_000_000L
    val hour = 60 * minute
    // Off from minute 50 to minute 70: the first hour ended meanwhile.
    assertTrue(intervalEndedBetween(since, since + 50 * minute, since + 70 * minute, hour))
    // Off from minute 5 to minute 50: no hour ended.
    assertFalse(intervalEndedBetween(since, since + 5 * minute, since + 50 * minute, hour))
    // Off at the start, on exactly at the end of the first hour.
    assertTrue(intervalEndedBetween(since, since, since + hour, hour))
    // Off for several hours: one end is enough.
    assertTrue(intervalEndedBetween(since, since + 10 * minute, since + 5 * hour, hour))
  }

  @Test
  fun intervalEndedBetween_noTimeOrNoInterval_isFalse() {
    assertFalse(intervalEndedBetween(0L, 30 * minute, 30 * minute, 15 * minute))
    assertFalse(intervalEndedBetween(0L, 30 * minute, 10 * minute, 15 * minute))
    assertFalse(intervalEndedBetween(0L, 0L, 60 * minute, 0L))
  }
}
