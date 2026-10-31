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

package com.google.android.accessibility.talkback.compositor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentClicksTest {
  private var now = 1_000L
  private val clicks = SentClicks { now }

  @Test
  fun aClickEventSentWhileTheClickWasCarriedOutHadItsCuesGiven() {
    clicks.add(start = 1_000L, end = 1_030L)
    assertTrue(clicks.isSentDuring(1_000L))
    assertTrue(clicks.isSentDuring(1_030L))
  }

  @Test
  fun aClickEventSentBeforeOrAfterTheClickIsNotSandFlys() {
    clicks.add(start = 1_000L, end = 1_030L)
    assertFalse(clicks.isSentDuring(990L))
    assertFalse(clicks.isSentDuring(1_100L))
  }

  @Test
  fun theSaidStateIsMatchedWhenReportedSoonAfterTheClick() {
    clicks.add(start = 1_000L, end = 1_030L).said = "On"
    assertTrue(clicks.isSaid(1_020L, "On"))
    assertTrue(clicks.isSaid(1_130L, " on "))
  }

  @Test
  fun anotherStateIsStillSaid() {
    clicks.add(start = 1_000L, end = 1_030L).said = "On"
    assertFalse(clicks.isSaid(1_130L, "Off"))
  }

  @Test
  fun aStateReportedLongAfterTheClickIsStillSaid() {
    clicks.add(start = 1_000L, end = 1_030L).said = "On"
    assertFalse(clicks.isSaid(2_000L, "On"))
  }

  @Test
  fun aClickWithoutASaidStateMatchesNoReport() {
    clicks.add(start = 1_000L, end = 1_030L)
    assertFalse(clicks.isSaid(1_020L, "On"))
  }

  @Test
  fun clicksAreForgottenAfterAWhile() {
    clicks.add(start = 1_000L, end = 1_030L)
    now += 20_000L
    assertTrue(clicks.isEmpty())
    assertFalse(clicks.isSentDuring(1_010L))
  }
}
