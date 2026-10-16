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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SincResamplerTest {
  private fun resampleAll(input: FloatArray, from: Int, to: Int, channels: Int = 1): FloatArray {
    val resampler = SincResampler(from, to, channels)
    return resampler.process(input) + resampler.flush()
  }

  @Test
  fun theSameRateIsUnchanged() {
    val input = floatArrayOf(0.1f, -0.2f, 0.3f)
    assertArrayEquals(input, SincResampler(48000, 48000, 1).process(input), 0f)
  }

  @Test
  fun keepsTheLength() {
    val out = resampleAll(FloatArray(11025), 11025, 48000)
    assertEquals(48000.0, out.size.toDouble(), 48000 * 0.01)
  }

  @Test
  fun aSteadySignalKeepsItsLevel() {
    val out = resampleAll(FloatArray(2205) { 0.5f }, 22050, 48000)
    // Away from the edges, where the filter runs into silence.
    for (i in 200 until out.size - 200) assertEquals(0.5f, out[i], 0.005f)
  }

  @Test
  fun aToneKeepsItsLevelAndPitch() {
    val tone = 1000.0
    val input = FloatArray(11025) { sin(2 * PI * tone * it / 11025).toFloat() * 0.5f }
    val out = resampleAll(input, 11025, 48000)
    val middle = out.copyOfRange(4800, out.size - 4800)
    val rms = sqrt(middle.map { it * it }.average())
    assertEquals(0.5 / sqrt(2.0), rms, 0.01)
    // A 1 kHz tone crosses zero 2000 times a second.
    val crossings = (1 until middle.size).count { (middle[it - 1] < 0) != (middle[it] < 0) }
    assertEquals(2000.0 * middle.size / 48000, crossings.toDouble(), 3.0)
  }

  @Test
  fun piecesGiveTheSameResultAsTheWhole() {
    val input = FloatArray(4000) { sin(it * 0.05).toFloat() }
    val whole = resampleAll(input, 16000, 48000)
    val resampler = SincResampler(16000, 48000, 1)
    var pieces = FloatArray(0)
    for (start in 0 until 4000 step 333) {
      pieces += resampler.process(input.copyOfRange(start, minOf(4000, start + 333)))
    }
    pieces += resampler.flush()
    assertEquals(whole.size, pieces.size)
    for (i in whole.indices) assertTrue("frame $i", abs(whole[i] - pieces[i]) < 1e-5f)
  }

  @Test
  fun stereoChannelsStaySeparate() {
    val input = FloatArray(2000) { if (it % 2 == 0) 0.5f else -0.25f }
    val out = resampleAll(input, 24000, 48000, channels = 2)
    for (frame in 100 until out.size / 2 - 100) {
      assertEquals(0.5f, out[frame * 2], 0.01f)
      assertEquals(-0.25f, out[frame * 2 + 1], 0.01f)
    }
  }

  @Test
  fun monoBecomesStereo() {
    assertArrayEquals(
      floatArrayOf(0.1f, 0.1f, 0.2f, 0.2f),
      LowLatencyAudio.toStereo(floatArrayOf(0.1f, 0.2f), 1),
      0f,
    )
  }
}
