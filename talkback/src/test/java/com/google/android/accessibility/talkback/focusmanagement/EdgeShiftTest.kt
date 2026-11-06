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

package com.google.android.accessibility.talkback.focusmanagement

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the scroll delta that the early scroll check measures from the focused item's bounds,
 * which are cut to the list's bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdgeShiftTest {

  // A list from y=200 to y=2240.
  private val listTop = 200
  private val listBottom = 2240

  @Test
  fun itemWithBothEdgesOnScreenUsesTop() {
    // Scrolling back moves the item down by 300.
    assertEquals(-300, FocusProcessorForLogicalNavigation.edgeShift(500, 800, 900, 1200))
    // Scrolling forward moves it up by 300.
    assertEquals(300, FocusProcessorForLogicalNavigation.edgeShift(800, 500, 1200, 900))
  }

  @Test
  fun tallItemCutOffAtTopMovingDownUsesBottom() {
    // A post taller than the list, its top above the list and its bottom 4 px above the list's
    // bottom. Scrolling back moves its bottom past the list's bottom while its top stays cut off.
    assertEquals(
      -4,
      FocusProcessorForLogicalNavigation.edgeShift(listTop, listTop, listBottom - 4, listBottom),
    )
  }

  @Test
  fun tallItemCutOffAtTopMovingUpUsesBottom() {
    // Scrolling forward brings the bottom of the same post up the list.
    assertEquals(
      600,
      FocusProcessorForLogicalNavigation.edgeShift(listTop, listTop, listBottom, listBottom - 600),
    )
  }

  @Test
  fun tallItemCutOffAtBottomMovingDownUsesTop() {
    // A post whose top is on screen and whose bottom is below the list.
    assertEquals(
      -700,
      FocusProcessorForLogicalNavigation.edgeShift(listTop, listTop + 700, listBottom, listBottom),
    )
  }

  @Test
  fun unchangedBoundsGiveNoShift() {
    assertEquals(
      0,
      FocusProcessorForLogicalNavigation.edgeShift(listTop, listTop, listBottom, listBottom),
    )
  }
}
