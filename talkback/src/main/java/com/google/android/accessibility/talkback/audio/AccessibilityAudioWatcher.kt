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

package com.google.android.accessibility.talkback.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executor

/**
 * Resets the accessibility audio output with [AccessibilityAudioReset] when Android sends it to the
 * earpiece outside a call, if the setting is on and Shizuku already
 * allows SandFly. It checks when the audio mode, the communication device, the connected devices or
 * the playing audio change.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AccessibilityAudioWatcher(private val context: Context) {
  private val audio: AudioManager? = context.getSystemService(AudioManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private var started = false
  private var resetting = false
  private var lastResetAt = 0L

  private val checkNow = Runnable { check() }
  private val checkAfterCall = Runnable { check() }

  private val devices =
    object : AudioDeviceCallback() {
      override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = schedule()

      override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = schedule()
    }

  private val modeListener = AudioManager.OnModeChangedListener { mode ->
    schedule()
    // Telecom changes the communication device after the mode, so the end of a call is checked
    // again once it has settled.
    if (mode == AudioManager.MODE_NORMAL) {
      handler.removeCallbacks(checkAfterCall)
      handler.postDelayed(checkAfterCall, CALL_END_SETTLE_MS)
    }
  }

  private val communicationListener = AudioManager.OnCommunicationDeviceChangedListener { schedule() }

  private val playback =
    object : AudioManager.AudioPlaybackCallback() {
      override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) = schedule()
    }

  private val prefsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == AccessibilityAudioReset.PREF_ENABLED) listenWhileEnabled()
    }

  // Whether the audio callbacks are registered: only while the setting is on.
  private var listening = false

  fun start() {
    if (audio == null || started) return
    started = true
    prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    listenWhileEnabled()
  }

  fun stop() {
    if (audio == null || !started) return
    started = false
    prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    listenWhileEnabled()
  }

  /** Registers the audio callbacks while the service runs and the setting is on, else removes them. */
  private fun listenWhileEnabled() {
    val audio = audio ?: return
    val wanted = started && AccessibilityAudioReset.isEnabled(prefs)
    if (wanted == listening) return
    listening = wanted
    if (wanted) {
      val onMain = Executor { handler.post(it) }
      audio.registerAudioDeviceCallback(devices, handler)
      audio.addOnModeChangedListener(onMain, modeListener)
      audio.addOnCommunicationDeviceChangedListener(onMain, communicationListener)
      audio.registerAudioPlaybackCallback(playback, handler)
      schedule()
    } else {
      handler.removeCallbacks(checkNow)
      handler.removeCallbacks(checkAfterCall)
      audio.unregisterAudioDeviceCallback(devices)
      audio.removeOnModeChangedListener(modeListener)
      audio.removeOnCommunicationDeviceChangedListener(communicationListener)
      audio.unregisterAudioPlaybackCallback(playback)
    }
  }

  /** Checks once things have stopped changing, as playback changes come in bursts. */
  private fun schedule() {
    handler.removeCallbacks(checkNow)
    handler.postDelayed(checkNow, DEBOUNCE_MS)
  }

  private fun check() {
    val audio = audio ?: return
    if (!started || resetting || !AccessibilityAudioReset.isEnabled(prefs) || !ShizukuShell.isAllowed()) {
      return
    }
    val stuck =
      try {
        audio.mode == AudioManager.MODE_NORMAL &&
          audio.getAudioDevicesForAttributes(ACCESSIBILITY).any { it.type == EARPIECE }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Could not read the audio outputs: %s", e)
        false
      }
    if (!stuck) return
    val now = SystemClock.uptimeMillis()
    // If a reset doesn't hold, something sets the earpiece again on purpose.
    if (lastResetAt != 0L && now - lastResetAt < RETRY_MS) return
    lastResetAt = now
    resetting = true
    LogUtils.i(TAG, "Accessibility audio is on the earpiece outside a call, resetting it")
    ShizukuShell.run(context, ShizukuCommand.RESET_ACCESSIBILITY_AUDIO, "") { worked ->
      resetting = false
      LogUtils.i(TAG, "Accessibility audio reset: %s", worked)
    }
  }

  private companion object {
    const val TAG = "AccessibilityAudioWatcher"
    const val DEBOUNCE_MS = 500L
    const val CALL_END_SETTLE_MS = 3_000L
    const val RETRY_MS = 30_000L
    const val EARPIECE = AudioDeviceInfo.TYPE_BUILTIN_EARPIECE

    val ACCESSIBILITY: AudioAttributes =
      AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).build()
  }
}
