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
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.google.android.accessibility.talkback.R

/**
 * Volume keys > Volume up/down double press, triple press and long press: runs their commands, and
 * changes the volume itself for presses that are kept from Android but run no command.
 */
class VolumeKeyCommands(
  private val context: Context,
  perform: (String) -> Unit,
  /** Runs when a kept press changes the volume. */
  private val onVolumeAdjusted: () -> Unit,
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
          val direction =
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
              AudioManager.ADJUST_RAISE
            } else {
              AudioManager.ADJUST_LOWER
            }
          // As Android does for the key: the media playing, a cast device, or the ringer.
          var flags = AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_VIBRATE
          if (!repeated) {
            flags = flags or AudioManager.FLAG_PLAY_SOUND
          }
          audioManager?.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            flags,
          )
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
        longPress = ViewConfiguration.getLongPressTimeout().toLong(),
        repeat = ViewConfiguration.getKeyRepeatDelay().toLong(),
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

  /** Whether a phone or app call is ringing or going on, which the volume keys are left to. */
  private fun isInCall(): Boolean {
    val mode = audioManager?.mode ?: return false
    return mode != AudioManager.MODE_NORMAL
  }
}
