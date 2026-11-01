/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.updatetasks

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.accessibility.talkback.actor.gemini.local.LiteRtCaches
import java.io.File

/**
 * Deletes, once, the cache files that earlier versions left behind: LiteRT-LM caches of deleted
 * models and of processors no longer chosen, and the files that primed the speech engine. The caches
 * of the model in use go too and are made again the next time it loads.
 */
object CacheCleanup {
  private const val TAG = "CacheCleanup"
  private const val PREF_VERSION = "cache_cleanup_version"
  private const val VERSION = 1
  /** The prefix of the files that FailoverTextToSpeech primes the speech engine with. */
  private const val TTS_PRIME_PREFIX = "tmpsynthesize"

  fun run(context: Context, prefs: SharedPreferences) {
    if (prefs.getInt(PREF_VERSION, 0) >= VERSION) return
    prefs.edit().putInt(PREF_VERSION, VERSION).apply()
    val app = context.applicationContext
    Thread(
        {
          try {
            val freed = clean(app.cacheDir)
            Log.i(TAG, "Freed $freed bytes")
          } catch (e: Throwable) {
            Log.w(TAG, "Cache cleanup failed", e)
          }
        },
        "cache-cleanup",
      )
      .start()
  }

  /** Deletes the leftover files in [cacheDir]. Returns the bytes freed. */
  fun clean(cacheDir: File): Long {
    var freed = LiteRtCaches.deleteAll(cacheDir)
    cacheDir.listFiles()?.forEach { file ->
      if (file.isFile && file.name.startsWith(TTS_PRIME_PREFIX)) {
        val bytes = file.length()
        if (file.delete()) freed += bytes
      }
    }
    return freed
  }
}
