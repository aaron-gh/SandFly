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

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/**
 * Head-related transfer functions, which make a mono sound seem to come from a direction in
 * headphones. The responses are the MIT Media Lab KEMAR measurements by Bill Gardner and Keith
 * Martin, packed by tools/control_sounds/make_hrtf.py, at 44.1 kHz.
 *
 * Each elevation ring holds responses for sources on the right, from the front (azimuth 0) round
 * to behind (180). A source on the left uses the response of its mirror image with the ears
 * swapped. Between rings, the two nearest responses are blended.
 */
class Hrtf private constructor(val taps: Int, private val rings: List<Ring>) {

  private class Ring(
    val elevation: Int,
    val azimuths: IntArray,
    val left: Array<FloatArray>,
    val right: Array<FloatArray>,
  ) {
    fun nearest(azimuth: Float): Int {
      var best = 0
      for (i in azimuths.indices) {
        if (abs(azimuths[i] - azimuth) < abs(azimuths[best] - azimuth)) best = i
      }
      return best
    }
  }

  /**
   * Fills [left] and [right], each [taps] long, with the response for a source at [azimuth]
   * degrees (negative to the left, positive to the right) and [elevation] degrees (negative
   * below). Elevations outside the measured rings use the nearest ring.
   */
  fun response(azimuth: Float, elevation: Float, left: FloatArray, right: FloatArray) {
    val side = abs(azimuth).coerceAtMost(180f)
    val upper = rings.indexOfFirst { it.elevation >= elevation }.let { if (it < 0) rings.size - 1 else it }
    val lower = max(upper - 1, 0)
    val span = rings[upper].elevation - rings[lower].elevation
    val upperWeight =
      if (span == 0) 1f
      else ((elevation - rings[lower].elevation) / span).coerceIn(0f, 1f)

    val lowerIndex = rings[lower].nearest(side)
    val upperIndex = rings[upper].nearest(side)
    // Mirrored sources hear the right ear's response in the left ear.
    val mirrored = azimuth < 0
    val lowerNear = if (mirrored) rings[lower].right else rings[lower].left
    val lowerFar = if (mirrored) rings[lower].left else rings[lower].right
    val upperNear = if (mirrored) rings[upper].right else rings[upper].left
    val upperFar = if (mirrored) rings[upper].left else rings[upper].right
    for (t in 0 until taps) {
      left[t] =
        lowerNear[lowerIndex][t] * (1 - upperWeight) + upperNear[upperIndex][t] * upperWeight
      right[t] =
        lowerFar[lowerIndex][t] * (1 - upperWeight) + upperFar[upperIndex][t] * upperWeight
    }
  }

  /**
   * Returns [mono] as interleaved stereo, heard from [azimuth] and [elevation] degrees. The result
   * is [taps] - 1 frames longer than the input, so the filters' tails are not cut off.
   */
  fun render(mono: FloatArray, azimuth: Float, elevation: Float): FloatArray {
    val left = FloatArray(taps)
    val right = FloatArray(taps)
    response(azimuth, elevation, left, right)
    val frames = mono.size + taps - 1
    val out = FloatArray(frames * 2)
    for (n in 0 until frames) {
      var l = 0f
      var r = 0f
      val first = max(0, n - mono.size + 1)
      val last = minOf(taps - 1, n)
      for (t in first..last) {
        val s = mono[n - t]
        l += s * left[t]
        r += s * right[t]
      }
      out[2 * n] = l.coerceIn(-1f, 1f)
      out[2 * n + 1] = r.coerceIn(-1f, 1f)
    }
    return out
  }

  companion object {
    const val SAMPLE_RATE = 44100

    /** Reads the format written by tools/control_sounds/make_hrtf.py. */
    @JvmStatic
    fun parse(bytes: ByteArray): Hrtf {
      val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      val magic = ByteArray(4).also { buffer.get(it) }
      require(String(magic, Charsets.US_ASCII) == "HRTF") { "Not an HRTF file" }
      val taps = buffer.short.toInt()
      val ringCount = buffer.short.toInt()
      val scale = buffer.float
      val rings =
        List(ringCount) {
          val elevation = buffer.short.toInt()
          val count = buffer.short.toInt()
          val azimuths = IntArray(count)
          val left = Array(count) { FloatArray(taps) }
          val right = Array(count) { FloatArray(taps) }
          for (i in 0 until count) {
            azimuths[i] = buffer.short.toInt()
            for (t in 0 until taps) left[i][t] = buffer.short * scale
            for (t in 0 until taps) right[i][t] = buffer.short * scale
          }
          Ring(elevation, azimuths, left, right)
        }
      return Hrtf(taps, rings.sortedBy { it.elevation })
    }

    /**
     * Unspoken's mapping from screen to sound, in degrees: the width of the screen spans from 90
     * degrees left to 90 degrees right, and the height from 40 degrees below to 10 degrees above.
     * [x] and [y] are fractions of the screen from its left and top edges.
     */
    @JvmStatic fun azimuthForScreen(x: Float): Float = ((x - 0.5f) * 180f).coerceIn(-90f, 90f)

    @JvmStatic fun elevationForScreen(y: Float): Float = (10f - 50f * y).coerceIn(-40f, 10f)
  }
}
