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

package com.google.android.accessibility.talkback.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.preference.BasePreferencesActivity

/**
 * Report AI response: a problem report about the last text an AI model gave, such as an image
 * description, a screen description, live recognition or an AI agent task, for a response that is
 * offensive or wrong. The last response is kept in memory only, until SandFly stops.
 */
object AiResponseReport {
  @Volatile private var last: String? = null

  /** Keeps [text] as the last AI response. Called for every response that AI models give. */
  @JvmStatic
  fun record(text: CharSequence?) {
    val trimmed = text?.toString()?.trim()
    if (trimmed.isNullOrEmpty()) return
    last = trimmed.take(MAX_RESPONSE)
  }

  /** The last AI response, or null when there has been none since SandFly started. */
  @JvmStatic fun lastResponse(): String? = last

  /**
   * Opens a new problem report about the last AI response, with its text in the description.
   * Returns false when there is none.
   */
  @JvmStatic
  fun open(context: Context): Boolean {
    val response = last ?: return false
    val draft =
      ReportDraft(
        area = OTHER_AREA,
        title = context.getString(R.string.report_ai_response_title),
        description = context.getString(R.string.report_ai_response_description, response),
      )
    ReportStore.get(context).putDraft(draft)
    context.startActivity(
      Intent(context, TalkBackPreferencesActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, ProblemReportFragment::class.java.name)
        .putExtra(
          BasePreferencesActivity.FRAGMENT_ARGS,
          Bundle().apply { putString(ProblemReportFragment.ARG_DRAFT_ID, draft.id) },
        )
    )
    return true
  }

  /** The report's area, as in problem_report_area_values. */
  private const val OTHER_AREA = "Other"

  /** Room for the person's words in the description, which the server takes up to 10,000 characters. */
  private const val MAX_RESPONSE = ProblemReport.MAX_TEXT - 2_000
}
