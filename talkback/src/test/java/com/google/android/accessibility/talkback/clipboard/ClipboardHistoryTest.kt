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

package com.google.android.accessibility.talkback.clipboard

import android.content.ClipboardManager
import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.google.android.accessibility.utils.ClipboardHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONArray
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks what the clipboard history keeps, and what Add to clipboard puts on the clipboard. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipboardHistoryTest {

  private val context: Context = RuntimeEnvironment.getApplication()

  @Before
  fun clear() {
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 0)
    ClipboardHistory.clear(context)
  }

  private fun historyFile() = context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)

  /** Sets when each entry, newest first, was copied, as stored in the history's file. */
  private fun setTimes(vararg times: Long) {
    historyFile().edit().putString("times", JSONArray(times.toList()).toString()).commit()
  }

  private fun clip(): String? =
    context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()

  @Test
  fun copiesAreKeptNewestFirstAndOnce() {
    ClipboardHistory.copy(context, "one")
    ClipboardHistory.copy(context, "two")
    ClipboardHistory.copy(context, "one")
    assertEquals(listOf("one", "two"), ClipboardHistory.entries(context))
    assertEquals("one", clip())
  }

  @Test
  fun historyKeepsOnlyTheNewestEntries() {
    repeat(ClipboardHistory.DEFAULT_MAX_ENTRIES + 5) { ClipboardHistory.record(context, "text $it") }
    val entries = ClipboardHistory.entries(context)
    assertEquals(ClipboardHistory.DEFAULT_MAX_ENTRIES, entries.size)
    assertEquals("text ${ClipboardHistory.DEFAULT_MAX_ENTRIES + 4}", entries.first())
  }

  @Test
  fun historyKeepsUpToTheSetNumberOfEntries() {
    ClipboardHistory.setLimits(context, ClipboardHistory.MAX_ENTRIES + 100, 0)
    assertEquals(ClipboardHistory.MAX_ENTRIES, ClipboardHistory.maxEntries(context))
    repeat(ClipboardHistory.MAX_ENTRIES + 5) { ClipboardHistory.record(context, "$it") }
    assertEquals(ClipboardHistory.MAX_ENTRIES, ClipboardHistory.entries(context).size)
    ClipboardHistory.setLimits(context, 3, 0)
    val last = ClipboardHistory.MAX_ENTRIES + 4
    assertEquals(listOf("$last", "${last - 1}", "${last - 2}"), ClipboardHistory.entries(context))
    ClipboardHistory.setLimits(context, 0, 0)
    assertEquals(1, ClipboardHistory.maxEntries(context))
  }

  @Test
  fun entriesOlderThanTheSetDaysAreDeletedUnderTheLimit() {
    ClipboardHistory.record(context, "old")
    ClipboardHistory.record(context, "new")
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    setTimes(now, now - 3 * day)
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 0)
    assertEquals(listOf("new", "old"), ClipboardHistory.entries(context))
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 2)
    assertEquals(2, ClipboardHistory.days(context))
    assertEquals(listOf("new"), ClipboardHistory.entries(context))
    // Deleted from the file, not only left out.
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 0)
    assertEquals(listOf("new"), ClipboardHistory.entries(context))
  }

  @Test
  fun copyingAgainRenewsTheTime() {
    ClipboardHistory.record(context, "a")
    ClipboardHistory.record(context, "b")
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    setTimes(now - 5 * day, now - 5 * day)
    ClipboardHistory.record(context, "a")
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 1)
    assertEquals(listOf("a"), ClipboardHistory.entries(context))
  }

  @Test
  fun editedEntryKeepsItsTime() {
    ClipboardHistory.record(context, "a")
    ClipboardHistory.record(context, "b")
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    setTimes(now, now - 5 * day)
    ClipboardHistory.replace(context, "a", "a edited")
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 1)
    assertEquals(listOf("b"), ClipboardHistory.entries(context))
  }

  @Test
  fun entriesStoredWithoutTimesAreKeptFromNow() {
    historyFile()
      .edit()
      .putString("entries", JSONArray(listOf("b", "a")).toString())
      .remove("times")
      .commit()
    ClipboardHistory.setLimits(context, ClipboardHistory.DEFAULT_MAX_ENTRIES, 1)
    assertEquals(listOf("b", "a"), ClipboardHistory.entries(context))
    assertEquals(2, JSONArray(historyFile().getString("times", null)).length())
  }

  @Test
  fun addedTextGoesOnANewLineInPlaceOfTheText() {
    ClipboardHistory.copy(context, "first")
    ClipboardHistory.append(context, "second")
    ClipboardHistory.append(context, "third")
    assertEquals("first\nsecond\nthird", clip())
    assertEquals(listOf("first\nsecond\nthird"), ClipboardHistory.entries(context))
  }

  @Test
  fun addedTextPastTenThousandCharactersIsKept() {
    // Report #612: text copied and added to was cut to 10,000 characters.
    val first = "a".repeat(9_000)
    val added = "b".repeat(2_000)
    ClipboardHistory.copy(context, first)
    repeat(3) { ClipboardHistory.append(context, added) }
    val expected = first + "\n$added".repeat(3)
    assertEquals(expected, clip())
    assertEquals(listOf(expected), ClipboardHistory.entries(context))
  }

  @Test
  fun addedTextIsCutToTheLongestEntry() {
    ClipboardHistory.copy(context, "x".repeat(ClipboardHistory.MAX_LENGTH - 5))
    ClipboardHistory.append(context, "more text")
    assertEquals(ClipboardHistory.MAX_LENGTH, clip()?.length)
    assertEquals(ClipboardHistory.MAX_LENGTH, ClipboardHistory.entries(context).single().length)
  }

  @Test
  fun historyKeepsOnlyTheNewestCharacters() {
    val count = ClipboardHistory.MAX_TOTAL_LENGTH / ClipboardHistory.MAX_LENGTH
    repeat(count + 1) { ClipboardHistory.record(context, "$it".repeat(ClipboardHistory.MAX_LENGTH)) }
    val entries = ClipboardHistory.entries(context)
    assertEquals(count, entries.size)
    assertEquals("$count".repeat(ClipboardHistory.MAX_LENGTH), entries.first())
  }

  @Test
  fun addedTextIsAllThereIsWhenNothingWasCopied() {
    assertEquals("only", ClipboardHistory.append(context, "only"))
  }

  @Test
  fun removedAndClearedEntriesAreGone() {
    ClipboardHistory.record(context, "a")
    ClipboardHistory.record(context, "b")
    ClipboardHistory.remove(context, "a")
    assertEquals(listOf("b"), ClipboardHistory.entries(context))
    ClipboardHistory.clear(context)
    assertEquals(emptyList<String>(), ClipboardHistory.entries(context))
  }

  @Test
  fun removedCheckedEntriesAreGoneAndTheOthersKeepTheirOrder() {
    ClipboardHistory.record(context, "d")
    ClipboardHistory.record(context, "c")
    ClipboardHistory.record(context, "b")
    ClipboardHistory.record(context, "a")
    ClipboardHistory.removeAll(context, listOf("c", "a", "not there"))
    assertEquals(listOf("b", "d"), ClipboardHistory.entries(context))
    ClipboardHistory.removeAll(context, emptyList())
    assertEquals(listOf("b", "d"), ClipboardHistory.entries(context))
  }

  @Test
  fun checkedEntriesExportNewestFirst() {
    ClipboardHistory.record(context, "c")
    ClipboardHistory.record(context, "b")
    ClipboardHistory.record(context, "a")
    val selection = ClipboardSelection()
    selection.toggle("c")
    selection.toggle("a")
    assertEquals(
      "a\n\n---\n\nc\n",
      ClipboardHistory.exportText(selection.checkedIn(ClipboardHistory.entries(context))),
    )
  }

  @Test
  fun editedTextTakesThePlaceOfTheEntry() {
    ClipboardHistory.record(context, "c")
    ClipboardHistory.record(context, "b")
    ClipboardHistory.record(context, "a")
    ClipboardHistory.copy(context, "on clipboard")
    assertTrue(ClipboardHistory.replace(context, "b", "b edited"))
    assertEquals(listOf("on clipboard", "a", "b edited", "c"), ClipboardHistory.entries(context))
    assertEquals("on clipboard", clip())
  }

  @Test
  fun editedTextSameAsAnotherEntryReplacesItOnce() {
    ClipboardHistory.record(context, "c")
    ClipboardHistory.record(context, "b")
    ClipboardHistory.record(context, "a")
    assertTrue(ClipboardHistory.replace(context, "c", "a"))
    assertEquals(listOf("b", "a"), ClipboardHistory.entries(context))
  }

  @Test
  fun blankEditChangesNothing() {
    ClipboardHistory.record(context, "a")
    assertFalse(ClipboardHistory.replace(context, "a", " \n "))
    assertEquals(listOf("a"), ClipboardHistory.entries(context))
  }

  @Test
  fun editOfARemovedEntryGoesAtTheTop() {
    ClipboardHistory.record(context, "a")
    assertTrue(ClipboardHistory.replace(context, "gone", "new"))
    assertEquals(listOf("new", "a"), ClipboardHistory.entries(context))
  }

  @Test
  fun editedTextIsCutToTheLongestEntry() {
    ClipboardHistory.record(context, "a")
    ClipboardHistory.replace(context, "a", "x".repeat(ClipboardHistory.MAX_LENGTH + 10))
    assertEquals(ClipboardHistory.MAX_LENGTH, ClipboardHistory.entries(context).single().length)
  }

  @Test
  fun exportedTextSeparatesEntriesWithALineOfHyphens() {
    assertEquals(
      "newest\n\n---\n\ntwo\nlines\n\n---\n\noldest\n",
      ClipboardHistory.exportText(listOf("newest", "two\nlines", "oldest")),
    )
    assertEquals("only\n", ClipboardHistory.exportText(listOf("only")))
    assertEquals("", ClipboardHistory.exportText(emptyList()))
  }
}
