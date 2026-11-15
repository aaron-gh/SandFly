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

package com.google.android.accessibility.talkback.preference.base;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Settings for where focus goes as the user moves through the screen. */
public class NavigationPrefFragment extends TalkbackBaseFragment {

  /** Shows Wrap around in reading controls following Wrap around while it isn't set. */
  private final OnSharedPreferenceChangeListener wrapListener =
      (prefs, key) -> {
        if (getString(R.string.pref_wrap_around_key).equals(key)) {
          showReadingControlsWrap(prefs);
        }
      };

  public NavigationPrefFragment() {
    super(R.xml.navigation_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_navigation);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    if (!FeatureFlagReader.enableSmartBrowseMode(getContext())) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_smart_browse_mode_key);
    }
    if (!FeatureFlagReader.enableBrowseMode(getContext())) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_browse_mode_per_app_key);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(getContext());
    showReadingControlsWrap(prefs);
    prefs.registerOnSharedPreferenceChangeListener(wrapListener);
  }

  @Override
  public void onPause() {
    SharedPreferencesUtils.getSharedPreferences(getContext())
        .unregisterOnSharedPreferenceChangeListener(wrapListener);
    super.onPause();
  }

  /**
   * Shows the value of Wrap around in reading controls, without saving it, while it isn't set and
   * follows Wrap around.
   */
  private void showReadingControlsWrap(SharedPreferences prefs) {
    String key = getString(R.string.pref_wrap_around_reading_controls_key);
    TwoStatePreference preference = findPreference(key);
    if (preference == null || prefs.contains(key)) {
      return;
    }
    preference.setPersistent(false);
    preference.setChecked(
        SelectorController.wrapsReadingControlValues(prefs, getResources()));
    preference.setPersistent(true);
  }
}
