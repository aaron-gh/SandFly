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

import android.os.Bundle
import androidx.preference.TwoStatePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** Gemini settings in Automatic descriptions: whether to use Gemini, its API key and its model. */
class GeminiSettingsFragment : TalkbackBaseFragment(R.xml.gemini_settings_preferences) {
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(requireContext()) }

  override fun getTitle(): CharSequence =
    getString(R.string.title_ai_provider_settings, getString(R.string.ai_provider_gemini))

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    findPreference<TwoStatePreference>(KEY_USE)?.setOnPreferenceChangeListener { _, value ->
      AiProvider.setActive(prefs, if (value as Boolean) AiProvider.GEMINI else AiProvider.NONE)
      true
    }
    GeminiApiKeyPreference.setUp(this) {
      // Without a key, Gemini can't be asked, so it is no longer the service in use.
      if (AiProvider.active(prefs) == AiProvider.GEMINI && !AiProvider.GEMINI.isReady(prefs)) {
        AiProvider.setActive(prefs, AiProvider.NONE)
      }
      updateUse()
    }
    GeminiModelPreference.setUp(this)
    updateUse()
  }

  override fun onResume() {
    super.onResume()
    updateUse()
  }

  /** Only Gemini with a key can be turned on. */
  private fun updateUse() {
    val use = findPreference<TwoStatePreference>(KEY_USE) ?: return
    val ready = AiProvider.GEMINI.isReady(prefs)
    use.isChecked = ready && AiProvider.active(prefs) == AiProvider.GEMINI
    use.isEnabled = ready
    use.summary =
      if (ready) null
      else getString(R.string.ai_provider_needs_key, getString(R.string.ai_provider_gemini))
  }

  private companion object {
    const val KEY_USE = "pref_gemini_use"
  }
}
