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

import com.google.android.accessibility.brailleime.OrientationMonitor.Orientation
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationMonitorTest {
  /**
   * The orientation after a reading of a device tilted [tiltDegrees] from lying flat, turned
   * [turnDegrees] clockwise from upright, as [android.view.OrientationEventListener] measures it.
   */
  private fun next(current: Orientation, tiltDegrees: Double, turnDegrees: Double): Orientation {
    val tilt = Math.toRadians(tiltDegrees)
    val turn = Math.toRadians(turnDegrees)
    val g = 9.8
    return OrientationMonitor.nextOrientation(
      current,
      /* defaultPortrait= */ true,
      (-g * sin(tilt) * sin(turn)).toFloat(),
      (g * sin(tilt) * cos(turn)).toFloat(),
      (g * cos(tilt)).toFloat(),
    )
  }

  @Test
  fun lyingFlat_isPortrait() {
    assertEquals(Orientation.PORTRAIT, next(Orientation.LANDSCAPE, 10.0, 90.0))
  }

  @Test
  fun tiltedLittle_keepsTheOrientation() {
    // Picked up by one edge, or held low to read: a small roll reads as a large turn.
    assertEquals(Orientation.PORTRAIT, next(Orientation.PORTRAIT, 30.0, 70.0))
    assertEquals(Orientation.PORTRAIT, next(Orientation.PORTRAIT, 30.0, 290.0))
  }

  @Test
  fun heldUpAndTurned_changes() {
    assertEquals(Orientation.LANDSCAPE, next(Orientation.PORTRAIT, 70.0, 85.0))
    assertEquals(Orientation.REVERSE_LANDSCAPE, next(Orientation.PORTRAIT, 70.0, 275.0))
    assertEquals(Orientation.PORTRAIT, next(Orientation.LANDSCAPE, 70.0, 5.0))
  }

  @Test
  fun nearTheEdge_keepsTheOrientation() {
    assertEquals(Orientation.PORTRAIT, next(Orientation.PORTRAIT, 80.0, 48.0))
    assertEquals(Orientation.LANDSCAPE, next(Orientation.PORTRAIT, 80.0, 52.0))
    assertEquals(Orientation.LANDSCAPE, next(Orientation.LANDSCAPE, 80.0, 42.0))
    assertEquals(Orientation.PORTRAIT, next(Orientation.PORTRAIT, 80.0, 312.0))
  }

  @Test
  fun landscapeNaturalDevice_coversEveryTurn() {
    assertEquals(Orientation.REVERSE_LANDSCAPE, OrientationMonitor.toOrientation(false, 0))
    assertEquals(Orientation.REVERSE_LANDSCAPE, OrientationMonitor.toOrientation(false, 20))
    assertEquals(Orientation.PORTRAIT, OrientationMonitor.toOrientation(false, 90))
    assertEquals(Orientation.UNKNOWN, OrientationMonitor.toOrientation(false, -1))
  }
}
