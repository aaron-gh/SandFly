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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// On Android R and later, where vibrators can play primitives.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HapticPatternParserPrimitivesTest {
  private var fallback: LongArray? = null
  private var timings: LongArray? = null

  private val pattern = intArrayOf(0, 30, -9998, 8, 110, -9999, 7, 140, 0)

  private fun parser(canVaryStrength: Boolean = true) =
    HapticPatternParser(
      { pattern ->
        fallback = pattern
        null
      },
      if (canVaryStrength) {
        { t, _ ->
          timings = t
          null
        }
      } else {
        null
      },
      { true },
    )

  @Test
  fun hapticEffectsArePrimitives() {
    val effect = parser().parse(pattern)
    assertNotNull(effect)
    assertNull(timings)
    assertNull(fallback)
  }

  @Test
  fun pulsesAreTheAmplitudeFormEvenWithPrimitives() {
    val parser = parser()
    parser.setType(HapticPatternParser.TYPE_PULSES)
    parser.parse(pattern)
    assertArrayEquals(longArrayOf(8), timings)
    assertNull(fallback)
  }

  @Test
  fun onAndOffIsTheOnAndOffTimes() {
    val parser = parser()
    parser.setType(HapticPatternParser.TYPE_ON_OFF)
    parser.parse(pattern)
    assertArrayEquals(longArrayOf(0, 30), fallback)
    assertNull(timings)
  }

  @Test
  fun pulsesWithoutAnAmplitudeFormAreTheOnAndOffTimes() {
    val parser = parser()
    parser.setType(HapticPatternParser.TYPE_PULSES)
    parser.parse(intArrayOf(0, 30, -9999, 7, 140, 0))
    assertArrayEquals(longArrayOf(0, 30), fallback)
  }
}
