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

package com.google.android.accessibility.talkback.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDictationTest {
  @Test
  fun answersKeepTheirTextWithoutWrapping() {
    assertEquals("Hello, world.", AiDictation.cleanUp("  Hello, world.\n"))
    assertEquals("Hello, world.", AiDictation.cleanUp("\"\"\"Hello, world.\"\"\""))
    assertEquals("Hello, world.", AiDictation.cleanUp("```\nHello, world.\n```"))
    assertEquals("\"Quoted,\" she said.", AiDictation.cleanUp("\"Quoted,\" she said."))
  }

  @Test
  fun promptHasTheRecognizedSpeechAndRecentContext() {
    val before = "x".repeat(1000) + "Dear Sam,"
    val prompt = AiDictation.prompt(before, "thanks for the help")
    assertTrue(prompt.contains("\"\"\"thanks for the help\"\"\""))
    assertTrue(prompt.contains("Dear Sam,\"\"\""))
    assertTrue(!prompt.contains("x".repeat(301)))
    // Rambler's cleanup, not only punctuation.
    assertTrue(prompt.contains("filler words"))
    assertTrue(prompt.contains("keep only the correction"))
  }
}
