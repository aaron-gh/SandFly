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


package com.google.android.accessibility.brailleime

import org.junit.Assert.assertEquals
import org.junit.Test

class HeldOrientationTrackerTest {
  @Test
  fun upright_isZero() {
    assertEquals(0, HeldOrientationTracker.orientationDegrees(0f, 9.8f, 0f))
  }

  @Test
  fun turnedClockwise_isNinety() {
    assertEquals(90, HeldOrientationTracker.orientationDegrees(-9.8f, 0f, 0f))
  }

  @Test
  fun upsideDown_is180() {
    assertEquals(180, HeldOrientationTracker.orientationDegrees(0f, -9.8f, 0f))
  }

  @Test
  fun turnedAnticlockwise_is270() {
    assertEquals(270, HeldOrientationTracker.orientationDegrees(9.8f, 0f, 0f))
  }

  @Test
  fun tiltedBackUpright_isZero() {
    assertEquals(0, HeldOrientationTracker.orientationDegrees(0f, 6.9f, 6.9f))
  }

  @Test
  fun lyingFlat_isUnknown() {
    assertEquals(
      HeldOrientationTracker.ORIENTATION_UNKNOWN,
      HeldOrientationTracker.orientationDegrees(0.5f, 0.5f, 9.8f),
    )
  }
}
