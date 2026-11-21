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
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.google.android.accessibility.talkback.R

/**
 * Editing mode of the SandFly keyboard: in the text field being edited, single keys move the cursor
 * and select, copy, cut and paste, instead of typing, so that a keyboard without arrow keys or Ctrl
 * edits as one with them does. The keyboard sends the app the arrow, Home and End keys with Shift
 * and Ctrl, so the app moves the cursor as it does for those keys, and SandFly speaks the moves as
 * it does for them.
 *
 * Keys: J and L, or Left and Right, move by character; I and K, or Up and Down, by line; U and O by
 * word; with Alt, J and L go to the line's start and end, I and K to the text's; T and B go to the
 * text's start and end. Shift with them selects, as S does until pressed again. A selects all, C
 * copies, X cuts, V pastes, Z undoes, Shift + Z redoes, R reads the selection or the line. Delete
 * deletes. Enter, Escape or Back ends editing mode.
 */
class EditingMode(private val context: Context) {
  var isOn = false
    private set

  private var selecting = false

  fun turnOn() {
    isOn = true
    selecting = false
    SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_editing_mode_on))
  }

  fun turnOff(announce: Boolean) {
    if (!isOn) return
    isOn = false
    selecting = false
    if (announce) {
      SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_editing_mode_off))
    }
  }

  /**
   * Handles a key pressed in editing mode in the field of [connection]. Returns false for keys left
   * to the app: modifier keys, Delete and keys that aren't on the keyboard's letters.
   */
  fun onKeyDown(keyCode: Int, event: KeyEvent, connection: InputConnection?): Boolean {
    if (KeyEvent.isModifierKey(keyCode)) {
      return false
    }
    if (connection == null) {
      turnOff(announce = false)
      return false
    }
    val alt = event.isAltPressed
    val extend = selecting || event.isShiftPressed
    when (keyCode) {
      KeyEvent.KEYCODE_J,
      KeyEvent.KEYCODE_DPAD_LEFT ->
        if (alt) send(connection, KeyEvent.KEYCODE_MOVE_HOME, extend)
        else send(connection, KeyEvent.KEYCODE_DPAD_LEFT, extend)
      KeyEvent.KEYCODE_L,
      KeyEvent.KEYCODE_DPAD_RIGHT ->
        if (alt) send(connection, KeyEvent.KEYCODE_MOVE_END, extend)
        else send(connection, KeyEvent.KEYCODE_DPAD_RIGHT, extend)
      KeyEvent.KEYCODE_I,
      KeyEvent.KEYCODE_DPAD_UP ->
        if (alt) send(connection, KeyEvent.KEYCODE_MOVE_HOME, extend, ctrl = true)
        else send(connection, KeyEvent.KEYCODE_DPAD_UP, extend)
      KeyEvent.KEYCODE_K,
      KeyEvent.KEYCODE_DPAD_DOWN ->
        if (alt) send(connection, KeyEvent.KEYCODE_MOVE_END, extend, ctrl = true)
        else send(connection, KeyEvent.KEYCODE_DPAD_DOWN, extend)
      KeyEvent.KEYCODE_U -> send(connection, KeyEvent.KEYCODE_DPAD_LEFT, extend, ctrl = true)
      KeyEvent.KEYCODE_O -> send(connection, KeyEvent.KEYCODE_DPAD_RIGHT, extend, ctrl = true)
      KeyEvent.KEYCODE_T -> send(connection, KeyEvent.KEYCODE_MOVE_HOME, extend, ctrl = true)
      KeyEvent.KEYCODE_B -> send(connection, KeyEvent.KEYCODE_MOVE_END, extend, ctrl = true)
      KeyEvent.KEYCODE_MOVE_HOME,
      KeyEvent.KEYCODE_MOVE_END -> send(connection, keyCode, extend, ctrl = event.isCtrlPressed)
      KeyEvent.KEYCODE_S -> {
        selecting = !selecting
        SandFlyKeyboardHost.speak(
          context.getString(
            if (selecting) R.string.sandfly_keyboard_selecting_on
            else R.string.sandfly_keyboard_selecting_off
          )
        )
      }
      KeyEvent.KEYCODE_A -> connection.performContextMenuAction(android.R.id.selectAll)
      KeyEvent.KEYCODE_C -> {
        val selected = selection(connection)
        if (selected == null) {
          SandFlyKeyboardHost.speak(context.getString(R.string.template_no_text_selected))
        } else {
          connection.performContextMenuAction(android.R.id.copy)
          SandFlyKeyboardHost.speak(context.getString(R.string.template_text_copied, selected))
        }
      }
      KeyEvent.KEYCODE_X ->
        if (selection(connection) == null) {
          SandFlyKeyboardHost.speak(context.getString(R.string.template_no_text_selected))
        } else {
          connection.performContextMenuAction(android.R.id.cut)
        }
      KeyEvent.KEYCODE_V -> connection.performContextMenuAction(android.R.id.paste)
      KeyEvent.KEYCODE_Z -> send(connection, KeyEvent.KEYCODE_Z, event.isShiftPressed, ctrl = true)
      KeyEvent.KEYCODE_R -> {
        val text = selection(connection) ?: line(connection)
        if (text.isNotBlank()) SandFlyKeyboardHost.speak(text)
      }
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_NUMPAD_ENTER,
      KeyEvent.KEYCODE_DPAD_CENTER,
      KeyEvent.KEYCODE_ESCAPE,
      KeyEvent.KEYCODE_BACK -> turnOff(announce = true)
      KeyEvent.KEYCODE_DEL,
      KeyEvent.KEYCODE_FORWARD_DEL -> return false
      // Other keys that type do nothing in editing mode; the rest, such as the volume keys, go on.
      else -> return event.isPrintingKey
    }
    return true
  }

  /** Sends the app [keyCode], with Shift where it selects and Ctrl where asked. */
  private fun send(connection: InputConnection, keyCode: Int, shift: Boolean, ctrl: Boolean = false) {
    var meta = 0
    if (shift) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    val time = SystemClock.uptimeMillis()
    for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
      connection.sendKeyEvent(
        KeyEvent(
          time,
          time,
          action,
          keyCode,
          /* repeat= */ 0,
          meta,
          KeyCharacterMap.VIRTUAL_KEYBOARD,
          /* scancode= */ 0,
          KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
        )
      )
    }
  }

  /** The selected text, at most [MAX_READ] characters of it, or null when none is selected. */
  private fun selection(connection: InputConnection): String? =
    connection.getSelectedText(0)?.toString()?.takeIf { it.isNotEmpty() }?.take(MAX_READ)

  /** The line the cursor is on, as far as [MAX_READ] characters each side. */
  private fun line(connection: InputConnection): String {
    val before = connection.getTextBeforeCursor(MAX_READ, 0)?.toString().orEmpty()
    val after = connection.getTextAfterCursor(MAX_READ, 0)?.toString().orEmpty()
    return before.substringAfterLast('\n') + after.substringBefore('\n')
  }

  private companion object {
    const val MAX_READ = 1000
  }
}
