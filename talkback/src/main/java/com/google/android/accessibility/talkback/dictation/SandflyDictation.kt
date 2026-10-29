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
 * correction chosen in Dictation settings, until [listen] is called again. Main thread only.
 */
object SandflyDictation {
  /** The dictation of the running screen reader, or null when it isn't running. */
  @Volatile internal var instance: AiDictation? = null

  /** Whether SandFly's dictation can be used: the screen reader is running. */
  @JvmStatic
  val isAvailable: Boolean
    get() = instance != null

  /** Whether it is recording or recognizing for [listen]. */
  @JvmStatic
  val isListening: Boolean
    get() = instance?.isListening == true

  /**
   * Starts recording, or stops recording when it is on. [onText] gets the text, or null when
   * nothing was recognized or recording couldn't start. Returns false when it can't start.
   */
  @JvmStatic
  fun listen(onText: (String?) -> Unit): Boolean {
    val dictation = instance
    if (dictation == null) {
      onText(null)
      return false
    }
    return dictation.listen(onText)
  }

  /** Stops recording for [listen], so that what was said is recognized. */
  @JvmStatic
  fun stop() {
    instance?.takeIf { it.isListening }?.start(null)
  }
}
