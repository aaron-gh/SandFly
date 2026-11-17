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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.base.focusAfterLayout
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions

/** Reading controls > Reading control lists: the lists to add or open. */
class ReadingControlListsFragment :
  NotificationCodeFragment(R.string.title_pref_reading_control_lists) {

  override fun build(screen: PreferenceGroup) {
    screen.action(getString(R.string.title_pref_add_reading_control_list)) { add() }
    for (list in ReadingControlLists.read(prefs)) {
      screen.link(list.name, null, ReadingControlListFragment::class.java) {
        putString(ReadingControlListFragment.ARG_ID, list.id)
      }
    }
  }

  private fun add() {
    val lists = ReadingControlLists.read(prefs)
    var number = lists.size + 1
    while (lists.any { it.name == getString(R.string.reading_control_list_new_name, number) }) {
      number++
    }
    val suggested = getString(R.string.reading_control_list_new_name, number)
    askText(getString(R.string.title_pref_add_reading_control_list), hint = suggested) { entered, _, _ ->
      val name = entered.ifEmpty { suggested }
      val all = ReadingControlLists.read(prefs)
      when {
        all.any { it.name.equals(name, ignoreCase = true) } ->
          getString(R.string.reading_control_list_name_taken, name)
        else -> {
          ReadingControlLists.write(
            prefs,
            all + ReadingControlLists.ControlList(ReadingControlLists.newId(), name, emptyList()),
          )
          rebuild()
          null
        }
      }
    }
  }
}

/**
 * One reading control list: its name, the reading controls in it under Current, in order, each
 * with Move up, Move down and Remove, as accessibility actions and on a double tap, and below them
 * the reading controls not in it, which a double tap adds at the end.
 */
class ReadingControlListFragment :
  NotificationCodeFragment(R.string.title_pref_reading_control_lists) {
  private val id: String by lazy { requireArguments().getString(ARG_ID).orEmpty() }

  // The groups of reading controls on the Reading controls screen: the title of each, and the
  // preference key and title of each of its controls.
  private var groups: List<Pair<CharSequence, List<Pair<String, CharSequence>>>> = emptyList()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    // Read from the Reading controls screen, which the screen built in code then replaces.
    setPreferencesFromResource(R.xml.selector_menu_preferences, null)
    groups = readGroups(preferenceScreen)
    super.onCreatePreferences(savedInstanceState, rootKey)
  }

  private fun readGroups(screen: PreferenceGroup): List<Pair<CharSequence, List<Pair<String, CharSequence>>>> {
    val known = SelectorController.defaultOrderKeys(requireContext()).toSet()
    return (0 until screen.preferenceCount)
      .map { screen.getPreference(it) }
      .filterIsInstance<PreferenceCategory>()
      .map { group ->
        val controls =
          (0 until group.preferenceCount)
            .map { group.getPreference(it) }
            .filter { it.key in known && it.title != null }
            .map { it.key to it.title!! }
        (group.title ?: "") to controls
      }
  }

  private val list: ReadingControlLists.ControlList?
    get() = ReadingControlLists.read(prefs).firstOrNull { it.id == id }

  override fun getTitle(): CharSequence =
    list?.name ?: getText(R.string.title_pref_reading_control_lists)

  override fun build(screen: PreferenceGroup) {
    val saved = list
    if (saved == null) {
      activity?.onBackPressedDispatcher?.onBackPressed()
      return
    }
    val list =
      if (saved.ordered) {
        saved
      } else {
        // A list saved before lists had their own order keeps the order it was used in.
        ReadingControlLists.inMainOrder(requireContext(), prefs, saved).also {
          saveControls(it.controls)
        }
      }
    screen.action(getString(R.string.voice_profile_rename)) { rename(list) }
    screen.action(getString(R.string.title_pref_delete_reading_control_list)) {
      confirm(getString(R.string.voice_profile_delete_confirm, list.name), R.string.voice_profile_delete) {
        ReadingControlLists.write(prefs, ReadingControlLists.read(prefs).filterNot { it.id == id })
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }
    val titles = groups.flatMap { it.second }.toMap()
    val controls = list.controls.filter { it in titles }
    val current =
      AccessibilitySuitePreferenceCategory(requireContext()).also {
        it.key = "pref_reading_control_list_current"
        it.setTitle(R.string.item_list_current)
        it.isIconSpaceReserved = false
        screen.addPreference(it)
      }
    controls.forEachIndexed { index, key ->
      current.addPreference(ControlRow(requireContext(), controls, index, titles.getValue(key)))
    }
    current.setCollapsible(getString(R.string.category_collapsed), getString(R.string.category_expanded))
    val notInList =
      PreferenceCategory(requireContext()).also {
        it.title = getString(R.string.item_list_not_in_reading_control_list, list.name)
        it.isIconSpaceReserved = false
        screen.addPreference(it)
      }
    var anyNotInList = false
    groups.forEachIndexed { index, (title, groupControls) ->
      val available = groupControls.filter { it.first !in list.controls }
      if (available.isEmpty()) return@forEachIndexed
      anyNotInList = true
      val group =
        AccessibilitySuitePreferenceCategory(requireContext()).also {
          it.key = "pref_reading_control_list_group_$index"
          it.title = title
          it.isIconSpaceReserved = false
          screen.addPreference(it)
        }
      available.forEachIndexed { position, (key, name) ->
        // Focus stays in the group, on the control that takes the added one's place.
        val next = (available.getOrNull(position + 1) ?: available.getOrNull(position - 1))?.first
        group.action(name) { add(key, name, next) }.also { it.key = availableKey(key) }
      }
      group.setCollapsible(getString(R.string.category_collapsed), getString(R.string.category_expanded))
    }
    notInList.isVisible = anyNotInList
  }

  private fun rowKey(key: String) = "pref_reading_control_list_row_$key"

  private fun availableKey(key: String) = "pref_reading_control_list_available_$key"

  /** Saves [controls] as the reading controls of this list, in order. */
  private fun saveControls(controls: List<String>) {
    ReadingControlLists.write(
      prefs,
      ReadingControlLists.read(prefs).map {
        if (it.id != id) it else it.copy(controls = controls, ordered = true)
      },
    )
  }

  /** Moves focus to the row with [key] once the screen is built again, and says [announcement]. */
  private fun focusAfterChange(key: String?, announcement: String?) {
    val view = listView ?: return
    focusAfterLayout(view, key?.let { findPreference<Preference>(it) }, announcement)
  }

  /** Adds [key] at the end of the list. Focus goes to [next] in its group, or else to [key]. */
  private fun add(key: String, name: CharSequence, next: String?) {
    val controls = list?.controls ?: return
    if (key in controls) return
    saveControls(controls + key)
    rebuild()
    focusAfterChange(
      next?.let { availableKey(it) } ?: rowKey(key),
      getString(R.string.quick_settings_added, name),
    )
  }

  private fun remove(controls: List<String>, index: Int, name: CharSequence): Boolean {
    if (index !in controls.indices) return false
    val left = controls.filterIndexed { i, _ -> i != index }
    saveControls(left)
    rebuild()
    focusAfterChange(
      left.getOrNull(minOf(index, left.size - 1))?.let { rowKey(it) },
      getString(R.string.quick_settings_removed, name),
    )
    return true
  }

  private fun move(controls: List<String>, index: Int, offset: Int): Boolean {
    val moved = ReadingControlLists.moved(controls, index, index + offset) ?: return false
    saveControls(moved)
    rebuild()
    val view = listView ?: return true
    focusAfterChange(rowKey(controls[index]), positionText(view, index + offset, moved.size))
    return true
  }

  private fun showActions(controls: List<String>, index: Int, name: CharSequence) {
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    if (index > 0) actions += R.string.action_move_up to { move(controls, index, -1) }
    if (index < controls.size - 1) actions += R.string.action_move_down to { move(controls, index, 1) }
    actions += R.string.quick_settings_remove to { remove(controls, index, name) }
    choose(name, actions.map { getString(it.first) }) { actions[it].second() }
  }

  private fun rename(list: ReadingControlLists.ControlList) {
    askText(
      getString(R.string.voice_profile_rename),
      list.name,
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { name, _, _ ->
      val all = ReadingControlLists.read(prefs)
      when {
        name.isEmpty() -> ""
        all.any { it.id != id && it.name.equals(name, ignoreCase = true) } ->
          getString(R.string.reading_control_list_name_taken, name)
        else -> {
          ReadingControlLists.write(prefs, all.map { if (it.id == id) it.copy(name = name) else it })
          rebuild()
          null
        }
      }
    }
  }

  /** A reading control in the list, at [index] of [controls]. */
  private inner class ControlRow(
    context: Context,
    private val controls: List<String>,
    private val index: Int,
    private val name: CharSequence,
  ) : Preference(context) {
    init {
      key = rowKey(controls[index])
      isPersistent = false
      isIconSpaceReserved = false
      title = name
      setOnPreferenceClickListener {
        showActions(controls, index, name)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other preferences, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, index, controls.size) { move(controls, index, it) }
      val removeText = getString(R.string.quick_settings_remove)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, removeText),
        removeText,
      ) { _, _ ->
        remove(controls, index, name)
      }
    }
  }

  companion object {
    const val ARG_ID = "id"
  }
}
