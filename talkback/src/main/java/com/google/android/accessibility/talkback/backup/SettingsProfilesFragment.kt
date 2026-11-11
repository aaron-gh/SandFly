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

package com.google.android.accessibility.talkback.backup

import android.os.Bundle
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment

/**
 * Back up and restore > Settings profiles: the profiles, with the one in use marked, and New
 * settings profile. A profile's item offers Switch to, Save current settings to this profile,
 * Rename and Delete.
 */
class SettingsProfilesFragment : NotificationCodeFragment(R.string.title_pref_settings_profiles) {

  private val store: SettingsProfileStore
    get() = SettingsProfiles.store(requireContext())

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    // The first time, the settings become Default; later, the profile in use gets the changes.
    SettingsProfiles.inBackground(
      requireContext(),
      { context ->
        SettingsProfiles.ready(context)
        SettingsProfiles.saveActive(context)
      },
    ) {
      if (isAdded) rebuild()
    }
  }

  override fun build(screen: PreferenceGroup) {
    // The screen is built again once the profiles have been read in the background.
    val store = SettingsProfiles.loadedStore() ?: return
    screen.action(getString(R.string.title_pref_new_settings_profile)) { add() }
    val activeId = store.active()?.id
    for (profile in store.profiles()) {
      val active = profile.id == activeId
      screen.action(profile.name, if (active) getString(R.string.settings_profile_in_use) else null) {
        actions(profile, active)
      }
    }
  }

  private fun actions(profile: SettingsProfileStore.Profile, active: Boolean) {
    val actions = mutableListOf<Pair<CharSequence, () -> Unit>>()
    if (!active) {
      actions += getString(R.string.settings_profile_switch) to { switchTo(profile) }
    }
    actions += getString(R.string.settings_profile_save_current) to { saveTo(profile, active) }
    actions += getString(R.string.voice_profile_rename) to { rename(profile) }
    if (!active) {
      actions += getString(R.string.voice_profile_delete) to { delete(profile) }
    }
    choose(profile.name, actions.map { it.first }) { actions[it].second() }
  }

  private fun add() {
    val context = requireContext()
    var number = store.profiles().size + 1
    while (nameTaken(getString(R.string.settings_profile_new_name, number), null)) {
      number++
    }
    val suggested = getString(R.string.settings_profile_new_name, number)
    askText(getString(R.string.title_pref_new_settings_profile), hint = suggested) { entered, _, _ ->
      val name = entered.ifEmpty { suggested }.take(SettingsProfileStore.MAX_NAME_LENGTH)
      if (nameTaken(name, null)) {
        getString(R.string.settings_profile_name_taken, name)
      } else {
        SettingsProfiles.inBackground(context, { SettingsProfiles.add(it, name) }) { added ->
          done(if (added == null) getString(R.string.settings_profile_not_saved) else null)
        }
        null
      }
    }
  }

  private fun switchTo(profile: SettingsProfileStore.Profile) {
    SettingsProfiles.switchTo(requireContext(), profile.id) { said -> done(said) }
  }

  private fun saveTo(profile: SettingsProfileStore.Profile, active: Boolean) {
    val save = {
      val context = requireContext()
      SettingsProfiles.inBackground(context, { SettingsProfiles.saveTo(it, profile.id) }) { saved ->
        done(
          if (saved) context.getString(R.string.settings_profile_saved, profile.name)
          else context.getString(R.string.settings_profile_not_saved)
        )
      }
    }
    if (active) {
      save()
    } else {
      confirm(getString(R.string.settings_profile_save_confirm, profile.name), R.string.settings_profile_save) {
        save()
      }
    }
  }

  private fun rename(profile: SettingsProfileStore.Profile) {
    askText(
      getString(R.string.voice_profile_rename),
      profile.name,
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { entered, _, _ ->
      val name = entered.take(SettingsProfileStore.MAX_NAME_LENGTH)
      when {
        name.isEmpty() -> ""
        nameTaken(name, profile.id) -> getString(R.string.settings_profile_name_taken, name)
        else -> {
          val profiles = store
          SettingsProfiles.inBackground(requireContext(), { profiles.rename(profile.id, name) }) {
            done(null)
          }
          null
        }
      }
    }
  }

  private fun delete(profile: SettingsProfileStore.Profile) {
    confirm(getString(R.string.voice_profile_delete_confirm, profile.name), R.string.voice_profile_delete) {
      val profiles = store
      SettingsProfiles.inBackground(requireContext(), { profiles.delete(profile.id) }) { done(null) }
    }
  }

  /** Whether another profile is called [name], or Default before it is made. */
  private fun nameTaken(name: String, exceptId: String?): Boolean =
    store.isNameTaken(name, exceptId) ||
      (store.isEmpty() && name.trim().equals(SettingsProfiles.defaultName(requireContext()), ignoreCase = true))

  /** Says [said], if anything, and shows the profiles as they are now. */
  private fun done(said: String?) {
    if (!isAdded) return
    rebuild()
    said?.let { listView?.announceForAccessibility(it) }
  }
}
