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
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** A keymap the user made: any shortcuts, with or without the SandFly key. */
data class CustomKeymap(val id: String, val name: String, val usesSequences: Boolean) {
  /** The keymap preference's value for this keymap. */
  val value: String
    get() = VALUE_PREFIX + id

  /** Where its shortcuts are saved. */
  val persisterPrefix: String
    get() = PREFIX + id

  /** Where its shortcuts as it was made are saved, which resetting it goes back to. */
  val originalPersisterPrefix: String
    get() = PREFIX + id + "_original"

  /** Where its SandFly key, which can be any, is saved. */
  val sandFlyKeyPref: String
    get() = SANDFLY_KEY + id

  /** The SandFly key chosen for this keymap, or Meta. */
  fun sandFlyKey(context: Context): SandFlyKey =
    SandFlyKey.fromValue(
      context,
      SharedPreferencesUtils.getSharedPreferences(context).getString(sandFlyKeyPref, null),
    ) ?: SandFlyKey.META

  companion object {
    const val VALUE_PREFIX = "custom_keymap:"
    private const val PREFIX = "custom_key_combo_model_"
    const val SANDFLY_KEY = "pref_custom_keymap_sandfly_key_"
  }
}

/** The keymaps the user made, kept in shared preferences. */
object CustomKeymaps {
  private const val IDS = "pref_custom_keymap_ids"
  private const val NAME = "pref_custom_keymap_name_"
  private const val SEQUENCES = "pref_custom_keymap_sequences_"

  private fun prefs(context: Context): SharedPreferences =
    SharedPreferencesUtils.getSharedPreferences(context)

  private fun ids(context: Context): List<String> =
    prefs(context).getString(IDS, "").orEmpty().split(',').filter { it.isNotEmpty() }

  @JvmStatic
  fun all(context: Context): List<CustomKeymap> = ids(context).mapNotNull { find(context, it) }

  private fun find(context: Context, id: String): CustomKeymap? {
    val prefs = prefs(context)
    val name = prefs.getString(NAME + id, null) ?: return null
    return CustomKeymap(id, name, prefs.getBoolean(SEQUENCES + id, false))
  }

  /** The custom keymap the keymap preference's [value] names, or null. */
  @JvmStatic
  fun of(context: Context, value: String?): CustomKeymap? {
    if (value == null || !value.startsWith(CustomKeymap.VALUE_PREFIX)) return null
    val id = value.removePrefix(CustomKeymap.VALUE_PREFIX)
    return if (id in ids(context)) find(context, id) else null
  }

  /**
   * Makes a keymap named [name] with [shortcuts], by command key, pressed with [sandFlyKey], which
   * it also goes back to when reset. Returns it.
   */
  @JvmStatic
  fun create(
    context: Context,
    name: String,
    usesSequences: Boolean,
    sandFlyKey: SandFlyKey,
    shortcuts: Map<String, KeyCombo>,
  ): CustomKeymap {
    val ids = ids(context)
    val id = ((ids.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
    val keymap = CustomKeymap(id, name, usesSequences)
    val current = KeyComboPersister(context, keymap.persisterPrefix)
    val original = KeyComboPersister(context, keymap.originalPersisterPrefix)
    for ((key, keyCombo) in shortcuts) {
      current.saveKeyCombo(key, keyCombo)
      original.saveKeyCombo(key, keyCombo)
    }
    prefs(context).edit {
      putString(NAME + id, name)
      putBoolean(SEQUENCES + id, usesSequences)
      putString(keymap.sandFlyKeyPref, context.getString(sandFlyKey.valueRes))
      putString(IDS, (ids + id).joinToString(","))
    }
    return keymap
  }

  @JvmStatic
  fun rename(context: Context, keymap: CustomKeymap, name: String) {
    prefs(context).edit { putString(NAME + keymap.id, name) }
  }

  /** Forgets [keymap] and its shortcuts. */
  @JvmStatic
  fun delete(context: Context, keymap: CustomKeymap) {
    for (prefix in listOf(keymap.persisterPrefix, keymap.originalPersisterPrefix)) {
      val persister = KeyComboPersister(context, prefix)
      for (key in persister.keysStartingWith("")) {
        persister.remove(key)
      }
    }
    val remaining = ids(context) - keymap.id
    prefs(context).edit {
      remove(NAME + keymap.id)
      remove(SEQUENCES + keymap.id)
      remove(keymap.sandFlyKeyPref)
      putString(IDS, remaining.joinToString(","))
    }
  }
}
