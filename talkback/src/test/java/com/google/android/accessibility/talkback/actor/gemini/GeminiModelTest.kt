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

package com.google.android.accessibility.talkback.actor.gemini

import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiModelTest {
  @Test
  fun automaticAsksTheBuiltInModelThenFlashLite() {
    assertEquals(
      listOf(BuildConfig.GEMINI_MODEL, GeminiModel.FALLBACK),
      GeminiModel.chain(FakeSharedPreferences()),
    )
  }

  @Test
  fun aChosenModelComesFirst() {
    val prefs = FakeSharedPreferences()
    GeminiModel.setChosen(prefs, " gemini-3.6-flash ")
    assertEquals(listOf("gemini-3.6-flash", GeminiModel.FALLBACK), GeminiModel.chain(prefs))
    GeminiModel.setChosen(prefs, GeminiModel.FALLBACK)
    assertEquals(listOf(GeminiModel.FALLBACK), GeminiModel.chain(prefs))
    GeminiModel.setChosen(prefs, "")
    assertEquals("", GeminiModel.chosen(prefs))
  }

  @Test
  fun theListKeepsModelsThatDescribeImages() {
    val body =
      """
      {"models": [
        {"name": "models/gemini-3.8-flash", "displayName": "Gemini 3.8 Flash",
         "supportedGenerationMethods": ["generateContent", "countTokens"]},
        {"name": "models/gemini-3.8-flash-tts", "displayName": "TTS",
         "supportedGenerationMethods": ["generateContent"]},
        {"name": "models/gemini-embedding-001", "supportedGenerationMethods": ["embedContent"]},
        {"name": "models/gemini-3.1-flash-lite", "supportedGenerationMethods": ["generateContent"]},
        {"name": "models/imagen-4", "supportedGenerationMethods": ["predict"]}
      ]}
      """
    assertEquals(
      listOf(
        GeminiModel.Choice("gemini-3.8-flash", "Gemini 3.8 Flash"),
        GeminiModel.Choice("gemini-3.1-flash-lite", "gemini-3.1-flash-lite"),
      ),
      GeminiModel.parse(body),
    )
    assertThrows(IOException::class.java) { GeminiModel.parse("not json") }
  }
}

class AiModelsTest {
  @Test
  fun theListKeepsChatModels() {
    val body =
      """
      {"data": [
        {"id": "claude-opus-5", "display_name": "Claude Opus 5"},
        {"id": "text-embedding-3-large"},
        {"id": "whisper-1"},
        {"id": "mistral-large-latest", "name": "Mistral Large"},
        {"id": "gpt-6"}
      ]}
      """
    assertEquals(
      listOf(
        GeminiModel.Choice("claude-opus-5", "Claude Opus 5"),
        GeminiModel.Choice("mistral-large-latest", "Mistral Large"),
        GeminiModel.Choice("gpt-6", "gpt-6"),
      ),
      AiModels.parse(body),
    )
  }
}
