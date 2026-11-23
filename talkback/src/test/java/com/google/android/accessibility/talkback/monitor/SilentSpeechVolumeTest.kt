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

import android.media.AudioManager.STREAM_ACCESSIBILITY
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.STREAM_RING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH #177: Volume keys > Change speech volume silently while speaking. */
class SilentSpeechVolumeTest {
  private val speech = STREAM_ACCESSIBILITY

  private fun on() = SilentSpeechVolume().apply { enabled = true }

  @Test
  fun off_announcesEveryChange() {
    val silent = SilentSpeechVolume()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    assertFalse(silent.isSilent(speech, speech, now = 1_010))
  }

  @Test
  fun pressWhileSpeaking_changesSpeechVolumeSilently() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    assertTrue(silent.isSilent(speech, speech, now = 1_010))
  }

  @Test
  fun pressWhileQuiet_isAnnounced() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = false, touching = false)
    assertFalse(silent.isSilent(speech, speech, now = 1_010))
  }

  @Test
  fun fingerOnScreen_isAnnounced() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = true)
    assertFalse(silent.isSilent(speech, speech, now = 1_010))
  }

  @Test
  fun otherStreams_areAnnounced() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    assertFalse(silent.isSilent(STREAM_MUSIC, speech, now = 1_010))
    assertFalse(silent.isSilent(STREAM_RING, speech, now = 1_010))
  }

  @Test
  fun changeWithNoPressBeforeIt_isAnnounced() {
    val silent = on()
    assertFalse(silent.isSilent(speech, speech, now = 1_010))
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    // Such as from the volume panel, after the series of presses.
    assertFalse(silent.isSilent(speech, speech, now = 1_000 + SilentSpeechVolume.SERIES_MS))
  }

  @Test
  fun seriesStaysSilentAfterSpeechEnds() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    // A held key repeats, and speech has ended.
    silent.onVolumeKeyDown(now = 1_400, speaking = false, touching = false)
    silent.onVolumeKeyDown(now = 1_450, speaking = false, touching = false)
    assertTrue(silent.isSilent(speech, speech, now = 1_460))
  }

  @Test
  fun newSeriesAfterSpeechEnds_isAnnounced() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = true, touching = false)
    silent.onVolumeKeyDown(
      now = 1_000 + SilentSpeechVolume.SERIES_MS,
      speaking = false,
      touching = false,
    )
    assertFalse(silent.isSilent(speech, speech, now = 1_010 + SilentSpeechVolume.SERIES_MS))
  }

  @Test
  fun eachPressInSeries_decidesBySpeechAndTouchAtThatPress() {
    val silent = on()
    silent.onVolumeKeyDown(now = 1_000, speaking = false, touching = false)
    // The press is the one that decides: speech started by now.
    silent.onVolumeKeyDown(now = 1_300, speaking = true, touching = false)
    assertTrue(silent.isSilent(speech, speech, now = 1_310))
    silent.onVolumeKeyDown(now = 1_600, speaking = true, touching = true)
    assertFalse(silent.isSilent(speech, speech, now = 1_610))
  }

  @Test
  fun speechVolumeStream_isMediaWithoutAnAccessibilityStream() {
    assertEquals(
      STREAM_ACCESSIBILITY,
      SilentSpeechVolume.speechVolumeStream(STREAM_ACCESSIBILITY, hasAccessibilityStream = true),
    )
    assertEquals(
      STREAM_MUSIC,
      SilentSpeechVolume.speechVolumeStream(STREAM_ACCESSIBILITY, hasAccessibilityStream = false),
    )
    assertEquals(
      STREAM_RING,
      SilentSpeechVolume.speechVolumeStream(STREAM_RING, hasAccessibilityStream = true),
    )
  }
}
