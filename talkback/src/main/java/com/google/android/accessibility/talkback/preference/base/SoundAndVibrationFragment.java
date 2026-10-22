/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.TextUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.audio.AudioDeviceRouter;
import com.google.android.accessibility.talkback.audio.SpeakerDevices;
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants;
import com.google.android.accessibility.talkback.speakerphone.SpeakerphoneSettings;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Fragment to display sound and vibration settings. */
public class SoundAndVibrationFragment extends TalkbackBaseFragment {
  private static final int PERCENT_MIN = 5;
  private static final int PERCENT_STEP = 5;

  /** Preferences managed by this activity. */
  private SharedPreferences prefs;

  public SoundAndVibrationFragment() {
    super(R.xml.sound_and_vibration_preferences);
  }

  @Override
  public CharSequence getTitle() {
    // Changes title by supporting vibration or not. Preference Vibration Feedback will be removed
    // by TalkBackPreferenceFilter if this device doesn't support vibration.
    int titleResid =
        FeatureSupport.isVibratorSupported(getContext())
            ? R.string.title_pref_sound_and_vibration
            : R.string.title_pref_sound;
    return getText(titleResid);
  }

  /** Listens to shared preference changes and updates the preference items accordingly. */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPrefs, key) -> {
        FragmentActivity activity = getActivity();
        if (TextUtils.equals(key, getString(R.string.pref_use_audio_focus_key))) {
          updateTwoStatePreferenceStatus(
              activity, R.string.pref_use_audio_focus_key, R.bool.pref_use_audio_focus_default);
        } else if (TextUtils.equals(key, getString(R.string.pref_soundback_key))) {
          updateTwoStatePreferenceStatus(
              activity, R.string.pref_soundback_key, R.bool.pref_soundback_default);
        } else if (TextUtils.equals(key, getString(R.string.pref_vibration_key))) {
          if (FeatureSupport.isVibratorSupported(getContext())) {
            updateTwoStatePreferenceStatus(
                activity, R.string.pref_vibration_key, R.bool.pref_vibration_default);
          }
        } else if (TextUtils.equals(key, getString(R.string.pref_audio_output_device_key))) {
          updateAudioOutputPreference();
        } else if (TextUtils.equals(key, getString(R.string.pref_speaker_devices_key))) {
          updateSpeakerDevicesPreference();
        }
      };

  private final AudioDeviceCallback audioDeviceCallback =
      new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
          updateAudioOutputPreference();
          updateSpeakerDevicesPreference();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
          updateAudioOutputPreference();
          updateSpeakerDevicesPreference();
        }
      };

  /**
   * The speakerphone setting needs a permission that the user grants with adb, and Android only has
   * it from Android 12. Until it is granted, the setting is off, says how to grant it and has an
   * action that copies the command.
   */
  private void updateSpeakerphonePreference() {
    Preference pref = findPreference(getString(R.string.pref_speakerphone_away_from_ear_key));
    if (pref == null) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_speakerphone_away_from_ear_key);
      return;
    }
    boolean granted = SpeakerphoneSettings.INSTANCE.canControlCalls(getContext());
    String command = SpeakerphoneSettings.INSTANCE.grantCommand(getContext());
    // With Shizuku running, turning the setting on grants the permission.
    pref.setEnabled(granted || ShizukuGrants.INSTANCE.isReady());
    pref.setSummary(
        granted
            ? getString(R.string.summary_pref_speakerphone_away_from_ear)
            : getString(R.string.summary_pref_speakerphone_needs_permission, command));
    if (pref instanceof CommandSwitchPreference) {
      CommandSwitchPreference commandPreference = (CommandSwitchPreference) pref;
      commandPreference.setCommand(granted ? null : command);
      commandPreference.setGrant(ShizukuGrants.Grant.MANAGE_ONGOING_CALLS);
      commandPreference.setNeedsGrant(true);
      commandPreference.setOnGrantFinished(
          () -> {
            if (isAdded()) {
              updateSpeakerphonePreference();
            }
          });
    }
  }

  private void updateAudioOutputPreference() {
    FragmentActivity activity = getActivity();
    if (activity == null) {
      return;
    }
    Preference preference =
        PreferenceSettingsUtils.findPreference(activity, getString(R.string.pref_audio_output_device_key));
    if (preference instanceof ListPreference) {
      ListPreference listPref = (ListPreference) preference;
      AudioDeviceRouter.updatePreference(listPref, activity);
    }
  }

  /** Fills the phone speaker devices with the outputs connected now and those seen before. */
  private void updateSpeakerDevicesPreference() {
    FragmentActivity activity = getActivity();
    if (activity == null) {
      return;
    }
    Preference preference = findPreference(getString(R.string.pref_speaker_devices_key));
    if (!(preference instanceof MultiSelectListPreference)) {
      return;
    }
    MultiSelectListPreference devicesPref = (MultiSelectListPreference) preference;
    List<String> connected = new ArrayList<>();
    AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager != null) {
      for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
        if (AudioDeviceRouter.Companion.isExternalDevice(device)) {
          connected.add(AudioDeviceRouter.Companion.getDeviceDisplayName(device, activity));
        }
      }
    }
    Set<String> chosen = SpeakerDevices.chosen(prefs, activity);
    List<String> choices =
        SpeakerDevices.choices(connected, SpeakerDevices.seen(prefs, activity), chosen);
    CharSequence[] names = choices.toArray(new CharSequence[0]);
    devicesPref.setEntries(names);
    devicesPref.setEntryValues(names);
    devicesPref.setEnabled(!choices.isEmpty());
    if (choices.isEmpty()) {
      devicesPref.setSummary(R.string.summary_pref_speaker_devices_no_devices);
    } else if (chosen.isEmpty()) {
      devicesPref.setSummary(R.string.summary_pref_speaker_devices_none);
    } else {
      devicesPref.setSummary(TextUtils.join(", ", new TreeSet<>(chosen)));
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    prefs = SharedPreferencesUtils.getSharedPreferences(getContext());
    setUpPercentSlider(R.string.pref_soundback_volume_seekbar_key_int);
    // The volume is kept as text, as the list it replaced kept it.
    if (findPreference(getString(R.string.pref_soundback_volume_seekbar_key_int))
        instanceof SeekBarPreference volume) {
      volume.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            prefs
                .edit()
                .putString(getString(R.string.pref_soundback_volume_key), newValue.toString())
                .apply();
            return true;
          });
    }
    setUpPercentSlider(R.string.pref_vibration_strength_key);
    // Only vibrators that vary their strength can play a weaker vibration.
    Vibrator vibrator = getContext().getSystemService(Vibrator.class);
    if (vibrator == null || !vibrator.hasVibrator() || !vibrator.hasAmplitudeControl()) {
      PreferenceSettingsUtils.hidePreference(
          getContext(), getPreferenceScreen(), R.string.pref_vibration_strength_key);
    }
  }

  /** Sets a slider to go from 5 to 100 percent in steps of 5. */
  private void setUpPercentSlider(int keyResId) {
    if (findPreference(getString(keyResId)) instanceof SeekBarPreference slider) {
      slider.setMin(PERCENT_MIN);
      slider.setMax(100);
      slider.setSeekBarIncrement(PERCENT_STEP);
    }
  }

  private void updateSoundVolumeSlider() {
    if (findPreference(getString(R.string.pref_soundback_volume_seekbar_key_int))
        instanceof SeekBarPreference volume) {
      volume.setValue(
          SharedPreferencesUtils.getIntFromStringPref(
              prefs,
              getResources(),
              R.string.pref_soundback_volume_key,
              R.string.pref_soundback_volume_default));
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    FragmentActivity activity = this.getActivity();
    prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    updateSoundVolumeSlider();

    updateTwoStatePreferenceStatus(
        activity, R.string.pref_use_audio_focus_key, R.bool.pref_use_audio_focus_default);
    updateTwoStatePreferenceStatus(
        activity, R.string.pref_soundback_key, R.bool.pref_soundback_default);
    if (FeatureSupport.isVibratorSupported(getContext())) {
      updateTwoStatePreferenceStatus(
          activity, R.string.pref_vibration_key, R.bool.pref_vibration_default);
    }

    updateAudioOutputPreference();
    updateSpeakerDevicesPreference();
    updateSpeakerphonePreference();
    Preference audioPref =
        PreferenceSettingsUtils.findPreference(activity, getString(R.string.pref_audio_output_device_key));
    if (audioPref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) audioPref;
      listPref.setOnPreferenceClickListener(
          p -> {
            AudioDeviceRouter.updatePreference(listPref, activity);
            return false;
          });
      listPref.setOnPreferenceChangeListener(
          (p, newValue) -> {
            listPref.setValue((String) newValue);
            AudioDeviceRouter.updatePreference(listPref, activity);
            AudioDeviceRouter router = AudioDeviceRouter.getInstance();
            if (router != null) {
              router.setPreferredDevice((String) newValue);
            }
            return true;
          });
    }

    AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager != null) {
      audioManager.registerAudioDeviceCallback(audioDeviceCallback, new Handler(Looper.getMainLooper()));
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    FragmentActivity activity = getActivity();
    if (activity != null) {
      AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
      if (audioManager != null) {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
      }
    }
  }

  /**
   * Updates the status of preference to on or off after the selector or context menu change the
   * state while the activity is visible.
   *
   * @param activity FragmentActivity which contain Fragments.
   * @param preferenceKeyResId key string id of Preference which likes to find
   * @param preferenceDefaultKeyResId default key string id of Preference which likes to find
   */
  private static void updateTwoStatePreferenceStatus(
      FragmentActivity activity, int preferenceKeyResId, int preferenceDefaultKeyResId) {

    @Nullable Preference preference =
        PreferenceSettingsUtils.findPreference(activity, activity.getString(preferenceKeyResId));
    if (preference instanceof TwoStatePreference) {
      // Make sure that we have the latest value of preference before continuing.
      boolean enabledState =
          SharedPreferencesUtils.getBooleanPref(
              SharedPreferencesUtils.getSharedPreferences(activity.getApplicationContext()),
              activity.getResources(),
              preferenceKeyResId,
              preferenceDefaultKeyResId);

      ((TwoStatePreference) preference).setChecked(enabledState);
    }
  }
}
