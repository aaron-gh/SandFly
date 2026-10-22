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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/** The models an AI service with an OpenAI-compatible API offers, from its list of models. */
object AiModels {
  private const val TIMEOUT_MS = 15_000
  private const val MAX_LIST_BYTES = 4 * 1024 * 1024
  private const val MAX_MODELS = 500
  private const val ANTHROPIC_VERSION = "2023-06-01"

  /** Models for other uses than chat, which can't describe images. */
  private val NOT_FOR_CHAT =
    listOf("embed", "whisper", "tts", "dall-e", "moderation", "davinci", "babbage", "transcribe", "rerank")

  /**
   * The models of [provider] at [address], with [apiKey]. Throws [IOException] with the reason when
   * the service can't be asked.
   */
  @Throws(IOException::class)
  fun list(provider: AiProvider, address: String, apiKey: String): List<GeminiModel.Choice> {
    val connection = URL(address.trim().trimEnd('/') + "/models").openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      if (apiKey.isNotEmpty()) {
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        // Claude's own list of models takes its key in this header.
        if (provider == AiProvider.CLAUDE) connection.setRequestProperty("x-api-key", apiKey)
      }
      if (provider == AiProvider.CLAUDE) {
        connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
      }
      val code = connection.responseCode
      if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
      val body =
        connection.inputStream.use { input ->
          val out = ByteArrayOutputStream()
          val buffer = ByteArray(8192)
          while (out.size() <= MAX_LIST_BYTES) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
          }
          if (out.size() > MAX_LIST_BYTES) throw IOException("The list is too long")
          out.toByteArray().decodeToString()
        }
      return parse(body)
    } finally {
      connection.disconnect()
    }
  }

  /** The chat models in a list of models, `{"data": [{"id": ...}]}`, as these services give it. */
  @Throws(IOException::class)
  fun parse(body: String): List<GeminiModel.Choice> =
    try {
      val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
      (0 until data.length())
        .mapNotNull { data.optJSONObject(it) }
        .mapNotNull { model ->
          val id = model.optString("id")
          if (id.isEmpty() || NOT_FOR_CHAT.any { it in id.lowercase() }) return@mapNotNull null
          val name = model.optString("display_name").ifBlank { model.optString("name") }.ifBlank { id }
          GeminiModel.Choice(id, name)
        }
        .distinctBy { it.id }
        .take(MAX_MODELS)
    } catch (e: JSONException) {
      throw IOException("The list could not be read")
    }
}
