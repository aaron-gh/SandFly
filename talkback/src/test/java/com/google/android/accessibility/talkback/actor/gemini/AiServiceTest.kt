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

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServiceTest {
  private fun geminiRequest(json: Boolean = false): JSONObject {
    val parts =
      JSONArray()
        .put(JSONObject().put("text", "Describe this image."))
        .put(
          JSONObject()
            .put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", "QUJD"))
        )
    val config = JSONObject().put("temperature", 0.0).put("topP", 0.85).put("topK", 1)
    if (json) config.put("responseMimeType", "application/json")
    return JSONObject()
      .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
      .put("safetySettings", JSONArray())
      .put("generationConfig", config)
  }

  @Test
  fun aServiceThatIsSetUpIsUsedWhenTheChosenOneIsNot() {
    val prefs = FakeSharedPreferences()
    assertFalse(AiService.isSetUp(prefs))
    // Gemini, the first choice, has no key, so Claude is used once it is set up.
    AiProvider.CLAUDE.setApiKey(prefs, "key")
    assertFalse(AiService.isSetUp(prefs))
    AiProvider.CLAUDE.setModel(prefs, "claude-model")
    assertTrue(AiService.isSetUp(prefs))
    assertEquals(AiProvider.CLAUDE, AiProvider.active(prefs))
    assertEquals("https://api.anthropic.com/v1", AiService.address(prefs))
    assertEquals("claude-model", AiService.model(prefs))
    assertEquals("key", AiService.apiKey(prefs))
    AiProvider.CLAUDE.setModel(prefs, " ")
    assertFalse(AiService.isSetUp(prefs))
  }

  @Test
  fun aChosenServiceThatIsSetUpStaysChosen() {
    val prefs = FakeSharedPreferences()
    AiProvider.GEMINI.setApiKey(prefs, "gemini-key")
    AiProvider.CLAUDE.setApiKey(prefs, "key")
    AiProvider.CLAUDE.setModel(prefs, "claude-model")
    AiProvider.setActive(prefs, AiProvider.GEMINI)
    assertEquals(AiProvider.GEMINI, AiProvider.active(prefs))
    assertFalse(AiService.isSetUp(prefs))
    AiProvider.setActive(prefs, AiProvider.NONE)
    assertEquals(AiProvider.NONE, AiProvider.active(prefs))
  }

  @Test
  fun aServiceIsSetUpInOrder() {
    val prefs = FakeSharedPreferences()
    assertEquals(AiProvider.Missing.ADDRESS, AiProvider.OTHER.missing(prefs))
    AiProvider.setOtherAddress(prefs, "https://example.com/v1")
    assertEquals(AiProvider.Missing.API_KEY, AiProvider.OTHER.missing(prefs))
    AiProvider.OTHER.setApiKey(prefs, "key")
    assertEquals(AiProvider.Missing.MODEL, AiProvider.OTHER.missing(prefs))
    AiProvider.OTHER.setModel(prefs, "m")
    assertEquals(null, AiProvider.OTHER.missing(prefs))
    assertEquals(AiProvider.Missing.API_KEY, AiProvider.MISTRAL.missing(prefs))
    assertEquals(AiProvider.Missing.API_KEY, AiProvider.GEMINI.missing(prefs))
    AiProvider.GEMINI.setApiKey(prefs, "gemini key")
    assertEquals(null, AiProvider.GEMINI.missing(prefs))
    assertEquals(null, AiProvider.NONE.missing(prefs))
  }

  @Test
  fun turningAServiceOffFallsBackToGeminiOnlyWhenItHasAKey() {
    val prefs = FakeSharedPreferences()
    assertEquals(AiProvider.NONE, AiProvider.fallback(prefs))
    AiProvider.GEMINI.setApiKey(prefs, "gemini key")
    assertEquals(AiProvider.GEMINI, AiProvider.fallback(prefs))
    AiProvider.setActive(prefs, AiProvider.NONE)
    assertFalse(AiService.isSetUp(prefs))
    assertEquals("", AiService.apiKey(prefs))
  }

  @Test
  fun eachServiceKeepsItsOwnKey() {
    val prefs = FakeSharedPreferences()
    AiProvider.MISTRAL.setApiKey(prefs, "mistral-key")
    AiProvider.OPENAI.setApiKey(prefs, "openai-key")
    assertEquals("mistral-key", AiProvider.MISTRAL.apiKey(prefs))
    assertEquals("openai-key", AiProvider.OPENAI.apiKey(prefs))
  }

  @Test
  fun theOldOtherServiceMovesToItsOwnSettings() {
    val known = FakeSharedPreferences()
    known.edit()
      .putString("pref_ai_service_address", "https://api.groq.com/openai/v1/")
      .putString("pref_ai_service_model", "llama")
      .putString("pref_ai_service_api_key", "key")
      .apply()
    assertEquals(AiProvider.GROQ, AiProvider.active(known))
    assertEquals("llama", AiProvider.GROQ.model(known))
    assertEquals("key", AiProvider.GROQ.apiKey(known))
    assertFalse(known.contains("pref_ai_service_address"))

    val other = FakeSharedPreferences()
    other.edit()
      .putString("pref_ai_service_address", "https://example.com/v1")
      .putString("pref_ai_service_model", "m")
      .putString("pref_ai_service_api_key", "k")
      .apply()
    assertEquals(AiProvider.OTHER, AiProvider.active(other))
    assertEquals("https://example.com/v1", AiService.address(other))

    assertEquals(AiProvider.GEMINI, AiProvider.active(FakeSharedPreferences()))

    // One without a key isn't set up, so Gemini stays in use; its address and model are kept.
    val keyless = FakeSharedPreferences()
    keyless.edit()
      .putString("pref_ai_service_address", "https://api.mistral.ai/v1")
      .putString("pref_ai_service_model", "m")
      .apply()
    assertEquals(AiProvider.GEMINI, AiProvider.active(keyless))
    assertEquals("m", AiProvider.MISTRAL.model(keyless))
  }

  @Test
  fun onlyHttpsAddressesCanBeUsed() {
    assertTrue(AiService.isValidAddress("https://ollama.com/v1"))
    assertFalse(AiService.isValidAddress("http://192.168.1.5:11434/v1"))
    assertFalse(AiService.isValidAddress("ollama.com/v1"))
    assertFalse(AiService.isValidAddress("https://"))
  }

  @Test
  fun chatUrlAddsThePathOnce() {
    assertEquals(
      "https://api.mistral.ai/v1/chat/completions",
      AiService.chatUrl("https://api.mistral.ai/v1/"),
    )
    assertEquals(
      "https://api.mistral.ai/v1/chat/completions",
      AiService.chatUrl("https://api.mistral.ai/v1/chat/completions"),
    )
  }

  @Test
  fun geminiRequestBecomesOneUserMessageWithTextAndImage() {
    val chat = AiService.toChatRequest(geminiRequest(), "claude-sonnet-5-5")

    assertEquals("claude-sonnet-5-5", chat.getString("model"))
    assertFalse(chat.has("temperature"))
    assertFalse(chat.has("response_format"))
    val messages = chat.getJSONArray("messages")
    assertEquals(1, messages.length())
    val message = messages.getJSONObject(0)
    assertEquals("user", message.getString("role"))
    val content = message.getJSONArray("content")
    assertEquals("text", content.getJSONObject(0).getString("type"))
    assertEquals("Describe this image.", content.getJSONObject(0).getString("text"))
    assertEquals("image_url", content.getJSONObject(1).getString("type"))
    assertEquals(
      "data:image/jpeg;base64,QUJD",
      content.getJSONObject(1).getJSONObject("image_url").getString("url"),
    )
  }

  private fun chatResponse(content: Any, finish: String = "stop") =
    JSONObject()
      .put(
        "choices",
        JSONArray()
          .put(
            JSONObject()
              .put("index", 0)
              .put("message", JSONObject().put("role", "assistant").put("content", content))
              .put("finish_reason", finish)
          ),
      )

  @Test
  fun chatAnswerBecomesAGeminiAnswer() {
    val response = AiService.parseChatResponse(chatResponse(" A cat on a sofa. "), false)
    assertEquals("A cat on a sofa.", response.text())
    assertEquals(DataFieldUtils.FINISH_REASON_STOP, response.finishReason())
  }

  @Test
  fun answerCutOffAtTheLengthLimitIsStillAnAnswer() {
    val response = AiService.parseChatResponse(chatResponse("A cat on", "length"), false)
    assertEquals("A cat on", response.text())
    assertEquals(DataFieldUtils.FINISH_REASON_STOP, response.finishReason())
  }

  @Test
  fun answerInPartsIsJoined() {
    val parts =
      JSONArray()
        .put(JSONObject().put("type", "text").put("text", "A cat"))
        .put(JSONObject().put("type", "text").put("text", " on a sofa."))
    assertEquals("A cat on a sofa.", AiService.parseChatResponse(chatResponse(parts), false).text())
  }

  @Test
  fun jsonAnswerLosesItsFence() {
    val response =
      AiService.parseChatResponse(chatResponse("```json\n{\"a\": 1}\n```"), wantsJson = true)
    assertEquals("{\"a\": 1}", response.text())
    assertTrue(AiService.wantsJson(geminiRequest(json = true)))
    assertFalse(AiService.wantsJson(geminiRequest()))
  }

  @Test
  fun emptyOrFilteredAnswerIsNotAStop() {
    val empty = AiService.parseChatResponse(chatResponse(" "), false)
    assertNull(empty.text())
    assertEquals(AiService.FINISH_EMPTY, empty.finishReason())
    val filtered = AiService.parseChatResponse(chatResponse("", "content_filter"), false)
    assertEquals("CONTENT_FILTER", filtered.finishReason())
  }
}
