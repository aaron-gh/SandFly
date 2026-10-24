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

import org.junit.Assert.*
import org.junit.Test

class RecognitionAnnouncementsTest {
  @Test fun rotorWrapsInBothDirectionsAcrossAllFiveModes() {
    assertEquals(RecognitionMode.DOORS, RecognitionMode.SCENES.step(false))
    assertEquals(RecognitionMode.SCENES, RecognitionMode.DOORS.step(true))
    var mode = RecognitionMode.SCENES
    val seen = mutableSetOf<RecognitionMode>()
    repeat(5) { seen.add(mode); mode = mode.step(true) }
    assertEquals(RecognitionMode.entries.toSet(), seen)
    assertEquals(RecognitionMode.SCENES, mode)
  }

  @Test fun nothingVisibleIsSilent() {
    val results = RecognitionAnnouncements()
    assertFalse(results.shouldSpeak("gemini", " NONE ", 1000))
    assertFalse(results.shouldSpeak("gemini", "None.", 1000))
    assertFalse(results.shouldSpeak("text", "\n", 1000))
  }

  @Test fun repeatedResultsAreNormalizedAndExpire() {
    val results = RecognitionAnnouncements()
    assertTrue(results.shouldSpeak("text", "Hello  world", 1000))
    assertFalse(results.shouldSpeak("text", "HELLO\nworld", 8000))
    assertTrue(results.shouldSpeak("text", "Hello world", 31000))
  }

  @Test fun changedResultsAreRateLimitedButSourcesDoNotSuppressEachOther() {
    val results = RecognitionAnnouncements()
    assertTrue(results.shouldSpeak("text", "First label", 1000))
    assertFalse(results.shouldSpeak("text", "New label", 2000))
    assertTrue(results.shouldSpeak("gemini", "A door", 2000))
    assertTrue(results.shouldSpeak("text", "New label", 5000))
  }

  @Test fun restartingAllowsTheSameResultAgain() {
    val results = RecognitionAnnouncements()
    results.shouldSpeak("gemini", "A door", 1000)
    results.clear()
    assertTrue(results.shouldSpeak("gemini", "A door", 2000))
  }
}
