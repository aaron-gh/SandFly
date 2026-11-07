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

package com.google.android.accessibility.utils.output

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechEqualizerTest {
  private val rate = 48_000

  @Test
  fun damagedSettingsAreFlat() {
    val flat = IntArray(SpeechEqualizer.BANDS)
    assertArrayEquals(flat, SpeechEqualizer.parse(null))
    assertArrayEquals(flat, SpeechEqualizer.parse(""))
    assertArrayEquals(flat, SpeechEqualizer.parse("1,2,3"))
    assertArrayEquals(flat, SpeechEqualizer.parse("1,2,3,4,5,6,7,8,9,10,11,x"))
  }

  @Test
  fun gainsAreKeptInRange() {
    val gains = SpeechEqualizer.parse("99,-99,0,0,0,0,0,0,0,0,0,3")
    assertEquals(SpeechEqualizer.MAX_GAIN_DB, gains[0])
    assertEquals(-SpeechEqualizer.MAX_GAIN_DB, gains[1])
    assertEquals(3, gains[11])
  }

  @Test
  fun formatParsesBack() {
    val gains = SpeechEqualizer.Preset.CLARITY.gains()
    assertArrayEquals(gains, SpeechEqualizer.parse(SpeechEqualizer.format(gains)))
  }

  @Test
  fun presetsAreFound() {
    for (preset in SpeechEqualizer.Preset.entries) {
      assertEquals(SpeechEqualizer.BANDS, preset.gains().size)
      assertEquals(preset, SpeechEqualizer.presetOf(preset.gains()))
      assertEquals(preset, SpeechEqualizer.presetFor(preset.value))
    }
    val custom = SpeechEqualizer.Preset.FLAT.gains().also { it[5] = 1 }
    assertNull(SpeechEqualizer.presetOf(custom))
  }

  @Test
  fun keys() {
    assertTrue(SpeechEqualizer.isKey(SpeechEqualizer.PREF_DEFAULT))
    assertTrue(SpeechEqualizer.isKey(SpeechEqualizer.key("3")))
    assertTrue(VoiceProfiles.isProfileKey(SpeechEqualizer.key("3")))
    assertFalse(SpeechEqualizer.isKey(VoiceProfiles.key("3", VoiceProfiles.RATE)))
    assertFalse(SpeechEqualizer.isKey(VoiceProfiles.PREF_ACTIVE))
    assertFalse(SpeechEqualizer.isKey(null))
  }

  @Test
  fun bandChangesItsFrequencyByItsGain() {
    for (band in 0 until SpeechEqualizer.BANDS) {
      for (gain in intArrayOf(-6, 6)) {
        val gains = IntArray(SpeechEqualizer.BANDS).also { it[band] = gain }
        val frequency = SpeechEqualizer.FREQUENCIES[band].toDouble()
        val measured = gainDb(gains, frequency, amplitude = 0.05f)
        assertEquals("band $band at $gain dB", gain.toDouble(), measured, 0.5)
      }
    }
  }

  @Test
  fun bandLeavesFarFrequenciesAlone() {
    val gains = IntArray(SpeechEqualizer.BANDS).also { it[0] = 12 }
    assertEquals(0.0, gainDb(gains, 4000.0, amplitude = 0.05f), 0.2)
  }

  @Test
  fun limiterKeepsBoostedSpeechFromClipping() {
    val gains = IntArray(SpeechEqualizer.BANDS) { SpeechEqualizer.MAX_GAIN_DB }
    val samples = sine(1000.0, amplitude = 0.9f, frames = rate)
    SpeechEqualizer.Filter(gains, rate, 1).process(samples)
    assertTrue(samples.all { abs(it) <= 0.98f + 1e-4f })
  }

  @Test
  fun stereoChannelsAreFilteredApart() {
    val gains = IntArray(SpeechEqualizer.BANDS).also { it[5] = -12 }
    val left = sine(1000.0, amplitude = 0.1f, frames = rate)
    val stereo = FloatArray(left.size * 2) { if (it % 2 == 0) left[it / 2] else 0f }
    SpeechEqualizer.Filter(gains, rate, 2).process(stereo)
    assertTrue((0 until rate).all { stereo[it * 2 + 1] == 0f })
  }

  @Test
  fun bandsAboveHalfTheRateAreSkipped() {
    val gains = IntArray(SpeechEqualizer.BANDS).also { it[11] = 12 }
    val samples = sine(1000.0, amplitude = 0.1f, frames = 16_000)
    val copy = samples.copyOf()
    SpeechEqualizer.Filter(gains, 16_000, 1).process(samples)
    assertArrayEquals(copy, samples, 0f)
  }

  /** The gain [gains] give a sine at [frequency], in dB, measured after the filter settles. */
  private fun gainDb(gains: IntArray, frequency: Double, amplitude: Float): Double {
    val frames = rate * 2
    val samples = sine(frequency, amplitude, frames)
    val input = rms(samples, frames / 2)
    SpeechEqualizer.Filter(gains, rate, 1).process(samples)
    return 20 * log10(rms(samples, frames / 2) / input)
  }

  private fun sine(frequency: Double, amplitude: Float, frames: Int): FloatArray =
    FloatArray(frames) { (amplitude * sin(2 * PI * frequency * it / rate)).toFloat() }

  private fun rms(samples: FloatArray, from: Int): Double {
    var sum = 0.0
    for (i in from until samples.size) sum += samples[i].toDouble() * samples[i]
    return sqrt(sum / (samples.size - from))
  }
}
