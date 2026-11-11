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
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyCommands
import com.google.android.accessibility.talkback.pause.PauseController
import com.google.android.accessibility.talkback.soundthemes.SoundThemeManifest
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.accessibility.utils.output.FeedbackController
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Volume keys > Volume change feedback: a tone on each volume step made by the user, at the volume
 * of the stream that changed, an announcement of the new volume after the steps, or both. Either can
 * be silenced while an app plays media.
 *
 * Volume changes come from [VolumeMonitor], which is registered for them anyway, so this class
 * registers nothing itself. A track exists only from the start of a tone until shortly after it.
 *
 * A sound theme can replace the tone with a sound file, which a media player plays instead, at the
 * volume of the stream that changed too. The player is kept while steps follow each other, and
 * released shortly after its sound ends.
 */
class VolumeChangeFeedback(private val context: Context) {
  private val filter = VolumeChangeFilter()
  private val gate = VolumeToneGate()
  private val handler = Handler(Looper.getMainLooper())
  private val tone by lazy {
    generateVolumeTone(SAMPLE_RATE, TONE_MS.toInt(), TONE_HZ, TONE_AMPLITUDE, FADE_MS)
  }
  private val releaseTrackTask = Runnable { release() }

  private var playsTone = false
  private var speaksVolume = false
  private var toneDuringMedia = true
  private var speechDuringMedia = true
  private var track: AudioTrack? = null

  /** The theme's file for the tone, [SoundThemeManifest.NONE] for none, or null for the tone. */
  private var customSound: String? = null
  private var player: MediaPlayer? = null
  private var playerSound: String? = null
  private var playerUsage = 0

  /** Reads the setting from [prefs]. */
  fun reload(prefs: SharedPreferences) {
    migrateSpeakVolumeChanges(prefs)
    val value =
      prefs.getString(
        context.getString(R.string.pref_volume_change_feedback_key),
        context.getString(R.string.pref_volume_change_feedback_default),
      )
    val both = value == context.getString(R.string.value_volume_change_feedback_tone_and_spoken)
    playsTone = both || value == context.getString(R.string.value_volume_change_feedback_tone)
    speaksVolume = both || value == context.getString(R.string.value_volume_change_feedback_spoken)
    toneDuringMedia =
      !prefs.getBoolean(
        context.getString(R.string.pref_volume_change_tone_during_media_key),
        context.resources.getBoolean(R.bool.pref_volume_change_tone_during_media_default),
      )
    speechDuringMedia =
      !prefs.getBoolean(
        context.getString(R.string.pref_volume_change_speech_during_media_key),
        context.resources.getBoolean(R.bool.pref_volume_change_speech_during_media_default),
      )
    if (!playsTone) {
      release()
    }
  }

  /** Turns the feedback off for users who had the old Speak volume changes off. Runs once. */
  private fun migrateSpeakVolumeChanges(prefs: SharedPreferences) {
    if (!prefs.contains(OLD_PREF_SPEAK_VOLUME_CHANGES)) {
      return
    }
    val key = context.getString(R.string.pref_volume_change_feedback_key)
    val editor = prefs.edit().remove(OLD_PREF_SPEAK_VOLUME_CHANGES)
    if (!prefs.getBoolean(OLD_PREF_SPEAK_VOLUME_CHANGES, true) && !prefs.contains(key)) {
      editor.putString(key, context.getString(R.string.value_volume_change_feedback_off))
    }
    editor.apply()
  }

  /**
   * Sets the theme's replacement for the tone: a sound file's path, [SoundThemeManifest.NONE] for
   * no sound, or null for SandFly's tone.
   */
  fun setCustomSound(path: String?) {
    if (path != customSound) {
      customSound = path
      release()
    }
  }

  /** Records that SandFly itself is changing a volume, which gets no feedback. */
  fun noteOwnChange() {
    filter.noteOwnChange(SystemClock.uptimeMillis())
  }

  /**
   * Handles a reported change of [stream], and plays the tone if it is a step made by the user and
   * neither a phone call, [phoneCallActive], nor an app call is going on. Returns whether this
   * change is to be announced. Media playback silences the tone or the announcement when the user
   * has chosen so.
   */
  fun onVolumeChanged(stream: Int, value: Int, prevValue: Int, phoneCallActive: Boolean): Boolean {
    if (!playsTone && !speaksVolume) {
      return false
    }
    val kind = filter.classify(stream, value, prevValue, SystemClock.uptimeMillis())
    val media = if (toneDuringMedia && speechDuringMedia) false else isMediaActive()
    if (
      playsTone &&
        (toneDuringMedia || !media) &&
        kind == VolumeChangeFilter.Kind.STEP &&
        !phoneCallActive &&
        !PauseController.isPaused() &&
        !isAppCallActive()
    ) {
      playTone(stream)
    }
    return speaksVolume &&
      (speechDuringMedia || !media) &&
      (kind == VolumeChangeFilter.Kind.STEP || kind == VolumeChangeFilter.Kind.REPEAT)
  }

  /** Releases the tone's track and player, for when the setting is turned off or SandFly stops. */
  fun release() {
    handler.removeCallbacks(releaseTrackTask)
    track?.release()
    track = null
    releasePlayer()
    gate.clear()
  }

  private fun releasePlayer() {
    player?.release()
    player = null
    playerSound = null
  }

  private fun playTone(stream: Int) {
    val custom = customSound
    if (custom == SoundThemeManifest.NONE) {
      return
    }
    if (!gate.tryStart(SystemClock.uptimeMillis(), TONE_MS + TONE_LATENCY_MS)) {
      return
    }
    if (custom != null) {
      playCustomSound(stream, custom)
      return
    }
    // The previous tone has ended by now. Only one track exists at a time.
    handler.removeCallbacks(releaseTrackTask)
    track?.release()
    track = startTrack(stream)
    if (track == null) {
      gate.clear()
      return
    }
    handler.postDelayed(releaseTrackTask, TONE_MS + RELEASE_AFTER_MS)
  }

  /**
   * Plays the theme's sound [path] at the volume of [stream], from its start. A step during the
   * sound cuts it off and starts it again, as control sounds do.
   */
  private fun playCustomSound(stream: Int, path: String) {
    handler.removeCallbacks(releaseTrackTask)
    track?.release()
    track = null
    val usage = volumeToneUsage(stream)
    val started = startPlayer(path, usage)
    if (started == null) {
      gate.clear()
      return
    }
    val length = started.duration.toLong()
    handler.postDelayed(
      releaseTrackTask,
      (if (length > 0) length else UNKNOWN_SOUND_MS) + RELEASE_AFTER_MS,
    )
  }

  /** Starts the player of [path] for [usage] from the start, creating it if needed. */
  private fun startPlayer(path: String, usage: Int): MediaPlayer? {
    val current = player
    if (current != null && playerSound == path && playerUsage == usage) {
      try {
        current.seekTo(0)
        current.start()
        return current
      } catch (e: IllegalStateException) {
        LogUtils.w(TAG, "Cannot restart theme sound: %s", e)
        releasePlayer()
        return null
      }
    }
    releasePlayer()
    val created = MediaPlayer()
    try {
      created.setAudioAttributes(FeedbackController.feedbackAttributes(usage))
      created.setDataSource(path)
      created.prepare()
      created.start()
    } catch (e: Exception) {
      // A file the player can't read, which is gone, or no audio resources left.
      LogUtils.w(TAG, "Cannot play theme sound: %s", e)
      created.release()
      return null
    }
    player = created
    playerSound = path
    playerUsage = usage
    return created
  }

  /**
   * Whether an app plays media. The tone of an earlier step may still be playing with the media
   * usage, and so may speech and sounds when they play at the media volume.
   */
  private fun isMediaActive(): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    val players = audio.activePlaybackConfigurations.map { it.audioAttributes }
    return isMediaPlaying(
      players.map { it.usage },
      players.map { it.contentType },
      ownMediaAudio =
        track != null ||
          player != null ||
          FailoverTextToSpeech.speechUsage(context) == AudioAttributes.USAGE_MEDIA,
    )
  }

  private fun isAppCallActive(): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    return VolumeKeyCommands.isAppCall(
      audio.activePlaybackConfigurations.map { it.audioAttributes.usage },
      FailoverTextToSpeech.speechUsage(context),
      audio.mode,
    )
  }

  /** Starts a new track playing the tone at the volume of [stream]. */
  private fun startTrack(stream: Int): AudioTrack? {
    var created: AudioTrack? = null
    try {
      val samples = tone
      val newTrack =
        AudioTrack.Builder()
          .setAudioAttributes(FeedbackController.feedbackAttributes(volumeToneUsage(stream)))
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
    private const val OLD_PREF_SPEAK_VOLUME_CHANGES = "pref_speak_volume_changes"
    private const val SAMPLE_RATE = 48_000
    private const val TONE_HZ = 1_000
    private const val TONE_MS = 50L
    private const val FADE_MS = 5
    private const val TONE_AMPLITUDE = 0.35

    /** Added to the tone's length while the next tone is dropped, for the start of playback. */
    private const val TONE_LATENCY_MS = 50L

    /** How long after the tone ends its track is released if no further tone replaced it. */
    private const val RELEASE_AFTER_MS = 200L

    /** How long a theme sound whose length the player doesn't know is taken to last. */
    private const val UNKNOWN_SOUND_MS = 2_000L
  }
}
