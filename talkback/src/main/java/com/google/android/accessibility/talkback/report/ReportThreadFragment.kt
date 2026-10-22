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
import android.text.InputFilter
import android.text.InputType
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper
import java.time.Instant

/**
 * A sent report: its status, what was sent, the replies and the reporter's own messages, in order,
 * and a reply to send. Opening it marks the replies and the status as read.
 */
class ReportThreadFragment : TalkbackBaseFragment() {
  private val store by lazy { ReportStore.get(requireContext()) }
  private val reportId: String by lazy { requireArguments().getString(ARG_REPORT_ID).orEmpty() }
  private val onChange: () -> Unit = { view?.post { if (isAdded) refresh() } }
  private var sending = false
  // The rows that change, updated in place so that the screen reader's focus stays where it is.
  private var status: Preference? = null
  private var messages: PreferenceCategory? = null
  private var shownMessageIds = emptyList<Long>()
  private var reply: EditTextPreference? = null
  private var send: Preference? = null

  override fun getTitle(): CharSequence =
    store.sentReport(reportId)?.title ?: getText(R.string.title_pref_problem_report)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    build()
  }

  override fun onResume() {
    super.onResume()
    store.addListener(onChange)
    markRead()
    ReportInbox.cancelNotification(requireContext(), reportId)
    ReportInbox.checkNow(requireContext())
  }

  override fun onPause() {
    store.removeListener(onChange)
    super.onPause()
  }

  private fun markRead() {
    store.updateSent(reportId) { report ->
      val newest = report.messages.filter { it.fromDeveloper }.maxOfOrNull { it.id } ?: 0
      if (newest <= report.readMessageId && !report.statusUnread) report
      else report.copy(readMessageId = maxOf(report.readMessageId, newest), readStatus = report.status)
    }
  }

  /** Updates the rows in place, adding new messages at the end. */
  private fun refresh() {
    val report = store.sentReport(reportId)
    val shownMessages = messages
    if (report == null || shownMessages == null) {
      build()
      return
    }
    status?.summary = getString(ReportInbox.statusName(report.status))
    val ids = report.messages.map { it.id }
    if (ids.take(shownMessageIds.size) == shownMessageIds) {
      report.messages.drop(shownMessageIds.size).forEach { shownMessages.addPreference(messageRow(it)) }
      shownMessageIds = ids
    } else {
      build()
      return
    }
    shownMessages.isVisible = ids.isNotEmpty()
    reply?.apply {
      text = report.replyDraft
      summary = report.replyDraft.ifEmpty { null }
    }
    updateSend(report)
  }

  private fun updateSend(report: SentReport) {
    send?.apply {
      summary = if (sending) getString(R.string.summary_pref_problem_report_sending) else null
      isEnabled =
        !sending && report.replyDraft.isNotBlank() && report.status != SentReport.STATUS_UNKNOWN
    }
  }

  private fun messageRow(message: ReportMessage): Preference =
    text(
      getString(
        if (message.fromDeveloper) R.string.report_message_from_developer
        else R.string.report_message_from_you,
        sentAt(message.at),
      ),
      message.body,
    )

  private fun build() {
    val context = requireContext()
    val report = store.sentReport(reportId)
    val screen = preferenceManager.createPreferenceScreen(context)
    status = null
    messages = null
    reply = null
    send = null
    if (report == null) {
      screen.addPreference(text(getString(R.string.report_missing), null))
      preferenceScreen = screen
      return
    }
    val statusRow =
      text(getString(R.string.title_report_status), getString(ReportInbox.statusName(report.status)))
    status = statusRow
    screen.addPreference(statusRow)

    val sent = category(getString(R.string.title_report_your_report))
    screen.addPreference(sent)
    sent.addPreference(
      text(
        getString(R.string.report_message_from_you, sentAt(report.sentAt)),
        "${report.title}\n\n${report.description}",
      )
    )

    val messageCategory = category(getString(R.string.title_report_messages))
    screen.addPreference(messageCategory)
    report.messages.forEach { messageCategory.addPreference(messageRow(it)) }
    messageCategory.isVisible = report.messages.isNotEmpty()
    messages = messageCategory
    shownMessageIds = report.messages.map { it.id }

    val replyCategory = category(getString(R.string.title_report_reply))
    screen.addPreference(replyCategory)
    val replyField =
      EditTextPreference(context).apply {
        key = "report_reply_$reportId"
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.title_report_reply_text)
        text = report.replyDraft
        summary = report.replyDraft.ifEmpty { null }
        setOnBindEditTextListener { field ->
          field.inputType =
            InputType.TYPE_CLASS_TEXT or
              InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
              InputType.TYPE_TEXT_FLAG_MULTI_LINE
          field.filters = arrayOf(InputFilter.LengthFilter(ProblemReport.MAX_TEXT))
          field.setSelection(field.length())
        }
        setOnPreferenceChangeListener { _, value ->
          val text = value as String
          store.updateSent(reportId) { it.copy(replyDraft = text) }
          true
        }
      }
    replyCategory.addPreference(replyField)
    reply = replyField
    val sendRow =
      AccessibilitySuitePreference(context).apply {
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.title_report_send_reply)
        setOnPreferenceClickListener {
          store.sentReport(reportId)?.let { sendReply(it.token, it.replyDraft.trim()) }
          true
        }
      }
    replyCategory.addPreference(sendRow)
    send = sendRow
    updateSend(report)
    screen.addPreference(
      AccessibilitySuitePreference(context).apply {
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.title_report_remove)
        setOnPreferenceClickListener {
          confirmRemove()
          true
        }
      }
    )
    preferenceScreen = screen
  }

  private fun sendReply(token: String, body: String) {
    if (body.isEmpty()) return
    val appContext = requireContext().applicationContext
    val id = reportId
    sending = true
    store.sentReport(id)?.let { updateSend(it) }
    ProblemReportSender.reply(appContext, token, body) { result, message ->
      sending = false
      Toast.makeText(appContext, replyMessage(result), Toast.LENGTH_LONG).show()
      if (result == ProblemReportSender.Result.SENT) {
        ReportStore.get(appContext).updateSent(id) { report ->
          report.copy(
            replyDraft = "",
            messages = if (message == null) report.messages else report.messages + message,
          )
        }
      }
      if (isAdded) refresh()
    }
  }

  private fun confirmRemove() {
    A11yAlertDialogWrapper.materialDialogBuilder(requireActivity())
      .setTitle(R.string.dialog_title_report_remove)
      .setMessage(R.string.dialog_message_report_remove)
      .setPositiveButton(R.string.button_report_delete) { _, _ ->
        store.deleteSent(reportId)
        ReportInbox.cancelNotification(requireContext(), reportId)
        parentFragmentManager.popBackStack()
      }
      .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
      .setCancelable(true)
      .create()
      .show()
  }

  private fun sentAt(millis: Long): CharSequence = ReportInbox.timeAgo(requireContext(), millis)

  /** When a message from the server was written, from its time in ISO 8601. */
  private fun sentAt(iso: String): CharSequence =
    try {
      sentAt(Instant.parse(iso).toEpochMilli())
    } catch (e: RuntimeException) {
      ""
    }

  private fun category(title: String): PreferenceCategory =
    AccessibilitySuitePreferenceCategory(requireContext()).apply {
      this.title = title
      isIconSpaceReserved = false
    }

  private fun text(title: String, summary: String?): Preference =
    AccessibilitySuitePreference(requireContext()).apply {
      this.title = title
      this.summary = summary
      isPersistent = false
      isIconSpaceReserved = false
      isSingleLineTitle = false
    }

  private fun replyMessage(result: ProblemReportSender.Result): Int =
    when (result) {
      ProblemReportSender.Result.SENT -> R.string.report_reply_sent
      ProblemReportSender.Result.RATE_LIMITED -> R.string.problem_report_rate_limited
      ProblemReportSender.Result.NO_CONNECTION -> R.string.problem_report_no_connection
      ProblemReportSender.Result.NOT_SENT -> R.string.report_reply_not_sent
    }

  companion object {
    const val ARG_REPORT_ID = "report_id"
  }
}
