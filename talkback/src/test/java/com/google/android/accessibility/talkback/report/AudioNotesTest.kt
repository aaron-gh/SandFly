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

import android.media.AudioAttributes
import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AudioNotesTest {
  @Test
  fun describe_listsPlayersRecordingsAndMode() {
    assertEquals(
      "Playing: assistant (speaker), accessibility (speaker); recording: voice communication; " +
        "mode: in communication",
      AudioNotes.describe(
        listOf("assistant (speaker)", "accessibility (speaker)"),
        listOf("voice communication"),
        AudioManager.MODE_IN_COMMUNICATION,
      ),
    )
  }

  @Test
  fun describe_nothing() {
    assertEquals(
      "Playing: nothing; recording: nothing; mode: normal",
      AudioNotes.describe(emptyList(), emptyList(), AudioManager.MODE_NORMAL),
    )
  }

  @Test
  fun usageName_unknownUsage_givesItsNumber() {
    assertEquals("assistant", AudioNotes.usageName(AudioAttributes.USAGE_ASSISTANT))
    assertEquals("usage 1000", AudioNotes.usageName(1000))
  }

  @Test
  fun modeName_unknownMode_givesItsNumber() {
    assertEquals("mode 99", AudioNotes.modeName(99))
  }
}
