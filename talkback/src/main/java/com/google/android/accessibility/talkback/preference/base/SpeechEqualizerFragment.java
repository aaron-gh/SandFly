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
import android.text.TextUtils;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import com.google.android.accessibility.material.preference.AccessibilitySeekBarPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.SessionEqualizer;
import com.google.android.accessibility.utils.output.SpeechEqualizer;
import com.google.android.accessibility.utils.output.SpeechEqualizer.Preset;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The equalizer of SandFly default or of one voice profile: a switch, a preset list and a slider
 * per band.
 */
public class SpeechEqualizerFragment extends TalkbackBaseFragment {

  /** The ID of the profile whose equalizer to show, or empty for SandFly default. */
  static final String ARG_PROFILE_ID = "profile_id";

  private static final String CUSTOM = "custom";

  private SharedPreferences prefs;
  private String id = "";
  private @Nullable ListPreference presetPref;
  private @Nullable AccessibilitySuiteSwitchPreference onPref;
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
          Preset preset = SpeechEqualizer.presetFor((String) newValue);
          if (preset != null) {
            SpeechEqualizer.save(prefs, id, preset.gains());
            update();
          }
          return false;
        });
    screen.addPreference(presetPref);

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

  // The Equalizer reading controls change the gains and the switch while the screen is open.
  private final OnSharedPreferenceChangeListener prefsListener =
      (sharedPrefs, key) -> {
        if (!saving
            && (TextUtils.equals(key, SpeechEqualizer.key(id))
                || TextUtils.equals(key, SpeechEqualizer.onKey(id)))) {
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

  /** Lists the presets, and Custom as well while the gains are no preset's. */
  private void updatePreset(int[] gains) {
    if (presetPref == null) {
      return;
    }
    Context context = requireContext();
    Preset current = SpeechEqualizer.presetOf(gains);
    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> values = new ArrayList<>();
    for (Preset preset : Preset.values()) {
      entries.add(presetName(context, preset));
      values.add(preset.getValue());
    }
    if (current == null) {
      entries.add(context.getString(R.string.speech_equalizer_custom));
      values.add(CUSTOM);
    }
    presetPref.setEntries(entries.toArray(new CharSequence[0]));
    presetPref.setEntryValues(values.toArray(new CharSequence[0]));
    presetPref.setValue(current == null ? CUSTOM : current.getValue());
    presetPref.setSummary("%s");
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
    Preset preset = SpeechEqualizer.presetOf(SpeechEqualizer.read(prefs, profileId));
    return (preset == null)
        ? context.getString(R.string.speech_equalizer_custom)
        : presetName(context, preset);
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
