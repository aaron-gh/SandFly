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
import android.view.KeyEvent

/**
 * Turns key events into [QuickNavCommand]s. A key that is also part of two keys pressed together
 * acts when it is released, unless the second key was pressed meanwhile; other keys act when
 * pressed. Toggle works while Quick Nav is off too, and in a text field being edited only commands
 * of two keys together and Toggle work, so that single arrow keys move the cursor.
 */
class QuickNavKeyHandler(private val context: Context, private val callbacks: Callbacks) {

  interface Callbacks {
    /** Performs [command], including Toggle. */
    fun perform(command: QuickNavCommand)

    /** Whether the focused input is a text field being edited. */
    fun isEditingText(): Boolean
  }

  /** Keys other than modifiers that are down, in the order they went down, with their down time. */
  private val heldKeys = LinkedHashMap<Int, Long>()

  /** Keys whose down event Quick Nav consumed, so their up event is consumed too. */
  private val consumedKeys = HashSet<Int>()

  /** A key whose command waits for its release, in case a second key is pressed with it. */
  private var pendingKey = KeyEvent.KEYCODE_UNKNOWN
  private var pendingCommand: QuickNavCommand? = null

  /** Forgets every key, such as when key events may have been lost. */
  fun reset() {
    heldKeys.clear()
    consumedKeys.clear()
    clearPending()
  }

  /**
   * Handles a key event that nothing before Quick Nav consumed. Returns true to consume it.
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

  /** Notes a key event that something before Quick Nav consumed. */
  fun onKeyEventConsumedElsewhere(event: KeyEvent) {
    if (event.action == KeyEvent.ACTION_UP) {
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
    // A key whose up event was lost would otherwise pair with every later key.
    heldKeys.entries.removeIf { event.downTime - it.value > STALE_KEY_MILLIS }
    val otherKey = heldKeys.keys.lastOrNull { it != keyCode }
    heldKeys[keyCode] = event.downTime
    if (!canAct) {
      return false
    }

    val on = QuickNav.isOn(context)
    val modifiers = event.modifiers and KeyCombo.KEY_EVENT_MODIFIER_MASK
    if (otherKey != null) {
      val command = QuickNav.commandFor(context, QuickNav.keys(modifiers, otherKey, keyCode))
      if (command != null && (on || command == QuickNavCommand.TOGGLE)) {
        // The first key's own command, if it waits, doesn't run.
        clearPending()
        consumedKeys += keyCode
        callbacks.perform(command)
        return true
      }
    }

    val command = QuickNav.commandFor(context, QuickNav.keys(modifiers, keyCode)) ?: return false
    if (command != QuickNavCommand.TOGGLE && (!on || callbacks.isEditingText())) {
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

  private fun onKeyUp(keyCode: Int): Boolean {
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

  /** Whether [keyCode] with [modifiers] is the first of two keys of an assigned command. */
  private fun startsTwoKeyCommand(modifiers: Int, keyCode: Int): Boolean =
    QuickNavCommand.entries.any {
      val keys = QuickNav.keysFor(context, it)
      val prefix = KeyCombo.extractPrefixKeyCode(keys)
      keys != QuickNav.UNASSIGNED &&
        prefix != KeyEvent.KEYCODE_UNKNOWN &&
        KeyCombo.extractModifiers(keys) == modifiers &&
        (prefix == keyCode || KeyCombo.extractKeyCode(keys) == keyCode)
    }

  private fun clearPending() {
    pendingKey = KeyEvent.KEYCODE_UNKNOWN
    pendingCommand = null
  }

  private companion object {
    const val STALE_KEY_MILLIS = 30_000L
  }
}
