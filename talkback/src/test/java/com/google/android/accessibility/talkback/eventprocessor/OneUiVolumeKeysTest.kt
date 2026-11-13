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

import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.STREAM_RING
import com.google.android.accessibility.talkback.eventprocessor.OneUiVolumeKeys.Companion.isAccessibilityStreamActive
import com.google.android.accessibility.talkback.eventprocessor.OneUiVolumeKeys.Companion.keepsFromAccessibilityStream
import com.google.android.accessibility.talkback.eventprocessor.OneUiVolumeKeys.Companion.streamWithoutAccessibilityAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OneUiVolumeKeysTest {
  @Test
  fun playing_isActive() {
    assertTrue(isAccessibilityStreamActive(playing = true, stoppedAt = 0L, now = 100_000L))
  }

  @Test
  fun justStopped_isActive() {
    assertTrue(isAccessibilityStreamActive(playing = false, stoppedAt = 10_000L, now = 11_000L))
  }

  @Test
  fun stoppedLongAgo_isNotActive() {
    assertFalse(isAccessibilityStreamActive(playing = false, stoppedAt = 10_000L, now = 12_000L))
  }

  @Test
  fun neverPlayed_isNotActive() {
    assertFalse(
      isAccessibilityStreamActive(playing = false, stoppedAt = Long.MIN_VALUE, now = 100L)
    )
  }

  @Test
  fun playing_keptByDefault() {
    assertTrue(keepsFromAccessibilityStream(true, Long.MIN_VALUE, 100_000L, false))
  }

  @Test
  fun playing_speechVolumeWhileSpeaking_notKept() {
    assertFalse(keepsFromAccessibilityStream(true, Long.MIN_VALUE, 100_000L, true))
  }

  @Test
  fun playingAgainAfterStop_speechVolumeWhileSpeaking_notKept() {
    assertFalse(keepsFromAccessibilityStream(true, 10_000L, 10_500L, true))
  }

  @Test
  fun justStopped_speechVolumeWhileSpeaking_kept() {
    assertTrue(keepsFromAccessibilityStream(false, 10_000L, 11_000L, true))
  }

  @Test
  fun justStopped_keptByDefault() {
    assertTrue(keepsFromAccessibilityStream(false, 10_000L, 11_000L, false))
  }

  @Test
  fun stoppedLongAgo_speechVolumeWhileSpeaking_notKept() {
    assertFalse(keepsFromAccessibilityStream(false, 10_000L, 12_000L, true))
  }

  @Test
  fun neverPlayed_notKept() {
    assertFalse(keepsFromAccessibilityStream(false, Long.MIN_VALUE, 100L, false))
  }

  @Test
  fun volumeKeysForMedia_adjustsMedia() {
    assertEquals(STREAM_MUSIC, streamWithoutAccessibilityAudio(true, true))
  }

  @Test
  fun volumeKeysForMediaOff_adjustsRing() {
    assertEquals(STREAM_RING, streamWithoutAccessibilityAudio(false, true))
  }

  @Test
  fun noVoice_adjustsMedia() {
    assertEquals(STREAM_MUSIC, streamWithoutAccessibilityAudio(false, false))
  }
}
