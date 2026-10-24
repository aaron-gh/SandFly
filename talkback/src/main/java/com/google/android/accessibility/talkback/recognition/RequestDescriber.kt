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

import android.os.SystemClock
import com.google.android.accessibility.talkback.actor.gemini.GeminiFailure
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Describes one frame at a time with a request that answers in text: to the on-device model, or
 * to the AI service in use. A new frame is taken once the answer to the last one is in.
 *
 * @param prompt the prompt for the modes and the side of the fingertip
 * @param ask sends the prompt and the base64 JPEG, and calls back on the main thread with the
 *   answer, or a failure that is null when the request was cancelled
 * @param cancel stops the request that is running
 * @param minIntervalMs the shortest time between two frames
 * @param maxResultAgeMs how old an answer can be, from its frame, and still be spoken
 * @param onFailure called when requests stop working: the key was rejected, the quota is used up,
 *   or [MAX_FAILURES] requests in a row failed
 */
internal class RequestDescriber(
  private val prompt: (Set<RecognitionMode>, Boolean) -> String,
  private val ask: (String, String, (String?, GeminiFailure?) -> Unit) -> Unit,
  private val cancel: () -> Unit,
  private val minIntervalMs: Long,
  private val maxResultAgeMs: Long,
  private val onResult: (String) -> Unit,
  private val onFailure: (GeminiFailure?) -> Unit,
) : FrameDescriber {
  @Volatile private var closed = false
  @Volatile private var pending = false
  @Volatile private var lastRequest = Long.MIN_VALUE / 2
  private var request = 0
  private var failures = 0

  override fun canRequest(): Boolean =
    !closed && !pending && SystemClock.elapsedRealtime() - lastRequest >= minIntervalMs

  override fun describe(jpeg: String, modes: Set<RecognitionMode>, pointAbove: Boolean) {
    if (!canRequest() || modes.none { it.usesAi }) return
    pending = true
    val sentAt = SystemClock.elapsedRealtime()
    lastRequest = sentAt
    val id = ++request
    LogUtils.d(TAG, "Asking about a frame, %d bytes, for %s", jpeg.length, modes)
    ask(prompt(modes, pointAbove), jpeg) { answer, failure ->
      if (!closed && id == request) answered(answer, failure, SystemClock.elapsedRealtime() - sentAt)
    }
  }

  private fun answered(answer: String?, failure: GeminiFailure?, age: Long) {
    pending = false
    if (answer != null) {
      failures = 0
      LogUtils.d(TAG, "Answer, %d characters, %d ms after its frame", answer.length, age)
      if (age <= maxResultAgeMs) onResult(answer)
      return
    }
    failures++
    LogUtils.w(TAG, "No answer after %d ms: %s", age, failure ?: "cancelled")
    val kind = failure?.kind
    if (kind == GeminiFailure.Kind.API_KEY || kind == GeminiFailure.Kind.QUOTA || failures >= MAX_FAILURES) {
      close()
      onFailure(failure)
    }
  }

  override fun close() {
    if (closed) return
    closed = true
    if (pending) cancel()
    pending = false
  }

  companion object {
    /** Failures in a row, such as a busy service or a timeout, after which requests stop. */
    const val MAX_FAILURES = 3
    private const val TAG = "LiveRecognition"
  }
}
