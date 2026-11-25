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
 * The default shortcuts of the BlackBerry keymap: the BlackBerry 10 Screen Reader's keys for an
 * external keyboard (OS 10.3), with the SandFly key for Ctrl, on keyboard phones such as the Zinwa
 * Q25 and the BlackBerry KEYone and KEY2, which have no Ctrl key.
 *
 * - SandFly key + A reads from the top, + Shift + A from the current item.
 * - SandFly key + Enter speaks the item in focus.
 * - SandFly key + S stops speech, + Shift + S hides or shows the screen (Dark Screen Mode).
 * - SandFly key + . says where the focus is.
 * - The arrow keys, such as those a trackpad sends, move by the text chunk with right
 *   and left, and change the size of the text chunk with up and down (arrow key navigation).
 *
 * - SandFly key + Shift + T starts Quick Tasks.
 *
 * The rest are the keyboard phone keymap's. Its shortcuts on keys that BlackBerry uses move: long
 * press to SandFly key + Shift + Enter, search to SandFly key + Shift + Space, SandFly settings to
 * SandFly key + Shift + O, describe image to SandFly key + Z, which hid the screen there.
 */
object BlackBerryKeymap : KeymapDefaults() {
  override val defaults: Map<Int, KeyCombo> =
    buildMap {
      putAll(KeyboardPhoneKeymap.defaults)

      put(R.string.keycombo_shortcut_other_read_from_top, sandFly(NONE, KeyEvent.KEYCODE_A))
      put(R.string.keycombo_shortcut_other_read_from_cursor_item, sandFly(SHIFT, KeyEvent.KEYCODE_A))
      put(R.string.keycombo_shortcut_global_refocus_current_node, sandFly(NONE, KeyEvent.KEYCODE_ENTER))
      put(R.string.keycombo_shortcut_global_hide_or_show_screen, sandFly(SHIFT, KeyEvent.KEYCODE_S))
      put(R.string.keycombo_shortcut_global_announce_current_title, sandFly(NONE, KeyEvent.KEYCODE_PERIOD))

      put(R.string.keycombo_shortcut_perform_long_click, sandFly(SHIFT, KeyEvent.KEYCODE_ENTER))
      put(R.string.keycombo_shortcut_other_toggle_search, sandFly(SHIFT, KeyEvent.KEYCODE_SPACE))
      put(R.string.keycombo_shortcut_other_open_talkback_settings, sandFly(SHIFT, KeyEvent.KEYCODE_O))
      put(R.string.keycombo_shortcut_global_describe_image, sandFly(NONE, KeyEvent.KEYCODE_Z))
    }

  // S stops speech, as Ctrl + S does on BlackBerry 10; Quick Tasks had no key there.
  override val commandDefaults: Map<Int, KeyCombo> =
    KeyboardPhoneKeymap.commandDefaults +
      mapOf(
        R.string.shortcut_value_pause_or_resume_feedback to sandFly(NONE, KeyEvent.KEYCODE_S),
        R.string.shortcut_value_quick_tasks to sandFly(SHIFT, KeyEvent.KEYCODE_T),
      )
}
