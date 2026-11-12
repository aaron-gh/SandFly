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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.content.SharedPreferences

/**
 * Model download server: where model files listed on Hugging Face are downloaded from, Hugging Face
 * or hf-mirror.com, which serves the same files at the same paths. Downloads from other servers,
 * such as FUTO's, are left as they are.
 */
object ModelServer {
  const val KEY = "pref_model_download_server"
  const val HUGGING_FACE = "huggingface"
  const val HF_MIRROR = "hf-mirror"

  private const val HUGGING_FACE_ROOT = "https://huggingface.co/"
  private const val HF_MIRROR_ROOT = "https://hf-mirror.com/"

  /** [url] on the chosen server. */
  @JvmStatic
  fun url(prefs: SharedPreferences, url: String): String =
    urlOn(prefs.getString(KEY, HUGGING_FACE), url)

  /** [url] on [server], one of [HUGGING_FACE] and [HF_MIRROR]. */
  @JvmStatic
  fun urlOn(server: String?, url: String): String =
    if (server == HF_MIRROR && url.startsWith(HUGGING_FACE_ROOT)) {
      HF_MIRROR_ROOT + url.substring(HUGGING_FACE_ROOT.length)
    } else {
      url
    }
}
