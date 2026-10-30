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
 * The default shortcuts of the VoiceOver keymap: VoiceOver's keyboard commands on iPhone, with the
 * SandFly key as the VO key (Ctrl + Alt) and Meta as Command. In browse mode, single keys move as
 * VoiceOver's single-key Quick Nav does.
 */
object VoiceOverKeymap : KeymapDefaults() {
  private fun vo(modifiers: Int, keyCode: Int) = sandFly(modifiers, keyCode)

  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      // Moving and activating.
      put(R.string.keycombo_shortcut_navigate_next_default, vo(NONE, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(R.string.keycombo_shortcut_navigate_previous_default, vo(NONE, KeyEvent.KEYCODE_DPAD_LEFT))
      put(R.string.keycombo_shortcut_navigate_first, vo(NONE, KeyEvent.KEYCODE_MOVE_HOME))
      put(R.string.keycombo_shortcut_navigate_last, vo(NONE, KeyEvent.KEYCODE_MOVE_END))
      put(R.string.keycombo_shortcut_perform_click, vo(NONE, KeyEvent.KEYCODE_SPACE))
      put(R.string.keycombo_shortcut_perform_long_click, vo(SHIFT, KeyEvent.KEYCODE_SPACE))
      put(R.string.keycombo_shortcut_other_custom_actions, vo(META, KeyEvent.KEYCODE_SPACE))
      put(R.string.keycombo_shortcut_global_play_pause_media, vo(NONE, KeyEvent.KEYCODE_MINUS))
      put(
        R.string.keycombo_shortcut_navigate_next_window,
        vo(SHIFT or META, KeyEvent.KEYCODE_DPAD_DOWN),
      )
      put(
        R.string.keycombo_shortcut_navigate_previous_window,
        vo(SHIFT or META, KeyEvent.KEYCODE_DPAD_UP),
      )
      put(
        R.string.keycombo_shortcut_navigate_next_container,
        vo(SHIFT or META, KeyEvent.KEYCODE_DPAD_RIGHT),
      )
      put(
        R.string.keycombo_shortcut_navigate_previous_container,
        vo(SHIFT or META, KeyEvent.KEYCODE_DPAD_LEFT),
      )

      // The rotor is the reading controls.
      put(
        R.string.keycombo_shortcut_global_adjust_reading_setting_next,
        vo(NONE, KeyEvent.KEYCODE_DPAD_DOWN),
      )
      put(
        R.string.keycombo_shortcut_global_adjust_reading_settings_previous,
        vo(NONE, KeyEvent.KEYCODE_DPAD_UP),
      )
      put(
        R.string.keycombo_shortcut_global_scroll_forward_reading_menu,
        vo(META, KeyEvent.KEYCODE_DPAD_RIGHT),
      )
      put(
        R.string.keycombo_shortcut_global_scroll_backward_reading_menu,
        vo(META, KeyEvent.KEYCODE_DPAD_LEFT),
      )

      // Reading.
      put(R.string.keycombo_shortcut_other_read_from_cursor_item, vo(NONE, KeyEvent.KEYCODE_A))
      put(R.string.keycombo_shortcut_other_read_from_top, vo(NONE, KeyEvent.KEYCODE_B))
      put(
        R.string.keycombo_shortcut_other_copy_last_spoken_phrase,
        vo(SHIFT, KeyEvent.KEYCODE_C),
      )
      put(R.string.keycombo_shortcut_other_toggle_search, vo(NONE, KeyEvent.KEYCODE_I))
      put(R.string.keycombo_shortcut_global_toggle_voice_feedback, vo(NONE, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_global_hide_or_show_screen, vo(SHIFT, KeyEvent.KEYCODE_S))

      // Device.
      put(R.string.keycombo_shortcut_global_home, vo(NONE, KeyEvent.KEYCODE_H))
      put(R.string.keycombo_shortcut_global_recents, vo(SHIFT, KeyEvent.KEYCODE_H))
      put(R.string.keycombo_shortcut_global_notifications, vo(NONE, KeyEvent.KEYCODE_N))
      put(R.string.keycombo_shortcut_other_talkback_context_menu, vo(SHIFT, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_other_language_options, vo(SHIFT, KeyEvent.KEYCODE_L))

      // Speech.
      put(
        R.string.keycombo_shortcut_global_speech_volume_increase,
        vo(NONE, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_volume_decrease,
        vo(NONE, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
      put(
        R.string.keycombo_shortcut_global_speech_pitch_increase,
        vo(SHIFT, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_pitch_decrease,
        vo(SHIFT, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
      put(
        R.string.keycombo_shortcut_global_speech_rate_increase,
        vo(META, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_rate_decrease,
        vo(META, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
      put(R.string.keycombo_shortcut_global_toggle_sound_feedback, vo(SHIFT, KeyEvent.KEYCODE_E))

      // Help.
      put(
        R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog,
        vo(SHIFT, KeyEvent.KEYCODE_SLASH),
      )
      put(R.string.keycombo_shortcut_global_show_learn_mode_page, vo(NONE, KeyEvent.KEYCODE_K))
      put(R.string.keycombo_shortcut_other_show_tutorial, vo(SHIFT, KeyEvent.KEYCODE_K))

      // Two keys one after the other, as in the SandFly keymap.
      put(R.string.keycombo_shortcut_other_read_current_url, sequence(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_U))
      put(R.string.keycombo_shortcut_global_screen_overview, sequence(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_global_describe_image, sequence(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_I))
      put(
        R.string.keycombo_shortcut_global_refocus_current_node,
        sequence(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_R),
      )
      put(R.string.keycombo_shortcut_other_read_link_url, sequence(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_L))
      put(
        R.string.keycombo_shortcut_other_open_talkback_settings,
        sequence(KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_S),
      )
      put(
        R.string.keycombo_shortcut_other_open_tts_settings,
        sequence(KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_T),
      )
      put(R.string.keycombo_shortcut_other_report_issue, sequence(KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_I))

      // Browse mode is VoiceOver's single-key Quick Nav, which VO + Q turns on and off.
      put(R.string.keycombo_shortcut_other_toggle_browse_mode_extra, vo(NONE, KeyEvent.KEYCODE_Q))
      fun pair(next: Int, previous: Int, keyCode: Int) {
        put(next, browse(NONE, keyCode))
        put(previous, browse(SHIFT, keyCode))
      }
      pair(
        R.string.keycombo_shortcut_navigate_next_aria_landmark,
        R.string.keycombo_shortcut_navigate_previous_aria_landmark,
        KeyEvent.KEYCODE_W,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_button,
        R.string.keycombo_shortcut_navigate_previous_button,
        KeyEvent.KEYCODE_B,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_control,
        R.string.keycombo_shortcut_navigate_previous_control,
        KeyEvent.KEYCODE_C,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading,
        R.string.keycombo_shortcut_navigate_previous_heading,
        KeyEvent.KEYCODE_H,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_graphic,
        R.string.keycombo_shortcut_navigate_previous_graphic,
        KeyEvent.KEYCODE_I,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_link,
        R.string.keycombo_shortcut_navigate_previous_link,
        KeyEvent.KEYCODE_L,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_list,
        R.string.keycombo_shortcut_navigate_previous_list,
        KeyEvent.KEYCODE_X,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_table,
        R.string.keycombo_shortcut_navigate_previous_table,
        KeyEvent.KEYCODE_T,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_edit_field,
        R.string.keycombo_shortcut_navigate_previous_edit_field,
        KeyEvent.KEYCODE_R,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_1,
        R.string.keycombo_shortcut_navigate_previous_heading_1,
        KeyEvent.KEYCODE_1,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_2,
        R.string.keycombo_shortcut_navigate_previous_heading_2,
        KeyEvent.KEYCODE_2,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_3,
        R.string.keycombo_shortcut_navigate_previous_heading_3,
        KeyEvent.KEYCODE_3,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_4,
        R.string.keycombo_shortcut_navigate_previous_heading_4,
        KeyEvent.KEYCODE_4,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_5,
        R.string.keycombo_shortcut_navigate_previous_heading_5,
        KeyEvent.KEYCODE_5,
      )
      pair(
        R.string.keycombo_shortcut_navigate_next_heading_6,
        R.string.keycombo_shortcut_navigate_previous_heading_6,
        KeyEvent.KEYCODE_6,
      )

      // Text and tables in browse mode, where VO + arrows still move by item.
      put(R.string.keycombo_shortcut_navigate_next_character, browse(NONE, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_character,
        browse(NONE, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      put(R.string.keycombo_shortcut_navigate_next_line, browse(NONE, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_line, browse(NONE, KeyEvent.KEYCODE_DPAD_UP))
      val ctrl = CTRL
      put(R.string.keycombo_shortcut_navigate_next_word, browse(ctrl, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(R.string.keycombo_shortcut_navigate_previous_word, browse(ctrl, KeyEvent.KEYCODE_DPAD_LEFT))
      put(R.string.keycombo_shortcut_navigate_next_paragraph, browse(ctrl, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_paragraph, browse(ctrl, KeyEvent.KEYCODE_DPAD_UP))
      val ctrlShift = CTRL or SHIFT
      put(R.string.keycombo_shortcut_navigate_next_row, browse(ctrlShift, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_row, browse(ctrlShift, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_column, browse(ctrlShift, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_column,
        browse(ctrlShift, KeyEvent.KEYCODE_DPAD_LEFT),
      )
    }
}
