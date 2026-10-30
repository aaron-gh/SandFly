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
 * The default shortcuts of the Orca keymap: Orca's laptop layout with the SandFly key as the Orca
 * modifier (Insert), which needs no number pad. Flat review's previous and next line move by item.
 * In browse mode, single keys move as Orca's structural navigation does.
 */
object OrcaKeymap : KeymapDefaults() {
  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      put(R.string.keycombo_shortcut_navigate_next_default, sandFly(NONE, KeyEvent.KEYCODE_O))
      put(R.string.keycombo_shortcut_navigate_previous_default, sandFly(NONE, KeyEvent.KEYCODE_U))
      put(R.string.keycombo_shortcut_perform_click, sandFly(NONE, KeyEvent.KEYCODE_7))
      put(R.string.keycombo_shortcut_perform_long_click, sandFly(NONE, KeyEvent.KEYCODE_8))
      put(R.string.keycombo_shortcut_other_custom_actions, sandFly(NONE, KeyEvent.KEYCODE_MENU))
      put(R.string.keycombo_shortcut_global_refocus_current_node, sandFly(NONE, KeyEvent.KEYCODE_ENTER))

      put(R.string.keycombo_shortcut_other_read_from_cursor_item, sandFly(NONE, KeyEvent.KEYCODE_SEMICOLON))
      put(
        R.string.keycombo_shortcut_global_announce_current_time_and_date,
        sandFly(NONE, KeyEvent.KEYCODE_T),
      )

      put(R.string.keycombo_shortcut_global_show_learn_mode_page, sandFly(NONE, KeyEvent.KEYCODE_H))
      put(R.string.keycombo_shortcut_global_toggle_voice_feedback, sandFly(NONE, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_other_open_talkback_settings, sandFly(NONE, KeyEvent.KEYCODE_SPACE))
      put(R.string.keycombo_shortcut_other_talkback_context_menu, sandFly(CTRL, KeyEvent.KEYCODE_SPACE))
      putReadingControlKeys()
      putSpeechKeys()
      put(
        R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog,
        sandFly(SHIFT, KeyEvent.KEYCODE_SLASH),
      )

      put(R.string.keycombo_shortcut_other_toggle_browse_mode_extra, sandFly(NONE, KeyEvent.KEYCODE_A))
      putCaretKeys()
      put(R.string.keycombo_shortcut_other_show_heading_list, browse(ALT or SHIFT, KeyEvent.KEYCODE_H))
      put(R.string.keycombo_shortcut_other_show_link_list, browse(ALT or SHIFT, KeyEvent.KEYCODE_K))
      put(R.string.keycombo_shortcut_other_show_control_list, browse(ALT or SHIFT, KeyEvent.KEYCODE_F))
      put(R.string.keycombo_shortcut_other_show_table_list, browse(ALT or SHIFT, KeyEvent.KEYCODE_T))
      put(R.string.keycombo_shortcut_other_show_landmark_list, browse(ALT or SHIFT, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_navigate_next_row, browse(ALT or SHIFT, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_row, browse(ALT or SHIFT, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_column, browse(ALT or SHIFT, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_column,
        browse(ALT or SHIFT, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      browsePair(R.string.keycombo_shortcut_navigate_next_paragraph, R.string.keycombo_shortcut_navigate_previous_paragraph, KeyEvent.KEYCODE_P)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading, R.string.keycombo_shortcut_navigate_previous_heading, KeyEvent.KEYCODE_H)
      browsePair(R.string.keycombo_shortcut_navigate_next_link, R.string.keycombo_shortcut_navigate_previous_link, KeyEvent.KEYCODE_K)
      browsePair(R.string.keycombo_shortcut_navigate_next_list, R.string.keycombo_shortcut_navigate_previous_list, KeyEvent.KEYCODE_L)
      browsePair(R.string.keycombo_shortcut_navigate_next_list_item, R.string.keycombo_shortcut_navigate_previous_list_item, KeyEvent.KEYCODE_I)
      browsePair(R.string.keycombo_shortcut_navigate_next_table, R.string.keycombo_shortcut_navigate_previous_table, KeyEvent.KEYCODE_T)
      browsePair(R.string.keycombo_shortcut_navigate_next_control, R.string.keycombo_shortcut_navigate_previous_control, KeyEvent.KEYCODE_F)
      browsePair(R.string.keycombo_shortcut_navigate_next_button, R.string.keycombo_shortcut_navigate_previous_button, KeyEvent.KEYCODE_B)
      browsePair(R.string.keycombo_shortcut_navigate_next_edit_field, R.string.keycombo_shortcut_navigate_previous_edit_field, KeyEvent.KEYCODE_E)
      browsePair(R.string.keycombo_shortcut_navigate_next_checkbox, R.string.keycombo_shortcut_navigate_previous_checkbox, KeyEvent.KEYCODE_X)
      browsePair(R.string.keycombo_shortcut_navigate_next_combobox, R.string.keycombo_shortcut_navigate_previous_combobox, KeyEvent.KEYCODE_C)
      browsePair(R.string.keycombo_shortcut_navigate_next_radio, R.string.keycombo_shortcut_navigate_previous_radio, KeyEvent.KEYCODE_R)
      browsePair(R.string.keycombo_shortcut_navigate_next_graphic, R.string.keycombo_shortcut_navigate_previous_graphic, KeyEvent.KEYCODE_G)
      browsePair(R.string.keycombo_shortcut_navigate_next_aria_landmark, R.string.keycombo_shortcut_navigate_previous_aria_landmark, KeyEvent.KEYCODE_M)
      browsePair(R.string.keycombo_shortcut_navigate_next_unvisited_link, R.string.keycombo_shortcut_navigate_previous_unvisited_link, KeyEvent.KEYCODE_U)
      browsePair(R.string.keycombo_shortcut_navigate_next_visited_link, R.string.keycombo_shortcut_navigate_previous_visited_link, KeyEvent.KEYCODE_V)
      headingLevels()
    }
}
