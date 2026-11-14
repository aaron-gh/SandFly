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

package com.google.android.accessibility.talkback.selector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.audio.AudioDeviceRouter;
import com.google.android.accessibility.talkback.focusmanagement.LiftToActivateMode;
import com.google.android.accessibility.talkback.gesture.GestureProfiles;
import com.google.android.accessibility.talkback.keyboardecho.KeyboardEchoSettings;
import com.google.android.accessibility.talkback.preference.base.FocusDelayPrefFragment;
import com.google.android.accessibility.talkback.preference.base.TypingFocusDelayPrefFragment;
import com.google.android.accessibility.talkback.selector.SelectorController.Setting;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.talkback.translation.TranslationSettings;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.SpeechEqualizer;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The value of a reading control, said after its name when the reading control is selected, such
 * as "60 percent" or "on". The texts are those said when the value changes.
 */
final class ReadingControlValues {

  private ReadingControlValues() {}

  /** {@code name} followed by {@code value}, or {@code name} alone without a value. */
  static String withValue(String name, @Nullable String value) {
    return TextUtils.isEmpty(value) ? name : name + ", " + value;
  }

  /**
   * The value of {@code setting} from settings and the system, or null for a reading control
   * without one here: navigation by text or element, lists of actions, and the reading controls
   * whose value {@link SelectorController} knows.
   */
  static @Nullable String of(Context context, SharedPreferences prefs, Setting setting) {
    Resources res = context.getResources();
    switch (setting) {
      case PUNCTUATION -> {
        String[] entries = res.getStringArray(R.array.pref_punctuation_entries);
        int level =
            parseInt(
                SharedPreferencesUtils.getStringPref(
                    prefs,
                    res,
                    R.string.pref_punctuation_verbosity,
                    R.string.pref_punctuation_verbosity_default));
        return level >= 0 && level < entries.length ? entries[level] : null;
      }
      case EMOJI -> {
        String[] values = res.getStringArray(R.array.pref_emoji_speech_values);
        String[] entries = res.getStringArray(R.array.pref_emoji_speech_entries);
        String current =
            SharedPreferencesUtils.getStringPref(
                prefs, res, R.string.pref_emoji_speech_key, R.string.pref_emoji_speech_default);
        return entries[Math.max(0, Arrays.asList(values).indexOf(current))];
      }
      case KEYBOARD_ECHO_ON_SCREEN,
          KEYBOARD_ECHO_PHYSICAL,
          KEYBOARD_ECHO_BRAILLE_KEYBOARD,
          KEYBOARD_ECHO_BRAILLE_DISPLAY,
          KEYBOARD_ECHO_HANDWRITING_KEYBOARD -> {
        return KeyboardEchoSettings.choiceName(
            res,
            KeyboardEchoSettings.read(prefs, res, SelectorController.keyboardEchoInput(setting)));
      }
      case FORMATTING -> {
        return onOff(
            context,
            SharedPreferencesUtils.getBooleanPref(
                prefs,
                res,
                R.string.pref_formatting_inline_key,
                R.bool.pref_formatting_inline_default));
      }
      case AUDIO_FOCUS -> {
        return onOff(
            context,
            SharedPreferencesUtils.getBooleanPref(
                prefs, res, R.string.pref_use_audio_focus_key, R.bool.pref_use_audio_focus_default));
      }
      case CHANGE_ACCESSIBILITY_VOLUME -> {
        return context.getString(
            R.string.tb_template_percent, String.valueOf(accessibilityVolumePercent(context)));
      }
      case SOUND_MODE -> {
        return SoundModeControl.current(context);
      }
      case AUTO_TRANSLATION -> {
        return onOff(context, TranslationSettings.INSTANCE.auto(prefs));
      }
      case CHANGE_BRIGHTNESS -> {
        if (!ScreenBrightness.canWrite(context)) {
          return null;
        }
        if (ScreenBrightness.isAdaptive(context)) {
          return context.getString(R.string.quick_settings_adaptive_brightness);
        }
        return context.getString(
            R.string.tb_template_percent, String.valueOf(ScreenBrightness.getPercent(context)));
      }
      case CHANGE_TOUCH_FOCUS_LATENCY -> {
        return focusDelayText(context, prefs);
      }
      case CHANGE_TYPING_FOCUS_LATENCY -> {
        return typingFocusDelayText(context, prefs);
      }
      case CHANGE_LIFT_TO_ACTIVATE -> {
        LiftToActivateMode mode =
            LiftToActivateMode.fromPrefValue(
                prefs.getString(context.getString(R.string.pref_lift_to_activate_key), null));
        return res.getStringArray(R.array.pref_lift_to_activate_entries)[mode.ordinal()];
      }
      case WRAP_AROUND -> {
        return onOff(
            context,
            SharedPreferencesUtils.getBooleanPref(
                prefs, res, R.string.pref_wrap_around_key, R.bool.pref_wrap_around_default));
      }
      case TABLET_HELD_UP_FACES_AWAY -> {
        return onOff(context, BrailleKeyboardSettings.isTabletHeldUpFacesAway(context));
      }
      case CONTROL_TELLING_TIME -> {
        return onOff(
            context,
            SharedPreferencesUtils.getBooleanPref(
                prefs, res, R.string.pref_speak_time_key, R.bool.pref_tell_time_default));
      }
      case SWITCH_TTS_ENGINE -> {
        // A voice profile in use sets the engine, and the reading control can't change it.
        if (VoiceProfiles.isProfileActive()) {
          return null;
        }
        @Nullable String engine = FailoverTextToSpeech.getSelectedEngine(context);
        return TextUtils.isEmpty(engine)
            ? null
            : FailoverTextToSpeech.getEngineDisplayName(context, engine);
      }
      case SPEECH_STREAM -> {
        int index =
            FailoverTextToSpeech.STREAM_VALUES.indexOf(
                FailoverTextToSpeech.chosenSpeechStream(prefs));
        String[] names = res.getStringArray(R.array.pref_speech_stream_entries);
        return index >= 0 && index < names.length ? names[index] : null;
      }
      case AUDIO_OUTPUT -> {
        List<kotlin.Pair<String, String>> choices = AudioDeviceRouter.outputChoices(context);
        List<String> values = new ArrayList<>();
        for (kotlin.Pair<String, String> choice : choices) {
          values.add(choice.getFirst());
        }
        int index =
            AudioDeviceRouter.AudioTarget.indexOfChoice(
                values,
                SharedPreferencesUtils.getStringPref(
                    prefs,
                    res,
                    R.string.pref_audio_output_device_key,
                    R.string.pref_audio_output_device_default));
        return index >= 0 ? choices.get(index).getSecond() : null;
      }
      case SWITCH_VOICE_PROFILE -> {
        return VoiceProfileNames.nameOf(context, prefs, VoiceProfiles.activeId(prefs));
      }
      case EQUALIZER -> {
        return onOff(context, SpeechEqualizer.isOn(prefs, VoiceProfiles.activeId(prefs)));
      }
      case GESTURE_PROFILE -> {
        String id = GestureProfiles.activeId(prefs);
        for (kotlin.Pair<String, String> choice : GestureProfiles.choices(context, prefs)) {
          if (choice.getFirst().equals(id)) {
            return choice.getSecond();
          }
        }
        return null;
      }
      default -> {
        return null;
      }
    }
  }

  /** "on" or "off". */
  static String onOff(Context context, boolean on) {
    return context.getString(on ? R.string.value_on : R.string.value_off);
  }

  /** A band's gain as spoken, such as "+3 dB". */
  static String equalizerGainText(Context context, int gain) {
    String number = (gain > 0 ? "+" : "") + NumberFormat.getIntegerInstance().format(gain);
    return context.getString(R.string.speech_equalizer_gain_db, number);
  }

  /** The accessibility volume as a percentage of its range. */
  static int accessibilityVolumePercent(Context context) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    int min =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            ? audioManager.getStreamMinVolume(AudioManager.STREAM_ACCESSIBILITY)
            : 0;
    int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY);
    int volume = audioManager.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY);
    return max > min ? Math.round(100f * (volume - min) / (max - min)) : 100;
  }

  /** The name of the Focus delay in use, or "" for a delay not among the choices. */
  static String focusDelayText(Context context, SharedPreferences prefs) {
    int timeout =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            R.string.pref_touch_focus_time_out_key,
            R.string.pref_touch_focus_time_out_default);
    for (FocusDelayPrefFragment.FocusDelayPref source :
        FocusDelayPrefFragment.FocusDelayPref.values()) {
      if (timeout == source.getDelay()) {
        return context.getString(source.getTitleId());
      }
    }
    return "";
  }

  /** The name of the Typing focus delay in use, or "" for a delay not among the choices. */
  static String typingFocusDelayText(Context context, SharedPreferences prefs) {
    int timeout =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_focus_time_out_key,
            R.string.pref_touch_explore_time_out_default);
    for (TypingFocusDelayPrefFragment.TypingFocusDelayPref source :
        TypingFocusDelayPrefFragment.TypingFocusDelayPref.values()) {
      if (timeout == source.getDelay()) {
        return context.getString(source.getTitleId());
      }
    }
    return "";
  }

  private static int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
