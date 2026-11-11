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

import android.media.AudioAttributes.CONTENT_TYPE_MOVIE
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.AudioAttributes.CONTENT_TYPE_SPEECH
import android.media.AudioAttributes.USAGE_ALARM
import android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
import android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
import android.media.AudioAttributes.USAGE_GAME
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioAttributes.USAGE_NOTIFICATION
import android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE
import android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
import android.media.AudioManager.STREAM_ACCESSIBILITY
import android.media.AudioManager.STREAM_ALARM
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.STREAM_NOTIFICATION
import android.media.AudioManager.STREAM_RING
import android.media.AudioManager.STREAM_SYSTEM
import android.media.AudioManager.STREAM_VOICE_CALL
import com.google.android.accessibility.talkback.monitor.VolumeChangeFilter.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VolumeChangeFilterTest {
  private val filter = VolumeChangeFilter()

  @Test
  fun aChangeOfTheVolumeIsAStep() {
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_000))
  }

  @Test
  fun anUnchangedVolumeIsNotAStep() {
    assertEquals(Kind.NONE, filter.classify(STREAM_MUSIC, 5, 5, 1_000))
  }

  @Test
  fun aSecondReportOfTheSameChangeIsARepeat() {
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_000))
    assertEquals(Kind.REPEAT, filter.classify(STREAM_MUSIC, 6, 5, 1_050))
  }

  @Test
  fun eachPressOfARapidTrainIsAStep() {
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_000))
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 7, 6, 1_050))
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 8, 7, 1_100))
  }

  @Test
  fun theSameValueOnAnotherStreamIsAStep() {
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_000))
    assertEquals(Kind.STEP, filter.classify(STREAM_RING, 6, 5, 1_050))
  }

  @Test
  fun returningToAValueAfterTheRepeatWindowIsAStep() {
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_000))
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 5, 6, 1_100))
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_500))
  }

  @Test
  fun aChangeRightAfterSandFlysOwnChangeIsOwn() {
    filter.noteOwnChange(1_000)
    assertEquals(Kind.OWN, filter.classify(STREAM_MUSIC, 6, 5, 1_100))
    assertEquals(Kind.OWN, filter.classify(STREAM_MUSIC, 6, 5, 1_150))
  }

  @Test
  fun sandFlysOwnChangeCoversAStreamReportedUnderAnotherName() {
    filter.noteOwnChange(1_000)
    assertEquals(Kind.OWN, filter.classify(STREAM_RING, 6, 5, 1_100))
  }

  @Test
  fun theToneUsesTheUsageOfTheChangedStream() {
    assertEquals(USAGE_MEDIA, volumeToneUsage(STREAM_MUSIC))
    assertEquals(USAGE_NOTIFICATION_RINGTONE, volumeToneUsage(STREAM_RING))
    assertEquals(USAGE_ALARM, volumeToneUsage(STREAM_ALARM))
    assertEquals(USAGE_NOTIFICATION, volumeToneUsage(STREAM_NOTIFICATION))
    assertEquals(USAGE_ASSISTANCE_SONIFICATION, volumeToneUsage(STREAM_SYSTEM))
    assertEquals(USAGE_VOICE_COMMUNICATION, volumeToneUsage(STREAM_VOICE_CALL))
    assertEquals(USAGE_ASSISTANCE_ACCESSIBILITY, volumeToneUsage(STREAM_ACCESSIBILITY))
  }

  @Test
  fun aChangeLaterThanSandFlysOwnChangeIsAStep() {
    filter.noteOwnChange(1_000)
    assertEquals(Kind.STEP, filter.classify(STREAM_MUSIC, 6, 5, 1_600))
  }

  @Test
  fun aRepeatOfAnOwnChangeReportedAfterTheOwnWindowIsARepeat() {
    filter.noteOwnChange(1_000)
    assertEquals(Kind.OWN, filter.classify(STREAM_MUSIC, 6, 5, 1_450))
    assertEquals(Kind.REPEAT, filter.classify(STREAM_MUSIC, 6, 5, 1_550))
  }

  @Test
  fun theToneGateDropsAStepWhileATonePlays() {
    val gate = VolumeToneGate()
    assertTrue(gate.tryStart(1_000, 100))
    assertFalse(gate.tryStart(1_050, 100))
    assertFalse(gate.tryStart(1_099, 100))
  }

  @Test
  fun aDroppedStepDoesNotExtendTheTone() {
    val gate = VolumeToneGate()
    assertTrue(gate.tryStart(1_000, 100))
    assertFalse(gate.tryStart(1_050, 100))
    assertTrue(gate.tryStart(1_100, 100))
  }

  @Test
  fun theToneGateAllowsAToneAfterTheLastEnded() {
    val gate = VolumeToneGate()
    assertTrue(gate.tryStart(1_000, 100))
    assertTrue(gate.tryStart(1_100, 100))
  }

  @Test
  fun clearingTheToneGateAllowsAToneAtOnce() {
    val gate = VolumeToneGate()
    assertTrue(gate.tryStart(1_000, 100))
    gate.clear()
    assertTrue(gate.tryStart(1_010, 100))
  }

  @Test
  fun theToneHasTheRequestedLengthAndFrequency() {
    val samples =
      generateVolumeTone(
        sampleRate = 48_000,
        durationMs = 50,
        frequencyHz = 1_000,
        amplitude = 0.5,
        fadeMs = 5,
      )
    assertEquals(2_400, samples.size)
    val crossings = samples.toList().zipWithNext().count { (a, b) -> a < 0 && b >= 0 }
    assertEquals(50.0, crossings.toDouble(), 2.0)
  }

  @Test
  fun theToneFadesInAndOutAndStaysWithinItsAmplitude() {
    val samples =
      generateVolumeTone(
        sampleRate = 48_000,
        durationMs = 50,
        frequencyHz = 1_000,
        amplitude = 0.5,
        fadeMs = 5,
      )
    assertEquals(0, samples.first().toInt())
    assertEquals(0, samples.last().toInt())
    assertTrue(samples.maxOf { it.toInt() } <= Short.MAX_VALUE / 2)
    assertTrue(samples.maxOf { it.toInt() } > Short.MAX_VALUE / 3)
  }

  @Test
  fun mediaPlaysWhenAnAppPlaysMediaOrAGame() {
    assertTrue(isMediaPlaying(listOf(USAGE_MEDIA), listOf(CONTENT_TYPE_MUSIC), false))
    assertTrue(isMediaPlaying(listOf(USAGE_GAME), listOf(CONTENT_TYPE_SONIFICATION), false))
    assertTrue(
      isMediaPlaying(
        listOf(USAGE_ASSISTANCE_ACCESSIBILITY, USAGE_MEDIA),
        listOf(CONTENT_TYPE_SPEECH, CONTENT_TYPE_MOVIE),
        true,
      )
    )
  }

  @Test
  fun otherAudioIsNotMedia() {
    assertFalse(isMediaPlaying(emptyList(), emptyList(), false))
    assertFalse(
      isMediaPlaying(
        listOf(USAGE_ASSISTANCE_ACCESSIBILITY, USAGE_NOTIFICATION, USAGE_VOICE_COMMUNICATION),
        listOf(CONTENT_TYPE_SPEECH, CONTENT_TYPE_SONIFICATION, CONTENT_TYPE_SPEECH),
        false,
      )
    )
  }

  @Test
  fun ownSpeechAndSoundsAtTheMediaVolumeAreNotMedia() {
    assertFalse(
      isMediaPlaying(
        listOf(USAGE_MEDIA, USAGE_MEDIA),
        listOf(CONTENT_TYPE_SPEECH, CONTENT_TYPE_SONIFICATION),
        true,
      )
    )
    // A podcast app's speech counts when SandFly plays nothing at the media volume.
    assertTrue(isMediaPlaying(listOf(USAGE_MEDIA), listOf(CONTENT_TYPE_SPEECH), false))
  }
}
