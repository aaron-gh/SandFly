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

package com.google.android.accessibility.braille.brailledisplay.settings;

import static com.google.android.accessibility.braille.brailledisplay.settings.KeyBindingsActivity.PROPERTY_KEY;
import static com.google.android.accessibility.braille.common.BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayKeys;
import com.google.android.accessibility.braille.brailledisplay.platform.Connectioneer;
import com.google.android.accessibility.braille.brailledisplay.platform.Connectioneer.AspectDisplayer;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import com.google.android.accessibility.braille.interfaces.ScreenReaderCommands;
import com.google.android.accessibility.utils.preference.PreferencesActivity;
import java.util.List;

/**
 * Lists the screen reader commands that gestures run, by category, with the braille display keys
 * assigned to them.
 */
public class ScreenReaderCommandKeysActivity extends PreferencesActivity {
  private static final String TAG = "ScreenReaderCommandKeysActivity";
  private static final String CATEGORY_KEY = "category";

  @Override
  protected PreferenceFragmentCompat createPreferenceFragment() {
    return new ScreenReaderCommandKeysFragment();
  }

  @Override
  protected String getFragmentTag() {
    return TAG;
  }

  /** Fragment that lists the categories, or the commands of one category. */
  public static class ScreenReaderCommandKeysFragment extends PreferenceFragmentCompat {
    private Connectioneer connectioneer;
    @Nullable private BrailleDisplayProperties props;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      connectioneer = Connectioneer.getInstance(getContext());
    }

    @Override
    public void onResume() {
      super.onResume();
      connectioneer.aspectDisplayer.attach(displayerCallback);
    }

    @Override
    public void onPause() {
      super.onPause();
      connectioneer.aspectDisplayer.detach(displayerCallback);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
      getPreferenceManager().setSharedPreferencesName(BRAILLE_SHARED_PREFS_FILENAME);
      setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
      refresh(getActivity().getIntent().getParcelableExtra(PROPERTY_KEY));
    }

    private void refresh(@Nullable BrailleDisplayProperties props) {
      this.props = props;
      PreferenceScreen screen = getPreferenceScreen();
      screen.removeAll();
      ScreenReaderCommands commands = ScreenReaderCommands.Provider.get();
      if (commands == null) {
        return;
      }
      List<ScreenReaderCommands.Category> categories = commands.categories(getContext());
      int index = getActivity().getIntent().getIntExtra(CATEGORY_KEY, -1);
      if (index < 0 || index >= categories.size()) {
        getActivity().setTitle(R.string.bd_key_bindings_sandfly_commands);
        for (int i = 0; i < categories.size(); i++) {
          Preference preference = new Preference(getContext());
          preference.setTitle(categories.get(i).title);
          Intent intent = new Intent(getContext(), ScreenReaderCommandKeysActivity.class);
          intent.putExtra(CATEGORY_KEY, i);
          intent.putExtra(PROPERTY_KEY, props);
          preference.setIntent(intent);
          screen.addPreference(preference);
        }
        return;
      }
      ScreenReaderCommands.Category category = categories.get(index);
      getActivity().setTitle(category.title);
      DisplayKeys.Describer describer = new DisplayKeys.Describer(getContext(), props);
      DisplayKeyOverrides overrides = BrailleUserPreferences.readDisplayKeyOverrides(getContext());
      for (ScreenReaderCommands.Command command : category.commands) {
        String target = DisplayKeyOverrides.screenReaderTarget(command.value);
        if (DisplayKeyOverrides.screenReaderValueOf(target) == null) {
          continue;
        }
        String keys = describer.describeKeysFor(overrides, target);
        Preference preference = new Preference(getContext());
        preference.setTitle(
            getString(
                R.string.bd_commands_description_template,
                command.label,
                keys.isEmpty() ? getString(R.string.bd_key_bindings_no_keys) : keys));
        preference.setOnPreferenceClickListener(
            p -> {
              DisplayKeyAssigner.showOptions(
                  requireActivity(), target, command.label, describer, this::refreshIfAdded);
              return true;
            });
        screen.addPreference(preference);
      }
    }

    private void refreshIfAdded() {
      if (isAdded()) {
        refresh(props);
      }
    }

    private final AspectDisplayer.Callback displayerCallback =
        new AspectDisplayer.Callback() {
          @Override
          public void onDisplayStarted(BrailleDisplayProperties brailleDisplayProperties) {
            refresh(brailleDisplayProperties);
          }

          @Override
          public void onDisplayStopped() {
            refresh(null);
          }
        };
  }
}
