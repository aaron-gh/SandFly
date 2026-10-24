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
import com.google.android.libraries.accessibility.utils.log.LogUtils
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
) : FrameDescriber {
  private val handler = Handler(Looper.getMainLooper())
  private val response = GeminiLiveResponse()
  // Volatile so that the camera thread can check canRequest before it encodes a frame.
  @Volatile private var ready = false
  @Volatile private var closed = false
  /** Whether a frame was sent and its answer isn't complete. */
  @Volatile private var pending = false
  /** Whether any of the answer to the frame in [pending] came. */
  private var answerStarted = false
  @Volatile private var lastRequest = -REQUEST_INTERVAL_MS
  private val timeout = Runnable {
    LogUtils.w(TAG, "No answer in time (ready=%s)", ready)
    fail(R.string.live_recognition_timeout)
  }
  private val finish = Runnable { finishAnswer() }
  private val socket: WebSocket

  init {
    handler.postDelayed(timeout, SETUP_TIMEOUT_MS)
    // The key goes in a header rather than the address, which logs show.
    val request = Request.Builder().url(URL).header(API_KEY_HEADER, apiKey).build()
    LogUtils.i(TAG, "Connecting to %s", GeminiLiveProtocol.MODEL)
    socket = connect(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        LogUtils.i(TAG, "Connected, sending setup")
        handler.post {
          if (!closed && !webSocket.send(GeminiLiveProtocol.setup(GeminiLiveProtocol.MODEL, language))) {
            fail(R.string.live_recognition_connection_error)
          }
        }
      }
      override fun onMessage(webSocket: WebSocket, text: String) { receive(text) }
      override fun onMessage(webSocket: WebSocket, bytes: ByteString) { receive(bytes.utf8()) }
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        LogUtils.w(TAG, "Connection failed: %s, HTTP %s", t.javaClass.simpleName, response?.code())
        handler.post { fail(if (response?.code() in listOf(401, 403))
          R.string.gemini_error_api_key else R.string.live_recognition_connection_error) }
      }
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        LogUtils.w(TAG, "Closed by Gemini: %d %s", code, reason)
        webSocket.close(code, null)
        handler.post { fail(closeMessage(code, reason)) }
      }
      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        handler.post { fail(R.string.live_recognition_session_ended) }
      }
    })
  }

  /** Whether [describe] would send a frame now. Can be called from any thread. */
  override fun canRequest(): Boolean = ready && !closed && !pending &&
    SystemClock.elapsedRealtime() - lastRequest >= REQUEST_INTERVAL_MS

  override fun describe(jpeg: String, modes: Set<RecognitionMode>, pointAbove: Boolean) {
    if (!canRequest() || modes.none { it.usesAi }) return
    pending = true
    answerStarted = false
    response.clear()
    lastRequest = SystemClock.elapsedRealtime()
    handler.postDelayed(timeout, RESPONSE_TIMEOUT_MS)
    LogUtils.d(TAG, "Sending a frame, %d bytes, for %s", jpeg.length, modes)
    if (socket.queueSize() > MAX_QUEUED_BYTES ||
        !socket.send(GeminiLiveProtocol.video(jpeg)) ||
        !socket.send(GeminiLiveProtocol.request(modes, pointAbove))) {
      LogUtils.w(TAG, "Frame not sent, %d bytes queued", socket.queueSize())
      fail(R.string.live_recognition_connection_error)
    }
  }

  private fun receive(message: String) {
    handler.post {
      if (closed) return@post
      try {
        val json = JSONObject(message)
        if (json.has("error")) {
          LogUtils.w(TAG, "Error from Gemini: %s", json.optJSONObject("error")?.optString("message"))
          fail(R.string.live_recognition_connection_error)
        } else if (json.has("goAway")) {
          LogUtils.i(TAG, "Gemini is ending the session")
          fail(R.string.live_recognition_session_ended)
        } else if (json.has("setupComplete")) {
          LogUtils.i(TAG, "Setup complete")
          handler.removeCallbacks(timeout)
          ready = true
          onReady()
        } else {
          val content = json.optJSONObject("serverContent")
          if (content == null) {
            LogUtils.v(TAG, "Ignored message: %s", json.keys().asSequence().toList())
          } else {
            LogUtils.v(TAG, "Server content: %s", content.keys().asSequence().toList())
            receiveContent(content)
          }
        }
      } catch (e: Exception) {
        LogUtils.w(TAG, "Unreadable message: %s", e.javaClass.simpleName)
        fail(R.string.live_recognition_connection_error)
      }
    }
  }

  /**
   * Gemini sends the answer, then generationComplete, and ends the turn with turnComplete only
   * once its audio would have finished playing. The next frame goes as soon as the answer is
   * complete, so the end of the previous turn, or its interruption by that frame, can come after
   * it: those are ignored until the new answer starts.
   */
  private fun receiveContent(content: JSONObject) {
    if (!pending) return
    val partOfAnswer = content.has("modelTurn") || content.has("outputTranscription")
    if (partOfAnswer && !answerStarted) {
      answerStarted = true
      LogUtils.d(TAG, "First answer content %d ms after its frame", SystemClock.elapsedRealtime() - lastRequest)
    }
    if (!answerStarted && !content.optBoolean("generationComplete")) return
    if (response.accept(content)) {
      LogUtils.d(TAG, "Answer complete %d ms after its frame", SystemClock.elapsedRealtime() - lastRequest)
      // Transcript that is still on its way is part of the answer.
      handler.removeCallbacks(finish)
      handler.postDelayed(finish, SETTLE_MS)
    }
  }

  private fun finishAnswer() {
    if (closed || !pending) return
    pending = false
    val result = response.take()
    handler.removeCallbacks(timeout)
    val age = SystemClock.elapsedRealtime() - lastRequest
    LogUtils.d(TAG, "Answer, %d characters, %d ms after its frame", result.length, age)
    // A slow answer must not describe an old frame as the current surroundings.
    if (age <= MAX_RESULT_AGE_MS) onResult(result)
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
    handler.removeCallbacks(finish)
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
    /** How long to wait after the end of the turn for transcript still on its way. */
    private const val SETTLE_MS = 800L
    private const val TAG = "LiveRecognition"

    private val client: OkHttpClient by lazy {
      OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    }
  }
}
