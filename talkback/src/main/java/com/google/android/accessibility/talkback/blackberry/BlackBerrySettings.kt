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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.SettingsBackup
import com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.keyboard.KeyboardPhones
import com.google.android.accessibility.talkback.keyboard.Keymap
import com.google.android.accessibility.talkback.soundthemes.SoundThemes

/**
 * The settings of BlackBerry mode's profile when it is made, after the BlackBerry 10 Screen
 * Reader's Low verbosity: no hints, no position in a list ("item 3 of 10"), no actions available,
 * and a sound rather than speech when the keyboard shows or hides. Kinds of control are still said,
 * and pitch changes stay on, as BlackBerry spoke typed and deleted text at other pitches.
 */
object BlackBerrySettings {

  /** The settings by their key in the default preferences file. */
  @JvmStatic
  fun values(context: Context, themeId: String?, keyboardPhone: Boolean): Map<String, Any> =
    buildMap {
      put(GestureLayout.KEY, GestureLayout.BLACKBERRY)
      put(GestureLayout.TWO_FINGER_SWIPES_KEY, true)
      // The BlackBerry keymap is for keyboards without Ctrl; other phones keep their keymap.
      if (keyboardPhone) {
        put(
          context.getString(R.string.pref_select_keymap_key),
          context.getString(Keymap.BLACKBERRY.valueRes),
        )
      }
      if (themeId != null) {
        put(SoundThemes.PREF_ACTIVE, themeId)
        put(ControlSoundsSettings.PREF_ON, false)
        put(ControlSoundsSettings.PREF_SPEAK_ROLES, true)
      }
      put(context.getString(R.string.pref_a11y_hints_key), false)
      put(context.getString(R.string.pref_speak_container_element_positions_key), false)
      put(context.getString(R.string.pref_speak_grid_positions_key), false)
      put(SPEAK_ACTIONS_AVAILABLE_KEY, false)
      put(context.getString(R.string.pref_verbose_scroll_announcement_key), false)
      put(context.getString(R.string.pref_announce_keyboard_shown_hidden_key), false)
      put(context.getString(R.string.pref_intonation_key), true)
      put(context.getString(R.string.pref_speak_roles_key), true)
    }

  /** The settings of the profile, by profile key. */
  @JvmStatic
  fun profileSettings(context: Context, themeId: String?): Map<String, Any?> =
    values(context, themeId, KeyboardPhones.isKeyboardPhone()).mapKeys { (key, _) ->
      SettingsBackup.profileKey(SettingsBackup.DEFAULT_PREFS, key)
    }

  private const val SPEAK_ACTIONS_AVAILABLE_KEY = "pref_speak_actions_available"
}
