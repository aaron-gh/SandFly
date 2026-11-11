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

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Checks when a swipe jumps straight to the next row of a list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JumpToNextRowTest {

  @Test
  fun usesRowAfterSpan() {
    assertEquals(3, FocusProcessorForLogicalNavigation.rowAfter(2, 1, 10))
    assertEquals(5, FocusProcessorForLogicalNavigation.rowAfter(2, 3, 10))
    assertEquals(3, FocusProcessorForLogicalNavigation.rowAfter(2, 0, 10))
  }

  @Test
  fun noRowPastTheEnd() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(9, 1, 10))
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(8, 3, 10))
  }

  @Test
  fun noRowForIndexOutOfBounds() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(-1, 1, 10))
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(12, 1, 10))
  }

  @Test
  fun itemMustReachListBottom() {
    val list = Rect(0, 200, 1000, 2000)
    assertTrue(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), list))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 1990), list)
    )
  }

  @Test
  fun emptyBoundsDoNotReachListBottom() {
    val list = Rect(0, 200, 1000, 2000)
    assertFalse(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(), list))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), Rect())
    )
  }

  @Test
  fun plainItemHasNoPartsTakingFocus() {
    val item = node()
    node(item)
    node(item)
    assertFalse(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun clickableDescendantTakesFocus() {
    val item = node()
    val layout = node(item)
    node(layout).unwrap().isClickable = true
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun adjustableSliderTakesFocus() {
    val item = node()
    node(item).unwrap().className = "android.widget.SeekBar"
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun pivotInsideItemDoesNotCount() {
    val item = node()
    val pivot = node(item)
    pivot.unwrap().isFocusable = true
    assertFalse(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, pivot))
  }

  @Test
  fun tooManyNodesCountAsPartsTakingFocus() {
    val item = node()
    repeat(101) { node(item) }
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun listWithoutScrollEventsWaitsBriefly() {
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1179, 1000, 0, 0, 0, 0))
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1180, 1000, 0, 0, 0, 0))
  }

  @Test
  fun listMovingAtSpeedKeepsWaiting() {
    // 20 px just after the scroll, then a full step: speeding up.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1110, 1000, 2, 1100, 600, 20))
    // A step of at least half the one before.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1210, 1000, 3, 1200, 300, 600))
    // One scroll event: nothing to compare yet.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1110, 1000, 1, 1100, 5, 900))
  }

  @Test
  fun listSlowingDownCountsAsStopped() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1210, 1000, 3, 1200, 250, 600))
  }

  @Test
  fun listWithoutRecentScrollEventCountsAsStopped() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1250, 1000, 2, 1100, 600, 600))
  }

  @Test
  fun waitEndsAfterMaximum() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1270, 1000, 3, 1260, 600, 600))
  }

  private fun node(parent: AccessibilityNodeInfoCompat? = null): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    // Distinct sources keep the nodes unequal.
    raw.setSource(View(RuntimeEnvironment.getApplication()))
    raw.className = "android.view.View"
    raw.isVisibleToUser = true
    raw.setBoundsInScreen(Rect(0, 0, 100, 100))
    val node = AccessibilityNodeInfoCompat.wrap(raw)
    if (parent != null) {
      shadowOf(parent.unwrap()).addChild(raw)
    }
    return node
  }
}
