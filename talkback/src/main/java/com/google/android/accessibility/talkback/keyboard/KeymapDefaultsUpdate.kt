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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Gives saved shortcuts the defaults of this version where the user never changed them. A keymap's
 * defaults are saved when it is first used, so a default that changed stays as it was unless the
 * saved shortcut is forgotten.
 */
object KeymapDefaultsUpdate {
  /** The version of the defaults of the NVDA, JAWS and Orca keymaps. */
  private const val VERSION = 4

  private const val NONE = KeyComboModel.NO_MODIFIER
  private const val SHIFT = KeyEvent.META_SHIFT_ON
  private const val CTRL = KeyEvent.META_CTRL_ON

  private fun old(modifiers: Int, keyCode: Int, sandFly: Boolean) =
    KeyCombo(modifiers, KeyComboModel.NO_PREFIX_KEY_CODE, keyCode, sandFly)

  /**
   * The version 1 defaults of the commands whose defaults changed in version 2, when arrow keys
   * started to move SandFly focus and object navigation and the review cursor came.
   */
  private val VERSION_1: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    mapOf(
      Keymap.NVDA to
        mapOf(
          R.string.keycombo_shortcut_focus_review_cursor to KeyCombo(),
          R.string.keycombo_shortcut_global_adjust_reading_setting_next to old(CTRL, KeyEvent.KEYCODE_DPAD_DOWN, sandFly = true),
          R.string.keycombo_shortcut_global_adjust_reading_settings_previous to old(CTRL, KeyEvent.KEYCODE_DPAD_UP, sandFly = true),
          R.string.keycombo_shortcut_global_describe_image to old(NONE, KeyEvent.KEYCODE_R, sandFly = true),
          R.string.keycombo_shortcut_global_notifications to old(NONE, KeyEvent.KEYCODE_MOVE_END, sandFly = true),
          R.string.keycombo_shortcut_global_scroll_backward_reading_menu to old(CTRL, KeyEvent.KEYCODE_DPAD_LEFT, sandFly = true),
          R.string.keycombo_shortcut_global_scroll_forward_reading_menu to old(CTRL, KeyEvent.KEYCODE_DPAD_RIGHT, sandFly = true),
          R.string.keycombo_shortcut_navigate_first to KeyCombo(),
          R.string.keycombo_shortcut_navigate_last to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_default to old(SHIFT, KeyEvent.KEYCODE_DPAD_RIGHT, sandFly = true),
          R.string.keycombo_shortcut_navigate_next_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_line to old(NONE, KeyEvent.KEYCODE_DPAD_DOWN, sandFly = false),
          R.string.keycombo_shortcut_navigate_previous_default to old(SHIFT, KeyEvent.KEYCODE_DPAD_LEFT, sandFly = true),
          R.string.keycombo_shortcut_navigate_previous_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_previous_line to old(NONE, KeyEvent.KEYCODE_DPAD_UP, sandFly = false),
          R.string.keycombo_shortcut_object_first_child to KeyCombo(),
          R.string.keycombo_shortcut_object_next to KeyCombo(),
          R.string.keycombo_shortcut_object_parent to KeyCombo(),
          R.string.keycombo_shortcut_object_previous to KeyCombo(),
          R.string.keycombo_shortcut_other_read_from_cursor_item to old(NONE, KeyEvent.KEYCODE_DPAD_DOWN, sandFly = true),
          R.string.keycombo_shortcut_recognize_screen_text to KeyCombo(),
          R.string.keycombo_shortcut_review_bottom to KeyCombo(),
          R.string.keycombo_shortcut_review_click to KeyCombo(),
          R.string.keycombo_shortcut_review_current_character to KeyCombo(),
          R.string.keycombo_shortcut_review_current_line to KeyCombo(),
          R.string.keycombo_shortcut_review_current_word to KeyCombo(),
          R.string.keycombo_shortcut_review_line_end to KeyCombo(),
          R.string.keycombo_shortcut_review_line_start to KeyCombo(),
          R.string.keycombo_shortcut_review_long_click to KeyCombo(),
          R.string.keycombo_shortcut_review_move_to_focus to KeyCombo(),
          R.string.keycombo_shortcut_review_next_character to KeyCombo(),
          R.string.keycombo_shortcut_review_next_line to KeyCombo(),
          R.string.keycombo_shortcut_review_next_word to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_character to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_line to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_word to KeyCombo(),
          R.string.keycombo_shortcut_review_top to KeyCombo(),
        ),
      Keymap.JAWS to
        mapOf(
          R.string.keycombo_shortcut_focus_review_cursor to KeyCombo(),
          R.string.keycombo_shortcut_global_refocus_current_node to old(NONE, KeyEvent.KEYCODE_TAB, sandFly = true),
          R.string.keycombo_shortcut_navigate_first to KeyCombo(),
          R.string.keycombo_shortcut_navigate_last to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_default to old(NONE, KeyEvent.KEYCODE_DPAD_RIGHT, sandFly = true),
          R.string.keycombo_shortcut_navigate_next_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_line to old(NONE, KeyEvent.KEYCODE_DPAD_DOWN, sandFly = false),
          R.string.keycombo_shortcut_navigate_next_link to old(NONE, KeyEvent.KEYCODE_TAB, sandFly = false),
          R.string.keycombo_shortcut_navigate_previous_default to old(NONE, KeyEvent.KEYCODE_DPAD_LEFT, sandFly = true),
          R.string.keycombo_shortcut_navigate_previous_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_previous_line to old(NONE, KeyEvent.KEYCODE_DPAD_UP, sandFly = false),
          R.string.keycombo_shortcut_navigate_previous_link to old(SHIFT, KeyEvent.KEYCODE_TAB, sandFly = false),
          R.string.keycombo_shortcut_recognize_screen_text to KeyCombo(),
          R.string.keycombo_shortcut_review_cursor_off to KeyCombo(),
          R.string.keycombo_shortcut_review_cursor_on to KeyCombo(),
          R.string.keycombo_shortcut_review_move_to_focus to KeyCombo(),
        ),
      Keymap.ORCA to
        mapOf(
          R.string.keycombo_shortcut_navigate_first to KeyCombo(),
          R.string.keycombo_shortcut_navigate_last to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_default to old(NONE, KeyEvent.KEYCODE_O, sandFly = true),
          R.string.keycombo_shortcut_navigate_next_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_next_line to old(NONE, KeyEvent.KEYCODE_DPAD_DOWN, sandFly = false),
          R.string.keycombo_shortcut_navigate_next_paragraph to old(NONE, KeyEvent.KEYCODE_P, sandFly = false),
          R.string.keycombo_shortcut_navigate_previous_default to old(NONE, KeyEvent.KEYCODE_U, sandFly = true),
          R.string.keycombo_shortcut_navigate_previous_focusable to KeyCombo(),
          R.string.keycombo_shortcut_navigate_previous_line to old(NONE, KeyEvent.KEYCODE_DPAD_UP, sandFly = false),
          R.string.keycombo_shortcut_navigate_previous_paragraph to old(SHIFT, KeyEvent.KEYCODE_P, sandFly = false),
          R.string.keycombo_shortcut_perform_click to old(NONE, KeyEvent.KEYCODE_7, sandFly = true),
          R.string.keycombo_shortcut_perform_long_click to old(NONE, KeyEvent.KEYCODE_8, sandFly = true),
          R.string.keycombo_shortcut_review_bottom to KeyCombo(),
          R.string.keycombo_shortcut_review_click to KeyCombo(),
          R.string.keycombo_shortcut_review_current_character to KeyCombo(),
          R.string.keycombo_shortcut_review_current_line to KeyCombo(),
          R.string.keycombo_shortcut_review_current_word to KeyCombo(),
          R.string.keycombo_shortcut_review_long_click to KeyCombo(),
          R.string.keycombo_shortcut_review_next_character to KeyCombo(),
          R.string.keycombo_shortcut_review_next_line to KeyCombo(),
          R.string.keycombo_shortcut_review_next_word to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_character to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_line to KeyCombo(),
          R.string.keycombo_shortcut_review_previous_word to KeyCombo(),
          R.string.keycombo_shortcut_review_top to KeyCombo(),
        ),
    )
  }

  /**
   * Forgets the shortcuts saved under [persisterPrefix] for [keymap] pressed with [sandFlyKey] that
   * are still an earlier version's defaults, so that this version's defaults take their place.
   */
  /**
   * The version 2 defaults of the commands whose defaults changed in version 3, when Escape became
   * Back and the show desktop keys Home: none.
   */
  private val VERSION_2: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    val systemKeys =
      mapOf(
        R.string.keycombo_shortcut_global_back to KeyCombo(),
        R.string.keycombo_shortcut_global_home to KeyCombo(),
      )
    mapOf(Keymap.NVDA to systemKeys, Keymap.JAWS to systemKeys, Keymap.ORCA to systemKeys)
  }

  /**
   * The version 3 defaults of the commands whose defaults changed in version 4, when passing keys
   * to the app came: none.
   */
  private val VERSION_3: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    val passKeys =
      mapOf(R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once to KeyCombo())
    mapOf(Keymap.NVDA to passKeys, Keymap.JAWS to passKeys, Keymap.ORCA to passKeys)
  }

  /** Each version's earlier defaults of the commands it changed. */
  private val CHANGES: List<Pair<Int, Map<Keymap, Map<Int, KeyCombo>>>> by lazy {
    listOf(2 to VERSION_1, 3 to VERSION_2, 4 to VERSION_3)
  }

  @JvmStatic
  fun update(context: Context, keymap: Keymap, sandFlyKey: SandFlyKey, persisterPrefix: String) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val versionKey = persisterPrefix + "_defaults_version"
    val saved = prefs.getInt(versionKey, 1)
    if (saved >= VERSION) return
    val persister = KeyComboPersister(context, persisterPrefix)
    for ((version, earlier) in CHANGES) {
      if (saved >= version) continue
      earlier[keymap]?.forEach { (keyRes, oldDefault) ->
        val key = context.getString(keyRes)
        if (!persister.contains(key)) return@forEach
        val keyCombo = persister.getKeyCombo(key)
        // A default unassigned because another took its keys is forgotten too.
        if (
          keyCombo == SandFlyKey.adapt(oldDefault, keymap.nativeKey, sandFlyKey) ||
            keyCombo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()
        ) {
          persister.remove(key)
        }
      }
    }
    prefs.edit().putInt(versionKey, VERSION).apply()
  }
}
