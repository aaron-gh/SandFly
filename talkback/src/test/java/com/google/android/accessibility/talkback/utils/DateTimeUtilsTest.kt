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

package com.google.android.accessibility.talkback.utils

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DateTimeUtilsTest {
  private lateinit var context: Context
  private lateinit var zone: TimeZone
  private lateinit var locale: Locale

  // 2026-11-17 14:05:09 UTC.
  private val millis = Instant.parse("2026-11-17T14:05:09Z").toEpochMilli()

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    zone = TimeZone.getDefault()
    locale = Locale.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    Locale.setDefault(Locale.US)
    DateTimeUtils.setMockClock(Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC))
  }

  @After
  fun tearDown() {
    DateTimeUtils.resetMockClock()
    TimeZone.setDefault(zone)
    Locale.setDefault(locale)
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .remove(context.getString(R.string.pref_time_seconds_key))
      .remove(context.getString(R.string.pref_time_seconds_screen_on_key))
      .commit()
  }

  private fun setSeconds(on: Boolean) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_time_seconds_key), on)
      .commit()
  }

  @Test
  fun formatsSecondsIn12And24Hours() {
    val utc = TimeZone.getTimeZone("UTC")
    val twelve = DateTimeUtils.formatTimeWithSeconds(millis, false, Locale.US, utc)
    assertTrue(twelve, twelve.startsWith("2:05:09"))
    assertTrue(twelve, twelve.contains("PM"))
    assertEquals("14:05:09", DateTimeUtils.formatTimeWithSeconds(millis, true, Locale.US, utc))
  }

  @Test
  fun timeHasNoSecondsByDefault() {
    val time = DateTimeUtils.getCurrentTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS)
    assertFalse(time, time.contains("05:09"))
    assertTrue(time, time.contains("14:05"))
  }

  @Test
  fun timeHasSecondsWhenTheSwitchIsOn() {
    setSeconds(true)
    assertEquals(
      "14:05:09",
      DateTimeUtils.getCurrentTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS),
    )
    assertTrue(DateTimeUtils.getCurrentTimeAndDate(context).contains(":05:09"))
  }

  @Test
  fun screenOnTimeHasSecondsByDefaultWhenTheSwitchIsOn() {
    setSeconds(true)
    assertEquals(
      "14:05:09",
      DateTimeUtils.getScreenOnTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS),
    )
  }

  @Test
  fun screenOnTimeLeavesOutSecondsWhenOnlyAsked() {
    setSeconds(true)
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_time_seconds_screen_on_key), false)
      .commit()
    val screenOn =
      DateTimeUtils.getScreenOnTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS)
    assertFalse(screenOn, screenOn.contains("05:09"))
    assertTrue(screenOn, screenOn.contains("14:05"))
    // A command and the status readout still say the seconds.
    assertEquals(
      "14:05:09",
      DateTimeUtils.getCurrentTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS),
    )
    assertTrue(DateTimeUtils.getCurrentTimeAndDate(context).contains(":05:09"))
  }

  @Test
  fun screenOnTimeHasNoSecondsWhileTheMainSwitchIsOff() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_time_seconds_screen_on_key), true)
      .commit()
    val screenOn =
      DateTimeUtils.getScreenOnTime(context, DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS)
    assertFalse(screenOn, screenOn.contains("05:09"))
  }
}
