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

/** A set of default keyboard shortcuts. */
enum class Keymap(
  @StringRes val valueRes: Int,
  @StringRes val nameRes: Int,
  /** Where the keymap's shortcuts are saved. */
  val persisterPrefix: String,
  /**
   * The SandFly key the keymap's defaults are written for. With another SandFly key, shortcuts
   * that use the chosen key with other keys use this one there instead.
   */
  val nativeKey: SandFlyKey,
  /** Default shortcuts, or null for SandFly's own, which NewKeyComboModel has. */
  val defaults: KeymapDefaults?,
  /** Whether SandFly key + R or O starts a shortcut of two keys one after the other. */
  val usesSequences: Boolean,
  /** Where this keymap kept its own SandFly key before there was one for all keymaps. */
  val oldSandFlyKeyPref: String?,
) {
  SANDFLY(
    R.string.new_keymap_entry_value,
    R.string.value_new_keymap,
    NewKeyComboModel.PREF_KEY_PREFIX,
    SandFlyKey.META,
    defaults = null,
    usesSequences = true,
    oldSandFlyKeyPref = null,
  ),
  VOICEOVER(
    R.string.voiceover_keymap_entry_value,
    R.string.value_voiceover_keymap,
    "voiceover_key_combo_model",
    SandFlyKey.CTRL_ALT,
    VoiceOverKeymap,
    usesSequences = true,
    "pref_voiceover_keymap_trigger_modifier",
  ),
  NVDA(
    R.string.nvda_keymap_entry_value,
    R.string.value_nvda_keymap,
    "nvda_key_combo_model",
    SandFlyKey.INSERT,
    NvdaKeymap,
    usesSequences = false,
    "pref_nvda_keymap_trigger_modifier",
  ),
  JAWS(
    R.string.jaws_keymap_entry_value,
    R.string.value_jaws_keymap,
    "jaws_key_combo_model",
    SandFlyKey.INSERT,
    JawsKeymap,
    usesSequences = false,
    "pref_jaws_keymap_trigger_modifier",
  ),
  ORCA(
    R.string.orca_keymap_entry_value,
    R.string.value_orca_keymap,
    "orca_key_combo_model",
    SandFlyKey.INSERT,
    OrcaKeymap,
    usesSequences = false,
    "pref_orca_keymap_trigger_modifier",
  );

  /** Where shortcuts are saved with [sandFlyKey], so each SandFly key keeps its own changes. */
  fun persisterPrefix(sandFlyKey: SandFlyKey): String =
    if (sandFlyKey == nativeKey) persisterPrefix
    else persisterPrefix + "_" + sandFlyKey.name.lowercase()

  companion object {
    /** The keymap saved as [value]; the old Default and classic keymaps are the SandFly keymap. */
    @JvmStatic
    fun of(context: Context, value: String?): Keymap =
      entries.firstOrNull { context.getString(it.valueRes) == value } ?: SANDFLY
  }
}
