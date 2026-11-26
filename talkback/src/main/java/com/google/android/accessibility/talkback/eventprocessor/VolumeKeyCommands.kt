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
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.report.VolumeKeyLog
import com.google.android.accessibility.talkback.shizuku.ScreenOffVolumeKeys
import com.google.android.accessibility.utils.monitor.ScreenMonitor
import com.google.android.accessibility.utils.output.FailoverTextToSpeech

/**
 * Volume keys > Volume up/down press, double press, triple press and long press: runs their
 * commands, and changes the volume itself for presses that are kept from Android but run no
 * command. The single press commands apply only with SandFly handles every volume key press.
 */
class VolumeKeyCommands(
  private val context: Context,
  perform: (String) -> Unit,
  /** Runs when a kept press changes the volume. */
  private val onVolumeAdjusted: () -> Unit,
  /** The stream to suggest to Android for a kept press. */
  private val suggestedStream: () -> Int,
  /** What the kept presses that run no command do instead of changing the volume, if anything. */
  private val volumeChangeReplacement: VolumeChangeReplacement? = null,
) {
  /**
   * Something that the presses of a volume key with a command do in place of changing the volume
   * when they run no command, such as moving the cursor in a text field.
   */
  interface VolumeChangeReplacement {
    /** Whether the presses of [keyCode] being counted do something else instead. */
    fun replaces(keyCode: Int): Boolean

    /** Does it once for a press of [keyCode], or a repeat while the key is held. */
    fun replace(keyCode: Int)
  }

  private val audioManager = context.getSystemService(AudioManager::class.java)
  private val volume = VolumeKeyVolume(context)
  private val powerManager = context.getSystemService(PowerManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val screenOffKeys = ScreenOffVolumeKeys(context, perform)

  /** Volume keys > SandFly handles every volume key press. */
  private var takesAllPresses = false

  /** Volume keys > Hide volume panel. */
  private var hidesVolumePanel = false

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
        override fun perform(command: String) {
          VolumeKeyLog.note("Volume key press command: $command")
          perform(command)
        }

        override fun adjustVolume(keyCode: Int, repeated: Boolean) {
          val replacement = volumeChangeReplacement
          if (replacement != null && replacement.replaces(keyCode)) {
            replacement.replace(keyCode)
            return
          }
          volume.adjust(keyCode, repeated, suggestedStream(), showUi = !hidesVolumePanel)
          onVolumeAdjusted()
        }

        override fun passed(keyCode: Int, reason: String) = logOff(keyCode, reason)
      },
    )

  /** Reads the commands of the presses, and what the other presses do, from [prefs]. */
  fun reload(prefs: SharedPreferences) {
    takesAllPresses =
      prefs.getBoolean(
        context.getString(R.string.pref_volume_keys_take_all_key),
        context.resources.getBoolean(R.bool.pref_volume_keys_take_all_default),
      )
    hidesVolumePanel =
      prefs.getBoolean(
        context.getString(R.string.pref_volume_keys_hide_panel_key),
        context.resources.getBoolean(R.bool.pref_volume_keys_hide_panel_default),
      )
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    fun command(keyRes: Int): String? =
      prefs.getString(context.getString(keyRes), unassigned)?.takeIf {
        it.isNotEmpty() && it != unassigned
      }
    val systemLongPress = ViewConfiguration.getLongPressTimeout().toLong()
    val up = VolumeKeyPresses.KeyCommands(
      double = command(R.string.pref_volume_up_double_press_key),
      triple = command(R.string.pref_volume_up_triple_press_key),
      long = command(R.string.pref_volume_up_long_press_key),
      single = if (takesAllPresses) command(R.string.pref_volume_up_press_key) else null,
    )
    val down = VolumeKeyPresses.KeyCommands(
      double = command(R.string.pref_volume_down_double_press_key),
      triple = command(R.string.pref_volume_down_triple_press_key),
      long = command(R.string.pref_volume_down_long_press_key),
      single = if (takesAllPresses) command(R.string.pref_volume_down_press_key) else null,
    )
    val timeouts = VolumeKeyPresses.Timeouts(
      multiPress = ViewConfiguration.getDoubleTapTimeout().toLong(),
      longPress =
        longPressDuration(
          prefs.getString(context.getString(R.string.pref_volume_key_long_press_duration_key), null),
          systemLongPress,
        ),
      repeat = ViewConfiguration.getKeyRepeatDelay().toLong(),
      repeatStart = systemLongPress,
    )
    presses.setCommands(up, down, timeouts)
    screenOffKeys.configure(
      prefs.getBoolean(context.getString(R.string.pref_volume_keys_screen_off_key), false),
      up,
      down,
      timeouts,
      FailoverTextToSpeech.speechUsage(context),
    )
  }

  fun setScreenOffAllowed(allowed: Boolean) = screenOffKeys.setActive(allowed)

  fun refreshScreenOffKeys() = screenOffKeys.refresh()

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
   * Handles a volume key event and returns whether it is kept from Android. [offReason] says why
   * presses may not be counted now, or gives null when they may, and is asked only when a volume
   * key with a command is pressed, or any volume key while one of the switches keeps every press.
   */
  fun onKeyEvent(event: KeyEvent, offReason: () -> String?): Boolean {
    if (
      (!presses.isUsed && !takesAllPresses && !hidesVolumePanel) ||
        !VolumeKeyPresses.isVolumeKey(event.keyCode)
    ) {
      return false
    }
    // Asked again when the key is held, after the event may have been recycled.
    val keyCode = event.keyCode
    return when (event.action) {
      KeyEvent.ACTION_DOWN ->
        presses.onKeyDown(
          keyCode,
          event.repeatCount > 0,
          otherPresses(
            takesAllPresses,
            hidesVolumePanel,
            powerManager?.isInteractive == true,
            volumeChangeReplacement?.replaces(keyCode) == true,
          ),
        ) {
          val reason = offReason() ?: if (isInCall()) "app call audio playing" else null
          if (reason != null) {
            logOff(keyCode, reason)
          }
          reason == null
        }
      KeyEvent.ACTION_UP -> presses.onKeyUp(keyCode)
      else -> false
    }
  }

  /**
   * Logs at info level, which problem reports include, why a press of [keyCode] runs no command,
   * so that a report shows why the presses changed the volume instead.
   */
  private fun logOff(keyCode: Int, reason: String) {
    val text = "Volume key presses off for ${KeyEvent.keyCodeToString(keyCode)}: $reason"
    Log.i(TAG, text)
    VolumeKeyLog.note(text)
  }

  /**
   * The state of the presses, for problem reports: whether any press has a command, the presses
   * being counted and the volume keys down.
   */
  fun describeState(): String =
    (if (presses.isUsed) "" else "no press commands; ") + presses.describe()

  /** Whether any press of [keyCode] has a command. */
  fun hasAnyCommand(keyCode: Int): Boolean = presses.hasAnyCommand(keyCode)

  /** Whether a single, double, triple or long press of [keyCode] has [command]. */
  fun hasCommand(keyCode: Int, command: String): Boolean = presses.hasCommand(keyCode, command)

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
    return VolumeKeyPresses.isAppCall(
      audioManager.activePlaybackConfigurations.map { it.audioAttributes.usage },
      FailoverTextToSpeech.speechUsage(context),
      audioManager.mode,
    )
  }

  companion object {
    private const val TAG = "VolumeKeyCommands"

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

    /**
     * What the presses with no command do: with the screen on, Volume keys > SandFly handles every
     * volume key press, [takesAll], keeps them and changes no volume, and Hide volume panel,
     * [hidesPanel], keeps them and changes the volume without Android's volume panel. When they are
     * [replaced], such as by cursor movement in a text field, they do that instead, whatever the
     * switches.
     */
    @JvmStatic
    internal fun otherPresses(
      takesAll: Boolean,
      hidesPanel: Boolean,
      interactive: Boolean,
      replaced: Boolean = false,
    ): VolumeKeyPresses.OtherPresses =
      when {
        !interactive -> VolumeKeyPresses.OtherPresses.ANDROID
        replaced -> VolumeKeyPresses.OtherPresses.VOLUME
        takesAll -> VolumeKeyPresses.OtherPresses.NOTHING
        hidesPanel -> VolumeKeyPresses.OtherPresses.VOLUME
        else -> VolumeKeyPresses.OtherPresses.ANDROID
      }
  }
}
