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

import android.content.Context;
import android.os.Bundle;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.backup.SettingsBackup;
import com.google.android.accessibility.talkback.shizuku.PlayInstaller;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.TvTutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.VendorConfigReader;
import com.google.android.accessibility.talkback.update.Updater;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;

/**
 * A small screen of the main settings screen, such as Pause and turn off or Updates and
 * installation. It sets up whichever of the settings below its screen has.
 */
public class AdvancedSettingFragment extends TalkbackBaseFragment {
  private final int titleResId;

  public AdvancedSettingFragment() {
    this(R.xml.pause_preferences, R.string.title_pref_pause);
  }

  protected AdvancedSettingFragment(int xmlResId, int titleResId) {
    super(xmlResId);
    this.titleResId = titleResId;
  }

  @Override
  public CharSequence getTitle() {
    return getText(titleResId);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    // TODO hide preference until the feature stable.
    if (!TalkbackFeatureSupport.supportTalkBackExitBanner(getContext())) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_show_exit_watermark_key);
    }
    Updater.setUpPreferences(this);
    SettingsBackup.setUpPreferences(this);
    setUpHelp();
  }

  /** The tutorial, or on a TV its help screen, and gesture practice. */
  private void setUpHelp() {
    Preference tutorial = findPreference(getString(R.string.pref_tutorial_and_help_key));
    if (tutorial != null) {
      if (FormFactorUtils.isAndroidTv()) {
        tutorial.setTitle(
            TvTutorialInitiator.shouldShowTraining(VendorConfigReader.retrieveConfig(getContext()))
                ? R.string.title_pref_category_tutorial
                : R.string.title_pref_category_help_no_tutorial);
        tutorial.setFragment(TutorialAndHelpFragment.class.getName());
      } else {
        tutorial.setIntent(TutorialInitiator.createTutorialIntent(getActivity()));
      }
    }
    Preference practice = findPreference("pref_practice_gestures_help");
    if (practice != null) {
      if (FormFactorUtils.isAndroidWear() || FormFactorUtils.isAndroidTv()) {
        practice.setVisible(false);
      } else {
        practice.setIntent(TutorialInitiator.createPracticeGesturesIntent(getContext()));
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updatePlayInstaller();
  }

  /**
   * The "Appear installed from Google Play" switch shows whether the user has turned it on, runs
   * the change through Shizuku when toggled, and offers the adb command for someone without
   * Shizuku. When it is on but Android doesn't record Google Play yet, which it can't without
   * Shizuku, the summary says so.
   */
  private void updatePlayInstaller() {
    Context context = getContext();
    Preference preference = findPreference(getString(R.string.pref_play_installer_key));
    if (context == null || !(preference instanceof CommandSwitchPreference)) {
      return;
    }
    CommandSwitchPreference commandPreference = (CommandSwitchPreference) preference;
    boolean wanted = PlayInstaller.INSTANCE.isWanted(context);
    boolean isPlay = PlayInstaller.INSTANCE.isPlay(context);
    commandPreference.setChecked(wanted);
    commandPreference.setSummary(
        wanted && !isPlay
            ? getString(R.string.summary_pref_play_installer_pending)
            : getString(R.string.summary_pref_play_installer));
    commandPreference.setCommand(PlayInstaller.INSTANCE.command(context, !isPlay));
    commandPreference.setOnToggle(
        (checked, done) ->
            PlayInstaller.INSTANCE.set(
                context,
                checked,
                worked -> {
                  done.accept(worked);
                  if (isAdded()) {
                    updatePlayInstaller();
                  }
                }));
  }
}
