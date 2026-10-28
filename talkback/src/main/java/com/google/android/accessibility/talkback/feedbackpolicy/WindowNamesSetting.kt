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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.content.SharedPreferences
import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.VerbosityPreferences

/**
 * The Window names setting: whether a window's name is spoken when the window opens and when focus
 * moves into it from another window, and whether the first item focused in a new window cuts the
 * name off. It replaced two switches: "Speak window names", a verbosity preset setting that only
 * stopped the name of a system or keyboard window being spoken when focus moved into it, and
 * "Speak first item without waiting for window name".
 */
object WindowNamesSetting {
  /** Names are spoken when a window opens and when focus moves into it, before the first item. */
  const val ALWAYS = "always"

  /** As [ALWAYS], but the first item focused in a new window cuts the name off. */
  const val UNTIL_FIRST_ITEM = "until_first_item"

  /** Names are spoken when a window opens, not when focus moves into it from another window. */
  const val WINDOW_OPENS = "window_opens"

  /** No window names are spoken. */
  const val NEVER = "never"

  @JvmStatic
  fun read(prefs: SharedPreferences, res: Resources): String =
    prefs.getString(res.getString(R.string.pref_window_names_key), null)
      ?.takeIf { it in res.getStringArray(R.array.pref_window_names_values) }
      ?: res.getString(R.string.pref_window_names_default)

  /**
   * Gives the setting the choice that matches the two switches it replaced, unless it has a value:
   * the first item cutting the name off wins, then window names being off when focus moves.
   */
  @JvmStatic
  fun migrate(prefs: SharedPreferences, res: Resources) {
    val key = res.getString(R.string.pref_window_names_key)
    if (prefs.contains(key)) {
      return
    }
    val firstItemInterrupts =
      prefs.getBoolean(
        res.getString(R.string.pref_first_item_interrupts_window_name_key),
        res.getBoolean(R.bool.pref_first_item_interrupts_window_name_default),
      )
    val speakSystemWindowNames =
      VerbosityPreferences.getPresetValueBool(
        prefs,
        res,
        res.getString(R.string.pref_speak_system_window_titles_key),
        res.getBoolean(R.bool.pref_speak_system_window_titles_default),
      )
    val value =
      when {
        firstItemInterrupts -> UNTIL_FIRST_ITEM
        !speakSystemWindowNames -> WINDOW_OPENS
        else -> ALWAYS
      }
    prefs.edit().putString(key, value).apply()
  }
}
