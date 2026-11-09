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
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.monitor.ScreenMonitor
import com.google.android.accessibility.utils.output.FailoverTextToSpeech

/**
 * Volume keys > Volume up/down double press, triple press and long press: runs their commands, and
 * changes the volume itself for presses that are kept from Android but run no command.
 */
class VolumeKeyCommands(
  private val context: Context,
  perform: (String) -> Unit,
  /** Runs when a kept press changes the volume. */
  private val onVolumeAdjusted: () -> Unit,
  /** The stream to suggest to Android for a kept press. */
  private val suggestedStream: () -> Int,
) {
  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val handler = Handler(Looper.getMainLooper())

  private val presses =
    VolumeKeyPresses(
      object : VolumeKeyPresses.Timer {
        private var task: Runnable? = null

        override fun schedule(delayMs: Long, task: Runnable) {
          cancel()
          this.task = task
          handler.postDelayed(task, delayMs)
        }

        override fun cancel() {
          task?.let { handler.removeCallbacks(it) }
          task = null
        }
      },
      object : VolumeKeyPresses.Callbacks {
        override fun perform(command: String) = perform(command)

        override fun adjustVolume(keyCode: Int, repeated: Boolean) {
          adjustSuggestedVolume(audioManager, keyCode, repeated, suggestedStream())
          onVolumeAdjusted()
        }
      },
    )

  /** Reads the commands of the presses from [prefs]. */
  fun reload(prefs: SharedPreferences) {
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    fun command(keyRes: Int): String? =
      prefs.getString(context.getString(keyRes), unassigned)?.takeIf {
        it.isNotEmpty() && it != unassigned
      }
    val systemLongPress = ViewConfiguration.getLongPressTimeout().toLong()
    presses.setCommands(
      VolumeKeyPresses.KeyCommands(
        double = command(R.string.pref_volume_up_double_press_key),
        triple = command(R.string.pref_volume_up_triple_press_key),
        long = command(R.string.pref_volume_up_long_press_key),
      ),
      VolumeKeyPresses.KeyCommands(
        double = command(R.string.pref_volume_down_double_press_key),
        triple = command(R.string.pref_volume_down_triple_press_key),
        long = command(R.string.pref_volume_down_long_press_key),
      ),
      VolumeKeyPresses.Timeouts(
        multiPress = ViewConfiguration.getDoubleTapTimeout().toLong(),
        longPress =
          longPressDuration(
            prefs.getString(
              context.getString(R.string.pref_volume_key_long_press_duration_key),
              null,
            ),
            systemLongPress,
          ),
        repeat = ViewConfiguration.getKeyRepeatDelay().toLong(),
        repeatStart = systemLongPress,
      ),
    )
  }

  /**
   * Notes which volume keys are pressed. Called for every key event before anything else can
   * consume it, whether or not any press has a command.
   */
  fun onKeyEventSeen(event: KeyEvent) {
    if (!VolumeKeyPresses.isVolumeKey(event.keyCode)) {
      return
    }
    when (event.action) {
      KeyEvent.ACTION_DOWN -> presses.onKeyState(event.keyCode, /* down= */ true)
      KeyEvent.ACTION_UP -> presses.onKeyState(event.keyCode, /* down= */ false)
    }
  }

  /**
   * Handles a volume key event and returns whether it is kept from Android. [canUse] says whether
   * presses may be counted now, and is asked only when a volume key with a command is pressed.
   */
  fun onKeyEvent(event: KeyEvent, canUse: () -> Boolean): Boolean {
    if (!presses.isUsed || !VolumeKeyPresses.isVolumeKey(event.keyCode)) {
      return false
    }
    return when (event.action) {
      KeyEvent.ACTION_DOWN ->
        presses.onKeyDown(event.keyCode, event.repeatCount > 0) { canUse() && !isInCall() }
      KeyEvent.ACTION_UP -> presses.onKeyUp(event.keyCode)
      else -> false
    }
  }

  /** Forgets the presses so far, such as when another feature took the volume keys. */
  fun cancel() {
    presses.cancel()
  }

  /** Forgets the presses and the keys held, whose releases may not come with the screen off. */
  fun onScreenOff() {
    presses.onScreenOff()
  }

  /**
   * Whether an app call is going on, which the volume keys are left to. Phone calls are left to
   * them by the [canUse] of [onKeyEvent]. A call audio mode that stays after a call, as on some
   * phones, or that an app keeps with no call, doesn't count.
   */
  private fun isInCall(): Boolean {
    val audioManager = audioManager ?: return false
    return isAppCall(
      audioManager.activePlaybackConfigurations.map { it.audioAttributes.usage },
      FailoverTextToSpeech.speechUsage(context),
      audioManager.mode,
    )
  }

  companion object {
    /** Shortest and longest stored long press times accepted, in milliseconds. */
    private const val MIN_LONG_PRESS_MS = 100L
    private const val MAX_LONG_PRESS_MS = 2000L

    /**
     * The long press time for the [stored] setting value, or [systemDefault] for the System default
     * value and for a value outside the accepted range.
     */
    @JvmStatic
    internal fun longPressDuration(stored: String?, systemDefault: Long): Long =
      stored?.toLongOrNull()?.takeIf { it in MIN_LONG_PRESS_MS..MAX_LONG_PRESS_MS } ?: systemDefault

    /**
     * Whether presses may be counted while the device is not interactive. Android passes the volume
     * keys to accessibility services in that state only while the lock screen shows on a display
     * that is not off, as with an always-on display.
     */
    @JvmStatic
    fun canCountWhileNotInteractive(context: Context): Boolean {
      if (!ScreenMonitor.isDeviceLocked(context)) {
        return false
      }
      val display =
        context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
      return display != null && display.state != Display.STATE_OFF
    }

    /** Changes the volume as Android does for the key, starting from [stream]. */
    fun adjustSuggestedVolume(
      audioManager: AudioManager?,
      keyCode: Int,
      repeated: Boolean,
      stream: Int,
    ) {
      val direction =
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
          AudioManager.ADJUST_RAISE
        } else {
          AudioManager.ADJUST_LOWER
        }
      var flags = AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_VIBRATE
      if (!repeated) {
        flags = flags or AudioManager.FLAG_PLAY_SOUND
      }
      audioManager?.adjustSuggestedStreamVolume(direction, stream, flags)
    }

    /**
     * Whether the audio [usages] playing and the audio [mode] make up an app call: audio with the
     * voice communication usage plays. When SandFly's own speech and sounds have that usage, its
     * players can't be told apart from an app's, and the audio mode of an app call counts instead.
     */
    @JvmStatic
    internal fun isAppCall(usages: List<Int>, ownUsage: Int, mode: Int): Boolean =
      if (ownUsage == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
        mode == AudioManager.MODE_IN_COMMUNICATION
      } else {
        AudioAttributes.USAGE_VOICE_COMMUNICATION in usages
      }
  }
}
