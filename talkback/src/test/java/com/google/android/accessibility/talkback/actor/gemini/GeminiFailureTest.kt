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

import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.google.android.accessibility.talkback.actor.gemini.GeminiFailure.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeminiFailureTest {

  private fun body(code: Int, status: String, message: String, reason: String? = null): String {
    val details = if (reason == null) "" else """, "details": [{"reason": "$reason"}]"""
    return """{"error": {"code": $code, "message": "$message", "status": "$status"$details}}"""
  }

  @Test
  fun invalidKeyIsAnApiKeyError() {
    val failure =
      GeminiFailure.fromResponse(
        400,
        body(
          400,
          "INVALID_ARGUMENT",
          "API key not valid. Please pass a valid API key.",
          "API_KEY_INVALID",
        ),
      )
    assertEquals(Kind.API_KEY, failure.kind)
    assertEquals(400, failure.statusCode)
    assertEquals("INVALID_ARGUMENT", failure.status)
    assertEquals("API key not valid. Please pass a valid API key.", failure.message)
  }

  @Test
  fun expiredKeyWithoutDetailsIsAnApiKeyError() {
    val failure =
      GeminiFailure.fromResponse(
        400,
        body(400, "INVALID_ARGUMENT", "API key expired. Please renew the API key."),
      )
    assertEquals(Kind.API_KEY, failure.kind)
  }

  @Test
  fun forbiddenIsAnApiKeyError() {
    val failure =
      GeminiFailure.fromResponse(
        403,
        body(403, "PERMISSION_DENIED", "Your API key was reported as leaked."),
      )
    assertEquals(Kind.API_KEY, failure.kind)
  }

  @Test
  fun googlesRealAnswerToAWrongKeyIsAnApiKeyError() {
    // What Google answered on 2026-10-03 to a made-up key.
    val body =
      """
      {
        "error": {
          "code": 400,
          "message": "API key not valid. Please pass a valid API key.",
          "status": "INVALID_ARGUMENT",
          "details": [
            {
              "@type": "type.googleapis.com/google.rpc.ErrorInfo",
              "reason": "API_KEY_INVALID",
              "domain": "googleapis.com",
              "metadata": {"service": "generativelanguage.googleapis.com"}
            },
            {
              "@type": "type.googleapis.com/google.rpc.LocalizedMessage",
              "locale": "en-US",
              "message": "API key not valid. Please pass a valid API key."
            }
          ]
        }
      }
      """
    assertEquals(Kind.API_KEY, GeminiFailure.fromResponse(400, body).kind)
  }

  @Test
  fun googlesRealAnswerToNoKeyIsAnApiKeyError() {
    // What Google answered on 2026-10-03 to a request without a key.
    val body =
      """
      {
        "error": {
          "code": 403,
          "message": "Method doesn't allow unregistered callers (callers without established identity). Please use API Key or other form of API consumer identity to call this API.",
          "status": "PERMISSION_DENIED"
        }
      }
      """
    assertEquals(Kind.API_KEY, GeminiFailure.fromResponse(403, body).kind)
  }

  @Test
  fun tooManyRequestsIsTheUsageLimit() {
    val failure =
      GeminiFailure.fromResponse(429, body(429, "RESOURCE_EXHAUSTED", "You exceeded your quota."))
    assertEquals(Kind.QUOTA, failure.kind)
  }

  @Test
  fun overloadedAndInternalErrorsMeanBusy() {
    assertEquals(
      Kind.BUSY,
      GeminiFailure.fromResponse(503, body(503, "UNAVAILABLE", "The model is overloaded.")).kind,
    )
    assertEquals(
      Kind.BUSY,
      GeminiFailure.fromResponse(500, body(500, "INTERNAL", "An internal error occurred.")).kind,
    )
  }

  @Test
  fun deadlineExceededIsATimeout() {
    assertEquals(
      Kind.TIMEOUT,
      GeminiFailure.fromResponse(504, body(504, "DEADLINE_EXCEEDED", "Deadline expired.")).kind,
    )
  }

  @Test
  fun otherBadRequestKeepsGooglesMessage() {
    val failure =
      GeminiFailure.fromResponse(
        400,
        body(400, "FAILED_PRECONDITION", "User location is not supported for the API use."),
      )
    assertEquals(Kind.OTHER, failure.kind)
    assertEquals("User location is not supported for the API use.", failure.message)
  }

  @Test
  fun errorInAListIsRead() {
    val list = "[" + body(429, "RESOURCE_EXHAUSTED", "Quota exceeded.") + "]"
    val failure = GeminiFailure.fromResponse(429, list)
    assertEquals(Kind.QUOTA, failure.kind)
    assertEquals("RESOURCE_EXHAUSTED", failure.status)
    assertEquals("Quota exceeded.", failure.message)
  }

  @Test
  fun bodyThatIsNotJsonBecomesTheMessage() {
    val failure = GeminiFailure.fromResponse(502, "Bad Gateway")
    assertEquals(Kind.BUSY, failure.kind)
    assertEquals("", failure.status)
    assertEquals("Bad Gateway", failure.message)
  }

  @Test
  fun volleyErrorWithAnAnswerIsReadFromTheAnswer() {
    val data = body(429, "RESOURCE_EXHAUSTED", "Quota exceeded.").toByteArray()
    val response = NetworkResponse(429, data, false, 0L, emptyList())
    assertEquals(Kind.QUOTA, GeminiFailure.fromVolleyError(ClientError(response)).kind)
    val busy = NetworkResponse(503, ByteArray(0), false, 0L, emptyList())
    assertEquals(Kind.BUSY, GeminiFailure.fromVolleyError(ServerError(busy)).kind)
  }

  @Test
  fun volleyTimeoutAndNoConnection() {
    assertEquals(Kind.TIMEOUT, GeminiFailure.fromVolleyError(TimeoutError()).kind)
    assertEquals(Kind.NO_CONNECTION, GeminiFailure.fromVolleyError(NoConnectionError()).kind)
  }

  @Test
  fun logMessageHasEverything() {
    val failure =
      GeminiFailure.fromResponse(429, body(429, "RESOURCE_EXHAUSTED", "You exceeded your quota."))
    assertEquals("QUOTA, HTTP 429 RESOURCE_EXHAUSTED: You exceeded your quota.", failure.logMessage)
    assertEquals("OTHER: broken", GeminiFailure.other("broken").logMessage)
  }

  @Test
  fun firstSentenceIsEnoughToSpeak() {
    assertEquals(
      "You exceeded your current quota, please check your plan and billing details.",
      GeminiFailure.firstSentence(
        "You exceeded your current quota, please check your plan and billing details. " +
          "For more information on this error, head to: https://ai.google.dev/gemini-api/docs\n" +
          "* Quota exceeded for metric: generate_content_free_tier_requests"
      ),
    )
    assertEquals(
      "models/gemini-x is not found for API version v1beta.",
      GeminiFailure.firstSentence("models/gemini-x is not found for API version v1beta."),
    )
    val long = GeminiFailure.firstSentence("x".repeat(500))
    assertEquals(201, long.length)
    assertFalse(GeminiFailure.firstSentence("").isNotEmpty())
  }
}
