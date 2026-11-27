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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import com.google.android.accessibility.talkback.sandflyai.SandflyAi
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import org.json.JSONException
import org.json.JSONObject

/** Sends Gemini requests to the [AiService] the user set up, as chat requests. */
internal class AiServiceRequestPerformer(context: Context) {
  private val appContext = context.applicationContext
  private val requestQueue: RequestQueue by lazy { GeminiRestRequestPerformer.sharedQueue(appContext) }
  private val mainHandler = Handler(Looper.getMainLooper())
  private val retryToken = Any()

  /**
   * Reports a failure found before sending, after [performRequest] returns, as Volley reports its
   * failures. The caller starts the progress tone after the request is made, so a failure reported
   * during the call would stop the tone before it starts.
   */
  private fun failLater(callback: GeminiRestResponseCallback, failure: GeminiFailure) {
    mainHandler.post { callback.onFailure(failure) }
  }

  fun performRequest(
    address: String,
    model: String,
    apiKey: String,
    geminiRequest: JSONObject,
    callback: GeminiRestResponseCallback,
  ) {
    if (!AiService.isValidAddress(address)) {
      failLater(callback, GeminiFailure.other("The AI service address must start with https"))
      return
    }
    val sandflyPro =
      AiProvider.active(SharedPreferencesUtils.getSharedPreferences(appContext)) == AiProvider.SANDFLY_PRO
    if (sandflyPro) {
      SandflyAi.features?.aiServiceUnavailable(appContext)?.let {
        failLater(callback, GeminiFailure.spoken(it))
        return
      }
    }
    val chatRequest =
      try {
        AiService.toChatRequest(geminiRequest, model)
      } catch (e: JSONException) {
        failLater(callback, GeminiFailure.other("Could not make the request: $e"))
        return
      }
    val wantsJson = AiService.wantsJson(geminiRequest)
    send(address, model, apiKey, chatRequest, wantsJson, sandflyPro, callback, 0, SystemClock.uptimeMillis())
  }

  private fun send(
    address: String,
    model: String,
    apiKey: String,
    chatRequest: JSONObject,
    wantsJson: Boolean,
    sandflyPro: Boolean,
    callback: GeminiRestResponseCallback,
    round: Int,
    firstAt: Long,
  ) {
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
          { error ->
            val failure = failure(error, sandflyPro)
            val now = SystemClock.uptimeMillis()
            val elapsedMs = maxOf(0L, now - firstAt)
            if (
              !sandflyPro &&
                failure.kind == GeminiFailure.Kind.BUSY &&
                round < BUSY_RETRY_DELAYS_MS.size &&
                elapsedMs < RETRY_WINDOW_MS
            ) {
              val retryAfterMs = GeminiFailure.parseRetryAfterMs(error.networkResponse?.headers)
              val delayMs =
                minOf(
                  maxOf(BUSY_RETRY_DELAYS_MS[round], retryAfterMs),
                  maxOf(0L, RETRY_WINDOW_MS - elapsedMs),
                )
              LogUtils.w(TAG, "AI service busy, retrying in %d ms: %s", delayMs, failure.logMessage)
              mainHandler.postAtTime(
                { send(address, model, apiKey, chatRequest, wantsJson, false, callback, round + 1, firstAt) },
                retryToken,
                now + delayMs,
              )
            } else {
              callback.onFailure(failure)
            }
          },
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

  fun cancel() {
    mainHandler.removeCallbacksAndMessages(retryToken)
  }

  /**
   * The failure of a request. SandFly Pro's refusals (HTTP 402) carry a message written for the user,
   * such as that the code ended, and make the module check the licence again.
   */
  private fun failure(error: VolleyError, sandflyPro: Boolean): GeminiFailure {
    val failure = GeminiFailure.fromVolleyError(error).fromAiService()
    if (!sandflyPro || failure.statusCode != HTTP_PAYMENT_REQUIRED || failure.message.isEmpty()) {
      return failure
    }
    SandflyAi.features?.onAiServiceRefused(appContext)
    return GeminiFailure.spoken(failure.message, failure.statusCode)
  }

  private companion object {
    const val TAG = "AiServiceRequest"
    const val HTTP_PAYMENT_REQUIRED = 402
    // Services that run large models on demand can take longer than Gemini to answer.
    const val TIMEOUT_MS = 60_000
    // Retrying a request that timed out sends and pays for the same slow request again.
    const val MAX_RETRIES = 0
    val BUSY_RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)
    const val RETRY_WINDOW_MS = 24_000L
  }
}
