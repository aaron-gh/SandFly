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
import androidx.annotation.XmlRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** A set of default keyboard shortcuts, with the keys that can be its SandFly key. */
enum class Keymap(
  @StringRes val valueRes: Int,
  @StringRes val nameRes: Int,
  /** Where the keymap's shortcuts are saved. */
  val persisterPrefix: String,
  @StringRes val sandFlyKeyPrefRes: Int,
  @XmlRes val preferencesRes: Int,
  /** The keys that can be the SandFly key, the default first. */
  val sandFlyKeys: List<SandFlyKey>,
  /** Default shortcuts, or null for SandFly's own, which NewKeyComboModel has. */
  val defaults: KeymapDefaults?,
  /** Whether SandFly key + R or O starts a shortcut of two keys one after the other. */
  val usesSequences: Boolean,
) {
  SANDFLY(
    R.string.new_keymap_entry_value,
    R.string.value_new_keymap,
    NewKeyComboModel.PREF_KEY_PREFIX,
    R.string.pref_new_keymap_trigger_modifier_key,
    R.xml.new_key_combo_preferences,
    // Its shortcuts use Shift, Ctrl and Alt.
    listOf(SandFlyKey.META, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
    defaults = null,
    usesSequences = true,
  ),
  VOICEOVER(
    R.string.voiceover_keymap_entry_value,
    R.string.value_voiceover_keymap,
    "voiceover_key_combo_model",
    R.string.pref_voiceover_keymap_trigger_modifier_key,
    R.xml.voiceover_key_combo_preferences,
    // Its shortcuts use Shift and Meta, as VoiceOver's use Shift and Command.
    listOf(SandFlyKey.CTRL_ALT, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
    VoiceOverKeymap,
    usesSequences = true,
  ),
  NVDA(
    R.string.nvda_keymap_entry_value,
    R.string.value_nvda_keymap,
    "nvda_key_combo_model",
    R.string.pref_nvda_keymap_trigger_modifier_key,
    R.xml.nvda_key_combo_preferences,
    // Their shortcuts use Shift, Ctrl and Alt.
    SCREEN_READER_KEYS,
    NvdaKeymap,
    usesSequences = false,
  ),
  JAWS(
    R.string.jaws_keymap_entry_value,
    R.string.value_jaws_keymap,
    "jaws_key_combo_model",
    R.string.pref_jaws_keymap_trigger_modifier_key,
    R.xml.jaws_key_combo_preferences,
    SCREEN_READER_KEYS,
    JawsKeymap,
    usesSequences = false,
  ),
  ORCA(
    R.string.orca_keymap_entry_value,
    R.string.value_orca_keymap,
    "orca_key_combo_model",
    R.string.pref_orca_keymap_trigger_modifier_key,
    R.xml.orca_key_combo_preferences,
    SCREEN_READER_KEYS,
    OrcaKeymap,
    usesSequences = false,
  );

  /** The SandFly key the user chose for this keymap. */
  fun sandFlyKey(context: Context): SandFlyKey {
    val value =
      SharedPreferencesUtils.getSharedPreferences(context)
        .getString(context.getString(sandFlyKeyPrefRes), null)
    return sandFlyKeys.firstOrNull { context.getString(it.valueRes) == value } ?: sandFlyKeys[0]
  }

  companion object {
    /** The keymap saved as [value]; the old Default and classic keymaps are the SandFly keymap. */
    @JvmStatic
    fun of(context: Context, value: String?): Keymap =
      entries.firstOrNull { context.getString(it.valueRes) == value } ?: SANDFLY
  }
}

/** The SandFly keys of the NVDA, JAWS and Orca keymaps: Insert, as theirs is. */
private val SCREEN_READER_KEYS =
  listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META)
