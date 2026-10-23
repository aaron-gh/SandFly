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

package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Navigation > Remember browse mode in each app: the apps where the user last turned browse mode
 * on, and those where they last turned it off.
 */
object BrowseModeApps {
  private const val PREF_ON = "pref_browse_mode_on_apps"
  private const val PREF_OFF = "pref_browse_mode_off_apps"

  /** Whether Remember browse mode in each app is on. */
  @JvmStatic
  fun isOn(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_browse_mode_per_app_key),
        context.resources.getBoolean(R.bool.pref_browse_mode_per_app_default),
      )

  /** True or false for how the user last left browse mode in [app], or null if never. */
  @JvmStatic
  fun remembered(context: Context, app: String): Boolean? {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    return when {
      prefs.getStringSet(PREF_ON, null)?.contains(app) == true -> true
      prefs.getStringSet(PREF_OFF, null)?.contains(app) == true -> false
      else -> null
    }
  }

  /** Remembers that the user turned browse mode on or off in [app]. */
  @JvmStatic
  fun remember(context: Context, app: String, on: Boolean) {
    if (!isOn(context)) return
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val onApps = HashSet(prefs.getStringSet(PREF_ON, null) ?: emptySet())
    val offApps = HashSet(prefs.getStringSet(PREF_OFF, null) ?: emptySet())
    if (on) {
      onApps += app
      offApps -= app
    } else {
      offApps += app
      onApps -= app
    }
    prefs.edit {
      putStringSet(PREF_ON, onApps)
      putStringSet(PREF_OFF, offApps)
    }
  }
}
