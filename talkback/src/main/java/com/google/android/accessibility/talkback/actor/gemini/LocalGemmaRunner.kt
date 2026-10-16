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

import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlm
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlmCancelledException
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlmMemoryException
import java.util.Base64
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import org.json.JSONException
import org.json.JSONObject

/**
 * Answers the Gemini-style requests that [GeminiRestEndpoint] builds with a [LocalLlm], so that the
 * prompts, the parsing and the screens stay the same whether the model is in the cloud or on the
 * phone. Runs one request at a time, and a new request cancels the one that is running.
 *
 * @param llm gives the model, loading it if needed, or null when none is installed. Only called on
 *   the worker thread, because it can take a while.
 * @param activeLlm gives the model that is loaded now without loading one, and never blocks
 * @param worker runs the model; it should have a single thread
 * @param canceller runs the cancelling, because stopping a model can block and cancel is called on
 *   the main thread
 * @param callbackExecutor runs the callbacks, which expect the main thread
 * @param announce speaks why the model could not answer, on the callback executor. The request
 *   then ends as cancelled, which is silent, so the user does not also hear a generic error.
 */
internal class LocalGemmaRunner(
  private val llm: () -> LocalLlm?,
  private val activeLlm: () -> LocalLlm?,
  private val worker: ExecutorService,
  private val canceller: Executor,
  private val callbackExecutor: Executor,
  private val announce: Consumer<String>,
) {
  private class Job(val prompt: String, val jpeg: ByteArray?, val json: Boolean) {
    val cancelled = AtomicBoolean(false)
  }

  private val pending = AtomicReference<Job?>(null)

  val hasPending: Boolean
    get() = pending.get() != null

  fun run(postData: JSONObject, callback: GeminiRestResponseCallback) {
    val job =
      try {
        parse(postData)
      } catch (e: JSONException) {
        callbackExecutor.execute {
          callback.onFailure(GeminiFailure.other("Bad request: ${e.message}"))
        }
        return
      }
    cancel()
    pending.set(job)
    worker.execute { execute(job, callback) }
  }

  fun cancel() {
    pending.get()?.let {
      it.cancelled.set(true)
      canceller.execute { activeLlm()?.cancel() }
    }
  }

  private fun execute(job: Job, callback: GeminiRestResponseCallback) {
    try {
      if (job.cancelled.get()) {
        report(job) { callback.onCancelled() }
        return
      }
      val model = llm()
      if (model == null) {
        report(job) {
          callback.onFailure(GeminiFailure.other("No on-device model is installed"))
        }
        return
      }
      val raw = model.generate(job.prompt, job.jpeg)
      val text = if (job.json) extractJson(raw) else raw.trim()
      if (job.cancelled.get()) {
        report(job) { callback.onCancelled() }
      } else if (text.isEmpty()) {
        report(job) {
          callback.onFailure(GeminiFailure.other("The model gave an empty answer"))
        }
      } else {
        val response =
          DataFieldUtils.GeminiResponse.builder()
            .setText(text)
            .setFinishReason(DataFieldUtils.FINISH_REASON_STOP)
            .build()
        report(job) { callback.onResponse(response) }
      }
    } catch (_: LocalLlmCancelledException) {
      report(job) { callback.onCancelled() }
    } catch (e: LocalLlmMemoryException) {
      report(job) {
        announce.accept(e.spokenMessage)
        callback.onCancelled()
      }
    } catch (e: Exception) {
      report(job) { callback.onFailure(GeminiFailure.other(e.toString())) }
    } finally {
      // Already cleared when a result was reported, unless something unexpected was thrown.
      pending.compareAndSet(job, null)
    }
  }

  /**
   * Reports how [job] ended. It stops being pending first, so that the callback, and anything
   * waiting for it, sees nothing pending.
   */
  private fun report(job: Job, result: Runnable) {
    pending.compareAndSet(job, null)
    callbackExecutor.execute(result)
  }

  private fun parse(postData: JSONObject): Job {
    val parts = postData.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
    var prompt = ""
    var jpeg: ByteArray? = null
    for (i in 0 until parts.length()) {
      val part = parts.getJSONObject(i)
      if (part.has("text")) {
        prompt = part.getString("text")
      } else if (part.has("inlineData")) {
        jpeg = Base64.getDecoder().decode(part.getJSONObject("inlineData").getString("data"))
      }
    }
    val json =
      postData.optJSONObject("generationConfig")?.optString("responseMimeType") ==
        "application/json"
    return Job(prompt, jpeg, json)
  }

  companion object {
    /**
     * Models often wrap JSON in a markdown fence or add a sentence around it, which the parser does
     * not accept. Keeps the text from the first "{" to the last "}".
     */
    fun extractJson(raw: String): String {
      val start = raw.indexOf('{')
      val end = raw.lastIndexOf('}')
      return if (start in 0 until end) raw.substring(start, end + 1) else raw.trim()
    }
  }
}
