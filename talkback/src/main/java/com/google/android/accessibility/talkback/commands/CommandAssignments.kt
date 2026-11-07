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

package com.google.android.accessibility.talkback.commands

import com.google.android.accessibility.talkback.keyboard.KeyComboModel

/** Finds whether the saved settings assign a command to anything that runs it. */
object CommandAssignments {

  /**
   * Whether [saved], the preferences as [android.content.SharedPreferences.getAll] gives them,
   * assign the command saved as [value]: a gesture, arrow key or menu item saved as the value, the
   * value inside a list or a JSON setting such as app gestures and gesture profiles, or a keyboard
   * shortcut saved under the value. Commands assigned by default are not in [saved].
   */
  @JvmStatic
  fun isAssigned(saved: Map<String, *>, value: String): Boolean {
    if (value.isEmpty()) {
      return false
    }
    val shortcutKey = Commands.shortcutKey(value)
    return saved.any { (key, setting) ->
      when (setting) {
        is String -> containsValue(setting, value)
        is Set<*> -> setting.any { it is String && containsValue(it, value) }
        is Long ->
          setting != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong() &&
            containsValue(key, shortcutKey)
        else -> false
      }
    }
  }

  /** Whether [text] holds [value] whole, not as part of a longer value. */
  private fun containsValue(text: String, value: String): Boolean {
    var start = text.indexOf(value)
    while (start >= 0) {
      val end = start + value.length
      if (
        (start == 0 || !isValueChar(text[start - 1])) &&
          (end == text.length || !isValueChar(text[end]))
      ) {
        return true
      }
      start = text.indexOf(value, start + 1)
    }
    return false
  }

  private fun isValueChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'
}
