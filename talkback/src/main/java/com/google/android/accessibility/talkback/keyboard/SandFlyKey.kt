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
 * Ctrl + Meta and Ctrl are modifiers to Android; Insert, Numpad Insert, Sym, Right Alt, Right Shift
 * and Fn are keys that SandFly takes and treats as a modifier while they're held. Right Alt is the
 * Sym key of keyboard phones such as the Zinwa Q25, whose Alt key is left Alt. Ctrl, Right Shift and
 * Fn leave Sym to the keyboard on keyboard phones that have them: Ctrl where Fn is set to Ctrl, as
 * on the Unihertz Titan 2, Fn as the BlackBerry KEY2's Speed key.
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
  ),
  SYM(
    R.string.trigger_modifier_sym_entry_value,
    R.string.keycombo_key_modifier_sym,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_SYM,
  ),
  RIGHT_ALT(
    R.string.trigger_modifier_right_alt_entry_value,
    R.string.keycombo_key_modifier_right_alt,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_ALT_RIGHT,
  ),
  CTRL(
    R.string.trigger_modifier_ctrl_entry_value,
    R.string.keycombo_key_modifier_ctrl,
    KeyEvent.META_CTRL_ON,
    KeyEvent.KEYCODE_UNKNOWN,
  ),
  RIGHT_SHIFT(
    R.string.trigger_modifier_right_shift_entry_value,
    R.string.keycombo_key_modifier_right_shift,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_SHIFT_RIGHT,
  ),
  FUNCTION(
    R.string.trigger_modifier_function_entry_value,
    R.string.keycombo_key_modifier_function,
    KeyComboModel.HELD_KEY_MODIFIER,
    KeyEvent.KEYCODE_FUNCTION,
  );

  /** Whether this is one key, which can be pressed twice, as two held together can't. */
  val canBePressedTwice: Boolean
    get() = this != CTRL_ALT && this != CTRL_META

  /** Whether [keyCode] is a key of this SandFly key, such as left or right Alt for Alt. */
  fun hasKey(keyCode: Int): Boolean =
    when (this) {
      META -> keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT
      ALT -> keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
      CTRL -> keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
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
      NUMPAD_INSERT,
      SYM,
      RIGHT_ALT,
      RIGHT_SHIFT,
      FUNCTION -> keyCode == this.keyCode
    }

  /**
   * Whether Alt pressed with letters types digits and symbols on the keyboards this key is used on,
   * so that the SandFly key with a letter is typing in a text field being edited.
   */
  val typesWithLetters: Boolean
    get() = this == ALT

  /**
   * Removes from [modifiers] what Android adds while this held key is down: Right Alt held alone
   * adds Alt to the keys pressed with it, Right Shift held alone Shift, Sym Sym and Fn Fn.
   */
  fun withoutOwnModifiers(modifiers: Int, metaState: Int): Int =
    when {
      this == RIGHT_ALT && metaState and KeyEvent.META_ALT_LEFT_ON == 0 ->
        modifiers and KeyEvent.META_ALT_ON.inv()
      this == RIGHT_SHIFT && metaState and KeyEvent.META_SHIFT_LEFT_ON == 0 ->
        modifiers and KeyEvent.META_SHIFT_ON.inv()
      this == SYM -> modifiers and KeyEvent.META_SYM_ON.inv()
      this == FUNCTION -> modifiers and KeyEvent.META_FUNCTION_ON.inv()
      else -> modifiers
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
