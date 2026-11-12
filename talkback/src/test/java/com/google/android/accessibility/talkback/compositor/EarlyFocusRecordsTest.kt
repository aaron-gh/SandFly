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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

  @Test
  fun aChangeJustAfterASpokenFocusIsItsReaction() {
    records.addSpoken("a", actionTime = 1_000L)
    assertTrue(records.hasSpokenFocus())
    // Another node, a change sent before the action, and an unknown time.
    assertFalse(records.consumeReactionToSpokenFocus("b", eventTime = 1_003L))
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 999L))
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 0L))
    assertFalse(records.consumeReactionToSpokenFocus(null, eventTime = 1_003L))
    // After the focus event used the entry.
    assertEquals(SPOKEN, records.match("a", eventTime = 1_003L, consume = true))
    assertTrue(records.consumeReactionToSpokenFocus("a", eventTime = 1_050L))
  }

  @Test
  fun onlyTheFirstChangeAfterASpokenFocusIsItsReaction() {
    records.addSpoken("a", actionTime = 1_000L)
    assertTrue(records.consumeReactionToSpokenFocus("a", eventTime = 1_050L))
    // Such as Play becoming Pause after a quick double tap.
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 1_600L))
    assertFalse(records.hasSpokenFocus())
  }

  @Test
  fun aChangeAfterAnActionIsNotAReactionToTheFocus() {
    records.addSpoken("a", actionTime = 1_000L)
    records.addSpoken("b", actionTime = 1_300L)
    records.forgetSpokenFocusBefore(actionTime = 1_200L)
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 1_250L))
    assertTrue(records.consumeReactionToSpokenFocus("b", eventTime = 1_350L))
    // No action performed yet.
    records.addSpoken("c", actionTime = 1_400L)
    records.forgetSpokenFocusBefore(actionTime = 0L)
    assertTrue(records.hasSpokenFocus())
  }

  @Test
  fun aClickForgetsEverySpokenFocus() {
    records.addSpoken("a", actionTime = 1_000L)
    records.addSpoken("b", actionTime = 1_100L)
    records.forgetSpokenFocus()
    assertFalse(records.hasSpokenFocus())
    assertFalse(records.consumeReactionToSpokenFocus("b", eventTime = 1_150L))
  }

  @Test
  fun aChangeLongAfterASpokenFocusIsNotItsReaction() {
    records.addSpoken("a", actionTime = 1_000L)
    assertFalse(
      records.consumeReactionToSpokenFocus("a", eventTime = 1_001L + EarlyFocusRecords.REACTION_MS)
    )
    now = 1_001L + EarlyFocusRecords.REACTION_MS
    assertFalse(records.hasSpokenFocus())
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 1_500L))
  }

  @Test
  fun focusNotSpokenEarlyHasNoReaction() {
    records.addStateUpdated("a", actionTime = 1_000L)
    assertFalse(records.consumeReactionToSpokenFocus("a", eventTime = 1_003L))
    // Focused again without being spoken early, such as by touch.
    records.addSpoken("b", actionTime = 1_100L)
    records.forget("b")
    assertFalse(records.consumeReactionToSpokenFocus("b", eventTime = 1_103L))
    records.addSpoken("c", actionTime = 1_200L)
    records.clear()
    assertFalse(records.hasSpokenFocus())
  }
}
