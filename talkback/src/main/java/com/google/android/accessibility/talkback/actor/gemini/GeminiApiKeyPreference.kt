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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** The "Gemini API key" preference in Automatic descriptions settings. */
object GeminiApiKeyPreference {

  /** Sets up the preference, if the [fragment] has it. */
  @JvmStatic
  fun setUp(fragment: PreferenceFragmentCompat) {
    val preference = fragment.findPreference<Preference>(GeminiApiKey.PREF_KEY) ?: return
    updateSummary(preference)
    preference.setOnPreferenceClickListener {
      showDialog(fragment, preference)
      true
    }
  }

  private fun updateSummary(preference: Preference) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(preference.context)
    preference.setSummary(
      when {
        GeminiApiKey.userKey(prefs).isNotEmpty() -> R.string.summary_pref_gemini_api_key_set
        BuildConfig.GEMINI_API_KEY.isNotBlank() -> R.string.summary_pref_gemini_api_key_built_in
        else -> R.string.summary_pref_gemini_api_key_not_set
      }
    )
  }

  private fun showDialog(fragment: PreferenceFragmentCompat, preference: Preference) {
    val context = fragment.requireContext()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    // A visible password field, so that the key is not learned by the keyboard or offered as a
    // suggestion, but can still be read back after pasting it.
    val field =
      EditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        isSingleLine = true
        setHint(R.string.gemini_api_key_hint)
        setText(GeminiApiKey.userKey(prefs))
      }
    // Lines the field up with the dialog's message.
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    val container =
      FrameLayout(context).apply {
        setPadding(padding, 0, padding, 0)
        addView(field)
      }
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_gemini_api_key)
      .setMessage(R.string.gemini_api_key_dialog_message)
      .setView(container)
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        GeminiApiKey.setUserKey(prefs, field.text.toString())
        updateSummary(preference)
      }
      .setNeutralButton(R.string.gemini_api_key_get) { _, _ ->
        try {
          fragment.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(GeminiApiKey.GET_KEY_URL))
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
        } catch (e: ActivityNotFoundException) {
          // No browser. The message still names the address.
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
