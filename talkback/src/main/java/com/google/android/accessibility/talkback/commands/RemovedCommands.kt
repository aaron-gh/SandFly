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

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Moves what was saved for removed commands to the commands that replaced them: gestures, keys and
 * other bindings saved as a command's value, the same values inside JSON settings such as app
 * gestures and gesture profiles, and keyboard shortcuts saved under a command's value.
 */
object RemovedCommands {
  /** Removed command values and the values of the commands that replaced them. */
  private val REPLACED =
    mapOf(
      "VOICE_INPUT" to "DICTATE",
      "MEDIA_CONTROL_OR_VOICE_INPUT" to "DICTATE_OR_MEDIA_CONTROL",
    )

  private const val PREF_DONE = "removed_commands_moved_1"

  /**
   * Moves the bindings in [prefs] once, or again with [force], as after settings are imported from a
   * file that can hold removed commands.
   */
  @JvmStatic
  @JvmOverloads
  fun migrate(prefs: SharedPreferences, force: Boolean = false) {
    if (!force && prefs.getBoolean(PREF_DONE, false)) {
      return
    }
    val all = prefs.all
    prefs.edit {
      for ((key, value) in all) {
        val newKey = replaceShortcutKey(key)
        if (newKey != null) {
          remove(key)
          if (!all.containsKey(newKey)) {
            put(this, newKey, value)
          }
          continue
        }
        if (value is String) {
          val replaced = replaceValue(value)
          if (replaced != value) {
            putString(key, replaced)
          }
        }
      }
      putBoolean(PREF_DONE, true)
    }
  }

  /** Returns [value] with removed command values replaced, as the whole value or JSON strings. */
  internal fun replaceValue(value: String): String {
    REPLACED[value]?.let {
      return it
    }
    if (!value.startsWith("{") && !value.startsWith("[")) {
      return value
    }
    var result = value
    for ((old, new) in REPLACED) {
      result = result.replace("\"$old\"", "\"$new\"")
    }
    return result
  }

  /** Returns [key] with a removed command's keyboard shortcut key replaced, or null for others. */
  internal fun replaceShortcutKey(key: String): String? {
    for ((old, new) in REPLACED) {
      val oldKey = Commands.shortcutKey(old)
      var start = key.indexOf(oldKey)
      while (start >= 0) {
        val end = start + oldKey.length
        if (end == key.length || !isValueChar(key[end])) {
          return key.substring(0, start) + Commands.shortcutKey(new) + key.substring(end)
        }
        start = key.indexOf(oldKey, start + 1)
      }
    }
    return null
  }

  private fun isValueChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

  private fun put(editor: SharedPreferences.Editor, key: String, value: Any?) {
    when (value) {
      is String -> editor.putString(key, value)
      is Long -> editor.putLong(key, value)
      is Int -> editor.putInt(key, value)
      is Boolean -> editor.putBoolean(key, value)
      is Float -> editor.putFloat(key, value)
      is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
    }
  }
}
