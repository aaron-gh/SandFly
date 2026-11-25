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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;

/**
 * Says sample text with a voice profile as speech says text with it: its engine, voice, volume,
 * rate, pitch and equalizer, with the text-to-speech default rate and pitch of the system applied as
 * {@link FailoverTextToSpeech} applies them. For a settings screen; all calls are on the main
 * thread. The engine is connected for the first sample and released by {@link #shutdown}, or after
 * a while without one.
 */
public final class VoiceProfileSample {

  private static final String UTTERANCE_ID = "VoiceProfileSample";

  private final Context context;
  private final ProfileVoice voice;

  public VoiceProfileSample(Context context) {
    this.context = context.getApplicationContext();
    voice =
        new ProfileVoice(
            this.context,
            new Handler(Looper.getMainLooper()),
            new ProfileVoice.Listener() {
              @Override
              public void onStart(String utteranceId) {}

              @Override
              public void onDone(String utteranceId, boolean success) {}
            },
            "VoiceProfileSample");
  }

  /**
   * Says {@code text} with {@code profile}, as saved in {@code prefs}, stopping a sample being said.
   * {@code onFailed} runs if the engine doesn't start or doesn't take the text.
   */
  public void play(
      SharedPreferences prefs, VoiceProfile profile, CharSequence text, Runnable onFailed) {
    voice.setProfile(profile, EmojiVoice.profileEqualizer(prefs, profile.id()));
    ContentResolver resolver = context.getContentResolver();
    float rateFactor = Secure.getInt(resolver, Secure.TTS_DEFAULT_RATE, 100) / 100f;
    float pitchFactor = Secure.getInt(resolver, Secure.TTS_DEFAULT_PITCH, 100) / 100f;
    voice.speak(UTTERANCE_ID, text, rateFactor, pitchFactor, onFailed);
  }

  /** Stops the sample being said. */
  public void stop() {
    voice.stop();
  }

  /** Stops and releases the engine. */
  public void shutdown() {
    voice.shutdown();
  }
}
