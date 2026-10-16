/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.compositor

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.monitor.CollectionState
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** What a table cell says about its row and column, for each of the table reading settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TableItemTransitionDescriptionTest {
  private lateinit var context: Context
  private val state = CollectionState()

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  /** A 3 by 3 table. With [headers], the first row holds them, as a web table's does. */
  private fun table(headers: List<CharSequence>?): List<List<AccessibilityNodeInfoCompat>> {
    val root = AccessibilityNodeInfo.obtain()
    AccessibilityNodeInfoCompat.wrap(root)
        .setCollectionInfo(CollectionInfoCompat.obtain(3, 3, /* hierarchical= */ false))
    val text =
        listOf(
            headers ?: listOf("Apple", "Red", "3"),
            listOf("Banana", "Yellow", "5"),
            listOf("Cherry", "Dark red", "20"),
        )
    return text.mapIndexed { row, cells ->
      cells.mapIndexed { column, cellText ->
        val isHeader = headers != null && row == 0
        val cell = AccessibilityNodeInfo.obtain()
        shadowOf(root).addChild(cell)
        AccessibilityNodeInfoCompat.wrap(cell).apply {
          setText(cellText)
          setHeading(isHeader)
          setCollectionItemInfo(CollectionItemInfoCompat.obtain(row, 1, column, 1, isHeader))
        }
      }
    }
  }

  private fun describe(
      cell: AccessibilityNodeInfoCompat,
      columnHeaders: String = GlobalVariables.TABLE_HEADERS_AFTER,
      numbers: Boolean = true,
  ): CharSequence {
    state.updateCollectionInformation(
        cell, AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED))
    return CollectionStateFeedbackUtils.getCollectionItemTransitionDescription(
        cell, state, context, columnHeaders, numbers)
  }

  private fun row(number: Int) = context.getString(R.string.row_index_template, number)

  private fun column(number: Int) = context.getString(R.string.column_index_template, number)

  @Test
  fun headers_areSpokenForTheColumnThatChanged() {
    val cells = table(headers = listOf("Name", "Colour", "Count"))
    assertEquals("${row(2)}, Name", describe(cells[1][0]).toString())
    assertEquals("Colour", describe(cells[1][1]).toString())
    assertEquals(row(3), describe(cells[2][1]).toString())
  }

  @Test
  fun headerCell_saysItIsAColumnHeading() {
    val cells = table(headers = listOf("Name", "Colour", "Count"))
    assertEquals(
        "${context.getString(R.string.column_heading_template)}, ${row(1)}",
        describe(cells[0][0]).toString())
  }

  @Test
  fun tableWithoutHeaders_saysNumbersAndNeverTheFirstRow() {
    val cells = table(headers = null)
    assertEquals("${row(1)}, ${column(1)}", describe(cells[0][0]).toString())
    assertEquals(row(2), describe(cells[1][0]).toString())
    assertEquals(column(2), describe(cells[1][1]).toString())
  }

  @Test
  fun headersOff_saysColumnNumbersInstead() {
    val cells = table(headers = listOf("Name", "Colour", "Count"))
    val off = GlobalVariables.TABLE_HEADERS_OFF
    assertEquals("${row(2)}, ${column(1)}", describe(cells[1][0], off).toString())
    assertEquals(column(2), describe(cells[1][1], off).toString())
  }

  @Test
  fun numbersOff_keepsHeadersAndDropsNumbers() {
    val cells = table(headers = listOf("Name", "Colour", "Count"))
    assertEquals("Name", describe(cells[1][0], numbers = false).toString())
    assertEquals("Colour", describe(cells[1][1], numbers = false).toString())
    assertEquals("", describe(cells[2][1], numbers = false).toString())
  }

  @Test
  fun headersOffAndNumbersOff_saysNothingForADataCell() {
    val cells = table(headers = listOf("Name", "Colour", "Count"))
    val off = GlobalVariables.TABLE_HEADERS_OFF
    assertEquals("", describe(cells[1][0], off, numbers = false).toString())
    assertEquals("", describe(cells[1][1], off, numbers = false).toString())
  }

  @Test
  fun numbersOff_inATableWithoutHeaders_saysNothing() {
    val cells = table(headers = null)
    assertEquals("", describe(cells[1][0], numbers = false).toString())
    assertEquals("", describe(cells[1][1], numbers = false).toString())
  }

  @Test
  fun header_keepsItsLanguage() {
    val french = SpannableString("Couleur")
    french.setSpan(LocaleSpan(Locale.FRENCH), 0, french.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val cells = table(headers = listOf("Name", french, "Count"))
    describe(cells[1][0])
    val spoken = describe(cells[1][1])

    assertEquals("Couleur", spoken.toString())
    val spans = (spoken as Spanned).getSpans(0, spoken.length, LocaleSpan::class.java)
    assertEquals(listOf(Locale.FRENCH), spans.map { it.locale })
  }
}
