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
import androidx.preference.SwitchPreferenceCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.report.ProblemReportSender.Result
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper

/**
 * Settings > Report a problem: a form that sends a problem report to the report server, which
 * creates a GitHub issue. The fields are kept until the report is sent or Clear form, so a report
 * can be finished later.
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

  override fun getTitle(): CharSequence = getText(R.string.title_pref_problem_report)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
      confirmClear()
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
    ProblemReportSender.send(appContext, report, log.isChecked) { result ->
      toast(appContext, messageFor(result))
      if (result == Result.SENT) clearReport(appContext)
      shown?.apply {
        if (result == Result.SENT) showStoredValues()
        showSending()
      }
    }
    showSending()
  }

  private fun confirmClear() {
    A11yAlertDialogWrapper.materialDialogBuilder(requireActivity())
      .setTitle(R.string.dialog_title_problem_report_clear)
      .setPositiveButton(R.string.button_problem_report_clear) { _, _ ->
        val context = requireContext()
        clearReport(context)
        SharedPreferencesUtils.getSharedPreferences(context).edit {
          remove(getString(R.string.pref_problem_report_email_key))
          remove(getString(R.string.pref_problem_report_device_key))
          remove(getString(R.string.pref_problem_report_log_key))
        }
        showStoredValues()
      }
      .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
      .setCancelable(true)
      .create()
      .show()
  }

  /** Shows the stored values, after they were cleared outside the preferences. */
  private fun showStoredValues() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    type.value = prefs.getString(type.key, type.entryValues.first().toString())
    area.value = prefs.getString(area.key, null)
    for (field in listOf(title, app, description, steps, email)) {
      field.text = prefs.getString(field.key, null)
    }
    device.isChecked = prefs.getBoolean(device.key, true)
    log.isChecked = prefs.getBoolean(log.key, false)
    steps.isVisible = type.value != ProblemReport.SUGGESTION
  }

  private fun deviceInformation(): List<String> =
    listOf(
      "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
      "${Build.MANUFACTURER} ${Build.MODEL}",
    )

  companion object {
    // The form on screen, which shows the result of a send that started on an earlier one.
    private var shown: ProblemReportFragment? = null

    private val REPORT_KEYS =
      intArrayOf(
        R.string.pref_problem_report_type_key,
        R.string.pref_problem_report_area_key,
        R.string.pref_problem_report_title_key,
        R.string.pref_problem_report_app_key,
        R.string.pref_problem_report_description_key,
        R.string.pref_problem_report_steps_key,
      )

    /** Clears the report, keeping the email address and switches for the next report. */
    private fun clearReport(context: Context) {
      SharedPreferencesUtils.getSharedPreferences(context).edit {
        REPORT_KEYS.forEach { remove(context.getString(it)) }
      }
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
