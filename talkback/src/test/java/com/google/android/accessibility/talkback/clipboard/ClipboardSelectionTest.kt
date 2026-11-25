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

package com.google.android.accessibility.talkback.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the texts checked in the clipboard history's Select page. */
class ClipboardSelectionTest {

  @Test
  fun toggleChecksThenUnchecks() {
    val selection = ClipboardSelection()
    assertTrue(selection.toggle("a"))
    assertTrue(selection.isChecked("a"))
    assertEquals(1, selection.count)
    assertFalse(selection.toggle("a"))
    assertFalse(selection.isChecked("a"))
    assertEquals(0, selection.count)
  }

  @Test
  fun checkedTextsComeInTheHistorysOrder() {
    val selection = ClipboardSelection()
    selection.toggle("c")
    selection.toggle("a")
    assertEquals(listOf("a", "c"), selection.checkedIn(listOf("a", "b", "c")))
  }

  @Test
  fun textsRemovedFromTheHistoryAreUnchecked() {
    val selection = ClipboardSelection()
    selection.toggle("a")
    selection.toggle("b")
    selection.retain(listOf("b", "c"))
    assertFalse(selection.isChecked("a"))
    assertTrue(selection.isChecked("b"))
    assertEquals(1, selection.count)
  }

  @Test
  fun clearUnchecksAll() {
    val selection = ClipboardSelection()
    selection.toggle("a")
    selection.clear()
    assertEquals(0, selection.count)
    assertEquals(emptyList<String>(), selection.checkedIn(listOf("a")))
  }
}
