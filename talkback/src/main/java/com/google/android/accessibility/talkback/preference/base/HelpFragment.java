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
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;

/** Help and feedback: the tutorial, gesture practice, problem reports and the privacy policy. */
public class HelpFragment extends AdvancedSettingFragment {
  public HelpFragment() {
    super(R.xml.help_and_feedback_preferences, R.string.title_pref_help_and_feedback);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Preference privacy = findPreference(getString(R.string.pref_privacy_policy_key));
    if (privacy != null) {
      privacy.setOnPreferenceClickListener(
          preference -> {
            String url = getString(R.string.privacy_policy_url);
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
}
