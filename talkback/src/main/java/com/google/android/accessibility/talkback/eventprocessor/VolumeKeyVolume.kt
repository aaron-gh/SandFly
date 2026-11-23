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

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.notificationlist.NotificationListService

/**
 * Changes the volume for a volume key press kept from Android, as Android does for the key.
 *
 * Android gives a volume key press to the media session that plays, such as a cast session, which
 * changes its own volume, or a local session, which changes the volume of its stream. An app's
 * volume change goes to the suggested stream instead whenever that app started audio last, which
 * SandFly does with every utterance, so the session is chosen here. Sessions are seen only with
 * notification access; without it, the change goes to the suggested stream.
 *
 * Not followed: the stream that the app on screen sets for its volume keys, which apps can't read.
 */
class VolumeKeyVolume(private val context: Context) {
  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val sessionManager = context.getSystemService(MediaSessionManager::class.java)

  /** The session of the last press, used again while the key is held. */
  private var session: MediaController? = null

  /**
   * Changes the volume as a press of [keyCode] does, [repeated] while the key is held, starting
   * from [stream]: with [AudioManager.USE_DEFAULT_STREAM_TYPE], the stream of the call, the media
   * playing or the ringer, as Android chooses. Android's volume panel shows only with [showUi].
   */
  fun adjust(keyCode: Int, repeated: Boolean, stream: Int, showUi: Boolean) {
    val audio = audioManager ?: return
    val direction = direction(keyCode)
    val flags = volumeFlags(repeated, showUi)
    if (!repeated) {
      session = if (prefersStream(stream, audio)) null else volumeSession()
    }
    val session = session
    val info = session?.playbackInfo
    if (session == null || info == null) {
      audio.adjustSuggestedStreamVolume(
        direction,
        streamWithoutSession(stream, audio.isMusicActive),
        flags,
      )
      return
    }
    if (info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
      session.adjustVolume(direction, flags)
      return
    }
    audio.adjustSuggestedStreamVolume(
      direction,
      localSessionStream(info.audioAttributes.volumeControlStream, stream, audio.isMusicActive),
      flags,
    )
  }

  /** Whether Android changes [stream] rather than the session's volume: it is playing. */
  private fun prefersStream(stream: Int, audio: AudioManager): Boolean =
    stream == AudioManager.STREAM_MUSIC && audio.isMusicActive

  /** The session Android gives volume keys to, or null when none plays or none can be seen. */
  private fun volumeSession(): MediaController? {
    val manager = sessionManager ?: return null
    val sessions =
      try {
        manager.getActiveSessions(ComponentName(context, NotificationListService::class.java))
      } catch (e: SecurityException) {
        // Notification access is off.
        return null
      } catch (e: RuntimeException) {
        Log.w(TAG, "Media sessions unavailable", e)
        return null
      }
    // In Android's priority order.
    return sessions.firstOrNull {
      val info = it.playbackInfo
      isPlaybackActive(it.playbackState?.state) &&
        handlesVolumeKeys(info.playbackType, info.volumeControl)
    }
  }

  companion object {
    private const val TAG = "VolumeKeyVolume"

    /** AudioManager.FLAG_FROM_KEY, hidden: the change is for a hardware key press. */
    @VisibleForTesting const val FLAG_FROM_KEY = 1 shl 12

    @JvmStatic
    internal fun direction(keyCode: Int): Int =
      if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        AudioManager.ADJUST_RAISE
      } else {
        AudioManager.ADJUST_LOWER
      }

    /** The flags of a volume change for a key press, [repeated] while the key is held. */
    @JvmStatic
    internal fun volumeFlags(repeated: Boolean, showUi: Boolean): Int {
      var flags = AudioManager.FLAG_VIBRATE or FLAG_FROM_KEY
      if (showUi) {
        flags = flags or AudioManager.FLAG_SHOW_UI
      }
      if (!repeated) {
        flags = flags or AudioManager.FLAG_PLAY_SOUND
      }
      return flags
    }

    /**
     * The stream to change when no session takes the keys. Left to Android with the default stream,
     * a change from SandFly goes to the accessibility stream, so the music stream is named when
     * music plays.
     */
    @JvmStatic
    internal fun streamWithoutSession(stream: Int, musicActive: Boolean): Int =
      if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE && musicActive) {
        AudioManager.STREAM_MUSIC
      } else {
        stream
      }

    /** Whether a session in playback [state] takes volume keys, as PlaybackState.isActive(). */
    @JvmStatic
    internal fun isPlaybackActive(state: Int?): Boolean =
      when (state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> true
        else -> false
      }

    /** Whether a session of [playbackType] and [volumeControl] changes volume for the keys. */
    @JvmStatic
    internal fun handlesVolumeKeys(playbackType: Int, volumeControl: Int): Boolean =
      playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL ||
        volumeControl != VolumeProvider.VOLUME_CONTROL_FIXED

    /**
     * The stream to change for a local session that plays on [sessionStream]: that stream, unless
     * it is the music stream and no music plays, when Android uses [stream].
     */
    @JvmStatic
    internal fun localSessionStream(sessionStream: Int, stream: Int, musicActive: Boolean): Int =
      if (sessionStream == AudioManager.STREAM_MUSIC && !musicActive) stream else sessionStream
  }
}
