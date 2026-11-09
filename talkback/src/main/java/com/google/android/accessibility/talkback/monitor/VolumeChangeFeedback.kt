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

package com.google.android.accessibility.talkback.monitor

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.pause.PauseController
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.accessibility.utils.output.FeedbackController
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Volume keys > Volume change feedback: a tone on each volume step made by the user, a spoken
 * percentage after the steps, or both.
 *
 * Volume changes come from [VolumeMonitor], which is registered for them anyway, so this class
 * registers nothing itself. A track exists only from the start of a tone until shortly after it.
 */
class VolumeChangeFeedback(
  private val context: Context,
  /** SandFly's speech volume, from 0 to 1, which the tone plays at. */
  private val speechVolume: () -> Float,
) {
  private val filter = VolumeChangeFilter()
  private val gate = VolumeToneGate()
  private val handler = Handler(Looper.getMainLooper())
  private val tone by lazy {
    generateVolumeTone(SAMPLE_RATE, TONE_MS.toInt(), TONE_HZ, TONE_AMPLITUDE, FADE_MS)
  }
  private val releaseTrackTask = Runnable { release() }

  private var playsTone = false
  private var speaksPercentage = false
  private var track: AudioTrack? = null

  /** Reads the setting from [prefs]. */
  fun reload(prefs: SharedPreferences) {
    val value =
      prefs.getString(
        context.getString(R.string.pref_volume_change_feedback_key),
        context.getString(R.string.pref_volume_change_feedback_default),
      )
    val both = value == context.getString(R.string.value_volume_change_feedback_tone_and_spoken)
    playsTone = both || value == context.getString(R.string.value_volume_change_feedback_tone)
    speaksPercentage =
      both || value == context.getString(R.string.value_volume_change_feedback_spoken)
    if (!playsTone) {
      release()
    }
  }

  /** Records that SandFly itself is changing a volume, which gets no feedback. */
  fun noteOwnChange() {
    filter.noteOwnChange(SystemClock.uptimeMillis())
  }

  /**
   * Handles a reported change of [stream], and plays the tone if it is a step made by the user.
   * Returns whether the announcement of this change is to say only the percentage.
   */
  fun onVolumeChanged(stream: Int, value: Int, prevValue: Int, callActive: Boolean): Boolean {
    if (!playsTone && !speaksPercentage) {
      return false
    }
    val kind = filter.classify(stream, value, prevValue, SystemClock.uptimeMillis())
    if (
      playsTone &&
        kind == VolumeChangeFilter.Kind.STEP &&
        !callActive &&
        !PauseController.isPaused()
    ) {
      playTone()
    }
    return speaksPercentage &&
      (kind == VolumeChangeFilter.Kind.STEP || kind == VolumeChangeFilter.Kind.REPEAT)
  }

  /** Releases the tone's track, for when the setting is turned off or SandFly stops. */
  fun release() {
    handler.removeCallbacks(releaseTrackTask)
    track?.release()
    track = null
    gate.clear()
  }

  private fun playTone() {
    if (!gate.tryStart(SystemClock.uptimeMillis(), TONE_MS + TONE_LATENCY_MS)) {
      return
    }
    // The previous tone has ended by now. Only one track exists at a time.
    handler.removeCallbacks(releaseTrackTask)
    track?.release()
    track = startTrack()
    if (track == null) {
      gate.clear()
      return
    }
    handler.postDelayed(releaseTrackTask, TONE_MS + RELEASE_AFTER_MS)
  }

  /** Starts a new track playing the tone, with the audio usage speech has now. */
  private fun startTrack(): AudioTrack? {
    var created: AudioTrack? = null
    try {
      val samples = tone
      val newTrack =
        AudioTrack.Builder()
          .setAudioAttributes(
            FeedbackController.feedbackAttributes(FailoverTextToSpeech.speechUsage(context))
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(SAMPLE_RATE)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
          .build()
      created = newTrack
      val written = newTrack.write(samples, 0, samples.size)
      if (written != samples.size || newTrack.state != AudioTrack.STATE_INITIALIZED) {
        LogUtils.w(TAG, "Tone track not ready, written=%d state=%d", written, newTrack.state)
        newTrack.release()
        return null
      }
      newTrack.setVolume(speechVolume().coerceIn(0f, 1f))
      newTrack.play()
      return newTrack
    } catch (e: RuntimeException) {
      // Thrown when the track cannot be created or started, such as with no audio resources left.
      LogUtils.w(TAG, "Cannot play tone: %s", e)
      created?.release()
      return null
    }
  }

  private companion object {
    private const val TAG = "VolumeChangeFeedback"
    private const val SAMPLE_RATE = 48_000
    private const val TONE_HZ = 1_000
    private const val TONE_MS = 50L
    private const val FADE_MS = 5
    private const val TONE_AMPLITUDE = 0.35

    /** Added to the tone's length while the next tone is dropped, for the start of playback. */
    private const val TONE_LATENCY_MS = 50L

    /** How long after the tone ends its track is released if no further tone replaced it. */
    private const val RELEASE_AFTER_MS = 200L
  }
}
