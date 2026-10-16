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

package com.google.android.accessibility.talkback.actor.gemini

import android.content.SharedPreferences

/**
 * The Gemini API key that Describe image and Describe screen send to Google.
 *
 * The user enters it in Automatic descriptions settings. A key built into the app from
 * local.properties is used only while the user has not entered one, so that developer builds keep
 * working.
 */
object GeminiApiKey {
  const val PREF_KEY = "pref_gemini_api_key"

  /** Where to get a key. */
  const val GET_KEY_URL = "https://aistudio.google.com/apikey"

  /** The key the user entered, or an empty string. */
  fun userKey(prefs: SharedPreferences): String = prefs.getString(PREF_KEY, null)?.trim() ?: ""

  /** Saves the [key], or removes the user's key when it is blank. */
  fun setUserKey(prefs: SharedPreferences, key: String) {
    val trimmed = key.trim()
    if (trimmed.isEmpty()) {
      prefs.edit().remove(PREF_KEY).apply()
    } else {
      prefs.edit().putString(PREF_KEY, trimmed).apply()
    }
  }

  /** The key to use: the user's, or else the [builtInKey], which may be empty. */
  @JvmStatic
  fun effectiveKey(prefs: SharedPreferences, builtInKey: String): String =
    userKey(prefs).ifEmpty { builtInKey.trim() }
}
