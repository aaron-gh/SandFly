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

package com.google.android.accessibility.talkback.quicksettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySizesTest {

  /** A 1080 px wide phone at its own density of 480 dpi, 360 dp wide. */
  private val phone = DisplaySizes(initial = 480, default = 480, current = 480, smallestPx = 1080)

  @Test
  fun thePhoneGoesDownToUnder600DpAndUpTo320Dp() {
    assertEquals(61, phone.min)
    assertEquals(293, phone.density(phone.min))
    assertTrue(1080 * 160.0 / phone.density(phone.min) < 600)
    // One percent less would reach 600 dp.
    assertTrue(1080 * 160.0 / phone.density(phone.min - 1) >= 600)
    assertEquals(112, phone.max)
    assertTrue(1080 * 160.0 / phone.density(phone.max) >= 320)
    assertEquals(listOf(85, 100, 112), phone.options)
    assertEquals(100, phone.currentPercent)
  }

  @Test
  fun theDefaultResetsAndOtherSizesSetADensity() {
    assertEquals("reset", phone.argument(100))
    assertEquals("408", phone.argument(85))
    assertEquals("293", phone.argument(61))
  }

  @Test
  fun aChangedDensityIsAPercentOfTheDefault() {
    val smaller = DisplaySizes(initial = 480, default = 480, current = 384, smallestPx = 1080)
    assertEquals(80, smaller.currentPercent)
  }

  @Test
  fun aLoweredResolutionScalesTheDefaultAndDoesNotReset() {
    // A 1440 px display set to 1080 px, whose own density 600 is for 1440 px.
    val lowered = DisplaySizes(initial = 600, default = 450, current = 450, smallestPx = 1080)
    assertEquals(100, lowered.currentPercent)
    assertEquals("450", lowered.argument(100))
    assertEquals(65, lowered.min)
    assertTrue(1080 * 160.0 / lowered.density(lowered.min) < 600)
  }

  @Test
  fun aDisplayAlready600DpWideGoesDownTo85Percent() {
    val unfolded = DisplaySizes(initial = 420, default = 420, current = 420, smallestPx = 1812)
    assertEquals(85, unfolded.min)
    assertEquals(215, unfolded.max)
    assertEquals(listOf(85, 100, 138, 177, 215), unfolded.options)
  }

  @Test
  fun aDisplayUnder320DpGetsNoLargerSizes() {
    val small = DisplaySizes(initial = 240, default = 240, current = 240, smallestPx = 480)
    assertEquals(100, small.max)
    assertEquals(listOf(85, 100), small.options)
  }
}
