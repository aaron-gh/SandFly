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

/**
 * AI services: the AI service in use, the settings of each service, such as its API key and model,
 * and the on-device model.
 */
class AiServicesFragment : TalkbackBaseFragment(R.xml.ai_services_preferences) {

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_category_ai_services)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    AiProviderPreferences.setUp(this)
  }

  override fun onResume() {
    super.onResume()
    AiProviderPreferences.refresh(this)
  }
}
