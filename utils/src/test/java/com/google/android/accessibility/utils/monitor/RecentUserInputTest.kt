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

package com.google.android.accessibility.utils.monitor

import android.os.SystemClock
import java.time.Duration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentUserInputTest {

  @Test
  fun speechHandedBeforeTheInputIsBeforeIt() {
    val handed = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    RecentUserInput.mark()
    assertTrue(RecentUserInput.isRecentAfter(handed))
  }

  @Test
  fun speechHandedAfterTheInputIsNotBeforeIt() {
    RecentUserInput.mark()
    ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    assertFalse(RecentUserInput.isRecentAfter(SystemClock.uptimeMillis()))
  }

  @Test
  fun anOldInputCountsForNothing() {
    val handed = SystemClock.uptimeMillis()
    ShadowSystemClock.advanceBy(Duration.ofMillis(50))
    RecentUserInput.mark()
    ShadowSystemClock.advanceBy(Duration.ofMillis(5_000))
    assertFalse(RecentUserInput.isRecentAfter(handed))
  }
}
