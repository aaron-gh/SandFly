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
 * Back up and restore > Settings profiles: New settings profile, and the profiles, with those in
 * use marked. Each profile opens its own screen, [SettingsProfileFragment].
 */
class SettingsProfilesFragment : NotificationCodeFragment(R.string.title_pref_settings_profiles) {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    SettingsProfiles.inBackground(requireContext(), { SettingsProfiles.ready(it) }) {
      if (isAdded) rebuild()
    }
  }

  override fun onResume() {
    super.onResume()
    // Coming back from changing a profile's settings ends it.
    SettingsProfiles.finishEditing(requireContext()) { said -> done(said) }
  }

  override fun build(screen: PreferenceGroup) {
    // The screen is built again once the profiles have been read in the background.
    val store = SettingsProfiles.loadedStore() ?: return
    if (store.isLegacy()) return
    screen.action(getString(R.string.title_pref_new_settings_profile)) { add(store) }
    val inUse = SettingsProfiles.inUse()
    for (profile in store.profiles()) {
      screen.link(
        profile.name,
        if (profile.id in inUse) getString(R.string.settings_profile_in_use) else null,
        SettingsProfileFragment::class.java,
      ) {
        putString(SettingsProfileFragment.ARG_ID, profile.id)
      }
    }
  }

  private fun add(store: SettingsProfileStore) {
    val context = requireContext()
    var number = store.profiles().size + 1
    while (store.isNameTaken(getString(R.string.settings_profile_new_name, number))) {
      number++
    }
    val suggested = getString(R.string.settings_profile_new_name, number)
    askText(getString(R.string.title_pref_new_settings_profile), hint = suggested) { entered, _, _ ->
      val name = entered.ifEmpty { suggested }.take(SettingsProfileStore.MAX_NAME_LENGTH)
      if (store.isNameTaken(name)) {
        getString(R.string.settings_profile_name_taken, name)
      } else {
        SettingsProfiles.inBackground(context, { SettingsProfiles.add(it, name) }) { added ->
          done(if (added == null) getString(R.string.settings_profile_not_saved) else null)
        }
        null
      }
    }
  }

  /** Says [said], if anything, and shows the profiles as they are now. */
  private fun done(said: String?) {
    if (!isAdded) return
    rebuild()
    said?.let { listView?.announceForAccessibility(it) }
  }
}
