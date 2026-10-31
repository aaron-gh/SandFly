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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandFlyVolumeTest {

  @Test
  fun stepsGoUpAndDownByFive() {
    assertEquals(105, SandFlyVolume.stepped(100, raise = true))
    assertEquals(95, SandFlyVolume.stepped(100, raise = false))
  }

  @Test
  fun aValueBetweenStepsMovesOntoTheNearestStepAway() {
    assertEquals(80, SandFlyVolume.stepped(83, raise = false))
    assertEquals(85, SandFlyVolume.stepped(83, raise = true))
  }

  @Test
  fun stepsStayInRange() {
    assertEquals(SandFlyVolume.MAX_PERCENT, SandFlyVolume.stepped(SandFlyVolume.MAX_PERCENT, true))
    assertEquals(SandFlyVolume.MIN_PERCENT, SandFlyVolume.stepped(SandFlyVolume.MIN_PERCENT, false))
  }

  @Test
  fun quietAudioPassesTheLimiterUnchanged() {
    assertEquals(0.5f, SandFlyVolume.limit(0.5f), 0f)
    assertEquals(-0.8f, SandFlyVolume.limit(-0.8f), 0f)
  }

  @Test
  fun boostedPeaksStayBelowFullScaleAndKeepTheirOrder() {
    val a = SandFlyVolume.limit(1.2f)
    val b = SandFlyVolume.limit(2.0f)
    assertTrue(a > 0.8f && a < 1f)
    assertTrue(b > a && b < 1f)
    assertEquals(-a, SandFlyVolume.limit(-1.2f), 1e-6f)
  }
}
