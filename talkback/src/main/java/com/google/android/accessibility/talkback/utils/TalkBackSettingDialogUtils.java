/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.fragment.app.Fragment;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.preference.base.TypingFocusDelayPrefFragment;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility class for TalkBack fragments to get common functionalities. */
public class TalkBackSettingDialogUtils {

  private TalkBackSettingDialogUtils() {}

  /**
   * This is used to decide whether an alert dialog is shown to inform user the typing latency can
   * be configured by reading control.
   *
   * @param fragment the fragment asking whether to show the alert dialog.
   * @param prefs the shared preference.
   * @param context the context when calling this method.
   * @return the alert dial itself or {@code null} if no dialog to be shown.
   */
  public static @Nullable A11yAlertDialogWrapper createDialogForAddReadingControlItem(
      Fragment fragment, SharedPreferences prefs, Context context) {
    if (!FeatureFlagReader.typingFocusTimeout(context)) {
      return null;
    }
    SharedPreferencesUtils.putStringPref(
        prefs,
        context.getResources(),
        R.string.pref_typing_focus_time_out_key,
        String.valueOf(TypingFocusDelayPrefFragment.TypingFocusDelayPref.DELAY_150_MS.getDelay()));
    // Changing a delay doesn't add its reading control; the user adds the ones they want.
    return null;
  }
}
