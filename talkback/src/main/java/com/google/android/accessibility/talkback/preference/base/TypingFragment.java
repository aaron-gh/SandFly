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
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;

/** Typing: dictation, keyboard echo, the keyboards and speech while typing. */
public class TypingFragment extends TalkbackBaseFragment {

  public TypingFragment() {
    super(R.xml.typing_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_typing);
  }

  @Override
  public void onResume() {
    super.onResume();
    updatePhysicalKeyboardPreference();
  }

  /** Opens the system's physical keyboard settings, shown only while a keyboard is connected. */
  private void updatePhysicalKeyboardPreference() {
    Context context = getContext();
    Preference preference = findPreferenceByResId(R.string.pref_physical_keyboard_key);
    if (context == null || preference == null) {
      return;
    }
    boolean connected = new InputDeviceMonitor(context).hasPhysicalKeyboard();
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
