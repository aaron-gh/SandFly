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

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment

/**
 * Settings > Send feedback: a new report, the inbox of sent reports with their status and
 * replies, and the drafts.
 */
class ReportsFragment : TalkbackBaseFragment() {
  private val store by lazy { ReportStore.get(requireContext()) }
  private val onChange: () -> Unit = { view?.post { if (isAdded) refresh() } }
  // The reports and drafts on screen, so that a change to them updates the rows in place, which
  // keeps the screen reader's focus where it is.
  private var shownIds = emptyList<String>()
  private val rows = mutableMapOf<String, Preference>()

  override fun getTitle(): CharSequence = getText(R.string.title_pref_problem_report)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    ProblemReportFragment.moveOldForm(requireContext())
    build()
  }

  override fun onResume() {
    super.onResume()
    store.addListener(onChange)
    refresh()
    ReportInbox.checkNow(requireContext())
  }

  override fun onPause() {
    store.removeListener(onChange)
    super.onPause()
  }

  /** Updates the rows in place, or builds the screen again if reports or drafts came or went. */
  private fun refresh() {
    val sent = store.sentReports()
    val drafts = store.drafts().filterNot { it.isEmpty }
    if (sent.map { it.id } + drafts.map { it.id } != shownIds) {
      build()
      return
    }
    for (report in sent) {
      rows[report.id]?.apply {
        title = report.title
        summary = inboxSummary(report)
      }
    }
    for (draft in drafts) {
      rows[draft.id]?.apply {
        title = draft.title.ifBlank { getString(R.string.report_untitled) }
        summary = draftSummary(draft)
      }
    }
  }

  private fun draftSummary(draft: ReportDraft): String =
    getString(R.string.report_draft_summary, ReportInbox.timeAgo(requireContext(), draft.updatedAt))

  private fun build() {
    val context = requireContext()
    rows.clear()
    val screen = preferenceManager.createPreferenceScreen(context)
    screen.addPreference(
      link(getString(R.string.title_report_new), null, ProblemReportFragment::class.java.name) {
        putString(ProblemReportFragment.ARG_DRAFT_ID, store.newDraftId())
      }
    )

    val inbox = category(R.string.title_report_inbox)
    screen.addPreference(inbox)
    val sent = store.sentReports()
    if (sent.isEmpty()) {
      inbox.addPreference(note(getString(R.string.report_inbox_empty)))
    }
    for (report in sent) {
      val row =
        link(report.title, inboxSummary(report), ReportThreadFragment::class.java.name) {
          putString(ReportThreadFragment.ARG_REPORT_ID, report.id)
        }
      rows[report.id] = row
      inbox.addPreference(row)
    }

    val drafts = category(R.string.title_report_drafts)
    screen.addPreference(drafts)
    val saved = store.drafts().filterNot { it.isEmpty }
    if (saved.isEmpty()) {
      drafts.addPreference(note(getString(R.string.report_drafts_empty)))
    }
    for (draft in saved) {
      val row =
        link(
          draft.title.ifBlank { getString(R.string.report_untitled) },
          draftSummary(draft),
          ProblemReportFragment::class.java.name,
        ) {
          putString(ProblemReportFragment.ARG_DRAFT_ID, draft.id)
        }
      rows[draft.id] = row
      drafts.addPreference(row)
    }
    shownIds = sent.map { it.id } + saved.map { it.id }
    preferenceScreen = screen
  }

  /** The status of [report], what is new in it, and when it was sent. */
  private fun inboxSummary(report: SentReport): String {
    val parts = mutableListOf<String>()
    parts += getString(ReportInbox.statusName(report.status))
    if (report.unreadMessages > 0) {
      parts +=
        resources.getQuantityString(
          R.plurals.report_new_replies,
          report.unreadMessages,
          report.unreadMessages,
        )
    } else if (report.statusUnread) {
      parts += getString(R.string.report_status_changed)
    }
    parts +=
      getString(R.string.report_sent_at, ReportInbox.timeAgo(requireContext(), report.sentAt))
    return parts.joinToString(", ")
  }

  private fun category(title: Int): PreferenceCategory =
    AccessibilitySuitePreferenceCategory(requireContext()).apply {
      this.title = getString(title)
      isIconSpaceReserved = false
    }

  private fun note(text: String): Preference =
    AccessibilitySuitePreference(requireContext()).apply {
      title = text
      isSelectable = false
      isIconSpaceReserved = false
    }

  private fun link(
    title: String,
    summary: String?,
    fragment: String,
    arguments: Bundle.() -> Unit,
  ): Preference =
    AccessibilitySuitePreference(requireContext()).apply {
      this.title = title
      this.summary = summary
      this.fragment = fragment
      isPersistent = false
      isIconSpaceReserved = false
      isSingleLineTitle = false
      extras.arguments()
    }
}
