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

import android.os.Bundle
import androidx.preference.Preference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** Commands: one entry per category of commands. */
class CommandsFragment : TalkbackBaseFragment() {

  override fun getTitle(): CharSequence = getString(R.string.title_pref_commands)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    val context = requireContext()
    val screen = preferenceManager.createPreferenceScreen(context)
    for (category in Commands.categories(context)) {
      screen.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_commands_${category.titleRes}"
          isPersistent = false
          isIconSpaceReserved = false
          title = getString(category.titleRes)
          fragment = CommandCategoryFragment::class.java.name
          extras.putInt(CommandCategoryFragment.ARG_CATEGORY, category.titleRes)
        }
      )
    }
    preferenceScreen = screen
  }
}

/** The commands of one category, each with the gestures and keyboard shortcut that run it. */
class CommandCategoryFragment : TalkbackBaseFragment() {

  private val categoryTitle: Int
    get() = arguments?.getInt(ARG_CATEGORY) ?: 0

  override fun getTitle(): CharSequence =
    if (categoryTitle != 0) getString(categoryTitle) else getString(R.string.title_pref_commands)

  private var gestures: List<CommandBindings.GestureSlot> = emptyList()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    gestures = readGestures(this) + CommandBindings.arrowKeys(requireContext())
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
  }

  override fun onResume() {
    super.onResume()
    // Gestures and shortcuts change on the command screens opened from here.
    fill()
  }

  /** Adds a row for each command the first time, and updates what runs each command after. */
  private fun fill() {
    val context = requireContext()
    val screen = preferenceScreen
    val category =
      Commands.categories(context).firstOrNull { it.titleRes == categoryTitle } ?: return
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val manager = CommandBindings.keyComboManager(context)
    val model = manager.keyComboModel
    for (command in category.commands) {
      val names = gestures.filter { it.value(context, prefs) == command.value }.map { it.name }
      val shortcut =
        model?.let {
          CommandBindings.keyboardShortcutText(
            manager,
            CommandBindings.keyboardKey(context, it, command),
          )
        }
      val summary = (names + listOfNotNull(shortcut)).joinToString(", ").ifEmpty { null }
      val key = "pref_command_${command.value}"
      val row: Preference =
        screen.findPreference(key)
          ?: AccessibilitySuitePreference(context).apply {
            this.key = key
            isPersistent = false
            isIconSpaceReserved = false
            title = command.label
            fragment = CommandFragment::class.java.name
            extras.putString(CommandFragment.ARG_COMMAND, command.value)
            screen.addPreference(this)
          }
      row.summary = summary
    }
  }

  companion object {
    const val ARG_CATEGORY = "category"
  }
}

/**
 * Returns the gestures that can be assigned, read from the Gestures screen, which [fragment] shows
 * only until it sets its own screen.
 */
internal fun readGestures(fragment: TalkbackBaseFragment): List<CommandBindings.GestureSlot> {
  fragment.addPreferencesFromResource(R.xml.gesture_preferences)
  return CommandBindings.gestures(fragment.requireContext(), fragment.preferenceScreen)
}
