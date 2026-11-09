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
  fun listMovesUntilQuietPeriodAfterLastActivity() {
    assertTrue(FocusProcessorForLogicalNavigation.listMayStillMove(1199, 1000, 0))
    assertTrue(FocusProcessorForLogicalNavigation.listMayStillMove(1199, 0, 1000))
    assertFalse(FocusProcessorForLogicalNavigation.listMayStillMove(1200, 1000, 500))
    assertFalse(FocusProcessorForLogicalNavigation.listMayStillMove(1200, 0, 0))
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
