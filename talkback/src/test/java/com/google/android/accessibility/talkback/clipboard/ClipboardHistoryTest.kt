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
import org.junit.Before
import org.junit.Test
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
    ClipboardHistory.clear(context)
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
    repeat(ClipboardHistory.MAX_ENTRIES + 5) { ClipboardHistory.record(context, "text $it") }
    val entries = ClipboardHistory.entries(context)
    assertEquals(ClipboardHistory.MAX_ENTRIES, entries.size)
    assertEquals("text ${ClipboardHistory.MAX_ENTRIES + 4}", entries.first())
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
}
