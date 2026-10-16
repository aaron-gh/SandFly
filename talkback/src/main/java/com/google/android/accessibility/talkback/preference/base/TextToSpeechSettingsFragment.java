package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.ServiceStateListener;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;
import java.util.List;
import java.util.function.IntFunction;

public class TextToSpeechSettingsFragment extends TalkbackBaseFragment {

  private static final int[] SEEK_BAR_KEYS = {
    R.string.pref_speech_volume_seekbar_key_int,
    R.string.pref_speech_rate_seekbar_key_int,
    R.string.pref_speech_pitch_seekbar_key_int,
  };

  private SharedPreferences prefs;

  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPrefs, key) -> {
        if (TextUtils.equals(key, getString(R.string.pref_speech_rate_key))
            || TextUtils.equals(key, getString(R.string.pref_speech_pitch_key))) {
          updateSeekBarValues();
        } else if (TextUtils.equals(key, VoiceProfiles.PREF_ACTIVE)) {
          // Switched by a gesture, keyboard shortcut or the Backtalk menu.
          updateVoiceProfileSummary();
        }
      };

  public TextToSpeechSettingsFragment() {
    super(R.xml.text_to_speech_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.tts_preferences_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext());

    setUpEnginePreference();
    setUpSystemTtsSettingsPreference();

    setUpSeekBar(
        R.string.pref_speech_volume_seekbar_key_int,
        getResources().getInteger(R.integer.pref_speech_volume_min),
        /* max= */ 100,
        R.string.pref_speech_volume_key,
        Integer::toString);
    setUpSeekBar(
        R.string.pref_speech_rate_seekbar_key_int,
        toPercent(SpeechRateAndPitchActor.RATE_MINIMUM),
        toPercent(SpeechRateAndPitchActor.RATE_MAXIMUM),
        R.string.pref_speech_rate_key,
        TextToSpeechSettingsFragment::fromPercent);
    setUpSeekBar(
        R.string.pref_speech_pitch_seekbar_key_int,
        toPercent(SpeechRateAndPitchActor.PITCH_MINIMUM),
        toPercent(SpeechRateAndPitchActor.PITCH_MAXIMUM),
        R.string.pref_speech_pitch_key,
        TextToSpeechSettingsFragment::fromPercent);
  }

  @Override
  public void onResume() {
    super.onResume();
    prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    updateSeekBarValues();
    updateVoiceProfileSummary();

    boolean serviceActive =
        TalkBackService.getServiceState() == ServiceStateListener.SERVICE_STATE_ACTIVE;
    for (int keyResId : SEEK_BAR_KEYS) {
      setEnabled(requireContext(), keyResId, serviceActive);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  private void setUpEnginePreference() {
    if (!(findPreferenceByResId(R.string.pref_tts_engine_key) instanceof ListPreference enginePref)) {
      return;
    }

    Context context = requireContext();
    List<String> engines = FailoverTextToSpeech.getInstalledTtsEngines(context.getPackageManager());
    CharSequence[] entries = new CharSequence[engines.size() + 1];
    CharSequence[] values = new CharSequence[engines.size() + 1];
    entries[0] = getString(R.string.summary_pref_tts_engine_default);
    values[0] = "";
    for (int i = 0; i < engines.size(); i++) {
      entries[i + 1] = FailoverTextToSpeech.getEngineDisplayName(context, engines.get(i));
      values[i + 1] = engines.get(i);
    }
    enginePref.setEntries(entries);
    enginePref.setEntryValues(values);

    enginePref.setSummaryProvider(
        preference -> {
          CharSequence entry = ((ListPreference) preference).getEntry();
          return (entry == null) ? entries[0] : entry;
        });
  }

  /**
   * Names the voice profile in use, if any, since the settings on this screen are Backtalk
   * default's and don't change how a profile sounds.
   */
  private void updateVoiceProfileSummary() {
    Preference preference = findPreferenceByResId(R.string.pref_voice_profiles_screen_key);
    if (preference == null) {
      return;
    }
    VoiceProfile profile = VoiceProfiles.readActive(prefs);
    preference.setSummary(
        (profile == null)
            ? null
            : getString(
                R.string.voice_profile_in_use,
                VoiceProfileNames.nameOf(requireContext(), prefs, profile.id())));
  }

  private void setUpSystemTtsSettingsPreference() {
    Preference preference = findPreferenceByResId(R.string.pref_system_tts_settings_key);
    if (preference == null) {
      return;
    }

    Intent intent = new Intent(TalkBackService.INTENT_TTS_SETTINGS);
    if (FormFactorUtils.isAndroidTv()
        || !SettingsUtils.allowLinksOutOfSettings(requireContext())
        || !PreferenceSettingsUtils.canHandleIntent(requireContext(), intent)) {
      getPreferenceScreen().removePreference(preference);
      return;
    }
    preference.setIntent(intent);
  }

  private void setUpSeekBar(
      @StringRes int seekBarKeyResId,
      int min,
      int max,
      @StringRes int prefKeyResId,
      IntFunction<String> toPrefValue) {
    if (!(findPreferenceByResId(seekBarKeyResId) instanceof SeekBarPreference seekBar)) {
      return;
    }
    seekBar.setMin(min);
    seekBar.setMax(max);
    seekBar.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          prefs
              .edit()
              .putString(getString(prefKeyResId), toPrefValue.apply((Integer) newValue))
              .apply();
          return true;
        });
  }

  private void updateSeekBarValues() {
    Resources res = getResources();
    setSeekBarValue(
        R.string.pref_speech_volume_seekbar_key_int,
        SharedPreferencesUtils.getIntFromStringPref(
            prefs, res, R.string.pref_speech_volume_key, R.string.pref_speech_volume_default));
    setSeekBarValue(
        R.string.pref_speech_rate_seekbar_key_int,
        toPercent(
            SharedPreferencesUtils.getFloatFromStringPref(
                prefs, res, R.string.pref_speech_rate_key, R.string.pref_speech_rate_default)));
    setSeekBarValue(
        R.string.pref_speech_pitch_seekbar_key_int,
        toPercent(
            SharedPreferencesUtils.getFloatFromStringPref(
                prefs, res, R.string.pref_speech_pitch_key, R.string.pref_speech_pitch_default)));
  }

  private void setSeekBarValue(@StringRes int seekBarKeyResId, int value) {
    if (findPreferenceByResId(seekBarKeyResId) instanceof SeekBarPreference seekBar) {
      seekBar.setValue(value);
    }
  }

  static int toPercent(float multiplier) {
    return Math.round(multiplier * 100);
  }

  static String fromPercent(int percent) {
    return Float.toString(percent / 100f);
  }
}
