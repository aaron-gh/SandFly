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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests run below Android R, so the composed form is never used here.
class HapticPatternParserTest {
  private var fallback: LongArray? = null
  private var timings: LongArray? = null
  private var amplitudes: IntArray? = null

  private fun parser(canVaryStrength: Boolean) =
    HapticPatternParser(
      { pattern ->
        fallback = pattern
        null
      },
      if (canVaryStrength) {
        { t, a ->
          timings = t
          amplitudes = a
          null
        }
      } else {
        null
      },
      { true },
    )

  private val pattern =
    intArrayOf(0, 30, 40, 15, -9998, 15, 255, 40, 0, 10, 120, -9999, 1, 255, 0, 7, 150, 40)

  @Test
  fun phonesThatCanVaryStrengthGetTheAmplitudes() {
    parser(canVaryStrength = true).parse(pattern)
    assertArrayEquals(longArrayOf(15, 40, 10), timings)
    assertArrayEquals(intArrayOf(255, 0, 120), amplitudes)
    assertNull(fallback)
  }

  @Test
  fun otherPhonesGetTheOnAndOffTimesWithoutTheAmplitudes() {
    parser(canVaryStrength = false).parse(pattern)
    assertArrayEquals(longArrayOf(0, 30, 40, 15), fallback)
    assertNull(timings)
  }

  @Test
  fun anAmplitudeFormWithoutAComposedFormRunsToTheEnd() {
    parser(canVaryStrength = true).parse(intArrayOf(0, 20, -9998, 10, 200, 10, 100))
    assertArrayEquals(longArrayOf(10, 10), timings)
    assertArrayEquals(intArrayOf(200, 100), amplitudes)
  }

  @Test
  fun aPatternWithoutSeparatorsIsOnlyOnAndOffTimes() {
    parser(canVaryStrength = true).parse(intArrayOf(0, 25, 50, 25))
    assertArrayEquals(longArrayOf(0, 25, 50, 25), fallback)
    assertNull(timings)
  }
}
