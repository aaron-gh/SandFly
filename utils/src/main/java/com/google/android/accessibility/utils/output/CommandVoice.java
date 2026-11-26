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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The voice profile that the result of a command or of a text edit is said with, instead of the
 * speech engine of {@link SpeechControllerImpl}. The speech controller does everything else with
 * the speech, such as the muting, the filters, the history and the queue, and asks this voice only
 * to say the text of an item flagged {@link FeedbackItem#FLAG_COMMAND_FEEDBACK}.
 */
public interface CommandVoice {

  /** Told how a text said by the voice ended, on any thread. */
  interface Callback {
    /** The voice started to say the text. */
    void onStart();

    /** The voice said the text, or it was stopped or dropped. */
    void onDone(boolean spoken);

    /** The voice couldn't say the text soon enough, so SandFly's own voice should say it. */
    void onUnavailable();
  }

  /** Whether the result of commands is said in a voice profile now. */
  boolean isActive();

  /**
   * Whether the voice talks over SandFly's speech, instead of waiting in its queue. Speech that is
   * said over doesn't hold back the speech after it.
   */
  boolean isOverSpeech();

  /** Says {@code text}. {@code callback} is told how it ended. */
  void say(CharSequence text, @Nullable Callback callback);

  /**
   * Stops what the voice is saying, and nothing else that the voice profile says, such as
   * notifications. With {@code includingOverSpeech} false, the speech that is said over SandFly's
   * speech goes on.
   */
  void stop(boolean includingOverSpeech);
}
