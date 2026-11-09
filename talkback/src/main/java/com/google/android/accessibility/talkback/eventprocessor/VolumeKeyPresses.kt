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

import android.view.KeyEvent

/**
 * Recognizes double presses, triple presses and long presses of the volume keys that have a
 * command, and changes the volume for the presses that make up none of them.
 *
 * A key with no command passes to Android untouched. A key with a command is kept from Android
 * while it is pressed: when the presses end without making up a press with a command, each press
 * changes the volume once, and holding the key changes the volume repeatedly, as Android does. A
 * single press waits for a second press only when the key has a double or triple press command.
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
  }

  /** The commands of one volume key's presses, null when a press has none. */
  data class KeyCommands(val double: String?, val triple: String?, val long: String?) {
    val isEmpty: Boolean
      get() = double == null && triple == null && long == null

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
   * kept from Android. [canUse] says whether presses may be counted now; it is asked only when a key
   * with a command is pressed.
   */
  fun onKeyDown(keyCode: Int, repeated: Boolean, canUse: () -> Boolean): Boolean {
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
      return false
    }
    if (commandsFor(keyCode).isEmpty || !canUse()) {
      return false
    }
    key = keyCode
    this.canUse = canUse
    state = State.PRESSES
    presses = 0
    repeats = 0
    held = true
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

  /** Runs the command of the presses that ended, or changes the volume once for each press. */
  private fun finishPresses() {
    timer.cancel()
    val keyCode = key
    val count = presses
    val commands = commandsFor(keyCode)
    reset()
    val command =
      when (count) {
        2 -> commands.double
        3 -> commands.triple
        else -> null
      }
    if (command != null) {
      callbacks.perform(command)
      return
    }
    repeat(count) { callbacks.adjustVolume(keyCode, /* repeated= */ false) }
  }

  /** How long [key], pressed now, is held before [onHeld]. */
  private fun holdDelay(): Long =
    if (presses == 0 && commandsFor(key).long != null) timeouts.longPress else timeouts.repeatStart

  /** The key is held long enough for a long press, or to change the volume repeatedly. */
  private fun onHeld() {
    if (key == 0 || !held || state != State.PRESSES) {
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
    presses = 0
    held = false
    repeats = 0
    canUse = { false }
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
  }
}
