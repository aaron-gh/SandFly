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
  const val KEY = "pref_keep_models_loaded_minutes"
  private const val DEFAULT_MINUTES = "0"
  /** The value of Until SandFly stops. The others are minutes. */
  private const val UNTIL_STOPPED_VALUE = "until_stopped"
  private const val MS_PER_MINUTE = 60_000L

  /** What [keepLoadedMs] and [onDeviceIdleMs] return for Until SandFly stops: never unload. */
  const val UNTIL_STOPPED = Long.MAX_VALUE

  /** Whether models stay loaded until SandFly stops, and the on-device model loads at its start. */
  @JvmStatic
  fun untilStopped(prefs: SharedPreferences): Boolean =
    prefs.getString(KEY, DEFAULT_MINUTES) == UNTIL_STOPPED_VALUE

  /**
   * How long a Whisper model stays loaded after use, 0 to unload it at once, or [UNTIL_STOPPED].
   */
  @JvmStatic
  fun keepLoadedMs(prefs: SharedPreferences): Long {
    val value = prefs.getString(KEY, DEFAULT_MINUTES)
    if (value == UNTIL_STOPPED_VALUE) return UNTIL_STOPPED
    return (value?.toLongOrNull() ?: 0L).coerceIn(0L, MAX_MINUTES) * MS_PER_MINUTE
  }

  /**
   * How long the on-device model stays loaded after use, or [UNTIL_STOPPED]. It always stays for at
   * least its usual idle time, since loading it for each request would be slow.
   */
  @JvmStatic
  fun onDeviceIdleMs(prefs: SharedPreferences): Long =
    maxOf(keepLoadedMs(prefs), LiteRtLmLocalLlm.DEFAULT_IDLE_UNLOAD_MS)

  /** The most minutes read from the setting, so that the milliseconds cannot overflow. */
  private const val MAX_MINUTES = 24 * 60L
}
