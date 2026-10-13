/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.speechcontrol

import android.content.SharedPreferences

/** Which apps the user let speak through Backtalk, and which they didn't. */
object SpeechControlApps {
  enum class State {
    ALLOWED,
    DENIED,
    /** The app hasn't asked yet, or the user hasn't answered. */
    UNKNOWN,
  }

  private const val PREF_ALLOWED = "pref_speech_control_allowed_apps"
  private const val PREF_DENIED = "pref_speech_control_denied_apps"

  fun state(prefs: SharedPreferences, app: String): State =
    when (app) {
      in set(prefs, PREF_ALLOWED) -> State.ALLOWED
      in set(prefs, PREF_DENIED) -> State.DENIED
      else -> State.UNKNOWN
    }

  fun setAllowed(prefs: SharedPreferences, app: String, allowed: Boolean) {
    val allowedApps = set(prefs, PREF_ALLOWED)
    val deniedApps = set(prefs, PREF_DENIED)
    prefs
      .edit()
      .putStringSet(PREF_ALLOWED, if (allowed) allowedApps + app else allowedApps - app)
      .putStringSet(PREF_DENIED, if (allowed) deniedApps - app else deniedApps + app)
      .apply()
  }

  /** Every app the user has answered for, allowed or not. */
  fun answered(prefs: SharedPreferences): Set<String> =
    set(prefs, PREF_ALLOWED) + set(prefs, PREF_DENIED)

  private fun set(prefs: SharedPreferences, key: String): Set<String> =
    prefs.getStringSet(key, null).orEmpty()
}
