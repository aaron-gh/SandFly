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
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.TextUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Locale;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A speech engine connection of its own for emoji, set up from the voice profile chosen as the
 * emoji voice, for {@link FailoverTextToSpeech}. Speech is sent one fragment at a time, and the next
 * only once the last has ended, so an emoji fragment never overlaps the main engine's speech.
 *
 * <p>The engine is connected for the first emoji and released after {@link #IDLE_MS} without one.
 * An emoji waits up to {@link #INIT_WAIT_MS} for the engine to start. Text the engine refuses, or
 * doesn't start saying within {@link #START_TIMEOUT_MS}, is said with the main engine instead, and
 * the emoji voice isn't used again for {@link #RETRY_AFTER_FAILURE_MS}. All calls are on the main
 * engine's handler thread.
 */
final class EmojiVoice {

  /** Told when the fragment said with the emoji voice starts and ends, on the handler thread. */
  interface Listener {
    void onStart(String utteranceId);

    /** {@code success} is false when it was stopped. */
    void onDone(String utteranceId, boolean success);
  }

  private static final String TAG = "EmojiVoice";

  /** How long an emoji waits for the engine to start before it is said with the main engine. */
  private static final long INIT_WAIT_MS = 3000;

  /** How long the engine has to start saying an emoji it took. */
  private static final long START_TIMEOUT_MS = 3000;

  /** How long an emoji can take to say before it is ended, so speech carries on. */
  private static final long DONE_TIMEOUT_MS = 15_000;

  /** How long the engine stays connected after the last emoji. */
  private static final long IDLE_MS = 5 * 60_000;

  /** How long emoji are said with the main engine after the emoji voice failed. */
  private static final long RETRY_AFTER_FAILURE_MS = 60_000;

  /** How often an unset voice is tried again, at most, when an emoji is said. */
  private static final long VOICE_RETRY_MS = 1000;

  /** The lowest volume, as for the speech volume setting. */
  private static final float MIN_VOLUME = 0.25f;

  private final Context context;
  private final Handler handler;
  private final Listener listener;

  private @Nullable VoiceProfile profile;
  private @Nullable TextToSpeech tts;
  private boolean ready;

  /** Tells reports from an earlier connection apart, which are ignored. */
  private int generation;

  /** When the emoji voice last failed, or 0. */
  private long failedAt;

  /** Whether the profile's voice is set on the connection. */
  private boolean voiceSet;

  /** When the profile's voice was last tried. */
  private long voiceTriedAt;

  /** The engine's voices, listed once per connection, or null until needed. */
  private @Nullable Set<Voice> voices;

  private int nextId;

  /** The emoji being said, or waiting for the engine to start, or null. */
  private @Nullable Request current;

  private static final class Request {
    /** The ID of the main engine's utterance the emoji is a fragment of. */
    final String utteranceId;

    /** The ID it is said with, unique for each, so late reports on earlier ones are ignored. */
    final String engineId;

    final CharSequence text;
    final float rate;
    final float pitch;

    /** Says the emoji with the main engine instead. */
    final Runnable fallback;

    boolean sent;
    boolean started;

    Request(
        String utteranceId,
        String engineId,
        CharSequence text,
        float rate,
        float pitch,
        Runnable fallback) {
      this.utteranceId = utteranceId;
      this.engineId = engineId;
      this.text = text;
      this.rate = rate;
      this.pitch = pitch;
      this.fallback = fallback;
    }
  }

  private final Runnable initTimeout = this::onInitTimeout;
  private final Runnable startTimeout = this::onStartTimeout;
  private final Runnable doneTimeout = this::onDoneTimeout;
  private final Runnable release =
      () -> {
        if (current == null) {
          LogUtils.d(TAG, "Releasing the emoji voice");
          disconnect();
        }
      };

  EmojiVoice(Context context, Handler handler, Listener listener) {
    this.context = context;
    this.handler = handler;
    this.listener = listener;
  }

  /** Returns whether a change to {@code key} can change the emoji voice. */
  static boolean isKey(@Nullable String key) {
    return EmojiSpeech.PREF_VOICE_PROFILE.equals(key) || VoiceProfiles.isProfileKey(key);
  }

  /** Reads the voice profile chosen as the emoji voice. Another engine is connected when needed. */
  void read(SharedPreferences prefs) {
    String id = prefs.getString(EmojiSpeech.PREF_VOICE_PROFILE, "");
    @Nullable VoiceProfile next =
        !TextUtils.isEmpty(id) && VoiceProfiles.ids(prefs).contains(id)
            ? VoiceProfiles.read(prefs, id)
            : null;
    @Nullable VoiceProfile previous = profile;
    if (next == null ? previous == null : next.equals(previous)) {
      return;
    }
    profile = next;
    failedAt = 0;
    if (next == null || previous == null || !next.engine().equals(previous.engine())) {
      endConnection();
    } else if (!next.sameVoiceAs(previous)) {
      voiceSet = false;
      voiceTriedAt = 0;
    }
  }

  /**
   * Returns whether emoji are said with the emoji voice now: one is chosen, it isn't the voice of
   * the voice profile in use, and it hasn't failed lately.
   */
  boolean isInUse() {
    @Nullable VoiceProfile emojiProfile = profile;
    if (emojiProfile == null || emojiProfile.sameVoiceAs(VoiceProfiles.active())) {
      return false;
    }
    return failedAt == 0 || SystemClock.uptimeMillis() - failedAt >= RETRY_AFTER_FAILURE_MS;
  }

  /**
   * Says {@code text}, a fragment of utterance {@code utteranceId}, with the emoji voice, stopping
   * any emoji it is saying. {@code rateFactor} and {@code pitchFactor} multiply the profile's rate
   * and pitch. {@code fallback} says it with the main engine if the emoji voice can't.
   */
  void speak(
      String utteranceId,
      CharSequence text,
      float rateFactor,
      float pitchFactor,
      Runnable fallback) {
    stop();
    @Nullable VoiceProfile emojiProfile = profile;
    if (emojiProfile == null) {
      fallback.run();
      return;
    }
    Request request =
        new Request(
            utteranceId,
            "emoji_" + nextId++,
            text,
            emojiProfile.rate() * rateFactor,
            emojiProfile.pitch() * pitchFactor,
            fallback);
    current = request;
    handler.removeCallbacks(release);
    if (ready && tts != null) {
      send(request);
      return;
    }
    connect();
    if (current == request && !request.sent) {
      handler.postDelayed(initTimeout, INIT_WAIT_MS);
    }
  }

  /** Stops the emoji being said or waiting. Its listener isn't told. */
  void stop() {
    handler.removeCallbacks(initTimeout);
    handler.removeCallbacks(startTimeout);
    handler.removeCallbacks(doneTimeout);
    @Nullable Request stopped = current;
    current = null;
    if (stopped != null && stopped.sent && ready && tts != null) {
      try {
        tts.stop();
      } catch (RuntimeException e) {
        // The engine has stopped.
      }
    }
    releaseWhenIdle();
  }

  /** Stops and lets go of the engine, for good. */
  void shutdown() {
    stop();
    disconnect();
    profile = null;
    handler.removeCallbacks(release);
  }

  private void connect() {
    if (tts != null) {
      // Starting already: it takes the waiting emoji when it's ready.
      return;
    }
    @Nullable VoiceProfile emojiProfile = profile;
    if (emojiProfile == null) {
      return;
    }
    int connection = ++generation;
    ready = false;
    voiceSet = false;
    voiceTriedAt = 0;
    voices = null;
    LogUtils.d(TAG, "Connecting %s", emojiProfile.engine());
    try {
      tts =
          new TextToSpeech(
              context,
              status -> handler.post(() -> onInit(connection, status)),
              emojiProfile.engine().isEmpty() ? null : emojiProfile.engine());
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Can't connect %s: %s", emojiProfile.engine(), e);
      tts = null;
      fail();
    }
  }

  /** Ends the connection. Reports from it are ignored from now on. */
  private void disconnect() {
    generation++;
    handler.removeCallbacks(release);
    @Nullable TextToSpeech connection = tts;
    tts = null;
    ready = false;
    voiceSet = false;
    voices = null;
    if (connection != null) {
      try {
        connection.setOnUtteranceProgressListener(null);
      } catch (RuntimeException e) {
        // Shut down below either way.
      }
      TextToSpeechUtils.attemptTtsShutdown(connection);
    }
  }

  /** Gives up on the emoji voice for a while. */
  private void fail() {
    failedAt = Math.max(1, SystemClock.uptimeMillis());
    endConnection();
  }

  /**
   * Ends the connection. An emoji waiting for it is said with the main engine, and one being said
   * is reported ended, so that speech carries on.
   */
  private void endConnection() {
    @Nullable Request waiting = current;
    current = null;
    handler.removeCallbacks(initTimeout);
    handler.removeCallbacks(startTimeout);
    handler.removeCallbacks(doneTimeout);
    disconnect();
    if (waiting == null) {
      return;
    }
    if (waiting.started) {
      listener.onDone(waiting.utteranceId, /* success= */ true);
    } else {
      waiting.fallback.run();
    }
  }

  private void onInit(int connection, int status) {
    if (connection != generation || tts == null) {
      return;
    }
    if (status != TextToSpeech.SUCCESS) {
      LogUtils.e(TAG, "%s didn't start: %d", profile == null ? null : profile.engine(), status);
      fail();
      return;
    }
    TextToSpeech started = tts;
    try {
      started.setOnUtteranceProgressListener(progressListener(connection));
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Can't listen to the emoji voice: %s", e);
      fail();
      return;
    }
    ready = true;
    applyVoice(started);
    @Nullable Request waiting = current;
    if (waiting != null && !waiting.sent) {
      handler.removeCallbacks(initTimeout);
      send(waiting);
    } else {
      releaseWhenIdle();
    }
  }

  private UtteranceProgressListener progressListener(int connection) {
    return new UtteranceProgressListener() {
      @Override
      public void onStart(String engineId) {
        handler.post(() -> onStarted(connection, engineId));
      }

      @Override
      public void onDone(String engineId) {
        handler.post(() -> onEnded(connection, engineId, /* success= */ true));
      }

      @Override
      public void onStop(String engineId, boolean interrupted) {
        handler.post(() -> onEnded(connection, engineId, !interrupted));
      }

      @Deprecated
      @Override
      public void onError(String engineId) {
        handler.post(() -> onFailed(connection, engineId));
      }

      @Override
      public void onError(String engineId, int errorCode) {
        handler.post(() -> onFailed(connection, engineId));
      }
    };
  }

  /** Returns the request said as {@code engineId} on {@code connection}, if it is the current one. */
  private @Nullable Request currentFor(int connection, @Nullable String engineId) {
    @Nullable Request request = current;
    return connection == generation && request != null && request.engineId.equals(engineId)
        ? request
        : null;
  }

  private void send(Request request) {
    @Nullable TextToSpeech connection = tts;
    if (connection == null) {
      fail();
      return;
    }
    request.sent = true;
    int result;
    try {
      connection.setAudioAttributes(
          CapturePolicy.applyTo(
                  new AudioAttributes.Builder()
                      .setUsage(FailoverTextToSpeech.speechUsage(context))
                      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH),
                  SharedPreferencesUtils.getSharedPreferences(context))
              .build());
      long now = SystemClock.uptimeMillis();
      if (!voiceSet && now - voiceTriedAt >= VOICE_RETRY_MS) {
        // An engine that has just started may not have loaded its voices yet.
        voices = null;
        applyVoice(connection);
      }
      connection.setSpeechRate(Math.max(0.1f, Math.min(6f, request.rate)));
      connection.setPitch(Math.max(0.2f, Math.min(2f, request.pitch)));
      @Nullable VoiceProfile emojiProfile = profile;
      float volume = emojiProfile == null ? 1f : emojiProfile.volume() / 100f;
      Bundle params = new Bundle();
      params.putFloat(
          TextToSpeech.Engine.KEY_PARAM_VOLUME, Math.max(MIN_VOLUME, Math.min(1f, volume)));
      result = connection.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, request.engineId);
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Can't send an emoji: %s", e);
      result = TextToSpeech.ERROR;
    }
    if (result != TextToSpeech.SUCCESS) {
      // The connection is gone, such as after the engine was stopped.
      LogUtils.e(TAG, "%s didn't take an emoji", profile == null ? null : profile.engine());
      fail();
      return;
    }
    handler.postDelayed(startTimeout, START_TIMEOUT_MS);
  }

  private void onStarted(int connection, String engineId) {
    @Nullable Request request = currentFor(connection, engineId);
    if (request == null || request.started) {
      return;
    }
    request.started = true;
    handler.removeCallbacks(startTimeout);
    handler.postDelayed(doneTimeout, DONE_TIMEOUT_MS);
    listener.onStart(request.utteranceId);
  }

  private void onEnded(int connection, String engineId, boolean success) {
    @Nullable Request request = currentFor(connection, engineId);
    if (request == null) {
      return;
    }
    finish(request, success);
  }

  /** An emoji the engine failed before saying it is said with the main engine. */
  private void onFailed(int connection, String engineId) {
    @Nullable Request request = currentFor(connection, engineId);
    if (request == null) {
      return;
    }
    LogUtils.e(TAG, "%s failed an emoji", profile == null ? null : profile.engine());
    if (request.started) {
      // Speech carries on after it.
      finish(request, /* success= */ true);
      return;
    }
    current = null;
    handler.removeCallbacks(startTimeout);
    handler.removeCallbacks(doneTimeout);
    request.fallback.run();
    releaseWhenIdle();
  }

  private void finish(Request request, boolean success) {
    current = null;
    handler.removeCallbacks(startTimeout);
    handler.removeCallbacks(doneTimeout);
    if (!request.started) {
      listener.onStart(request.utteranceId);
    }
    listener.onDone(request.utteranceId, success);
    releaseWhenIdle();
  }

  private void onInitTimeout() {
    @Nullable Request request = current;
    if (request == null || request.sent) {
      return;
    }
    LogUtils.e(TAG, "The emoji voice didn't start within %d ms", INIT_WAIT_MS);
    // The engine can still start, for the next emoji.
    current = null;
    request.fallback.run();
    releaseWhenIdle();
  }

  private void onStartTimeout() {
    @Nullable Request request = current;
    if (request == null || request.started) {
      return;
    }
    LogUtils.e(TAG, "The emoji voice didn't start saying an emoji within %d ms", START_TIMEOUT_MS);
    fail();
  }

  private void onDoneTimeout() {
    @Nullable Request request = current;
    if (request == null) {
      return;
    }
    LogUtils.e(TAG, "The emoji voice didn't end an emoji within %d ms", DONE_TIMEOUT_MS);
    try {
      if (tts != null) {
        tts.stop();
      }
    } catch (RuntimeException e) {
      // The engine has stopped.
    }
    finish(request, /* success= */ true);
  }

  /** Releases the engine once nothing is said or waiting. */
  private void releaseWhenIdle() {
    handler.removeCallbacks(release);
    if (current == null && tts != null) {
      handler.postDelayed(release, IDLE_MS);
    }
  }

  /**
   * Sets the profile's voice as the main engine does: its voice, else its language, else the
   * engine's default voice.
   */
  private void applyVoice(TextToSpeech connection) {
    @Nullable VoiceProfile emojiProfile = profile;
    if (emojiProfile == null) {
      return;
    }
    voiceTriedAt = SystemClock.uptimeMillis();
    try {
      @Nullable Voice voice = findVoice(connection, emojiProfile.voice());
      if (voice != null && connection.setVoice(voice) == TextToSpeech.SUCCESS) {
        voiceSet = true;
        return;
      }
      if (!emojiProfile.language().isEmpty()
          && connection.setLanguage(Locale.forLanguageTag(emojiProfile.language()))
              >= TextToSpeech.LANG_AVAILABLE) {
        voiceSet = emojiProfile.voice().isEmpty();
        return;
      }
      @Nullable Voice engineDefault = connection.getDefaultVoice();
      if (engineDefault != null) {
        connection.setVoice(engineDefault);
      }
      voiceSet = emojiProfile.voice().isEmpty();
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Can't set the emoji voice: %s", e);
    }
  }

  /** Returns the engine's installed voice called {@code name}, or null. */
  private @Nullable Voice findVoice(TextToSpeech connection, String name) {
    if (name.isEmpty()) {
      return null;
    }
    if (voices == null) {
      voices = connection.getVoices();
    }
    @Nullable Set<Voice> listed = voices;
    if (listed == null) {
      return null;
    }
    for (Voice voice : listed) {
      if (voice != null && name.equals(voice.getName())) {
        Set<String> features = voice.getFeatures();
        boolean installed =
            features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
        return installed ? voice : null;
      }
    }
    return null;
  }
}
