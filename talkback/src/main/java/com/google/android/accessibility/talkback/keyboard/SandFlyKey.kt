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
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * A key that can be the SandFly key, which keymap shortcuts are pressed with. Meta and Ctrl + Alt
 * are modifiers to Android; Insert and Numpad Insert are ordinary keys that SandFly takes and treats
 * as a modifier while they're held.
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
  CTRL_ALT(
    R.string.trigger_modifier_ctrl_alt_entry_value,
    R.string.keycombo_key_modifier_ctrl_alt,
    KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON,
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
    /** The SandFly key the user chose; every keymap uses it. Meta when none was chosen. */
    @JvmStatic
    fun chosen(context: Context): SandFlyKey {
      val value =
        SharedPreferencesUtils.getSharedPreferences(context)
          .getString(context.getString(R.string.pref_sandfly_key), null)
      return entries.firstOrNull { context.getString(it.valueRes) == value } ?: META
    }

    /**
     * Returns [combo], written for [native] as the SandFly key, as it is pressed with [chosen].
     * Where it uses the chosen key with other keys, [native] takes the chosen key's place there,
     * so Ctrl + Alt + Meta + Right stays the same keys. Shortcuts that can't be pressed that way
     * become unassigned: an ordinary key such as Insert can't be held with another SandFly key,
     * and browse mode keys that press all of the SandFly key would be taken as its shortcuts.
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
        return if (modifiers and chosenModifiers == chosenModifiers) KeyCombo() else combo
      }
      if (modifiers and chosenModifiers == 0) {
        return combo
      }
      if (native.isHeldKey) {
        return KeyCombo()
      }
      return KeyCombo(
        (modifiers and chosenModifiers.inv()) or native.modifiers,
        combo.prefixKeyCode,
        combo.keyCode,
        /* triggerModifierUsed= */ true,
      )
    }
  }
}
