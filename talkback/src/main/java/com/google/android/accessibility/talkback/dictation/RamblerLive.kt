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

package com.google.android.accessibility.talkback.dictation

import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Rambler live transcription: the recording is streamed to Gemini 3.5 Transcribe Live while the
 * user speaks, and the text of each phrase comes back, cleaned up in smart mode, when they pause.
 * When recording stops, only the last phrase is left to transcribe, so the text is ready sooner
 * than when the whole recording is sent at the end.
 *
 * [send] can be called from any thread. [finish] gets all the text, or null when the session
 * failed, in which case the whole recording can still be sent the usual way.
 */
internal class RamblerLive(private val apiKey: String, private val url: String = URL) {
  private val main = Handler(Looper.getMainLooper())
  private val lock = Any()
  private var socket: WebSocket? = null
  /** Audio recorded before the session is ready, sent when it is. */
  private val waiting = mutableListOf<ByteArray>()
  private var ready = false
  private var failed = false
  /** Whether a phrase was heard that has no final text yet. */
  private var pending = false
  private val phrases = mutableListOf<String>()
  private var onDone: ((String?) -> Unit)? = null
  private val complete = Runnable { done(failedResult = false) }

  /** Whether a phrase was heard that has no final text yet. */
  @get:VisibleForTesting
  internal val isPhrasePending: Boolean
    get() = synchronized(lock) { pending }

  fun start() {
    val request =
      Request.Builder()
        .url(url)
        // In a header rather than the address, which logs show.
        .header(API_KEY_HEADER, apiKey)
        .build()
    socket = client.newWebSocket(request, listener)
  }

  /** Streams [pcm], 16 kHz mono 16-bit audio. */
  fun send(pcm: ByteArray) {
    synchronized(lock) {
      if (failed || onDone != null) return
      if (!ready) {
        // A few seconds at most, while the session opens.
        if (waiting.size < MAX_WAITING_CHUNKS) waiting.add(pcm) else failed = true
        return
      }
    }
    sendAudio(pcm)
  }

  /**
   * Ends the audio and passes all the text to [result] on the main thread, once the last phrase is
   * transcribed, or null when the session failed.
   */
  fun finish(result: (String?) -> Unit) {
    val alreadyFailed: Boolean
    val waitForLast: Boolean
    synchronized(lock) {
      onDone = result
      alreadyFailed = failed || !ready
      waitForLast = pending
    }
    if (alreadyFailed) {
      done(failedResult = true)
      return
    }
    socket?.send(JSONObject().put("realtimeInput", JSONObject().put("audioStreamEnd", true)).toString())
    main.postDelayed(::timeOut, FINISH_TIMEOUT_MS)
    // Text may still be on its way even when no phrase was heard since the last.
    main.postDelayed(complete, if (waitForLast) FINISH_TIMEOUT_MS else SETTLE_MS)
  }

  /** Ends the session without a result, as when SandFly turns off. */
  fun cancel() {
    synchronized(lock) {
      failed = true
      onDone = null
    }
    main.removeCallbacksAndMessages(null)
    socket?.cancel()
  }

  /** The last phrase took too long: when one was heard, the whole recording is sent instead. */
  private fun timeOut() {
    LogUtils.w(TAG, "No final text in time")
    done(failedResult = synchronized(lock) { pending })
  }

  private fun done(failedResult: Boolean) {
    main.removeCallbacksAndMessages(null)
    val callback: ((String?) -> Unit)?
    val text: String?
    synchronized(lock) {
      callback = onDone
      onDone = null
      text = if (failedResult) null else phrases.joinToString(" ")
      failed = true
    }
    socket?.close(NORMAL_CLOSURE, null)
    callback?.let { main.post { it(text) } }
  }

  private fun sendAudio(pcm: ByteArray) {
    val chunk =
      JSONObject()
        .put(
          "realtimeInput",
          JSONObject()
            .put(
              "audio",
              JSONObject()
                .put("data", Base64.encodeToString(pcm, Base64.NO_WRAP))
                .put("mimeType", "audio/pcm;rate=${AudioCapture.SAMPLE_RATE}"),
            ),
        )
    // False when the connection is gone or can't keep up; the recording is then sent at the end.
    if (socket?.send(chunk.toString()) != true) synchronized(lock) { failed = true }
  }

  private val listener =
    object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        val setup =
          JSONObject()
            .put(
              "setup",
              JSONObject()
                .put("model", "models/$MODEL")
                .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("TEXT")))
                .put("inputAudioTranscription", JSONObject().put("mode", "SMART")),
            )
        webSocket.send(setup.toString())
      }

      override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

      // The Live API sends its JSON in binary frames too.
      override fun onMessage(webSocket: WebSocket, bytes: ByteString) = handle(bytes.utf8())

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        LogUtils.w(TAG, "Session failed: %s (HTTP %s)", t, response?.code())
        endEarly()
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        LogUtils.w(TAG, "Session closed: %d %s", code, reason)
        endEarly()
      }
    }

  /**
   * The session ended by itself. Before [finish], or before the last phrase came, what was recorded
   * is sent the usual way.
   */
  private fun endEarly() {
    val finishing: Boolean
    val incomplete: Boolean
    synchronized(lock) {
      finishing = onDone != null
      incomplete = pending || phrases.isEmpty()
      if (!finishing) failed = true
    }
    if (finishing) main.post { done(failedResult = incomplete) }
  }

  private fun handle(message: String) {
    val json =
      try {
        JSONObject(message)
      } catch (e: JSONException) {
        LogUtils.w(TAG, "Not JSON: %s", e)
        return
      }
    if (json.has("setupComplete")) {
      val queued: List<ByteArray>
      synchronized(lock) {
        ready = true
        queued = waiting.toList()
        waiting.clear()
      }
      queued.forEach(::sendAudio)
      return
    }
    // The session ends soon, as after ten minutes.
    if (json.has("goAway")) {
      endEarly()
      return
    }
    val content = json.optJSONObject("serverContent") ?: return
    content.optJSONObject("interimInputTranscription")?.let {
      synchronized(lock) { pending = true }
    }
    content.optJSONObject("inputTranscription")?.optString("text")?.trim()?.let { text ->
      val finishing: Boolean
      synchronized(lock) {
        if (text.isNotEmpty()) phrases.add(text)
        pending = false
        finishing = onDone != null
      }
      // After the end of the audio, the last phrase is the one awaited.
      if (finishing) {
        main.removeCallbacks(complete)
        main.postDelayed(complete, SETTLE_MS)
      }
    }
  }

  private companion object {
    const val TAG = "RamblerLive"
    const val MODEL = "gemini-3.5-transcribe-live"
    const val URL =
      "wss://generativelanguage.googleapis.com/ws/" +
        "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    const val API_KEY_HEADER = "x-goog-api-key"
    const val NORMAL_CLOSURE = 1000
    /** Fifty seconds of audio. */
    const val MAX_WAITING_CHUNKS = 500
    /** How long to wait for the last phrase after the end of the audio. */
    const val FINISH_TIMEOUT_MS = 5_000L
    /** How long to wait for any text already on its way. */
    const val SETTLE_MS = 600L

    val client: OkHttpClient by lazy {
      OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    }
  }
}
