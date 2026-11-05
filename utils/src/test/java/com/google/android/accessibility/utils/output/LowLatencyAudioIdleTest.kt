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

import com.google.android.accessibility.utils.output.LowLatencyAudio.Companion.idleMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LowLatencyAudioIdleTest {
  @Test
  fun trackPausesSoonAfterTheLastSound() {
    assertEquals(250, idleMs(microphone = false, typing = false))
  }

  @Test
  fun trackRunsLongerBetweenKeysWhileTyping() {
    assertEquals(2_000, idleMs(microphone = false, typing = true))
  }

  @Test
  fun recordingAppPausesTheTrackSoonestEvenWhileTyping() {
    assertEquals(100, idleMs(microphone = true, typing = true))
    assertEquals(100, idleMs(microphone = true, typing = false))
  }

  @Test
  fun typingIsKeptUntilSetAgain() {
    try {
      LowLatencyAudio.setTyping(true)
      assertTrue(LowLatencyAudio.isTyping())
      LowLatencyAudio.setTyping(false)
      assertFalse(LowLatencyAudio.isTyping())
    } finally {
      LowLatencyAudio.setTyping(false)
    }
  }
}
