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
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R

/**
 * A key that can be the SandFly key, which keymap shortcuts are pressed with. Meta, Alt, Ctrl + Alt
 * and Ctrl + Meta are modifiers to Android; Insert and Numpad Insert are ordinary keys that SandFly
 * takes and treats as a modifier while they're held.
 */
enum class SandFlyKey(
  @StringRes val valueRes: Int,
  @StringRes val nameRes: Int,
  /**
   * The modifier bits of this key, or [KeyComboModel.HELD_KEY_MODIFIER] for a key that Android
   * doesn't treat as one.
   */
  val modifiers: Int,
  /** The key code of a held key, or [KeyEvent.KEYCODE_UNKNOWN] for modifiers. */
  val keyCode: Int,
) {
  META(
    R.string.trigger_modifier_meta_entry_value,
    R.string.keycombo_key_modifier_meta,
    KeyEvent.META_META_ON,
    KeyEvent.KEYCODE_UNKNOWN,
  ),
  ALT(
    R.string.trigger_modifier_alt_entry_value,
    R.string.keycombo_key_modifier_alt,
    KeyEvent.META_ALT_ON,
    KeyEvent.KEYCODE_UNKNOWN,
  ),
  CTRL_ALT(
    R.string.trigger_modifier_ctrl_alt_entry_value,
    R.string.keycombo_key_modifier_ctrl_alt,
    KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON,
    KeyEvent.KEYCODE_UNKNOWN,
  ),
  CTRL_META(
    R.string.trigger_modifier_ctrl_meta_entry_value,
    R.string.keycombo_key_modifier_ctrl_meta,
    KeyEvent.META_CTRL_ON or KeyEvent.META_META_ON,
    KeyEvent.KEYCODE_UNKNOWN,
  ),
  INSERT(
    R.string.trigger_modifier_insert_entry_value,
    R.string.keycombo_key_modifier_insert,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_INSERT,
  ),
  NUMPAD_INSERT(
    R.string.trigger_modifier_numpad_insert_entry_value,
    R.string.keycombo_key_modifier_numpad_insert,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_NUMPAD_0,
  );

  /** Whether this is one key, which can be pressed twice, as two held together can't. */
  val canBePressedTwice: Boolean
    get() = this != CTRL_ALT && this != CTRL_META

  /** Whether [keyCode] is a key of this SandFly key, such as left or right Alt for Alt. */
  fun hasKey(keyCode: Int): Boolean =
    when (this) {
      META -> keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT
      ALT -> keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
      CTRL_ALT ->
        keyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
          keyCode == KeyEvent.KEYCODE_CTRL_RIGHT ||
          keyCode == KeyEvent.KEYCODE_ALT_LEFT ||
          keyCode == KeyEvent.KEYCODE_ALT_RIGHT
      CTRL_META ->
        keyCode == KeyEvent.KEYCODE_CTRL_LEFT ||
          keyCode == KeyEvent.KEYCODE_CTRL_RIGHT ||
          keyCode == KeyEvent.KEYCODE_META_LEFT ||
          keyCode == KeyEvent.KEYCODE_META_RIGHT
      INSERT,
      NUMPAD_INSERT -> keyCode == this.keyCode
    }

  /** Whether this is an ordinary key held as a modifier. */
  val isHeldKey: Boolean
    get() = keyCode != KeyEvent.KEYCODE_UNKNOWN

  /** Whether [event] presses or releases this key, where it's a held key. */
  fun isKey(event: KeyEvent): Boolean =
    isHeldKey &&
      event.keyCode == keyCode &&
      // Numpad 0 is Insert only with Num Lock off.
      (keyCode != KeyEvent.KEYCODE_NUMPAD_0 || !event.isNumLockOn)

  companion object {
    /** The SandFly key saved as [value], or null. */
    @JvmStatic
    fun fromValue(context: Context, value: String?): SandFlyKey? =
      entries.firstOrNull { context.getString(it.valueRes) == value }

    /**
     * Returns [combo], written for [native] as the SandFly key, as it is pressed with [chosen].
     * Where it uses the chosen key with other keys, another modifier takes the chosen key's place
     * there: the native key, so Ctrl + Alt + Meta + Right stays the same keys, or, where the native
     * key is an ordinary key such as Insert or shares a key with the chosen one, the first of Meta,
     * Alt, Ctrl and Shift that the shortcut and the chosen key leave free. Browse mode keys that press all of the chosen key become shortcuts
     * with it, as pressing them presses the SandFly key. Defaults that become the same keys are
     * sorted out by the caller.
     */
    @JvmStatic
    fun adapt(combo: KeyCombo, native: SandFlyKey, chosen: SandFlyKey): KeyCombo {
      val chosenModifiers = chosen.modifiers
      if (
        combo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong() ||
          chosenModifiers == native.modifiers
      ) {
        return combo
      }
      val modifiers = combo.modifiers
      if (!combo.isTriggerModifierUsed) {
        if (modifiers and chosenModifiers != chosenModifiers) {
          return combo
        }
        return KeyCombo(
          modifiers and chosenModifiers.inv(),
          combo.prefixKeyCode,
          combo.keyCode,
          /* triggerModifierUsed= */ true,
        )
      }
      if (modifiers and chosenModifiers == 0) {
        return combo
      }
      val others = modifiers and chosenModifiers.inv()
      val taken = chosenModifiers or others
      val replacement =
        if (!native.isHeldKey && native.modifiers and taken == 0) {
          native.modifiers
        } else {
          REPLACEMENTS.firstOrNull { it and taken == 0 } ?: return KeyCombo()
        }
      return KeyCombo(
        others or replacement,
        combo.prefixKeyCode,
        combo.keyCode,
        /* triggerModifierUsed= */ true,
      )
    }

    private val REPLACEMENTS =
      listOf(
        KeyEvent.META_META_ON,
        KeyEvent.META_ALT_ON,
        KeyEvent.META_CTRL_ON,
        KeyEvent.META_SHIFT_ON,
      )
  }
}
