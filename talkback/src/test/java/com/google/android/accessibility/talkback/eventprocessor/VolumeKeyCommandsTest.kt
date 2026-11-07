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

package com.google.android.accessibility.talkback.eventprocessor

import android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
import android.media.AudioManager.MODE_IN_CALL
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.media.AudioManager.MODE_NORMAL
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyCommands.Companion.isAppCall
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VolumeKeyCommandsTest {
  @Test
  fun voiceCommunicationPlaying_isAppCall() {
    assertTrue(
      isAppCall(
        listOf(USAGE_MEDIA, USAGE_VOICE_COMMUNICATION),
        USAGE_ASSISTANCE_ACCESSIBILITY,
        MODE_IN_COMMUNICATION,
      )
    )
  }

  @Test
  fun callModeWithoutVoiceCommunication_isNotAppCall() {
    // A mode left after a call, or kept by an app with no call.
    assertFalse(isAppCall(listOf(USAGE_MEDIA), USAGE_ASSISTANCE_ACCESSIBILITY, MODE_IN_CALL))
    assertFalse(isAppCall(emptyList(), USAGE_ASSISTANCE_ACCESSIBILITY, MODE_IN_COMMUNICATION))
  }

  @Test
  fun normalModeWithoutVoiceCommunication_isNotAppCall() {
    assertFalse(isAppCall(listOf(USAGE_MEDIA), USAGE_ASSISTANCE_ACCESSIBILITY, MODE_NORMAL))
  }

  @Test
  fun ownSpeechOnCallVolume_usesCommunicationMode() {
    assertFalse(
      isAppCall(listOf(USAGE_VOICE_COMMUNICATION), USAGE_VOICE_COMMUNICATION, MODE_NORMAL)
    )
    assertFalse(
      isAppCall(listOf(USAGE_VOICE_COMMUNICATION), USAGE_VOICE_COMMUNICATION, MODE_IN_CALL)
    )
    assertTrue(isAppCall(emptyList(), USAGE_VOICE_COMMUNICATION, MODE_IN_COMMUNICATION))
  }
}
