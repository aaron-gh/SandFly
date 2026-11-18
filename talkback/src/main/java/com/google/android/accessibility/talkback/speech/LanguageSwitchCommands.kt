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

package com.google.android.accessibility.talkback.speech

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.output.LanguageVoices

/**
 * The Switch language automatically and Identify languages from words commands, which turn those
 * settings on or off. The speech engine and the service apply a setting when it changes.
 */
object LanguageSwitchCommands {

  /** Turns Switch language automatically on or off and returns the new value. */
  @JvmStatic
  fun toggleSwitchLanguages(context: Context, prefs: SharedPreferences): Boolean {
    val key = context.getString(R.string.pref_switch_languages_key)
    val on = !prefs.getBoolean(key, context.resources.getBoolean(R.bool.pref_switch_languages_default))
    prefs.edit().putBoolean(key, on).apply()
    return on
  }

  /** Turns Identify languages from words on or off and returns the new value. */
  @JvmStatic
  fun toggleIdentifyFromWords(prefs: SharedPreferences): Boolean {
    // Off by default, as on the Speech screen.
    val on = !prefs.getBoolean(LanguageVoices.PREF_IDENTIFY_FROM_WORDS, false)
    prefs.edit().putBoolean(LanguageVoices.PREF_IDENTIFY_FROM_WORDS, on).apply()
    return on
  }
}
