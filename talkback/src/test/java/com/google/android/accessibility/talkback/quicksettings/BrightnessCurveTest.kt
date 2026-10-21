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

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessCurveTest {

  @Test
  fun matchesTheSystemSlider() {
    // The emulator's system slider showed 83% for a setting of 102.
    assertEquals(83, (BrightnessCurve.toSlider(102) * 100).roundToInt())
    assertEquals(0, (BrightnessCurve.toSlider(BrightnessCurve.MIN) * 100).roundToInt())
    assertEquals(100, (BrightnessCurve.toSlider(BrightnessCurve.MAX) * 100).roundToInt())
  }

  @Test
  fun goesBothWays() {
    for (setting in listOf(1, 10, 50, 102, 200, 255)) {
      val back = BrightnessCurve.toSetting(BrightnessCurve.toSlider(setting))
      assertEquals(setting.toDouble(), back.toDouble(), 1.0)
    }
  }
}
