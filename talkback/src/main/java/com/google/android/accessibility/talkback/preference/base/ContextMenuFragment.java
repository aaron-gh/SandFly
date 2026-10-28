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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.contextmenu.TalkbackMenuProcessor;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.preference.ItemOrder;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.List;

/** SandFly menu: the items in the menu, in order, and those that aren't. */
public class ContextMenuFragment extends ItemListFragment {

  // Prevents from runtime exception that will occur in some cases during state restore if the
  // no-argument constructor is not available.
  public ContextMenuFragment() {
    super(R.xml.context_menu_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_category_context_menu);
  }

  @Override
  public void onCreate(Bundle bundle) {
    PreferencesActivityUtils.removeEditingKey(getContext());
    super.onCreate(bundle);
  }

  @Override
  protected String getOrderKey() {
    return ItemOrder.SANDFLY_MENU_KEY;
  }

  @Override
  protected int getNotInListTitle() {
    return R.string.item_list_not_in_sandfly_menu;
  }

  @Override
  protected List<String> defaultOrder(Context context) {
    return TalkbackMenuProcessor.defaultOrderKeys(context);
  }

  @Override
  protected void hideUnavailable(PreferenceScreen screen) {
    Context context = requireContext();
    hide(
        screen,
        R.string.pref_show_context_menu_summarize_view_setting_key,
        ImageCaptioner.supportsImageCaption(context));
    Preference screenOverview =
        screen.findPreference(
            context.getString(R.string.pref_show_context_menu_summarize_view_setting_key));
    if (screenOverview != null && GeminiConfiguration.isScreenQnaActionsEnabled(context)) {
      screenOverview.setSummary(R.string.summary_screen_overview);
    }
    hide(
        screen,
        R.string.pref_show_context_menu_image_caption_setting_key,
        ImageCaptioner.supportsImageCaption(context));
    hide(
        screen,
        R.string.pref_show_context_menu_show_keyboard_shortcuts_setting_key,
        FeatureFlagReader.enableShowKeyboardShortcutsDialog(context));
    updateNavigationOptions(screen);
  }

  private void hide(PreferenceScreen screen, int keyResId, boolean visible) {
    Preference preference = screen.findPreference(getString(keyResId));
    if (preference != null && !visible) {
      preference.setVisible(false);
    }
  }

  @Override
  protected void onMembershipChanged(String key, boolean inList) {
    if (TextUtils.equals(key, getString(R.string.pref_show_context_menu_granularity_setting_key))) {
      updateNavigationOptions(getPreferenceScreen());
    }
  }

  /** The navigation options only apply while Navigation is in the menu. */
  private void updateNavigationOptions(PreferenceScreen screen) {
    Preference navigationOptions =
        screen.findPreference(
            getString(R.string.pref_show_context_menu_granularity_detail_setting_key));
    if (navigationOptions == null) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(requireContext());
    navigationOptions.setEnabled(
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            getResources(),
            R.string.pref_show_context_menu_granularity_setting_key,
            R.bool.pref_show_context_menu_granularity_default));
  }
}
