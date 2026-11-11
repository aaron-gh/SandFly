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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import com.google.android.accessibility.material.preference.AccessibilitySeekBarPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.SessionEqualizer;
import com.google.android.accessibility.utils.output.SpeechEqualizer;
import com.google.android.accessibility.utils.output.SpeechEqualizer.Preset;
import com.google.android.accessibility.utils.output.SpeechEqualizer.SavedPreset;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The equalizer of SandFly default or of one voice profile: a switch, a preset list, buttons that
 * save the bands as a preset and rename or delete saved presets, and a slider per band. Saved
 * presets are shared by SandFly default and every voice profile.
 */
public class SpeechEqualizerFragment extends TalkbackBaseFragment {

  /** The ID of the profile whose equalizer to show, or empty for SandFly default. */
  static final String ARG_PROFILE_ID = "profile_id";

  private static final String CUSTOM = "custom";

  // Starts the preset list's value of a saved preset, followed by its name.
  private static final String SAVED_PREFIX = "saved:";

  private SharedPreferences prefs;
  private String id = "";
  private @Nullable ListPreference presetPref;
  private @Nullable AccessibilitySuiteSwitchPreference onPref;
  private @Nullable Preference savedPresetsPref;
  private final List<BandPreference> bands = new ArrayList<>();
  // Set while this screen saves the gains, whose sliders already show them.
  private boolean saving;

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_speech_equalizer);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    Context context = requireContext();
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    Bundle args = getArguments();
    id = (args == null) ? "" : args.getString(ARG_PROFILE_ID, "");
    // SandFly's settings are in device protected storage; see VoiceProfileFragment.
    getPreferenceManager().setStorageDeviceProtected();
    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
    setPreferenceScreen(screen);
    if (!id.isEmpty() && !VoiceProfiles.ids(prefs).contains(id)) {
      return;
    }

    // Bound to the setting itself, so the service sees each change.
    onPref = new AccessibilitySuiteSwitchPreference(context);
    onPref.setKey(SpeechEqualizer.onKey(id));
    onPref.setDefaultValue(true);
    onPref.setTitle(R.string.title_pref_speech_equalizer_on);
    onPref.setIconSpaceReserved(false);
    screen.addPreference(onPref);

    presetPref = new AccessibilitySuiteListPreference(context);
    presetPref.setKey("speech_equalizer_preset");
    presetPref.setPersistent(false);
    presetPref.setTitle(R.string.title_pref_speech_equalizer_preset);
    presetPref.setDialogTitle(R.string.title_pref_speech_equalizer_preset);
    presetPref.setIconSpaceReserved(false);
    presetPref.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          String value = (String) newValue;
          Preset preset = SpeechEqualizer.presetFor(value);
          int[] gains = null;
          if (preset != null) {
            gains = preset.gains();
          } else if (value.startsWith(SAVED_PREFIX)) {
            SavedPreset saved =
                SpeechEqualizer.savedPresetNamed(
                    SpeechEqualizer.readPresets(prefs), value.substring(SAVED_PREFIX.length()));
            if (saved != null) {
              gains = saved.gains();
            }
          }
          if (gains != null) {
            SpeechEqualizer.save(prefs, id, gains);
            update();
          }
          return false;
        });
    screen.addPreference(presetPref);

    Preference savePresetPref = new AccessibilitySuitePreference(context);
    savePresetPref.setKey("speech_equalizer_save_preset");
    savePresetPref.setPersistent(false);
    savePresetPref.setTitle(R.string.title_pref_speech_equalizer_save_preset);
    savePresetPref.setIconSpaceReserved(false);
    savePresetPref.setOnPreferenceClickListener(
        preference -> {
          askToSavePreset();
          return true;
        });
    screen.addPreference(savePresetPref);

    savedPresetsPref = new AccessibilitySuitePreference(context);
    savedPresetsPref.setKey("speech_equalizer_saved_presets");
    savedPresetsPref.setPersistent(false);
    savedPresetsPref.setTitle(R.string.title_pref_speech_equalizer_saved_presets);
    savedPresetsPref.setIconSpaceReserved(false);
    savedPresetsPref.setOnPreferenceClickListener(
        preference -> {
          showSavedPresets();
          return true;
        });
    screen.addPreference(savedPresetsPref);

    for (int i = 0; i < SpeechEqualizer.BANDS; i++) {
      int band = i;
      BandPreference bar = new BandPreference(context);
      bar.setKey("speech_equalizer_band_" + i);
      bar.setPersistent(false);
      bar.setTitle(frequencyName(context, SpeechEqualizer.FREQUENCIES[i]));
      bar.setMin(-SpeechEqualizer.MAX_GAIN_DB);
      bar.setMax(SpeechEqualizer.MAX_GAIN_DB);
      bar.setShowSeekBarValue(true);
      bar.setIconSpaceReserved(false);
      bar.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            int[] gains = SpeechEqualizer.read(prefs, id);
            gains[band] = (Integer) newValue;
            saving = true;
            try {
              SpeechEqualizer.save(prefs, id, gains);
            } finally {
              saving = false;
            }
            bar.describe((Integer) newValue);
            updatePreset(gains);
            return true;
          });
      screen.addPreference(bar);
      bands.add(bar);
    }
  }

  // The Equalizer reading controls change the gains and the switch while the screen is open, and
  // presets are saved, renamed and deleted from this screen.
  private final OnSharedPreferenceChangeListener prefsListener =
      (sharedPrefs, key) -> {
        if (!saving
            && (TextUtils.equals(key, SpeechEqualizer.key(id))
                || TextUtils.equals(key, SpeechEqualizer.onKey(id))
                || TextUtils.equals(key, SpeechEqualizer.PREF_PRESETS))) {
          update();
        }
      };

  @Override
  public void onResume() {
    super.onResume();
    prefs.registerOnSharedPreferenceChangeListener(prefsListener);
    update();
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
  }

  private void update() {
    if (onPref != null) {
      onPref.setChecked(SpeechEqualizer.isOn(prefs, id));
    }
    int[] gains = SpeechEqualizer.read(prefs, id);
    for (int i = 0; i < bands.size(); i++) {
      bands.get(i).setValue(gains[i]);
      bands.get(i).describe(gains[i]);
    }
    updatePreset(gains);
  }

  /**
   * Lists the presets, then the saved presets, and Custom as well while the gains are no preset's.
   */
  private void updatePreset(int[] gains) {
    if (presetPref == null) {
      return;
    }
    Context context = requireContext();
    List<SavedPreset> saved = SpeechEqualizer.readPresets(prefs);
    if (savedPresetsPref != null) {
      savedPresetsPref.setVisible(!saved.isEmpty());
    }
    Preset current = SpeechEqualizer.presetOf(gains);
    SavedPreset currentSaved =
        (current == null) ? SpeechEqualizer.savedPresetOf(saved, gains) : null;
    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> values = new ArrayList<>();
    for (Preset preset : Preset.values()) {
      entries.add(presetName(context, preset));
      values.add(preset.getValue());
    }
    for (SavedPreset preset : saved) {
      entries.add(preset.getName());
      values.add(SAVED_PREFIX + preset.getName());
    }
    String value;
    if (current != null) {
      value = current.getValue();
    } else if (currentSaved != null) {
      value = SAVED_PREFIX + currentSaved.getName();
    } else {
      entries.add(context.getString(R.string.speech_equalizer_custom));
      values.add(CUSTOM);
      value = CUSTOM;
    }
    presetPref.setEntries(entries.toArray(new CharSequence[0]));
    presetPref.setEntryValues(values.toArray(new CharSequence[0]));
    presetPref.setValue(value);
    presetPref.setSummary("%s");
  }

  /** Asks for a name, and saves the gains as a preset with it. */
  private void askToSavePreset() {
    Context context = requireContext();
    if (SpeechEqualizer.readPresets(prefs).size() >= SpeechEqualizer.MAX_SAVED_PRESETS) {
      new AlertDialog.Builder(context)
          .setTitle(R.string.title_pref_speech_equalizer_save_preset)
          .setMessage(R.string.speech_equalizer_presets_full)
          .setPositiveButton(android.R.string.ok, null)
          .show();
      return;
    }
    askForPresetName(
        R.string.title_pref_speech_equalizer_save_preset,
        /* name= */ null,
        name -> {
          if (SpeechEqualizer.addPreset(prefs, name, SpeechEqualizer.read(prefs, id))) {
            announce(getString(R.string.speech_equalizer_preset_saved, name));
          }
        });
  }

  /** Lists the saved presets, to rename or delete one. */
  private void showSavedPresets() {
    List<SavedPreset> saved = SpeechEqualizer.readPresets(prefs);
    if (saved.isEmpty()) {
      return;
    }
    CharSequence[] names = new CharSequence[saved.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = saved.get(i).getName();
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.title_pref_speech_equalizer_saved_presets)
        .setItems(names, (dialog, which) -> showPresetActions(saved.get(which).getName()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Offers Rename and Delete for the saved preset called {@code name}. */
  private void showPresetActions(String name) {
    CharSequence[] actions = {
      getString(R.string.speech_equalizer_preset_rename),
      getString(R.string.speech_equalizer_preset_delete)
    };
    new AlertDialog.Builder(requireContext())
        .setTitle(name)
        .setItems(
            actions,
            (dialog, which) -> {
              if (which == 0) {
                askForPresetName(
                    R.string.speech_equalizer_preset_rename,
                    name,
                    newName -> SpeechEqualizer.renamePreset(prefs, name, newName));
              } else {
                confirmDeletePreset(name);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmDeletePreset(String name) {
    new AlertDialog.Builder(requireContext())
        .setMessage(getString(R.string.speech_equalizer_preset_delete_confirm, name))
        .setPositiveButton(
            R.string.speech_equalizer_preset_delete,
            (dialog, which) -> {
              SpeechEqualizer.deletePreset(prefs, name);
              announce(getString(R.string.speech_equalizer_preset_deleted, name));
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Receives a name from {@link #askForPresetName}, never blank and never another preset's. */
  private interface PresetNameListener {
    void onName(String name);
  }

  /**
   * Asks for the name of a preset, starting with {@code name} when renaming one, or empty for a new
   * one. OK can't be pressed while the name is blank or another preset's, a built-in one's or
   * Custom, and the field says when it is taken.
   */
  private void askForPresetName(
      int titleResId, @Nullable String name, PresetNameListener listener) {
    Context context = requireContext();
    EditText field = new KeyboardSafeEditText(context);
    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    field.setSingleLine(true);
    field.setFilters(
        new InputFilter[] {new InputFilter.LengthFilter(SpeechEqualizer.MAX_PRESET_NAME_LENGTH)});
    field.setHint(R.string.speech_equalizer_preset_name);
    if (name != null) {
      field.setText(name);
      field.setSelection(field.length());
    }
    AlertDialog dialog =
        new AlertDialog.Builder(context)
            .setTitle(titleResId)
            .setView(VoiceProfilesFragment.padded(context, field))
            .setPositiveButton(
                android.R.string.ok,
                (shown, which) -> {
                  String entered = SpeechEqualizer.cleanPresetName(field.getText().toString());
                  if (!entered.isEmpty() && !isPresetNameTaken(context, entered, name)) {
                    listener.onName(entered);
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    Runnable check =
        () -> {
          String entered = SpeechEqualizer.cleanPresetName(field.getText().toString());
          boolean taken = !entered.isEmpty() && isPresetNameTaken(context, entered, name);
          ok.setEnabled(!entered.isEmpty() && !taken);
          field.setError(
              taken
                  ? context.getString(R.string.speech_equalizer_preset_name_taken, entered)
                  : null);
        };
    check.run();
    field.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence text, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable text) {
            check.run();
          }
        });
    field.requestFocus();
  }

  /**
   * Whether {@code name} is a saved preset's other than {@code except}'s, a built-in preset's or
   * Custom, ignoring case.
   */
  private boolean isPresetNameTaken(Context context, String name, @Nullable String except) {
    if (SpeechEqualizer.isPresetNameTaken(SpeechEqualizer.readPresets(prefs), name, except)) {
      return true;
    }
    if (name.equalsIgnoreCase(context.getString(R.string.speech_equalizer_custom))) {
      return true;
    }
    for (Preset preset : Preset.values()) {
      if (name.equalsIgnoreCase(presetName(context, preset))) {
        return true;
      }
    }
    return false;
  }

  private void announce(String text) {
    View view = getView();
    if (view != null) {
      view.announceForAccessibility(text);
    }
  }

  /**
   * What the equalizer of profile {@code profileId}, or of SandFly default if empty, is set to, for
   * the screen that opens it.
   */
  static String summary(Context context, SharedPreferences prefs, String profileId) {
    if (!SessionEqualizer.isAvailable()
        && !prefs.getBoolean(
            FailoverTextToSpeech.PREF_LOW_LATENCY_AUDIO_KEY,
            FailoverTextToSpeech.lowLatencyAudioDefault(context))) {
      return context.getString(R.string.summary_speech_equalizer_needs_low_latency);
    }
    if (!SpeechEqualizer.isOn(prefs, profileId)) {
      return context.getString(R.string.speech_equalizer_off);
    }
    int[] gains = SpeechEqualizer.read(prefs, profileId);
    Preset preset = SpeechEqualizer.presetOf(gains);
    if (preset != null) {
      return presetName(context, preset);
    }
    SavedPreset saved = SpeechEqualizer.savedPresetOf(SpeechEqualizer.readPresets(prefs), gains);
    return (saved == null) ? context.getString(R.string.speech_equalizer_custom) : saved.getName();
  }

  static String presetName(Context context, Preset preset) {
    int resId =
        switch (preset) {
          case FLAT -> R.string.speech_equalizer_flat;
          case CLARITY -> R.string.speech_equalizer_clarity;
          case BASS_BOOST -> R.string.speech_equalizer_bass_boost;
          case BASS_CUT -> R.string.speech_equalizer_bass_cut;
          case TREBLE_BOOST -> R.string.speech_equalizer_treble_boost;
          case TREBLE_CUT -> R.string.speech_equalizer_treble_cut;
          case WARM -> R.string.speech_equalizer_warm;
          case REDUCE_HARSHNESS -> R.string.speech_equalizer_reduce_harshness;
          case REDUCE_SIBILANCE -> R.string.speech_equalizer_reduce_sibilance;
          case SMALL_SPEAKER -> R.string.speech_equalizer_small_speaker;
        };
    return context.getString(resId);
  }

  private static String frequencyName(Context context, int hz) {
    NumberFormat format = NumberFormat.getInstance();
    return (hz >= 1000)
        ? context.getString(R.string.speech_equalizer_band_khz, format.format(hz / 1000.0))
        : context.getString(R.string.speech_equalizer_band_hz, format.format(hz));
  }

  /** A band's slider, which says its gain in dB. */
  private static final class BandPreference extends AccessibilitySeekBarPreference {
    private @Nullable SeekBar seekBar;

    BandPreference(@NonNull Context context) {
      super(context);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
      super.onBindViewHolder(holder);
      seekBar = (SeekBar) holder.findViewById(androidx.preference.R.id.seekbar);
      describe(getValue());
    }

    void describe(int gain) {
      if (seekBar == null) {
        return;
      }
      String number =
          (gain > 0 ? "+" : "") + NumberFormat.getIntegerInstance().format(gain);
      ViewCompat.setStateDescription(
          seekBar, getContext().getString(R.string.speech_equalizer_gain_db, number));
    }
  }

  /** Opens the equalizer of profile {@code profileId}, or of SandFly default if empty. */
  static void setUp(Preference preference, String profileId) {
    preference.setFragment(SpeechEqualizerFragment.class.getName());
    preference.getExtras().putString(ARG_PROFILE_ID, profileId);
  }
}
