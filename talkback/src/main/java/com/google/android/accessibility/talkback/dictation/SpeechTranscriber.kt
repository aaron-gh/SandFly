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

/** Recognizes speech on the phone, from recorded audio. */
interface SpeechTranscriber {
  /**
   * The text spoken in [samples], 16 kHz mono audio from -1 to 1, in [language], a two-letter
   * code. Blocks until done. Returns null when it failed or was cancelled.
   */
  fun transcribe(samples: FloatArray, language: String): String?

  /** Stops [transcribe]. Safe to call from any thread. */
  fun cancel()
}
