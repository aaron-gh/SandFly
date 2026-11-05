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
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.GestureProfiles
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.base.GestureListPreference

/** Gestures > Gesture profiles: the profile in use, and the profiles to add, change or delete. */
class GestureProfilesFragment : NotificationCodeFragment(R.string.title_pref_gesture_profiles) {

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val profiles = GestureProfiles.read(prefs)
    val choices = GestureProfiles.choices(context, prefs)
    val activeId = GestureProfiles.activeId(prefs)
    val active = choices.indexOfFirst { it.first == activeId }
    screen.action(getString(R.string.title_pref_gesture_profile_in_use), choices[active].second) {
      choose(getString(R.string.title_pref_gesture_profile_in_use), choices.map { it.second }, active) {
        GestureProfiles.select(context, prefs, choices[it].first)
        rebuild()
      }
    }
    screen.action(getString(R.string.title_pref_add_gesture_profile)) { add() }
    for (profile in profiles) {
      screen.link(profile.name, null, GestureProfileFragment::class.java) {
        putString(GestureProfileFragment.ARG_ID, profile.id)
      }
    }
  }

  private fun add() {
    val profiles = GestureProfiles.read(prefs)
    var number = profiles.size + 1
    while (nameTaken(profiles, getString(R.string.gesture_profile_new_name, number), null)) {
      number++
    }
    askText(
      getString(R.string.title_pref_add_gesture_profile),
      getString(R.string.gesture_profile_new_name, number),
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { name, _, _ ->
      val all = GestureProfiles.read(prefs)
      when {
        name.isEmpty() -> ""
        nameTaken(all, name, null) -> getString(R.string.gesture_profile_name_taken, name)
        else -> {
          GestureProfiles.write(prefs, all + GestureProfiles.Profile(GestureProfiles.newId(), name, emptyMap()))
          rebuild()
          null
        }
      }
    }
  }
}

/** One gesture profile: its name, its place in the list, and what each gesture runs in it. */
class GestureProfileFragment : NotificationCodeFragment(R.string.title_pref_gesture_profiles) {

  private val id: String by lazy { requireArguments().getString(ARG_ID).orEmpty() }

  // The gestures that can be assigned, named, grouped and in order as on the Gestures screen.
  private var categories: List<CommandBindings.GestureCategory> = emptyList()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    // Read from the Gestures screen, which the screen built in code then replaces.
    categories = readGestureCategories(this)
    super.onCreatePreferences(savedInstanceState, rootKey)
  }

  private val profile: GestureProfiles.Profile?
    get() = GestureProfiles.read(prefs).firstOrNull { it.id == id }

  override fun getTitle(): CharSequence = profile?.name ?: getText(R.string.title_pref_gesture_profiles)

  override fun build(screen: PreferenceGroup) {
    val profile = profile
    if (profile == null) {
      // Deleted, or restored settings without it.
      activity?.onBackPressedDispatcher?.onBackPressed()
      return
    }
    val all = GestureProfiles.read(prefs)
    val index = all.indexOfFirst { it.id == id }
    screen.action(getString(R.string.voice_profile_rename)) { rename() }
    if (index > 0) {
      screen.action(getString(R.string.voice_profile_move_up)) { move(-1) }
    }
    if (index < all.size - 1) {
      screen.action(getString(R.string.voice_profile_move_down)) { move(1) }
    }
    screen.action(getString(R.string.title_pref_delete_gesture_profile)) {
      confirm(getString(R.string.voice_profile_delete_confirm, profile.name), R.string.voice_profile_delete) {
        GestureProfiles.write(prefs, GestureProfiles.read(prefs).filterNot { it.id == id })
        if (prefs.getString(GestureProfiles.ACTIVE_KEY, "") == id) {
          GestureProfiles.select(requireContext(), prefs, "")
        }
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }

    // The categories and rows of the Gestures screen, with the list of commands it opens.
    val context = requireContext()
    for (category in categories) {
      val group = AccessibilitySuitePreferenceCategory(context)
      // The Gestures screen's key, which keeps whether it is expanded.
      group.key = category.key
      group.title = category.title
      screen.addPreference(group)
      for (slot in category.gestures) {
        val key = GestureProfiles.rowKey(id, slot.baseKey)
        GestureListPreference(context, key, slot.name, GestureProfiles.DEFAULT_VALUE).also {
          it.isPersistent = false
          it.isIconSpaceReserved = false
          group.addPreference(it)
        }
      }
    }
    collapseCategories()
  }

  override fun onDisplayPreferenceDialog(preference: Preference) {
    if (preference is GestureListPreference) {
      GestureListPreference.showCommandList(this, preference)
    } else {
      super.onDisplayPreferenceDialog(preference)
    }
  }

  private fun rename() {
    val profile = profile ?: return
    askText(getString(R.string.voice_profile_rename), profile.name, hint = getString(R.string.title_pref_voice_profile_name)) {
      name, _, _ ->
      when {
        name.isEmpty() -> ""
        nameTaken(GestureProfiles.read(prefs), name, id) -> getString(R.string.gesture_profile_name_taken, name)
        else -> {
          update { it.copy(name = name) }
          requireActivity().title = name
          null
        }
      }
    }
  }

  private fun move(by: Int) {
    val all = GestureProfiles.read(prefs).toMutableList()
    val index = all.indexOfFirst { it.id == id }
    val target = index + by
    if (index < 0 || target !in all.indices) return
    all.add(target, all.removeAt(index))
    GestureProfiles.write(prefs, all)
    rebuild()
    val neighbour = all[target - by].name
    announce(
      getString(if (by < 0) R.string.voice_profile_moved_above else R.string.voice_profile_moved_below, neighbour)
    )
  }

  private fun announce(text: CharSequence) {
    listView?.announceForAccessibility(text)
  }

  private fun update(change: (GestureProfiles.Profile) -> GestureProfiles.Profile) {
    GestureProfiles.write(prefs, GestureProfiles.read(prefs).map { if (it.id == id) change(it) else it })
    rebuild()
  }

  companion object {
    const val ARG_ID = "id"
  }
}

/** Whether a profile other than [exceptId], or Default, is called [name]. */
private fun NotificationCodeFragment.nameTaken(
  profiles: List<GestureProfiles.Profile>,
  name: String,
  exceptId: String?,
): Boolean =
  name.equals(getString(R.string.gesture_profile_default), ignoreCase = true) ||
    profiles.any { it.id != exceptId && it.name.equals(name, ignoreCase = true) }
