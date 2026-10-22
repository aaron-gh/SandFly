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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment

/** Gemini settings in Automatic descriptions: the API key and the model. */
class GeminiSettingsFragment : TalkbackBaseFragment(R.xml.gemini_settings_preferences) {
  override fun getTitle(): CharSequence =
    getString(R.string.title_ai_provider_settings, getString(R.string.ai_provider_gemini))

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    GeminiApiKeyPreference.setUp(this)
    GeminiModelPreference.setUp(this)
  }
}
