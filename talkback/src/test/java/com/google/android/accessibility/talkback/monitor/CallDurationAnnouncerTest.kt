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

import android.content.Context
import android.telephony.TelephonyManager.CALL_STATE_IDLE
import android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallDurationAnnouncerTest {
  private lateinit var context: Context
  private lateinit var locale: Locale
  private val spoken = mutableListOf<String>()
  private var now = 0L
  private lateinit var announcer: CallDurationAnnouncer

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    locale = Locale.getDefault()
    Locale.setDefault(Locale.US)
    announcer = CallDurationAnnouncer(context, { spoken += it }, { now })
    setOn(true)
  }

  @After
  fun tearDown() {
    Locale.setDefault(locale)
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .remove(context.getString(R.string.pref_announce_call_length_key))
      .commit()
  }

  private fun setOn(on: Boolean) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_announce_call_length_key), on)
      .commit()
  }

  private fun change(old: Int, new: Int, at: Long) {
    now = at
    announcer.onCallStateChanged(old, new)
  }

  @Test
  fun anAnsweredCallIsMeasuredFromAnswerToEnd() {
    change(CALL_STATE_IDLE, CALL_STATE_RINGING, 0)
    change(CALL_STATE_RINGING, CALL_STATE_OFFHOOK, 10_000)
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 10_000 + 192_400)
    assertEquals(listOf("Call ended, 3 minutes, 12 seconds"), spoken)
  }

  @Test
  fun anOutgoingCallIsMeasuredFromDialing() {
    change(CALL_STATE_IDLE, CALL_STATE_OFFHOOK, 1_000)
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 46_000)
    assertEquals(listOf("Call ended, 45 seconds"), spoken)
  }

  @Test
  fun aMissedOrRejectedCallIsNotSaid() {
    change(CALL_STATE_IDLE, CALL_STATE_RINGING, 0)
    change(CALL_STATE_RINGING, CALL_STATE_IDLE, 20_000)
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun aWaitingCallKeepsTheFirstCallsStart() {
    change(CALL_STATE_IDLE, CALL_STATE_OFFHOOK, 0)
    change(CALL_STATE_OFFHOOK, CALL_STATE_RINGING, 60_000)
    change(CALL_STATE_RINGING, CALL_STATE_OFFHOOK, 65_000)
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 3_725_000)
    assertEquals(listOf("Call ended, 1 hour, 2 minutes, 5 seconds"), spoken)
  }

  @Test
  fun nothingIsSaidWhileTheSwitchIsOff() {
    setOn(false)
    change(CALL_STATE_IDLE, CALL_STATE_OFFHOOK, 0)
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 30_000)
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun aCallAlreadyOffHookWhenFollowingStartsIsNotSaid() {
    // Call state monitoring started during a call, so only its end is seen.
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 30_000)
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun anEarlierCallMissedWhileNotFollowingIsForgotten() {
    // Monitoring stopped during a call and started again after it ended.
    change(CALL_STATE_IDLE, CALL_STATE_OFFHOOK, 0)
    change(CALL_STATE_IDLE, CALL_STATE_RINGING, 600_000)
    change(CALL_STATE_RINGING, CALL_STATE_OFFHOOK, 605_000)
    change(CALL_STATE_OFFHOOK, CALL_STATE_IDLE, 665_000)
    assertEquals(listOf("Call ended, 1 minute"), spoken)
  }

  @Test
  fun formatsZeroAsSeconds() {
    assertEquals("0 seconds", CallDurationAnnouncer.formatDuration(0, Locale.US))
    assertEquals("2 hours", CallDurationAnnouncer.formatDuration(7200, Locale.US))
  }
}
