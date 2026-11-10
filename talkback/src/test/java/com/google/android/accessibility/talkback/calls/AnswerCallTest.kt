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

import android.media.AudioManager
import android.telephony.TelephonyManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnswerCallTest {
  @Test
  fun ringingCallState_isRinging() {
    assertTrue(
      AnswerCall.isRinging(TelephonyManager.CALL_STATE_RINGING, AudioManager.MODE_NORMAL)
    )
  }

  @Test
  fun ringtoneAudioMode_isRinging_withoutTheCallState() {
    // Without READ_PHONE_STATE the call state stays idle.
    assertTrue(AnswerCall.isRinging(TelephonyManager.CALL_STATE_IDLE, AudioManager.MODE_RINGTONE))
  }

  @Test
  fun callInProgress_isNotRinging() {
    assertFalse(
      AnswerCall.isRinging(TelephonyManager.CALL_STATE_OFFHOOK, AudioManager.MODE_IN_CALL)
    )
  }

  @Test
  fun idle_isNotRinging() {
    assertFalse(AnswerCall.isRinging(TelephonyManager.CALL_STATE_IDLE, AudioManager.MODE_NORMAL))
  }
}
