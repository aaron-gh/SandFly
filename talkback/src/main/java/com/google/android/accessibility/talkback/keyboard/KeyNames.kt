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

/**
 * The names of physical keys in the language of the user, for the keyboard shortcuts and keyboard
 * practice. Android names keys in English only, such as "PERIOD" and "SEARCH". Letters, digits and
 * function keys are the same in every language, so they have no name here.
 */
object KeyNames {
  private val NAMES: Map<Int, Int> =
    mapOf(
      KeyEvent.KEYCODE_ENTER to R.string.keycombo_key_enter,
      KeyEvent.KEYCODE_SPACE to R.string.keycombo_key_space,
      KeyEvent.KEYCODE_ESCAPE to R.string.keycombo_key_escape,
      KeyEvent.KEYCODE_TAB to R.string.keycombo_key_tab,
      KeyEvent.KEYCODE_PAGE_UP to R.string.keycombo_key_page_up,
      KeyEvent.KEYCODE_PAGE_DOWN to R.string.keycombo_key_page_down,
      KeyEvent.KEYCODE_MOVE_HOME to R.string.keycombo_key_home,
      KeyEvent.KEYCODE_MOVE_END to R.string.keycombo_key_end,
      KeyEvent.KEYCODE_INSERT to R.string.keycombo_key_insert,
      KeyEvent.KEYCODE_FORWARD_DEL to R.string.keycombo_key_delete,
      KeyEvent.KEYCODE_MENU to R.string.keycombo_key_menu,
      KeyEvent.KEYCODE_SEARCH to R.string.keycombo_key_search,
      KeyEvent.KEYCODE_GRAVE to R.string.keycombo_key_grave,
      KeyEvent.KEYCODE_MINUS to R.string.keycombo_key_minus,
      KeyEvent.KEYCODE_EQUALS to R.string.keycombo_key_equals,
      KeyEvent.KEYCODE_LEFT_BRACKET to R.string.keycombo_key_left_bracket,
      KeyEvent.KEYCODE_RIGHT_BRACKET to R.string.keycombo_key_right_bracket,
      KeyEvent.KEYCODE_BACKSLASH to R.string.keycombo_key_backslash,
      KeyEvent.KEYCODE_SEMICOLON to R.string.keycombo_key_semicolon,
      KeyEvent.KEYCODE_APOSTROPHE to R.string.keycombo_key_apostrophe,
      KeyEvent.KEYCODE_COMMA to R.string.keycombo_key_comma,
      KeyEvent.KEYCODE_PERIOD to R.string.keycombo_key_period,
      KeyEvent.KEYCODE_SLASH to R.string.keycombo_key_slash,
      KeyEvent.KEYCODE_PLUS to R.string.keycombo_key_plus,
      KeyEvent.KEYCODE_AT to R.string.keycombo_key_at,
      KeyEvent.KEYCODE_STAR to R.string.keycombo_key_star,
      KeyEvent.KEYCODE_POUND to R.string.keycombo_key_pound,
      KeyEvent.KEYCODE_CAPS_LOCK to R.string.keycombo_key_caps_lock,
      KeyEvent.KEYCODE_SCROLL_LOCK to R.string.keycombo_key_scroll_lock,
      KeyEvent.KEYCODE_NUM_LOCK to R.string.keycombo_key_num_lock,
      KeyEvent.KEYCODE_BREAK to R.string.keycombo_key_pause,
      KeyEvent.KEYCODE_SYSRQ to R.string.keycombo_key_print_screen,
      KeyEvent.KEYCODE_NUMPAD_ADD to R.string.keycombo_key_numpad_add,
      KeyEvent.KEYCODE_NUMPAD_SUBTRACT to R.string.keycombo_key_numpad_subtract,
      KeyEvent.KEYCODE_NUMPAD_MULTIPLY to R.string.keycombo_key_numpad_multiply,
      KeyEvent.KEYCODE_NUMPAD_DIVIDE to R.string.keycombo_key_numpad_divide,
      KeyEvent.KEYCODE_NUMPAD_DOT to R.string.keycombo_key_numpad_dot,
      KeyEvent.KEYCODE_NUMPAD_COMMA to R.string.keycombo_key_numpad_comma,
      KeyEvent.KEYCODE_NUMPAD_EQUALS to R.string.keycombo_key_numpad_equals,
      KeyEvent.KEYCODE_NUMPAD_ENTER to R.string.keycombo_key_numpad_enter,
      KeyEvent.KEYCODE_VOLUME_UP to R.string.keycombo_key_volume_up,
      KeyEvent.KEYCODE_VOLUME_DOWN to R.string.keycombo_key_volume_down,
      KeyEvent.KEYCODE_VOLUME_MUTE to R.string.keycombo_key_volume_mute,
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to R.string.keycombo_key_media_play_pause,
      KeyEvent.KEYCODE_MEDIA_NEXT to R.string.keycombo_key_media_next,
      KeyEvent.KEYCODE_MEDIA_PREVIOUS to R.string.keycombo_key_media_previous,
      KeyEvent.KEYCODE_MEDIA_STOP to R.string.keycombo_key_media_stop,
      KeyEvent.KEYCODE_BACK to R.string.keycombo_key_back,
    )

  /** Returns the name of [keyCode] in the language of the user, or null if it has none here. */
  @JvmStatic
  fun of(context: Context, keyCode: Int): String? {
    if (keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9) {
      return context.getString(R.string.keycombo_key_numpad, (keyCode - KeyEvent.KEYCODE_NUMPAD_0).toString())
    }
    return NAMES[keyCode]?.let { context.getString(it) }
  }

  /** Returns the name of [keyCode], or Android's own name for it, in English. */
  @JvmStatic
  fun orAndroidName(context: Context, keyCode: Int): String =
    of(context, keyCode) ?: KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')
}
