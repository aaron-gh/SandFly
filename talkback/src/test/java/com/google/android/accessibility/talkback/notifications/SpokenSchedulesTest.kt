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

import android.content.Context
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.util.TimeZone
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpokenSchedulesTest {
  private val chicago = TimeZone.getTimeZone("America/Chicago")

  private fun utc(text: String) = Instant.parse(text).toEpochMilli()

  @Test
  fun timeIsSaidInTheNotificationVoiceUntilAVoiceIsChosenForIt() {
    val prefs =
      RuntimeEnvironment.getApplication()
        .getSharedPreferences("clock-voice", Context.MODE_PRIVATE)
    assertEquals(
      NotificationSettings.CLOCK_VOICE_NOTIFICATIONS,
      NotificationSettings.read(prefs).clockVoiceProfile,
    )

    prefs.edit().putString(NotificationSettings.Companion.Keys.CLOCK_VOICE_PROFILE, "").commit()
    assertEquals("", NotificationSettings.read(prefs).clockVoiceProfile)
  }

  @Test
  fun turningTimeAnnouncementsOnAgainRestoresTheInterval() {
    val prefs =
      RuntimeEnvironment.getApplication()
        .getSharedPreferences("clock-toggle", Context.MODE_PRIVATE)
    prefs.edit().clear().putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "30").commit()

    assertFalse(SpokenSchedules.toggleClock(prefs))
    assertEquals(0, NotificationSettings.int(prefs, SpokenSchedules.KEY_CLOCK_INTERVAL, -1))

    assertTrue(SpokenSchedules.toggleClock(prefs))
    assertEquals(30, NotificationSettings.int(prefs, SpokenSchedules.KEY_CLOCK_INTERVAL, -1))
  }

  @Test
  fun turningTimeAnnouncementsOnWithNoIntervalSetUsesTheDefault() {
    val prefs =
      RuntimeEnvironment.getApplication()
        .getSharedPreferences("clock-toggle-default", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    assertTrue(SpokenSchedules.toggleClock(prefs))
    assertEquals(
      SpokenSchedules.DEFAULT_CLOCK_INTERVAL,
      NotificationSettings.int(prefs, SpokenSchedules.KEY_CLOCK_INTERVAL, -1),
    )
  }

  @Test
  fun intervalChosenBeforeTurningOffInSettingsIsRestored() {
    val prefs =
      RuntimeEnvironment.getApplication()
        .getSharedPreferences("clock-toggle-settings", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    SpokenSchedules.rememberClockInterval(prefs, 15)
    SpokenSchedules.rememberClockInterval(prefs, 0)
    prefs.edit().putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "0").commit()

    assertTrue(SpokenSchedules.toggleClock(prefs))
    assertEquals(15, NotificationSettings.int(prefs, SpokenSchedules.KEY_CLOCK_INTERVAL, -1))
  }

  @Test
  fun intervalInUseWhenSchedulesStartIsRestoredAfterTurningOffInSettings() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs
      .edit()
      .remove(SpokenSchedules.KEY_CLOCK_LAST_INTERVAL)
      .putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "15")
      .commit()
    val schedules = SpokenSchedules(context, { _, _ -> }, {})
    schedules.start()
    prefs.edit().putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "0").commit()
    schedules.stop()

    assertTrue(SpokenSchedules.toggleClock(prefs))
    assertEquals(15, NotificationSettings.int(prefs, SpokenSchedules.KEY_CLOCK_INTERVAL, -1))
  }

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

  @Test
  fun batteryIsSaidAsAWordNotAPercentSign() {
    val context = RuntimeEnvironment.getApplication()
    assertEquals("76 percent", SpokenSchedules.batteryText(context, 76, 100))
    assertEquals("50 percent", SpokenSchedules.batteryText(context, 100, 200))
    assertEquals("", SpokenSchedules.batteryText(context, -1, 100))
  }

  @Test
  fun alarmsAndRemindersIsNeededOnlyWhileTheTimeIsSaidWithoutIt() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    ShadowAlarmManager.setCanScheduleExactAlarms(false)
    prefs.edit().putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "0").commit()
    assertFalse(SpokenSchedules.needsExactAlarms(context))
    prefs.edit().putString(SpokenSchedules.KEY_CLOCK_INTERVAL, "30").commit()
    assertTrue(SpokenSchedules.needsExactAlarms(context))
    ShadowAlarmManager.setCanScheduleExactAlarms(true)
    assertFalse(SpokenSchedules.needsExactAlarms(context))
  }
}
