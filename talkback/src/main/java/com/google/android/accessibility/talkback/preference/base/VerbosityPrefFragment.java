/*
 * Copyright 2017 Google Inc.
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
import android.text.TextUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter;
import com.google.android.accessibility.talkback.monitor.BatteryLevelAnnouncements;
import com.google.android.accessibility.talkback.notifications.VoiceChoices;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.EmojiSpeech;
import com.google.android.accessibility.utils.output.SpeechCleanupUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Panel holding the verbosity preferences: what is spoken for items, text and screen changes. */
public class VerbosityPrefFragment extends TalkbackBaseFragment {
  private static final String TAG = "VerbosityPrefFragment";

  private SharedPreferences preferences;
  @Nullable private TwoStatePreference tellTimePreference;

  public VerbosityPrefFragment() {
    super(R.xml.verbosity_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_verbosity_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    preferences = SharedPreferencesUtils.getSharedPreferences(getContext());
    // Before the list shows its value, as when SandFly hasn't run since the switch it replaced.
    EmojiSpeech.migrateSuffix(preferences);
    super.onCreatePreferences(savedInstanceState, rootKey);
    setupTellTimePreference();
    setupEmojiRepeatCountPreference();
    setupSpeakUserFocusPreference();
    setupPunctuationNonePreference();
    setupBatteryLevelVoicePreference();
  }

  @Override
  public void onStart() {
    super.onStart();
    // Voice profiles may have been added or renamed on their own screen.
    setupBatteryLevelVoicePreference();
  }

  /** Lists the notification voice, SandFly's voice and the voice profiles for the battery level. */
  private void setupBatteryLevelVoicePreference() {
    Preference voice = findPreference(BatteryLevelAnnouncements.KEY_VOICE);
    if (voice instanceof ListPreference voiceList && preferences != null) {
      VoiceChoices.setUp(requireContext(), preferences, voiceList, /* notificationVoice= */ true);
    }
  }

  /** Say symbols that change meaning is shown only while Speak punctuation and symbols is None. */
  private void setupPunctuationNonePreference() {
    Preference punctuation = findPreference(R.string.pref_punctuation_verbosity);
    if (!(punctuation instanceof ListPreference punctuationList)) {
      return;
    }
    updatePunctuationNonePreference(punctuationList.getValue());
    punctuation.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          updatePunctuationNonePreference(String.valueOf(newValue));
          return true;
        });
  }

  private void updatePunctuationNonePreference(@Nullable String punctuation) {
    Preference keepMeaning = findPreference(R.string.pref_punctuation_none_keep_meaning_key);
    if (keepMeaning != null) {
      keepMeaning.setVisible(String.valueOf(SpeechCleanupUtils.NONE).equals(punctuation));
    }
  }

  /** Chosen apps only applies while Speak only focus moved by the user is In chosen apps. */
  private void setupSpeakUserFocusPreference() {
    Preference mode = findPreference(R.string.pref_speak_user_focus_key);
    if (!(mode instanceof ListPreference modeList)) {
      return;
    }
    updateSpeakUserFocusApps(modeList.getValue());
    mode.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          updateSpeakUserFocusApps(String.valueOf(newValue));
          return true;
        });
  }

  private void updateSpeakUserFocusApps(@Nullable String mode) {
    Preference apps = findPreference(R.string.pref_speak_user_focus_apps_key);
    if (apps != null) {
      apps.setEnabled(AppSpeechFilter.MODE_CHOSEN_APPS.equals(mode));
    }
  }

  /**
   * Counting repeated emoji only applies while SandFly names emoji; After emoji names also applies
   * while the speech engine reads them.
   */
  private void setupEmojiRepeatCountPreference() {
    Preference emoji = findPreference(R.string.pref_emoji_speech_key);
    Preference repeatCount = findPreference(R.string.pref_emoji_repeat_count_key);
    Preference afterName = findPreference(EmojiSpeech.PREF_AFTER_NAME);
    if (!(emoji instanceof ListPreference emojiList) || repeatCount == null) {
      return;
    }
    boolean names = EmojiSpeech.MODE_NAMES.equals(emojiList.getValue());
    repeatCount.setEnabled(names);
    if (afterName != null) {
      afterName.setEnabled(!EmojiSpeech.MODE_NONE.equals(emojiList.getValue()));
    }
    emoji.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          boolean namesNow = EmojiSpeech.MODE_NAMES.equals(newValue);
          repeatCount.setEnabled(namesNow);
          if (afterName != null) {
            afterName.setEnabled(!EmojiSpeech.MODE_NONE.equals(newValue));
          }
          return true;
        });
  }

  private void setupTellTimePreference() {
    tellTimePreference = (TwoStatePreference) findPreference(R.string.pref_speak_time_key);
    if (tellTimePreference != null) {
      tellTimePreference.setOnPreferenceClickListener(
          preference -> {
            TalkBackService service = TalkBackService.getInstance();
            if (service != null) {
              service
                  .getAnalytics()
                  .onManuallyChangeSetting(
                      getString(R.string.pref_speak_time_key),
                      TalkBackAnalytics.TYPE_PREFERENCE_SETTING);
            }
            return false;
          });
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    // Only PunctuationListPreference customizes the preference dialog.
    if (preference instanceof PunctuationListPreference) {
      PreferenceDialogFragmentCompat dialogFragment =
          PunctuationListPreference.CustomListPreferenceDialogFragment.newInstance(
              preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getParentFragmentManager(), "dialog_preference");
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Preference itemDetailsOrder = findPreference(R.string.pref_node_desc_order_key);
    if (itemDetailsOrder != null) {
      itemDetailsOrder.setSummary(ItemDetailsOrderFragment.getSummary(getContext()));
    }
    preferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  @Override
  public void onPause() {
    super.onPause();
    preferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  /** Shows changes made outside this screen, such as by a keyboard shortcut, while it is open. */
  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      (prefs, key) -> {
        if (!isAdded() || (getActivity() == null)) {
          LogUtils.w(
              TAG, "Fragment is not attached to activity, do not update verbosity setting page.");
          return;
        }
        if (TextUtils.equals(key, getString(R.string.pref_punctuation_key))) {
          if (findPreference(R.string.pref_punctuation_key) instanceof TwoStatePreference p) {
            p.setChecked(
                prefs.getBoolean(key, getResources().getBoolean(R.bool.pref_punctuation_default)));
          }
        } else if (TextUtils.equals(key, getString(R.string.pref_punctuation_verbosity))) {
          if (findPreference(R.string.pref_punctuation_verbosity) instanceof ListPreference p) {
            p.setValue(prefs.getString(key, getString(R.string.pref_punctuation_verbosity_default)));
            updatePunctuationNonePreference(p.getValue());
          }
        } else if (TextUtils.equals(key, getString(R.string.pref_speak_user_focus_key))) {
          // Changed by the reading control or the command.
          if (findPreference(R.string.pref_speak_user_focus_key) instanceof ListPreference p) {
            String mode = AppSpeechFilter.mode(requireContext(), prefs);
            p.setValue(mode);
            updateSpeakUserFocusApps(mode);
          }
        } else if (TextUtils.equals(key, getString(R.string.pref_speak_time_key))) {
          if (tellTimePreference != null) {
            tellTimePreference.setChecked(
                prefs.getBoolean(key, getResources().getBoolean(R.bool.pref_tell_time_default)));
          }
        }
      };

  private Preference findPreference(int keyId) {
    return getPreferenceScreen().findPreference(getString(keyId));
  }
}
