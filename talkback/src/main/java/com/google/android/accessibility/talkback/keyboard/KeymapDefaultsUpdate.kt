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
  /** The version of the defaults of the VoiceOver, NVDA, JAWS, Orca and keyboard phone keymaps. */
  private const val VERSION = 8

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

  /**
   * The version 4 defaults of the commands whose defaults changed in version 5, when NVDA's voice
   * settings and last spoken phrase keys came: none.
   */
  private val VERSION_4: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    mapOf(
      Keymap.NVDA to
        mapOf(
          R.string.keycombo_shortcut_other_open_tts_settings to KeyCombo(),
          R.string.keycombo_shortcut_other_copy_last_spoken_phrase to KeyCombo(),
        )
    )
  }

  /**
   * The version 5 defaults of the commands whose defaults changed in version 6: the keyboard phone
   * keymap's first single keys, which became presses and SandFly key shortcuts. The keyboard phone
   * single-key keymap has them as its own.
   */
  private val VERSION_5: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    fun plain(modifiers: Int, keyCode: Int) = old(modifiers, keyCode, sandFly = false)
    val firstKeys = mutableMapOf<Int, KeyCombo>()
    fun pair(next: Int, previous: Int, keyCode: Int) {
      firstKeys[next] = plain(NONE, keyCode)
      firstKeys[previous] = plain(SHIFT, keyCode)
    }
    pair(
      R.string.keycombo_shortcut_navigate_next_default,
      R.string.keycombo_shortcut_navigate_previous_default,
      KeyEvent.KEYCODE_SPACE,
    )
    pair(
      R.string.keycombo_shortcut_navigate_next_heading,
      R.string.keycombo_shortcut_navigate_previous_heading,
      KeyEvent.KEYCODE_H,
    )
    pair(
      R.string.keycombo_shortcut_navigate_next_link,
      R.string.keycombo_shortcut_navigate_previous_link,
      KeyEvent.KEYCODE_U,
    )
    pair(
      R.string.keycombo_shortcut_navigate_next_control,
      R.string.keycombo_shortcut_navigate_previous_control,
      KeyEvent.KEYCODE_F,
    )
    pair(
      R.string.keycombo_shortcut_navigate_next_edit_field,
      R.string.keycombo_shortcut_navigate_previous_edit_field,
      KeyEvent.KEYCODE_E,
    )
    pair(
      R.string.keycombo_shortcut_navigate_next_graphic,
      R.string.keycombo_shortcut_navigate_previous_graphic,
      KeyEvent.KEYCODE_G,
    )
    firstKeys.putAll(
      mapOf(
        R.string.keycombo_shortcut_navigate_first to plain(NONE, KeyEvent.KEYCODE_T),
        R.string.keycombo_shortcut_navigate_last to plain(NONE, KeyEvent.KEYCODE_B),
        R.string.keycombo_shortcut_global_scroll_forward_reading_menu to plain(NONE, KeyEvent.KEYCODE_L),
        R.string.keycombo_shortcut_global_scroll_backward_reading_menu to plain(NONE, KeyEvent.KEYCODE_J),
        R.string.keycombo_shortcut_global_adjust_reading_setting_next to plain(NONE, KeyEvent.KEYCODE_K),
        R.string.keycombo_shortcut_global_adjust_reading_settings_previous to plain(NONE, KeyEvent.KEYCODE_I),
        R.string.keycombo_shortcut_perform_long_click to plain(SHIFT, KeyEvent.KEYCODE_ENTER),
        R.string.keycombo_shortcut_global_back to plain(NONE, KeyEvent.KEYCODE_DEL),
        R.string.keycombo_shortcut_global_home to plain(SHIFT, KeyEvent.KEYCODE_DEL),
        R.string.keycombo_shortcut_global_notifications to plain(NONE, KeyEvent.KEYCODE_N),
        R.string.keycombo_shortcut_global_recents to plain(SHIFT, KeyEvent.KEYCODE_N),
        R.string.keycombo_shortcut_other_talkback_context_menu to plain(NONE, KeyEvent.KEYCODE_M),
        R.string.keycombo_shortcut_other_custom_actions to plain(SHIFT, KeyEvent.KEYCODE_M),
        R.string.keycombo_shortcut_other_toggle_search to plain(NONE, KeyEvent.KEYCODE_S),
        R.string.keycombo_shortcut_other_open_talkback_settings to plain(SHIFT, KeyEvent.KEYCODE_S),
        R.string.keycombo_shortcut_other_read_from_cursor_item to plain(NONE, KeyEvent.KEYCODE_R),
        R.string.keycombo_shortcut_other_read_from_top to plain(SHIFT, KeyEvent.KEYCODE_R),
        R.string.keycombo_shortcut_global_announce_current_title to plain(NONE, KeyEvent.KEYCODE_W),
        R.string.keycombo_shortcut_global_announce_current_time_and_date to plain(NONE, KeyEvent.KEYCODE_D),
        R.string.keycombo_shortcut_global_announce_battery_state to plain(SHIFT, KeyEvent.KEYCODE_D),
        R.string.keycombo_shortcut_other_repeat_last_spoken_phrase to plain(NONE, KeyEvent.KEYCODE_X),
        R.string.keycombo_shortcut_other_copy_last_spoken_phrase to plain(SHIFT, KeyEvent.KEYCODE_X),
        R.string.keycombo_shortcut_global_describe_image to plain(NONE, KeyEvent.KEYCODE_A),
        R.string.keycombo_shortcut_global_hide_or_show_screen to plain(NONE, KeyEvent.KEYCODE_Z),
        R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog to plain(NONE, KeyEvent.KEYCODE_Q),
        R.string.keycombo_shortcut_toggle_pass_keys_to_app to plain(SHIFT, KeyEvent.KEYCODE_Q),
        R.string.keycombo_shortcut_global_play_pause_media to KeyCombo(),
      )
    )
    mapOf(Keymap.KEYBOARD_PHONE to firstKeys.toMap())
  }

  /**
   * The version 6 defaults of the commands whose defaults changed in version 7, when the keyboard
   * phone single-key keymap got browse mode, with the SandFly keymap's letters: none.
   */
  private val VERSION_6: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    val browseModeKeys =
      listOf(
          R.string.keycombo_shortcut_other_toggle_browse_mode_extra,
          R.string.keycombo_shortcut_navigate_next_aria_landmark,
          R.string.keycombo_shortcut_navigate_previous_aria_landmark,
          R.string.keycombo_shortcut_navigate_next_button,
          R.string.keycombo_shortcut_navigate_previous_button,
          R.string.keycombo_shortcut_navigate_next_checkbox,
          R.string.keycombo_shortcut_navigate_previous_checkbox,
          R.string.keycombo_shortcut_navigate_next_combobox,
          R.string.keycombo_shortcut_navigate_previous_combobox,
          R.string.keycombo_shortcut_navigate_next_list,
          R.string.keycombo_shortcut_navigate_previous_list,
          R.string.keycombo_shortcut_navigate_next_list_item,
          R.string.keycombo_shortcut_navigate_previous_list_item,
          R.string.keycombo_shortcut_navigate_next_radio,
          R.string.keycombo_shortcut_navigate_previous_radio,
          R.string.keycombo_shortcut_navigate_next_table,
          R.string.keycombo_shortcut_navigate_previous_table,
          R.string.keycombo_shortcut_navigate_next_visited_link,
          R.string.keycombo_shortcut_navigate_previous_visited_link,
          R.string.keycombo_shortcut_navigate_next_unvisited_link,
          R.string.keycombo_shortcut_navigate_previous_unvisited_link,
          R.string.keycombo_shortcut_navigate_next_heading_1,
          R.string.keycombo_shortcut_navigate_previous_heading_1,
          R.string.keycombo_shortcut_navigate_next_heading_2,
          R.string.keycombo_shortcut_navigate_previous_heading_2,
          R.string.keycombo_shortcut_navigate_next_heading_3,
          R.string.keycombo_shortcut_navigate_previous_heading_3,
          R.string.keycombo_shortcut_navigate_next_heading_4,
          R.string.keycombo_shortcut_navigate_previous_heading_4,
          R.string.keycombo_shortcut_navigate_next_heading_5,
          R.string.keycombo_shortcut_navigate_previous_heading_5,
          R.string.keycombo_shortcut_navigate_next_heading_6,
          R.string.keycombo_shortcut_navigate_previous_heading_6,
        )
        .associateWith { KeyCombo() }
    mapOf(Keymap.KEYBOARD_PHONE_SINGLE_KEYS to browseModeKeys)
  }

  /**
   * The version 7 defaults of the commands whose defaults changed in version 8, when the keyboard
   * help keys of the VoiceOver, NVDA, JAWS and Orca keymaps moved from Practice keyboard shortcuts
   * to Turn keyboard practice on or off, and the NVDA keymap got Insert + F9 for selection mode.
   */
  private val VERSION_7: Map<Keymap, Map<Int, KeyCombo>> by lazy {
    fun helpKey(keyCode: Int) =
      mapOf(
        R.string.keycombo_shortcut_global_show_learn_mode_page to old(NONE, keyCode, sandFly = true),
        R.string.keycombo_shortcut_toggle_key_practice to KeyCombo(),
      )
    mapOf(
      Keymap.VOICEOVER to helpKey(KeyEvent.KEYCODE_K),
      Keymap.NVDA to
        helpKey(KeyEvent.KEYCODE_1) +
          (R.string.keycombo_shortcut_other_toggle_selection to KeyCombo()),
      Keymap.JAWS to helpKey(KeyEvent.KEYCODE_1),
      Keymap.ORCA to helpKey(KeyEvent.KEYCODE_H),
    )
  }

  /** Each version's earlier defaults of the commands it changed. */
  private val CHANGES: List<Pair<Int, Map<Keymap, Map<Int, KeyCombo>>>> by lazy {
    listOf(
      2 to VERSION_1,
      3 to VERSION_2,
      4 to VERSION_3,
      5 to VERSION_4,
      6 to VERSION_5,
      7 to VERSION_6,
      8 to VERSION_7,
    )
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
