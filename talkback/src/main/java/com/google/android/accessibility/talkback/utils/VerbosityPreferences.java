/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.android.accessibility.talkback.actor.TalkBackUIActor.Type.GESTURE_OR_KEYBOARD_ACTION_OVERLAY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.eventprocessor.PhoneticLetterMode;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.keyboardecho.KeyboardEchoSettings;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility functions for verbosity preferences. Verbosity preferences should be read through
 * getPreferenceValue() to use preference verbosity rules.
 */
public class VerbosityPreferences {

  private static final String TAG = "VerbosityPreferences";

  private VerbosityPreferences() {} // Do not instantiate.

  /** Reads a setting that the verbosity presets once kept a copy of for each preset. */
  public static boolean getPreferenceValueBool(
      SharedPreferences preferences, Resources resources, String key, boolean defaultValue) {
    return preferences.getBoolean(key, defaultValue);
  }

  /** Reads a setting that the verbosity presets once kept a copy of for each preset. */
  public static String getPreferenceValueString(
      SharedPreferences preferences, Resources resources, String key, String defaultValue) {
    return preferences.getString(key, defaultValue);
  }

  /**
   * Reads a setting as the verbosity preset chosen before presets were removed had it, for
   * migrations that run before {@link #removePresets}.
   */
  public static boolean getPresetValueBool(
      SharedPreferences preferences, Resources resources, String key, boolean defaultValue) {
    String preset =
        SharedPreferencesUtils.getStringPref(
            preferences, resources, R.string.pref_verbosity_preset_key, 0); // Default to null.
    if (preset == null) {
      return preferences.getBoolean(key, defaultValue);
    }
    return getPreferenceVerbosityBool(preferences, resources, preset, key, defaultValue);
  }

  /**
   * Reads a setting as the verbosity preset chosen before presets were removed had it, for
   * migrations that run before {@link #removePresets}.
   */
  public static String getPresetValueString(
      SharedPreferences preferences, Resources resources, String key, String defaultValue) {
    String preset =
        SharedPreferencesUtils.getStringPref(
            preferences, resources, R.string.pref_verbosity_preset_key, 0); // Default to null.
    if (preset == null) {
      return preferences.getString(key, defaultValue);
    }
    return getPreferenceVerbosityString(preferences, resources, preset, key, defaultValue);
  }

  /**
   * Gives each setting that the verbosity presets controlled the value it had in the preset that
   * was chosen, under the setting's own key, and removes the preset and its copies. Does nothing
   * once they are gone, so imported settings from before are moved too.
   */
  public static void removePresets(SharedPreferences preferences, Resources resources) {
    String presetKey = resources.getString(R.string.pref_verbosity_preset_key);
    String[] presets = resources.getStringArray(R.array.pref_verbosity_preset_values);
    List<String> copies = new ArrayList<>();
    for (String presetValue : presets) {
      for (int keyId : PRESET_SWITCHES) {
        copies.add(toVerbosityPrefKey(presetValue, resources.getString(keyId)));
      }
      for (int keyId : PRESET_LISTS) {
        copies.add(toVerbosityPrefKey(presetValue, resources.getString(keyId)));
      }
    }
    boolean found = preferences.contains(presetKey);
    for (String copy : copies) {
      found |= preferences.contains(copy);
    }
    if (!found) {
      return;
    }
    String preset =
        preferences.getString(
            presetKey, resources.getString(R.string.pref_verbosity_preset_value_default));
    boolean custom = !isPresetHighOrLow(preset, resources);
    SharedPreferences.Editor editor = preferences.edit();
    for (int i = 0; i < PRESET_SWITCHES.length; i++) {
      String key = resources.getString(PRESET_SWITCHES[i]);
      if (custom && !preferences.contains(toVerbosityPrefKey(preset, key))) {
        continue;
      }
      editor.putBoolean(
          key,
          getPreferenceVerbosityBool(
              preferences,
              resources,
              preset,
              key,
              resources.getBoolean(PRESET_SWITCH_DEFAULTS[i])));
    }
    for (int i = 0; i < PRESET_LISTS.length; i++) {
      String key = resources.getString(PRESET_LISTS[i]);
      if (custom && !preferences.contains(toVerbosityPrefKey(preset, key))) {
        continue;
      }
      String value =
          getPreferenceVerbosityString(
              preferences, resources, preset, key, resources.getString(PRESET_LIST_DEFAULTS[i]));
      if (value != null) {
        editor.putString(key, value);
      }
    }
    editor.remove(presetKey);
    for (String copy : copies) {
      editor.remove(copy);
    }
    editor.apply();
  }

  private static boolean isPresetHighOrLow(String preset, Resources resources) {
    return TextUtils.equals(preset, resources.getString(R.string.pref_verbosity_preset_value_high))
        || TextUtils.equals(preset, resources.getString(R.string.pref_verbosity_preset_value_low));
  }

  /** The switches that the verbosity presets controlled, with their defaults below. */
  private static final int[] PRESET_SWITCHES = {
    R.string.pref_a11y_hints_key,
    R.string.pref_speak_container_element_positions_key,
    R.string.pref_table_speak_row_column_numbers_key,
    R.string.pref_verbose_scroll_announcement_key,
    R.string.pref_speak_roles_key,
    R.string.pref_intonation_key,
  };

  private static final int[] PRESET_SWITCH_DEFAULTS = {
    R.bool.pref_a11y_hints_default,
    R.bool.pref_speak_container_element_positions_default,
    R.bool.pref_table_speak_row_column_numbers_default,
    R.bool.pref_verbose_scroll_announcement_default,
    R.bool.pref_speak_roles_default,
    R.bool.pref_intonation_default,
  };

  /** The lists that the verbosity presets controlled, with their defaults below. */
  private static final int[] PRESET_LISTS = {
    R.string.pref_phonetic_letters_mode_key, R.string.pref_capital_letters_key,
  };

  private static final int[] PRESET_LIST_DEFAULTS = {
    R.string.pref_phonetic_letters_mode_default, R.string.pref_capital_letters_default,
  };

  public static boolean getPreferenceVerbosityBool(
      SharedPreferences preferences,
      Resources resources,
      @NonNull String verbosityValue,
      String key,
      boolean defaultValue) {
    if (verbosityValue.equals(resources.getString(R.string.pref_verbosity_preset_value_high))) {
      return true;
    } else if (verbosityValue.equals(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      return false;
    } else {
      String keyForVerbosity = toVerbosityPrefKey(verbosityValue, key);
      return preferences.getBoolean(keyForVerbosity, defaultValue);
    }
  }

  public static String getPreferenceVerbosityString(
      SharedPreferences preferences,
      Resources resources,
      @NonNull String verbosityValue,
      String key,
      String defaultValue) {
    if (verbosityValue.equals(resources.getString(R.string.pref_verbosity_preset_value_high))) {
      // If verbosity is high... use rule to select list preference value.
      return getVerbosityValueHighFromListPreference(key, resources);
    } else if (verbosityValue.equals(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      // If verbosity is low... use rule to select list preference value.
      return getVerbosityValueLowFromListPreference(key, resources);
    } else {
      // If verbosity is custom... retrieve preference value.
      String keyForVerbosity = toVerbosityPrefKey(verbosityValue, key);
      return preferences.getString(keyForVerbosity, defaultValue);
    }
  }

  private static String getVerbosityValueHighFromListPreference(String key, Resources resources) {
    if (key.equals(resources.getString(R.string.pref_keyboard_echo_on_screen_key))
        || key.equals(resources.getString(R.string.pref_keyboard_echo_physical_key))) {
      String[] keyboardEchoValues = resources.getStringArray(R.array.pref_keyboard_echo_values);
      return (keyboardEchoValues == null || keyboardEchoValues.length == 0)
          ? null
          : keyboardEchoValues[keyboardEchoValues.length - 1];
    } else if (key.equals(resources.getString(R.string.pref_capital_letters_key))) {
      String[] capitalLetterValues = resources.getStringArray(R.array.pref_capital_letters_values);
      return (capitalLetterValues == null || capitalLetterValues.length == 0)
          ? null
          : capitalLetterValues[1]; // Say "cap"
    } else if (key.equals(resources.getString(R.string.pref_phonetic_letters_mode_key))) {
      return PhoneticLetterMode.AFTER_LETTER_DELAY;
    } else {
      LogUtils.e(TAG, "Unhandled key \"%s\"", key);
      return null;
    }
  }

  private static String getVerbosityValueLowFromListPreference(String key, Resources resources) {
    if (key.equals(resources.getString(R.string.pref_keyboard_echo_on_screen_key))
        || key.equals(resources.getString(R.string.pref_keyboard_echo_physical_key))) {
      String[] keyboardEchoValues = resources.getStringArray(R.array.pref_keyboard_echo_values);
      return (keyboardEchoValues == null || keyboardEchoValues.length == 0)
          ? null
          : keyboardEchoValues[0];
    } else if (key.equals(resources.getString(R.string.pref_capital_letters_key))) {
      String[] capitalLetterValues = resources.getStringArray(R.array.pref_capital_letters_values);
      return (capitalLetterValues == null || capitalLetterValues.length == 0)
          ? null
          : capitalLetterValues[0]; // Do nothing
    } else if (key.equals(resources.getString(R.string.pref_phonetic_letters_mode_key))) {
      return PhoneticLetterMode.OFF;
    } else {
      LogUtils.e(TAG, "Unhandled key \"%s\"", key);
      return null;
    }
  }

  /**
   * Creates new preference key by appending the verbosityName. For example, pref_a11y_hints becomes
   * pref_verbosity_preset_value_high_pref_a11y_hints
   *
   * @param verbosityName verbosity name, such as pref_verbosity_preset_value_high.
   * @param preferenceKey preference key.
   */
  public static String toVerbosityPrefKey(String verbosityName, String preferenceKey) {
    return verbosityName + "_" + preferenceKey;
  }

  /**
   * Restores the preference key to the custom verbosity pref key. For example,
   * pref_verbosity_preset_value_high_pref_a11y_hints restores to pref_a11y_hints
   *
   * @param resources the resources that is used to get the string.
   * @param preferenceKey preference key.
   */
  public static String restoreToCustomVerbosityPrefKey(Resources resources, String preferenceKey) {
    if (preferenceKey == null) {
      return null;
    }

    String substring = preferenceKey;
    if (preferenceKey.contains(resources.getString(R.string.pref_verbosity_preset_value_custom))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_custom).length() + 1,
              preferenceKey.length());
    } else if (preferenceKey.contains(
        resources.getString(R.string.pref_verbosity_preset_value_high))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_high).length() + 1,
              preferenceKey.length());
    } else if (preferenceKey.contains(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_low).length() + 1,
              preferenceKey.length());
    }

    return substring;
  }

  /**
   * Maps verbosity value key to verbosity name.
   *
   * @param verbosityValueKey Preset verbosity key
   * @param context Context used to evaluate strings.
   * @return preset verbosity name
   */
  public static @Nullable String verbosityValueToName(String verbosityValueKey, Context context) {
    if (verbosityValueKey.equals(context.getString(R.string.pref_verbosity_preset_value_high))) {
      return context.getString(R.string.pref_verbosity_preset_entry_high);
    } else if (verbosityValueKey.equals(
        context.getString(R.string.pref_verbosity_preset_value_custom))) {
      return context.getString(R.string.pref_verbosity_preset_entry_custom);
    } else if (verbosityValueKey.equals(
        context.getString(R.string.pref_verbosity_preset_value_low))) {
      return context.getString(R.string.pref_verbosity_preset_entry_low);
    } else {
      return null;
    }
  }

  /**
   * Returns {@code true} if the current verbosity preset value is high or low. Some verbosity
   * preset settings need to be disabled when it is true.
   *
   * @param verbosityValue Preset verbosity
   * @param context Context used to evaluate strings
   */
  public static boolean isVerbosityValueHighOrLow(String verbosityValue, Context context) {
    return TextUtils.equals(
            verbosityValue, context.getString(R.string.pref_verbosity_preset_value_high))
        || TextUtils.equals(
            verbosityValue, context.getString(R.string.pref_verbosity_preset_value_low));
  }

  /**
   * Returns announcement for the change of verbosity.
   *
   * @param verbosityValueKey Preset verbosity key
   * @param context Context used to evaluate strings.
   * @return Announcement text
   */
  public static @Nullable String getVerbosityChangeAnnouncement(
      String verbosityValueKey, Context context) {
    String name = verbosityValueToName(verbosityValueKey, context);
    return TextUtils.isEmpty(name)
        ? null
        : String.format(
            context.getString(R.string.pref_verbosity_preset_change),
            verbosityValueToName(verbosityValueKey, context));
  }

  /**
   * Changes the physical keyboard echo to the next value, wrapping around to the first value if the
   * last value is currently selected.
   *
   * @param context Context used to evaluate strings.
   * @param pipeline Used to play announcement.
   * @param EventId Event id
   * @return true if the typing echo was successfully cycled, false if an error occurred, in which
   *     case all settings are left untouched.
   */
  public static boolean cycleKeyboardTypingEcho(
      Context context, Pipeline.FeedbackReturner pipeline, EventId eventId) {
    if (!FeatureFlagReader.enableCycleTypingEchoKeyboard(context)) {
      return false;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);

    List<String> keyboardEchoValues =
        Arrays.asList(context.getResources().getStringArray(R.array.pref_keyboard_echo_values));
    List<String> keyboardEchoEntries =
        Arrays.asList(context.getResources().getStringArray(R.array.pref_keyboard_echo_entries));
    String keyboardEchoKey =
        KeyboardEchoSettings.key(context.getResources(), TypingInput.PHYSICAL_KEYBOARD);
    if (keyboardEchoEntries.size() != keyboardEchoValues.size()) {
      LogUtils.e(TAG, "Keyboard echo values and entries have different sizes");
      return false;
    }

    String currentKeyboardEcho =
        String.valueOf(
            KeyboardEchoSettings.read(
                prefs, context.getResources(), TypingInput.PHYSICAL_KEYBOARD));
    int currentIndex = keyboardEchoValues.indexOf(currentKeyboardEcho);
    if (currentIndex == -1) {
      LogUtils.e(TAG, " Current keyboard echo setting not present in possible values");
      return false;
    }

    int newIndex = (currentIndex + 1) % keyboardEchoValues.size();
    String newKeyboardEcho = keyboardEchoValues.get(newIndex);
    prefs.edit().putString(keyboardEchoKey, newKeyboardEcho).apply();
    pipeline.returnFeedback(
        eventId,
        Feedback.speech(
            context.getString(R.string.typing_echo_change, keyboardEchoEntries.get(newIndex))));
    pipeline.returnFeedback(
        eventId,
        Feedback.talkBackUI(
            Feedback.TalkBackUI.Action.SHOW_GESTURE_OR_KEYBOARD_ACTION_UI,
            GESTURE_OR_KEYBOARD_ACTION_OVERLAY,
            context.getString(
                R.string.typing_echo_change_visual_feedback, keyboardEchoEntries.get(newIndex)),
            /* showIcon= */ false,
            Feedback.TalkBackUI.Item.ITEM_UNSPECIFIED));
    return true;
  }
}
