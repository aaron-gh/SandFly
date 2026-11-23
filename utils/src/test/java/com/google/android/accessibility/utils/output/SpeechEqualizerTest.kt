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
    assertArrayEquals(flat, SpeechEqualizer.parse("1,2,3,4,5,6,7,8,9,10,x"))
    assertArrayEquals(flat, SpeechEqualizer.parse("1,2,3,4,5,6,7,8,9,10,11,x"))
    assertArrayEquals(flat, SpeechEqualizer.parse("1,2,3,4,5,6,7,8,9,10,11,12,13"))
  }

  @Test
  fun gainsAreKeptInRange() {
    val gains = SpeechEqualizer.parse("99,-99,0,0,0,0,0,0,0,0,3")
    assertEquals(SpeechEqualizer.MAX_GAIN_DB, gains[0])
    assertEquals(-SpeechEqualizer.MAX_GAIN_DB, gains[1])
    assertEquals(3, gains[10])
  }

  @Test
  fun twelveBandSettingsLoseTheirFirstBand() {
    assertArrayEquals(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      SpeechEqualizer.parse("9,1,2,3,4,5,6,7,8,9,10,11"),
    )
    // Presets saved with 12 bands are still found.
    assertEquals(
      SpeechEqualizer.Preset.BASS_BOOST,
      SpeechEqualizer.presetOf(SpeechEqualizer.parseValues("6,6,5,3,1,0,0,0,0,0,0,0")),
    )
  }

  @Test
  fun effectsAreSavedAfterTheGains() {
    val values = SpeechEqualizer.Values(SpeechEqualizer.Preset.CLARITY.gains(), intArrayOf(1, 0, 3))
    val saved = SpeechEqualizer.format(values)
    assertEquals(values, SpeechEqualizer.parseValues(saved))
    assertArrayEquals(SpeechEqualizer.Preset.CLARITY.gains(), SpeechEqualizer.parse(saved))
    // Without effects, the value is as before effects.
    val plain = SpeechEqualizer.Values(SpeechEqualizer.Preset.CLARITY.gains(), IntArray(3))
    assertEquals(SpeechEqualizer.format(plain.gains()), SpeechEqualizer.format(plain))
    assertArrayEquals(IntArray(3), SpeechEqualizer.parseEffects(SpeechEqualizer.format(plain)))
  }

  @Test
  fun damagedEffectsAreOff() {
    val gains = "1,2,3,4,5,6,7,8,9,10,11"
    assertArrayEquals(IntArray(3), SpeechEqualizer.parseEffects("$gains;1,2"))
    assertArrayEquals(IntArray(3), SpeechEqualizer.parseEffects("$gains;1,x,2"))
    assertArrayEquals(intArrayOf(3, 0, 1), SpeechEqualizer.parseEffects("$gains;9,-1,1"))
    assertArrayEquals(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), SpeechEqualizer.parse("$gains;1,x,2"))
  }

  @Test
  fun quietBassIsRaisedByBassCompression() {
    val gains = IntArray(SpeechEqualizer.BANDS)
    val medium = intArrayOf(2, 0, 0)
    // Below the threshold, the range gets the level's whole gain.
    assertEquals(6.0, gainDb(gains, 100.0, amplitude = 0.01f, effects = medium), 0.5)
    assertEquals(0.0, gainDb(gains, 1000.0, amplitude = 0.01f, effects = medium), 0.5)
  }

  @Test
  fun loudBassIsCompressed() {
    val gains = IntArray(SpeechEqualizer.BANDS)
    // 9 dB over the -15 dB threshold at 3:1 is 6 dB lower, which the 6 dB gain makes up.
    assertEquals(0.0, gainDb(gains, 100.0, amplitude = 0.5f, effects = intArrayOf(2, 0, 0)), 1.5)
  }

  @Test
  fun rangesAddUpToTheSameLevel() {
    val gains = IntArray(SpeechEqualizer.BANDS)
    // Only the treble range is compressed, so speech below it keeps its level.
    for (frequency in doubleArrayOf(60.0, 200.0, 1000.0, 1500.0)) {
      assertEquals(
        "at $frequency Hz",
        0.0,
        gainDb(gains, frequency, amplitude = 0.01f, effects = intArrayOf(0, 0, 1)),
        0.3,
      )
    }
  }

  @Test
  fun limiterKeepsCompressedSpeechFromClipping() {
    val samples = sine(150.0, amplitude = 0.95f, frames = rate)
    SpeechEqualizer.Filter(IntArray(SpeechEqualizer.BANDS), rate, 1, intArrayOf(3, 3, 3)).process(samples)
    assertTrue(samples.all { abs(it) <= 0.98f + 1e-4f })
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
      assertEquals(SpeechEqualizer.EFFECTS, preset.effects().size)
      assertEquals(preset, SpeechEqualizer.presetOf(preset.equalizerValues()))
      assertEquals(preset, SpeechEqualizer.presetFor(preset.value))
    }
    val custom = SpeechEqualizer.Preset.FLAT.gains().also { it[5] = 1 }
    assertNull(SpeechEqualizer.presetOf(SpeechEqualizer.Values(custom, IntArray(SpeechEqualizer.EFFECTS))))
    val deepBassWithoutEffects =
      SpeechEqualizer.Values(SpeechEqualizer.Preset.DEEP_BASS.gains(), IntArray(SpeechEqualizer.EFFECTS))
    assertNull(SpeechEqualizer.presetOf(deepBassWithoutEffects))
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
    val gains = IntArray(SpeechEqualizer.BANDS).also { it[SpeechEqualizer.BANDS - 1] = 12 }
    val samples = sine(1000.0, amplitude = 0.1f, frames = 16_000)
    val copy = samples.copyOf()
    SpeechEqualizer.Filter(gains, 16_000, 1).process(samples)
    assertArrayEquals(copy, samples, 0f)
  }

  @Test
  fun responseMatchesTheFilter() {
    val presetGains = SpeechEqualizer.Preset.entries.map { it.gains() }
    val custom = intArrayOf(-12, 0, 6, -4, 12, -8, 0, 2, -6, 9, -3)
    for (gains in presetGains + listOf(custom)) {
      for (frequency in doubleArrayOf(50.0, 200.0, 700.0, 1500.0, 3500.0, 7000.0, 12000.0)) {
        assertEquals(
          "${gains.joinToString()} at $frequency Hz",
          gainDb(gains, frequency, amplitude = 0.01f),
          SpeechEqualizer.responseDb(gains, frequency, rate),
          0.3,
        )
      }
    }
  }

  @Test
  fun flatResponseIsZero() {
    val gains = IntArray(SpeechEqualizer.BANDS)
    assertEquals(0.0, SpeechEqualizer.responseDb(gains, 1000.0, rate), 0.0)
  }

  /** The gain [gains] give a sine at [frequency], in dB, measured after the filter settles. */
  private fun gainDb(
    gains: IntArray,
    frequency: Double,
    amplitude: Float,
    effects: IntArray = IntArray(SpeechEqualizer.EFFECTS),
  ): Double {
    val frames = rate * 2
    val samples = sine(frequency, amplitude, frames)
    val input = rms(samples, frames / 2)
    SpeechEqualizer.Filter(gains, rate, 1, effects).process(samples)
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
