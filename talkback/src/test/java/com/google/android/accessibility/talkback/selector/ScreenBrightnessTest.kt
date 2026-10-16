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

package com.google.android.accessibility.talkback.selector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenBrightnessTest {
  @Test
  fun limits_haveNoNextStep() {
    assertNull(ScreenBrightness.nextSetting(255, increase = true))
    assertNull(ScreenBrightness.nextSetting(1, increase = false))
  }

  @Test
  fun steps_moveTheRightWay() {
    val brighter = ScreenBrightness.nextSetting(128, increase = true)!!
    val dimmer = ScreenBrightness.nextSetting(128, increase = false)!!
    assertTrue(brighter > 128)
    assertTrue(dimmer < 128)
  }

  @Test
  fun zero_isTreatedAsTheMinimum() {
    assertNull(ScreenBrightness.nextSetting(0, increase = false))
    val brighter = ScreenBrightness.nextSetting(0, increase = true)
    assertNotNull(brighter)
    assertTrue(brighter!! > 1)
    assertEquals(0, ScreenBrightness.settingToPercent(0))
  }

  @Test
  fun aboveTheMaximum_isTreatedAsTheMaximum() {
    assertNull(ScreenBrightness.nextSetting(2047, increase = true))
    val dimmer = ScreenBrightness.nextSetting(2047, increase = false)
    assertNotNull(dimmer)
    assertTrue(dimmer!! < 255)
    assertEquals(100, ScreenBrightness.settingToPercent(2047))
  }
}
