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

package com.google.android.accessibility.talkback.distribution

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.CommandSwitchPreference
import com.google.android.accessibility.talkback.shizuku.PlayInstaller
import com.google.android.accessibility.talkback.update.Updater

/**
 * What differs between the full build, published on GitHub, and the Google Play build. This is the
 * full build's: it updates itself, and can record Google Play as its installer.
 */
object Distribution {
  /** Whether this is the Google Play build. */
  const val IS_PLAY = false

  /** Whether an update is downloading or being installed. */
  @JvmStatic
  val isUpdating: Boolean
    get() = Updater.isBusy

  @JvmStatic
  fun onServiceConnected(context: Context) {
    Updater.startAutomaticChecks(context)
  }

  @JvmStatic
  fun onServiceDestroyed() {
    Updater.stopAutomaticChecks()
  }

  /** Called when Shizuku allows SandFly, after the service starts. */
  fun onShizukuAllowed(context: Context) {
    // Puts Google Play back as the installer if the user wants it and an update dropped it.
    PlayInstaller.reapplyIfNeeded(context)
  }

  /** Sets up the settings of the Updates screen that [fragment] shows, if it does. */
  @JvmStatic
  fun setUpPreferences(fragment: PreferenceFragmentCompat) {
    Updater.setUpPreferences(fragment)
  }

  /** Shows the state of the settings of the Updates screen again, when [fragment] resumes. */
  @JvmStatic
  fun onPreferencesResumed(fragment: PreferenceFragmentCompat) {
    val context = fragment.context ?: return
    val preference =
      fragment.findPreference<CommandSwitchPreference>(fragment.getString(R.string.pref_play_installer_key))
        ?: return
    val wanted = PlayInstaller.isWanted(context)
    val isPlay = PlayInstaller.isPlay(context)
    preference.isChecked = wanted
    preference.summary =
      fragment.getString(
        if (wanted && !isPlay) R.string.summary_pref_play_installer_pending
        else R.string.summary_pref_play_installer
      )
    preference.command = PlayInstaller.command(context, !isPlay)
    preference.onToggle = CommandSwitchPreference.Toggle { checked, done ->
      PlayInstaller.set(context, checked) { worked ->
        done.accept(worked)
        if (fragment.isAdded) {
          onPreferencesResumed(fragment)
        }
      }
    }
  }
}
