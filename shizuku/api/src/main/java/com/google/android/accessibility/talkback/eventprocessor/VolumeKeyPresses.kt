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

import android.media.AudioAttributes
import android.media.AudioManager
import android.view.KeyEvent

/**
 * Recognizes single presses, double presses, triple presses and long presses of the volume keys
 * that have a command, and changes the volume for the presses that make up none of them.
 *
 * A key with no command passes to Android untouched, unless [OtherPresses] keeps it. A key with a
 * command is kept from Android while it is pressed: when the presses end without making up a press
 * with a command, each press changes the volume once, and holding the key changes the volume
 * repeatedly, as Android does, unless [OtherPresses.NOTHING]. A single press waits for a second
 * press only when the key has a double or triple press command.
 *
 * Only one timer runs at a time, and only between the first press of a key and the end of its
 * presses.
 *
 * [onKeyState] is told of every volume key press and release, including ones that do not reach
 * [onKeyDown] and [onKeyUp], so that a key pressed while the other volume key is held passes to
 * Android.
 */
class VolumeKeyPresses(private val timer: Timer, private val callbacks: Callbacks) {

  /** One timer, which a new [schedule] replaces. */
  interface Timer {
    /** Runs [task] after [delayMs], in place of the task scheduled before. */
    fun schedule(delayMs: Long, task: Runnable)

    /** Drops the task scheduled. */
    fun cancel()
  }

  interface Callbacks {
    /** Runs the command with gesture value [command]. */
    fun perform(command: String)

    /** Changes the volume as a press of [keyCode] would, [repeated] when the key is held. */
    fun adjustVolume(keyCode: Int, repeated: Boolean)

    /** A press of [keyCode], which has a command, passes to Android because of [reason]. */
    fun passed(keyCode: Int, reason: String) {}
  }

  /** What happens to the presses of a volume key that run no command. */
  enum class OtherPresses {
    /** A key with no command passes to Android; other presses change the volume. */
    ANDROID,
    /** Every press is kept from Android; presses with no command change the volume. */
    VOLUME,
    /** Every press is kept from Android; presses with no command do nothing. */
    NOTHING,
  }

  /**
   * The commands of one volume key's presses, null when a press has none. A [single] press command
   * is given only with [OtherPresses.NOTHING], where a single press changes no volume.
   */
  data class KeyCommands(
    val double: String?,
    val triple: String?,
    val long: String?,
    val single: String? = null,
  ) {
    val isEmpty: Boolean
      get() = single == null && double == null && triple == null && long == null

    /** The number of presses after which no further press can change the command. */
    val maxPresses: Int
      get() =
        when {
          triple != null -> 3
          double != null -> 2
          else -> 1
        }

    companion object {
      @JvmField val NONE = KeyCommands(null, null, null)
    }
  }

  /** Times, in milliseconds. */
  data class Timeouts(
    /** Longest gap between the end of a press and the next press of the same key. */
    val multiPress: Long,
    /** How long a key is held to make a long press. */
    val longPress: Long,
    /** Time between volume changes while a key is held. */
    val repeat: Long,
    /** How long a key with no long press command is held to start changing the volume. */
    val repeatStart: Long = longPress,
  )

  private enum class State {
    /** Counting presses. */
    PRESSES,
    /** The long press ran its command; the key's release is kept from Android. */
    LONG_PRESS_DONE,
    /** The key is held and changes the volume repeatedly. */
    REPEATING,
    /** Nothing more happens until the key is released, which is kept from Android. */
    IGNORED,
    /**
     * A key with no command, kept from Android, is pressed: its press changed the volume, if
     * [OtherPresses.VOLUME], and holding it changes the volume repeatedly.
     */
    NO_COMMAND,
  }

  private var upCommands = KeyCommands.NONE
  private var downCommands = KeyCommands.NONE
  private var timeouts = Timeouts(multiPress = 300, longPress = 400, repeat = 50)

  /** The key of the presses being counted, or 0 when there are none. */
  private var key = 0
  private var state = State.PRESSES
  /** Presses of [key] that have ended. */
  private var presses = 0
  /** Whether [key] is pressed now. */
  private var held = false
  private var repeats = 0
  /** Whether presses may be counted now, as given with the press of [key]. */
  private var canUse: () -> Boolean = { false }
  /** What happens to the presses of [key] that run no command, as given with its first press. */
  private var others = OtherPresses.ANDROID

  /** Whether each volume key is pressed now, whether or not its presses have commands. */
  private var volumeUpDown = false
  private var volumeDownDown = false

  /** Sets the commands of the volume keys' presses, and the times that tell presses apart. */
  fun setCommands(up: KeyCommands, down: KeyCommands, timeouts: Timeouts) {
    upCommands = up
    downCommands = down
    this.timeouts = timeouts
    if (up.isEmpty && down.isEmpty) {
      cancel()
    }
  }

  /** Whether any press of a volume key has a command. */
  val isUsed: Boolean
    get() = !upCommands.isEmpty || !downCommands.isEmpty

  /** Whether any press of [keyCode] has a command. */
  fun hasAnyCommand(keyCode: Int): Boolean = !commandsFor(keyCode).isEmpty

  /** Whether a single, double, triple or long press of [keyCode] has [command]. */
  fun hasCommand(keyCode: Int, command: String): Boolean {
    val commands = commandsFor(keyCode)
    return commands.single == command ||
      commands.double == command ||
      commands.triple == command ||
      commands.long == command
  }

  /**
   * Notes that [keyCode] was pressed ([down]) or released. Called for every volume key event, before
   * [onKeyDown] or [onKeyUp] and also when they are not called.
   */
  fun onKeyState(keyCode: Int, down: Boolean) {
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> volumeUpDown = down
      KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownDown = down
    }
  }

  /**
   * Handles a key press, or a [repeated] press while the key is held, and returns whether it is
   * kept from Android. [others] says what happens to presses that run no command. [canUse] says
   * whether presses may be counted now; it is asked only when a key with a command is pressed, or
   * any key when [others] is not [OtherPresses.ANDROID].
   */
  fun onKeyDown(
    keyCode: Int,
    repeated: Boolean,
    others: OtherPresses = OtherPresses.ANDROID,
    canUse: () -> Boolean,
  ): Boolean {
    if (!isVolumeKey(keyCode)) {
      return false
    }
    if (repeated) {
      // Repeats go where the press went.
      return keyCode == key && held
    }
    if (key == keyCode && held) {
      // The release went elsewhere, such as to Pause SandFly.
      cancel()
    }
    if (key != 0) {
      if (keyCode == key) {
        if (!canUse()) {
          finishPresses()
          return false
        }
        held = true
        this.canUse = canUse
        timer.schedule(holdDelay()) { onHeld() }
        return true
      }
      if (held && (state != State.LONG_PRESS_DONE || isDown(key))) {
        // Both volume keys: the accessibility shortcut, or stopping a notification readout.
        timer.cancel()
        state = State.IGNORED
        callbacks.passed(keyCode, "both volume keys held")
        return false
      }
      if (held) {
        // The release of the long press went elsewhere, such as to Pause SandFly.
        cancel()
      } else {
        finishPresses()
      }
    }
    if (isDown(otherKey(keyCode))) {
      // Both volume keys, the first of which passed to Android.
      if (!commandsFor(keyCode).isEmpty || others != OtherPresses.ANDROID) {
        callbacks.passed(keyCode, "other volume key held")
      }
      return false
    }
    val noCommand = commandsFor(keyCode).isEmpty
    if (noCommand && others == OtherPresses.ANDROID || !canUse()) {
      return false
    }
    key = keyCode
    this.canUse = canUse
    this.others = others
    presses = 0
    repeats = 0
    held = true
    if (noCommand) {
      // Changes the volume at once, as Android does, rather than after the presses end.
      state = State.NO_COMMAND
      if (others == OtherPresses.VOLUME) {
        callbacks.adjustVolume(keyCode, /* repeated= */ false)
        timer.schedule(timeouts.repeatStart) { onHeld() }
      }
      return true
    }
    state = State.PRESSES
    timer.schedule(holdDelay()) { onHeld() }
    return true
  }

  /** Handles a key release and returns whether it is kept from Android. */
  fun onKeyUp(keyCode: Int): Boolean {
    if (key == 0 || keyCode != key || !held) {
      return false
    }
    held = false
    timer.cancel()
    if (state != State.PRESSES) {
      reset()
      return true
    }
    presses++
    if (presses >= commandsFor(key).maxPresses) {
      finishPresses()
    } else {
      timer.schedule(timeouts.multiPress) { finishPresses() }
    }
    return true
  }

  /** Forgets the presses, without running a command or changing the volume. */
  fun cancel() {
    timer.cancel()
    reset()
  }

  /** Forgets the presses and which keys are pressed, as releases may not come with the screen off. */
  fun onScreenOff() {
    cancel()
    volumeUpDown = false
    volumeDownDown = false
  }

  /**
   * Runs the command of the presses that ended, or changes the volume once for each press unless
   * [OtherPresses.NOTHING].
   */
  private fun finishPresses() {
    timer.cancel()
    val keyCode = key
    val count = presses
    val commands = commandsFor(keyCode)
    val changesVolume = others != OtherPresses.NOTHING
    reset()
    val command =
      when (count) {
        1 -> commands.single
        2 -> commands.double
        3 -> commands.triple
        else -> null
      }
    if (command != null) {
      callbacks.perform(command)
      return
    }
    if (changesVolume) {
      repeat(count) { callbacks.adjustVolume(keyCode, /* repeated= */ false) }
    }
  }

  /** How long [key], pressed now, is held before [onHeld]. */
  private fun holdDelay(): Long =
    if (presses == 0 && commandsFor(key).long != null) timeouts.longPress else timeouts.repeatStart

  /** The key is held long enough for a long press, or to change the volume repeatedly. */
  private fun onHeld() {
    if (key == 0 || !held || (state != State.PRESSES && state != State.NO_COMMAND)) {
      return
    }
    if (!canUse()) {
      // Such as a touch on the screen, or a display that turned off with the key's release lost.
      state = State.IGNORED
      return
    }
    val long = commandsFor(key).long
    if (presses == 0 && long != null) {
      state = State.LONG_PRESS_DONE
      callbacks.perform(long)
      return
    }
    if (others == OtherPresses.NOTHING) {
      // The release is kept from Android too.
      state = State.IGNORED
      return
    }
    // A held key changes the volume, after the presses before it.
    state = State.REPEATING
    repeat(presses + 1) { callbacks.adjustVolume(key, /* repeated= */ false) }
    presses = 0
    timer.schedule(timeouts.repeat) { onRepeat() }
  }

  private fun onRepeat() {
    if (key == 0 || !held || state != State.REPEATING) {
      return
    }
    if (!canUse()) {
      // Such as a touch on the screen; the release stays kept from Android.
      state = State.IGNORED
      return
    }
    callbacks.adjustVolume(key, /* repeated= */ true)
    repeats++
    if (repeats >= MAX_REPEATS) {
      // The release may never come, such as when the screen turned off meanwhile.
      state = State.IGNORED
      return
    }
    timer.schedule(timeouts.repeat) { onRepeat() }
  }

  private fun reset() {
    key = 0
    state = State.PRESSES
    others = OtherPresses.ANDROID
    presses = 0
    held = false
    repeats = 0
    canUse = { false }
  }

  /**
   * The presses being counted and the volume keys down, for problem reports, such as "presses: down
   * LONG_PRESS_DONE, 0 ended, held; keys down: down".
   */
  fun describe(): String {
    val counting =
      if (key == 0) "none"
      else "${keyName(key)} $state, $presses ended" + (if (held) ", held" else "")
    val down = listOfNotNull("up".takeIf { volumeUpDown }, "down".takeIf { volumeDownDown })
    return "presses: $counting; keys down: ${down.joinToString(" ").ifEmpty { "none" }}"
  }

  private fun keyName(keyCode: Int): String =
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> "up"
      KeyEvent.KEYCODE_VOLUME_DOWN -> "down"
      else -> keyCode.toString()
    }

  private fun isDown(keyCode: Int): Boolean =
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> volumeUpDown
      KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownDown
      else -> false
    }

  private fun otherKey(keyCode: Int): Int =
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      KeyEvent.KEYCODE_VOLUME_DOWN
    } else {
      KeyEvent.KEYCODE_VOLUME_UP
    }

  private fun commandsFor(keyCode: Int): KeyCommands =
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> upCommands
      KeyEvent.KEYCODE_VOLUME_DOWN -> downCommands
      else -> KeyCommands.NONE
    }

  companion object {
    /** Most volume changes of one held key, enough to go from silent to the loudest. */
    const val MAX_REPEATS = 100

    @JvmStatic
    fun isVolumeKey(keyCode: Int): Boolean =
      keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    /**
     * Whether the audio [usages] playing and the audio [mode] make up an app call: audio with the
     * voice communication usage plays. When SandFly's own speech and sounds have that usage, its
     * players can't be told apart from an app's, and the audio mode of an app call counts instead.
     */
    @JvmStatic
    fun isAppCall(usages: List<Int>, ownUsage: Int, mode: Int): Boolean =
      if (ownUsage == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
        mode == AudioManager.MODE_IN_COMMUNICATION
      } else {
        AudioAttributes.USAGE_VOICE_COMMUNICATION in usages
      }
  }
}
