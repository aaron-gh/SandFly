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

  @Test
  fun appSpeechNote_namesPackageClassWindowAndSourceApp() {
    assertEquals(
      "Announcement from com.oplus.viewtalk, class android.view.View, window 12, " +
        "item in com.google.android.youtube, spoken: \"Image may contain\"",
      ReportNotes.appSpeechNote(
        "Announcement",
        "com.oplus.viewtalk",
        "android.view.View",
        12,
        "com.google.android.youtube",
        leftOut = false,
        "Image may contain",
      ),
    )
  }

  @Test
  fun appSpeechNote_leavesOutUnknownsAndSamePackage() {
    assertEquals(
      "Window name from no package, left out: \"YouTube\"",
      ReportNotes.appSpeechNote("Window name", null, null, -1, null, leftOut = true, "YouTube"),
    )
    assertEquals(
      "Live region from a.b, spoken: \"x\"",
      ReportNotes.appSpeechNote("Live region", "a.b", null, -1, "a.b", leftOut = false, "x"),
    )
  }

  @Test
  fun appSpeechNote_cutsLongTextToOneLine() {
    val text = "line one\n" + "x".repeat(100)
    val note =
      ReportNotes.appSpeechNote("Announcement", "a.b", null, -1, null, leftOut = false, text)
    val quoted = note.substringAfter('"').substringBeforeLast('"')
    assertEquals(ReportNotes.MAX_APP_SPEECH_TEXT + 1, quoted.length)
    assertTrue(quoted.startsWith("line one x"))
    assertTrue(quoted.endsWith("…"))
  }
}
