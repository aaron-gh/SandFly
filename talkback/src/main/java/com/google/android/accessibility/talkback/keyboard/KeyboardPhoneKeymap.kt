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
 * and KEY2, and Zinwa's phones.
 *
 * None is a letter or Space on its own, which typing needs: the most used
 * are presses of keys that typing leaves alone, as Key Mapper's are, and work anywhere, text fields
 * too. The SandFly key, Sym, pressed alone moves to the next item, with Shift to the previous one.
 * Enter activates the item SandFly focus moved to; after typing, it goes to the app, so it still
 * sends a message. Alt pressed twice plays or
 * pauses media and answers calls, and Alt + Shift held chooses the next reading control. The rest
 * are the SandFly key with a letter, which work anywhere as well: J chooses the previous reading
 * control, I and K move by it, T and B go to the top and bottom, as on BlackBerry.
 *
 * Arrow key navigation is on in this keymap, for the arrow keys that the Zinwa Q25 trackpad sends,
 * and a press of the trackpad is Enter.
 *
 * Written for Sym as the SandFly key; none uses Meta, so Right Alt and Meta can be the SandFly key
 * too, and Alt, where Alt + Shift held and Alt twice become the SandFly key's.
 */
object KeyboardPhoneKeymap : KeymapDefaults() {
  public override val defaults: Map<Int, KeyCombo> =
    buildMap {
      // Presses that work anywhere.
      put(R.string.keycombo_shortcut_navigate_next_default, KeyCombo.modifierKeys(NONE, true))
      put(R.string.keycombo_shortcut_navigate_previous_default, KeyCombo.modifierKeys(SHIFT, true))
      put(
        R.string.keycombo_shortcut_perform_click,
        KeyCombo(NONE, KeyComboModel.NO_PREFIX_KEY_CODE, KeyEvent.KEYCODE_ENTER, false),
      )
      put(
        R.string.keycombo_shortcut_global_play_pause_media,
        KeyCombo.modifierKeys(ALT, false).withPressType(KeyCombo.PRESSED_TWICE),
      )
      put(
        R.string.keycombo_shortcut_global_scroll_forward_reading_menu,
        KeyCombo.modifierKeys(ALT or SHIFT, false).withPressType(KeyCombo.HELD_DOWN),
      )

      // Moving, with the SandFly key.
      put(R.string.keycombo_shortcut_global_scroll_backward_reading_menu, sandFly(NONE, KeyEvent.KEYCODE_J))
      put(R.string.keycombo_shortcut_global_adjust_reading_setting_next, sandFly(NONE, KeyEvent.KEYCODE_K))
      put(R.string.keycombo_shortcut_global_adjust_reading_settings_previous, sandFly(NONE, KeyEvent.KEYCODE_I))
      put(R.string.keycombo_shortcut_navigate_first, sandFly(NONE, KeyEvent.KEYCODE_T))
      put(R.string.keycombo_shortcut_navigate_last, sandFly(NONE, KeyEvent.KEYCODE_B))
      sandFlyPair(
        R.string.keycombo_shortcut_navigate_next_heading,
        R.string.keycombo_shortcut_navigate_previous_heading,
        KeyEvent.KEYCODE_H,
      )
      sandFlyPair(
        R.string.keycombo_shortcut_navigate_next_link,
        R.string.keycombo_shortcut_navigate_previous_link,
        KeyEvent.KEYCODE_U,
      )
      sandFlyPair(
        R.string.keycombo_shortcut_navigate_next_control,
        R.string.keycombo_shortcut_navigate_previous_control,
        KeyEvent.KEYCODE_F,
      )
      sandFlyPair(
        R.string.keycombo_shortcut_navigate_next_edit_field,
        R.string.keycombo_shortcut_navigate_previous_edit_field,
        KeyEvent.KEYCODE_E,
      )
      sandFlyPair(
        R.string.keycombo_shortcut_navigate_next_graphic,
        R.string.keycombo_shortcut_navigate_previous_graphic,
        KeyEvent.KEYCODE_G,
      )

      // Acting.
      put(R.string.keycombo_shortcut_perform_long_click, sandFly(NONE, KeyEvent.KEYCODE_ENTER))
      put(R.string.keycombo_shortcut_global_back, sandFly(NONE, KeyEvent.KEYCODE_DEL))
      put(R.string.keycombo_shortcut_global_home, sandFly(SHIFT, KeyEvent.KEYCODE_DEL))
      put(R.string.keycombo_shortcut_global_notifications, sandFly(NONE, KeyEvent.KEYCODE_N))
      put(R.string.keycombo_shortcut_global_recents, sandFly(SHIFT, KeyEvent.KEYCODE_N))
      put(R.string.keycombo_shortcut_other_talkback_context_menu, sandFly(NONE, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_other_custom_actions, sandFly(SHIFT, KeyEvent.KEYCODE_M))
      put(R.string.keycombo_shortcut_other_toggle_search, sandFly(NONE, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_other_open_talkback_settings, sandFly(SHIFT, KeyEvent.KEYCODE_S))

      // Reading.
      put(R.string.keycombo_shortcut_other_read_from_cursor_item, sandFly(NONE, KeyEvent.KEYCODE_R))
      put(R.string.keycombo_shortcut_other_read_from_top, sandFly(SHIFT, KeyEvent.KEYCODE_R))
      put(R.string.keycombo_shortcut_global_announce_current_title, sandFly(NONE, KeyEvent.KEYCODE_W))
      put(R.string.keycombo_shortcut_global_announce_current_time_and_date, sandFly(NONE, KeyEvent.KEYCODE_D))
      put(R.string.keycombo_shortcut_global_announce_battery_state, sandFly(SHIFT, KeyEvent.KEYCODE_D))
      put(R.string.keycombo_shortcut_other_repeat_last_spoken_phrase, sandFly(NONE, KeyEvent.KEYCODE_X))
      put(R.string.keycombo_shortcut_other_copy_last_spoken_phrase, sandFly(SHIFT, KeyEvent.KEYCODE_X))
      put(R.string.keycombo_shortcut_global_describe_image, sandFly(NONE, KeyEvent.KEYCODE_A))
      put(R.string.keycombo_shortcut_global_hide_or_show_screen, sandFly(NONE, KeyEvent.KEYCODE_Z))

      // SandFly and the keyboard.
      put(R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog, sandFly(NONE, KeyEvent.KEYCODE_Q))
      put(R.string.keycombo_shortcut_toggle_pass_keys_to_app, sandFly(SHIFT, KeyEvent.KEYCODE_Q))
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

  // Touch can be locked from the keyboard, for people who use only the keyboard. In text fields,
  // with the SandFly keyboard, O searches emoji, C turns editing mode on or off, Y spells the
  // password field (the one letter left free with the SandFly key, and two keys with Right Shift as
  // the SandFly key, where SandFly key + Shift would need both Shift keys), and Alt + Space chooses
  // a symbol, as on BlackBerry keyboards.
  public override val commandDefaults: Map<Int, KeyCombo> =
    mapOf(
      R.string.shortcut_value_toggle_touch_lock to sandFly(NONE, KeyEvent.KEYCODE_L),
      R.string.shortcut_value_search_emoji to sandFly(NONE, KeyEvent.KEYCODE_O),
      R.string.shortcut_value_toggle_editing_mode to sandFly(NONE, KeyEvent.KEYCODE_C),
      R.string.shortcut_value_spell_password_field to sandFly(NONE, KeyEvent.KEYCODE_Y),
      R.string.shortcut_value_choose_symbol to editing(ALT, KeyEvent.KEYCODE_SPACE),
    )

  /** Puts SandFly key + [keyCode] for [next] and with Shift for [previous]. */
  private fun MutableMap<Int, KeyCombo>.sandFlyPair(next: Int, previous: Int, keyCode: Int) {
    put(next, sandFly(NONE, keyCode))
    put(previous, sandFly(SHIFT, keyCode))
  }
}
