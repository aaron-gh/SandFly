/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.google.android.accessibility.talkback.preference.base;

import android.os.Bundle;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;

/**
 * Settings that most people never change. Touch and activation, navigation and the settings for
 * apps have screens of their own on the main settings screen.
 */
public class AdvancedSettingFragment extends TalkbackBaseFragment {

  public AdvancedSettingFragment() {
    super(R.xml.advanced_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_advanced_settings);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    // TODO hide preference until the feature stable.
    if (!TalkbackFeatureSupport.supportTalkBackExitBanner(getContext())) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_show_exit_watermark_key);
    }
  }
}
