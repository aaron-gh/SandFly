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

/** The default shortcuts of a keymap modelled on another screen reader. */
abstract class KeymapDefaults {
  /** Default shortcuts by the string resource of their command's key; the rest are unassigned. */
  protected abstract val defaults: Map<Int, KeyCombo>

  /** Returns the default shortcut of the command saved under [key], or none. */
  fun defaultKeyCombo(context: Context, key: String): KeyCombo {
    for ((keyRes, combo) in defaults) {
      if (context.getString(keyRes) == key) {
        return combo
      }
    }
    return KeyCombo()
  }

  protected companion object {
    const val NONE = KeyComboModel.NO_MODIFIER
    const val SHIFT = KeyEvent.META_SHIFT_ON
    const val CTRL = KeyEvent.META_CTRL_ON
    const val ALT = KeyEvent.META_ALT_ON
    const val META = KeyEvent.META_META_ON

    /** A shortcut pressed with the SandFly key. */
    fun sandFly(modifiers: Int, keyCode: Int) =
      KeyCombo(modifiers, KeyComboModel.NO_PREFIX_KEY_CODE, keyCode, /* triggerModifierUsed= */ true)

    /** A SandFly key shortcut pressed as two keys one after the other, like SandFly + R, then U. */
    fun sequence(prefixKeyCode: Int, keyCode: Int) =
      KeyCombo(NONE, prefixKeyCode, keyCode, /* triggerModifierUsed= */ true)

    /** A key that works without the SandFly key in browse mode. */
    fun browse(modifiers: Int, keyCode: Int) =
      KeyCombo(modifiers, KeyComboModel.NO_PREFIX_KEY_CODE, keyCode, /* triggerModifierUsed= */ false)

    /** Puts the SandFly key with the volume keys for speech volume, with Shift pitch, Ctrl rate. */
    fun MutableMap<Int, KeyCombo>.putSpeechKeys() {
      put(
        R.string.keycombo_shortcut_global_speech_volume_increase,
        sandFly(NONE, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_volume_decrease,
        sandFly(NONE, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
      put(
        R.string.keycombo_shortcut_global_speech_pitch_increase,
        sandFly(SHIFT, KeyEvent.KEYCODE_VOLUME_UP),
      )
      put(
        R.string.keycombo_shortcut_global_speech_pitch_decrease,
        sandFly(SHIFT, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
      put(R.string.keycombo_shortcut_global_speech_rate_increase, sandFly(CTRL, KeyEvent.KEYCODE_VOLUME_UP))
      put(
        R.string.keycombo_shortcut_global_speech_rate_decrease,
        sandFly(CTRL, KeyEvent.KEYCODE_VOLUME_DOWN),
      )
    }

    /**
     * Puts the reading controls on the SandFly key with Ctrl and the arrows: left and right choose
     * one, up and down move by it or change it, as NVDA's synth settings ring.
     */
    fun MutableMap<Int, KeyCombo>.putReadingControlKeys() {
      put(
        R.string.keycombo_shortcut_global_scroll_forward_reading_menu,
        sandFly(CTRL, KeyEvent.KEYCODE_DPAD_RIGHT),
      )
      put(
        R.string.keycombo_shortcut_global_scroll_backward_reading_menu,
        sandFly(CTRL, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      put(
        R.string.keycombo_shortcut_global_adjust_reading_setting_next,
        sandFly(CTRL, KeyEvent.KEYCODE_DPAD_DOWN),
      )
      put(
        R.string.keycombo_shortcut_global_adjust_reading_settings_previous,
        sandFly(CTRL, KeyEvent.KEYCODE_DPAD_UP),
      )
    }

    /** Puts the caret keys of a desktop browser in browse mode: arrows, and Ctrl + arrows by word. */
    fun MutableMap<Int, KeyCombo>.putCaretKeys() {
      put(R.string.keycombo_shortcut_navigate_next_character, browse(NONE, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(
        R.string.keycombo_shortcut_navigate_previous_character,
        browse(NONE, KeyEvent.KEYCODE_DPAD_LEFT),
      )
      put(R.string.keycombo_shortcut_navigate_next_line, browse(NONE, KeyEvent.KEYCODE_DPAD_DOWN))
      put(R.string.keycombo_shortcut_navigate_previous_line, browse(NONE, KeyEvent.KEYCODE_DPAD_UP))
      put(R.string.keycombo_shortcut_navigate_next_word, browse(CTRL, KeyEvent.KEYCODE_DPAD_RIGHT))
      put(R.string.keycombo_shortcut_navigate_previous_word, browse(CTRL, KeyEvent.KEYCODE_DPAD_LEFT))
    }

    /** Puts the number keys 1 to 6 for headings at those levels, in browse mode. */
    fun MutableMap<Int, KeyCombo>.headingLevels() {
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_1, R.string.keycombo_shortcut_navigate_previous_heading_1, KeyEvent.KEYCODE_1)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_2, R.string.keycombo_shortcut_navigate_previous_heading_2, KeyEvent.KEYCODE_2)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_3, R.string.keycombo_shortcut_navigate_previous_heading_3, KeyEvent.KEYCODE_3)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_4, R.string.keycombo_shortcut_navigate_previous_heading_4, KeyEvent.KEYCODE_4)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_5, R.string.keycombo_shortcut_navigate_previous_heading_5, KeyEvent.KEYCODE_5)
      browsePair(R.string.keycombo_shortcut_navigate_next_heading_6, R.string.keycombo_shortcut_navigate_previous_heading_6, KeyEvent.KEYCODE_6)
    }

    /** Puts [keyCode] for [next] and Shift + [keyCode] for [previous], in browse mode. */
    fun MutableMap<Int, KeyCombo>.browsePair(next: Int, previous: Int, keyCode: Int) {
      put(next, browse(NONE, keyCode))
      put(previous, browse(SHIFT, keyCode))
    }
  }
}
