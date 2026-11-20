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
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * The keys a keyboard phone keymap can be pressed with: Sym first, then keys that leave Sym to the
 * keyboard where the phone has them.
 */
private val KEYBOARD_PHONE_SANDFLY_KEYS =
  listOf(
    SandFlyKey.SYM,
    SandFlyKey.CTRL,
    SandFlyKey.RIGHT_SHIFT,
    SandFlyKey.FUNCTION,
    SandFlyKey.RIGHT_ALT,
    SandFlyKey.ALT,
    SandFlyKey.META,
  )

/** A set of default keyboard shortcuts, with the SandFly keys it can be used with. */
enum class Keymap(
  @StringRes val valueRes: Int,
  @StringRes val nameRes: Int,
  /** Where the keymap's shortcuts are saved. */
  val persisterPrefix: String,
  /** Where the keymap's SandFly key is saved. */
  val sandFlyKeyPref: String,
  /**
   * The keys that can be its SandFly key, its own first, which its defaults are written for. Only
   * keys that keep every default shortcut are offered.
   */
  val sandFlyKeys: List<SandFlyKey>,
  /** Default shortcuts, or null for SandFly's own, which NewKeyComboModel has. */
  val defaults: KeymapDefaults?,
  /** Whether SandFly key + R or O starts a shortcut of two keys one after the other. */
  val usesSequences: Boolean,
  /**
   * Whether its shortcuts without the SandFly key, such as the arrow keys, work outside browse mode
   * and text fields being edited, as a computer screen reader's do.
   */
  val arrowsNavigate: Boolean = false,
  /**
   * Whether it is for keyboards without Ctrl, Meta or arrow keys: its shortcuts are single keys,
   * which work outside text fields being edited, and the SandFly key with one of them does the same
   * anywhere. Shortcuts can be recorded without the SandFly key.
   */
  val plainKeys: Boolean = false,
) {
  SANDFLY(
    R.string.new_keymap_entry_value,
    R.string.value_new_keymap,
    NewKeyComboModel.PREF_KEY_PREFIX,
    "pref_new_keymap_trigger_modifier",
    listOf(SandFlyKey.META, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
    defaults = null,
    usesSequences = true,
  ),
  VOICEOVER(
    R.string.voiceover_keymap_entry_value,
    R.string.value_voiceover_keymap,
    "voiceover_key_combo_model",
    "pref_voiceover_keymap_trigger_modifier",
    // VoiceOver's own VO key, or Insert.
    listOf(SandFlyKey.CTRL_ALT, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
    VoiceOverKeymap,
    usesSequences = true,
  ),
  NVDA(
    R.string.nvda_keymap_entry_value,
    R.string.value_nvda_keymap,
    "nvda_key_combo_model",
    "pref_nvda_keymap_trigger_modifier",
    listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META),
    NvdaKeymap,
    usesSequences = false,
    arrowsNavigate = true,
  ),
  JAWS(
    R.string.jaws_keymap_entry_value,
    R.string.value_jaws_keymap,
    "jaws_key_combo_model",
    "pref_jaws_keymap_trigger_modifier",
    listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META),
    JawsKeymap,
    usesSequences = false,
    arrowsNavigate = true,
  ),
  ORCA(
    R.string.orca_keymap_entry_value,
    R.string.value_orca_keymap,
    "orca_key_combo_model",
    "pref_orca_keymap_trigger_modifier",
    listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META, SandFlyKey.ALT),
    OrcaKeymap,
    usesSequences = false,
    arrowsNavigate = true,
  ),
  KEYBOARD_PHONE(
    R.string.keyboard_phone_keymap_entry_value,
    R.string.value_keyboard_phone_keymap,
    "keyboard_phone_key_combo_model",
    "pref_keyboard_phone_keymap_trigger_modifier",
    KEYBOARD_PHONE_SANDFLY_KEYS,
    KeyboardPhoneKeymap,
    usesSequences = false,
    arrowsNavigate = true,
    plainKeys = true,
  ),
  KEYBOARD_PHONE_SINGLE_KEYS(
    R.string.keyboard_phone_single_key_keymap_entry_value,
    R.string.value_keyboard_phone_single_key_keymap,
    "keyboard_phone_single_key_combo_model",
    "pref_keyboard_phone_single_keymap_trigger_modifier",
    KEYBOARD_PHONE_SANDFLY_KEYS,
    KeyboardPhoneSingleKeyKeymap,
    usesSequences = false,
    arrowsNavigate = true,
    plainKeys = true,
  );

  /** The SandFly key its defaults are written for. */
  val nativeKey: SandFlyKey
    get() = sandFlyKeys[0]

  /**
   * The SandFly key the user chose for this keymap, or its own: for the keyboard phone keymap, the
   * key that the phone's keyboard has.
   */
  fun sandFlyKey(context: Context): SandFlyKey =
    SandFlyKey.fromValue(
        context,
        SharedPreferencesUtils.getSharedPreferences(context).getString(sandFlyKeyPref, null),
      )
      ?.takeIf { it in sandFlyKeys } ?: defaultSandFlyKey()

  private fun defaultSandFlyKey(): SandFlyKey =
    if (plainKeys) KeyboardPhones.sandFlyKey() else nativeKey

  /** Where shortcuts are saved with [sandFlyKey], so each SandFly key keeps its own changes. */
  fun persisterPrefix(sandFlyKey: SandFlyKey): String =
    if (sandFlyKey == nativeKey) persisterPrefix
    else persisterPrefix + "_" + sandFlyKey.name.lowercase()

  companion object {
    /** The keymap saved as [value]; the old classic keymap is the SandFly keymap. */
    @JvmStatic
    fun of(context: Context, value: String?): Keymap =
      entries.firstOrNull { context.getString(it.valueRes) == value } ?: SANDFLY

    /** The keymap used until one is chosen: the keyboard phone keymap on keyboard phones. */
    @JvmStatic
    fun defaultKeymap(): Keymap = if (KeyboardPhones.isKeyboardPhone()) KEYBOARD_PHONE else SANDFLY
  }
}
