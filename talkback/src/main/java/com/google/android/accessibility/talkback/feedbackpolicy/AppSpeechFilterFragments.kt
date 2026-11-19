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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.NotificationReader

/** Verbosity > Chosen apps: the apps in which Speak only focus moved by the user applies. */
class SpeakUserFocusAppsFragment :
  NotificationCodeFragment(R.string.title_pref_speak_user_focus_apps) {

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val apps = AppSpeechFilter.apps(context, prefs)
    screen.action(getString(R.string.app_gestures_add_app)) {
      chooseApp { packageName, _ ->
        if (packageName.isNotEmpty()) {
          AppSpeechFilter.setApps(context, prefs, AppSpeechFilter.apps(context, prefs) + packageName)
          rebuild()
        }
      }
    }
    val names = apps.associateWith { NotificationReader.appName(context, it) }
    for (packageName in apps.sortedBy { names.getValue(it).lowercase() }) {
      val name = names.getValue(packageName)
      screen.action(name) {
        confirm(
          getString(R.string.settings_profile_remove_confirm, name, getTitle()),
          R.string.quick_settings_remove,
        ) {
          AppSpeechFilter.setApps(context, prefs, AppSpeechFilter.apps(context, prefs) - packageName)
          rebuild()
        }
      }
    }
  }
}

/**
 * Verbosity > Keyboards with their own speech: the keyboards whose keys, typing and cursor movement
 * SandFly doesn't speak while they are shown.
 */
class KeyboardsWithOwnSpeechFragment :
  NotificationCodeFragment(R.string.title_pref_keyboards_with_own_speech) {

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val manager = context.getSystemService(InputMethodManager::class.java) ?: return
    val keyboards = AppSpeechFilter.keyboards(context, prefs)
    val installed = manager.inputMethodList
    val names = installed.associate { it.id to it.loadLabel(context.packageManager).toString() }
    val addable =
      installed
        .filter { it.id !in keyboards && it.packageName != context.packageName }
        .map { it.id to (names[it.id] ?: it.id) }
        .sortedBy { it.second.lowercase() }
    if (addable.isNotEmpty()) {
      screen.action(getString(R.string.settings_profile_add_keyboard)) {
        choose(getString(R.string.settings_profile_add_keyboard), addable.map { it.second }) {
          AppSpeechFilter.setKeyboards(
            context,
            prefs,
            AppSpeechFilter.keyboards(context, prefs) + addable[it].first,
          )
          rebuild()
        }
      }
    }
    val added = keyboards.associateWith { names[it] ?: it }
    for (id in keyboards.sortedBy { added.getValue(it).lowercase() }) {
      val name = added.getValue(id)
      screen.action(name) {
        confirm(
          getString(R.string.settings_profile_remove_confirm, name, getTitle()),
          R.string.quick_settings_remove,
        ) {
          AppSpeechFilter.setKeyboards(context, prefs, AppSpeechFilter.keyboards(context, prefs) - id)
          rebuild()
        }
      }
    }
  }
}
