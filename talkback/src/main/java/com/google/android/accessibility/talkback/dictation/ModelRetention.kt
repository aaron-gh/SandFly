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

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.actor.gemini.local.LiteRtLmLocalLlm

/** Keep recognition models loaded: how long Whisper and the on-device model stay in memory. */
object ModelRetention {
  private const val KEY = "pref_keep_models_loaded_minutes"
  private const val DEFAULT_MINUTES = "0"
  private const val MS_PER_MINUTE = 60_000L

  /** How long a Whisper model stays loaded after use, or 0 to unload it at once. */
  @JvmStatic
  fun keepLoadedMs(prefs: SharedPreferences): Long =
    (prefs.getString(KEY, DEFAULT_MINUTES)?.toLongOrNull() ?: 0L) * MS_PER_MINUTE

  /**
   * How long the on-device model stays loaded after use. It always stays for at least its usual
   * idle time, since loading it for each request would be slow.
   */
  @JvmStatic
  fun onDeviceIdleMs(prefs: SharedPreferences): Long =
    maxOf(keepLoadedMs(prefs), LiteRtLmLocalLlm.DEFAULT_IDLE_UNLOAD_MS)
}
