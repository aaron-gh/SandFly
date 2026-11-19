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

package com.google.android.accessibility.talkback.menurules

import android.content.Context
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.SheetsGrid
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Edit cell and Insert formula are offered on Google Sheets cells only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheetsEditMenuTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val menu = SheetsEditMenu()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  private fun node(packageName: String, description: String): AccessibilityNodeInfoCompat =
    @Suppress("DEPRECATION")
    AccessibilityNodeInfoCompat.obtain().apply {
      this.packageName = packageName
      contentDescription = description
    }

  @Test
  fun sheetsCell_offersEditCellAndInsertFormula() {
    val cell = node(SheetsGrid.PACKAGE, "B13; 162")

    assertTrue(menu.accept(context, cell))
    val items = menu.getMenuItemsForNode(context, cell, false)
    assertEquals(listOf(R.id.sheets_edit_cell, R.id.sheets_insert_formula), items.map { it.itemId })
    assertEquals(
      listOf(context.getString(R.string.title_sheets_edit_cell), context.getString(R.string.title_sheets_insert_formula)),
      items.map { it.title.toString() },
    )
  }

  @Test
  fun otherNodes_offerNothing() {
    assertFalse(menu.accept(context, node(SheetsGrid.PACKAGE, "Grid range: from B1 to D21")))
    assertFalse(menu.accept(context, node("com.example", "B13")))
  }
}
