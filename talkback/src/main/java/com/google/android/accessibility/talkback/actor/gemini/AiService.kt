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

import android.content.SharedPreferences
import java.net.URI
import java.net.URISyntaxException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * An AI service with an OpenAI-compatible chat completions API, such as OpenRouter, Groq, Mistral,
 * Ollama or Claude, that Describe image and Describe screen use instead of Gemini when the user
 * sets one up in Automatic descriptions settings. Requests are built for Gemini, so they are turned
 * into chat requests, and the answers back into Gemini's.
 */
object AiService {
  const val PREF_ADDRESS = "pref_ai_service_address"
  const val PREF_MODEL = "pref_ai_service_model"
  // "api_key" in the name keeps it out of settings backups, as the Gemini key is.
  const val PREF_API_KEY = "pref_ai_service_api_key"

  private const val FINISH_STOP = "stop"
  private const val FINISH_LENGTH = "length"
  const val FINISH_EMPTY = "EMPTY"

  fun address(prefs: SharedPreferences): String = prefs.getString(PREF_ADDRESS, null)?.trim() ?: ""

  fun model(prefs: SharedPreferences): String = prefs.getString(PREF_MODEL, null)?.trim() ?: ""

  fun apiKey(prefs: SharedPreferences): String = prefs.getString(PREF_API_KEY, null)?.trim() ?: ""

  /** Saves the settings, removing each one that is blank. */
  fun save(prefs: SharedPreferences, address: String, model: String, apiKey: String) {
    val editor = prefs.edit()
    val values = listOf(PREF_ADDRESS to address, PREF_MODEL to model, PREF_API_KEY to apiKey)
    for ((key, value) in values) {
      val trimmed = value.trim()
      if (trimmed.isEmpty()) editor.remove(key) else editor.putString(key, trimmed)
    }
    editor.apply()
  }

  /** The host of the service's address, to name it to the user. */
  fun host(prefs: SharedPreferences): String =
    try {
      URI(address(prefs)).host ?: address(prefs)
    } catch (e: URISyntaxException) {
      address(prefs)
    }

  /** Whether requests go to the service: it has an address and a model. */
  @JvmStatic
  fun isSetUp(prefs: SharedPreferences): Boolean =
    address(prefs).isNotEmpty() && model(prefs).isNotEmpty()

  /** Whether [address] can be used: an https address with a host. */
  fun isValidAddress(address: String): Boolean =
    try {
      val uri = URI(address.trim())
      "https".equals(uri.scheme, ignoreCase = true) && !uri.host.isNullOrEmpty()
    } catch (e: URISyntaxException) {
      false
    }

  /** The chat completions address of the service at [address], such as .../v1/chat/completions. */
  fun chatUrl(address: String): String {
    val base = address.trim().trimEnd('/')
    return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
  }

  /**
   * Turns a Gemini request, a text part and an image part in one user message, into a chat request
   * for [model]. JSON answers are asked for in the prompt only, since not every service accepts a
   * response format, and the service's own sampling settings are used.
   */
  @Throws(JSONException::class)
  fun toChatRequest(geminiRequest: JSONObject, model: String): JSONObject {
    val content = JSONArray()
    val parts = geminiRequest.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
    for (i in 0 until parts.length()) {
      val part = parts.getJSONObject(i)
      if (part.has("text")) {
        content.put(JSONObject().put("type", "text").put("text", part.getString("text")))
      } else if (part.has("inlineData")) {
        val data = part.getJSONObject("inlineData")
        val url = "data:${data.optString("mimeType", "image/jpeg")};base64,${data.getString("data")}"
        content.put(
          JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", url))
        )
      }
    }
    // No temperature: Gemini's requests ask for 0, which recent Claude models refuse.
    return JSONObject()
      .put("model", model)
      .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
  }

  /** Whether the Gemini [request] asks for a JSON answer. */
  fun wantsJson(request: JSONObject): Boolean =
    request.optJSONObject("generationConfig")?.optString("responseMimeType") == "application/json"

  /**
   * Reads a chat answer as a Gemini answer. An answer cut off at the service's length limit is still
   * an answer. Returns a response without text when there was none.
   */
  @Throws(JSONException::class)
  fun parseChatResponse(response: JSONObject, wantsJson: Boolean): DataFieldUtils.GeminiResponse {
    val choice = response.getJSONArray("choices").getJSONObject(0)
    val message = choice.optJSONObject("message")
    val raw = message?.let { textOf(it.opt("content")) }.orEmpty()
    val text = if (wantsJson) LocalGemmaRunner.extractJson(raw) else raw.trim()
    val finish = choice.optString("finish_reason", FINISH_STOP)
    val builder = DataFieldUtils.GeminiResponse.builder()
    if (text.isNotEmpty()) builder.setText(text)
    val finished = finish == FINISH_STOP || finish == FINISH_LENGTH
    builder.setFinishReason(
      when {
        finished && text.isNotEmpty() -> DataFieldUtils.FINISH_REASON_STOP
        // Not STOP, so that an empty answer counts as none, as an empty one from Gemini does.
        finished -> FINISH_EMPTY
        else -> finish.uppercase()
      }
    )
    return builder.build()
  }

  // Content is a string, or with some services a list of parts.
  private fun textOf(content: Any?): String =
    when (content) {
      is String -> content
      is JSONArray ->
        (0 until content.length())
          .mapNotNull { content.optJSONObject(it)?.optString("text")?.ifEmpty { null } }
          .joinToString("")
      else -> ""
    }
}
