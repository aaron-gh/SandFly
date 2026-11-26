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

package com.google.android.accessibility.talkback.recognition

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightToneTest {
  private fun plane(width: Int, height: Int, rowStride: Int, value: Int, padding: Int = 0): ByteBuffer {
    val bytes = ByteArray(rowStride * height)
    for (y in 0 until height) {
      for (x in 0 until width) bytes[y * rowStride + x] = value.toByte()
      for (x in width until rowStride) bytes[y * rowStride + x] = padding.toByte()
    }
    return ByteBuffer.wrap(bytes)
  }

  @Test fun meanOfBlackAndWhitePlanes() {
    assertEquals(0f, LightToneLevel.meanLinear(plane(64, 48, 64, 0), 64, 48, 64, 1), 1e-6f)
    assertEquals(1f, LightToneLevel.meanLinear(plane(64, 48, 64, 255), 64, 48, 64, 1), 1e-6f)
  }

  @Test fun rowPaddingIsNotRead() {
    val mean = LightToneLevel.meanLinear(plane(64, 48, 80, 0, padding = 255), 64, 48, 80, 1)
    assertEquals(0f, mean, 1e-6f)
  }

  @Test fun bufferTooSmallForItsSizeIsRejected() {
    assertEquals(-1f, LightToneLevel.meanLinear(ByteBuffer.allocate(100), 64, 48, 64, 1), 0f)
    assertEquals(-1f, LightToneLevel.meanLinear(ByteBuffer.allocate(100), 0, 0, 64, 1), 0f)
  }

  @Test fun readingLeavesTheBufferPosition() {
    val buffer = plane(16, 16, 16, 128)
    LightToneLevel.meanLinear(buffer, 16, 16, 16, 1)
    assertEquals(0, buffer.position())
  }

  @Test fun frameLevelFollowsBrightnessWithoutExposure() {
    assertEquals(0f, LightToneLevel.level(0f), 1e-6f)
    assertEquals(1f, LightToneLevel.level(1f), 1e-6f)
    // Mid grey in linear light looks about half as bright.
    assertEquals(0.46f, LightToneLevel.level(0.18f), 0.02f)
    assertEquals(LightToneLevel.level(0.5f), LightToneLevel.level(0.5f, 0L, 0, 0f), 0f)
  }

  @Test fun sceneLevelUsesTheExposure() {
    // The same frame brightness: a long exposure at a high ISO means a dark scene.
    val dark = LightToneLevel.level(0.18f, 66_000_000L, 3200, 1.8f)
    val room = LightToneLevel.level(0.18f, 16_000_000L, 400, 1.8f)
    val sun = LightToneLevel.level(0.18f, 125_000L, 50, 1.8f)
    assertTrue(dark < room)
    assertTrue(room < sun)
    assertTrue(dark < 0.2f)
    assertTrue(sun > 0.8f)
    // A brighter frame at the same exposure means a brighter scene.
    assertTrue(LightToneLevel.level(0.5f, 16_000_000L, 400, 1.8f) > room)
    // Without the aperture, the usual one is assumed.
    assertEquals(room, LightToneLevel.level(0.18f, 16_000_000L, 400, 0f), 1e-6f)
  }

  @Test fun pitchIsLogarithmicBetweenTheLimits() {
    assertEquals(LightToneLevel.MIN_HZ, LightToneLevel.frequency(0f), 1e-6)
    assertEquals(LightToneLevel.MAX_HZ, LightToneLevel.frequency(1f), 1e-6)
    // One octave per third of the range.
    assertEquals(LightToneLevel.MIN_HZ * 2, LightToneLevel.frequency(1f / 3), 1e-3)
    assertEquals(LightToneLevel.MAX_HZ, LightToneLevel.frequency(2f), 1e-6)
  }

  @Test fun toneFadesInWithoutAClick() {
    val synth = ToneSynth(48_000)
    val out = ShortArray(960)
    synth.fill(out, out.size, 440.0, 0.5)
    // The first millisecond stays quiet.
    for (i in 0 until 48) assertTrue(kotlin.math.abs(out[i].toInt()) < 0.05 * Short.MAX_VALUE)
    assertTrue(synth.amplitude > 0.05 && synth.amplitude < 0.5)
    repeat(50) { synth.fill(out, out.size, 440.0, 0.5) }
    assertEquals(0.5, synth.amplitude, 0.01)
    assertTrue(out.all { kotlin.math.abs(it.toInt()) <= 0.5 * Short.MAX_VALUE + 1 })
  }

  @Test fun toneIsSilentUntilThereIsAPitch() {
    val synth = ToneSynth(48_000)
    val out = ShortArray(100) { 1 }
    synth.fill(out, out.size, Double.NaN, 0.5)
    assertTrue(out.all { it.toInt() == 0 })
  }

  @Test fun toneGlidesToTheNewPitch() {
    val synth = ToneSynth(48_000)
    val out = ShortArray(48_000)
    synth.fill(out, out.size, 400.0, 0.5)
    synth.fill(out, out.size, 800.0, 0.5)
    assertEquals(800.0, synth.hz, 1.0)
    synth.fill(out, out.size, 800.0, 0.5)
    // 800 cycles in a second at the new pitch, two sign changes each.
    var changes = 0
    for (i in 1 until out.size) if ((out[i - 1] < 0) != (out[i] < 0)) changes++
    assertEquals(1600.0, changes.toDouble(), 10.0)
  }
}
