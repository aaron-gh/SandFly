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

package com.google.android.accessibility.talkback.translation

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A translation, and the languages it was from, when the service said, and into. */
data class Translation(val text: String, val from: String?, val to: String)

/** Why a translation failed. */
data class TranslationFailure(val kind: Kind, val detail: String = "") {
  enum class Kind {
    /** The service could not be reached. */
    CONNECTION,
    API_KEY,
    QUOTA,
    /** The language isn't one the service translates. */
    LANGUAGE,
    /** The language of the text could not be detected. */
    NOT_DETECTED,
    /** The on-device language model is downloading. */
    DOWNLOADING,
    /** Language identification is downloading, as Google Play services does on first use. */
    DETECTION_DOWNLOADING,
    /** The service is not set up, such as an AI service with none in use. */
    NOT_SET_UP,
    OTHER,
  }
}

/**
 * Requests to the translation services' web APIs, and their answers. The requests are built and
 * read here, apart from sending, so that they can be tested.
 */
object TranslationApis {
  /** Longer text is cut, which keeps requests within every service's limit. */
  const val MAX_TEXT = 5_000
  private const val TIMEOUT_MS = 15_000
  private const val MAX_RESPONSE_BYTES = 1024 * 1024

  private const val GOOGLE_CLOUD_URL = "https://translation.googleapis.com/language/translate/v2"
  private const val DEEPL_URL = "https://api.deepl.com/v2/translate"
  private const val DEEPL_FREE_URL = "https://api-free.deepl.com/v2/translate"
  private const val MICROSOFT_URL = "https://api.cognitive.microsofttranslator.com/translate"

  /** Countries whose English is British rather than American, for DeepL, which needs one. */
  private val BRITISH_ENGLISH = setOf("GB", "IE", "AU", "NZ", "ZA", "IN", "PK", "NG", "KE", "SG", "MT")

  data class Request(val url: String, val headers: Map<String, String>, val body: String)

  fun googleCloud(apiKey: String, text: String, from: String?, to: String): Request {
    val body = JSONObject().put("q", text).put("target", googleCode(to)).put("format", "text")
    if (from != null) body.put("source", googleCode(from))
    // Sent in a header rather than the address, which the system logs.
    return Request(GOOGLE_CLOUD_URL, mapOf("X-Goog-Api-Key" to apiKey), body.toString())
  }

  @Throws(JSONException::class)
  fun parseGoogleCloud(body: String, to: String): Translation {
    val translation = JSONObject(body).getJSONObject("data").getJSONArray("translations").getJSONObject(0)
    return Translation(
      translation.getString("translatedText"),
      TranslationLanguages.normalize(translation.optString("detectedSourceLanguage")),
      to,
    )
  }

  fun deepL(apiKey: String, text: String, from: String?, to: String, locale: Locale): Request {
    val body = JSONObject().put("text", JSONArray().put(text)).put("target_lang", deepLTarget(to, locale))
    if (from != null) body.put("source_lang", deepLSource(from))
    // Keys of free accounts end in ":fx" and work only with the free API.
    val url = if (apiKey.endsWith(":fx")) DEEPL_FREE_URL else DEEPL_URL
    return Request(url, mapOf("Authorization" to "DeepL-Auth-Key $apiKey"), body.toString())
  }

  @Throws(JSONException::class)
  fun parseDeepL(body: String, to: String): Translation {
    val translation = JSONObject(body).getJSONArray("translations").getJSONObject(0)
    return Translation(
      translation.getString("text"),
      TranslationLanguages.normalize(translation.optString("detected_source_language")),
      to,
    )
  }

  fun microsoft(apiKey: String, region: String, text: String, from: String?, to: String): Request {
    var url = "$MICROSOFT_URL?api-version=3.0&to=${microsoftCode(to)}"
    if (from != null) url += "&from=${microsoftCode(from)}"
    val headers = buildMap {
      put("Ocp-Apim-Subscription-Key", apiKey)
      if (region.isNotEmpty()) put("Ocp-Apim-Subscription-Region", region)
    }
    return Request(url, headers, JSONArray().put(JSONObject().put("Text", text)).toString())
  }

  @Throws(JSONException::class)
  fun parseMicrosoft(body: String, to: String): Translation {
    val result = JSONArray(body).getJSONObject(0)
    return Translation(
      result.getJSONArray("translations").getJSONObject(0).getString("text"),
      TranslationLanguages.normalize(result.optJSONObject("detectedLanguage")?.optString("language")),
      to,
    )
  }

  fun libreTranslate(address: String, apiKey: String, text: String, from: String?, to: String): Request {
    val body =
      JSONObject()
        .put("q", text)
        .put("source", from?.let { libreCode(it) } ?: "auto")
        .put("target", libreCode(to))
        .put("format", "text")
    if (apiKey.isNotEmpty()) body.put("api_key", apiKey)
    return Request(address.trim().trimEnd('/') + "/translate", emptyMap(), body.toString())
  }

  @Throws(JSONException::class)
  fun parseLibreTranslate(body: String, to: String): Translation {
    val json = JSONObject(body)
    return Translation(
      json.getString("translatedText"),
      TranslationLanguages.normalize(json.optJSONObject("detectedLanguage")?.optString("language")),
      to,
    )
  }

  /** The failure for an HTTP [status] other than 200, with the service's own message when it gave one. */
  fun failure(status: Int, body: String): TranslationFailure {
    val message = errorMessage(body)
    val kind =
      when (status) {
        401, 403 -> TranslationFailure.Kind.API_KEY
        // DeepL answers 456 when the month's characters are used up.
        429, 456 -> TranslationFailure.Kind.QUOTA
        else -> TranslationFailure.Kind.OTHER
      }
    return TranslationFailure(kind, message.ifEmpty { "HTTP $status" })
  }

  /** The message in an error answer: {"error": {"message"}}, {"message"} or {"error": "..."}. */
  fun errorMessage(body: String): String =
    try {
      val json = JSONObject(body)
      val error = json.opt("error")
      when {
        error is JSONObject -> error.optString("message")
        error is String -> error
        else -> json.optString("message")
      }.trim().take(200)
    } catch (e: JSONException) {
      ""
    }

  /** Sends [request] as JSON, and returns the HTTP status and the answer. */
  @Throws(IOException::class)
  fun send(request: Request): Pair<Int, String> {
    val connection = URL(request.url).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      connection.setRequestProperty("Accept", "application/json")
      for ((name, value) in request.headers) connection.setRequestProperty(name, value)
      connection.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val body = stream?.use { read(it) }.orEmpty()
      return status to body
    } finally {
      connection.disconnect()
    }
  }

  private fun read(input: InputStream): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
      val count = input.read(buffer)
      if (count < 0) break
      out.write(buffer, 0, count)
      if (out.size() > MAX_RESPONSE_BYTES) throw IOException("The answer is too long")
    }
    return out.toString("UTF-8")
  }

  fun googleCode(code: String): String =
    when (code) {
      TranslationLanguages.CHINESE_SIMPLIFIED -> "zh-CN"
      TranslationLanguages.CHINESE_TRADITIONAL -> "zh-TW"
      "mni" -> "mni-Mtei"
      else -> code
    }

  fun deepLTarget(code: String, locale: Locale): String =
    when (code) {
      // DeepL has no plain English or Portuguese to translate into.
      "en" -> if (locale.country in BRITISH_ENGLISH) "EN-GB" else "EN-US"
      "pt" -> if (locale.country == "BR" || locale.country.isEmpty()) "PT-BR" else "PT-PT"
      TranslationLanguages.CHINESE_SIMPLIFIED -> "ZH-HANS"
      TranslationLanguages.CHINESE_TRADITIONAL -> "ZH-HANT"
      else -> code.uppercase(Locale.ROOT)
    }

  fun deepLSource(code: String): String = code.substringBefore('-').uppercase(Locale.ROOT)

  fun microsoftCode(code: String): String =
    when (code) {
      "no" -> "nb"
      // Microsoft calls Central Kurdish "ku" and Northern Kurdish, Google's "ku", "kmr".
      "ckb" -> "ku"
      "ku" -> "kmr"
      else -> code
    }

  fun libreCode(code: String): String =
    when (code) {
      "fil" -> "tl"
      "no" -> "nb"
      else -> code
    }
}
