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

package com.google.android.accessibility.talkback.translation

/**
 * Translation on the phone, with a model per language that is downloaded first. Languages are
 * [TranslationLanguages] codes. Callbacks come on the main thread.
 */
interface OnDeviceTranslation {
  /** The languages that can be translated from and into. */
  val languages: Set<String>

  /**
   * The language of [text], or null when it can't be told or isn't in [languages]. [unavailable]
   * is true when language identification itself can't run yet, such as while Google Play services
   * downloads it on first use.
   */
  fun detect(text: String, done: (language: String?, unavailable: Boolean) -> Unit)

  /** The languages whose models are on the phone. */
  fun downloaded(done: (Set<String>) -> Unit)

  /** Downloads [language]'s model, on Wi-Fi only when [wifiOnly], and tells whether it worked. */
  fun download(language: String, wifiOnly: Boolean, done: (Boolean) -> Unit)

  /** Whether [language]'s model is downloading now. */
  fun isDownloading(language: String): Boolean

  fun delete(language: String, done: (Boolean) -> Unit)

  /** Translates [text] with downloaded models; [done] gets the translation, or null when it failed. */
  fun translate(text: String, from: String, to: String, done: (String?) -> Unit)

  fun close()
}
