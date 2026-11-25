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
 * The default shortcuts of the keyboard phone single-key keymap, for the same phones as the keyboard
 * phone keymap, by keys alone. Single keys work outside text fields being edited, Shift with most of
 * them goes the other way, and the SandFly key with any of them does the same anywhere, text fields
 * too, so SandFly key + Space moves out of a text field. Space moves by item, as the space bar
 * scrolls on BlackBerry, and T and B go to the top and bottom, as on BlackBerry. Enter activates the
 * item SandFly focus moved to; after typing, it goes to the app. Alt pressed twice plays or pauses
 * media and answers calls.
 *
 * In browse mode, letters move by element as in the SandFly keymap's browse mode: B buttons, C
 * combo boxes, D landmarks, I list items, K links, L lists, R radio buttons, T tables, U unvisited
 * links, V visited links, X check boxes and 1 to 6 heading levels, with Shift backwards, besides H,
 * F, E and G, which do the same in and outside browse mode. The SandFly key with one of these
 * letters does what the letter does outside browse mode, such as SandFly key + B to the bottom. The
 * SandFly key pressed twice turns browse mode on or off.
 *
 * Written for Sym as the SandFly key; none uses Meta, so Right Alt, Alt and Meta can be the SandFly
 * key too. Where Alt is the SandFly key, Alt pressed twice stays Play or pause, and SandFly key + Q
 * turns browse mode on or off.
 */
object KeyboardPhoneSingleKeyKeymap : KeymapDefaults() {
  /** The SandFly key pressed twice on its own; before [defaults], which uses it. */
  private val SANDFLY_KEY_TWICE: KeyCombo =
    KeyCombo.modifierKeys(NONE, /* triggerModifierUsed= */ true).withPressType(KeyCombo.PRESSED_TWICE)

  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      // Moving.
      plainPair(
        R.string.keycombo_shortcut_navigate_next_default,
        R.string.keycombo_shortcut_navigate_previous_default,
        KeyEvent.KEYCODE_SPACE,
      )
      put(R.string.keycombo_shortcut_navigate_first, plain(NONE, KeyEvent.KEYCODE_T))
      put(R.string.keycombo_shortcut_navigate_last, plain(NONE, KeyEvent.KEYCODE_B))
      // Reading controls on the keys where the arrows would be, J, L, I and K: J and L choose one,
      // I and K move by it or change it.
      put(R.string.keycombo_shortcut_global_scroll_forward_reading_menu, plain(NONE, KeyEvent.KEYCODE_L))
      put(R.string.keycombo_shortcut_global_scroll_backward_reading_menu, plain(NONE, KeyEvent.KEYCODE_J))
      put(R.string.keycombo_shortcut_global_adjust_reading_setting_next, plain(NONE, KeyEvent.KEYCODE_K))
      put(R.string.keycombo_shortcut_global_adjust_reading_settings_previous, plain(NONE, KeyEvent.KEYCODE_I))
      plainPair(
        R.string.keycombo_shortcut_navigate_next_heading,
        R.string.keycombo_shortcut_navigate_previous_heading,
        KeyEvent.KEYCODE_H,
      )
      plainPair(
        R.string.keycombo_shortcut_navigate_next_link,
        R.string.keycombo_shortcut_navigate_previous_link,
        KeyEvent.KEYCODE_U,
      )
      plainPair(
        R.string.keycombo_shortcut_navigate_next_control,
        R.string.keycombo_shortcut_navigate_previous_control,
        KeyEvent.KEYCODE_F,
      )
      plainPair(
        R.string.keycombo_shortcut_navigate_next_edit_field,
        R.string.keycombo_shortcut_navigate_previous_edit_field,
        KeyEvent.KEYCODE_E,
      )
      plainPair(
        R.string.keycombo_shortcut_navigate_next_graphic,
        R.string.keycombo_shortcut_navigate_previous_graphic,
        KeyEvent.KEYCODE_G,
      )

      // Browse mode: the SandFly keymap's letters, which come before the keys above while browse
      // mode is on. H, F, E and G are the same in both.
      put(R.string.keycombo_shortcut_other_toggle_browse_mode_extra, SANDFLY_KEY_TWICE)
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_aria_landmark,
        R.string.keycombo_shortcut_navigate_previous_aria_landmark,
        KeyEvent.KEYCODE_D,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_button,
        R.string.keycombo_shortcut_navigate_previous_button,
        KeyEvent.KEYCODE_B,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_checkbox,
        R.string.keycombo_shortcut_navigate_previous_checkbox,
        KeyEvent.KEYCODE_X,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_combobox,
        R.string.keycombo_shortcut_navigate_previous_combobox,
        KeyEvent.KEYCODE_C,
      )
      // Links have U outside browse mode, so K is their second key.
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_link_extra,
        R.string.keycombo_shortcut_navigate_previous_link_extra,
        KeyEvent.KEYCODE_K,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_list,
        R.string.keycombo_shortcut_navigate_previous_list,
        KeyEvent.KEYCODE_L,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_list_item,
        R.string.keycombo_shortcut_navigate_previous_list_item,
        KeyEvent.KEYCODE_I,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_radio,
        R.string.keycombo_shortcut_navigate_previous_radio,
        KeyEvent.KEYCODE_R,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_table,
        R.string.keycombo_shortcut_navigate_previous_table,
        KeyEvent.KEYCODE_T,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_visited_link,
        R.string.keycombo_shortcut_navigate_previous_visited_link,
        KeyEvent.KEYCODE_V,
      )
      inBrowseModePair(
        R.string.keycombo_shortcut_navigate_next_unvisited_link,
        R.string.keycombo_shortcut_navigate_previous_unvisited_link,
        KeyEvent.KEYCODE_U,
      )
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_1, R.string.keycombo_shortcut_navigate_previous_heading_1, KeyEvent.KEYCODE_1)
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_2, R.string.keycombo_shortcut_navigate_previous_heading_2, KeyEvent.KEYCODE_2)
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_3, R.string.keycombo_shortcut_navigate_previous_heading_3, KeyEvent.KEYCODE_3)
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_4, R.string.keycombo_shortcut_navigate_previous_heading_4, KeyEvent.KEYCODE_4)
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_5, R.string.keycombo_shortcut_navigate_previous_heading_5, KeyEvent.KEYCODE_5)
      inBrowseModePair(R.string.keycombo_shortcut_navigate_next_heading_6, R.string.keycombo_shortcut_navigate_previous_heading_6, KeyEvent.KEYCODE_6)

      // Acting.
      put(R.string.keycombo_shortcut_perform_click, plain(NONE, KeyEvent.KEYCODE_ENTER))
      put(
        R.string.keycombo_shortcut_global_play_pause_media,
        KeyCombo.modifierKeys(ALT, false).withPressType(KeyCombo.PRESSED_TWICE),
      )
      put(R.string.keycombo_shortcut_perform_long_click, plain(SHIFT, KeyEvent.KEYCODE_ENTER))
      put(R.string.keycombo_shortcut_global_back, plain(NONE, KeyEvent.KEYCODE_DEL))
      put(R.string.keycombo_shortcut_global_home, plain(SHIFT, KeyEvent.KEYCODE_DEL))
      put(R.string.keycombo_shortcut_global_notifications, plain(NONE, KeyEvent.KEYCODE_N))
      put(R.string.keycombo_shortcut_global_recents, plain(SHIFT, KeyEvent.KEYCODE_N))
      put(R.string.keycombo_shortcut_other_talkback_context_menu, plain(NONE, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_other_custom_actions, plain(SHIFT, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_other_toggle_search, plain(NONE, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_other_open_talkback_settings, plain(SHIFT, KeyEvent.KEYCODE_S))

      // Reading.
      put(R.string.keycombo_shortcut_other_read_from_cursor_item, plain(NONE, KeyEvent.KEYCODE_R))
      put(R.string.keycombo_shortcut_other_read_from_top, plain(SHIFT, KeyEvent.KEYCODE_R))
      put(R.string.keycombo_shortcut_global_announce_current_title, plain(NONE, KeyEvent.KEYCODE_W))
      put(R.string.keycombo_shortcut_global_announce_current_time_and_date, plain(NONE, KeyEvent.KEYCODE_D))
      put(R.string.keycombo_shortcut_global_announce_battery_state, plain(SHIFT, KeyEvent.KEYCODE_D))
      put(R.string.keycombo_shortcut_other_repeat_last_spoken_phrase, plain(NONE, KeyEvent.KEYCODE_X))
      put(R.string.keycombo_shortcut_other_copy_last_spoken_phrase, plain(SHIFT, KeyEvent.KEYCODE_X))
      put(R.string.keycombo_shortcut_global_describe_image, plain(NONE, KeyEvent.KEYCODE_A))
      put(R.string.keycombo_shortcut_global_hide_or_show_screen, plain(NONE, KeyEvent.KEYCODE_Z))

      // SandFly and the keyboard.
      put(R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog, plain(NONE, KeyEvent.KEYCODE_Q))
      put(R.string.keycombo_shortcut_toggle_pass_keys_to_app, plain(SHIFT, KeyEvent.KEYCODE_Q))
      // Only with the SandFly key: with keyboard input off, or for the next key, every other key
      // goes to the app.
      put(
        R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once,
        sandFly(NONE, KeyEvent.KEYCODE_P),
      )
      put(R.string.keycombo_shortcut_toggle_keyboard_input, sandFly(SHIFT, KeyEvent.KEYCODE_P))
      // Keyboard practice speaks each key and its command: K for keys.
      put(R.string.keycombo_shortcut_toggle_key_practice, sandFly(SHIFT, KeyEvent.KEYCODE_K))
      // A touch held on the focused item, as for recording a voice message: V for voice. The SandFly
      // key and V are under the two thumbs, so both are easy to hold down.
      put(R.string.keycombo_shortcut_touch_hold_focus, sandFly(NONE, KeyEvent.KEYCODE_V))
      // No Ctrl: the SandFly key with the volume keys changes the speech rate, with Shift the speech
      // volume.
      put(R.string.keycombo_shortcut_global_speech_rate_increase, sandFly(NONE, KeyEvent.KEYCODE_VOLUME_UP))
      put(R.string.keycombo_shortcut_global_speech_rate_decrease, sandFly(NONE, KeyEvent.KEYCODE_VOLUME_DOWN))
      put(
        R.string.keycombo_shortcut_global_speech_volume_increase,
        sandFly(SHIFT, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_volume_decrease,
        sandFly(SHIFT, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
    }

  // Touch can be locked from the keyboard, next to Z, which hides the screen. In text fields,
  // with the SandFly keyboard, the SandFly key with O searches emoji, with C turns editing mode on
  // or off and with Y spells the password field, as in the keyboard phone keymap, and Alt + Space
  // chooses a symbol, as on BlackBerry keyboards.
  override val commandDefaults: Map<Int, KeyCombo> =
    mapOf(
      R.string.shortcut_value_toggle_touch_lock to plain(SHIFT, KeyEvent.KEYCODE_Z),
      R.string.shortcut_value_search_emoji to sandFly(NONE, KeyEvent.KEYCODE_O),
      R.string.shortcut_value_toggle_editing_mode to sandFly(NONE, KeyEvent.KEYCODE_C),
      R.string.shortcut_value_spell_password_field to sandFly(NONE, KeyEvent.KEYCODE_Y),
      R.string.shortcut_value_choose_symbol to editing(ALT, KeyEvent.KEYCODE_SPACE),
    )

  // Alt pressed twice plays or pauses media; where Alt is the SandFly key, browse mode turns on and
  // off with SandFly key + Q instead.
  override val fallbackDefaults: Map<Int, KeyCombo> =
    mapOf(R.string.keycombo_shortcut_other_toggle_browse_mode_extra to sandFly(NONE, KeyEvent.KEYCODE_Q))

  /** Puts [keyCode] for [next] and Shift + [keyCode] for [previous], outside text fields. */
  private fun MutableMap<Int, KeyCombo>.plainPair(next: Int, previous: Int, keyCode: Int) {
    put(next, plain(NONE, keyCode))
    put(previous, plain(SHIFT, keyCode))
  }
}
