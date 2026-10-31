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
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.SandFlyVolume
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executor

/**
 * Keeps [SandFlyVolume.PREF_ON_BLUETOOTH_KEY] up to date: whether media plays on a Bluetooth device
 * while no call is on. During a call, speech keeps the accessibility volume, which call audio
 * doesn't silence.
 */
class BluetoothOutputWatcher(private val context: Context) {
  private val audio: AudioManager? = context.getSystemService(AudioManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private var started = false

  private val devices =
    object : AudioDeviceCallback() {
      override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = check()

      override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = check()
    }

  private val modeListener: Any? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      AudioManager.OnModeChangedListener { check() }
    } else {
      null
    }

  fun start() {
    val audio = audio ?: return
    if (started) return
    started = true
    audio.registerAudioDeviceCallback(devices, handler)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      audio.addOnModeChangedListener(
        Executor { handler.post(it) },
        modeListener as AudioManager.OnModeChangedListener,
      )
    }
    check()
  }

  fun stop() {
    val audio = audio ?: return
    if (!started) return
    started = false
    audio.unregisterAudioDeviceCallback(devices)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      audio.removeOnModeChangedListener(modeListener as AudioManager.OnModeChangedListener)
    }
  }

  private fun check() {
    val audio = audio ?: return
    val onBluetooth =
      try {
        audio.mode == AudioManager.MODE_NORMAL && mediaOutputs(audio).any { it.type in BLUETOOTH }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Could not read the media output: %s", e)
        false
      }
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    if (prefs.getBoolean(SandFlyVolume.PREF_ON_BLUETOOTH_KEY, false) != onBluetooth) {
      LogUtils.d(TAG, "Media on Bluetooth: %s", onBluetooth)
      prefs.edit().putBoolean(SandFlyVolume.PREF_ON_BLUETOOTH_KEY, onBluetooth).apply()
    }
  }

  /** Where media plays, or the outputs connected where Android can't say. */
  private fun mediaOutputs(audio: AudioManager): List<AudioDeviceInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      audio.getAudioDevicesForAttributes(MEDIA)
    } else {
      audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }

  private companion object {
    const val TAG = "BluetoothOutputWatcher"

    val MEDIA: AudioAttributes =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    // Headphones, speakers and hearing aids. Call audio, SCO, isn't media's output.
    val BLUETOOTH =
      setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
      )
  }
}
