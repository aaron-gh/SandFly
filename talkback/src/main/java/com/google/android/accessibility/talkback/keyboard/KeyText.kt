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

import android.view.KeyEvent
import java.util.Locale

/**
 * Keyboard shortcuts as text in files, the same in every language: modifiers and the key joined
 * with "+", such as "SandFly+Shift+Right" or "Ctrl+Alt+H". "SandFly" is the keymap's SandFly key. A
 * sequence has its first key, a comma and its second key, such as "SandFly+O, H". The SandFly key
 * pressed twice is "SandFly twice".
 */
object KeyText {
  const val SANDFLY = "SandFly"
  private const val TWICE = "SandFly twice"
  private const val MAX_LENGTH = 100

  private val MODIFIERS =
    listOf(
      "Shift" to KeyEvent.META_SHIFT_ON,
      "Ctrl" to KeyEvent.META_CTRL_ON,
      "Alt" to KeyEvent.META_ALT_ON,
      "Meta" to KeyEvent.META_META_ON,
    )

  private val MODIFIER_NAMES =
    mapOf(
      "shift" to KeyEvent.META_SHIFT_ON,
      "ctrl" to KeyEvent.META_CTRL_ON,
      "control" to KeyEvent.META_CTRL_ON,
      "alt" to KeyEvent.META_ALT_ON,
      "meta" to KeyEvent.META_META_ON,
      "search" to KeyEvent.META_META_ON,
    )

  /** Names written for keys whose Android names are long or unclear. */
  private val NAMES =
    linkedMapOf(
      KeyEvent.KEYCODE_DPAD_LEFT to "Left",
      KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
      KeyEvent.KEYCODE_DPAD_UP to "Up",
      KeyEvent.KEYCODE_DPAD_DOWN to "Down",
      KeyEvent.KEYCODE_DPAD_CENTER to "Center",
      KeyEvent.KEYCODE_ENTER to "Enter",
      KeyEvent.KEYCODE_SPACE to "Space",
      KeyEvent.KEYCODE_DEL to "Backspace",
      KeyEvent.KEYCODE_FORWARD_DEL to "Delete",
      KeyEvent.KEYCODE_ESCAPE to "Escape",
      KeyEvent.KEYCODE_TAB to "Tab",
      KeyEvent.KEYCODE_PAGE_UP to "PageUp",
      KeyEvent.KEYCODE_PAGE_DOWN to "PageDown",
      KeyEvent.KEYCODE_MOVE_HOME to "Home",
      KeyEvent.KEYCODE_MOVE_END to "End",
      KeyEvent.KEYCODE_INSERT to "Insert",
      KeyEvent.KEYCODE_MENU to "Menu",
      KeyEvent.KEYCODE_GRAVE to "Grave",
      KeyEvent.KEYCODE_MINUS to "Minus",
      KeyEvent.KEYCODE_EQUALS to "Equals",
      KeyEvent.KEYCODE_LEFT_BRACKET to "LeftBracket",
      KeyEvent.KEYCODE_RIGHT_BRACKET to "RightBracket",
      KeyEvent.KEYCODE_BACKSLASH to "Backslash",
      KeyEvent.KEYCODE_SEMICOLON to "Semicolon",
      KeyEvent.KEYCODE_APOSTROPHE to "Apostrophe",
      KeyEvent.KEYCODE_COMMA to "Comma",
      KeyEvent.KEYCODE_PERIOD to "Period",
      KeyEvent.KEYCODE_SLASH to "Slash",
    )

  /** Letters, digits, function keys and the number pad, named the same in every Android version. */
  private val PLAIN: Map<Int, String> =
    ('A'..'Z').associate { KeyEvent.KEYCODE_A + (it - 'A') to it.toString() } +
      ('0'..'9').associate { KeyEvent.KEYCODE_0 + (it - '0') to it.toString() } +
      (1..12).associate { KeyEvent.KEYCODE_F1 + it - 1 to "F$it" } +
      (0..9).associate { KeyEvent.KEYCODE_NUMPAD_0 + it to "Numpad_$it" }

  private val CODES: Map<String, Int> =
    (NAMES + PLAIN).entries.associate { (code, name) -> name.lowercase(Locale.ROOT) to code } +
      mapOf(
        "esc" to KeyEvent.KEYCODE_ESCAPE,
        "return" to KeyEvent.KEYCODE_ENTER,
        "del" to KeyEvent.KEYCODE_FORWARD_DEL,
        "pgup" to KeyEvent.KEYCODE_PAGE_UP,
        "pgdn" to KeyEvent.KEYCODE_PAGE_DOWN,
        "applications" to KeyEvent.KEYCODE_MENU,
        "dot" to KeyEvent.KEYCODE_PERIOD,
        "backquote" to KeyEvent.KEYCODE_GRAVE,
      )

  /** The names of keys with a name of their own, for the file reference. */
  @JvmStatic fun namedKeys(): Collection<String> = NAMES.values

  /** Returns [keyCombo] as text, or null when it is unassigned. */
  @JvmStatic
  fun of(keyCombo: KeyCombo): String? {
    if (keyCombo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) return null
    if (keyCombo.isSandFlyKeyPressedTwice) return TWICE
    val parts = mutableListOf<String>()
    if (keyCombo.isTriggerModifierUsed) parts += SANDFLY
    val modifiers = keyCombo.modifiers
    for ((name, bit) in MODIFIERS) {
      if (modifiers and bit != 0) parts += name
    }
    val prefix = keyCombo.prefixKeyCode
    return if (prefix != KeyComboModel.NO_PREFIX_KEY_CODE) {
      (parts + keyName(prefix)).joinToString("+") + ", " + keyName(keyCombo.keyCode)
    } else {
      (parts + keyName(keyCombo.keyCode)).joinToString("+")
    }
  }

  /** Returns the key combination written as [text], or null when it isn't one. */
  @JvmStatic
  fun parse(text: String): KeyCombo? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return null
    if (trimmed.equals(TWICE, ignoreCase = true)) return KeyCombo.sandFlyKeyPressedTwice()
    val comma = trimmed.indexOf(',')
    val first = if (comma < 0) trimmed else trimmed.substring(0, comma)
    val second = if (comma < 0) null else trimmed.substring(comma + 1).trim()
    val parts = first.split('+').map { it.trim() }
    if (parts.any { it.isEmpty() }) return null
    var trigger = false
    var modifiers = 0
    for (part in parts.dropLast(1)) {
      if (part.equals(SANDFLY, ignoreCase = true)) {
        trigger = true
        continue
      }
      modifiers = modifiers or (MODIFIER_NAMES[part.lowercase(Locale.ROOT)] ?: return null)
    }
    val key = keyCode(parts.last()) ?: return null
    if (second == null) return KeyCombo(modifiers, key, trigger)
    // A sequence: the first key, then a key on its own.
    val next = keyCode(second) ?: return null
    return KeyCombo(modifiers, key, next, trigger)
  }

  /** The name of the key [keyCode]. */
  private fun keyName(keyCode: Int): String {
    (NAMES[keyCode] ?: PLAIN[keyCode])?.let {
      return it
    }
    val name = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    // Letters and digits as they are; other names in words, such as "F1" or "Numpad_5".
    return if (name.length == 1) name else name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
  }

  private fun keyCode(name: String): Int? {
    val lower = name.trim().lowercase(Locale.ROOT)
    if (lower.isEmpty()) return null
    CODES[lower]?.let {
      return it
    }
    // Android's names, such as "A", "F5" or "NUMPAD_5", without "KEYCODE_".
    val code = KeyEvent.keyCodeFromString("KEYCODE_" + lower.uppercase(Locale.ROOT))
    return code.takeIf { it != KeyEvent.KEYCODE_UNKNOWN && !KeyEvent.isModifierKey(it) }
  }
}
