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

import android.text.InputType
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/** The "Other AI service" preference in Automatic descriptions settings. */
object AiServicePreference {
  const val PREF_KEY = "pref_ai_service"

  /** Sets up the preference, if the [fragment] has it. */
  @JvmStatic
  fun setUp(fragment: PreferenceFragmentCompat) {
    val preference = fragment.findPreference<Preference>(PREF_KEY) ?: return
    updateSummary(preference)
    preference.setOnPreferenceClickListener {
      showDialog(fragment, preference)
      true
    }
  }

  private fun updateSummary(preference: Preference) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(preference.context)
    preference.summary =
      if (AiService.isSetUp(prefs)) {
        preference.context.getString(R.string.summary_pref_ai_service_set, AiService.model(prefs))
      } else {
        preference.context.getString(R.string.summary_pref_ai_service_not_set)
      }
  }

  private fun showDialog(fragment: PreferenceFragmentCompat, preference: Preference) {
    val context = fragment.requireContext()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    fun field(hint: Int, value: String, inputType: Int) =
      KeyboardSafeEditText(context).apply {
        this.inputType = inputType
        isSingleLine = true
        setHint(hint)
        setText(value)
      }
    val address =
      field(
        R.string.ai_service_address_hint,
        AiService.address(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
      )
    val model =
      field(
        R.string.ai_service_model_hint,
        AiService.model(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
      )
    // A visible password field, so that the key is not learned by the keyboard or offered as a
    // suggestion, but can still be read back after pasting it.
    val apiKey =
      field(
        R.string.ai_service_api_key_hint,
        AiService.apiKey(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
      )
    // Lines the fields up with the dialog's message.
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    val container =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, 0, padding, 0)
        addView(address)
        addView(model)
        addView(apiKey)
      }
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_ai_service)
      .setMessage(R.string.ai_service_dialog_message)
      .setView(container)
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        val newAddress = address.text.toString().trim()
        if (newAddress.isNotEmpty() && !AiService.isValidAddress(newAddress)) {
          Toast.makeText(context, R.string.ai_service_address_invalid, Toast.LENGTH_LONG).show()
          return@setPositiveButton
        }
        AiService.save(prefs, newAddress, model.text.toString(), apiKey.text.toString())
        updateSummary(preference)
        GeminiApiKeyPreference.refresh(fragment)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
    address.requestFocus()
  }
}
