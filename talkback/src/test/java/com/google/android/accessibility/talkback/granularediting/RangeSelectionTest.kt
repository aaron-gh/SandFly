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

package com.google.android.accessibility.talkback.granularediting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeSelectionTest {
  @Test
  fun end_afterStart_coversBothItems() {
    val range = RangeSelection()
    range.start(2)
    assertTrue(range.isStarted)
    assertEquals(2..5, range.end(5))
    assertFalse(range.isStarted)
  }

  @Test
  fun end_beforeStart_isInListOrder() {
    val range = RangeSelection()
    range.start(7)
    assertEquals(3..7, range.end(3))
  }

  @Test
  fun end_onStartItem_isThatItem() {
    val range = RangeSelection()
    range.start(4)
    assertEquals(4..4, range.end(4))
  }

  @Test
  fun end_withoutStart_isThatItem() {
    val range = RangeSelection()
    assertEquals(6..6, range.end(6))
    assertEquals(RangeSelection.NONE, range.marked)
  }

  @Test
  fun start_again_movesTheMark() {
    val range = RangeSelection()
    range.start(1)
    range.start(9)
    assertEquals(9..12, range.end(12))
  }
}
