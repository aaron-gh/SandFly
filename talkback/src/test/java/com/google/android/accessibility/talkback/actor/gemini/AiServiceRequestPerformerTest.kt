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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiServiceRequestPerformerTest {
  private val validRequest =
    JSONObject()
      .put(
        "contents",
        JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "hi")))),
      )

  @Test
  fun aFailureBeforeSendingComesAfterTheCallReturns() {
    val failures = mutableListOf<GeminiFailure>()
    val callback = recordingCallback(failures)
    AiServiceRequestPerformer(RuntimeEnvironment.getApplication())
      .performRequest("http://example.com", "model", "key", JSONObject(), callback)
    // The caller starts its progress tone here, which the failure has to stop.
    assertTrue(failures.isEmpty())
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(1, failures.size)
  }

  @Test
  fun cancelDuringInFlightRequestStopsRetryChain() {
    val queue = RecordingRequestQueue()
    val performer = AiServiceRequestPerformer(RuntimeEnvironment.getApplication(), queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      "https://example.com",
      "model",
      "key",
      validRequest,
      recordingCallback(failures),
    )

    assertEquals(1, queue.requests.size)
    val inFlight = queue.requests.single()
    performer.cancel()
    assertTrue(inFlight.isCanceled)

    inFlight.deliverError(ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList())))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(1, queue.requests.size)
    assertTrue(failures.isEmpty())
  }

  @Test
  fun server500WithoutRetryAfterIsNotRetried() {
    val queue = RecordingRequestQueue()
    val performer = AiServiceRequestPerformer(RuntimeEnvironment.getApplication(), queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      "https://example.com",
      "model",
      "key",
      validRequest,
      recordingCallback(failures),
    )

    queue.requests[0].deliverError(
      ServerError(NetworkResponse(500, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(1, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(500, failures.single().statusCode)
  }

  @Test
  fun retryableStatusesAndRetryAfterAreRetried() {
    val queue = RecordingRequestQueue()
    val performer = AiServiceRequestPerformer(RuntimeEnvironment.getApplication(), queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      "https://example.com",
      "model",
      "key",
      validRequest,
      recordingCallback(failures),
    )

    // 529 (overloaded) retries after default round-0 delay (1_000 ms).
    queue.requests[0].deliverError(
      ServerError(NetworkResponse(529, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(999))
    assertEquals(1, queue.requests.size)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
    assertEquals(2, queue.requests.size)

    // 429 with Retry-After header waits the longer 2 s delay, then stops on 400.
    queue.requests[1].deliverError(
      ClientError(
        NetworkResponse(429, ByteArray(0), false, 0L, listOf(Header("Retry-After", "2")))
      )
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_999))
    assertEquals(2, queue.requests.size)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
    assertEquals(3, queue.requests.size)

    queue.requests[2].deliverError(
      ClientError(NetworkResponse(400, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(3, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(400, failures.single().statusCode)
  }

  @Test
  fun retryKeepsLastAttemptInside35sAndStopsPastWindow() {
    val queue = RecordingRequestQueue()
    val performer = AiServiceRequestPerformer(RuntimeEnvironment.getApplication(), queue)
    val failures = mutableListOf<GeminiFailure>()
    performer.performRequest(
      "https://example.com",
      "model",
      "key",
      validRequest,
      recordingCallback(failures),
    )

    // Attempt 0 fails at 19 s; round-0 delay (1 s) starts attempt 1 at 20 s.
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(19))
    queue.requests[0].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    assertEquals(2, queue.requests.size)
    // Remaining budget from 35 s at t=20 s is 15 s (not 60 s).
    assertEquals(15_000, queue.requests[1].retryPolicy.currentTimeout)

    // If attempt 1 fails at 21 s, round-1 delay (2 s) would exceed the 20 s window.
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    queue.requests[1].deliverError(
      ServerError(NetworkResponse(503, ByteArray(0), false, 0L, emptyList()))
    )
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))

    assertEquals(2, queue.requests.size)
    assertEquals(1, failures.size)
    assertEquals(503, failures.single().statusCode)
  }

  private fun recordingCallback(failures: MutableList<GeminiFailure>) =
    object : GeminiRestResponseCallback {
      override fun onResponse(response: GeminiResponse) {}

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
