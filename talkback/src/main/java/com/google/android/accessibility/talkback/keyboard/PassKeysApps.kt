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
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * The apps that get the keys pressed without the SandFly key, such as the arrow keys, instead of
 * SandFly, for apps that use them themselves.
 */
object PassKeysApps {
  private const val PREF_APPS = "pref_pass_keys_apps"

  /** Whether [app] gets the keys pressed without the SandFly key. */
  @JvmStatic
  fun isOn(context: Context, app: String?): Boolean =
    app != null &&
      SharedPreferencesUtils.getSharedPreferences(context).getStringSet(PREF_APPS, null)?.contains(app) ==
        true

  /** Turns passing keys to [app] on or off. Returns whether it is on now. */
  @JvmStatic
  fun toggle(context: Context, app: String): Boolean {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val apps = HashSet(prefs.getStringSet(PREF_APPS, null) ?: emptySet())
    val on = apps.add(app)
    if (!on) {
      apps.remove(app)
    }
    prefs.edit { putStringSet(PREF_APPS, apps) }
    return on
  }
}
