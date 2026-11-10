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

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import com.google.android.accessibility.talkback.actor.gemini.local.OnDeviceAiSettings
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Keep recognition models loaded, Until SandFly stops: loads the on-device model a minute after
 * SandFly starts, and when that value is chosen. Releases Whisper and the on-device model when
 * SandFly stops, and when another value is chosen.
 *
 * Loading and releasing run in order on one background thread, so that a release always comes
 * after a load that was under way.
 */
@MainThread
class RecognitionModelKeeper(context: Context) {
  private val appContext = context.applicationContext
  private val prefs = SharedPreferencesUtils.getSharedPreferences(appContext)
  private val executor =
    ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "recognition-model-keeper").apply { isDaemon = true }
      }
      .apply {
        setKeepAliveTime(30, TimeUnit.SECONDS)
        allowCoreThreadTimeOut(true)
        removeOnCancelPolicy = true
      }
  private var preloading: ScheduledFuture<*>? = null
  private var untilStopped = false
  private var started = false
  @Volatile private var stopped = false

  // Held here, since SharedPreferences keeps its listeners only weakly.
  private val listener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (stopped) return@OnSharedPreferenceChangeListener
      if (key in MODEL_CHOICE_KEYS) {
        // A kept model that on-device AI no longer uses is never unloaded on idle, so it is
        // released now, and the one in use, if any, loaded instead.
        if (untilStopped) {
          executor.execute { release() }
          schedulePreload(/* delayMs= */ 0)
        }
        return@OnSharedPreferenceChangeListener
      }
      if (key != ModelRetention.KEY) return@OnSharedPreferenceChangeListener
      val now = ModelRetention.untilStopped(prefs)
      if (now == untilStopped) return@OnSharedPreferenceChangeListener
      untilStopped = now
      if (now) {
        schedulePreload(/* delayMs= */ 0)
      } else {
        preloading?.cancel(false)
        preloading = null
        executor.execute { release() }
      }
    }

  /** Call when SandFly starts. */
  fun start() {
    if (started || stopped) return
    started = true
    WhisperEngines.setServiceRunning(true)
    prefs.registerOnSharedPreferenceChangeListener(listener)
    untilStopped = ModelRetention.untilStopped(prefs)
    if (untilStopped) schedulePreload(PRELOAD_DELAY_MS)
  }

  /** Call when SandFly stops. Releases the models, whatever the setting. */
  fun stop() {
    if (stopped) return
    stopped = true
    WhisperEngines.setServiceRunning(false)
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
    preloading?.cancel(false)
    preloading = null
    executor.execute { release() }
    executor.shutdown()
  }

  private fun schedulePreload(delayMs: Long) {
    preloading?.cancel(false)
    preloading = executor.schedule({ preload() }, delayMs, TimeUnit.MILLISECONDS)
  }

  private fun preload() {
    // Anything that escapes would stop SandFly.
    try {
      if (stopped || !ModelRetention.untilStopped(prefs)) return
      LocalModelManager.get(appContext).preload()
    } catch (e: Throwable) {
      LogUtils.w(TAG, "Could not load the on-device model ahead: %s", e)
    }
  }

  private fun release() {
    try {
      WhisperEngines.releaseKept()
      LocalModelManager.get(appContext).release()
    } catch (e: Throwable) {
      LogUtils.w(TAG, "Could not release the recognition models: %s", e)
    }
  }

  private companion object {
    const val TAG = "RecognitionModelKeeper"

    /** After SandFly starts, so that loading does not slow down the start. */
    const val PRELOAD_DELAY_MS = 60_000L

    /** Settings that change which on-device model answers requests, or whether one does. */
    val MODEL_CHOICE_KEYS =
      setOf(
        OnDeviceAiSettings.PREF_PROVIDER,
        OnDeviceAiSettings.PREF_MODEL,
        OnDeviceAiSettings.PREF_PROCESSOR,
      )
  }
}
