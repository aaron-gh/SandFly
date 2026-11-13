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

package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * Turns key events into the commands [ArrowKeys] run. A key that is also part of two keys pressed together
 * acts when it is released, unless the second key was pressed meanwhile; other keys act when
 * pressed. Toggle works while arrow key navigation is off too, and in a text field being edited
 * only commands of two keys together and Toggle work, so that single arrow keys move the cursor.
 * There, Toggle runs only once its two keys have been held together for [TOGGLE_HOLD_MILLIS], so
 * that rolling from one arrow key to the other moves the cursor.
 */
class ArrowKeyNavKeyHandler(private val context: Context, private val callbacks: Callbacks) {

  interface Callbacks {
    /** Runs the command saved as [command], including turning arrow key navigation off. */
    fun perform(command: String)

    /** Whether the focused input is a text field being edited. */
    fun isEditingText(): Boolean
  }

  /** Keys other than modifiers that are down, in the order they went down, with their down time. */
  private val heldKeys = LinkedHashMap<Int, Long>()

  /** Keys whose down event arrow key navigation consumed, so their up event is consumed too. */
  private val consumedKeys = HashSet<Int>()

  /** A key whose command waits for its release, in case a second key is pressed with it. */
  private var pendingKey = KeyEvent.KEYCODE_UNKNOWN
  private var pendingCommand: String? = null

  private val handler = Handler(Looper.getMainLooper())

  /** The two keys whose Toggle waits until they have been held together, in a text field. */
  private var heldToggleKeys: Pair<Int, Int>? = null
  private val heldToggle = Runnable { performHeldToggle() }

  /** Forgets every key, such as when key events may have been lost. */
  fun reset() {
    heldKeys.clear()
    consumedKeys.clear()
    clearPending()
    cancelHeldToggle()
  }

  /**
   * Handles a key event that nothing before arrow key navigation consumed. Returns true to consume
   * it.
   *
   * @param canAct whether commands may run; keys are still tracked when false.
   */
  fun onKeyEvent(event: KeyEvent, canAct: Boolean): Boolean {
    val keyCode = event.keyCode
    if (keyCode == KeyEvent.KEYCODE_UNKNOWN || KeyEvent.isModifierKey(keyCode)) {
      return false
    }
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> onKeyDown(event, canAct)
      KeyEvent.ACTION_UP -> onKeyUp(keyCode)
      else -> keyCode in consumedKeys
    }
  }

  /** Notes a key event that something before arrow key navigation consumed. */
  fun onKeyEventConsumedElsewhere(event: KeyEvent) {
    if (event.action == KeyEvent.ACTION_UP) {
      cancelHeldToggle()
      heldKeys.remove(event.keyCode)
      consumedKeys.remove(event.keyCode)
      if (pendingKey == event.keyCode) {
        clearPending()
      }
    }
  }

  private fun onKeyDown(event: KeyEvent, canAct: Boolean): Boolean {
    val keyCode = event.keyCode
    if (event.repeatCount > 0) {
      return keyCode in consumedKeys
    }
    cancelHeldToggle()
    // A key whose up event was lost would otherwise pair with every later key.
    heldKeys.entries.removeIf { event.downTime - it.value > STALE_KEY_MILLIS }
    val otherKey = heldKeys.keys.lastOrNull { it != keyCode }
    heldKeys[keyCode] = event.downTime
    if (!canAct) {
      return false
    }

    // Keymaps whose arrow keys move SandFly focus, such as NVDA's, don't have it.
    if (!ArrowKeyNav.isAvailable(context)) {
      return false
    }
    val on = ArrowKeyNav.isOn(context)
    val modifiers = event.modifiers and KeyCombo.KEY_EVENT_MODIFIER_MASK
    if (otherKey != null) {
      val command = commandFor(ArrowKeyNav.keys(modifiers, otherKey, keyCode))
      if (command == ArrowKeyNav.TOGGLE && callbacks.isEditingText()) {
        // Both key down events go to the field, so their up events must too.
        heldToggleKeys = otherKey to keyCode
        handler.postDelayed(heldToggle, TOGGLE_HOLD_MILLIS)
        return false
      }
      if (command != null && (on || command == ArrowKeyNav.TOGGLE)) {
        // The first key's own command, if it waits, doesn't run.
        clearPending()
        consumedKeys += keyCode
        callbacks.perform(command)
        return true
      }
    }

    val command = commandFor(ArrowKeyNav.keys(modifiers, keyCode)) ?: return false
    if (command != ArrowKeyNav.TOGGLE && (!on || callbacks.isEditingText())) {
      return false
    }
    consumedKeys += keyCode
    if (on && startsTwoKeyCommand(modifiers, keyCode)) {
      pendingKey = keyCode
      pendingCommand = command
    } else {
      callbacks.perform(command)
    }
    return true
  }

  /** Runs Toggle if its two keys are still held together in a text field being edited. */
  private fun performHeldToggle() {
    val keys = heldToggleKeys ?: return
    heldToggleKeys = null
    if (
      keys.first in heldKeys &&
        keys.second in heldKeys &&
        callbacks.isEditingText() &&
        ArrowKeyNav.isAvailable(context)
    ) {
      clearPending()
      callbacks.perform(ArrowKeyNav.TOGGLE)
    }
  }

  private fun cancelHeldToggle() {
    if (heldToggleKeys != null) {
      heldToggleKeys = null
      handler.removeCallbacks(heldToggle)
    }
  }

  /** The command that the arrow keys pressed as [keys] run, or null. */
  private fun commandFor(keys: Long): String? {
    val arrowKeys = ArrowKeyNav.arrowKeysFor(keys) ?: return null
    return ArrowKeyNav.commandFor(context, arrowKeys).takeIf { it != ArrowKeyNav.UNASSIGNED }
  }

  private fun onKeyUp(keyCode: Int): Boolean {
    cancelHeldToggle()
    heldKeys.remove(keyCode)
    if (!consumedKeys.remove(keyCode)) {
      return false
    }
    if (pendingKey == keyCode) {
      val command = pendingCommand
      clearPending()
      if (command != null) {
        callbacks.perform(command)
      }
    }
    return true
  }

  /** Whether [keyCode] with [modifiers] is one of two keys pressed together that run a command. */
  private fun startsTwoKeyCommand(modifiers: Int, keyCode: Int): Boolean =
    ArrowKeys.entries.any {
      it.isTwoKeys &&
        KeyCombo.extractModifiers(it.keys) == modifiers &&
        (KeyCombo.extractPrefixKeyCode(it.keys) == keyCode ||
          KeyCombo.extractKeyCode(it.keys) == keyCode) &&
        ArrowKeyNav.commandFor(context, it) != ArrowKeyNav.UNASSIGNED
    }

  private fun clearPending() {
    pendingKey = KeyEvent.KEYCODE_UNKNOWN
    pendingCommand = null
  }

  private companion object {
    const val STALE_KEY_MILLIS = 30_000L
    const val TOGGLE_HOLD_MILLIS = 400L
  }
}
