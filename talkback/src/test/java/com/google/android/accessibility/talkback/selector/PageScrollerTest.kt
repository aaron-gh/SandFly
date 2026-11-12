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

package com.google.android.accessibility.talkback.selector

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PageScrollerTest {
  private val list = Rect(0, 200, 1000, 1200)

  @Test
  fun scrollingForwardSwipesUpThroughTheMiddleOfTheList() {
    val line = PageScroller.swipeLine(list, forward = true)
    assertEquals(500f, line[0], 0.01f)
    assertEquals(900f, line[1], 0.01f)
    assertEquals(500f, line[2], 0.01f)
  }

  @Test
  fun scrollingBackSwipesDown() {
    val line = PageScroller.swipeLine(list, forward = false)
    assertEquals(500f, line[0], 0.01f)
    assertEquals(500f, line[1], 0.01f)
    assertEquals(900f, line[2], 0.01f)
  }

  @Test
  fun theSwipeStaysInsideTheList() {
    for (forward in listOf(true, false)) {
      val line = PageScroller.swipeLine(list, forward)
      assertTrue(list.contains(line[0].toInt(), line[1].toInt()))
      assertTrue(list.contains(line[0].toInt(), line[2].toInt()))
    }
  }

  @Test
  fun aListThatRejectedTheActionGetsASwipe() {
    assertTrue(PageScroller.swipesAfterFailedAction(listFound = true, listTakesAction = true))
  }

  @Test
  fun noListGetsNoSwipe() {
    assertFalse(PageScroller.swipesAfterFailedAction(listFound = false, listTakesAction = false))
    assertFalse(PageScroller.swipesAfterFailedAction(listFound = false, listTakesAction = true))
  }

  @Test
  fun theSwipeAreaIsThePartOfTheListOnTheScreen() {
    val screen = Rect(0, 0, 1000, 1000)
    assertEquals(Rect(0, 200, 1000, 1000), PageScroller.swipeArea(list, screen))
  }

  @Test
  fun aListOffTheScreenOrWithoutSizeGetsNoSwipe() {
    val screen = Rect(0, 0, 1000, 1000)
    assertNull(PageScroller.swipeArea(Rect(0, 1100, 1000, 1500), screen))
    assertNull(PageScroller.swipeArea(Rect(), screen))
  }

  @Test
  fun aListAtItsEdgeGetsNoSwipe() {
    assertFalse(PageScroller.swipesAfterFailedAction(listFound = true, listTakesAction = false))
  }
}
