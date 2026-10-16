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

import static com.google.android.accessibility.utils.SettingsUtils.isTouchExplorationEnabled;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.preference.base.PreferenceActionHelper.WebPage;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.PackageManagerUtils;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Fragment to display advanced settings. */
public class AdvancedSettingFragment extends TalkbackBaseFragment {
  private static final String TAG = "AdvancedSettingFragment";

  private Context context;

  private SharedPreferences prefs;

  private static final Pattern TALKBACK_VERSION_PATTERN = Pattern.compile("[0-9]+\\.[0-9]+");

  public AdvancedSettingFragment() {
    super(R.xml.advanced_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_advanced_settings);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTouchExplorationState();
    updateTypingMethodPreference();
  }

  private void updateTypingMethodPreference() {
    AccessibilitySuitePreference typingMethodPreference =
        (AccessibilitySuitePreference)
            findPreference(getString(R.string.pref_typing_confirmation_key));
    if (typingMethodPreference != null) {
      typingMethodPreference.setSummaryProvider(
          preference -> {
            int method =
                SharedPreferencesUtils.getIntFromStringPref(
                    prefs,
                    context.getResources(),
                    R.string.pref_typing_confirmation_key,
                    R.string.pref_typing_confirmation_default);

            String[] typingValues =
                context.getResources().getStringArray(R.array.pref_typing_types_talkback_values);
            int position = Integer.parseInt(typingValues[typingValues.length - 1]);
            for (int index = 0; index < typingValues.length; index++) {
              if (method == Integer.parseInt(typingValues[index])) {
                position = index;
                break;
              }
            }
            return context.getResources()
                .getStringArray(R.array.pref_typing_types_talkback_entries)[position];
          });
    }
  }

  /**
   * Updates the preferences state to match the actual state of touch exploration. This is called
   * once when the preferences activity launches and again whenever the actual state of touch
   * exploration changes.
   */
  private void updateTouchExplorationState() {
    final ContentResolver resolver = context.getContentResolver();
    final Resources res = getResources();
    final boolean requestedState =
        SharedPreferencesUtils.getBooleanPref(
            prefs, res, R.string.pref_explore_by_touch_key, R.bool.pref_explore_by_touch_default);
    final boolean actualState;

    // If accessibility is disabled then touch exploration is always
    // disabled, so the "actual" state should just be the requested state.
    if (TalkBackService.isServiceActive()) {
      actualState = isTouchExplorationEnabled(resolver);
    } else {
      actualState = requestedState;
    }

    // TODO: Use SharedViewModel mechanism to handle this across TalkBack settings fragments.
    // Enable/disable preferences that depend on explore-by-touch.
    // Cannot use "dependency" attribute in preferences XML file, because touch-explore-preference
    // is in a different preference-activity (developer preferences).
    Preference singleTapPref = findPreference(getString(R.string.pref_single_tap_key));
    if (singleTapPref != null) {
      singleTapPref.setEnabled(actualState);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!FormFactorUtils.isAndroidWear()) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_reserve_vibration_watch_gestures_key);
    }

    // During setup, do not allow user access below settings.
    if (!SettingsUtils.allowLinksOutOfSettings(context)) {
      PreferenceGroup controlCategory =
          (PreferenceGroup) findPreferenceByResId(R.string.pref_category_controls_key);
      if (controlCategory != null) {
        PreferenceSettingsUtils.hidePreference(
            context, controlCategory, R.string.pref_manage_labels_key);
      }
      // Legal and privacy settings that use WebView.
      PreferenceGroup otherCategory =
          (PreferenceGroup) findPreferenceByResId(R.string.pref_category_others_key);
      if (otherCategory != null) {
        PreferenceSettingsUtils.hidePreference(context, otherCategory, R.string.pref_policy_key);
        PreferenceSettingsUtils.hidePreference(context, otherCategory, R.string.pref_show_tos_key);
      }
    }

    // Link preferences to web-viewer. The behavior depends on the type of form factors.
    if (findPreference(getString(R.string.pref_policy_key)) != null) {
      PreferenceActionHelper.assignWebIntentToPreference(
          this,
          findPreference(getString(R.string.pref_policy_key)),
          WebPage.WEB_PAGE_PRIVACY_POLICY);
    }
    if (findPreference(getString(R.string.pref_show_tos_key)) != null) {
      PreferenceActionHelper.assignWebIntentToPreference(
          this,
          findPreference(getString(R.string.pref_show_tos_key)),
          WebPage.WEB_PAGE_TERMS_OF_SERVICE);
    }

    updatePlayStorePreference();

    // Hiding the smart browse mode toggle if the feature is not enabled.
    if (!FeatureFlagReader.enableSmartBrowseMode(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_smart_browse_mode_key);
    }

    // TODO hide preference until the feature stable.
    if (!TalkbackFeatureSupport.supportTalkBackExitBanner(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_show_exit_watermark_key);
    }
  }

  private void updatePlayStorePreference() {
    if (SettingsUtils.allowLinksOutOfSettings(context) || FormFactorUtils.isAndroidTv()) {
      // We should never try to open the play store in WebActivity.
      showTalkBackVersion();
      assignPlayStoreIntentToPreference();
    } else {
      // During setup, do not allow access to web.
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_play_store_key);
    }
  }

  @VisibleForTesting
  public boolean supportsPlayStore() {
    return PackageManagerUtils.hasGmsCorePackage(context);
  }

  private void assignPlayStoreIntentToPreference() {

    Preference pref = findPreferenceByResId(R.string.pref_play_store_key);
    if (pref == null) {
      return;
    }

    PreferenceGroup category =
        (PreferenceGroup) findPreferenceByResId(R.string.pref_category_others_key);
    // Hides the play store preference if device has no Google play store.
    if (!getResources().getBoolean(R.bool.show_play_store) || !supportsPlayStore()) {
      if (category != null) {
        category.removePreference(pref);
      }
      return;
    }

    String packageName = PackageManagerUtils.TALKBACK_PACKAGE;

    Uri uri;
    if (FormFactorUtils.isAndroidWear()) {
      // Only for watches, try the "market://" URL first. If there is a Play Store on the
      // device, this should succeed. Only for LE devices, there will be no Play Store.
      uri = Uri.parse("market://details?id=" + packageName);
    } else {
      uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
    }

    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    if (PreferenceSettingsUtils.canHandleIntent(context, intent)) {
      pref.setIntent(intent);
    } else {
      if (category != null) {
        category.removePreference(pref);
      }
    }
  }

  private static @Nullable PackageInfo getPackageInfo(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    } catch (NameNotFoundException e) {
      return null;
    }
  }

  /** Show TalkBack version in the Play Store button. */
  private void showTalkBackVersion() {
    PackageInfo packageInfo = getPackageInfo(context);
    if (packageInfo == null) {
      return;
    }
    final Preference playStoreButton = findPreferenceByResId(R.string.pref_play_store_key);
    if (playStoreButton == null) {
      return;
    }

    Matcher matcher = TALKBACK_VERSION_PATTERN.matcher(String.valueOf(packageInfo.versionName));
    String summary;
    if (matcher.find()) {
      summary = getString(R.string.summary_pref_play_store, matcher.group());
    } else {
      summary =
          getString(R.string.summary_pref_play_store, String.valueOf(packageInfo.versionName));
    }
    playStoreButton.setSummary(summary);
  }
}
