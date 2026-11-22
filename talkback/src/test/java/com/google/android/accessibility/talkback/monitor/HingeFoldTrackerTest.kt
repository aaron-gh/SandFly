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

package com.google.android.accessibility.talkback.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HingeFoldTrackerTest {

  @Test
  fun theFirstReadingSetsTheStateWithoutAChange() {
    val closed = HingeFoldTracker()
    assertNull(closed.folded)
    assertFalse(closed.onAngle(0f))
    assertEquals(true, closed.folded)

    val open = HingeFoldTracker()
    assertFalse(open.onAngle(180f))
    assertEquals(false, open.folded)
  }

  @Test
  fun aGalaxyFlipFoldsAndUnfoldsThroughNinetyDegrees() {
    // A Galaxy Z Flip reports only 0, 90 and 180.
    val tracker = HingeFoldTracker()
    tracker.onAngle(180f)
    assertFalse(tracker.onAngle(90f))
    assertTrue(tracker.onAngle(0f))
    assertEquals(true, tracker.folded)
    assertTrue(tracker.onAngle(90f))
    assertEquals(false, tracker.folded)
    assertFalse(tracker.onAngle(180f))
  }

  @Test
  fun anglesBetweenClosedAndOpenKeepTheState() {
    val tracker = HingeFoldTracker()
    tracker.onAngle(0f)
    // Held nearly closed, or jostled while closed.
    assertFalse(tracker.onAngle(30f))
    assertFalse(tracker.onAngle(10f))
    assertEquals(true, tracker.folded)
    assertTrue(tracker.onAngle(HingeFoldTracker.OPEN_AT_LEAST))
    assertFalse(tracker.onAngle(30f))
    assertEquals(false, tracker.folded)
    assertTrue(tracker.onAngle(HingeFoldTracker.CLOSED_AT_MOST))
  }

  @Test
  fun aFirstReadingBetweenClosedAndOpenIsOpen() {
    val tracker = HingeFoldTracker()
    tracker.onAngle(30f)
    assertEquals(false, tracker.folded)
  }
}
