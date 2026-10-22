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

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.SwitchPreferenceCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.report.ProblemReportSender.Result
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper

/**
 * A problem report being written, opened from Settings > Send feedback. What is typed is kept
 * in a draft, until the report is sent or the draft deleted, so a report can be finished later. The
 * email address and the two switches are kept for every report.
 */
class ProblemReportFragment : TalkbackBaseFragment(R.xml.problem_report_preferences) {
  private lateinit var type: ListPreference
  private lateinit var area: ListPreference
  private lateinit var title: EditTextPreference
  private lateinit var app: EditTextPreference
  private lateinit var description: EditTextPreference
  private lateinit var steps: EditTextPreference
  private lateinit var email: EditTextPreference
  private lateinit var device: SwitchPreferenceCompat
  private lateinit var log: SwitchPreferenceCompat
  private lateinit var send: Preference

  private val store by lazy { ReportStore.get(requireContext()) }
  private val draftId: String by lazy {
    arguments?.getString(ARG_DRAFT_ID) ?: store.newDraftId()
  }

  override fun getTitle(): CharSequence = getText(R.string.title_report_new)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.preferenceDataStore =
      DraftDataStore(requireContext(), store, draftId, SharedPreferencesUtils.getSharedPreferences(requireContext()))
    super.onCreatePreferences(savedInstanceState, rootKey)
    type = required(R.string.pref_problem_report_type_key)
    area = required(R.string.pref_problem_report_area_key)
    title = textField(R.string.pref_problem_report_title_key, ProblemReport.MAX_TITLE)
    app = textField(R.string.pref_problem_report_app_key, ProblemReport.MAX_APP)
    description =
      textField(R.string.pref_problem_report_description_key, ProblemReport.MAX_TEXT, multiLine = true)
    steps = textField(R.string.pref_problem_report_steps_key, ProblemReport.MAX_TEXT, multiLine = true)
    email =
      textField(
        R.string.pref_problem_report_email_key,
        ProblemReport.MAX_EMAIL,
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
      )
    device = required(R.string.pref_problem_report_device_key)
    log = required(R.string.pref_problem_report_log_key)
    send = required(R.string.pref_problem_report_send_key)

    steps.isVisible = type.value != ProblemReport.SUGGESTION
    type.setOnPreferenceChangeListener { _, value ->
      steps.isVisible = value != ProblemReport.SUGGESTION
      true
    }
    device.summary = deviceInformation().joinToString("\n")
    send.setOnPreferenceClickListener {
      send()
      true
    }
    required<Preference>(R.string.pref_problem_report_clear_key).setOnPreferenceClickListener {
      confirmDelete()
      true
    }
  }

  override fun onResume() {
    super.onResume()
    shown = this
    showSending()
  }

  override fun onPause() {
    if (shown === this) shown = null
    // A new report left empty isn't kept as a draft.
    if (store.draft(draftId)?.isEmpty == true) store.deleteDraft(draftId)
    super.onPause()
  }

  private fun <T : Preference> required(@StringRes key: Int): T = findPreference(getString(key))!!

  private fun textField(
    @StringRes key: Int,
    maxLength: Int,
    multiLine: Boolean = false,
    inputType: Int =
      InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
        (if (multiLine) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0),
  ): EditTextPreference =
    required<EditTextPreference>(key).apply {
      setOnBindEditTextListener { field ->
        field.inputType = inputType
        field.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        field.setSelection(field.length())
      }
    }

  private fun showSending() {
    send.isEnabled = !ProblemReportSender.isSending
    send.summary =
      if (ProblemReportSender.isSending) getString(R.string.summary_pref_problem_report_sending)
      else null
  }

  private fun send() {
    val context = requireContext()
    val report =
      ProblemReport(
        type = type.value ?: type.entryValues.first().toString(),
        area = area.value,
        title = title.text.orEmpty(),
        app = app.text,
        description = description.text.orEmpty(),
        steps = steps.text,
        email = email.text?.trim(),
        version = "SandFly ${BuildConfig.SANDFLY_VERSION}, build ${BuildConfig.COMMIT_COUNT}",
        device = if (device.isChecked) deviceInformation() else emptyList(),
        log = null,
      )
    if (report.title.isBlank() || report.description.isBlank()) {
      toast(context, R.string.problem_report_missing_fields)
      return
    }
    if (!report.email.isNullOrEmpty() && !Patterns.EMAIL_ADDRESS.matcher(report.email).matches()) {
      toast(context, R.string.problem_report_invalid_email)
      return
    }
    val appContext = context.applicationContext
    val id = draftId
    ProblemReportSender.send(appContext, report, log.isChecked) { result, token ->
      toast(appContext, messageFor(result))
      if (result == Result.SENT) {
        val sentStore = ReportStore.get(appContext)
        if (token != null) {
          sentStore.markSent(
            id,
            SentReport(
              id = id,
              token = token,
              type = report.type,
              title = report.title.trim(),
              description = report.description.trim(),
              sentAt = System.currentTimeMillis(),
            ),
          )
        } else {
          sentStore.deleteDraft(id)
        }
      }
      shown?.apply {
        showSending()
        if (result == Result.SENT && draftId == id) parentFragmentManager.popBackStack()
      }
    }
    showSending()
  }

  private fun confirmDelete() {
    A11yAlertDialogWrapper.materialDialogBuilder(requireActivity())
      .setTitle(R.string.dialog_title_report_delete_draft)
      .setPositiveButton(R.string.button_report_delete) { _, _ ->
        store.deleteDraft(draftId)
        parentFragmentManager.popBackStack()
      }
      .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
      .setCancelable(true)
      .create()
      .show()
  }

  private fun deviceInformation(): List<String> =
    listOf(
      "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
      "${Build.MANUFACTURER} ${Build.MODEL}",
    )

  /**
   * Keeps the fields of the report in the draft with [draftId], and the email address and the
   * switches, which every report shares, in [prefs].
   */
  private class DraftDataStore(
    private val context: Context,
    private val store: ReportStore,
    private val draftId: String,
    private val prefs: SharedPreferences,
  ) : PreferenceDataStore() {
    private fun draft() = store.draft(draftId) ?: ReportDraft(id = draftId)

    private fun key(@StringRes id: Int) = context.getString(id)

    override fun getString(key: String, defValue: String?): String? {
      val draft = draft()
      return when (key) {
        key(R.string.pref_problem_report_type_key) -> draft.type
        key(R.string.pref_problem_report_area_key) -> draft.area ?: defValue
        key(R.string.pref_problem_report_title_key) -> draft.title
        key(R.string.pref_problem_report_app_key) -> draft.app
        key(R.string.pref_problem_report_description_key) -> draft.description
        key(R.string.pref_problem_report_steps_key) -> draft.steps
        else -> prefs.getString(key, defValue)
      }
    }

    override fun putString(key: String, value: String?) {
      val draft = draft()
      val text = value.orEmpty()
      val changed =
        when (key) {
          key(R.string.pref_problem_report_type_key) -> draft.copy(type = text)
          key(R.string.pref_problem_report_area_key) -> draft.copy(area = value)
          key(R.string.pref_problem_report_title_key) -> draft.copy(title = text)
          key(R.string.pref_problem_report_app_key) -> draft.copy(app = text)
          key(R.string.pref_problem_report_description_key) -> draft.copy(description = text)
          key(R.string.pref_problem_report_steps_key) -> draft.copy(steps = text)
          else -> {
            prefs.edit { putString(key, value) }
            return
          }
        }
      store.putDraft(changed)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
      prefs.getBoolean(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
      prefs.edit { putBoolean(key, value) }
    }
  }

  companion object {
    const val ARG_DRAFT_ID = "draft_id"

    // The form on screen, which shows the result of a send that started on an earlier one.
    private var shown: ProblemReportFragment? = null

    private val OLD_FORM_KEYS =
      intArrayOf(
        R.string.pref_problem_report_type_key,
        R.string.pref_problem_report_area_key,
        R.string.pref_problem_report_title_key,
        R.string.pref_problem_report_app_key,
        R.string.pref_problem_report_description_key,
        R.string.pref_problem_report_steps_key,
      )

    /**
     * Moves a report that was being written before drafts, whose fields were settings, to a
     * draft. The email address and the switches stay settings.
     */
    @JvmStatic
    fun moveOldForm(context: Context) {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      fun old(@StringRes key: Int) = prefs.getString(context.getString(key), null).orEmpty()
      if (OLD_FORM_KEYS.none { prefs.contains(context.getString(it)) }) return
      val draft =
        ReportDraft(
          type = old(R.string.pref_problem_report_type_key).ifEmpty { ProblemReport.DEFAULT_TYPE },
          area = old(R.string.pref_problem_report_area_key).ifEmpty { null },
          title = old(R.string.pref_problem_report_title_key),
          app = old(R.string.pref_problem_report_app_key),
          description = old(R.string.pref_problem_report_description_key),
          steps = old(R.string.pref_problem_report_steps_key),
        )
      if (!draft.isEmpty) ReportStore.get(context).putDraft(draft)
      prefs.edit { OLD_FORM_KEYS.forEach { remove(context.getString(it)) } }
    }

    @StringRes
    private fun messageFor(result: Result): Int =
      when (result) {
        Result.SENT -> R.string.problem_report_sent
        Result.RATE_LIMITED -> R.string.problem_report_rate_limited
        Result.NO_CONNECTION -> R.string.problem_report_no_connection
        Result.NOT_SENT -> R.string.problem_report_not_sent
      }

    private fun toast(context: Context, @StringRes message: Int) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
  }
}
