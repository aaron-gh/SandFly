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

package com.google.android.accessibility.talkback.actor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousCopyTest {

  @Test
  fun emptyTextsAndRepeatsAreSkipped() {
    val lines = ContinuousCopy.Lines()
    assertTrue(lines.add("  Inbox "))
    assertTrue(lines.add("Inbox"))
    assertTrue(lines.add(null))
    assertTrue(lines.add("   "))
    assertTrue(lines.add("Archive"))
    assertTrue(lines.add("Inbox"))
    assertEquals(listOf("Inbox", "Archive", "Inbox"), lines.texts)
    assertEquals(17, lines.chars)
    assertFalse(lines.truncated)
  }

  @Test
  fun stopsAtTheItemLimit() {
    val lines = ContinuousCopy.Lines()
    for (i in 1 until ContinuousCopy.MAX_ITEMS) {
      assertTrue(lines.add("Item $i"))
    }
    assertFalse(lines.add("Item ${ContinuousCopy.MAX_ITEMS}"))
    assertTrue(lines.truncated)
    assertFalse(lines.add("More"))
    assertEquals(ContinuousCopy.MAX_ITEMS, lines.texts.size)
  }

  @Test
  fun stopsAtTheCharacterLimitWithTheLastTextCut() {
    val lines = ContinuousCopy.Lines()
    val long = "x".repeat(ContinuousCopy.MAX_CHARS - 3)
    assertTrue(lines.add(long))
    assertFalse(lines.add("abcdef"))
    assertEquals(listOf(long, "abc"), lines.texts)
    assertEquals(ContinuousCopy.MAX_CHARS, lines.chars)
    assertTrue(lines.truncated)
  }

  @Test
  fun cutDoesNotSplitASurrogatePair() {
    val lines = ContinuousCopy.Lines()
    assertTrue(lines.add("x".repeat(ContinuousCopy.MAX_CHARS - 2)))
    assertFalse(lines.add("a\uD83D\uDE00b"))
    assertEquals("a", lines.texts.last())
    assertTrue(lines.truncated)
  }
}
