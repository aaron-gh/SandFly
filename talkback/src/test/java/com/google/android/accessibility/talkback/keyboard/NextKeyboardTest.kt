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

package com.google.android.accessibility.talkback.keyboard

import com.google.android.accessibility.talkback.keyboard.NextKeyboard.nextIndex
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NextKeyboardTest {
  private val enabled = listOf("gboard", "braille", "samsung")

  @Test
  fun current_goesToTheNext() {
    assertEquals(1, nextIndex(enabled, "gboard"))
    assertEquals(2, nextIndex(enabled, "braille"))
  }

  @Test
  fun last_goesRoundToTheFirst() {
    assertEquals(0, nextIndex(enabled, "samsung"))
  }

  @Test
  fun currentNotEnabled_goesToTheFirst() {
    assertEquals(0, nextIndex(enabled, "other"))
    assertEquals(0, nextIndex(enabled, null))
  }

  @Test
  fun onlyTheCurrent_hasNoNext() {
    assertEquals(-1, nextIndex(listOf("gboard"), "gboard"))
  }

  @Test
  fun noneEnabled_hasNoNext() {
    assertEquals(-1, nextIndex(emptyList<String>(), "gboard"))
  }
}
