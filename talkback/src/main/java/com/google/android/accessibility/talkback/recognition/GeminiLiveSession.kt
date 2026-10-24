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

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.talkback.R
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/**
 * One Gemini Live session that describes camera frames. State and callbacks are on the main
 * thread. Nothing is recorded: Gemini's audio answer is dropped and its transcript spoken.
 */
internal class GeminiLiveSession(
  apiKey: String,
  language: String,
  private val onReady: () -> Unit,
  private val onResult: (String) -> Unit,
  private val onFailure: (Int) -> Unit,
  connect: (Request, WebSocketListener) -> WebSocket = { request, listener -> client.newWebSocket(request, listener) },
) : AutoCloseable {
  private val handler = Handler(Looper.getMainLooper())
  private val response = GeminiLiveResponse()
  // Volatile so that the camera thread can check canRequest before it encodes a frame.
  @Volatile private var ready = false
  @Volatile private var closed = false
  @Volatile private var pending = false
  @Volatile private var lastRequest = -REQUEST_INTERVAL_MS
  private val timeout = Runnable { fail(R.string.live_recognition_timeout) }
  private val socket: WebSocket

  init {
    handler.postDelayed(timeout, SETUP_TIMEOUT_MS)
    // The key goes in a header rather than the address, which logs show.
    val request = Request.Builder().url(URL).header(API_KEY_HEADER, apiKey).build()
    socket = connect(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        handler.post {
          if (!closed && !webSocket.send(GeminiLiveProtocol.setup(GeminiLiveProtocol.MODEL, language))) {
            fail(R.string.live_recognition_connection_error)
          }
        }
      }
      override fun onMessage(webSocket: WebSocket, text: String) { receive(text) }
      override fun onMessage(webSocket: WebSocket, bytes: ByteString) { receive(bytes.utf8()) }
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        handler.post { fail(if (response?.code() in listOf(401, 403))
          R.string.gemini_error_api_key else R.string.live_recognition_connection_error) }
      }
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(code, null)
        handler.post { fail(closeMessage(code, reason)) }
      }
      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        handler.post { fail(R.string.live_recognition_session_ended) }
      }
    })
  }

  /** Whether [describe] would send a frame now. Can be called from any thread. */
  fun canRequest(): Boolean = ready && !closed && !pending &&
    SystemClock.elapsedRealtime() - lastRequest >= REQUEST_INTERVAL_MS

  fun describe(jpeg: String, modes: Set<RecognitionMode>, pointAbove: Boolean) {
    if (!canRequest() || modes.none { it.cloud }) return
    pending = true
    lastRequest = SystemClock.elapsedRealtime()
    handler.postDelayed(timeout, RESPONSE_TIMEOUT_MS)
    if (socket.queueSize() > MAX_QUEUED_BYTES ||
        !socket.send(GeminiLiveProtocol.video(jpeg)) ||
        !socket.send(GeminiLiveProtocol.request(modes, pointAbove))) {
      fail(R.string.live_recognition_connection_error)
    }
  }

  private fun receive(message: String) {
    handler.post {
      if (closed) return@post
      try {
        val json = JSONObject(message)
        if (json.has("error")) {
          fail(R.string.live_recognition_connection_error)
        } else if (json.has("goAway")) {
          fail(R.string.live_recognition_session_ended)
        } else if (json.has("setupComplete")) {
          handler.removeCallbacks(timeout)
          ready = true
          onReady()
        } else {
          json.optJSONObject("serverContent")?.let { content ->
            val result = response.accept(content)
            if (result != null) {
              val wasPending = pending
              pending = false
              handler.removeCallbacks(timeout)
              // A slow answer must not describe an old frame as the current surroundings.
              if (wasPending && SystemClock.elapsedRealtime() - lastRequest <= MAX_RESULT_AGE_MS) onResult(result)
            }
          }
        }
      } catch (_: Exception) {
        fail(R.string.live_recognition_connection_error)
      }
    }
  }

  /** The Live API closes the session with a reason that names a rejected key or the quota. */
  private fun closeMessage(code: Int, reason: String): Int = when {
    code == NORMAL_CLOSURE -> R.string.live_recognition_session_ended
    reason.contains("API key", ignoreCase = true) -> R.string.gemini_error_api_key
    reason.contains("quota", ignoreCase = true) -> R.string.gemini_error_quota
    else -> R.string.live_recognition_connection_error
  }

  private fun fail(message: Int) {
    if (closed) return
    close()
    onFailure(message)
  }

  override fun close() {
    if (closed) return
    closed = true
    ready = false
    handler.removeCallbacks(timeout)
    socket.cancel()
  }

  companion object {
    /** The shortest time between two frames sent to Gemini. */
    const val REQUEST_INTERVAL_MS = 4_000L
    /** Answers to older frames are dropped, as they no longer describe what the camera sees. */
    const val MAX_RESULT_AGE_MS = 10_000L
    private const val SETUP_TIMEOUT_MS = 20_000L
    private const val RESPONSE_TIMEOUT_MS = 30_000L
    private const val MAX_QUEUED_BYTES = 512 * 1024L
    private const val URL =
      "wss://generativelanguage.googleapis.com/ws/" +
        "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    private const val API_KEY_HEADER = "x-goog-api-key"
    private const val NORMAL_CLOSURE = 1000

    private val client: OkHttpClient by lazy {
      OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    }
  }
}
