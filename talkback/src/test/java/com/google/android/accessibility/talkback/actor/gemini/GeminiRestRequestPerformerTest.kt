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

import android.os.Looper
import com.android.volley.ClientError
import com.android.volley.Header
import com.android.volley.Network
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.toolbox.NoCache
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import java.time.Duration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiRestRequestPerformerTest {
  private val postData =
    JSONObject().put("contents", "x").put("generationConfig", JSONObject().put("temperature", 0))

  private fun thinkingConfig(model: String): JSONObject? =
    GeminiRestRequestPerformer.withLeastThinking(postData, model)
      .optJSONObject("generationConfig")
      ?.optJSONObject("thinkingConfig")

  @Test
  fun latestAndNewerModelsGetTheMinimalThinkingLevel() {
    assertEquals("minimal", thinkingConfig("gemini-flash-latest")?.getString("thinkingLevel"))
    assertEquals("minimal", thinkingConfig("gemini-3.5-flash")?.getString("thinkingLevel"))
  }

  @Test
  fun gemini25GetsNoThinkingBudget() {
    assertEquals(0, thinkingConfig("gemini-2.5-flash")?.getInt("thinkingBudget"))
  }

  @Test
  fun olderModelsAreAskedAsBefore() {
    assertSame(postData, GeminiRestRequestPerformer.withLeastThinking(postData, "gemini-2.0-flash"))
  }

  @Test
  fun theRequestItselfIsNotChanged() {
    val copy = GeminiRestRequestPerformer.withLeastThinking(postData, "gemini-flash-latest")
    assertEquals(0, copy.getJSONObject("generationConfig").getInt("temperature"))
    assertFalse(postData.getJSONObject("generationConfig").has("thinkingConfig"))
  }

  @Test
  fun aRejectedThinkingSettingIsRecognized() {
    val rejected =
      GeminiFailure.fromResponse(
        400,
        """{"error":{"code":400,"status":"INVALID_ARGUMENT",""" +
          """"message":"Thinking level is not supported for this model."}}""",
      )
    assertTrue(GeminiRestRequestPerformer.rejectedThinkingSetting(rejected))
    val other =
      GeminiFailure.fromResponse(
        400,
        """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Bad image."}}""",
      )
    assertFalse(GeminiRestRequestPerformer.rejectedThinkingSetting(other))
  }

  @Test
  fun aModelWithAnotherAfterItIsGivenLessTime() {
    assertEquals(15_000, GeminiRestRequestPerformer.timeoutMs(true, 0))
    assertEquals(30_000, GeminiRestRequestPerformer.timeoutMs(false, 0))
    assertEquals(17_000, GeminiRestRequestPerformer.timeoutMs(false, 15_000))
    assertEquals(8_000, GeminiRestRequestPerformer.timeoutMs(false, 30_000))
  }

  @Test
  fun busyRetryDelayHonorsScheduleAndRetryAfter() {
    assertEquals(1_000L, GeminiRestRequestPerformer.busyRetryDelayMs(0, 0L))
    assertEquals(2_000L, GeminiRestRequestPerformer.busyRetryDelayMs(1, 500L))
    assertEquals(4_000L, GeminiRestRequestPerformer.busyRetryDelayMs(2, 0L))
    assertEquals(5_000L, GeminiRestRequestPerformer.busyRetryDelayMs(0, 5_000L))
  }

  @Test
  fun cancelDuringInFlightRequestStopsRetryChain() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    val responses = mutableListOf<GeminiResponse>()
    performer.performRequest(
      listOf(url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(responses, failures),
    )

    assertEquals(1, queue.requests.size)
    val inFlight = queue.requests.single()
    performer.cancelExistingRequestIfNeeded()
    assertTrue(inFlight.isCanceled)

    inFlight.deliverError(ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList())))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(1, queue.requests.size)
    assertTrue(failures.isEmpty())
    assertTrue(responses.isEmpty())
  }

  @Test
  fun nonBusyLastFailureDoesNotRetryChain() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      listOf(url("gemini-2.5-pro"), url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), failures),
    )

    assertEquals(1, queue.requests.size)
    queue.requests[0].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    assertEquals(2, queue.requests.size)

    val badRequest =
      """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Bad image."}}""".toByteArray()
    queue.requests[1].deliverError(
      ClientError(NetworkResponse(400, badRequest, false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15))

    assertEquals(2, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(400, failures.single().statusCode)
  }

  @Test
  fun busyRetriesRespectWindowAndKeepTimeoutInBudget() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      listOf(url("gemini-2.5-pro"), url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), failures),
    )

    // Fail model 0 quickly, then spend 18.5 s before model 1 fails with 503.
    queue.requests[0].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    assertEquals(2, queue.requests.size)

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(18_500))
    queue.requests[1].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    // Round 0 delay is 1_000 ms -> round 1 starts at 19_500 ms (inside 20 s window).
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(999))
    assertEquals(2, queue.requests.size)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
    assertEquals(3, queue.requests.size)
    // Model 0 with another after it gets remaining budget (32_000 - 19_500 = 12_500 <= 15_000).
    assertEquals(12_500, queue.requests[2].retryPolicy.currentTimeout)

    // Model 0 fails at 19_600 ms -> model 1 starts immediately, then fails past the window.
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    queue.requests[2].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    assertEquals(4, queue.requests.size)

    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
    queue.requests[3].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(4, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(GeminiFailure.Kind.BUSY, failures.single().kind)
  }

  @Test
  fun cancelDuringInFlightRequestDropsLateSuccess() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    val responses = mutableListOf<GeminiResponse>()
    performer.performRequest(
      listOf(url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(responses, failures),
    )

    val inFlight = queue.requests.single()
    performer.cancelExistingRequestIfNeeded()
    assertTrue(inFlight.isCanceled)

    ReflectionHelpers.callInstanceMethod<Any>(
      inFlight,
      "deliverResponse",
      ClassParameter.from(Any::class.java, geminiSuccessJson()),
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertTrue(responses.isEmpty())
    assertTrue(failures.isEmpty())
  }

  @Test
  fun cancelWhileRetryIsPostedDropsPendingRetry() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      listOf(url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), failures),
    )

    queue.requests.single().deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
    assertEquals(1, queue.requests.size)

    performer.cancelExistingRequestIfNeeded()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(1, queue.requests.size)
    assertTrue(failures.isEmpty())
  }

  @Test
  fun quotaWithoutBusyModelOrRetryAfterIsNotRetried() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val failures = mutableListOf<GeminiFailure>()
    val quotaBody =
      """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"You exceeded your quota."}}"""
        .toByteArray()

    performer.performRequest(
      listOf(url("gemini-2.5-pro"), url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), failures),
    )
    queue.requests[0].deliverError(
      ClientError(NetworkResponse(429, quotaBody, false, 0L, emptyList()))
    )
    assertEquals(2, queue.requests.size)
    queue.requests[1].deliverError(
      ClientError(NetworkResponse(429, quotaBody, false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(2, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(GeminiFailure.Kind.QUOTA, failures.single().kind)
  }

  @Test
  fun quotaIsRetriedWhenPrecededByBusyOrWithRetryAfter() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue)
    val quotaBody =
      """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"You exceeded your quota."}}"""
        .toByteArray()

    // Case 1: first model was 503 BUSY, second returned 429 QUOTA -> retried after 1 s.
    performer.performRequest(
      listOf(url("gemini-2.5-pro"), url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), mutableListOf()),
    )
    queue.requests[0].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    queue.requests[1].deliverError(
      ClientError(NetworkResponse(429, quotaBody, false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    assertEquals(3, queue.requests.size)

    // Case 2: single model returns 429 QUOTA with Retry-After: 2 -> retried after 2 s.
    performer.cancelExistingRequestIfNeeded()
    val singleFailures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      listOf(url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), singleFailures),
    )
    queue.requests[3].deliverError(
      ClientError(NetworkResponse(429, quotaBody, false, 0L, listOf(Header("Retry-After", "2"))))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_999))
    assertEquals(4, queue.requests.size)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
    assertEquals(5, queue.requests.size)
  }

  @Test
  fun nonRetryablePerformerDoesNotRetry503Round() {
    val queue = RecordingRequestQueue()
    val performer = GeminiRestRequestPerformer(queue, false)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      listOf(url("gemini-2.5-flash")),
      "key",
      postData,
      recordingCallback(mutableListOf(), failures),
    )

    queue.requests.single().deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(1, queue.requests.size)
    assertEquals(1, failures.size)
  }

  private fun geminiSuccessJson(): JSONObject =
    JSONObject()
      .put(
        "candidates",
        JSONArray()
          .put(
            JSONObject()
              .put("finishReason", "STOP")
              .put(
                "content",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "ok"))),
              )
          ),
      )

  private fun url(model: String) =
    "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

  private fun recordingCallback(
    responses: MutableList<GeminiResponse>,
    failures: MutableList<GeminiFailure>,
  ) =
    object : GeminiRestResponseCallback {
      override fun onResponse(response: GeminiResponse) {
        responses += response
      }

      override fun onFailure(failure: GeminiFailure) {
        failures += failure
      }

      override fun onCancelled() {}
    }

  private class RecordingRequestQueue :
    RequestQueue(NoCache(), Network { throw AssertionError("Unexpected network call") }) {
    val requests = mutableListOf<Request<*>>()

    override fun <T> add(request: Request<T>): Request<T> {
      super.add(request)
      requests += request
      return request
    }
  }
}
