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

package com.google.android.accessibility.talkback.commands

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav
import com.google.android.accessibility.talkback.keyboard.ArrowKeys
import com.google.android.accessibility.talkback.keyboard.KeyComboManager
import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import com.google.android.accessibility.talkback.preference.TalkBackPreferenceFilter
import com.google.android.accessibility.talkback.preference.base.GestureListPreference
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** Which gestures and keyboard shortcuts run each command, for the settings screens. */
object CommandBindings {

  /** A gesture that can run a command: its preference key in the current gesture set. */
  class GestureSlot(val key: String, val name: CharSequence, private val defaultValue: String) {
    /** Returns the value of the command this gesture runs. */
    fun value(context: Context, prefs: SharedPreferences): String {
      val value = prefs.getString(key, defaultValue) ?: defaultValue
      // Older values that the gesture list shows as other commands.
      return when (value) {
        context.getString(R.string.shortcut_value_local_breakout) ->
          context.getString(R.string.shortcut_value_talkback_breakout)
        context.getString(R.string.shortcut_value_editing) ->
          context.getString(R.string.shortcut_value_show_custom_actions)
        else -> value
      }
    }
  }

  /**
   * Returns the gestures that can be assigned on this device, in the order of the Gestures screen,
   * read from that screen's own definition: [screen] is the inflated Gestures screen.
   */
  @JvmStatic
  fun gestures(context: Context, screen: PreferenceGroup): List<GestureSlot> {
    TalkBackPreferenceFilter(context).filterPreferences(screen)
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val gestureSet =
      if (
        GestureShortcutMapping.isGestureSetEnabled(
          context,
          prefs,
          R.string.pref_multiple_gesture_set_key,
          R.bool.pref_multiple_gesture_set_default,
        )
      ) {
        SharedPreferencesUtils.getIntFromStringPref(
          prefs,
          context.resources,
          R.string.pref_gesture_set_key,
          R.string.pref_gesture_set_value_default,
        )
      } else {
        0
      }
    // Two-finger swipes scroll on their own while multi-finger gestures work.
    val reserved =
      if (FeatureSupport.isMultiFingerGestureSupported()) {
        context.resources.getStringArray(R.array.pref_2finger_pass_through_shortcut_keys).toSet()
      } else {
        emptySet()
      }
    return listPreferences(screen)
      .filter { it.key !in reserved }
      .map {
        GestureSlot(
          GestureShortcutMapping.getPrefKeyWithGestureSet(it.key, gestureSet),
          it.title ?: it.key,
          it.defaultValue,
        )
      }
  }

  private fun listPreferences(group: PreferenceGroup): List<GestureListPreference> =
    (0 until group.preferenceCount).flatMap { index ->
      when (val preference: Preference = group.getPreference(index)) {
        is GestureListPreference -> listOf(preference)
        is PreferenceGroup -> listPreferences(preference)
        else -> emptyList()
      }
    }

  /** Returns the arrow keys of arrow key navigation, each of which runs a command. */
  @JvmStatic
  fun arrowKeys(context: Context): List<GestureSlot> {
    ArrowKeyNav.migrate(SharedPreferencesUtils.getSharedPreferences(context))
    val manager = keyComboManager(context)
    return ArrowKeys.entries.map {
      GestureSlot(
        ArrowKeyNav.prefKey(it),
        manager.getArrowKeysStringRepresentation(it.keys),
        it.defaultCommand,
      )
    }
  }

  /** Returns the key combo manager of the running screen reader, or one for settings. */
  @JvmStatic
  fun keyComboManager(context: Context): KeyComboManager =
    TalkBackService.getInstance()?.keyComboManager ?: KeyComboManager.create(context)

  /**
   * Returns the key under which [model] keeps the keyboard shortcut of [command]: the command's
   * keyboard action's own key when this keymap has it, otherwise a key for the command.
   */
  @JvmStatic
  fun keyboardKey(context: Context, model: KeyComboModel, command: Commands.Command): String {
    val actionKey = command.keyboardAction?.getKey(context.resources)
    return if (!actionKey.isNullOrEmpty() && model.keyComboMap.containsKey(actionKey)) {
      actionKey
    } else {
      Commands.shortcutKey(command.value)
    }
  }

  /** Returns whether the keyboard shortcut kept under [key] can be changed. */
  @JvmStatic
  fun isCustomizable(context: Context, key: String): Boolean =
    key != context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)

  /** Returns the text of the keyboard shortcut kept under [key], or null when there is none. */
  @JvmStatic
  fun keyboardShortcutText(manager: KeyComboManager, key: String): String? {
    val model = manager.keyComboModel ?: return null
    val keyCombo = model.getKeyComboForKey(key)
    if (keyCombo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) {
      return null
    }
    return manager.getKeyComboStringRepresentation(keyCombo)
  }
}
