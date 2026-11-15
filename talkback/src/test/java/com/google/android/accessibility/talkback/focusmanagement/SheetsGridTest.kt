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

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks how Google Sheets cell labels are read as table positions, and how moves step. */
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
  private fun cell(address: String) = SheetsGrid.parse(address)!!

  private fun focused(address: String): AccessibilityEvent {
    val event = AccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
    event.packageName = SheetsGrid.PACKAGE
    event.contentDescription = address
    return event
  }

  @After
  fun endSilence() {
    // Ends any silence a test left running.
    SheetsGrid.silence(null, -1)
  }

  @Test
  fun rowGoal() {
    val goal = SheetsGrid.goalFor(NavigationTarget.TARGET_ROW, cell("B5"), true)!!
    assertEquals(SheetsGrid.Goal.CONTINUE, goal.test(cell("C5")))
    assertEquals(SheetsGrid.Goal.CONTINUE, goal.test(cell("A6")))
    assertEquals(SheetsGrid.Goal.FOUND, goal.test(cell("B6")))
    assertEquals(SheetsGrid.Goal.STOP, goal.test(cell("C6")))
    assertEquals(SheetsGrid.Goal.STOP, goal.test(cell("A7")))
    val back = SheetsGrid.goalFor(NavigationTarget.TARGET_ROW, cell("B5"), false)!!
    assertEquals(SheetsGrid.Goal.CONTINUE, back.test(cell("A5")))
    assertEquals(SheetsGrid.Goal.CONTINUE, back.test(cell("C4")))
    assertEquals(SheetsGrid.Goal.FOUND, back.test(cell("B4")))
    assertEquals(SheetsGrid.Goal.STOP, back.test(cell("A4")))
    assertNull(SheetsGrid.goalFor(NavigationTarget.TARGET_ROW, cell("B1"), false))
  }

  @Test
  fun columnAndRowBoundsGoals() {
    val column = SheetsGrid.goalFor(NavigationTarget.TARGET_COLUMN, cell("B5"), true)!!
    assertEquals(SheetsGrid.Goal.FOUND, column.test(cell("C5")))
    assertEquals(SheetsGrid.Goal.STOP, column.test(cell("A6")))
    val rowEnd =
      SheetsGrid.goalFor(NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS, cell("B5"), true)!!
    assertEquals(SheetsGrid.Goal.CONTINUE, rowEnd.test(cell("Z5")))
    assertEquals(SheetsGrid.Goal.STOP, rowEnd.test(cell("A6")))
    assertNull(SheetsGrid.goalFor(NavigationTarget.TARGET_DEFAULT, cell("B5"), true))
  }

  @Test
  fun nextInReadingOrder() {
    val cells = listOf(cell("C5"), cell("A6"), cell("B5"), cell("A5"))
    assertEquals(0, SheetsGrid.nextIndex(cells, cell("B5"), true))
    assertEquals(3, SheetsGrid.nextIndex(cells, cell("B5"), false))
    assertEquals(1, SheetsGrid.nextIndex(cells, cell("C5"), true))
    assertEquals(-1, SheetsGrid.nextIndex(cells, cell("A6"), true))
    assertEquals(-1, SheetsGrid.nextIndex(cells, cell("A5"), false))
    assertTrue(SheetsGrid.lists(cells, cell("B5")))
    assertFalse(SheetsGrid.lists(cells, cell("B6")))
  }

  @Test
  fun beyondAndTarget() {
    val origin = cell("B5")
    assertTrue(SheetsGrid.beyond(cell("C5"), origin, null, true))
    assertFalse(SheetsGrid.beyond(cell("A5"), origin, null, true))
    assertTrue(SheetsGrid.isTarget(cell("C5"), origin, null, true))
    assertFalse(SheetsGrid.isTarget(cell("A6"), origin, null, true))
    val goal = cell("B6")
    assertFalse(SheetsGrid.beyond(cell("A6"), origin, goal, true))
    assertTrue(SheetsGrid.beyond(cell("B6"), origin, goal, true))
    assertTrue(SheetsGrid.isTarget(cell("B6"), origin, goal, true))
    assertFalse(SheetsGrid.isTarget(cell("C6"), origin, goal, true))
  }

  @Test
  fun silenceSpeaksOnlyTheTarget() {
    SheetsGrid.silence(null, 1500)
    assertTrue(SheetsGrid.shouldDropFocusEvent(focused("C5")))
    SheetsGrid.endSilence("B6")
    assertTrue(SheetsGrid.shouldDropFocusEvent(focused("C6")))
    assertFalse(SheetsGrid.shouldDropFocusEvent(focused("B6")))
    // The target ended the silence.
    assertFalse(SheetsGrid.shouldDropFocusEvent(focused("C6")))
  }

  @Test
  fun endSilenceWithoutSilenceDropsNothing() {
    SheetsGrid.endSilence(null)
    assertFalse(SheetsGrid.shouldDropFocusEvent(focused("C5")))
  }

  @Test
  fun touchWithoutMoveIsNotDropped() {
    val touch = AccessibilityEvent(AccessibilityEvent.TYPE_TOUCH_INTERACTION_START)
    assertFalse(SheetsGrid.shouldDropFocusEvent(touch))
    assertFalse(SheetsGrid.isMoving())
  }
}
