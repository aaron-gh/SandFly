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
import androidx.annotation.VisibleForTesting

/**
 * Which apps the user let speak through Backtalk, and which they didn't, and apart from that,
 * which they let speak while the device is locked. Also the apps that used the speech service
 * before the user answered for them, so that Let other apps speak lists them.
 */
object SpeechControlApps {
  enum class State {
    ALLOWED,
    DENIED,
    /** The app hasn't asked yet, or the user hasn't answered. */
    UNKNOWN,
  }

  private const val PREF_ALLOWED = "pref_speech_control_allowed_apps"
  private const val PREF_DENIED = "pref_speech_control_denied_apps"
  private const val PREF_LOCKED_ALLOWED = "pref_speech_control_locked_allowed_apps"
  private const val PREF_LOCKED_DENIED = "pref_speech_control_locked_denied_apps"
  // Oldest first, one package name per line.
  internal const val PREF_CONNECTED = "pref_speech_control_connected_apps"

  /** At most this many unanswered apps are kept; the oldest is dropped first. */
  @VisibleForTesting internal const val MAX_CONNECTED = 32

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
      .putString(PREF_CONNECTED, join(connected(prefs) - app))
      .apply()
  }

  /**
   * Records that [app] used the speech service, so that it is listed before the user has answered
   * for it. Nothing changes if the user has answered or it is already recorded.
   */
  fun recordConnected(prefs: SharedPreferences, app: String) {
    if (state(prefs, app) != State.UNKNOWN) return
    val apps = connected(prefs)
    if (app in apps) return
    prefs.edit().putString(PREF_CONNECTED, join((apps + app).takeLast(MAX_CONNECTED))).apply()
  }

  /** Apps that used the speech service and that the user hasn't answered for, oldest first. */
  fun connected(prefs: SharedPreferences): List<String> =
    prefs.getString(PREF_CONNECTED, null)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

  /**
   * Whether the user let [app] speak while the device is locked, a separate answer from [state]:
   * allowing an app doesn't allow it while locked.
   */
  fun lockedState(prefs: SharedPreferences, app: String): State =
    when (app) {
      in set(prefs, PREF_LOCKED_ALLOWED) -> State.ALLOWED
      in set(prefs, PREF_LOCKED_DENIED) -> State.DENIED
      else -> State.UNKNOWN
    }

  fun setAllowedWhileLocked(prefs: SharedPreferences, app: String, allowed: Boolean) {
    val allowedApps = set(prefs, PREF_LOCKED_ALLOWED)
    val deniedApps = set(prefs, PREF_LOCKED_DENIED)
    prefs
      .edit()
      .putStringSet(PREF_LOCKED_ALLOWED, if (allowed) allowedApps + app else allowedApps - app)
      .putStringSet(PREF_LOCKED_DENIED, if (allowed) deniedApps - app else deniedApps + app)
      .apply()
  }

  /** Forgets the user's answers for [app], so it isn't listed. */
  fun forget(prefs: SharedPreferences, app: String) {
    val connected = connected(prefs)
    if (
      state(prefs, app) == State.UNKNOWN &&
        lockedState(prefs, app) == State.UNKNOWN &&
        app !in connected
    ) {
      return
    }
    prefs
      .edit()
      .putStringSet(PREF_ALLOWED, set(prefs, PREF_ALLOWED) - app)
      .putStringSet(PREF_DENIED, set(prefs, PREF_DENIED) - app)
      .putStringSet(PREF_LOCKED_ALLOWED, set(prefs, PREF_LOCKED_ALLOWED) - app)
      .putStringSet(PREF_LOCKED_DENIED, set(prefs, PREF_LOCKED_DENIED) - app)
      .putString(PREF_CONNECTED, join(connected - app))
      .apply()
  }

  /** Every app the user has answered for, allowed or not. */
  fun answered(prefs: SharedPreferences): Set<String> =
    set(prefs, PREF_ALLOWED) + set(prefs, PREF_DENIED)

  private fun join(apps: List<String>): String = apps.joinToString("\n")

  private fun set(prefs: SharedPreferences, key: String): Set<String> =
    prefs.getStringSet(key, null).orEmpty()
}
