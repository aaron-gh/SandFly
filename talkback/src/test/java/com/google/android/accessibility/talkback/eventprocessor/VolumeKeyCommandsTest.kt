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
import android.media.AudioManager.FLAG_PLAY_SOUND
import android.media.AudioManager.FLAG_SHOW_UI
import android.media.AudioManager.FLAG_VIBRATE
import android.media.AudioManager.MODE_IN_CALL
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.media.AudioManager.MODE_NORMAL
import android.media.AudioManager.STREAM_ALARM
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.USE_DEFAULT_STREAM_TYPE
import android.media.VolumeProvider.VOLUME_CONTROL_ABSOLUTE
import android.media.VolumeProvider.VOLUME_CONTROL_FIXED
import android.media.session.MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL
import android.media.session.MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
import android.media.session.PlaybackState.STATE_BUFFERING
import android.media.session.PlaybackState.STATE_PAUSED
import android.media.session.PlaybackState.STATE_PLAYING
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyCommands.Companion.longPressDuration
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyCommands.Companion.otherPresses
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses.Companion.isAppCall
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses.OtherPresses
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.FLAG_FROM_KEY
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.handlesVolumeKeys
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.isPlaybackActive
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.localSessionStream
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.streamWithoutSession
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyVolume.Companion.volumeFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VolumeKeyCommandsTest {
  @Test
  fun longPressDuration_fixedValueUsed() {
    assertEquals(700L, longPressDuration("700", 400L))
    assertEquals(1000L, longPressDuration("1000", 400L))
  }

  @Test
  fun longPressDuration_systemDefaultOrInvalid_usesSystemDefault() {
    assertEquals(400L, longPressDuration("0", 400L))
    assertEquals(400L, longPressDuration(null, 400L))
    assertEquals(400L, longPressDuration("", 400L))
    assertEquals(400L, longPressDuration("abc", 400L))
    assertEquals(400L, longPressDuration("-5", 400L))
    assertEquals(400L, longPressDuration("99", 400L))
    assertEquals(400L, longPressDuration("2001", 400L))
    assertEquals(400L, longPressDuration("99999999999999999999", 400L))
  }

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
  @Test
  fun otherPresses_switchesOff_passToAndroid() {
    assertEquals(OtherPresses.ANDROID, otherPresses(false, false, interactive = true))
  }

  @Test
  fun otherPresses_takeAllWins() {
    assertEquals(OtherPresses.NOTHING, otherPresses(true, false, interactive = true))
    assertEquals(OtherPresses.NOTHING, otherPresses(true, true, interactive = true))
  }

  @Test
  fun otherPresses_hidePanel_changesVolume() {
    assertEquals(OtherPresses.VOLUME, otherPresses(false, true, interactive = true))
  }

  @Test
  fun otherPresses_screenOff_passToAndroid() {
    assertEquals(OtherPresses.ANDROID, otherPresses(true, true, interactive = false))
    assertEquals(
      OtherPresses.ANDROID,
      otherPresses(false, false, interactive = false, replaced = true),
    )
  }

  @Test
  fun otherPresses_replaced_keepsThemWhateverTheSwitches() {
    // Volume keys move the cursor in a text field, with a press command assigned.
    assertEquals(OtherPresses.VOLUME, otherPresses(true, false, interactive = true, replaced = true))
    assertEquals(
      OtherPresses.VOLUME,
      otherPresses(false, false, interactive = true, replaced = true),
    )
  }

  @Test
  fun volumeFlags_panelOnlyWithShowUi() {
    assertEquals(
      FLAG_SHOW_UI or FLAG_VIBRATE or FLAG_PLAY_SOUND or FLAG_FROM_KEY,
      volumeFlags(false, true),
    )
    assertEquals(FLAG_VIBRATE or FLAG_PLAY_SOUND or FLAG_FROM_KEY, volumeFlags(false, false))
    assertEquals(FLAG_VIBRATE or FLAG_FROM_KEY, volumeFlags(true, false))
  }

  @Test
  fun isPlaybackActive_playingAndBufferingOnly() {
    assertTrue(isPlaybackActive(STATE_PLAYING))
    assertTrue(isPlaybackActive(STATE_BUFFERING))
    assertFalse(isPlaybackActive(STATE_PAUSED))
    assertFalse(isPlaybackActive(null))
  }

  @Test
  fun handlesVolumeKeys_notFixedRemoteSessions() {
    assertTrue(handlesVolumeKeys(PLAYBACK_TYPE_LOCAL, VOLUME_CONTROL_FIXED))
    assertTrue(handlesVolumeKeys(PLAYBACK_TYPE_REMOTE, VOLUME_CONTROL_ABSOLUTE))
    assertFalse(handlesVolumeKeys(PLAYBACK_TYPE_REMOTE, VOLUME_CONTROL_FIXED))
  }

  @Test
  fun localSessionStream_sessionStreamUnlessMusicSilent() {
    assertEquals(STREAM_MUSIC, localSessionStream(STREAM_MUSIC, USE_DEFAULT_STREAM_TYPE, true))
    assertEquals(
      USE_DEFAULT_STREAM_TYPE,
      localSessionStream(STREAM_MUSIC, USE_DEFAULT_STREAM_TYPE, false),
    )
    assertEquals(STREAM_ALARM, localSessionStream(STREAM_ALARM, USE_DEFAULT_STREAM_TYPE, false))
  }

  @Test
  fun streamWithoutSession_musicWhenMusicPlaysAndStreamIsDefault() {
    assertEquals(STREAM_MUSIC, streamWithoutSession(USE_DEFAULT_STREAM_TYPE, true))
    assertEquals(USE_DEFAULT_STREAM_TYPE, streamWithoutSession(USE_DEFAULT_STREAM_TYPE, false))
    assertEquals(STREAM_ALARM, streamWithoutSession(STREAM_ALARM, true))
  }
}
