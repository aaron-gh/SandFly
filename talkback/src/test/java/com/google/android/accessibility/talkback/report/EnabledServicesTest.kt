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

package com.google.android.accessibility.talkback.report

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EnabledServicesTest {
  private val names =
    mapOf(0x4 to "FLAG_REQUEST_TOUCH_EXPLORATION_MODE", 0x20 to "FLAG_REQUEST_FILTER_KEY_EVENTS")

  @Test
  fun bitNames_namesKnownBitsAndGivesOthersInHex() {
    assertEquals(
      "FLAG_REQUEST_TOUCH_EXPLORATION_MODE FLAG_REQUEST_FILTER_KEY_EVENTS 0x100",
      EnabledServices.bitNames(0x4 or 0x20 or 0x100) { names[it] },
    )
  }

  @Test
  fun bitNames_unknownNameGivesHex() {
    assertEquals("0x80000000", EnabledServices.bitNames(1 shl 31) { "UNKNOWN" })
  }

  @Test
  fun bitNames_noBits() {
    assertEquals("none", EnabledServices.bitNames(0) { null })
  }
}
