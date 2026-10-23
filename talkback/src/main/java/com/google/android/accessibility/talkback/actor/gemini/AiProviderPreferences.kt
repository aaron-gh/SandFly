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

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * The AI services in Automatic descriptions settings: which one describes images and screens, and
 * a settings screen for each, such as Gemini settings and Claude settings.
 */
object AiProviderPreferences {
  const val KEY_CATEGORY = "pref_ai_services"
  const val KEY_CHOICE = "pref_ai_provider_choice"
  private const val KEY_PREFIX = "pref_ai_settings_"

  /** Adds a row for each service to the [fragment]'s category, if it has it. */
  @JvmStatic
  fun setUp(fragment: PreferenceFragmentCompat) {
    val category = fragment.findPreference<PreferenceGroup>(KEY_CATEGORY) ?: return
    val context = fragment.requireContext()
    fragment.findPreference<Preference>(KEY_CHOICE)?.setOnPreferenceClickListener {
      choose(fragment)
      true
    }
    for (provider in AiProvider.entries) {
      if (provider == AiProvider.NONE) continue
      if (category.findPreference<Preference>(KEY_PREFIX + provider.id) != null) continue
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = KEY_PREFIX + provider.id
          isPersistent = false
          title = context.getString(R.string.title_ai_provider_settings, context.getString(provider.label))
          setFragment(
            if (provider == AiProvider.GEMINI) GeminiSettingsFragment::class.java.name
            else AiProviderFragment::class.java.name
          )
          extras.putString(AiProviderFragment.ARG_PROVIDER, provider.id)
        }
      )
    }
    refresh(fragment)
  }

  /** Shows which service is in use and how each is set up, after a settings screen closes. */
  @JvmStatic
  fun refresh(fragment: PreferenceFragmentCompat) {
    val context = fragment.context ?: return
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val active = AiProvider.active(prefs)
    fragment.findPreference<Preference>(KEY_CHOICE)?.summary =
      if (active.isReady(prefs)) context.getString(active.label)
      else context.getString(R.string.ai_provider_chosen_not_ready, context.getString(active.label))
    for (provider in AiProvider.entries) {
      val row = fragment.findPreference<Preference>(KEY_PREFIX + provider.id) ?: continue
      row.summary =
        when {
          provider == active && provider.isReady(prefs) -> context.getString(R.string.ai_provider_in_use)
          !provider.isReady(prefs) -> context.getString(R.string.ai_provider_not_set_up)
          else -> null
        }
    }
  }

  private fun choose(fragment: PreferenceFragmentCompat) {
    val context = fragment.requireContext()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    // Only services that are set up are listed; their settings rows below say what each needs.
    val providers = AiProvider.choices(prefs)
    if (providers.size == 1) {
      Toast.makeText(context, R.string.ai_provider_none_set_up, Toast.LENGTH_LONG).show()
      return
    }
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_ai_provider)
      .setSingleChoiceItems(
        providers.map { context.getString(it.label) }.toTypedArray(),
        providers.indexOf(AiProvider.active(prefs)),
      ) { dialog, which ->
        dialog.dismiss()
        AiProvider.setActive(prefs, providers[which])
        refresh(fragment)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
