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

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.talkback.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/**
 * The Gemini models that Describe image and Describe screen ask, in order. The user chooses one in
 * Automatic descriptions settings, or leaves it on Automatic. When a model is busy or its usage
 * limit is reached, the next one is asked, so a request still gets an answer when Gemini Flash is
 * overloaded, as it often is.
 */
object GeminiModel {
  const val PREF_KEY = "pref_gemini_model"

  /** The model asked when the others are busy, which is much less in demand. */
  const val FALLBACK = "gemini-flash-lite-latest"

  private const val LIST_URL = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200"
  private const val API_KEY_HEADER = "x-goog-api-key"
  private const val TIMEOUT_MS = 15_000
  private const val MAX_LIST_BYTES = 1024 * 1024
  private const val MAX_MODELS = 100

  /** Models for other uses than describing, which can't answer with text about an image. */
  private val NOT_FOR_IMAGES =
    listOf("tts", "image", "live", "embedding", "audio", "transcribe", "robotics", "banana", "veo", "lyria")

  /** A model that can describe images, by its id and Google's name for it. */
  data class Choice(val id: String, val name: String)

  /** The model the user chose, or an empty string for Automatic. */
  fun chosen(prefs: SharedPreferences): String = prefs.getString(PREF_KEY, null)?.trim().orEmpty()

  fun setChosen(prefs: SharedPreferences, id: String) =
    prefs.edit { if (id.isBlank()) remove(PREF_KEY) else putString(PREF_KEY, id.trim()) }

  /** The models to ask, in order: the chosen one or the built-in one, then [FALLBACK]. */
  @JvmStatic
  fun chain(prefs: SharedPreferences): List<String> =
    listOf(chosen(prefs).ifEmpty { BuildConfig.GEMINI_MODEL }, FALLBACK).distinct()

  /**
   * The models [apiKey] can use to describe images, newest first as Google lists them. Throws
   * [IOException] with the reason when Google can't be asked.
   */
  @Throws(IOException::class)
  fun list(apiKey: String): List<Choice> {
    val connection = URL(LIST_URL).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      connection.setRequestProperty(API_KEY_HEADER, apiKey)
      val code = connection.responseCode
      if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
      val body =
        connection.inputStream.use { input ->
          val bytes = input.readNBytesCompat(MAX_LIST_BYTES + 1)
          if (bytes.size > MAX_LIST_BYTES) throw IOException("The list is too long")
          bytes.decodeToString()
        }
      return parse(body)
    } finally {
      connection.disconnect()
    }
  }

  /** The models in Google's list that describe images. */
  @Throws(IOException::class)
  fun parse(body: String): List<Choice> =
    try {
      val models = JSONObject(body).optJSONArray("models") ?: return emptyList()
      (0 until models.length())
        .mapNotNull { models.optJSONObject(it) }
        .filter { model ->
          val methods = model.optJSONArray("supportedGenerationMethods")
          methods != null && (0 until methods.length()).any { methods.optString(it) == "generateContent" }
        }
        .mapNotNull { model ->
          val id = model.optString("name").removePrefix("models/")
          if (!id.startsWith("gemini") || NOT_FOR_IMAGES.any { it in id }) return@mapNotNull null
          Choice(id, model.optString("displayName").ifBlank { id })
        }
        .distinctBy { it.id }
        .take(MAX_MODELS)
    } catch (e: JSONException) {
      throw IOException("Google's list could not be read")
    }

  private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < limit) {
      val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
      if (read < 0) break
      out.write(buffer, 0, read)
    }
    return out.toByteArray()
  }
}
