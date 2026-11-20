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
 * The default shortcuts of the keyboard phone keymap, for phones whose keyboard has letters, Shift,
 * Alt and often Sym, but no Ctrl, Meta or arrow keys: the Unihertz Titan phones, BlackBerry KEYone
 * and KEY2, and Zinwa's phones. Single keys work outside text fields being edited, Shift with most
 * of them goes the other way, and the SandFly key with any of them does the same anywhere, text
 * fields too. Space moves by item, as the space bar scrolls on BlackBerry, and T and B go to the top
 * and bottom, as on BlackBerry. Written for Sym as the SandFly key; none uses Alt or Meta, so Right
 * Alt, Alt and Meta can be the SandFly key too.
 */
object KeyboardPhoneKeymap : KeymapDefaults() {
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

      // Acting.
      put(R.string.keycombo_shortcut_perform_click, plain(NONE, KeyEvent.KEYCODE_ENTER))
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

  /** Puts [keyCode] for [next] and Shift + [keyCode] for [previous], outside text fields. */
  private fun MutableMap<Int, KeyCombo>.plainPair(next: Int, previous: Int, keyCode: Int) {
    put(next, plain(NONE, keyCode))
    put(previous, plain(SHIFT, keyCode))
  }
}
