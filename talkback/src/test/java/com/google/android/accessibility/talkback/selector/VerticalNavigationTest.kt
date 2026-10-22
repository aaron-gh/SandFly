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

import android.graphics.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A row of a title and a button beside it, with the next row below. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerticalNavigationTest {
  private val title = Rect(0, 100, 800, 300)
  private val button = Rect(850, 150, 1000, 250)
  private val nextTitle = Rect(0, 320, 800, 520)

  @Test
  fun theButtonBesideIsOnTheSameRow() {
    assertFalse(VerticalNavigation.isOnOtherRow(button, title, forward = true))
    assertTrue(VerticalNavigation.isOnSameRow(button, title))
  }

  @Test
  fun theNextRowIsBelowAndThePreviousAbove() {
    assertTrue(VerticalNavigation.isOnOtherRow(nextTitle, title, forward = true))
    assertTrue(VerticalNavigation.isOnOtherRow(title, nextTitle, forward = false))
    assertFalse(VerticalNavigation.isOnSameRow(nextTitle, title))
  }
}
