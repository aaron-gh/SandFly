/*
 * Copyright 2025 Google Inc.
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
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Fragment holding a set of onscreen keyboard preferences. */
public class OnScreenKeyboardFragment extends TalkbackBaseFragment {
  private static final String TAG = "OnScreenKeyboardFragment";
  private Context context;
  private SharedPreferences prefs;

  public OnScreenKeyboardFragment() {
    super(R.xml.on_screen_keyboard_preferences);
  }

  @Override
  protected CharSequence getTitle() {
    return getText(R.string.title_pref_on_screen_keyboard);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();

    if (context == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
  }

  @Override
  public void onResume() {
    super.onResume();

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
}
