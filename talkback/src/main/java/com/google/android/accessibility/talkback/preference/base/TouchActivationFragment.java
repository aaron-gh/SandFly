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

import static com.google.android.accessibility.utils.SettingsUtils.isTouchExplorationEnabled;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Touch: how touching the screen focuses and activates items. */
public class TouchActivationFragment extends TalkBackLatencyReductionPreferenceFragment {

  public TouchActivationFragment() {
    super(R.xml.touch_activation_preferences, R.string.title_pref_touch);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateSingleTap();
    updateStuckTouchPreference();
  }

  /**
   * Without the DUMP permission, fixing stuck touch exploration can only warn, so the setting says
   * how to grant it and has an action that copies the command.
   */
  private void updateStuckTouchPreference() {
    Context context = getContext();
    Preference preference =
        findPreference(getString(R.string.pref_fix_stuck_touch_exploration_key));
    if (context == null || preference == null) {
      return;
    }
    boolean canFix =
        context.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED;
    preference.setSummary(
        canFix
            ? null
            : getString(
                R.string.summary_pref_fix_stuck_touch_exploration_no_dump,
                context.getPackageName()));
    if (preference instanceof CommandSwitchPreference) {
      CommandSwitchPreference commandPreference = (CommandSwitchPreference) preference;
      commandPreference.setCommand(
          canFix ? null : "adb shell pm grant " + context.getPackageName() + " android.permission.DUMP");
      commandPreference.setGrant(ShizukuGrants.Grant.DUMP);
      commandPreference.setOnGrantFinished(
          () -> {
            if (isAdded()) {
              updateStuckTouchPreference();
            }
          });
    }
  }

  /**
   * Single tap needs explore by touch, which is set in the developer settings, so it can't be a
   * dependency in the XML. Without Backtalk running, explore by touch is off, so the setting the
   * user asked for counts instead.
   */
  private void updateSingleTap() {
    Context context = getContext();
    Preference singleTap = findPreference(getString(R.string.pref_single_tap_key));
    if (context == null || singleTap == null) {
      return;
    }
    boolean touchExploration =
        TalkBackService.isServiceActive()
            ? isTouchExplorationEnabled(context.getContentResolver())
            : SharedPreferencesUtils.getBooleanPref(
                SharedPreferencesUtils.getSharedPreferences(context),
                getResources(),
                R.string.pref_explore_by_touch_key,
                R.bool.pref_explore_by_touch_default);
    singleTap.setEnabled(touchExploration);
  }
}
