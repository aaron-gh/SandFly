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

import android.util.Base64
import android.util.Base64OutputStream
import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The HTTP requests of dictation: transcription by Gemini 3.5 Transcribe, the model behind Gboard's
 * Rambler, and correction by the AI service chosen for dictation. Each call blocks, so call them
 * off the main thread.
 */
internal object DictationRequests {
  private const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
  private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
  private const val RAMBLER_MODEL = "gemini-3.5-transcribe"
  private const val API_KEY_HEADER = "x-goog-api-key"
  private const val CONNECT_TIMEOUT_MS = 15_000
  private const val READ_TIMEOUT_MS = 120_000
  private const val MAX_ANSWER_BYTES = 1024 * 1024
  private const val AUDIO_PLACEHOLDER = "<audio>"

  /**
   * The text spoken in [wav], in smart mode: without filler words, with self-corrections resolved,
   * punctuated and formatted, as Rambler types it.
   */
  @Throws(IOException::class)
  fun transcribe(apiKey: String, wav: ByteArray, url: String = INTERACTIONS_URL): String {
    // The audio goes into "data" as it is sent, so that its text form, larger than the recording,
    // is never held in memory whole.
    val body =
      JSONObject()
        .put("model", RAMBLER_MODEL)
        // Not kept as an interaction to look up later. On the free tier, Google may still use the
        // audio and text to improve its products; on the paid tier it doesn't.
        .put("store", false)
        .put(
          "input",
          JSONArray()
            .put(
              JSONObject()
                .put("type", "audio")
                .put("mime_type", "audio/wav")
                .put("data", AUDIO_PLACEHOLDER)
            ),
        )
        .put(
          "generation_config",
          JSONObject().put("transcription_config", JSONObject().put("mode", "smart")),
        )
        .toString()
    val (before, after) = body.split(AUDIO_PLACEHOLDER, limit = 2).let { it[0] to it[1] }
    val prefix = before.toByteArray(Charsets.UTF_8)
    val suffix = after.toByteArray(Charsets.UTF_8)
    val audioLength = (wav.size + 2L) / 3 * 4
    val answer =
      post(url, mapOf(API_KEY_HEADER to apiKey), prefix.size + audioLength + suffix.size) { output ->
        output.write(prefix)
        val base64 = Base64OutputStream(NonClosing(output), Base64.NO_WRAP)
        base64.write(wav)
        base64.close()
        output.write(suffix)
      }
    return try {
      interactionText(answer)
    } catch (e: JSONException) {
      throw IOException("Could not read the transcription: $e")
    }
  }

  /** Asks Gemini [model] for an answer to [prompt]. */
  @Throws(IOException::class)
  fun askGemini(apiKey: String, model: String, prompt: String, base: String = GEMINI_URL): String {
    val body =
      JSONObject()
        .put(
          "contents",
          JSONArray()
            .put(
              JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ),
        )
    val answer = post("$base$model:generateContent", mapOf(API_KEY_HEADER to apiKey), body)
    return try {
      val parts =
        answer.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
      (0 until parts.length())
        .mapNotNull { parts.getJSONObject(it).optString("text").ifEmpty { null } }
        .joinToString("")
    } catch (e: JSONException) {
      throw IOException("Could not read the answer: $e")
    }
  }

  /** Asks [model] of the OpenAI-compatible chat API at [chatUrl] for an answer to [prompt]. */
  @Throws(IOException::class)
  fun askChat(chatUrl: String, apiKey: String, model: String, prompt: String): String {
    val body =
      JSONObject()
        .put("model", model)
        .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
    val answer = post(chatUrl, mapOf("Authorization" to "Bearer $apiKey"), body)
    return try {
      val content = answer.getJSONArray("choices").getJSONObject(0).getJSONObject("message").opt("content")
      when (content) {
        is String -> content
        is JSONArray ->
          (0 until content.length())
            .mapNotNull { content.optJSONObject(it)?.optString("text")?.ifEmpty { null } }
            .joinToString("")
        else -> ""
      }
    } catch (e: JSONException) {
      throw IOException("Could not read the answer: $e")
    }
  }

  /**
   * The text of an Interactions API answer: the text of its model output steps, or of its outputs
   * in earlier versions of the API.
   */
  @Throws(JSONException::class)
  fun interactionText(answer: JSONObject): String {
    val text = StringBuilder()
    answer.optJSONArray("steps")?.let { steps ->
      for (i in 0 until steps.length()) {
        val step = steps.getJSONObject(i)
        if (step.optString("type") != "model_output") continue
        appendTexts(step.optJSONArray("content"), text)
      }
    }
    if (text.isEmpty()) appendTexts(answer.optJSONArray("outputs"), text)
    if (text.isEmpty()) text.append(answer.optString("output_text"))
    return text.toString().trim()
  }

  private fun appendTexts(parts: JSONArray?, text: StringBuilder) {
    if (parts == null) return
    for (i in 0 until parts.length()) {
      val part = parts.optJSONObject(i) ?: continue
      if (part.optString("type", "text") == "text") text.append(part.optString("text"))
    }
  }

  @Throws(IOException::class)
  private fun post(url: String, headers: Map<String, String>, body: JSONObject): JSONObject {
    val bytes = body.toString().toByteArray(Charsets.UTF_8)
    return post(url, headers, bytes.size.toLong()) { it.write(bytes) }
  }

  @Throws(IOException::class)
  private fun post(
    url: String,
    headers: Map<String, String>,
    length: Long,
    writeBody: (OutputStream) -> Unit,
  ): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = CONNECT_TIMEOUT_MS
      connection.readTimeout = READ_TIMEOUT_MS
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setFixedLengthStreamingMode(length)
      connection.setRequestProperty("Content-Type", "application/json")
      headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
      connection.outputStream.use(writeBody)
      val code = connection.responseCode
      if (code != HttpURLConnection.HTTP_OK) throw HttpException(code)
      val answer = connection.inputStream.use { input -> readLimited(input) }
      return try {
        JSONObject(String(answer, Charsets.UTF_8))
      } catch (e: JSONException) {
        throw IOException("Not JSON: $e")
      }
    } finally {
      connection.disconnect()
    }
  }

  /** Passes writes to [output], but leaves it open when closed. */
  private class NonClosing(output: OutputStream) : FilterOutputStream(output) {
    override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)

    override fun close() = flush()
  }

  private fun readLimited(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
      val read = input.read(buffer)
      if (read < 0) break
      out.write(buffer, 0, read)
      if (out.size() > MAX_ANSWER_BYTES) throw IOException("The answer is too long")
    }
    return out.toByteArray()
  }

  /** The service answered with HTTP status [code]. */
  class HttpException(val code: Int) : IOException("HTTP $code")
}
