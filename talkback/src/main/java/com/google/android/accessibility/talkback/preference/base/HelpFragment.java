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

package com.google.android.accessibility.talkback.preference.base;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.userguide.UserGuideActivity;
import com.google.android.accessibility.utils.FormFactorUtils;

/**
 * Help and feedback: the user guide, the tutorial, gesture practice, keyboard practice, problem
 * reports, the version history and the privacy policy.
 */
public class HelpFragment extends AdvancedSettingFragment {
  private static final String USER_GUIDE_KEY = "pref_user_guide";
  private static final String KEY_PRACTICE_KEY = "pref_key_practice_help";

  public HelpFragment() {
    super(R.xml.help_and_feedback_preferences, R.string.title_pref_help_and_feedback);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Preference userGuide = findPreference(USER_GUIDE_KEY);
    if (userGuide != null) {
      if (UserGuideActivity.isAvailable()) {
        userGuide.setIntent(UserGuideActivity.intent(requireContext()));
      } else {
        userGuide.setVisible(false);
      }
    }
    Preference keyPractice = findPreference(KEY_PRACTICE_KEY);
    if (keyPractice != null) {
      if (FormFactorUtils.isAndroidWear()) {
        keyPractice.setVisible(false);
      } else {
        keyPractice.setOnPreferenceClickListener(
            clicked -> {
              TalkBackKeyboardShortcutPreferenceFragment.startKeyPractice();
              return true;
            });
      }
    }
    opensUrl(R.string.pref_version_history_key, R.string.version_history_url);
    opensUrl(R.string.pref_privacy_policy_key, R.string.privacy_policy_url);
  }

  @Override
  public void onResume() {
    super.onResume();
    // Keyboard practice runs in SandFly, so it can only turn on while SandFly is on.
    Preference keyPractice = findPreference(KEY_PRACTICE_KEY);
    if (keyPractice != null) {
      keyPractice.setEnabled(TalkBackService.isServiceActive());
    }
  }

  /** Makes the preference with the key {@code keyRes} open the address {@code urlRes}. */
  private void opensUrl(@StringRes int keyRes, @StringRes int urlRes) {
    Preference preference = findPreference(getString(keyRes));
    if (preference == null) {
      return;
    }
    preference.setOnPreferenceClickListener(
        clicked -> {
          String url = getString(urlRes);
          try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
          } catch (ActivityNotFoundException e) {
            // Watches often have no browser: show the address to open elsewhere.
            Toast.makeText(getContext(), url, Toast.LENGTH_LONG).show();
          }
          return true;
        });
  }
}
