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

package com.google.android.accessibility.talkback.dictation

/**
 * SandFly's dictation for speech input that isn't typed into a field: questions about images and
 * the screen, voice commands, the SandFly Pro agent's prompt. It records with the engine and AI
 * correction chosen in Dictation settings, until the speech pauses or [listen] is called again.
 * Main thread only.
 */
object SandflyDictation {
  /** The dictation of the running screen reader, or null when it isn't running. */
  @Volatile internal var instance: AiDictation? = null

  /** Whether SandFly's dictation can be used: the screen reader is running. */
  @JvmStatic
  val isAvailable: Boolean
    get() = instance != null

  /** Whether a dictation into a field or a [listen] is recording or recognizing. */
  @JvmStatic
  val isActive: Boolean
    get() = instance?.isActive == true

  /** Whether it is recording or recognizing for [listen]. */
  @JvmStatic
  val isListening: Boolean
    get() = instance?.isListening == true

  /**
   * Starts recording, or stops recording when it is on. [onText] gets the text, or null when
   * nothing was recognized or recording couldn't start. Returns false when it can't start. With
   * [correct] false, the text isn't corrected with AI, as for voice commands.
   */
  @JvmStatic
  @JvmOverloads
  fun listen(correct: Boolean = true, onText: (String?) -> Unit): Boolean =
    listen(correct, false, true, onText)

  /**
   * [listen] for Start voice editing, which listens again and again: nothing recognized isn't said,
   * the start chime plays only with [startChime], and the stop chime only when speech was heard.
   */
  @JvmStatic
  fun listen(correct: Boolean, session: Boolean, startChime: Boolean, onText: (String?) -> Unit): Boolean {
    val dictation = instance
    if (dictation == null) {
      onText(null)
      return false
    }
    return dictation.listen(correct, session, startChime, onText)
  }

  /**
   * The error that ended the last [listen] without text: a SpeechRecognizer error, or
   * [LISTEN_TRANSCRIPTION_FAILED]; 0 when nothing was said.
   */
  @JvmStatic
  val lastListenError: Int
    get() = instance?.lastListenError ?: 0

  /** [lastListenError] when the recording couldn't be transcribed. */
  const val LISTEN_TRANSCRIPTION_FAILED = -1

  /** Stops [listen] and drops what was recorded, without giving it to its callback. */
  @JvmStatic
  fun cancelListen() {
    instance?.cancelListen()
  }

  /**
   * Whether a text change at [eventTime], in uptime milliseconds, is from text that Dictate typed
   * and spoke with Speak dictated text, so that the change itself isn't spoken.
   */
  @JvmStatic
  fun typedSpokenText(eventTime: Long): Boolean = instance?.typedSpokenText(eventTime) == true

  /** Stops recording for [listen], so that what was said is recognized. */
  @JvmStatic
  fun stop() {
    instance?.takeIf { it.isListening }?.start(null)
  }
}
