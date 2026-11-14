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

package com.google.android.accessibility.utils.output

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Report #364: a voice profile's fragment was ended after 15 s, however long its text. */
class ProfileVoiceTimeoutTest {
  @Test
  fun longText_getsMoreThanTheTimeItTakes() {
    // 418 characters at about 14 a second take 30 s.
    assertTrue(ProfileVoice.doneTimeoutMs(418, 1f) > 60_000)
  }

  @Test
  fun slowRate_getsLonger() {
    assertEquals(
      ProfileVoice.doneTimeoutMs(0, 1f) + 2 * (ProfileVoice.doneTimeoutMs(100, 1f) - 15_000),
      ProfileVoice.doneTimeoutMs(100, 0.5f),
    )
  }

  @Test
  fun emptyText_keepsTheMinimum() {
    assertEquals(15_000, ProfileVoice.doneTimeoutMs(0, 1f))
  }

  @Test
  fun badRate_isTakenAsNormal() {
    assertEquals(ProfileVoice.doneTimeoutMs(50, 1f), ProfileVoice.doneTimeoutMs(50, Float.NaN))
    assertEquals(ProfileVoice.doneTimeoutMs(50, 0.1f), ProfileVoice.doneTimeoutMs(50, 0f))
  }
}
