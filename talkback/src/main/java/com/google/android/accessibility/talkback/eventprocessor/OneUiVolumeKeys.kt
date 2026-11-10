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

package com.google.android.accessibility.talkback.eventprocessor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.utils.output.FailoverTextToSpeech

/**
 * Volume key presses on One UI while accessibility audio plays. With no suggested stream, One UI
 * adjusts the accessibility stream while it is active and for 1 s after it stops, so speech and the
 * spoken volume itself keep media volume from changing. These presses are kept from Android and
 * adjust the stream One UI uses when accessibility audio is not active.
 */
class OneUiVolumeKeys(
  private val context: Context,
  /** Runs when a kept press changes the volume. */
  private val onVolumeAdjusted: () -> Unit,
) {
  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val keptKeys = mutableSetOf<Int>()
  private var started = false
  private var playing = false
  private var stoppedAt = NEVER

  private val playback =
    object : AudioManager.AudioPlaybackCallback() {
      override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
        onPlaybackChanged(configs)
      }
    }

  fun start() {
    val audio = audioManager ?: return
    if (started || !Build.MANUFACTURER.equals(SAMSUNG, ignoreCase = true)) return
    started = true
    audio.registerAudioPlaybackCallback(playback, handler)
    onPlaybackChanged(audio.activePlaybackConfigurations)
  }

  fun stop() {
    if (!started) return
    started = false
    audioManager?.unregisterAudioPlaybackCallback(playback)
    keptKeys.clear()
  }

  /** Forgets the keys held, whose releases may not come with the screen off. */
  fun onScreenOff() {
    keptKeys.clear()
  }

  /**
   * Handles a volume key event and returns whether it is kept from Android. [canUse] says whether
   * SandFly may take the press now.
   */
  fun onKeyEvent(event: KeyEvent, canUse: () -> Boolean): Boolean {
    if (!started || !VolumeKeyPresses.isVolumeKey(event.keyCode)) return false
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        val repeated = event.repeatCount > 0
        if (!repeated) {
          keptKeys.remove(event.keyCode)
          if (
            suggestedStream() != AudioManager.USE_DEFAULT_STREAM_TYPE && !isAppCall() && canUse()
          ) {
            keptKeys.add(event.keyCode)
          }
        }
        if (event.keyCode !in keptKeys) return false
        VolumeKeyCommands.adjustSuggestedVolume(
          audioManager,
          event.keyCode,
          repeated,
          suggestedStream(),
        )
        onVolumeAdjusted()
        true
      }
      KeyEvent.ACTION_UP -> keptKeys.remove(event.keyCode)
      else -> false
    }
  }

  /**
   * The stream to suggest to Android for a press now: [AudioManager.USE_DEFAULT_STREAM_TYPE] unless
   * One UI would adjust the accessibility stream because of speech and sounds.
   */
  fun suggestedStream(): Int {
    if (
      !started ||
        !FailoverTextToSpeech.shouldUseAccessibilityStream(context) ||
        !isAccessibilityStreamActive(playing, stoppedAt, SystemClock.uptimeMillis())
    ) {
      return AudioManager.USE_DEFAULT_STREAM_TYPE
    }
    return streamWithoutAccessibilityAudio(adjustsMediaVolumeOnly(), isVoiceCapable())
  }

  /** Whether an app call is going on, whose volume the keys are left to change. */
  private fun isAppCall(): Boolean {
    val audio = audioManager ?: return false
    return VolumeKeyCommands.isAppCall(
      audio.activePlaybackConfigurations.map { it.audioAttributes.usage },
      FailoverTextToSpeech.speechUsage(context),
      audio.mode,
    )
  }

  private fun onPlaybackChanged(configs: List<AudioPlaybackConfiguration>) {
    val nowPlaying =
      configs.any { it.audioAttributes.usage == AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY }
    if (playing && !nowPlaying) {
      stoppedAt = SystemClock.uptimeMillis()
    }
    playing = nowPlaying
  }

  /** Reads Settings > Sounds and vibration > Volume > Use Volume keys for media. */
  private fun adjustsMediaVolumeOnly(): Boolean =
    try {
      Settings.System.getInt(context.contentResolver, ADJUST_MEDIA_VOLUME_ONLY, 1) > 0
    } catch (e: RuntimeException) {
      true
    }

  private fun isVoiceCapable(): Boolean =
    context.getSystemService(TelephonyManager::class.java)?.isVoiceCapable ?: true

  companion object {
    private const val SAMSUNG = "samsung"
    private const val ADJUST_MEDIA_VOLUME_ONLY = "adjust_media_volume_only"

    /** One UI's 1 s, and the delay until the playback callback reports the stop. */
    private const val ACTIVE_AFTER_STOP_MS = 1_500L
    private const val NEVER = Long.MIN_VALUE

    /** Whether One UI counts the accessibility stream as active at [now]. */
    @VisibleForTesting
    @JvmStatic
    fun isAccessibilityStreamActive(playing: Boolean, stoppedAt: Long, now: Long): Boolean =
      playing || (stoppedAt != NEVER && now - stoppedAt < ACTIVE_AFTER_STOP_MS)

    /** The stream One UI adjusts for a press when no stream is active. */
    @VisibleForTesting
    @JvmStatic
    fun streamWithoutAccessibilityAudio(mediaVolumeOnly: Boolean, voiceCapable: Boolean): Int =
      if (mediaVolumeOnly || !voiceCapable) AudioManager.STREAM_MUSIC else AudioManager.STREAM_RING
  }
}
