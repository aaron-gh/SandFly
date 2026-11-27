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

package com.google.android.accessibility.talkback.calls

import android.app.Notification
import android.content.Context
import android.media.AudioManager
import android.os.Looper
import android.telephony.TelephonyManager.CALL_STATE_IDLE
import android.telephony.TelephonyManager.CALL_STATE_OFFHOOK
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
import java.util.function.BooleanSupplier
import java.util.function.IntSupplier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallerAnnouncerTest {
  private lateinit var context: Context
  private val spoken = mutableListOf<String>()
  private var callState = CALL_STATE_IDLE
  private var audioMode = AudioManager.MODE_NORMAL
  private var speaking = false
  private var inShade: CharSequence? = null
  private var listener = false
  private lateinit var announcer: CallerAnnouncer

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    announcer =
      CallerAnnouncer(
        context = context,
        speak = { spoken += it.toString() },
        callState = IntSupplier { callState },
        isSpeaking = BooleanSupplier { speaking },
        audioMode = IntSupplier { audioMode },
        callerInShade = { inShade },
        listenerConnected = BooleanSupplier { listener },
      )
  }

  @After
  fun tearDown() {
    announcer.shutdown()
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .remove(context.getString(R.string.pref_announce_caller_key))
      .commit()
  }

  private fun setMode(value: String) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_announce_caller_key), value)
      .commit()
  }

  private fun idle(ms: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
  }

  private fun ring(caller: String) {
    callState = CALL_STATE_RINGING
    announcer.onIncomingCall(
      Notification.Builder(context, "calls")
        .setCategory(Notification.CATEGORY_CALL)
        .setContentTitle(caller)
        .setContentText("Incoming call")
        .build()
    )
    announcer.onIncomingCallAnnounced()
  }

  @Test
  fun settingValues_areParsed_withOnceForAnythingElse() {
    assertEquals(CallerAnnouncer.Mode.ONCE, CallerAnnouncer.parse("once"))
    assertEquals(CallerAnnouncer.Mode.REPEAT, CallerAnnouncer.parse("repeat"))
    assertEquals(CallerAnnouncer.Mode.OFF, CallerAnnouncer.parse("off"))
    assertEquals(CallerAnnouncer.Mode.ONCE, CallerAnnouncer.parse(null))
    assertEquals(CallerAnnouncer.Mode.ONCE, CallerAnnouncer.parse("unknown"))
  }

  @Test
  fun defaultMode_isOnce_andAnnouncesCalls() {
    assertEquals(CallerAnnouncer.Mode.ONCE, CallerAnnouncer.mode(context))
    assertTrue(announcer.announcesIncomingCalls())
    setMode("off")
    assertTrue(!announcer.announcesIncomingCalls())
  }

  @Test
  fun once_doesNotRepeat() {
    ring("Sam")
    idle(30_000)
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun repeat_saysTheCallerEveryFiveSecondsWhileTheCallRings() {
    setMode("repeat")
    ring("Sam")
    idle(CallerAnnouncer.REPEAT_MS - 1)
    assertEquals(0, spoken.size)
    idle(1)
    assertEquals(1, spoken.size)
    assertTrue(spoken[0].contains("Sam"))
    idle(CallerAnnouncer.REPEAT_MS)
    assertEquals(2, spoken.size)
  }

  @Test
  fun repeat_stopsWhenTheCallIsAnswered() {
    setMode("repeat")
    ring("Sam")
    idle(CallerAnnouncer.REPEAT_MS)
    callState = CALL_STATE_OFFHOOK
    announcer.onCallStateChanged(CALL_STATE_RINGING, CALL_STATE_OFFHOOK)
    idle(30_000)
    assertEquals(1, spoken.size)
  }

  @Test
  fun repeat_stopsWhenNothingShowsARingingCall() {
    setMode("repeat")
    ring("Sam")
    // An app's call: no call state change, and it is no longer in the shade.
    callState = CALL_STATE_IDLE
    idle(30_000)
    assertEquals(0, spoken.size)
  }

  @Test
  fun repeat_followsTheShadeForAnAppCall() {
    setMode("repeat")
    ring("Sam")
    callState = CALL_STATE_IDLE
    inShade = "Call, Alex, WhatsApp voice call"
    idle(CallerAnnouncer.REPEAT_MS)
    assertEquals(listOf("Call, Alex, WhatsApp voice call"), spoken)
    inShade = null
    idle(30_000)
    assertEquals(1, spoken.size)
  }

  @Test
  fun repeat_waitsForSpeechToEnd() {
    setMode("repeat")
    ring("Sam")
    speaking = true
    idle(CallerAnnouncer.REPEAT_MS + 3 * CallerAnnouncer.BUSY_RETRY_MS)
    assertEquals(0, spoken.size)
    speaking = false
    idle(CallerAnnouncer.BUSY_RETRY_MS)
    assertEquals(1, spoken.size)
  }

  @Test
  fun repeat_stopsWhenTheSettingChanges() {
    setMode("repeat")
    ring("Sam")
    setMode("once")
    idle(30_000)
    assertEquals(0, spoken.size)
  }

  @Test
  fun repeat_endsAfterTheLongestRingingTime() {
    setMode("repeat")
    ring("Sam")
    idle(CallerAnnouncer.MAX_REPEAT_MS + 60_000)
    val count = spoken.size
    assertTrue(count <= (CallerAnnouncer.MAX_REPEAT_MS / CallerAnnouncer.REPEAT_MS).toInt())
    idle(60_000)
    assertEquals(count, spoken.size)
  }

  @Test
  fun repeat_stopsOnShutdown() {
    setMode("repeat")
    ring("Sam")
    announcer.shutdown()
    idle(30_000)
    assertEquals(0, spoken.size)
  }

  @Test
  fun sayCaller_saysTheRingingCaller_alsoWithAnnouncementsOff() {
    setMode("off")
    ring("Sam")
    assertTrue(announcer.sayCallerText().toString().contains("Sam"))
  }

  @Test
  fun sayCaller_withoutACall_saysNoIncomingCall() {
    assertEquals(
      context.getString(R.string.answer_call_no_call),
      announcer.sayCallerText().toString(),
    )
  }

  @Test
  fun sayCaller_afterTheCallEnds_saysNoIncomingCall() {
    ring("Sam")
    callState = CALL_STATE_IDLE
    announcer.onCallStateChanged(CALL_STATE_RINGING, CALL_STATE_IDLE)
    assertEquals(
      context.getString(R.string.answer_call_no_call),
      announcer.sayCallerText().toString(),
    )
  }

  @Test
  fun sayCaller_whileRingingWithoutANotification_saysIncomingCall() {
    callState = CALL_STATE_RINGING
    assertEquals(
      context.getString(R.string.say_caller_unknown),
      announcer.sayCallerText().toString(),
    )
  }

  @Test
  fun choose_prefersTheShade_thenARingingCall() {
    assertEquals("shade", CallerAnnouncer.choose("shade", "recent", false, true, true))
    assertEquals("recent", CallerAnnouncer.choose(null, "recent", true, true, true))
    assertNull(CallerAnnouncer.choose(null, null, true, false, false))
  }

  @Test
  fun choose_withoutARingingCall_usesTheRecentCallOnlyWithoutTheShadeAndOutsideACall() {
    assertNull(CallerAnnouncer.choose(null, "recent", false, false, true))
    assertNull(CallerAnnouncer.choose(null, "recent", false, true, false))
    assertEquals("recent", CallerAnnouncer.choose(null, "recent", false, false, false))
  }

  @Test
  fun inCall_isFoundFromTheCallStateOrTheAudioMode() {
    assertTrue(CallerAnnouncer.isInCall(CALL_STATE_OFFHOOK, AudioManager.MODE_NORMAL))
    assertTrue(CallerAnnouncer.isInCall(CALL_STATE_IDLE, AudioManager.MODE_IN_COMMUNICATION))
    assertTrue(!CallerAnnouncer.isInCall(CALL_STATE_RINGING, AudioManager.MODE_RINGTONE))
  }
}
