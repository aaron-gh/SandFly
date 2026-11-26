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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.keyboard.KeypadKeys;
import com.google.android.accessibility.talkback.keyboard.SandFlyKeyboardHost;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;

/** Typing: the on-screen keyboard, keyboard echo, dictation and other ways to type. */
public class TypingFragment extends TalkBackLatencyReductionPreferenceFragment {

  public TypingFragment() {
    super(R.xml.typing_preferences, R.string.title_pref_typing);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Preference deleteLearned =
        findPreferenceByResId(R.string.pref_sandfly_keyboard_delete_learned_words_key);
    if (deleteLearned != null) {
      deleteLearned.setOnPreferenceClickListener(
          preference -> {
            Context context = getContext();
            if (context != null) {
              SandFlyKeyboardHost.deleteLearnedWords(context);
              Toast.makeText(
                      context, R.string.sandfly_keyboard_learned_words_deleted, Toast.LENGTH_SHORT)
                  .show();
            }
            return true;
          });
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updatePhysicalKeyboardPreference();
  }

  /**
   * Shows the physical keyboard settings only while a keyboard is connected, and letter keys on
   * keypads only for keyboards whose letters carry numbers.
   */
  private void updatePhysicalKeyboardPreference() {
    Context context = getContext();
    Preference preference = findPreferenceByResId(R.string.pref_physical_keyboard_key);
    if (context == null || preference == null) {
      return;
    }
    boolean connected = new InputDeviceMonitor(context).hasPhysicalKeyboard();
    Preference category = findPreference("pref_typing_category_physical_keyboard");
    if (category != null) {
      category.setVisible(connected);
    }
    Preference keypadKeys = findPreferenceByResId(R.string.pref_keypad_letter_keys_key);
    if (keypadKeys != null) {
      keypadKeys.setVisible(KeypadKeys.isAvailable());
    }
    if (connected) {
      Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      if (PreferenceSettingsUtils.canHandleIntent(context, intent)) {
        preference.setIntent(intent);
      }
    }
    preference.setVisible(connected);
  }
}
