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

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import com.google.android.accessibility.talkback.R;

/** Volume keys: what the volume keys do while SandFly is on. */
public class VolumeKeysFragment extends AdvancedSettingFragment {
  public VolumeKeysFragment() {
    super(R.xml.volume_keys_preferences, R.string.title_pref_volume_keys);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    ListPreference feedback = findPreference(getString(R.string.pref_volume_change_feedback_key));
    if (feedback == null) {
      return;
    }
    showMediaSwitches(feedback.getValue());
    feedback.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          showMediaSwitches((String) newValue);
          return true;
        });
  }

  /** Shows the media playback switch of the tone and of speech only when {@code value} has it. */
  private void showMediaSwitches(@Nullable String value) {
    boolean both = getString(R.string.value_volume_change_feedback_tone_and_spoken).equals(value);
    setVisible(
        R.string.pref_volume_change_tone_during_media_key,
        both || getString(R.string.value_volume_change_feedback_tone).equals(value));
    setVisible(
        R.string.pref_volume_change_speech_during_media_key,
        both || getString(R.string.value_volume_change_feedback_spoken).equals(value));
  }

  private void setVisible(int keyResId, boolean visible) {
    Preference preference = findPreference(getString(keyResId));
    if (preference != null) {
      preference.setVisible(visible);
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof GestureListPreference gesturePreference) {
      GestureListPreference.showCommandList(this, gesturePreference);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    // On a watch the command list is a screen of its own, which changes the command meanwhile.
    PreferenceGroup screen = getPreferenceScreen();
    for (int i = 0; i < screen.getPreferenceCount(); i++) {
      if (screen.getPreference(i) instanceof GestureListPreference preference) {
        preference.refresh();
      }
    }
  }
}
