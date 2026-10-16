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

import android.content.Context
import com.android.volley.NetworkError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.google.android.accessibility.talkback.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Why a request to Gemini failed: what went wrong, the HTTP status, and Google's own message. It
 * gives a full description for the log and a short one for the user, so that the user hears whether
 * the key, the usage limit, Google's servers or the connection is to blame.
 */
class GeminiFailure(
  val kind: Kind,
  /** The HTTP status Google answered with, or 0 when there was no answer. */
  val statusCode: Int = 0,
  /** Google's status name, such as RESOURCE_EXHAUSTED, or empty. */
  val status: String = "",
  /** Google's explanation, or what failed on the phone. Never contains the API key. */
  val message: String = "",
) {
  enum class Kind {
    /** The phone could not reach Google. */
    NO_CONNECTION,
    /** Google did not answer in time. */
    TIMEOUT,
    /** Google rejected the API key: wrong, expired, restricted or blocked. */
    API_KEY,
    /** The key's usage limit was reached. */
    QUOTA,
    /** Google's servers are overloaded or had an internal error. */
    BUSY,
    /** Anything else. */
    OTHER,
  }

  /** A full description for the log. */
  val logMessage: String
    get() =
      buildString {
        append(kind)
        if (statusCode > 0) append(", HTTP ").append(statusCode)
        if (status.isNotEmpty()) append(' ').append(status)
        if (message.isNotEmpty()) append(": ").append(message)
      }

  /** What to tell the user. */
  fun userMessage(context: Context): String =
    when (kind) {
      Kind.NO_CONNECTION -> context.getString(R.string.gemini_network_error)
      Kind.TIMEOUT -> context.getString(R.string.gemini_error_timeout)
      Kind.API_KEY -> context.getString(R.string.gemini_error_api_key)
      Kind.QUOTA -> context.getString(R.string.gemini_error_quota)
      Kind.BUSY -> context.getString(R.string.gemini_error_busy)
      Kind.OTHER -> {
        val shortMessage = firstSentence(message)
        when {
          statusCode < 400 -> context.getString(R.string.gemini_error_message)
          shortMessage.isEmpty() ->
            context.getString(R.string.gemini_error_with_code, statusCode)
          else -> context.getString(R.string.gemini_error_with_details, statusCode, shortMessage)
        }
      }
    }

  override fun toString(): String = logMessage

  companion object {
    private const val MAX_SPOKEN_DETAILS = 200

    /** A failure on the phone, such as an answer that could not be read. */
    @JvmStatic
    fun other(message: String): GeminiFailure = GeminiFailure(Kind.OTHER, message = message)

    /** Reads the failure from the error Volley reports for a request. */
    @JvmStatic
    fun fromVolleyError(error: VolleyError): GeminiFailure {
      val response = error.networkResponse
      if (response != null && response.statusCode >= 400) {
        return fromResponse(response.statusCode, response.data?.toString(Charsets.UTF_8))
      }
      val message = error.message ?: error.javaClass.simpleName
      return when (error) {
        is TimeoutError -> GeminiFailure(Kind.TIMEOUT, message = message)
        // Includes NoConnectionError: no answer came back from Google.
        is NetworkError -> GeminiFailure(Kind.NO_CONNECTION, message = message)
        else -> other(error.toString())
      }
    }

    /** Reads the failure from Google's HTTP status and the error body it sent. */
    @JvmStatic
    fun fromResponse(statusCode: Int, body: String?): GeminiFailure {
      val error = errorObject(body)
      val status = error?.optString("status").orEmpty()
      val message = error?.optString("message").orEmpty().ifEmpty { body?.trim().orEmpty() }
      val kind =
        when {
          isApiKeyError(statusCode, error, message) -> Kind.API_KEY
          statusCode == 429 -> Kind.QUOTA
          statusCode == 504 -> Kind.TIMEOUT
          statusCode >= 500 -> Kind.BUSY
          else -> Kind.OTHER
        }
      return GeminiFailure(kind, statusCode, status, message)
    }

    private fun isApiKeyError(statusCode: Int, error: JSONObject?, message: String): Boolean {
      if (statusCode == 401 || statusCode == 403) {
        return true
      }
      if (statusCode != 400) {
        return false
      }
      val details = error?.optJSONArray("details")
      if (details != null) {
        for (i in 0 until details.length()) {
          if (details.optJSONObject(i)?.optString("reason")?.startsWith("API_KEY") == true) {
            return true
          }
        }
      }
      return message.contains("API key", ignoreCase = true)
    }

    /** Google sends {"error": {...}}, and sometimes a list holding that object. */
    private fun errorObject(body: String?): JSONObject? {
      val text = body?.trim().orEmpty()
      return try {
        when {
          text.startsWith("{") -> JSONObject(text).optJSONObject("error")
          text.startsWith("[") -> JSONArray(text).optJSONObject(0)?.optJSONObject("error")
          else -> null
        }
      } catch (e: JSONException) {
        null
      }
    }

    /** The first line and sentence of Google's message, which is enough to say what happened. */
    @JvmStatic
    fun firstSentence(message: String): String {
      var text = message.trim().lineSequence().firstOrNull().orEmpty().trim()
      val end = text.indexOf(". ")
      if (end >= 0) {
        text = text.substring(0, end + 1)
      }
      return if (text.length > MAX_SPOKEN_DETAILS) text.take(MAX_SPOKEN_DETAILS).trimEnd() + "…"
      else text
    }
  }
}
