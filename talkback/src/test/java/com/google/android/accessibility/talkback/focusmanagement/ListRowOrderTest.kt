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
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Checks that the row after a swipe's starting row is found after the swipe's scroll (#387). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListRowOrderTest {

  private val focusable =
    object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat): Boolean = node.isFocusable
    }

  @Test
  fun rowOfAnItemAndOfAPartInsideIt() {
    val list = list(columns = 1)
    val item = row(list, 7, top = 0)
    val part = node("part", item)
    assertEquals(7, FocusProcessorForLogicalNavigation.rowOf(item)?.row())
    assertEquals(7, FocusProcessorForLogicalNavigation.rowOf(part)?.row())
    assertEquals(1, FocusProcessorForLogicalNavigation.rowOf(part)?.span())
  }

  @Test
  fun noRowOutsideAOneColumnList() {
    assertNull(FocusProcessorForLogicalNavigation.rowOf(node("loose", null)))
    val grid = list(columns = 3)
    assertNull(FocusProcessorForLogicalNavigation.rowOf(row(grid, 2, top = 0)))
  }

  @Test
  fun nextRowFollowsTheWayIndexesRun() {
    val from = FocusProcessorForLogicalNavigation.ListRow(list(columns = 1), 10, 2)
    assertEquals(12, FocusProcessorForLogicalNavigation.rowAfterMove(from, true, false))
    assertEquals(9, FocusProcessorForLogicalNavigation.rowAfterMove(from, false, false))
    // A chat laid out from the bottom: the next item down has the lower index.
    assertEquals(9, FocusProcessorForLogicalNavigation.rowAfterMove(from, true, true))
    assertEquals(12, FocusProcessorForLogicalNavigation.rowAfterMove(from, false, true))
  }

  @Test
  fun searchFromAReusedPivotLiesPastTheNextRow() {
    // Developer options after a page scroll from row 12: rows 13 to 21 on screen, and the
    // search from the pivot, whose view now shows row 19, found row 20.
    val list = list(columns = 1)
    val rows = (13..21).map { row(list, it, top = (it - 13) * 200) }
    assertTrue(FocusProcessorForLogicalNavigation.liesPastRow(row(rows[7]), list, 12, 13))
    assertFalse(FocusProcessorForLogicalNavigation.liesPastRow(row(rows[0]), list, 12, 13))
    // Going back from row 22: row 21 is next, and row 13 lies past it.
    assertTrue(FocusProcessorForLogicalNavigation.liesPastRow(row(rows[0]), list, 22, 21))
    assertFalse(FocusProcessorForLogicalNavigation.liesPastRow(row(rows[8]), list, 22, 21))
  }

  @Test
  fun nodeOfAnotherListDoesNotLiePastTheRow() {
    val list = list(columns = 1)
    row(list, 13, top = 0)
    val other = list(columns = 1)
    val elsewhere = row(other, 40, top = 0)
    assertFalse(FocusProcessorForLogicalNavigation.liesPastRow(row(elsewhere), list, 12, 13))
    assertFalse(FocusProcessorForLogicalNavigation.liesPastRow(row(node("loose", null)), list, 12, 13))
  }

  @Test
  fun itemAtRowIsTheVisibleChildWithThatRow() {
    val list = list(columns = 1)
    row(list, 13, top = 0)
    val fourteen = row(list, 14, top = 200)
    row(list, 15, top = 400).unwrap().isVisibleToUser = false
    assertEquals(fourteen.text, FocusProcessorForLogicalNavigation.itemAtRow(list, 14)?.text)
    assertNull(FocusProcessorForLogicalNavigation.itemAtRow(list, 15))
    assertNull(FocusProcessorForLogicalNavigation.itemAtRow(list, 30))
  }

  @Test
  fun focusInTheItemItselfOrItsFirstPart() {
    val list = list(columns = 1)
    val focusableRow = row(list, 13, top = 0)
    val layoutRow = row(list, 14, top = 200)
    layoutRow.unwrap().isFocusable = false
    val title = node("title", layoutRow)
    node("button", layoutRow)
    val strategy = OrderedTraversalStrategy(list)
    val forward = TraversalStrategy.SEARCH_FOCUS_FORWARD
    assertEquals(
      focusableRow.text,
      FocusProcessorForLogicalNavigation.focusIn(focusableRow, strategy, forward, focusable)?.text,
    )
    assertEquals(
      title.text,
      FocusProcessorForLogicalNavigation.focusIn(layoutRow, strategy, forward, focusable)?.text,
    )
  }

  @Test
  fun noFocusInAnItemWithNothingToFocus() {
    val list = list(columns = 1)
    val divider = row(list, 13, top = 0)
    divider.unwrap().isFocusable = false
    // The search would go on to the next row, outside the divider.
    row(list, 14, top = 200)
    val strategy = OrderedTraversalStrategy(list)
    assertNull(
      FocusProcessorForLogicalNavigation.focusIn(
        divider,
        strategy,
        TraversalStrategy.SEARCH_FOCUS_FORWARD,
        focusable,
      )
    )
  }

  private fun row(node: AccessibilityNodeInfoCompat) = FocusProcessorForLogicalNavigation.rowOf(node)

  private fun list(columns: Int): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    raw.className = "androidx.recyclerview.widget.RecyclerView"
    raw.isScrollable = true
    raw.isVisibleToUser = true
    raw.setBoundsInScreen(Rect(0, 0, 1080, 2000))
    raw.collectionInfo = AccessibilityNodeInfo.CollectionInfo(50, columns, false)
    return AccessibilityNodeInfoCompat.wrap(raw)
  }

  private fun row(list: AccessibilityNodeInfoCompat, index: Int, top: Int): AccessibilityNodeInfoCompat {
    val item = node("row $index", list)
    item.unwrap().setBoundsInScreen(Rect(0, top, 1080, top + 200))
    item.unwrap().collectionItemInfo =
      AccessibilityNodeInfo.CollectionItemInfo(index, 1, 0, 1, false)
    return item
  }

  private fun node(text: String, parent: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    raw.text = text
    raw.className = "android.widget.TextView"
    raw.isFocusable = true
    raw.isVisibleToUser = true
    raw.setBoundsInScreen(Rect(0, 0, 100, 100))
    val node = AccessibilityNodeInfoCompat.wrap(raw)
    if (parent != null) {
      shadowOf(parent.unwrap()).addChild(raw)
    }
    return node
  }
}
