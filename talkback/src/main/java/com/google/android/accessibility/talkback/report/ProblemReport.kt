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

package com.google.android.accessibility.talkback.report

import org.json.JSONArray
import org.json.JSONObject

/**
 * A problem report, sent as JSON to the report server in tools/report-worker. The limits match
 * the server's, which refuses longer fields.
 */
data class ProblemReport(
  val type: String,
  val area: String?,
  val title: String,
  val app: String?,
  val description: String,
  val steps: String?,
  val email: String?,
  val version: String,
  val device: List<String>,
  val log: String?,
) {
  val isSuggestion: Boolean
    get() = type == SUGGESTION

  fun toJson(): JSONObject =
    JSONObject().apply {
      put("type", type)
      putIfFilled("area", area)
      put("title", title.trim())
      putIfFilled("app", app)
      put("description", description.trim())
      if (!isSuggestion) putIfFilled("steps", steps)
      putIfFilled("email", email)
      put("version", version)
      put("device", JSONArray(device))
      log?.takeIf { it.isNotEmpty() }?.let { put("log", it.takeLast(MAX_LOG)) }
    }

  private fun JSONObject.putIfFilled(name: String, value: String?) {
    value?.trim()?.takeIf { it.isNotEmpty() }?.let { put(name, it) }
  }

  companion object {
    const val SUGGESTION = "Suggestion"
    const val DEFAULT_TYPE = "Incorrect behavior"
    const val MAX_TITLE = 200
    const val MAX_APP = 100
    const val MAX_TEXT = 10_000
    const val MAX_EMAIL = 254
    // The server takes 60,000 characters; the rest of the log is cut from the start.
    const val MAX_LOG = 50_000
  }
}
