package com.google.android.accessibility.talkback.gesture

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Moves the playing media back or forward.
 *
 * The playing app's media session is used when SandFly may see it, which it may while its
 * notification access is on: the position moves [STEP_MS] if the app can seek, or the app's own
 * rewind or fast forward runs. Otherwise the rewind or fast forward media key is sent.
 */
object MediaSeek {

  private const val TAG = "MediaSeek"

  /** How far a step moves the position. */
  const val STEP_MS = 10_000L

  /** Moves the playing media [STEP_MS] forward, or back if not [forward]. */
  @JvmStatic
  fun seek(context: Context, forward: Boolean): Boolean {
    val controller = activeController(context)
    if (controller != null && seek(controller, forward)) {
      return true
    }
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
    val keyCode =
      if (forward) KeyEvent.KEYCODE_MEDIA_FAST_FORWARD else KeyEvent.KEYCODE_MEDIA_REWIND
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    return true
  }

  private fun seek(controller: MediaController, forward: Boolean): Boolean {
    val state = controller.playbackState ?: return false
    val actions = state.actions
    if (actions and PlaybackState.ACTION_SEEK_TO != 0L && state.position >= 0) {
      val duration =
        controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 }
      val target =
        newPosition(
          currentPosition(state, SystemClock.elapsedRealtime()),
          if (forward) STEP_MS else -STEP_MS,
          duration,
        )
      controller.transportControls.seekTo(target)
      return true
    }
    if (forward && actions and PlaybackState.ACTION_FAST_FORWARD != 0L) {
      controller.transportControls.fastForward()
      return true
    }
    if (!forward && actions and PlaybackState.ACTION_REWIND != 0L) {
      controller.transportControls.rewind()
      return true
    }
    return false
  }

  /** The position at [now], in [SystemClock.elapsedRealtime] terms, of media in [state]. */
  internal fun currentPosition(state: PlaybackState, now: Long): Long {
    if (state.state != PlaybackState.STATE_PLAYING || state.lastPositionUpdateTime <= 0) {
      return state.position
    }
    val elapsed = (now - state.lastPositionUpdateTime).coerceAtLeast(0)
    return state.position + (elapsed * state.playbackSpeed).toLong()
  }

  /** [position] moved by [step], kept within the media, of [duration] if known. */
  internal fun newPosition(position: Long, step: Long, duration: Long?): Long {
    val moved = (position + step).coerceAtLeast(0)
    return if (duration == null) moved else moved.coerceAtMost(duration)
  }

  /** The session of the app playing media, or the most recent one, if SandFly may see them. */
  private fun activeController(context: Context): MediaController? {
    val manager = context.getSystemService(MediaSessionManager::class.java) ?: return null
    val sessions =
      try {
        manager.getActiveSessions(ComponentName(context, NotificationListService::class.java))
      } catch (e: SecurityException) {
        // Notification access is off.
        LogUtils.d(TAG, "No access to media sessions")
        return null
      }
    return sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
      ?: sessions.firstOrNull()
  }
}
