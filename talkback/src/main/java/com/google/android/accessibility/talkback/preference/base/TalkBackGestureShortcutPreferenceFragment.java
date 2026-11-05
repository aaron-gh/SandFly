/*
 * Copyright 2010 Google Inc.
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

import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_ARGS;
import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity.TalkBackSubSettings;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureLayout;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.gesture.VibrationWatchGestureSettings;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.preference.search.SettingsSearch;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Panel holding a set of shortcut preferences. Recreated when preset value changes. */
public class TalkBackGestureShortcutPreferenceFragment extends TalkbackBaseFragment
    implements SettingsSearch.FoundKey {
  private static final String TAG = "TalkBackGestureShortcutPreferenceFragment";

  /** Preferences managed by this activity. */
  private SharedPreferences prefs;

  private boolean shouldUpdatePreferenceOnResume = false;

  public TalkBackGestureShortcutPreferenceFragment() {
    super(R.xml.gesture_preferences);
  }

  private final DialogInterface.OnClickListener resetGestureConfirmDialogPositive =
      (dialogInterface, i) -> {
        PreferencesActivityUtils.announceText(
            getString(R.string.gestures_announce_reset_gesture_settings), getContext());
        // Reset logic here
        resetGestureShortcut();
        dialogInterface.dismiss();
      };

  private final Preference.OnPreferenceClickListener resetGesturePreferenceClickListener =
      (preference) -> {
        boolean gestureSetEnabled =
            GestureShortcutMapping.isGestureSetEnabled(
                getContext(),
                prefs,
                R.string.pref_multiple_gesture_set_key,
                R.bool.pref_multiple_gesture_set_default);

        // Show confirm dialog.
        A11yAlertDialogWrapper.materialDialogBuilder(
                getContext(), getActivity().getSupportFragmentManager())
            .setTitle(
                getString(
                    gestureSetEnabled
                        ? R.string.Reset_gesture_settings_set_dialog_title
                        : R.string.Reset_gesture_settings_dialog_title))
            .setMessage(getString(R.string.message_reset_gesture_settings_confirm_dialog))
            .setPositiveButton(
                R.string.reset_button_in_reset_gesture_settings_confirm_dialog,
                resetGestureConfirmDialogPositive)
            .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.cancel())
            .create()
            .show();

        return true;
      };

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof GestureListPreference gesturePreference) {
      shouldUpdatePreferenceOnResume = true;
      GestureListPreference.showCommandList(this, gesturePreference);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_category_manage_gestures);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();

    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    // Launches Practice gestures page.
    @Nullable
    Preference practiceGesturesPreference =
        findPreference(getString(R.string.pref_practice_gestures_entry_point_key));
    if (practiceGesturesPreference != null) {
      practiceGesturesPreference.setIntent(TutorialInitiator.createPracticeGesturesIntent(context));
    }

    boolean gestureSetEnabled =
        GestureShortcutMapping.isGestureSetEnabled(
            context,
            prefs,
            R.string.pref_multiple_gesture_set_key,
            R.bool.pref_multiple_gesture_set_default);

    @Nullable
    Preference gestureSetPreference = findPreference(getString(R.string.pref_gesture_set_key));
    if (!gestureSetEnabled && gestureSetPreference != null) {
      getPreferenceScreen().removePreference(gestureSetPreference);
    }
    setUpGestureLayout();
    updatePreferenceKeys();

    Preference resetGesturePreferenceScreen =
        (Preference) findPreference(getString(R.string.pref_reset_gesture_settings_key));
    resetGesturePreferenceScreen.setOnPreferenceClickListener(resetGesturePreferenceClickListener);

    updateTwoFingerSwipeRows();
    if (!FormFactorUtils.isAndroidWear()) {
      Preference vibrationWatch =
          findPreference(getString(R.string.pref_reserve_vibration_watch_gestures_key));
      if (vibrationWatch != null) {
        vibrationWatch.setVisible(false);
      }
    }
    getCustomizationMap().forEach(this::runPreferencesCustomization);
    collapseCategories();
    // Finds a gesture by its name or the command it runs, and opens its list of commands.
    SettingsSearch.addSearchItem(
        getPreferenceScreen(),
        () ->
            SettingsSearch.searchRows(
                this,
                preference -> preference instanceof GestureListPreference,
                this::onDisplayPreferenceDialog));
  }

  private HashMap<List<Integer>, Consumer<Preference>> getCustomizationMap() {
    HashMap<List<Integer>, Consumer<Preference>> customizations = new HashMap<>();
    if (FormFactorUtils.isAndroidWear()) {
      List<Integer> keys = new ArrayList<>();
      keys.add(R.string.pref_shortcut_2finger_1tap_key);
      keys.add(R.string.pref_shortcut_2finger_2tap_key);
      customizations.put(
          keys,
          preference -> {
            GestureListPreference gestureListPreference = (GestureListPreference) preference;
            gestureListPreference.setSummaryWhenDisabled(
                requireContext().getString(R.string.shortcut_disabled_due_to_vibration_watch));
          });
    }
    return customizations;
  }

  private void runPreferencesCustomization(
      List<Integer> keys, Consumer<Preference> preferenceModifier) {
    for (int keyId : keys) {
      Preference preference = findGestureRow(getString(keyId));
      if (preference == null) {
        preference = findPreferenceByResId(keyId);
      }
      if (preference != null) {
        preferenceModifier.accept(preference);
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    updatePreferencesOnResume();
    updatePreferencesForVibrationWatchIfNeeded();
  }

  private void updatePreferencesOnResume() {
    if (!shouldUpdatePreferenceOnResume) {
      return;
    }
    updatePreferenceKeys();
    shouldUpdatePreferenceOnResume = false;
  }

  private void updatePreferencesForVibrationWatchIfNeeded() {

    if (!FormFactorUtils.isAndroidWear()) {
      return;
    }

    boolean manuallyReserved = VibrationWatchGestureSettings.isEnabled(prefs);
    boolean unavailable =
        manuallyReserved || SettingsUtils.isVibrationWatchEnabled(requireContext());
    for (int keyId :
        new int[] {R.string.pref_shortcut_2finger_1tap_key, R.string.pref_shortcut_2finger_2tap_key}) {
      GestureListPreference preference = findGestureRow(getString(keyId));
      if (preference != null) {
        preference.setSummaryWhenDisabled(
            getString(
                manuallyReserved
                    ? R.string.shortcut_reserved_for_vibration_watch
                    : R.string.shortcut_disabled_due_to_vibration_watch));
        preference.setEnabled(!unavailable);
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  /** Listener for preference changes. */
  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      (prefs, key) -> {
        if (getActivity() == null) {
          LogUtils.w(
              TAG, "Fragment is not attached to activity, do not update verbosity setting page.");
          return;
        }
        if (TextUtils.equals(key, getString(R.string.pref_gesture_set_key))) {
          ListPreference preference =
              (ListPreference) findPreference(getString(R.string.pref_gesture_set_key));

          String newValueString =
              prefs.getString(
                  getString(R.string.pref_gesture_set_key),
                  getString(R.string.pref_gesture_set_value_default));
          if (preference != null) {
            preference.setValue(newValueString);
          }

          updatePreferenceKeys();
          updatePreferencesForVibrationWatchIfNeeded();
        } else if (TextUtils.equals(key, GestureLayout.KEY)) {
          ListPreference layout = findPreference(GestureLayout.KEY);
          if (layout != null) {
            layout.setValue(GestureLayout.current(prefs));
          }
          updatePreferenceKeys();
        } else if (TextUtils.equals(key, GestureLayout.TWO_FINGER_SWIPES_KEY)) {
          TwoStatePreference twoFingerSwipes = findPreference(GestureLayout.TWO_FINGER_SWIPES_KEY);
          if (twoFingerSwipes != null) {
            twoFingerSwipes.setChecked(GestureLayout.handlesTwoFingerSwipes(prefs));
          }
          updateTwoFingerSwipeRows();
        } else if (TextUtils.equals(key, VibrationWatchGestureSettings.PREF_RESERVED)) {
          updatePreferencesForVibrationWatchIfNeeded();
        }
      };

  /** Returns the gesture set in use, 0 with only one. */
  private int gestureSet() {
    if (!GestureShortcutMapping.isGestureSetEnabled(
        getContext(),
        prefs,
        R.string.pref_multiple_gesture_set_key,
        R.bool.pref_multiple_gesture_set_default)) {
      return 0;
    }
    return SharedPreferencesUtils.getIntFromStringPref(
        prefs, getResources(), R.string.pref_gesture_set_key, R.string.pref_gesture_set_value_default);
  }

  /** Returns every gesture row of the screen. */
  private List<GestureListPreference> gestureRows() {
    List<GestureListPreference> rows = new ArrayList<>();
    collectGestureRows(getPreferenceScreen(), rows);
    return rows;
  }

  private static void collectGestureRows(PreferenceGroup group, List<GestureListPreference> rows) {
    for (int i = 0; i < group.getPreferenceCount(); i++) {
      Preference preference = group.getPreference(i);
      if (preference instanceof GestureListPreference gesture) {
        rows.add(gesture);
      } else if (preference instanceof PreferenceGroup child) {
        collectGestureRows(child, rows);
      }
    }
  }

  /** Returns the row of the gesture whose key without a layout or set is {@code baseKey}. */
  @Nullable
  private GestureListPreference findGestureRow(String baseKey) {
    for (GestureListPreference row : gestureRows()) {
      if (TextUtils.equals(row.getBaseKey(), baseKey)) {
        return row;
      }
    }
    return null;
  }

  /** Points each gesture row at its key and default command in the layout and set in use. */
  private void updatePreferenceKeys() {
    int gestureSet = gestureSet();
    for (GestureListPreference row : gestureRows()) {
      String baseKey = row.getBaseKey();
      row.useKey(
          GestureShortcutMapping.getPrefKeyWithGestureSet(
              GestureLayout.key(prefs, baseKey), gestureSet),
          GestureLayout.defaultValue(getContext(), prefs, baseKey, row.getSandFlyDefault()));
    }
  }

  /** Gesture layout list and two-finger swipe switch at the top of the screen. */
  private void setUpGestureLayout() {
    ListPreference layout = findPreference(GestureLayout.KEY);
    if (layout != null) {
      layout.setValue(GestureLayout.current(prefs));
      layout.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            GestureLayout.select(prefs, String.valueOf(newValue));
            return true;
          });
    }
    TwoStatePreference twoFingerSwipes = findPreference(GestureLayout.TWO_FINGER_SWIPES_KEY);
    if (twoFingerSwipes != null) {
      if (FeatureSupport.isMultiFingerGestureSupported()) {
        twoFingerSwipes.setChecked(GestureLayout.handlesTwoFingerSwipes(prefs));
      } else {
        twoFingerSwipes.setVisible(false);
      }
    }
  }

  /**
   * Two-finger swipes scroll in the app unless SandFly handles them, and can't be assigned while
   * they do.
   */
  private void updateTwoFingerSwipeRows() {
    if (!FeatureSupport.isMultiFingerGestureSupported()) {
      return;
    }
    boolean reserved = !GestureLayout.handlesTwoFingerSwipes(prefs);
    String[] keys = getResources().getStringArray(R.array.pref_2finger_pass_through_shortcut_keys);
    String[] summaries =
        getResources().getStringArray(R.array.pref_2finger_pass_through_shortcut_summary);
    if (keys.length != summaries.length) {
      return;
    }
    for (int i = 0; i < keys.length; i++) {
      GestureListPreference row = findGestureRow(keys[i]);
      if (row != null) {
        row.setSummaryWhenDisabled(
            reserved ? getString(R.string.shortcut_not_customizable, summaries[i]) : null);
        row.setEnabled(!reserved);
      }
    }
  }

  /** Gestures' keys end with the layout and set in use. */
  @Override
  public String foundKey(String key) {
    if (prefs == null) {
      return key;
    }
    @Nullable GestureListPreference row = findGestureRow(key);
    return row != null ? row.getKey() : key;
  }

  private void resetGestureShortcut() {
    // Reset preference to default
    final SharedPreferences.Editor prefEditor = prefs.edit();
    final String[] gesturePrefKeys = getResources().getStringArray(R.array.pref_shortcut_keys);
    int gestureSet = gestureSet();
    for (String defaultKey : gesturePrefKeys) {
      prefEditor.remove(
          GestureShortcutMapping.getPrefKeyWithGestureSet(
              GestureLayout.key(prefs, defaultKey), gestureSet));
    }
    prefEditor.apply();
    for (GestureListPreference row : gestureRows()) {
      row.refresh();
    }
  }
}
