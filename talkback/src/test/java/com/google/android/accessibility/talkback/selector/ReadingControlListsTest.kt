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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReadingControlListsTest {
  @Test
  fun commandValue_namesTheListAndDirection() {
    val next = ReadingControlLists.valueOf("abc", true)
    val previous = ReadingControlLists.valueOf("abc", false)
    assertTrue(ReadingControlLists.isCommand(next))
    assertTrue(ReadingControlLists.isNext(next))
    assertFalse(ReadingControlLists.isNext(previous))
    assertEquals("abc", ReadingControlLists.idOf(next))
    assertEquals("abc", ReadingControlLists.idOf(previous))
    assertFalse(ReadingControlLists.isCommand("COPY"))
  }

  @Test
  fun json_roundTrips() {
    val lists =
      listOf(
        ReadingControlLists.ControlList("1", "Speech", listOf("a", "b")),
        ReadingControlLists.ControlList("2", "Web", emptyList()),
      )
    assertEquals(lists, ReadingControlLists.fromJson(ReadingControlLists.toJson(lists)))
  }

  @Test
  fun badJson_isEmpty() {
    assertEquals(emptyList<ReadingControlLists.ControlList>(), ReadingControlLists.fromJson("{"))
  }
}
