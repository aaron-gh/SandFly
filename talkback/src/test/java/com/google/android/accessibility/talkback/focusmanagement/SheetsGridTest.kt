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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks how Google Sheets cell labels are read as table positions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheetsGridTest {

  private fun node(description: String, pkg: String = SheetsGrid.PACKAGE): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfo()
    node.packageName = pkg
    node.contentDescription = description
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  @Test
  fun emptyCell() {
    val cell = SheetsGrid.parse("B13")!!
    assertEquals(12, cell.row)
    assertEquals(1, cell.column)
  }

  @Test
  fun cellWithContent() {
    val cell = SheetsGrid.parse("A2; 162 Has formula Right aligned Black font")!!
    assertEquals(1, cell.row)
    assertEquals(0, cell.column)
  }

  @Test
  fun multiLetterColumn() {
    val cell = SheetsGrid.parse("AB7")!!
    assertEquals(6, cell.row)
    assertEquals(27, cell.column)
  }

  @Test
  fun notCells() {
    assertNull(SheetsGrid.parse("Zakres siatki: od A1 do C23."))
    assertNull(SheetsGrid.parse("Column C"))
    assertNull(SheetsGrid.parse("A0"))
    assertNull(SheetsGrid.parse("A12b"))
  }

  @Test
  fun onlyInSheets() {
    assertTrue(SheetsGrid.isCell(node("C5")))
    assertFalse(SheetsGrid.isCell(node("C5", pkg = "com.example")))
  }
}
