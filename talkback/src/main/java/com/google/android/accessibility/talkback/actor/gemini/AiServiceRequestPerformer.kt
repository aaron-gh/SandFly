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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import org.json.JSONException
import org.json.JSONObject

/** Sends Gemini requests to the [AiService] the user set up, as chat requests. */
internal class AiServiceRequestPerformer(context: Context) {
  private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

  fun performRequest(
    address: String,
    model: String,
    apiKey: String,
    geminiRequest: JSONObject,
    callback: GeminiRestResponseCallback,
  ) {
    if (!AiService.isValidAddress(address)) {
      callback.onFailure(GeminiFailure.other("The AI service address must start with https"))
      return
    }
    val chatRequest =
      try {
        AiService.toChatRequest(geminiRequest, model)
      } catch (e: JSONException) {
        callback.onFailure(GeminiFailure.other("Could not make the request: $e"))
        return
      }
    val wantsJson = AiService.wantsJson(geminiRequest)
    val request =
      object :
        JsonObjectRequest(
          Request.Method.POST,
          AiService.chatUrl(address),
          chatRequest,
          { response ->
            try {
              callback.onResponse(AiService.parseChatResponse(response, wantsJson))
            } catch (e: JSONException) {
              callback.onFailure(GeminiFailure.other("Could not read the answer: $e"))
            }
          },
          { error -> callback.onFailure(GeminiFailure.fromVolleyError(error).fromAiService()) },
        ) {
        override fun getHeaders(): MutableMap<String, String> {
          val headers = HashMap(super.getHeaders())
          // Sent in a header rather than the address, which Volley and the system log.
          if (apiKey.isNotEmpty()) headers["Authorization"] = "Bearer $apiKey"
          return headers
        }
      }
    request.retryPolicy =
      DefaultRetryPolicy(TIMEOUT_MS, MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
    requestQueue.add(request)
  }

  private companion object {
    // Services that run large models on demand can take longer than Gemini to answer.
    const val TIMEOUT_MS = 60_000
    // Retrying a request that timed out sends and pays for the same slow request again.
    const val MAX_RETRIES = 0
  }
}
