/*
 * Copyright 2026 Backtalk contributors
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

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HrtfTest {
  // Unit tests run in the module directory, so the packed measurements can be read directly.
  private val hrtf = Hrtf.parse(File("src/main/res/raw/hrtf_kemar.bin").readBytes())

  private fun energy(samples: FloatArray) = samples.fold(0f) { sum, s -> sum + s * s }

  private fun ears(azimuth: Float, elevation: Float): Pair<FloatArray, FloatArray> {
    val left = FloatArray(hrtf.taps)
    val right = FloatArray(hrtf.taps)
    hrtf.response(azimuth, elevation, left, right)
    return left to right
  }

  @Test
  fun readsThePackedMeasurements() {
    assertEquals(128, hrtf.taps)
  }

  @Test
  fun aSoundOnTheRightIsLouderInTheRightEar() {
    val (left, right) = ears(90f, 0f)
    assertTrue(energy(right) > 4 * energy(left))
  }

  @Test
  fun leftIsTheMirrorOfRight() {
    val (rightLeft, rightRight) = ears(60f, -20f)
    val (leftLeft, leftRight) = ears(-60f, -20f)
    assertArrayEquals(rightLeft, leftRight, 0f)
    assertArrayEquals(rightRight, leftLeft, 0f)
  }

  @Test
  fun inFrontBothEarsAreAlike() {
    val (left, right) = ears(0f, 0f)
    assertEquals(energy(left), energy(right), energy(left) * 0.01f)
  }

  @Test
  fun elevationsBetweenRingsAreBlended() {
    val (below, _) = ears(0f, -10f)
    val (above, _) = ears(0f, 0f)
    val (between, _) = ears(0f, -5f)
    for (t in between.indices) {
      assertEquals((below[t] + above[t]) / 2, between[t], 1e-6f)
    }
  }

  @Test
  fun elevationsBeyondTheRingsUseTheNearestRing() {
    assertArrayEquals(ears(30f, -40f).first, ears(30f, -80f).first, 0f)
    assertArrayEquals(ears(30f, 10f).first, ears(30f, 60f).first, 0f)
  }

  @Test
  fun renderingAnImpulseGivesTheResponse() {
    val impulse = FloatArray(1) { 0.5f }
    val stereo = hrtf.render(impulse, 45f, 0f)
    val (left, right) = ears(45f, 0f)
    assertEquals(hrtf.taps * 2, stereo.size)
    for (t in 0 until hrtf.taps) {
      assertEquals(left[t] * 0.5f, stereo[2 * t], 1e-6f)
      assertEquals(right[t] * 0.5f, stereo[2 * t + 1], 1e-6f)
    }
  }

  @Test
  fun screenMapsToUnspokensAngles() {
    assertEquals(-90f, Hrtf.azimuthForScreen(0f), 0f)
    assertEquals(0f, Hrtf.azimuthForScreen(0.5f), 0f)
    assertEquals(90f, Hrtf.azimuthForScreen(1f), 0f)
    assertEquals(10f, Hrtf.elevationForScreen(0f), 0f)
    assertEquals(-15f, Hrtf.elevationForScreen(0.5f), 0f)
    assertEquals(-40f, Hrtf.elevationForScreen(1f), 0f)
  }

  @Test
  fun decodesMono16BitWav() {
    val samples = SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = 44100, 0, 16384, -32768))
    assertArrayEquals(floatArrayOf(0f, 0.5f, -1f), samples, 0f)
  }

  @Test
  fun mixesStereoWavDownToMono() {
    val samples = SpatialSoundPlayer.decodeWav(wav(channels = 2, rate = 44100, 16384, 0, -32768, 0))
    assertArrayEquals(floatArrayOf(0.25f, -0.5f), samples, 0f)
  }

  @Test
  fun resamplesWavTo44100() {
    val samples = SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = 22050, 0, 16384))!!
    assertEquals(4, samples.size)
    assertArrayEquals(floatArrayOf(0f, 0.25f, 0.5f, 0.5f), samples, 1e-6f)
  }

  @Test
  fun leavesOtherFormatsToTheDecoders() {
    assertNull(SpatialSoundPlayer.decodeWav("OggS".toByteArray()))
  }

  @Test
  fun rejectsSampleRatesThatWouldNeedHugeBuffers() {
    // Resampling 100,000 samples claimed to be at 10 Hz would need about 441 million floats.
    assertNull(SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = 10, *IntArray(100_000))))
    assertNull(SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = 400_000, 0, 1)))
  }

  @Test
  fun rejectsSoundsLongerThanTheLimit() {
    val rate = SpatialSoundPlayer.MIN_SAMPLE_RATE
    val tooLong = IntArray(rate * SpatialSoundPlayer.MAX_SECONDS + 1)
    assertNull(SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = rate, *tooLong)))
    val longest = IntArray(rate * SpatialSoundPlayer.MAX_SECONDS)
    assertTrue(SpatialSoundPlayer.decodeWav(wav(channels = 1, rate = rate, *longest)) != null)
  }

  @Test
  fun rejectsTooManyChannels() {
    assertNull(SpatialSoundPlayer.decodeWav(wav(channels = 9, rate = 44100, *IntArray(9))))
    assertTrue(SpatialSoundPlayer.isSupportedFormat(44100, 8))
    assertTrue(!SpatialSoundPlayer.isSupportedFormat(44100, 0))
  }

  @Test
  fun resamplingKeepsTheLength() {
    assertEquals(44100, SpatialSoundPlayer.resample(FloatArray(48000), 48000, 44100).size)
    val same = FloatArray(10)
    assertTrue(SpatialSoundPlayer.resample(same, 44100, 44100) === same)
  }

  private fun wav(channels: Int, rate: Int, vararg samples: Int): ByteArray {
    val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    samples.forEach { data.putShort(it.toShort()) }
    val format =
      ByteBuffer.allocate(16)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(1)
        .putShort(channels.toShort())
        .putInt(rate)
        .putInt(rate * channels * 2)
        .putShort((channels * 2).toShort())
        .putShort(16)
    val out = ByteArrayOutputStream()
    fun chunk(id: String, body: ByteArray) {
      out.write(id.toByteArray())
      out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(body.size).array())
      out.write(body)
    }
    out.write("RIFF".toByteArray())
    out.write(ByteArray(4))
    out.write("WAVE".toByteArray())
    chunk("fmt ", format.array())
    chunk("data", data.array())
    return out.toByteArray()
  }
}
