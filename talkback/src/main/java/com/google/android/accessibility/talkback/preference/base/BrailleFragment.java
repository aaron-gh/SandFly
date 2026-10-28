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
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;

/** Braille: the braille display and braille keyboard settings. */
public class BrailleFragment extends TalkbackBaseFragment {

  public BrailleFragment() {
    super(R.xml.braille_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_braille);
  }

  /** Returns whether this device can use a braille display or the braille keyboard. */
  public static boolean isSupported(Context context) {
    return FeatureSupport.supportBrailleDisplay(context)
        || FeatureSupport.supportBrailleKeyboard(context);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    if (!FeatureSupport.supportBrailleDisplay(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_brailledisplay_key);
    }
    if (!FeatureSupport.supportBrailleKeyboard(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_brailleime_key);
    } else if (!context
        .getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND)) {
      Preference brailleKeyboard = findPreferenceByResId(R.string.pref_brailleime_key);
      if (brailleKeyboard != null) {
        brailleKeyboard.setEnabled(false);
        brailleKeyboard.setSummary(R.string.summary_pref_brailleime_disabled);
      }
    }
  }
}
