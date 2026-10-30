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
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.core.content.edit
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Arrow key navigation: while it is on, the arrow keys, alone, two pressed together, or with Control
 * or Alt, run commands without the Meta key. Each of these [ArrowKeys] runs a command, as a gesture
 * does, saved as the command's value; the defaults are VoiceOver's keys on iOS.
 */
object ArrowKeyNav {
  private const val PREF_ON = "pref_arrow_key_nav_on"
  private const val PREF_PREFIX = "arrow_key_nav_slot_"

  /** The value of the command that turns arrow key navigation on or off. */
  const val TOGGLE = "TOGGLE_ARROW_KEY_NAV"

  /** The value of no command. */
  const val UNASSIGNED = "UNASSIGNED"

  /** Returns the key code for [first] alone, or [first] and [second] pressed together. */
  @JvmStatic
  @JvmOverloads
  fun keys(modifiers: Int, first: Int, second: Int = KeyEvent.KEYCODE_UNKNOWN): Long {
    if (second == KeyEvent.KEYCODE_UNKNOWN || second == first) {
      return KeyCombo.computeKeyComboCode(modifiers, first)
    }
    return KeyCombo.computeKeyComboCode(modifiers, minOf(first, second), maxOf(first, second))
  }

  /** Whether arrow key navigation is on. */
  @JvmStatic
  fun isOn(context: Context): Boolean = prefs(context).getBoolean(PREF_ON, false)

  @JvmStatic
  fun setOn(context: Context, on: Boolean) {
    prefs(context).edit { putBoolean(PREF_ON, on) }
  }

  /** The setting key of whether arrow key navigation is on. */
  @JvmStatic fun onKey(): String = PREF_ON

  /** The setting key of the command that [arrowKeys] run. */
  @JvmStatic fun prefKey(arrowKeys: ArrowKeys): String = PREF_PREFIX + arrowKeys.name.lowercase()

  /** The value of the command that [arrowKeys] run. */
  @JvmStatic
  fun commandFor(context: Context, arrowKeys: ArrowKeys): String =
    prefs(context).getString(prefKey(arrowKeys), arrowKeys.defaultCommand) ?: UNASSIGNED

  /** The arrow keys pressed as [keys], or null when they aren't arrow keys this lists. */
  @JvmStatic
  fun arrowKeysFor(keys: Long): ArrowKeys? = ArrowKeys.entries.firstOrNull { it.keys == keys }

  /** Gives every arrow key its default command. */
  @JvmStatic
  fun reset(context: Context) {
    prefs(context).edit { ArrowKeys.entries.forEach { remove(prefKey(it)) } }
  }

  private fun prefs(context: Context): SharedPreferences =
    SharedPreferencesUtils.getSharedPreferences(context)

  /**
   * Moves keys saved for the commands arrow key navigation had of its own to the arrow keys that run
   * the matching commands. Keys other than these arrow keys are dropped.
   */
  @JvmStatic
  fun migrate(prefs: SharedPreferences) {
    val saved = OLD_COMMANDS.keys.filter { prefs.contains(OLD_PREFIX + it) }
    if (saved.isEmpty()) {
      return
    }
    val commandByKeys = HashMap<Long, String>()
    for ((name, old) in OLD_COMMANDS) {
      val keys = prefs.getLong(OLD_PREFIX + name, old.second)
      if (keys != 0L) {
        commandByKeys[keys] = old.first
      }
    }
    prefs.edit {
      for (arrowKeys in ArrowKeys.entries) {
        putString(prefKey(arrowKeys), commandByKeys[arrowKeys.keys] ?: UNASSIGNED)
      }
      OLD_COMMANDS.keys.forEach { remove(OLD_PREFIX + it) }
    }
  }

  private const val OLD_PREFIX = "arrow_key_nav_keys_"
  private const val L = KeyEvent.KEYCODE_DPAD_LEFT
  private const val R = KeyEvent.KEYCODE_DPAD_RIGHT
  private const val U = KeyEvent.KEYCODE_DPAD_UP
  private const val D = KeyEvent.KEYCODE_DPAD_DOWN
  private const val CTRL = KeyEvent.META_CTRL_ON
  private const val ALT = KeyEvent.META_ALT_ON

  /** The commands arrow key navigation had of its own: the command now, and the default keys. */
  private val OLD_COMMANDS: Map<String, Pair<String, Long>> =
    mapOf(
      "toggle" to (TOGGLE to keys(0, L, R)),
      "next_item" to ("NEXT" to keys(0, R)),
      "previous_item" to ("PREVIOUS" to keys(0, L)),
      "reading_control_down" to ("SELECTED_SETTING_NEXT_ACTION" to keys(0, D)),
      "reading_control_up" to ("SELECTED_SETTING_PREVIOUS_ACTION" to keys(0, U)),
      "click" to ("PERFORM_CLICK_ACTION" to keys(0, U, D)),
      "next_reading_control" to ("SELECT_NEXT_SETTING" to keys(0, U, R)),
      "previous_reading_control" to ("SELECT_PREVIOUS_SETTING" to keys(0, U, L)),
      "first_item" to ("FIRST_IN_SCREEN" to keys(CTRL, U)),
      "last_item" to ("LAST_IN_SCREEN" to keys(CTRL, D)),
      "page_up" to ("SCROLL_BACK" to keys(ALT, U)),
      "page_down" to ("SCROLL_FORWARD" to keys(ALT, D)),
      "page_left" to ("SCROLL_RIGHT" to keys(ALT, L)),
      "page_right" to ("SCROLL_LEFT" to keys(ALT, R)),
    )
}

/** The arrow keys that arrow key navigation listens for, each with its default command. */
enum class ArrowKeys(val keys: Long, val defaultCommand: String) {
  RIGHT(ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_RIGHT), "NEXT"),
  LEFT(ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_LEFT), "PREVIOUS"),
  DOWN(ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_DOWN), "SELECTED_SETTING_NEXT_ACTION"),
  UP(ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_UP), "SELECTED_SETTING_PREVIOUS_ACTION"),
  LEFT_RIGHT(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT),
    ArrowKeyNav.TOGGLE,
  ),
  UP_DOWN(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN),
    "PERFORM_CLICK_ACTION",
  ),
  UP_RIGHT(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT),
    "SELECT_NEXT_SETTING",
  ),
  UP_LEFT(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT),
    "SELECT_PREVIOUS_SETTING",
  ),
  DOWN_RIGHT(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT),
    ArrowKeyNav.UNASSIGNED,
  ),
  DOWN_LEFT(
    ArrowKeyNav.keys(0, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT),
    ArrowKeyNav.UNASSIGNED,
  ),
  CTRL_UP(ArrowKeyNav.keys(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_UP), "FIRST_IN_SCREEN"),
  CTRL_DOWN(ArrowKeyNav.keys(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_DOWN), "LAST_IN_SCREEN"),
  CTRL_LEFT(
    ArrowKeyNav.keys(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_LEFT),
    ArrowKeyNav.UNASSIGNED,
  ),
  CTRL_RIGHT(
    ArrowKeyNav.keys(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_RIGHT),
    ArrowKeyNav.UNASSIGNED,
  ),
  ALT_UP(ArrowKeyNav.keys(KeyEvent.META_ALT_ON, KeyEvent.KEYCODE_DPAD_UP), "SCROLL_BACK"),
  ALT_DOWN(ArrowKeyNav.keys(KeyEvent.META_ALT_ON, KeyEvent.KEYCODE_DPAD_DOWN), "SCROLL_FORWARD"),
  ALT_LEFT(ArrowKeyNav.keys(KeyEvent.META_ALT_ON, KeyEvent.KEYCODE_DPAD_LEFT), "SCROLL_RIGHT"),
  ALT_RIGHT(ArrowKeyNav.keys(KeyEvent.META_ALT_ON, KeyEvent.KEYCODE_DPAD_RIGHT), "SCROLL_LEFT");

  /** Whether these are two keys pressed together. */
  val isTwoKeys: Boolean
    get() = KeyCombo.extractPrefixKeyCode(keys) != KeyEvent.KEYCODE_UNKNOWN
}
