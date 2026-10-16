/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils.output;

import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Estimates how far a speech engine has got through an utterance, for engines that report no word
 * positions, so that paused speech can resume near where it stopped instead of from the start.
 *
 * <p>The engine's speed is learned from utterances it finishes, as characters per second at a
 * speech rate of 1. The estimate steps back a little and then to the start of the sentence or
 * clause, so resuming may repeat a few words but does not skip any.
 */
public final class SpeechProgressEstimator {
  /** Utterances shorter than this are mostly silence before and after, so they are not learned. */
  static final int MIN_LEARN_LENGTH = 40;

  /** Characters per second at a speech rate of 1, until the engine's speed is learned. */
  static final float DEFAULT_CHARS_PER_SECOND = 14f;

  /** How much speech to step back before looking for the start of the phrase. */
  static final long SAFETY_MS = 400;

  /** How much each finished utterance moves the learned speed. */
  private static final float SMOOTHING = 0.3f;

  private static final class Utterance {
    final CharSequence text;
    final float rate;
    final @Nullable String engine;
    long startMs = -1;
    boolean reportsRanges;

    Utterance(CharSequence text, float rate, @Nullable String engine) {
      this.text = text;
      this.rate = rate;
      this.engine = engine;
    }
  }

  private final Map<String, Utterance> utterances = new HashMap<>();
  private final Map<String, Float> charsPerSecond = new HashMap<>();
  private @Nullable String currentId;

  /** Records an utterance sent to the engine as one piece. */
  public synchronized void onQueued(
      String utteranceId, CharSequence text, float rate, @Nullable String engine) {
    utterances.put(utteranceId, new Utterance(text, rate > 0 ? rate : 1f, engine));
  }

  public synchronized void onStarted(String utteranceId, long nowMs) {
    Utterance utterance = utterances.get(utteranceId);
    if (utterance != null) {
      utterance.startMs = nowMs;
      currentId = utteranceId;
    }
  }

  /**
   * The utterance was paused for {@code pausedMs} and carried on from where it stopped, so that
   * time is not counted as speaking.
   */
  public synchronized void onPaused(String utteranceId, long pausedMs) {
    Utterance utterance = utterances.get(utteranceId);
    if (utterance != null && utterance.startMs >= 0 && pausedMs > 0) {
      utterance.startMs += pausedMs;
    }
  }

  /** The engine reported a word position, so no estimate is needed for this utterance. */
  public synchronized void onRange(String utteranceId) {
    Utterance utterance = utterances.get(utteranceId);
    if (utterance != null) {
      utterance.reportsRanges = true;
    }
  }

  /** The utterance finished, or was stopped if {@code completed} is false. */
  public synchronized void onFinished(String utteranceId, long nowMs, boolean completed) {
    Utterance utterance = utterances.remove(utteranceId);
    if (utterance == null) {
      return;
    }
    if (utteranceId.equals(currentId)) {
      currentId = null;
    }
    long elapsedMs = nowMs - utterance.startMs;
    if (!completed
        || utterance.reportsRanges
        || utterance.startMs < 0
        || utterance.text.length() < MIN_LEARN_LENGTH
        || elapsedMs <= 0) {
      return;
    }
    float sample = utterance.text.length() * 1000f / elapsedMs / utterance.rate;
    Float learned = charsPerSecond.get(utterance.engine);
    charsPerSecond.put(
        utterance.engine, learned == null ? sample : learned + SMOOTHING * (sample - learned));
  }

  /** The utterance that is speaking, or null. */
  public synchronized @Nullable String currentUtteranceId() {
    return currentId;
  }

  /**
   * Returns where to resume the utterance that is speaking, as an offset into its text, or 0 when
   * it should start over or nothing is speaking.
   */
  public synchronized int estimateResumeOffset(long nowMs) {
    Utterance utterance = currentId == null ? null : utterances.get(currentId);
    if (utterance == null || utterance.startMs < 0 || utterance.reportsRanges) {
      return 0;
    }
    Float learned = charsPerSecond.get(utterance.engine);
    float speed = (learned == null ? DEFAULT_CHARS_PER_SECOND : learned) * utterance.rate;
    long spokenMs = nowMs - utterance.startMs - SAFETY_MS;
    if (spokenMs <= 0) {
      return 0;
    }
    return phraseStartBefore(utterance.text, (int) (spokenMs * speed / 1000f));
  }

  /**
   * Returns the start of the sentence or clause that contains {@code position}: just after the
   * last sentence or clause punctuation and space before it, or 0 if there is none.
   */
  static int phraseStartBefore(CharSequence text, int position) {
    for (int i = Math.min(position, text.length() - 1); i >= 2; i--) {
      char punctuation = text.charAt(i - 2);
      if (Character.isWhitespace(text.charAt(i - 1))
          && !Character.isWhitespace(text.charAt(i))
          && (punctuation == '.'
              || punctuation == '!'
              || punctuation == '?'
              || punctuation == ';'
              || punctuation == ':'
              || punctuation == ',')) {
        return i;
      }
    }
    return 0;
  }
}
