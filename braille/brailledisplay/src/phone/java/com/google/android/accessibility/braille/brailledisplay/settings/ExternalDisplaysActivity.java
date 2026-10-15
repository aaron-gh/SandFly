/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.brailledisplay.settings;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.platform.external.ExternalDisplayApproval;
import com.google.android.accessibility.braille.brailledisplay.platform.external.ExternalDisplayApps;
import com.google.android.accessibility.utils.preference.PreferencesActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lists the apps that can be braille displays, each of which the user can allow or not. */
public class ExternalDisplaysActivity extends PreferencesActivity {
  private static final String TAG = "ExternalDisplaysActivity";

  @Override
  protected PreferenceFragmentCompat createPreferenceFragment() {
    return new ExternalDisplaysFragment();
  }

  @Override
  protected String getFragmentTag() {
    return TAG;
  }

  /** Fragment that lists the apps. */
  public static class ExternalDisplaysFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {}

    @Override
    public void onResume() {
      super.onResume();
      // Apps can be installed while the screen is closed, so the list is built each time it shows.
      buildScreen();
    }

    private void buildScreen() {
      Context context = requireContext();
      PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
      Preference about = new Preference(context);
      about.setSummary(R.string.external_displays_summary);
      about.setSelectable(false);
      about.setIconSpaceReserved(false);
      screen.addPreference(about);

      List<String> apps = new ArrayList<>(ExternalDisplayApps.displayServices(context, 0).keySet());
      apps.sort(
          (a, b) ->
              label(context, a).toLowerCase(Locale.getDefault())
                  .compareTo(label(context, b).toLowerCase(Locale.getDefault())));
      if (apps.isEmpty()) {
        Preference none = new Preference(context);
        none.setSummary(R.string.external_displays_none);
        none.setSelectable(false);
        none.setIconSpaceReserved(false);
        screen.addPreference(none);
      }
      for (String app : apps) {
        SwitchPreference allowed = new SwitchPreference(context);
        allowed.setPersistent(false);
        allowed.setIconSpaceReserved(false);
        allowed.setTitle(label(context, app));
        allowed.setChecked(
            ExternalDisplayApps.state(ExternalDisplayApps.prefs(context), app)
                == ExternalDisplayApps.State.ALLOWED);
        allowed.setOnPreferenceChangeListener(
            (preference, newValue) -> {
              if (!(Boolean) newValue) {
                ExternalDisplayApproval.setAllowed(context, app, false);
                return true;
              }
              // Turning one on warns first, as asking does; the switch moves once the user allows.
              ExternalDisplayApproval.confirm(
                  requireActivity(),
                  app,
                  () -> {
                    ExternalDisplayApproval.setAllowed(context, app, true);
                    allowed.setChecked(true);
                  },
                  () -> ExternalDisplayApproval.setAllowed(context, app, false),
                  () -> {});
              return false;
            });
        screen.addPreference(allowed);
      }
      setPreferenceScreen(screen);
    }

    private static String label(Context context, String app) {
      return ExternalDisplayApps.label(context, app).toString();
    }
  }
}
