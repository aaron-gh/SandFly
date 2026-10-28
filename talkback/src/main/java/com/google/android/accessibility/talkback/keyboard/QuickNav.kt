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
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Quick Nav as in VoiceOver on iOS: while it is on, arrow keys, alone or two pressed together,
 * navigate without the Action key. Each command's keys are a key or two keys pressed together,
 * with or without modifiers, stored like a [KeyCombo] code: two keys as prefix key (the lower key
 * code) and key, so the order they are pressed in doesn't matter.
 */
object QuickNav {
  private const val PREF_ON = "pref_quick_nav_on"
  const val PREF_KEYS_PREFIX = "quick_nav_keys_"

  /** Keys of a command with no keys assigned. */
  const val UNASSIGNED = 0L

  /** Returns the keys for [first] alone, or [first] and [second] pressed together. */
  @JvmStatic
  @JvmOverloads
  fun keys(modifiers: Int, first: Int, second: Int = KeyEvent.KEYCODE_UNKNOWN): Long {
    if (second == KeyEvent.KEYCODE_UNKNOWN || second == first) {
      return KeyCombo.computeKeyComboCode(modifiers, first)
    }
    return KeyCombo.computeKeyComboCode(modifiers, minOf(first, second), maxOf(first, second))
  }

  /** Whether Quick Nav is on. */
  @JvmStatic
  fun isOn(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context).getBoolean(PREF_ON, false)

  @JvmStatic
  fun setOn(context: Context, on: Boolean) {
    SharedPreferencesUtils.getSharedPreferences(context).edit { putBoolean(PREF_ON, on) }
  }

  /** The keys assigned to [command], or [UNASSIGNED]. */
  @JvmStatic
  fun keysFor(context: Context, command: QuickNavCommand): Long =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getLong(command.prefKey, command.defaultKeys)

  @JvmStatic
  fun save(context: Context, command: QuickNavCommand, keys: Long) {
    SharedPreferencesUtils.getSharedPreferences(context).edit {
      if (keys == command.defaultKeys) remove(command.prefKey) else putLong(command.prefKey, keys)
    }
  }

  /** The command [keys] are assigned to, or null. */
  @JvmStatic
  fun commandFor(context: Context, keys: Long): QuickNavCommand? =
    if (keys == UNASSIGNED) null
    else QuickNavCommand.entries.firstOrNull { keysFor(context, it) == keys }

  /** Assigns every command its default keys. */
  @JvmStatic
  fun reset(context: Context) {
    SharedPreferencesUtils.getSharedPreferences(context).edit {
      QuickNavCommand.entries.forEach { remove(it.prefKey) }
    }
  }
}

private const val LEFT = KeyEvent.KEYCODE_DPAD_LEFT
private const val RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT
private const val UP = KeyEvent.KEYCODE_DPAD_UP
private const val DOWN = KeyEvent.KEYCODE_DPAD_DOWN

/** Quick Nav commands, with VoiceOver's keys as defaults. */
enum class QuickNavCommand(
  @StringRes val title: Int,
  /** The keyboard shortcut action the command performs; null for Toggle and the page commands. */
  val action: TalkBackPhysicalKeyboardShortcut?,
  val defaultKeys: Long,
) {
  TOGGLE(R.string.quick_nav_toggle, null, QuickNav.keys(0, LEFT, RIGHT)),
  NEXT_ITEM(
    R.string.keycombo_menu_navigate_next,
    TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT,
    QuickNav.keys(0, RIGHT),
  ),
  PREVIOUS_ITEM(
    R.string.keycombo_menu_navigate_previous,
    TalkBackPhysicalKeyboardShortcut.NAVIGATE_PREVIOUS_DEFAULT,
    QuickNav.keys(0, LEFT),
  ),
  READING_CONTROL_DOWN(
    R.string.keycombo_menu_global_adjust_reading_setting_next,
    TalkBackPhysicalKeyboardShortcut.ADJUST_READING_MENU_DOWN,
    QuickNav.keys(0, DOWN),
  ),
  READING_CONTROL_UP(
    R.string.keycombo_menu_global_adjust_reading_settings_previous,
    TalkBackPhysicalKeyboardShortcut.ADJUST_READING_MENU_UP,
    QuickNav.keys(0, UP),
  ),
  CLICK(
    R.string.keycombo_menu_perform_click,
    TalkBackPhysicalKeyboardShortcut.PERFORM_CLICK,
    QuickNav.keys(0, UP, DOWN),
  ),
  NEXT_READING_CONTROL(
    R.string.keycombo_menu_global_scroll_forward_reading_menu,
    TalkBackPhysicalKeyboardShortcut.SELECT_NEXT_READING_MENU,
    QuickNav.keys(0, UP, RIGHT),
  ),
  PREVIOUS_READING_CONTROL(
    R.string.keycombo_menu_global_scroll_backward_reading_menu,
    TalkBackPhysicalKeyboardShortcut.SELECT_PREVIOUS_READING_MENU,
    QuickNav.keys(0, UP, LEFT),
  ),
  FIRST_ITEM(
    R.string.keycombo_menu_navigate_first,
    TalkBackPhysicalKeyboardShortcut.NAVIGATE_FIRST,
    QuickNav.keys(KeyEvent.META_CTRL_ON, UP),
  ),
  LAST_ITEM(
    R.string.keycombo_menu_navigate_last,
    TalkBackPhysicalKeyboardShortcut.NAVIGATE_LAST,
    QuickNav.keys(KeyEvent.META_CTRL_ON, DOWN),
  ),
  PAGE_UP(R.string.title_viewpager_breakout_page_up, null, QuickNav.keys(KeyEvent.META_ALT_ON, UP)),
  PAGE_DOWN(
    R.string.title_viewpager_breakout_page_down,
    null,
    QuickNav.keys(KeyEvent.META_ALT_ON, DOWN),
  ),
  PAGE_LEFT(R.string.quick_nav_page_left, null, QuickNav.keys(KeyEvent.META_ALT_ON, LEFT)),
  PAGE_RIGHT(R.string.quick_nav_page_right, null, QuickNav.keys(KeyEvent.META_ALT_ON, RIGHT));

  /** Preference key of the command's keys, also the key of its preference on screen. */
  val prefKey: String
    get() = QuickNav.PREF_KEYS_PREFIX + name.lowercase()

  companion object {
    @JvmStatic fun fromPrefKey(key: String?): QuickNavCommand? = entries.firstOrNull { it.prefKey == key }
  }
}
