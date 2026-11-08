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

import android.os.Bundle
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment

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
    askText(
      getString(R.string.title_pref_add_reading_control_list),
      getString(R.string.reading_control_list_new_name, number),
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { name, _, _ ->
      val all = ReadingControlLists.read(prefs)
      when {
        name.isEmpty() -> ""
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

/** One reading control list: its name, and the reading controls in it. */
class ReadingControlListFragment :
  NotificationCodeFragment(R.string.title_pref_reading_control_lists) {
  private val id: String by lazy { requireArguments().getString(ARG_ID).orEmpty() }

  // The title of each reading control on the Reading controls screen, by preference key.
  private var titles: Map<String, CharSequence> = emptyMap()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    // Read from the Reading controls screen, which the screen built in code then replaces.
    setPreferencesFromResource(R.xml.selector_menu_preferences, null)
    titles = buildMap { collectTitles(preferenceScreen) }
    super.onCreatePreferences(savedInstanceState, rootKey)
  }

  private fun MutableMap<String, CharSequence>.collectTitles(group: PreferenceGroup) {
    for (index in 0 until group.preferenceCount) {
      val preference = group.getPreference(index)
      if (preference is PreferenceGroup) collectTitles(preference)
      val title = preference.title
      if (preference.key != null && title != null) put(preference.key, title)
    }
  }

  private val list: ReadingControlLists.ControlList?
    get() = ReadingControlLists.read(prefs).firstOrNull { it.id == id }

  override fun getTitle(): CharSequence =
    list?.name ?: getText(R.string.title_pref_reading_control_lists)

  override fun build(screen: PreferenceGroup) {
    val list = list
    if (list == null) {
      activity?.onBackPressedDispatcher?.onBackPressed()
      return
    }
    screen.action(getString(R.string.voice_profile_rename)) { rename(list) }
    screen.action(getString(R.string.title_pref_delete_reading_control_list)) {
      confirm(getString(R.string.voice_profile_delete_confirm, list.name), R.string.voice_profile_delete) {
        ReadingControlLists.write(prefs, ReadingControlLists.read(prefs).filterNot { it.id == id })
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }
    for (key in SelectorController.defaultOrderKeys(requireContext())) {
      val title = titles[key] ?: continue
      screen.switch(title, key in list.controls) { checked -> setControl(key, checked) }
    }
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

  private fun setControl(key: String, checked: Boolean) {
    ReadingControlLists.write(
      prefs,
      ReadingControlLists.read(prefs).map {
        if (it.id != id) it
        else it.copy(controls = if (checked) it.controls + key else it.controls - key)
      },
    )
  }

  companion object {
    const val ARG_ID = "id"
  }
}
