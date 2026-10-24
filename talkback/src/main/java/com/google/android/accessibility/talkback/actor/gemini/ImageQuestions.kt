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
import java.util.function.Consumer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Asks the on-device model, or the AI service in use, about one image, the way Describe image
 * does, for Live recognition. Not for Gemini, which Live recognition asks through the Live API.
 *
 * @param announce speaks why the on-device model could not answer, such as low memory
 */
class ImageQuestions(context: Context, announce: Consumer<String>) {
  private val performer = LocalGemmaRequestPerformer(context.applicationContext, announce)

  /**
   * Asks [prompt] about [jpeg], base64 JPEG. [done] gets the answer, or the failure, which is null
   * when the request was cancelled, on the main thread.
   */
  fun ask(prompt: String, jpeg: String, done: (String?, GeminiFailure?) -> Unit) {
    val parts =
      JSONArray()
        .put(JSONObject().put("text", prompt))
        .put(JSONObject().put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", jpeg)))
    val request = JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
    performer.performRequest(
      emptyList(),
      "",
      request,
      object : GeminiRestRequestPerformer.GeminiRestResponseCallback {
        override fun onResponse(response: DataFieldUtils.GeminiResponse) {
          val text = response.text()?.trim().orEmpty()
          if (text.isEmpty()) done(null, GeminiFailure.other("Empty answer")) else done(text, null)
        }

        override fun onFailure(failure: GeminiFailure) = done(null, failure)

        override fun onCancelled() = done(null, null)
      },
    )
  }

  fun cancel() = performer.cancelExistingRequestIfNeeded()
}
