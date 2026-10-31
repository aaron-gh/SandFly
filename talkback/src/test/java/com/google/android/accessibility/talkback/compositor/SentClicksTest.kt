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
  fun aClickEventSentWhileTheClickWasCarriedOutIsSandFlys() {
    clicks.add(start = 1_000L, end = 1_030L, packageName = "app")
    assertTrue(clicks.takeClickEvent(1_010L, "app"))
  }

  @Test
  fun aClickEventSentAfterTheClickReturnedIsSandFlys() {
    // Web content carries the click out after the click returns.
    clicks.add(start = 1_000L, end = 1_001L, packageName = "app")
    assertTrue(clicks.takeClickEvent(1_027L, "app"))
  }

  @Test
  fun eachClickMatchesOneClickEvent() {
    clicks.add(start = 1_000L, end = 1_030L, packageName = "app")
    assertTrue(clicks.takeClickEvent(1_010L, "app"))
    assertFalse(clicks.takeClickEvent(1_020L, "app"))
  }

  @Test
  fun aClickEventFromAnotherAppIsNotSandFlys() {
    clicks.add(start = 1_000L, end = 1_030L, packageName = "app")
    assertFalse(clicks.takeClickEvent(1_010L, "other"))
  }

  @Test
  fun aClickEventSentBeforeOrLongAfterTheClickIsNotSandFlys() {
    clicks.add(start = 1_000L, end = 1_030L, packageName = "app")
    assertFalse(clicks.takeClickEvent(990L, "app"))
    assertFalse(clicks.takeClickEvent(2_100L, "app"))
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
    assertFalse(clicks.takeClickEvent(1_010L, null))
  }
}
