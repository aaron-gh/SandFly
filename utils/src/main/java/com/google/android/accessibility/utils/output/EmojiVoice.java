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

package com.google.android.accessibility.utils.output;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The voice emoji are said with: a {@link ProfileVoice} for the voice profile chosen as the emoji
 * voice, for {@link FailoverTextToSpeech}.
 */
final class EmojiVoice {

  private final ProfileVoice voice;

  EmojiVoice(Context context, Handler handler, ProfileVoice.Listener listener) {
    voice = new ProfileVoice(context, handler, listener, "EmojiVoice");
  }

  /** Returns whether a change to {@code key} can change the emoji voice. */
  static boolean isKey(@Nullable String key) {
    return EmojiSpeech.PREF_VOICE_PROFILE.equals(key)
        || VoiceProfiles.isProfileKey(key)
        || SpeechEqualizer.isKey(key);
  }

  /** Reads the voice profile chosen as the emoji voice. Another engine is connected when needed. */
  void read(SharedPreferences prefs) {
    String id = prefs.getString(EmojiSpeech.PREF_VOICE_PROFILE, "");
    boolean exists = !TextUtils.isEmpty(id) && VoiceProfiles.ids(prefs).contains(id);
    @Nullable VoiceProfile profile = exists ? VoiceProfiles.read(prefs, id) : null;
    voice.setProfile(profile, exists ? profileEqualizer(prefs, id) : null);
  }

  /** The equalizer gains of voice profile {@code id}, or null if its equalizer is off or flat. */
  static int @Nullable [] profileEqualizer(SharedPreferences prefs, String id) {
    if (!SpeechEqualizer.isOn(prefs, id)) {
      return null;
    }
    int[] gains = SpeechEqualizer.read(prefs, id);
    return SpeechEqualizer.isFlat(gains) ? null : gains;
  }

  /** See {@link ProfileVoice#isInUse}. */
  boolean isInUse() {
    return voice.isInUse();
  }

  /** See {@link ProfileVoice#speak}. */
  void speak(
      String utteranceId,
      CharSequence text,
      float rateFactor,
      float pitchFactor,
      Runnable fallback) {
    voice.speak(utteranceId, text, rateFactor, pitchFactor, fallback);
  }

  void stop() {
    voice.stop();
  }

  void shutdown() {
    voice.shutdown();
  }
}
