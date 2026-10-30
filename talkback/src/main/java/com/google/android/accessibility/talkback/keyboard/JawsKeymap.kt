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
 * JAWS key (Insert), and its laptop clicks, which need no number pad. JAWS has no keys to move by
 * item, so Insert with the left and right arrows does. In browse mode, single keys move as JAWS's
 * quick navigation keys do; Tab moves by link, as it does in JAWS.
 */
object JawsKeymap : KeymapDefaults() {
  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      put(R.string.keycombo_shortcut_navigate_next_default, sandFly(NONE, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(R.string.keycombo_shortcut_navigate_previous_default, sandFly(NONE, KeyEvent.KEYCODE_DPAD_LEFT))
      put(R.string.keycombo_shortcut_perform_click, sandFly(NONE, KeyEvent.KEYCODE_8))
      put(R.string.keycombo_shortcut_perform_long_click, sandFly(NONE, KeyEvent.KEYCODE_9))
      put(R.string.keycombo_shortcut_other_custom_actions, sandFly(NONE, KeyEvent.KEYCODE_MENU))
      put(R.string.keycombo_shortcut_global_refocus_current_node, sandFly(NONE, KeyEvent.KEYCODE_TAB))

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

      // The virtual cursor is browse mode.
      put(R.string.keycombo_shortcut_other_toggle_browse_mode_extra, sandFly(NONE, KeyEvent.KEYCODE_Z))
      put(R.string.keycombo_shortcut_other_show_control_list, sandFly(NONE, KeyEvent.KEYCODE_F5))
      put(R.string.keycombo_shortcut_other_show_heading_list, sandFly(NONE, KeyEvent.KEYCODE_F6))
      put(R.string.keycombo_shortcut_other_show_link_list, sandFly(NONE, KeyEvent.KEYCODE_F7))
      put(R.string.keycombo_shortcut_other_toggle_search, sandFly(CTRL, KeyEvent.KEYCODE_F))
      putCaretKeys()
      put(R.string.keycombo_shortcut_navigate_next_paragraph, browse(CTRL, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_paragraph, browse(CTRL, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_row, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_row, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_column, browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_column,
        browse(CTRL or ALT, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      browsePair(R.string.keycombo_shortcut_navigate_next_link, R.string.keycombo_shortcut_navigate_previous_link, KeyEvent.KEYCODE_TAB)
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
