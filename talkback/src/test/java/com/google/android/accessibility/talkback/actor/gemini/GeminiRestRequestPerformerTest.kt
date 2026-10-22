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

package com.google.android.accessibility.talkback.actor.gemini

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiRestRequestPerformerTest {
  private val postData =
    JSONObject().put("contents", "x").put("generationConfig", JSONObject().put("temperature", 0))

  private fun thinkingConfig(model: String): JSONObject? =
    GeminiRestRequestPerformer.withLeastThinking(postData, model)
      .optJSONObject("generationConfig")
      ?.optJSONObject("thinkingConfig")

  @Test
  fun latestAndNewerModelsGetTheMinimalThinkingLevel() {
    assertEquals("minimal", thinkingConfig("gemini-flash-latest")?.getString("thinkingLevel"))
    assertEquals("minimal", thinkingConfig("gemini-3.5-flash")?.getString("thinkingLevel"))
  }

  @Test
  fun gemini25GetsNoThinkingBudget() {
    assertEquals(0, thinkingConfig("gemini-2.5-flash")?.getInt("thinkingBudget"))
  }

  @Test
  fun olderModelsAreAskedAsBefore() {
    assertSame(postData, GeminiRestRequestPerformer.withLeastThinking(postData, "gemini-2.0-flash"))
  }

  @Test
  fun theRequestItselfIsNotChanged() {
    val copy = GeminiRestRequestPerformer.withLeastThinking(postData, "gemini-flash-latest")
    assertEquals(0, copy.getJSONObject("generationConfig").getInt("temperature"))
    assertFalse(postData.getJSONObject("generationConfig").has("thinkingConfig"))
  }

  @Test
  fun aRejectedThinkingSettingIsRecognized() {
    val rejected =
      GeminiFailure.fromResponse(
        400,
        """{"error":{"code":400,"status":"INVALID_ARGUMENT",""" +
          """"message":"Thinking level is not supported for this model."}}""",
      )
    assertTrue(GeminiRestRequestPerformer.rejectedThinkingSetting(rejected))
    val other =
      GeminiFailure.fromResponse(
        400,
        """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Bad image."}}""",
      )
    assertFalse(GeminiRestRequestPerformer.rejectedThinkingSetting(other))
  }

  @Test
  fun aModelWithAnotherAfterItIsGivenLessTime() {
    assertEquals(15_000, GeminiRestRequestPerformer.timeoutMs(true, 0))
    assertEquals(30_000, GeminiRestRequestPerformer.timeoutMs(false, 0))
    assertEquals(17_000, GeminiRestRequestPerformer.timeoutMs(false, 15_000))
    assertEquals(8_000, GeminiRestRequestPerformer.timeoutMs(false, 30_000))
  }
}
