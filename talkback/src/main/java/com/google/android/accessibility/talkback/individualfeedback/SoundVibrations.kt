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

package com.google.android.accessibility.talkback.individualfeedback

import android.content.Context
import com.google.android.accessibility.talkback.controlsounds.ControlSounds

/**
 * The vibration that goes with each sound. Every sound has its own vibration, and it plays even
 * when sound feedback is off, so each action can be told apart by touch alone. The patterns are in
 * vibration_backtalk.xml.
 */
object SoundVibrations {
  /** The name of the switch that turns off the vibration of a sound, by its resource name. */
  @JvmStatic
  fun switchOf(soundName: String): String? = PATTERNS[soundName]

  /**
   * Sounds that play another sound's vibration, under that vibration's switch: a link control
   * feels like a link in text. A theme's vibration for one of them replaces only its own.
   */
  private val SHARED_PATTERNS: Set<String> = setOf("control_link")

  /**
   * Vibrations without a sound that a sound theme can replace, by their names in theme.json, with
   * the names they play under. Announcements play a vibration of their own, and the braille
   * keyboard and direct touch vibrate outside the feedback controller.
   */
  val VIBRATION_ONLY: Map<String, String> =
    mapOf(
      "announcement" to "notification_pattern",
      "braille_keyboard_character" to "braille_keyboard_character",
      "braille_keyboard_space" to "braille_keyboard_space",
      "braille_keyboard_new_line" to "braille_keyboard_new_line",
      "braille_keyboard_hold" to "braille_keyboard_hold",
      "braille_keyboard_gesture" to "braille_keyboard_gesture",
      "braille_keyboard_nothing_to_delete" to "braille_keyboard_nothing_to_delete",
      "direct_touch_on" to "direct_touch_on",
      "direct_touch_off" to "direct_touch_off",
    )

  /**
   * The braille keyboard's vibrations, which it plays itself. Each plays under its own name, which
   * is also the name of its switch.
   */
  val BRAILLE_KEYBOARD: Set<String> =
    setOf(
      "braille_keyboard_character",
      "braille_keyboard_space",
      "braille_keyboard_new_line",
      "braille_keyboard_hold",
      "braille_keyboard_gesture",
      "braille_keyboard_nothing_to_delete",
    )

  /** Patterns that play a sound's vibration without its sound: selection, by the sound's name. */
  private val ALSO_PLAYED_AS: Map<String, List<String>> =
    mapOf("focus_actionable" to listOf("view_focused_or_selected_pattern"))

  /** The names of every vibration a sound theme can replace, as theme.json names them. */
  @JvmStatic
  fun themeNames(soundKeys: Collection<String>): Set<String> = soundKeys.toSet() + VIBRATION_ONLY.keys

  /**
   * Turns a theme's vibrations, by their theme.json names, into the names they play under: the
   * sounds they play with, the patterns played without the sounds, and the vibrations without a
   * sound. [soundResources] gives the resource names of each sound, such as the eight circle menu
   * notes.
   */
  @JvmStatic
  fun playedAs(
    themeVibrations: Map<String, IntArray>,
    soundResources: Map<String, List<String>>,
  ): Map<String, IntArray> {
    val played = HashMap<String, IntArray>()
    for ((name, pattern) in themeVibrations) {
      VIBRATION_ONLY[name]?.let { played[it] = pattern }
      soundResources[name]?.forEach { sound ->
        played[sound] = pattern
        if (sound !in SHARED_PATTERNS) PATTERNS[sound]?.let { played[it] = pattern }
      }
      ALSO_PLAYED_AS[name]?.forEach { played[it] = pattern }
    }
    return played
  }

  /**
   * The switch in Individual sounds and vibrations that turns off what plays under [name], or null
   * if it has none, like direct touch, which has a setting of its own.
   */
  @JvmStatic
  fun switchOfPlayed(name: String): String? =
    switchOf(name)
      ?: when (name) {
        in PATTERNS.values,
        in BRAILLE_KEYBOARD,
        "notification_pattern" -> name
        "view_focused_or_selected_pattern" -> "view_actionable_pattern"
        else -> null
      }

  /** Vibration pattern resource names, by sound resource names. */
  val PATTERNS: Map<String, String> =
    mapOf(
      "focus" to "view_hovered_pattern",
      "focus_actionable" to "view_actionable_pattern",
      "view_entered" to "view_entered_pattern",
      "tick" to "view_clicked_pattern",
      "long_clicked" to "view_long_clicked_pattern",
      "scroll_tone" to "scroll_pattern",
      "chime_up" to "list_entered_pattern",
      "chime_down" to "list_exited_pattern",
      "complete" to "complete_pattern",
      "window_state" to "window_state_pattern",
      "typo" to "typo_pattern",
      "hyperlink" to "hyperlink_pattern",
      "formatting" to "formatting_pattern",
      "screen_off" to "screen_off_pattern",
      // The loading tone repeats while waiting, such as for an image description, so its
      // vibration is a heartbeat that shows the work goes on even with sounds off.
      "loading" to "loading_pattern",
      "browse_mode_on_v4_2" to "browse_mode_on_pattern",
      "browse_mode_off_v4_2" to "browse_mode_off_pattern",
      "screen_on" to "screen_on_pattern",
      // The braille display's sounds.
      "display_connected" to "display_connected_pattern",
      "display_disconnected" to "display_disconnected_pattern",
      "double_beep" to "double_beep_pattern",
      "turn_on" to "turn_on_pattern",
      "turn_off" to "turn_off_pattern",
      "calibration_done" to "calibration_done_pattern",
    ) +
      (1..8).associate { "radial_menu_$it" to "radial_menu_${it}_pattern" } +
      NavigationSounds.PATTERNS +
      AgentSounds.NAMES.associateWith { "${it}_pattern" } +
      ControlSounds.SOUNDS.keys.associateWith {
        if (it in SHARED_PATTERNS) "hyperlink_pattern" else "${it}_pattern"
      }

  /**
   * Vibration pattern resource IDs, by sound resource names, for the feedback controller. By name,
   * because the braille sounds are in a module whose R class talkback cannot see.
   */
  @JvmStatic
  fun patternIds(context: Context): Map<String, Int> =
    PATTERNS.mapValues { (_, pattern) ->
        context.resources.getIdentifier(pattern, "array", context.packageName)
      }
      .filterValues { it != 0 }
}
