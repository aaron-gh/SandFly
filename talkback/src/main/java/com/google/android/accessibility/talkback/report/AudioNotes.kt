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

package com.google.android.accessibility.talkback.report

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Notes for problem reports what plays and records each time that changes: the usage and output of
 * every player that plays, SandFly's own speech and sounds among them, the source of every
 * recording and whether Android silences it, and the audio mode. Kept in memory, so the notes are
 * there on phones whose log apps can't read.
 */
object AudioNotes {
  private const val TAG = "AudioNotes"

  /** The player state in [AudioPlaybackConfiguration.toString], which has no public getter. */
  private val STATE = Regex("""state:(\w+)""")

  /**
   * Notes what plays and records now. [playing] and [recording] are the configurations a callback
   * was given, or null to read them.
   */
  @JvmStatic
  fun note(
    audio: AudioManager,
    playing: List<AudioPlaybackConfiguration>? = null,
    recording: List<AudioRecordingConfiguration>? = null,
  ) {
    try {
      val players = (playing ?: audio.activePlaybackConfigurations).mapNotNull(::player)
      val recordings = (recording ?: audio.activeRecordingConfigurations).map(::recording)
      ReportNotes.audio.add(describe(players, recordings, audio.mode))
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Could not read the audio: %s", e)
    }
  }

  /** The usage and output of [config], or null when it isn't playing. */
  private fun player(config: AudioPlaybackConfiguration): String? {
    val state = STATE.find(config.toString())?.groupValues?.get(1)
    if (state != null && state != "started") return null
    val usage = usageName(config.audioAttributes.usage)
    val device =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) config.audioDeviceInfo else null
    return if (device == null) usage else "$usage (${deviceName(device.type)})"
  }

  private fun recording(config: AudioRecordingConfiguration): String {
    val source = sourceName(config.clientAudioSource)
    val silenced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && config.isClientSilenced
    return if (silenced) "$source, silenced" else source
  }

  /** One note: [players] and [recordings] as [player] and [recording] give them, and [mode]. */
  @VisibleForTesting
  @JvmStatic
  fun describe(players: List<String>, recordings: List<String>, mode: Int): String {
    val playing = if (players.isEmpty()) "nothing" else players.joinToString(", ")
    val recorded = if (recordings.isEmpty()) "nothing" else recordings.joinToString(", ")
    return "Playing: $playing; recording: $recorded; mode: ${modeName(mode)}"
  }

  @VisibleForTesting
  @JvmStatic
  fun usageName(usage: Int): String =
    when (usage) {
      AudioAttributes.USAGE_MEDIA -> "media"
      AudioAttributes.USAGE_VOICE_COMMUNICATION -> "voice communication"
      AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> "call signalling"
      AudioAttributes.USAGE_ALARM -> "alarm"
      AudioAttributes.USAGE_NOTIFICATION -> "notification"
      AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "ringtone"
      AudioAttributes.USAGE_NOTIFICATION_EVENT -> "notification event"
      AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY -> "accessibility"
      AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "navigation"
      AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "sonification"
      AudioAttributes.USAGE_GAME -> "game"
      AudioAttributes.USAGE_ASSISTANT -> "assistant"
      AudioAttributes.USAGE_UNKNOWN -> "unknown"
      else -> "usage $usage"
    }

  private fun sourceName(source: Int): String =
    when (source) {
      MediaRecorder.AudioSource.MIC -> "microphone"
      MediaRecorder.AudioSource.CAMCORDER -> "camcorder"
      MediaRecorder.AudioSource.VOICE_RECOGNITION -> "voice recognition"
      MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "voice communication"
      MediaRecorder.AudioSource.UNPROCESSED -> "unprocessed"
      MediaRecorder.AudioSource.VOICE_PERFORMANCE -> "voice performance"
      MediaRecorder.AudioSource.DEFAULT -> "default"
      else -> "source $source"
    }

  private fun deviceName(type: Int): String =
    when (type) {
      AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
      AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "earpiece"
      AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired headset"
      AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "wired headphones"
      AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth call"
      AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
      AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset"
      AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE speaker"
      AudioDeviceInfo.TYPE_USB_DEVICE,
      AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
      else -> "device $type"
    }

  @VisibleForTesting
  @JvmStatic
  fun modeName(mode: Int): String =
    when (mode) {
      AudioManager.MODE_NORMAL -> "normal"
      AudioManager.MODE_RINGTONE -> "ringtone"
      AudioManager.MODE_IN_CALL -> "in call"
      AudioManager.MODE_IN_COMMUNICATION -> "in communication"
      AudioManager.MODE_CALL_SCREENING -> "call screening"
      else -> "mode $mode"
    }
}
