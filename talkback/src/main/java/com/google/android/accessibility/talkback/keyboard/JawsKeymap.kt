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
import com.google.android.accessibility.talkback.R

/**
 * The default shortcuts of the JAWS keymap: JAWS's desktop keystrokes, with the SandFly key as the
 * JAWS key (Insert), and its laptop clicks and cursor keys, which need no number pad. The arrow keys
 * move SandFly focus as on a computer, and Tab moves between items that take input focus. The
 * review cursor works as the JAWS cursor: while it's on, the arrow keys move it over the screen as
 * it is laid out and clicks go where it is. In browse mode, single keys move as JAWS's quick
 * navigation keys do.
 */
object JawsKeymap : KeymapDefaults() {
  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      putDesktopKeys()
      // Windows + D shows the desktop.
      putSystemKeys(META)
      put(R.string.keycombo_shortcut_perform_click, sandFly(NONE, KeyEvent.KEYCODE_8))
      put(R.string.keycombo_shortcut_perform_long_click, sandFly(NONE, KeyEvent.KEYCODE_9))
      put(R.string.keycombo_shortcut_other_custom_actions, sandFly(NONE, KeyEvent.KEYCODE_MENU))
      put(R.string.keycombo_shortcut_global_refocus_current_node, sandFly(NONE, KeyEvent.KEYCODE_DPAD_UP))

      // The JAWS cursor is the review cursor.
      put(R.string.keycombo_shortcut_review_cursor_on, sandFly(NONE, KeyEvent.KEYCODE_P))
      put(R.string.keycombo_shortcut_review_cursor_off, sandFly(NONE, KeyEvent.KEYCODE_SEMICOLON))
      put(R.string.keycombo_shortcut_review_move_to_focus, sandFly(NONE, KeyEvent.KEYCODE_LEFT_BRACKET))
      put(R.string.keycombo_shortcut_focus_review_cursor, sandFly(NONE, KeyEvent.KEYCODE_RIGHT_BRACKET))
      put(R.string.keycombo_shortcut_recognize_screen_text, sandFly(SHIFT, KeyEvent.KEYCODE_O))

      put(R.string.keycombo_shortcut_other_read_from_cursor_item, sandFly(NONE, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_other_read_from_top, sandFly(NONE, KeyEvent.KEYCODE_B))
      put(R.string.keycombo_shortcut_global_announce_current_title, sandFly(NONE, KeyEvent.KEYCODE_T))
      put(
        R.string.keycombo_shortcut_global_announce_current_time_and_date,
        sandFly(NONE, KeyEvent.KEYCODE_F12),
      )
      put(R.string.keycombo_shortcut_global_notifications, sandFly(NONE, KeyEvent.KEYCODE_PAGE_DOWN))
      put(R.string.keycombo_shortcut_other_read_current_url, sandFly(NONE, KeyEvent.KEYCODE_A))

      put(R.string.keycombo_shortcut_other_talkback_context_menu, sandFly(NONE, KeyEvent.KEYCODE_J))
      put(R.string.keycombo_shortcut_global_show_learn_mode_page, sandFly(NONE, KeyEvent.KEYCODE_1))
      putReadingControlKeys()
      putSpeechKeys()
      put(
        R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog,
        sandFly(SHIFT, KeyEvent.KEYCODE_SLASH),
      )

      // Keys for the app: JAWS's pass key through, and every key without the SandFly key.
      put(
        R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once,
        sandFly(NONE, KeyEvent.KEYCODE_3),
      )
      put(R.string.keycombo_shortcut_toggle_pass_keys_to_app, sandFly(SHIFT, KeyEvent.KEYCODE_Z))

      // The virtual cursor is browse mode.
      put(R.string.keycombo_shortcut_other_toggle_browse_mode_extra, sandFly(NONE, KeyEvent.KEYCODE_Z))
      put(R.string.keycombo_shortcut_other_show_control_list, sandFly(NONE, KeyEvent.KEYCODE_F5))
      put(R.string.keycombo_shortcut_other_show_heading_list, sandFly(NONE, KeyEvent.KEYCODE_F6))
      put(R.string.keycombo_shortcut_other_show_link_list, sandFly(NONE, KeyEvent.KEYCODE_F7))
      put(R.string.keycombo_shortcut_other_toggle_search, sandFly(CTRL, KeyEvent.KEYCODE_F))
      put(R.string.keycombo_shortcut_navigate_next_row, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_row, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_column, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_column,
        browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      browsePair(R.string.keycombo_shortcut_navigate_next_heading, R.string.keycombo_shortcut_navigate_previous_heading, KeyEvent.KEYCODE_H)
      browsePair(R.string.keycombo_shortcut_navigate_next_list, R.string.keycombo_shortcut_navigate_previous_list, KeyEvent.KEYCODE_L)
      browsePair(R.string.keycombo_shortcut_navigate_next_list_item, R.string.keycombo_shortcut_navigate_previous_list_item, KeyEvent.KEYCODE_I)
      browsePair(R.string.keycombo_shortcut_navigate_next_table, R.string.keycombo_shortcut_navigate_previous_table, KeyEvent.KEYCODE_T)
      browsePair(R.string.keycombo_shortcut_navigate_next_control, R.string.keycombo_shortcut_navigate_previous_control, KeyEvent.KEYCODE_F)
      browsePair(R.string.keycombo_shortcut_navigate_next_button, R.string.keycombo_shortcut_navigate_previous_button, KeyEvent.KEYCODE_B)
      browsePair(R.string.keycombo_shortcut_navigate_next_edit_field, R.string.keycombo_shortcut_navigate_previous_edit_field, KeyEvent.KEYCODE_E)
      browsePair(R.string.keycombo_shortcut_navigate_next_checkbox, R.string.keycombo_shortcut_navigate_previous_checkbox, KeyEvent.KEYCODE_X)
      browsePair(R.string.keycombo_shortcut_navigate_next_combobox, R.string.keycombo_shortcut_navigate_previous_combobox, KeyEvent.KEYCODE_C)
      browsePair(R.string.keycombo_shortcut_navigate_next_radio, R.string.keycombo_shortcut_navigate_previous_radio, KeyEvent.KEYCODE_A)
      browsePair(R.string.keycombo_shortcut_navigate_next_graphic, R.string.keycombo_shortcut_navigate_previous_graphic, KeyEvent.KEYCODE_G)
      browsePair(R.string.keycombo_shortcut_navigate_next_aria_landmark, R.string.keycombo_shortcut_navigate_previous_aria_landmark, KeyEvent.KEYCODE_R)
      browsePair(R.string.keycombo_shortcut_navigate_next_unvisited_link, R.string.keycombo_shortcut_navigate_previous_unvisited_link, KeyEvent.KEYCODE_U)
      browsePair(R.string.keycombo_shortcut_navigate_next_visited_link, R.string.keycombo_shortcut_navigate_previous_visited_link, KeyEvent.KEYCODE_V)
      headingLevels()
    }
}
