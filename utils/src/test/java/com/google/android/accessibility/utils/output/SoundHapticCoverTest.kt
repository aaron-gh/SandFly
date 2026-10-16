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

package com.google.android.accessibility.utils.output

import com.google.android.accessibility.utils.Performance.EventId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundHapticCoverTest {
  private val cover = SoundHapticCover()
  private val event = EventId(1000, 0, 0)
  private val otherEvent = EventId(2000, 0, 0)

  @Test
  fun nothingIsSkippedBeforeAnySoundVibrates() {
    assertFalse(cover.covers(event, 0))
    assertFalse(cover.covers(null, 0))
  }

  @Test
  fun theSameEventsVibrationIsSkippedRightAfterItsSound() {
    cover.soundVibrated(event, 100)
    assertTrue(cover.covers(event, 100))
    assertTrue(cover.covers(event, 100 + SoundHapticCover.COVER_MILLIS - 1))
  }

  @Test
  fun aLaterVibrationOfTheSameEventPlays() {
    cover.soundVibrated(event, 100)
    assertFalse(cover.covers(event, 100 + SoundHapticCover.COVER_MILLIS))
  }

  @Test
  fun anotherEventsVibrationPlays() {
    cover.soundVibrated(event, 100)
    assertFalse(cover.covers(otherEvent, 110))
    assertFalse(cover.covers(null, 110))
  }

  @Test
  fun untrackedEventsCoverEachOther() {
    cover.soundVibrated(null, 100)
    assertTrue(cover.covers(null, 120))
  }
}
