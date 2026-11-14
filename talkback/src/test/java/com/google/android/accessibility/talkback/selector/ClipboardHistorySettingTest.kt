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

package com.google.android.accessibility.talkback.selector

import com.google.android.accessibility.utils.ValueSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks which clipboard history text the Clipboard history reading control says next. */
class ClipboardHistorySettingTest {

  private val entries = listOf("newest", "middle", "oldest")

  @Test
  fun firstSwipeDownSaysTheNewestAndUpTheOldest() {
    assertEquals("newest", ClipboardHistorySetting.next(entries, null, isNext = true))
    assertEquals("oldest", ClipboardHistorySetting.next(entries, null, isNext = false))
  }

  @Test
  fun swipesGoThroughTheEntriesAndAround() {
    assertEquals("middle", ClipboardHistorySetting.next(entries, "newest", isNext = true))
    assertEquals("newest", ClipboardHistorySetting.next(entries, "oldest", isNext = true))
    assertEquals("oldest", ClipboardHistorySetting.next(entries, "newest", isNext = false))
  }

  @Test
  fun withWrapAroundOffTheLastEntryIsSaidAgain() {
    var text: String? = null
    val stopped = ValueSteps.run(false) { text = ClipboardHistorySetting.next(entries, "oldest", isNext = true) }
    assertTrue(stopped)
    assertEquals("oldest", text)
  }

  @Test
  fun anEntryNoLongerInTheHistoryStartsAgain() {
    assertEquals("newest", ClipboardHistorySetting.next(entries, "removed", isNext = true))
  }

  @Test
  fun anEmptyHistoryHasNothingToSay() {
    assertNull(ClipboardHistorySetting.next(emptyList(), "newest", isNext = true))
  }
}
