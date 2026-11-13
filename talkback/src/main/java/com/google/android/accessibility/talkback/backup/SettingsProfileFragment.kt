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

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.NotificationReader
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.preference.search.SettingsIndex
import com.google.android.accessibility.talkback.preference.search.SettingsSearch

/**
 * One settings profile: Use now or Stop using, Change settings, when it is used automatically, the
 * settings it sets, and Rename, Move up, Move down and Delete.
 */
class SettingsProfileFragment : NotificationCodeFragment(R.string.title_pref_settings_profiles) {

  private val id: String
    get() = arguments?.getString(ARG_ID).orEmpty()

  /** The settings the profile sets, once read. */
  private var profileSettings: Map<String, Any?>? = null

  /** The title and the path of each setting by its key, once the settings screens are read. */
  private var titles: Map<String, Pair<String, String>> = emptyMap()

  private var stopIndex: Runnable? = null

  override fun getTitle(): CharSequence =
    SettingsProfiles.loadedStore()?.profile(id)?.name ?: super.getTitle()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    stopIndex =
      SettingsIndex.buildInBackground(requireContext()) { entries ->
        titles =
          entries
            .filter { it.key != null }
            .associate { it.key!! to (it.title to SettingsSearch.path(it.path)) }
        if (isAdded) rebuild()
      }
  }

  override fun onResume() {
    super.onResume()
    // Coming back from changing the settings ends it.
    SettingsProfiles.finishEditing(requireContext()) { said -> load(said) }
  }

  override fun onDestroy() {
    stopIndex?.run()
    super.onDestroy()
  }

  /** Reads the profile's settings, then shows them and says [said], if anything. */
  private fun load(said: String? = null) {
    if (!isAdded) return
    val id = id
    SettingsProfiles.inBackground(requireContext(), { SettingsProfiles.settingsOf(it, id) }) {
      profileSettings = it
      // The profiles may have been read only now.
      if (isAdded) activity?.title = getTitle()
      done(said)
    }
  }

  override fun build(screen: PreferenceGroup) {
    val store = SettingsProfiles.loadedStore() ?: return
    val profile = store.profile(id) ?: return
    val manual = store.manual()?.id == profile.id
    screen.action(
      getString(if (manual) R.string.settings_profile_stop_using else R.string.settings_profile_use_now),
      if (profile.id in SettingsProfiles.inUse()) getString(R.string.settings_profile_in_use) else null,
    ) {
      SettingsProfiles.switchTo(requireContext(), if (manual) "" else profile.id) { said -> load(said) }
    }
    screen.action(getString(R.string.settings_profile_change_settings)) { changeSettings(profile) }

    val triggers = profile.triggers
    val automatic = screen.category(R.string.settings_profile_use_automatically)
    automatic.switch(getString(R.string.settings_profile_web), triggers.web) {
      setTriggers(profile, triggers.copy(web = it))
    }
    automatic.switch(getString(R.string.settings_profile_reading), triggers.reading) {
      setTriggers(profile, triggers.copy(reading = it))
    }
    automatic.switch(getString(R.string.settings_profile_text_editing), triggers.editing) {
      setTriggers(profile, triggers.copy(editing = it))
    }

    val apps = screen.category(R.string.settings_profile_apps)
    apps.action(getString(R.string.settings_profile_add_app)) {
      chooseApp { packageName, _ ->
        if (packageName.isNotEmpty()) {
          setTriggers(profile, triggers.copy(apps = triggers.apps + packageName))
        }
      }
    }
    val context = requireContext()
    val names = triggers.apps.associateWith { NotificationReader.appName(context, it) }
    for (packageName in triggers.apps.sortedBy { names.getValue(it).lowercase() }) {
      val name = names.getValue(packageName)
      apps.action(name) {
        confirm(getString(R.string.settings_profile_remove_confirm, name, profile.name), R.string.quick_settings_remove) {
          setTriggers(profile, triggers.copy(apps = triggers.apps - packageName))
        }
      }
    }

    val list = screen.category(R.string.settings_profile_settings)
    profileSettings?.let { settings ->
      if (settings.isEmpty()) {
        list.action(getString(R.string.settings_profile_no_settings)) {}.isEnabled = false
      }
      val shown =
        settings.keys.map { key ->
          val (title, path) = titles[SettingsBackup.keyOf(key)] ?: (SettingsBackup.keyOf(key) to null)
          Triple(key, title, path)
        }
      for ((key, title, path) in shown.sortedBy { it.second.lowercase() }) {
        list.action(title, path) {
          confirm(getString(R.string.settings_profile_remove_confirm, title, profile.name), R.string.quick_settings_remove) {
            SettingsProfiles.inBackground(requireContext(), {
              SettingsProfiles.removeSetting(it, profile.id, key)
              SettingsProfiles.settingsOf(it, profile.id)
            }) {
              profileSettings = it
              done(null)
            }
          }
        }
      }
    }

    val index = store.profiles().indexOfFirst { it.id == profile.id }
    screen.action(getString(R.string.voice_profile_rename)) { rename(store, profile) }
    if (index > 0) {
      screen.action(getString(R.string.voice_profile_move_up)) { move(profile, -1) }
    }
    if (index < store.profiles().size - 1) {
      screen.action(getString(R.string.voice_profile_move_down)) { move(profile, 1) }
    }
    screen.action(getString(R.string.voice_profile_delete)) { delete(profile) }
  }

  /** Sends the settings changed from here to the profile, and opens the settings. */
  private fun changeSettings(profile: SettingsProfileStore.Profile) {
    val context = requireContext()
    SettingsProfiles.startEditing(context, profile.id) { said ->
      if (!isAdded || said == null) return@startEditing
      listView?.announceForAccessibility(said)
      if (SettingsProfiles.editing() == profile.id) {
        startActivity(Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java))
      }
    }
  }

  private fun setTriggers(profile: SettingsProfileStore.Profile, triggers: SettingsProfileStore.Triggers) {
    SettingsProfiles.inBackground(requireContext(), { SettingsProfiles.setTriggers(it, profile.id, triggers) }) {
      done(null)
    }
  }

  private fun move(profile: SettingsProfileStore.Profile, by: Int) {
    SettingsProfiles.inBackground(requireContext(), { SettingsProfiles.move(it, profile.id, by) }) {
      done(null)
    }
  }

  private fun rename(store: SettingsProfileStore, profile: SettingsProfileStore.Profile) {
    askText(
      getString(R.string.voice_profile_rename),
      profile.name,
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { entered, _, _ ->
      val name = entered.take(SettingsProfileStore.MAX_NAME_LENGTH)
      when {
        name.isEmpty() -> ""
        store.isNameTaken(name, profile.id) -> getString(R.string.settings_profile_name_taken, name)
        else -> {
          SettingsProfiles.inBackground(requireContext(), { store.rename(profile.id, name) }) {
            activity?.title = getTitle()
            done(null)
          }
          null
        }
      }
    }
  }

  private fun delete(profile: SettingsProfileStore.Profile) {
    confirm(getString(R.string.voice_profile_delete_confirm, profile.name), R.string.voice_profile_delete) {
      SettingsProfiles.inBackground(requireContext(), { SettingsProfiles.delete(it, profile.id) }) {
        if (isAdded) activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }
  }

  /** Says [said], if anything, and shows the profile as it is now. */
  private fun done(said: String?) {
    if (!isAdded) return
    rebuild()
    said?.let { listView?.announceForAccessibility(it) }
  }

  companion object {
    const val ARG_ID = "id"
  }
}
