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
  fun forwardUsesRowAfterSpan() {
    assertEquals(3, FocusProcessorForLogicalNavigation.adjacentRow(2, 1, 10, true))
    assertEquals(5, FocusProcessorForLogicalNavigation.adjacentRow(2, 3, 10, true))
    assertEquals(3, FocusProcessorForLogicalNavigation.adjacentRow(2, 0, 10, true))
  }

  @Test
  fun backwardUsesRowBefore() {
    assertEquals(1, FocusProcessorForLogicalNavigation.adjacentRow(2, 3, 10, false))
  }

  @Test
  fun noRowPastEitherEnd() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.adjacentRow(9, 1, 10, true))
    assertEquals(-1, FocusProcessorForLogicalNavigation.adjacentRow(8, 3, 10, true))
    assertEquals(-1, FocusProcessorForLogicalNavigation.adjacentRow(0, 1, 10, false))
  }

  @Test
  fun noRowForIndexOutOfBounds() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.adjacentRow(-1, 1, 10, true))
    assertEquals(-1, FocusProcessorForLogicalNavigation.adjacentRow(12, 1, 10, false))
  }

  @Test
  fun itemMustReachEdgeInDirection() {
    val list = Rect(0, 200, 1000, 2000)
    assertTrue(
      FocusProcessorForLogicalNavigation.reachesListEdge(Rect(0, 900, 1000, 2000), list, true)
    )
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListEdge(Rect(0, 900, 1000, 1990), list, true)
    )
    assertTrue(
      FocusProcessorForLogicalNavigation.reachesListEdge(Rect(0, 200, 1000, 900), list, false)
    )
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListEdge(Rect(0, 210, 1000, 900), list, false)
    )
  }

  @Test
  fun emptyBoundsDoNotReachEdge() {
    val list = Rect(0, 200, 1000, 2000)
    assertFalse(FocusProcessorForLogicalNavigation.reachesListEdge(Rect(), list, true))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListEdge(Rect(0, 900, 1000, 2000), Rect(), true)
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
