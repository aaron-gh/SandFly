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

package com.google.android.accessibility.talkback.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Text changes are noted only near the end of a touch that long-pressed a Gboard key. */
class GboardPopupNotesTest {
  @Test
  fun aTextChangeSoonAfterTheTouchIsNoted() {
    assertEquals(120L, GboardPopupNotes.msAfterTouchEnd(eventTime = 10_120L, end = 10_000L))
  }

  @Test
  fun aTextChangeJustBeforeTheTouchEndIsNoted() {
    assertEquals(-40L, GboardPopupNotes.msAfterTouchEnd(eventTime = 9_960L, end = 10_000L))
  }

  @Test
  fun aLaterTextChangeIsNotNoted() {
    val late = 10_000L + GboardPopupNotes.TEXT_CHANGE_WINDOW_MS + 1
    assertNull(GboardPopupNotes.msAfterTouchEnd(eventTime = late, end = 10_000L))
  }

  @Test
  fun nothingIsNotedBeforeAnyLongPress() {
    assertNull(GboardPopupNotes.msAfterTouchEnd(eventTime = 10_000L, end = -1L))
  }
}
