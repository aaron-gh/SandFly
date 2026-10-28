/*
 * Copyright 2010 Google Inc.
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

import static com.google.android.accessibility.talkback.trainingcommon.TrainingUtils.GUP_SUPPORT_PORTAL_URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity.HatsRequesterViewModel;
import com.google.android.accessibility.talkback.HatsSurveyRequester;
import com.google.android.accessibility.talkback.HelpAndFeedbackUtils;
import com.google.android.accessibility.talkback.NotificationActivity;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.backup.SettingsBackup;
import com.google.android.accessibility.talkback.TalkBackExitController;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.preference.base.PreferenceActionHelper.WebPage;
import com.google.android.accessibility.talkback.recognition.LiveRecognition;
import com.google.android.accessibility.talkback.training.OnboardingInitiator;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.TvTutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.VendorConfigReader;
import com.google.android.accessibility.talkback.update.Updater;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.NetworkUtils;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;
import java.util.Optional;

/** Fragment that holds the preference of Talkback settings. */
public class TalkBackPreferenceFragment extends TalkbackBaseFragment {
  private Context context;
  private SharedPreferences prefs;
  private SettingsMetricStore settingsMetricStore;

  private Optional<HatsSurveyRequester> hatsSurveyRequester;

  public TalkBackPreferenceFragment() {
    super(R.xml.preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.talkback_preferences_title);
  }

  /**
   * Loads the preferences from the XML preference definition and defines an
   * onPreferenceChangeListener
   */
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();
    if (context == null) {
      return;
    }

    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    settingsMetricStore = new SettingsMetricStore(context);

    fixListSummaries(getPreferenceScreen());

    HatsRequesterViewModel viewModel =
        new ViewModelProvider(getActivity()).get(HatsRequesterViewModel.class);
    hatsSurveyRequester = Optional.ofNullable(viewModel.getHatsSurveyRequester());
    hatsSurveyRequester.ifPresent(
        listener -> listener.setOnSurveyAvailableListener(() -> updateSurveyOption()));

    assignNewFeaturesIntent();

    if (!BrailleFragment.isSupported(context)) {
      Preference braille = findPreference("pref_braille_screen");
      if (braille != null) {
        braille.setVisible(false);
      }
    }

    // During setup, do not allow user access below settings.
    if (!SettingsUtils.allowLinksOutOfSettings(context)) {
      hidePreference(R.string.pref_manage_labels_key);
    }

    if (!ImageCaptioner.supportsImageCaption(context)) {
      hidePreference(R.string.pref_auto_image_captioning_key);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updateSurveyOption();
    updateTtsOverlay();

    // Disable watermark when user enters TalkBack settings.
    disableTalkBackExitWatermark();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    hatsSurveyRequester.ifPresent(requester -> requester.setOnSurveyAvailableListener(null));
  }

  private void hidePreference(int preferenceKeyId) {
    PreferenceSettingsUtils.hidePreference(context, getPreferenceScreen(), preferenceKeyId);
  }

  private void assignNewFeaturesIntent() {
    final Preference prefNewFeatures =
        findPreferenceByResId(R.string.pref_new_feature_in_talkback_entry_point_key);

    if (prefNewFeatures == null) {
      return;
    }

    if (FormFactorUtils.isAndroidTv() || FormFactorUtils.isAndroidXr()) {
      return;
    }

    Intent newFeatureIntent;
    if (FormFactorUtils.isAndroidWear()) {
      newFeatureIntent =
          NotificationActivity.createStartIntent(
              context,
              R.string.wear_new_feature_page_title,
              R.string.wear_new_feature_page_content,
              Integer.MIN_VALUE,
              R.string.wear_new_feature_page_button_content_description,
              /* url= */ null);
    } else {
      newFeatureIntent = OnboardingInitiator.createOnboardingIntentForSettings(context);
    }
    prefNewFeatures.setIntent(newFeatureIntent);
  }

  private void updateSurveyOption() {
    final Preference prefSurvey =
        findPreferenceByResId(R.string.pref_survey_setting_entry_point_key);

    if (prefSurvey == null) {
      return;
    }

    if (hatsSurveyRequester.isEmpty()) {
      prefSurvey.setVisible(false);
      return;
    }

    if (!hatsSurveyRequester.get().isSurveyAvailable()) {
      prefSurvey.setVisible(false);
      return;
    }

    prefSurvey.setVisible(true);
    prefSurvey.setOnPreferenceClickListener(
        preference -> {
          hatsSurveyRequester.ifPresent(
              requester -> {
                boolean unused = requester.presentCachedSurvey();
              });
          prefSurvey.setVisible(false);
          return true;
        });
  }

  private void updateTtsOverlay() {
    final TwoStatePreference preference =
        (TwoStatePreference) findPreferenceByResId(R.string.pref_tts_overlay_key);
    if (preference == null) {
      return;
    }
    boolean prefValue =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            R.string.pref_tts_overlay_key,
            context.getResources().getBoolean(R.bool.pref_tts_overlay_default));
    if (prefValue == preference.isChecked()) {
      return;
    }
    preference.setChecked(prefValue);
  }

  /**
   * Since the "%s" summary is currently broken, this sets the preference change listener for all
   * {@link ListPreference} views to fill in the summary with the current entry value.
   */
  private void fixListSummaries(PreferenceGroup group) {
    if (group == null) {
      return;
    }

    final int count = group.getPreferenceCount();

    for (int i = 0; i < count; i++) {
      final Preference preference = group.getPreference(i);

      if (preference instanceof PreferenceGroup) {
        fixListSummaries((PreferenceGroup) preference);
      } else if (preference instanceof ListPreference) {
        // First make sure the current summary is correct, then set the
        // listener. This is necessary for summaries to show correctly
        // on SDKs < 14.
        preferenceChangeListener.onPreferenceChange(
            preference, ((ListPreference) preference).getValue());

        preference.setOnPreferenceChangeListener(preferenceChangeListener);
      }
    }
  }

  private void disableTalkBackExitWatermark() {
    TalkBackService service = TalkBackService.getInstance();
    if (service == null) {
      return;
    }
    TalkBackExitController talkBackExitController = service.getTalkBackExitController();
    if (talkBackExitController != null && talkBackExitController.isTalkBackExitWatermarkShown()) {
      talkBackExitController.disableTalkBackExitWatermark(service);
    }
  }

  /**
   * Listens for preference changes and updates the summary to reflect the current setting. This
   * shouldn't be necessary, since preferences are supposed to automatically do this when the
   * summary is set to "%s".
   */
  private final OnPreferenceChangeListener preferenceChangeListener =
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          if (preference instanceof ListPreference && newValue instanceof String) {
            final ListPreference listPreference = (ListPreference) preference;
            final int index = listPreference.findIndexOfValue((String) newValue);
            final CharSequence[] entries = listPreference.getEntries();

            // Ignore Setting speech rate summary.
            if (listPreference
                .getKey()
                .equals(getContext().getString(R.string.pref_speech_rate_key))) {
              return true;
            }

            if (index >= 0 && index < entries.length) {
              preference.setSummary(entries[index].toString().replaceAll("%", "%%"));
            } else {
              preference.setSummary("");
            }
          }

          return true;
        }
      };
}
