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

package com.google.android.accessibility.talkback.actor.gemini

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.IOException
import java.util.concurrent.Executors

/**
 * The "Gemini model" preference in Automatic descriptions settings: Automatic, or one of the models
 * the user's key can use, listed by Google when the setting is opened.
 */
object GeminiModelPreference {
  private val executor = Executors.newSingleThreadExecutor()
  private val main = Handler(Looper.getMainLooper())

  /** Sets up the preference, if the [fragment] has it. */
  @JvmStatic
  fun setUp(fragment: PreferenceFragmentCompat) {
    val preference = fragment.findPreference<Preference>(GeminiModel.PREF_KEY) ?: return
    updateSummary(preference)
    preference.setOnPreferenceClickListener {
      choose(fragment, preference)
      true
    }
  }

  private fun updateSummary(preference: Preference) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(preference.context)
    val chosen = GeminiModel.chosen(prefs)
    preference.summary =
      if (chosen.isEmpty()) preference.context.getString(R.string.summary_pref_gemini_model_automatic)
      else preference.context.getString(R.string.summary_pref_gemini_model_chosen, chosen)
  }

  private fun choose(fragment: PreferenceFragmentCompat, preference: Preference) {
    val context = fragment.requireContext().applicationContext
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val key = GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY)
    if (key.isEmpty()) {
      Toast.makeText(context, R.string.gemini_model_needs_key, Toast.LENGTH_LONG).show()
      return
    }
    Toast.makeText(context, R.string.gemini_model_loading, Toast.LENGTH_SHORT).show()
    executor.execute {
      val result = runCatching { GeminiModel.list(key) }
      main.post {
        if (!fragment.isAdded) return@post
        result.fold(
          { models -> showChoices(fragment, preference, models) },
          { error ->
            val reason = (error as? IOException)?.message ?: error.javaClass.simpleName
            Toast.makeText(context, context.getString(R.string.gemini_model_list_failed, reason), Toast.LENGTH_LONG)
              .show()
          },
        )
      }
    }
  }

  private fun showChoices(
    fragment: PreferenceFragmentCompat,
    preference: Preference,
    models: List<GeminiModel.Choice>,
  ) {
    val context = fragment.requireContext()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val chosen = GeminiModel.chosen(prefs)
    val ids = listOf("") + models.map { it.id }
    val labels =
      listOf(context.getString(R.string.gemini_model_automatic)) +
        models.map { if (it.name == it.id) it.id else "${it.name}, ${it.id}" }
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_gemini_model)
      .setSingleChoiceItems(labels.toTypedArray(), ids.indexOf(chosen).coerceAtLeast(0)) { dialog, which ->
        GeminiModel.setChosen(prefs, ids[which])
        updateSummary(preference)
        dialog.dismiss()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
