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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.BACK
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.FORWARD
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap.NONE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColumnWrapTest {
  private val ltr = false

  private fun node(parent: AccessibilityNodeInfo?, bounds: Rect? = null): AccessibilityNodeInfo {
    val node = AccessibilityNodeInfo.obtain()
    parent?.let { shadowOf(it).addChild(node) }
    bounds?.let { node.setBoundsInScreen(it) }
    return node
  }

  /** A [rows] by [columns] table, each cell with collection item info. */
  private fun table(rows: Int, columns: Int): List<List<AccessibilityNodeInfoCompat>> {
    val root = node(null)
    AccessibilityNodeInfoCompat.wrap(root)
      .setCollectionInfo(CollectionInfoCompat.obtain(rows, columns, /* hierarchical= */ false))
    return (0 until rows).map { row ->
      (0 until columns).map { column ->
        AccessibilityNodeInfoCompat.wrap(node(root)).apply {
          setCollectionItemInfo(CollectionItemInfoCompat.obtain(row, 1, column, 1, false))
        }
      }
    }
  }

  /** A [rows] by [columns] grid of 100 pixel icons in one container, without collection info. */
  private fun iconGrid(rows: Int, columns: Int): List<List<AccessibilityNodeInfoCompat>> {
    val container = node(null)
    return (0 until rows).map { row ->
      (0 until columns).map { column ->
        val left = column * 150
        val top = row * 200
        AccessibilityNodeInfoCompat.wrap(node(container, Rect(left, top, left + 100, top + 100)))
      }
    }
  }

  @Test
  fun tableForwardFromTheEndOfARowWraps() {
    val cells = table(3, 3)
    assertEquals(FORWARD, ColumnWrap.detect(cells[0][2], cells[1][0], true, ltr))
  }

  @Test
  fun tableBackFromTheStartOfARowWrapsBack() {
    val cells = table(3, 3)
    assertEquals(BACK, ColumnWrap.detect(cells[1][0], cells[0][2], false, ltr))
  }

  @Test
  fun tableWithinARowDoesNotWrap() {
    val cells = table(3, 3)
    assertEquals(NONE, ColumnWrap.detect(cells[1][0], cells[1][1], true, ltr))
    assertEquals(NONE, ColumnWrap.detect(cells[1][1], cells[1][0], false, ltr))
  }

  @Test
  fun listDoesNotWrap() {
    val items = table(5, 1)
    assertEquals(NONE, ColumnWrap.detect(items[0][0], items[1][0], true, ltr))
  }

  @Test
  fun tablesNextToEachOtherDoNotWrap() {
    val first = table(2, 2)
    val second = table(2, 2)
    assertEquals(NONE, ColumnWrap.detect(first[0][1], second[1][0], true, ltr))
  }

  @Test
  fun cellOfTextInsideACellIsFound() {
    val cells = table(2, 2)
    val text = AccessibilityNodeInfoCompat.wrap(node(cells[1][0].unwrap()))
    assertEquals(FORWARD, ColumnWrap.detect(cells[0][1], text, true, ltr))
  }

  @Test
  fun iconGridForwardFromTheEndOfARowWraps() {
    val icons = iconGrid(2, 4)
    assertEquals(FORWARD, ColumnWrap.detect(icons[0][3], icons[1][0], true, ltr))
    assertEquals(NONE, ColumnWrap.detect(icons[0][2], icons[0][3], true, ltr))
  }

  @Test
  fun iconGridBackFromTheStartOfARowWrapsBack() {
    val icons = iconGrid(2, 4)
    assertEquals(BACK, ColumnWrap.detect(icons[1][0], icons[0][3], false, ltr))
    assertEquals(NONE, ColumnWrap.detect(icons[1][1], icons[1][0], false, ltr))
  }

  @Test
  fun iconGridRightToLeftWrapsTowardsTheRight() {
    val icons = iconGrid(2, 4)
    // In a right-to-left layout rows start at the right.
    assertEquals(FORWARD, ColumnWrap.detect(icons[0][0], icons[1][3], true, rtl = true))
    assertEquals(NONE, ColumnWrap.detect(icons[0][3], icons[1][0], true, rtl = true))
  }

  @Test
  fun keypadRowsInOneContainerWrap() {
    val keypad = node(null)
    val rows =
      (0 until 2).map { row ->
        val rowNode = node(keypad)
        (0 until 3).map { column ->
          val left = column * 100
          val top = row * 100
          AccessibilityNodeInfoCompat.wrap(node(rowNode, Rect(left, top, left + 90, top + 90)))
        }
      }
    assertEquals(FORWARD, ColumnWrap.detect(rows[0][2], rows[1][0], true, ltr))
  }

  @Test
  fun itemsOfDifferentSizesDoNotWrap() {
    val container = node(null)
    val button = AccessibilityNodeInfoCompat.wrap(node(container, Rect(500, 0, 600, 100)))
    val wide = AccessibilityNodeInfoCompat.wrap(node(container, Rect(0, 200, 600, 300)))
    assertEquals(NONE, ColumnWrap.detect(button, wide, true, ltr))
  }

  @Test
  fun itemsInUnrelatedContainersDoNotWrap() {
    val a = AccessibilityNodeInfoCompat.wrap(node(node(node(null)), Rect(500, 0, 600, 100)))
    val b = AccessibilityNodeInfoCompat.wrap(node(node(node(null)), Rect(0, 200, 100, 300)))
    assertEquals(NONE, ColumnWrap.detect(a, b, true, ltr))
  }
}
