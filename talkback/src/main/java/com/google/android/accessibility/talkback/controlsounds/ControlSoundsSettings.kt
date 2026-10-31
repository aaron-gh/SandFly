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

package com.google.android.accessibility.talkback.controlsounds

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.individualfeedback.SoundVibrations
import com.google.android.accessibility.talkback.soundthemes.SoundThemes
import com.google.android.accessibility.utils.output.FeedbackController

/** Stored settings for control sounds. They are off by default. */
object ControlSoundsSettings {
  const val PREF_ON = "pref_control_sounds"
  /** Each theme's own. Named apart from the setting all themes once shared, which was off. */
  const val PREF_SPEAK_ROLES = "pref_control_sounds_speak_roles_theme"
  const val PREF_3D = "pref_control_sounds_3d"

  const val VALUE_3D_WITH_HEADPHONES = "headphones"
  const val VALUE_3D_ALWAYS = "always"
  const val VALUE_3D_NEVER = "never"

  @JvmStatic fun isOn(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_ON, false)

  /**
   * Whether to still say "button" and the like when a control's sound plays. Until set, it is on
   * with SandFly's own theme and off with others.
   */
  @JvmStatic
  fun speakRoles(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREF_SPEAK_ROLES, speakRolesDefault(SoundThemes.activeId(prefs)))

  /** Whether the theme [themeId] says the kind of control when its theme.json doesn't say. */
  @JvmStatic
  fun speakRolesDefault(themeId: String): Boolean = themeId == SoundThemes.BACKTALK

  /** How control sounds are placed, as a [FeedbackController] spatial mode. */
  @JvmStatic
  fun spatialMode(prefs: SharedPreferences): Int =
    when (prefs.getString(PREF_3D, VALUE_3D_WITH_HEADPHONES)) {
      VALUE_3D_ALWAYS -> FeedbackController.SPATIAL_3D
      VALUE_3D_NEVER -> FeedbackController.SPATIAL_STEREO
      else -> FeedbackController.SPATIAL_3D_WITH_HEADPHONES
    }

  /**
   * The control sounds that play, which are none while control sounds or sound feedback are off.
   * The ones turned off in Individual sounds and vibrations don't play, nor the ones the sound
   * theme in use sets to no sound, in [silencedSounds]. A control whose sound does not play has its
   * kind spoken.
   */
  @JvmStatic
  fun playingSounds(
    prefs: SharedPreferences,
    soundFeedbackOn: Boolean,
    mutedSounds: Set<String>,
    silencedSounds: Set<String>,
  ): Set<Int> {
    if (!soundFeedbackOn || !isOn(prefs)) return emptySet()
    return ControlSounds.SOUNDS.filterKeys { it !in mutedSounds && it !in silencedSounds }
      .values
      .toSet()
  }

  /**
   * The control sounds whose vibrations play, which are none while control sounds or vibration
   * feedback are off. The ones turned off in Individual sounds and vibrations don't play, nor the
   * ones the sound theme in use sets to no vibration, in [unfeltVibrations]. A control's vibration
   * plays even when its sound does not, so that the kind of control can be told by touch alone.
   */
  @JvmStatic
  fun vibratingSounds(
    prefs: SharedPreferences,
    vibrationFeedbackOn: Boolean,
    mutedVibrations: Set<String>,
    unfeltVibrations: Set<String>,
  ): Set<Int> {
    if (!vibrationFeedbackOn || !isOn(prefs)) return emptySet()
    return ControlSounds.SOUNDS.filterKeys {
        it !in unfeltVibrations && SoundVibrations.switchOf(it) !in mutedVibrations
      }
      .values
      .toSet()
  }
}
