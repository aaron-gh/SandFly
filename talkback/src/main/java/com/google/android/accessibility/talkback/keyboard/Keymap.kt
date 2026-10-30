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
  ),
  JAWS(
    R.string.jaws_keymap_entry_value,
    R.string.value_jaws_keymap,
    "jaws_key_combo_model",
    "pref_jaws_keymap_trigger_modifier",
    listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META),
    JawsKeymap,
    usesSequences = false,
  ),
  ORCA(
    R.string.orca_keymap_entry_value,
    R.string.value_orca_keymap,
    "orca_key_combo_model",
    "pref_orca_keymap_trigger_modifier",
    listOf(SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT, SandFlyKey.META, SandFlyKey.ALT),
    OrcaKeymap,
    usesSequences = false,
  );

  /** The SandFly key its defaults are written for. */
  val nativeKey: SandFlyKey
    get() = sandFlyKeys[0]

  /** The SandFly key the user chose for this keymap, or its own. */
  fun sandFlyKey(context: Context): SandFlyKey =
    SandFlyKey.fromValue(
        context,
        SharedPreferencesUtils.getSharedPreferences(context).getString(sandFlyKeyPref, null),
      )
      ?.takeIf { it in sandFlyKeys } ?: nativeKey

  /** Where shortcuts are saved with [sandFlyKey], so each SandFly key keeps its own changes. */
  fun persisterPrefix(sandFlyKey: SandFlyKey): String =
    if (sandFlyKey == nativeKey) persisterPrefix
    else persisterPrefix + "_" + sandFlyKey.name.lowercase()

  companion object {
    /** The keymap saved as [value]; the old classic keymap is the SandFly keymap. */
    @JvmStatic
    fun of(context: Context, value: String?): Keymap =
      entries.firstOrNull { context.getString(it.valueRes) == value } ?: SANDFLY
  }
}
