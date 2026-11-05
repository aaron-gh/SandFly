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

package com.google.android.accessibility.utils

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The scroll position that sets the pitch of the scroll sound. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScrollPositionTest {
  private fun scrolled(
    scrollX: Int = 0,
    maxScrollX: Int = 0,
    scrollY: Int = 0,
    maxScrollY: Int = 0,
    itemCount: Int = -1,
    fromIndex: Int = -1,
  ) =
    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
      this.scrollX = scrollX
      this.maxScrollX = maxScrollX
      this.scrollY = scrollY
      this.maxScrollY = maxScrollY
      this.itemCount = itemCount
      this.fromIndex = fromIndex
    }

  @Test
  fun aViewThatScrollsOnlySidewaysHasItsSidewaysPosition() {
    // A strip of category tabs in a web page, as Chrome reports it.
    val event = scrolled(scrollX = 890, maxScrollX = 10167, itemCount = 21)

    assertEquals(890 / 10167f, AccessibilityEventUtils.getScrollPosition(event, 0.5f), 0.0001f)
  }

  @Test
  fun aViewThatScrollsDownHasItsVerticalPosition() {
    val event = scrolled(scrollX = 300, maxScrollX = 1000, scrollY = 6912, maxScrollY = 82965)

    assertEquals(6912 / 82965f, AccessibilityEventUtils.getScrollPosition(event, 0.5f), 0.0001f)
  }

  @Test
  fun aListThatGivesItsItemsHasThePositionOfTheFirstItemShown() {
    val event = scrolled(scrollX = 300, maxScrollX = 1000, itemCount = 20, fromIndex = 5)

    assertEquals(0.25f, AccessibilityEventUtils.getScrollPosition(event, 0.5f), 0.0001f)
  }

  @Test
  fun aViewWithoutAPositionHasTheDefault() {
    assertEquals(0.5f, AccessibilityEventUtils.getScrollPosition(scrolled(), 0.5f), 0.0001f)
  }
}
