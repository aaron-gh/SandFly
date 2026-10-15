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

package com.google.android.accessibility.talkback.stucktouch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InjectedPointersTest {

  private fun parse(text: String) = InjectedPointers.parse(text.lineSequence())

  @Test
  fun readsPointersHeldForTheAccessibilityDevice() {
    val dump =
      """
      Input Dispatcher State:
        TouchStatesByDisplay: <no displays touched>
        Filter verifier on display 0:
          Touching pointer IDs by device: {DeviceId(-1): {4, 1}}
          Hovering pointer IDs by device: {}
          Down times by device: {DeviceId(-1): 17735150000000, DeviceId(7): 10086882272000}
        RecentQueue: length=10
      """
        .trimIndent()
    assertEquals(mapOf(0 to setOf(4, 1)), parse(dump))
  }

  @Test
  fun leavesOutDisplaysWithNothingHeld() {
    val dump =
      """
        Filter verifier on display 0:
          Touching pointer IDs by device: {}
        Filter verifier on display 1:
          Touching pointer IDs by device: {DeviceId(3): {0}, DeviceId(-1): {3}}
      """
        .trimIndent()
    assertEquals(mapOf(1 to setOf(3)), parse(dump))
  }

  @Test
  fun ignoresOtherDevices() {
    val dump =
      """
        Filter verifier on display 0:
          Touching pointer IDs by device: {DeviceId(7): {0, 1}}
      """
        .trimIndent()
    assertEquals(emptyMap<Int, Set<Int>>(), parse(dump))
  }

  @Test
  fun returnsNullWithoutTheVerifier() {
    assertNull(parse("Input Dispatcher State:\n  TouchStatesByDisplay: <no displays touched>"))
  }
}
