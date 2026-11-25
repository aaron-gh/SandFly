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

package com.google.android.accessibility.talkback.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReportNotesTest {
  private val notes = ReportNotes(maxEntries = 5)

  @Test
  fun empty_readsNull() {
    assertNull(notes.read())
  }

  @Test
  fun keepsTheLastNotes_oldestFirst() {
    for (i in 1..8) notes.add("note $i")
    val lines = notes.read()!!.lines()
    assertEquals(5, lines.size)
    assertTrue(lines.first().endsWith(" note 4"))
    assertTrue(lines.last().endsWith(" note 8"))
  }

  @Test
  fun repeatedNote_fillsOneLine() {
    notes.add("first")
    notes.add("again")
    notes.add("again")
    notes.add("again")
    notes.add("last")
    val lines = notes.read()!!.lines()
    assertEquals(3, lines.size)
    assertTrue(lines[1].endsWith(" again (3 times)"))
    assertTrue(lines[2].endsWith(" last"))
  }

  @Test
  fun clear_removesAll() {
    notes.add("note")
    notes.clear()
    assertNull(notes.read())
  }
}
