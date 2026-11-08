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

package com.google.android.accessibility.talkback.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class LightLevelsTest {
  @Test
  fun firstReading_fallsInItsLevel() {
    assertEquals(0, LightLevels.level(1f, null))
    assertEquals(1, LightLevels.level(10f, null))
    assertEquals(2, LightLevels.level(100f, null))
    assertEquals(3, LightLevels.level(1000f, null))
    assertEquals(4, LightLevels.level(20000f, null))
  }

  @Test
  fun levelHolds_untilTheReadingPassesTheMargin() {
    assertEquals(1, LightLevels.level(55f, 1))
    assertEquals(2, LightLevels.level(61f, 1))
    assertEquals(2, LightLevels.level(45f, 2))
    assertEquals(1, LightLevels.level(39f, 2))
  }
}
