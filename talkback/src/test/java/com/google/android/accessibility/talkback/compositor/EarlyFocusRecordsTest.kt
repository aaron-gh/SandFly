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

package com.google.android.accessibility.talkback.compositor

import com.google.android.accessibility.talkback.compositor.EarlyFocusMatch.NONE
import com.google.android.accessibility.talkback.compositor.EarlyFocusMatch.SPOKEN
import com.google.android.accessibility.talkback.compositor.EarlyFocusMatch.STALE
import com.google.android.accessibility.talkback.compositor.EarlyFocusMatch.STATE_UPDATED
import org.junit.Assert.assertEquals
import org.junit.Test

class EarlyFocusRecordsTest {
  private var now = 1_000L
  private val records = EarlyFocusRecords<String>(maxItems = 4, maxAgeMs = 2_000L, clock = { now })

  @Test
  fun theEventOfASpokenFocusIsConsumedOnce() {
    records.addSpoken("a", actionTime = 1_000L)
    assertEquals(SPOKEN, records.match("a", eventTime = 1_001L, consume = false))
    assertEquals(SPOKEN, records.match("a", eventTime = 1_001L, consume = true))
    // A later focus of the same node, such as by touch, is spoken.
    assertEquals(NONE, records.match("a", eventTime = 1_500L, consume = true))
  }

  @Test
  fun aFailedEarlySpeechOnlySkipsTheStateUpdate() {
    records.addStateUpdated("a", actionTime = 1_000L)
    assertEquals(STATE_UPDATED, records.match("a", eventTime = 1_001L, consume = true))
    assertEquals(NONE, records.match("a", eventTime = 1_002L, consume = true))
  }

  @Test
  fun forgettingANodeLetsItsNextFocusBeSpoken() {
    records.addSpoken("a", actionTime = 1_000L)
    records.forget("a")
    assertEquals(NONE, records.match("a", eventTime = 1_500L, consume = true))
  }

  @Test
  fun clearingForgetsEveryNode() {
    records.addSpoken("a", actionTime = 1_000L)
    records.addSpoken("b", actionTime = 1_010L)
    records.clear()
    assertEquals(NONE, records.match("b", eventTime = 1_011L, consume = true))
  }

  @Test
  fun entriesExpire() {
    records.addSpoken("a", actionTime = 1_000L)
    now = 3_001L
    assertEquals(NONE, records.match("a", eventTime = 1_001L, consume = true))
  }

  @Test
  fun focusEventsOlderThanTheLatestEarlyFocusAreStale() {
    records.addSpoken("b", actionTime = 1_000L)
    assertEquals(STALE, records.match("slider", eventTime = 999L, consume = true))
    assertEquals(STALE, records.match(null, eventTime = 999L, consume = true))
    assertEquals(NONE, records.match("slider", eventTime = 1_000L, consume = true))
    // Time unknown.
    assertEquals(NONE, records.match("slider", eventTime = 0L, consume = true))
    // The entry is still there for its own event.
    assertEquals(SPOKEN, records.match("b", eventTime = 1_002L, consume = true))
  }

  @Test
  fun theEventOfAnEarlierFocusOfTheSameNodeIsStale() {
    records.addSpoken("a", actionTime = 1_000L)
    records.addSpoken("b", actionTime = 1_100L)
    records.addSpoken("a", actionTime = 1_200L)
    // The first focus of a, then b, then the second focus of a, each spoken once.
    assertEquals(STALE, records.match("a", eventTime = 1_001L, consume = true))
    assertEquals(SPOKEN, records.match("b", eventTime = 1_101L, consume = true))
    assertEquals(SPOKEN, records.match("a", eventTime = 1_201L, consume = true))
    assertEquals(NONE, records.match("a", eventTime = 1_500L, consume = true))
  }

  @Test
  fun anEventOfANodeNotLookedUpIsNotMatched() {
    records.addSpoken("a", actionTime = 1_000L)
    assertEquals(NONE, records.match(null, eventTime = 1_001L, consume = true))
    assertEquals(SPOKEN, records.match("a", eventTime = 1_001L, consume = true))
  }
}
