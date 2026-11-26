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

import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The braille keyboard's next and previous granularity gestures go to the nearest editing reading
 * control in the list, and find none, without looping, when the list has none.
 */
class AdjacentSettingTest {
  private val wanted = setOf(Setting.GRANULARITY_CHARACTERS, Setting.GRANULARITY_WORDS)
  private val entries =
    listOf(
      Setting.GRANULARITY_HEADINGS,
      Setting.GRANULARITY_CHARACTERS,
      Setting.GRANULARITY_LINKS,
      Setting.GRANULARITY_WORDS,
      Setting.SPEECH_RATE,
    )

  @Test
  fun next_skipsOtherControls() {
    assertEquals(
      Setting.GRANULARITY_WORDS,
      SelectorController.adjacentSettingOf(entries, 1, true, wanted),
    )
    assertEquals(
      Setting.GRANULARITY_WORDS,
      SelectorController.adjacentSettingOf(entries, 2, true, wanted),
    )
  }

  @Test
  fun next_wrapsAround() {
    assertEquals(
      Setting.GRANULARITY_CHARACTERS,
      SelectorController.adjacentSettingOf(entries, 3, true, wanted),
    )
  }

  @Test
  fun previous_wrapsAround() {
    assertEquals(
      Setting.GRANULARITY_WORDS,
      SelectorController.adjacentSettingOf(entries, 1, false, wanted),
    )
    assertEquals(
      Setting.GRANULARITY_CHARACTERS,
      SelectorController.adjacentSettingOf(entries, 2, false, wanted),
    )
  }

  @Test
  fun selectedNotInList_startsAtEnd() {
    assertEquals(
      Setting.GRANULARITY_CHARACTERS,
      SelectorController.adjacentSettingOf(entries, -1, true, wanted),
    )
    assertEquals(
      Setting.GRANULARITY_WORDS,
      SelectorController.adjacentSettingOf(entries, -1, false, wanted),
    )
  }

  @Test
  fun onlyOne_returnsItAgain() {
    val one = listOf(Setting.GRANULARITY_HEADINGS, Setting.GRANULARITY_CHARACTERS)
    assertEquals(
      Setting.GRANULARITY_CHARACTERS,
      SelectorController.adjacentSettingOf(one, 1, true, wanted),
    )
  }

  @Test
  fun noneInList_returnsNull() {
    val none = listOf<Any>(Setting.GRANULARITY_HEADINGS, Setting.GRANULARITY_LINKS, "script")
    assertNull(SelectorController.adjacentSettingOf(none, -1, true, wanted))
    assertNull(SelectorController.adjacentSettingOf(none, 0, false, wanted))
    assertNull(SelectorController.adjacentSettingOf(emptyList<Any>(), -1, true, wanted))
  }
}
