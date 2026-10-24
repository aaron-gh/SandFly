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

package com.google.android.accessibility.talkback.recognition

import android.os.Looper
import com.google.android.accessibility.talkback.R
import java.time.Duration
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiLiveSessionTest {
  private class Socket(private val request: Request) : WebSocket {
    val sent = mutableListOf<String>()
    var cancelled = false
    override fun request() = request
    override fun queueSize() = 0L
    override fun send(text: String): Boolean { sent.add(text); return !cancelled }
    override fun send(bytes: ByteString) = false
    override fun close(code: Int, reason: String?) = true
    override fun cancel() { cancelled = true }
  }
  private lateinit var listener: WebSocketListener
  private lateinit var socket: Socket
  private val results = mutableListOf<String>()
  private val failures = mutableListOf<Int>()
  private var ready = false

  private fun connect(): GeminiLiveSession = GeminiLiveSession(
    "test-key", "en", { ready = true }, results::add, failures::add,
    connect = { request, callbacks ->
      listener = callbacks
      Socket(request).also { socket = it }
    })

  private fun receive(message: String) {
    listener.onMessage(socket, message)
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test fun waitsForSetupAndAllowsOnlyOneOutstandingFrame() {
    val session = connect()
    listener.onOpen(socket, Response.Builder().request(socket.request()).code(101)
      .protocol(Protocol.HTTP_1_1).message("Switching Protocols").build())
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(1, socket.sent.size)
    session.describe("jpeg", setOf(RecognitionMode.SCENES), true)
    assertEquals(1, socket.sent.size)
    receive("""{"setupComplete":{}}""")
    assertTrue(ready)
    session.describe("jpeg", setOf(RecognitionMode.SCENES), true)
    session.describe("second", setOf(RecognitionMode.SCENES), true)
    assertEquals(3, socket.sent.size) // setup, one frame, one prompt
    receive("""{"serverContent":{"outputTranscription":{"text":"A door"},"generationComplete":true}}""")
    receive("""{"serverContent":{"outputTranscription":{"text":" on the left"}}}""")
    assertTrue(results.isEmpty()) // transcript can still come after the answer is complete
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
    assertEquals(listOf("A door on the left"), results)
    receive("""{"serverContent":{"turnComplete":true}}""")
    assertEquals(1, results.size)
    assertFalse(session.canRequest()) // still rate limited after fast response
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
    assertTrue(session.canRequest())
    session.close()
  }

  @Test fun apiKeyIsSentInAHeaderAndNotInTheAddress() {
    connect().close()
    assertEquals("test-key", socket.request().header("x-goog-api-key"))
    assertFalse(socket.request().url().toString().contains("test-key"))
  }

  @Test fun setupNamesTheLiveModel() {
    connect()
    listener.onOpen(socket, Response.Builder().request(socket.request()).code(101)
      .protocol(Protocol.HTTP_1_1).message("Switching Protocols").build())
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals("models/" + GeminiLiveProtocol.MODEL,
      JSONObject(socket.sent.single()).getJSONObject("setup").getString("model"))
  }

  @Test fun closeReasonsNameARejectedKeyOrTheQuota() {
    connect()
    listener.onClosing(socket, 1008, "API key not valid. Please pass a valid API key.")
    shadowOf(Looper.getMainLooper()).idle()
    connect()
    listener.onClosing(socket, 1011, "You exceeded your current quota")
    shadowOf(Looper.getMainLooper()).idle()
    connect()
    listener.onClosing(socket, 1000, "")
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(listOf(R.string.gemini_error_api_key, R.string.gemini_error_quota,
      R.string.live_recognition_session_ended), failures)
  }

  @Test fun closingCancelsCaptureTrafficAndDiscardsLateCallbacks() {
    val session = connect()
    session.close()
    assertTrue(socket.cancelled)
    receive("""{"setupComplete":{}}""")
    receive("""{"serverContent":{"outputTranscription":{"text":"Old scene"},"turnComplete":true}}""")
    listener.onFailure(socket, RuntimeException("must not be exposed"), null)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(40))
    session.describe("jpeg", setOf(RecognitionMode.SCENES), true)
    assertFalse(ready)
    assertTrue(results.isEmpty())
    assertTrue(failures.isEmpty())
    assertTrue(socket.sent.isEmpty())
  }

  @Test fun setupTimeoutIsBoundedAndReportedOnce() {
    connect()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(21))
    assertTrue(socket.cancelled)
    assertEquals(listOf(R.string.live_recognition_timeout), failures)
    listener.onClosed(socket, 1000, "closed")
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(1, failures.size)
  }

  @Test fun responseTimeoutStopsSendingRatherThanAccumulatingFrames() {
    val session = connect()
    receive("""{"setupComplete":{}}""")
    session.describe("jpeg", setOf(RecognitionMode.PEOPLE), true)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
    assertTrue(socket.cancelled)
    assertFalse(session.canRequest())
    assertEquals(listOf(R.string.live_recognition_timeout), failures)
  }

  @Test fun nextFrameGoesBeforeTheTurnEndsAndTheOldTurnEndIsIgnored() {
    val session = connect()
    receive("""{"setupComplete":{}}""")
    session.describe("jpeg", setOf(RecognitionMode.SCENES), true)
    receive("""{"serverContent":{"outputTranscription":{"text":"A chair"}}}""")
    receive("""{"serverContent":{"generationComplete":true}}""")
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
    assertEquals(listOf("A chair"), results)
    // Gemini is still playing its audio of the first answer.
    assertTrue(session.canRequest())
    session.describe("second", setOf(RecognitionMode.SCENES), true)
    receive("""{"serverContent":{"interrupted":true}}""")
    receive("""{"serverContent":{"turnComplete":true}}""")
    receive("""{"serverContent":{"outputTranscription":{"text":"A table"}}}""")
    receive("""{"serverContent":{"generationComplete":true}}""")
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    assertEquals(listOf("A chair", "A table"), results)
    assertTrue(failures.isEmpty())
    session.close()
  }

  @Test fun slowResponseIsDiscardedAndTheNextFrameCanBeRequested() {
    val session = connect()
    receive("""{"setupComplete":{}}""")
    session.describe("jpeg", setOf(RecognitionMode.PEOPLE), true)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
    receive("""{"serverContent":{"outputTranscription":{"text":"An old scene"},"turnComplete":true}}""")
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    assertTrue(results.isEmpty())
    assertTrue(session.canRequest())
    assertFalse(socket.cancelled)
    session.close()
  }

  @Test fun expiredSessionStopsWithExplicitRetryFeedback() {
    connect()
    receive("""{"goAway":{"timeLeft":"10s"}}""")
    assertTrue(socket.cancelled)
    assertEquals(listOf(R.string.live_recognition_session_ended), failures)
  }
}
