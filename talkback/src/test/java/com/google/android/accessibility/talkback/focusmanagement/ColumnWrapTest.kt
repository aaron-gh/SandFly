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
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.BACK
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.FORWARD
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColumnWrapTest {
  private val ltr = false

  /** A visible item that can take focus, at [bounds], in [parent]. */
  private fun item(parent: AccessibilityNodeInfo?, bounds: Rect): AccessibilityNodeInfoCompat {
    val node = container(parent)
    node.setBoundsInScreen(bounds)
    node.isClickable = true
    node.isFocusable = true
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  private fun container(parent: AccessibilityNodeInfo?): AccessibilityNodeInfo {
    val node = AccessibilityNodeInfo.obtain()
    node.isVisibleToUser = true
    parent?.let { shadowOf(it).addChild(node) }
    return node
  }

  /** A [rows] by [columns] grid of 100 pixel icons in one container. */
  private fun iconGrid(rows: Int, columns: Int): List<List<AccessibilityNodeInfoCompat>> {
    val grid = container(null)
    return (0 until rows).map { row ->
      (0 until columns).map { column ->
        val left = column * 150
        val top = row * 200
        item(grid, Rect(left, top, left + 100, top + 100))
      }
    }
  }

  /** A list whose rows each have a title and a button beside it, such as videos or settings. */
  private fun rowsWithButtons(
    rows: Int
  ): List<Pair<AccessibilityNodeInfoCompat, AccessibilityNodeInfoCompat>> {
    val list = container(null)
    return (0 until rows).map { row ->
      val rowNode = container(list)
      val top = row * 200
      val title = item(rowNode, Rect(0, top, 800, top + 180))
      title to item(rowNode, Rect(850, top + 50, 1000, top + 130))
    }
  }

  @Test
  fun gridForwardFromTheEndOfARowPlaysTheSound() {
    val icons = iconGrid(2, 4)
    assertEquals(FORWARD, ColumnWrap.detect(icons[0][3], icons[1][0], true, ltr))
    assertEquals(NONE, ColumnWrap.detect(icons[0][2], icons[0][3], true, ltr))
  }

  @Test
  fun gridBackFromTheStartOfARowPlaysTheBackSound() {
    val icons = iconGrid(2, 4)
    assertEquals(BACK, ColumnWrap.detect(icons[1][0], icons[0][3], false, ltr))
    assertEquals(NONE, ColumnWrap.detect(icons[1][1], icons[1][0], false, ltr))
  }

  @Test
  fun leavingARowWithAButtonBesidePlaysTheSound() {
    val rows = rowsWithButtons(3)
    // From the button at the end of a row to the title of the next.
    assertEquals(FORWARD, ColumnWrap.detect(rows[0].second, rows[1].first, true, ltr))
    // From the title at the start of a row back to the button of the row before.
    assertEquals(BACK, ColumnWrap.detect(rows[1].first, rows[0].second, false, ltr))
    // Along the row.
    assertEquals(NONE, ColumnWrap.detect(rows[0].first, rows[0].second, true, ltr))
  }

  @Test
  fun eachItemOfAListIsARow() {
    val list = container(null)
    val items = (0 until 3).map { item(list, Rect(0, it * 200, 1000, it * 200 + 180)) }
    assertEquals(FORWARD, ColumnWrap.detect(items[0], items[1], true, ltr))
    assertEquals(BACK, ColumnWrap.detect(items[1], items[0], false, ltr))
  }

  @Test
  fun leavingAHeadingIsARowChange() {
    val screen = container(null)
    val heading = item(screen, Rect(0, -150, 1000, -50))
    val rows = rowsWithButtons(1)
    assertEquals(FORWARD, ColumnWrap.detect(heading, rows[0].first, true, ltr))
  }

  @Test
  fun rowsArePlaces() {
    val item = Rect(0, 100, 800, 300)
    assertTrue(ColumnWrap.leavesRow(item, Rect(0, 320, 800, 500), forward = true))
    assertFalse(ColumnWrap.leavesRow(item, Rect(850, 150, 1000, 250), forward = true))
  }
}
