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

package com.google.android.accessibility.talkback.blackberry

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.SettingsProfileStore
import com.google.android.accessibility.talkback.backup.SettingsProfiles
import com.google.android.accessibility.talkback.soundthemes.SoundThemes
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.util.function.Consumer

/**
 * BlackBerry mode: a settings profile, "BlackBerry mode", that makes SandFly work as the BlackBerry
 * 10 Screen Reader did: its gestures, a keymap for keyboard phones with its keys, the BlackBerry
 * sound theme, and its short announcements. It is an overlay on the other settings, so everything
 * it doesn't set stays as it is, and its own settings can be changed like any profile's, with
 * Change settings on its screen in Settings profiles.
 *
 * The profile is made the first time the mode is turned on, from [BlackBerrySettings], and kept
 * after that, so changes to it stay. Deleting it brings back the defaults the next time.
 *
 * Its switch is on the Settings profiles screen, not the main settings screen, and both the switch
 * and the command ask before turning it on ([BlackBerryModeDialog]): most people never used
 * BlackBerry 10, and its gestures would be unfamiliar to them.
 */
object BlackBerryMode {

  /** The ID of the profile made for BlackBerry mode. State of this phone, kept out of profiles. */
  const val PROFILE_ID_KEY = "pref_blackberry_mode_profile"

  /** The sound theme installed from the app's assets the first time the mode is turned on. */
  const val THEME_ASSET = "blackberry_theme.zip"

  private const val TAG = "BlackBerryMode"

  /** Whether BlackBerry mode is on: its profile is the one used everywhere. */
  @JvmStatic
  fun isOn(context: Context): Boolean {
    val id = profileId(context) ?: return false
    return SettingsProfiles.store(context).manual()?.id == id
  }

  /** Turns BlackBerry mode on or off as [on] says, then runs [onDone] with what to say. */
  @JvmStatic
  fun set(context: Context, on: Boolean, onDone: Consumer<String>) {
    SettingsProfiles.inBackground(context, { setNow(it, on) }, onDone)
  }

  @WorkerThread
  internal fun setNow(context: Context, on: Boolean): String {
    if (!on) {
      if (isOn(context)) SettingsProfiles.setManualNow(context, null)
      return context.getString(R.string.blackberry_mode_off)
    }
    val id = profile(context) ?: return context.getString(R.string.blackberry_mode_not_changed)
    SettingsProfiles.setManualNow(context, id)
    return if (isOn(context)) context.getString(R.string.blackberry_mode_on)
    else context.getString(R.string.blackberry_mode_not_changed)
  }

  private fun profileId(context: Context): String? =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PROFILE_ID_KEY, null)

  /** The ID of BlackBerry mode's profile, made now if there is none. */
  @WorkerThread
  private fun profile(context: Context): String? {
    val store = SettingsProfiles.ready(context)
    profileId(context)?.let { id -> if (store.profile(id) != null) return id }
    val themeId = installTheme(context)
    val name = uniqueName(context)
    if (
      !SettingsProfiles.importProfile(
        context,
        name,
        triggers = SettingsProfileStore.Triggers(),
        settings = BlackBerrySettings.profileSettings(context, themeId),
      )
    ) {
      LogUtils.e(TAG, "Cannot make the BlackBerry mode profile")
      return null
    }
    val id =
      SettingsProfiles.store(context).profiles().firstOrNull { it.name == name }?.id ?: return null
    SharedPreferencesUtils.getSharedPreferences(context).edit().putString(PROFILE_ID_KEY, id).apply()
    return id
  }

  /**
   * The profile's name: "BlackBerry mode", or with a number when a profile of the user's has that
   * name, so that the user's profile isn't changed.
   */
  private fun uniqueName(context: Context): String {
    val store = SettingsProfiles.store(context)
    val base = context.getString(R.string.blackberry_mode_profile_name)
    if (!store.isNameTaken(base)) return base
    var number = 2
    while (store.isNameTaken("$base $number")) number++
    return "$base $number"
  }

  /**
   * Installs the BlackBerry sound theme if it isn't installed, without using it, and returns its ID,
   * or null when it can't be installed.
   */
  @WorkerThread
  private fun installTheme(context: Context): String? {
    val name = context.getString(R.string.blackberry_theme_name)
    val id = SoundThemes.idFor(name)
    if (SoundThemes.theme(context, id) != null) return id
    val zip = File(context.cacheDir, THEME_ASSET)
    return try {
      context.assets.open(THEME_ASSET).use { input ->
        zip.outputStream().use { output -> input.copyTo(output) }
      }
      val staged = SoundThemes.stage(context, zip, name)
      SoundThemes.install(
        context,
        SharedPreferencesUtils.getSharedPreferences(context),
        staged,
        /* use= */ false,
      )
      staged.id
    } catch (e: Throwable) {
      // On the profiles' executor, where an uncaught error would stop the screen reader.
      LogUtils.e(TAG, "Cannot install the BlackBerry sound theme: %s", e)
      null
    } finally {
      zip.delete()
    }
  }
}
