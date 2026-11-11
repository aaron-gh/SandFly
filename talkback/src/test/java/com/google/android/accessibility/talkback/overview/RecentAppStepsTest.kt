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

package com.google.android.accessibility.talkback.overview

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentAppStepsTest {
  private fun target(current: Int, steps: Int, newest: Int = 0, size: Int = 4) =
    RecentAppSteps.target(current, steps, newest, size)

  @Test
  fun olderStepsGoBackOneTaskEach() {
    assertEquals(1, target(current = 0, steps = 1))
    assertEquals(2, target(current = 1, steps = 1))
    assertEquals(3, target(current = 0, steps = 3))
  }

  @Test
  fun olderStopsAtTheOldestTask() {
    assertEquals(3, target(current = 3, steps = 1))
    assertEquals(3, target(current = 1, steps = 5))
  }

  @Test
  fun fromTheHomeScreenOlderOpensTheMostRecentTask() {
    assertEquals(0, target(current = -1, steps = 1))
  }

  @Test
  fun newerGoesBackNoFurtherThanTheStart() {
    assertEquals(1, target(current = 2, steps = -1))
    assertEquals(0, target(current = 1, steps = -1))
    assertEquals(0, target(current = 0, steps = -1))
    assertEquals(0, target(current = 3, steps = -5))
  }

  @Test
  fun newerFromTheHomeScreenHasNoTask() {
    assertEquals(-1, target(current = -1, steps = -1))
  }

  @Test
  fun inSplitScreenNewerStopsAtTheSplit() {
    // Tasks 0 and 1 were on screen together when the steps started.
    assertEquals(1, target(current = 2, steps = -1, newest = 1))
    assertEquals(1, target(current = 1, steps = -1, newest = 1))
  }

  @Test
  fun stepsThatCancelOutStay() {
    assertEquals(2, target(current = 2, steps = 0))
  }

  @Test
  fun aSingleTaskHasNoOlderOne() {
    assertEquals(0, target(current = 0, steps = 1, size = 1))
  }
}
