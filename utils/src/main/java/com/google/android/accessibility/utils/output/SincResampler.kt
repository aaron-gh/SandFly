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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Changes the sample rate of interleaved float audio with a Kaiser-windowed sinc filter, a piece at
 * a time, so that speech can be converted as it arrives. Speech from an engine at 11 or 22 kHz
 * becomes the output's rate without the harshness of simple interpolation.
 */
class SincResampler(
  private val inRate: Int,
  private val outRate: Int,
  private val channels: Int,
) {
  private val table = kernel(min(1.0, outRate.toDouble() / inRate) * CUTOFF)

  // Input frames not yet used up, interleaved, starting with zeros so the first frames have
  // history. Positions are kept as whole numbers, so that converting in pieces gives exactly the
  // frames converting at once would.
  private var buffer = FloatArray(HALF_TAPS * channels)
  private var bufferFrames = HALF_TAPS
  // Input frames dropped from the start of the buffer, and output frames made, so far.
  private var dropped = 0L
  private var outIndex = 0L

  /** Converts [input], interleaved frames, and returns as many output frames as it can so far. */
  fun process(input: FloatArray): FloatArray {
    if (inRate == outRate) return input
    append(input)
    return render()
  }

  /** Returns the output frames still held back for the filter, after the last input. */
  fun flush(): FloatArray {
    if (inRate == outRate) return FloatArray(0)
    append(FloatArray(HALF_TAPS * channels))
    return render()
  }

  private fun append(input: FloatArray) {
    val frames = input.size / channels
    if ((bufferFrames + frames) * channels > buffer.size) {
      buffer = buffer.copyOf(maxOf(buffer.size * 2, (bufferFrames + frames) * channels))
    }
    System.arraycopy(input, 0, buffer, bufferFrames * channels, frames * channels)
    bufferFrames += frames
  }

  /** The input frame, in the buffer, that output frame [index] centres on. */
  private fun center(index: Long): Int = (HALF_TAPS + index * inRate / outRate - dropped).toInt()

  /** Renders every output frame whose filter fits in the buffer, then drops used input. */
  private fun render(): FloatArray {
    val out = AudioDecoder.FloatList()
    while (center(outIndex) + HALF_TAPS < bufferFrames) {
      val center = center(outIndex)
      val phase = ((outIndex * inRate % outRate) * PHASES / outRate).toInt()
      val taps = table[phase]
      val first = center - HALF_TAPS + 1
      for (c in 0 until channels) {
        var sum = 0f
        var index = first * channels + c
        for (tap in taps) {
          sum += tap * buffer[index]
          index += channels
        }
        out.add(sum)
      }
      outIndex++
    }
    // Keep only the history the next output frame needs.
    val keepFrom = center(outIndex) - HALF_TAPS + 1
    if (keepFrom > 0) {
      System.arraycopy(buffer, keepFrom * channels, buffer, 0, (bufferFrames - keepFrom) * channels)
      bufferFrames -= keepFrom
      dropped += keepFrom
    }
    return out.toArray()
  }

  companion object {
    private const val HALF_TAPS = 16
    private const val PHASES = 256
    private const val CUTOFF = 0.95
    private const val KAISER_BETA = 8.0

    /** Filter taps for each fractional position between input frames. */
    private fun kernel(cutoff: Double): Array<FloatArray> =
      Array(PHASES) { phase ->
        val fraction = phase.toDouble() / PHASES
        val taps = FloatArray(HALF_TAPS * 2)
        var sum = 0.0
        val values = DoubleArray(HALF_TAPS * 2) { i ->
          val x = (i - HALF_TAPS + 1) - fraction
          val value = cutoff * sinc(cutoff * x) * kaiser(x / HALF_TAPS)
          sum += value
          value
        }
        // Each phase passes steady signals unchanged.
        for (i in taps.indices) taps[i] = (values[i] / sum).toFloat()
        taps
      }

    private fun sinc(x: Double): Double = if (abs(x) < 1e-9) 1.0 else sin(PI * x) / (PI * x)

    private fun kaiser(x: Double): Double {
      if (abs(x) > 1) return 0.0
      return besselI0(KAISER_BETA * sqrt(1 - x * x)) / besselI0(KAISER_BETA)
    }

    private fun besselI0(x: Double): Double {
      var sum = 1.0
      var term = 1.0
      var k = 1
      while (term > 1e-10 * sum) {
        term *= (x / (2 * k)) * (x / (2 * k))
        sum += term
        k++
      }
      return sum
    }
  }
}
