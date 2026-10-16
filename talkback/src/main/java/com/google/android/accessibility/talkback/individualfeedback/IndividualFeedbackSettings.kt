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

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R

/**
 * One sound or vibration the user can turn off on its own. [resourceNames] are resource entry names
 * rather than IDs, because IDs change between builds and the braille sounds live in a module
 * talkback cannot see. An item can cover more than one resource, like the eight radial menu notes.
 */
data class FeedbackItem(val key: String, val title: Int, val resourceNames: List<String>) {
  constructor(key: String, title: Int) : this(key, title, listOf(key))
}

/**
 * Settings for turning single sounds and vibrations off while sound feedback and vibration feedback
 * stay on. Everything is on by default, and only the items the user turned off are stored.
 */
object IndividualFeedbackSettings {
  const val PREF_MUTED_SOUNDS = "pref_individual_muted_sounds"
  const val PREF_MUTED_VIBRATIONS = "pref_individual_muted_vibrations"

  val SOUNDS =
    listOf(
      FeedbackItem("focus", R.string.individual_sound_focus),
      FeedbackItem("focus_actionable", R.string.individual_sound_focus_actionable),
      FeedbackItem("view_entered", R.string.individual_sound_view_entered),
      FeedbackItem("tick", R.string.individual_sound_tick),
      FeedbackItem("long_clicked", R.string.individual_sound_long_clicked),
      FeedbackItem("scroll_tone", R.string.individual_sound_scroll_tone),
      FeedbackItem("chime_up", R.string.individual_sound_chime_up),
      FeedbackItem("chime_down", R.string.individual_sound_chime_down),
      FeedbackItem("complete", R.string.individual_sound_complete),
      FeedbackItem("window_state", R.string.individual_sound_window_state),
      FeedbackItem("gesture_begin", R.string.individual_sound_gesture_begin),
      FeedbackItem("gesture_end", R.string.individual_sound_gesture_end),
      FeedbackItem("typo", R.string.individual_sound_typo),
      FeedbackItem("hyperlink", R.string.individual_sound_hyperlink),
      FeedbackItem("formatting", R.string.individual_sound_formatting),
      FeedbackItem("screen_off", R.string.individual_sound_screen_off),
      FeedbackItem("loading", R.string.individual_sound_loading),
      FeedbackItem("browse_mode_on_v4_2", R.string.individual_sound_browse_mode_on),
      FeedbackItem("browse_mode_off_v4_2", R.string.individual_sound_browse_mode_off),
      FeedbackItem(
        "radial_menu",
        R.string.individual_sound_radial_menu,
        (1..8).map { "radial_menu_$it" },
      ),
      FeedbackItem("display_connected", R.string.individual_sound_display_connected),
      FeedbackItem("display_disconnected", R.string.individual_sound_display_disconnected),
      FeedbackItem("double_beep", R.string.individual_sound_double_beep),
      FeedbackItem("turn_on", R.string.individual_sound_turn_on),
      FeedbackItem("turn_off", R.string.individual_sound_turn_off),
      FeedbackItem("calibration_done", R.string.individual_sound_calibration_done),
      FeedbackItem("control_button", R.string.individual_sound_control_button),
      FeedbackItem("control_checkbox", R.string.individual_sound_control_checkbox),
      FeedbackItem("control_radio_button", R.string.individual_sound_control_radio_button),
      FeedbackItem("control_edit_text", R.string.individual_sound_control_edit_text),
      FeedbackItem("control_combo_box", R.string.individual_sound_control_combo_box),
      FeedbackItem("control_slider", R.string.individual_sound_control_slider),
      FeedbackItem("control_link", R.string.individual_sound_control_link),
      FeedbackItem("control_image", R.string.individual_sound_control_image),
      FeedbackItem("control_clock", R.string.individual_sound_control_clock),
      FeedbackItem("control_tab", R.string.individual_sound_control_tab),
      FeedbackItem("control_menu_item", R.string.individual_sound_control_menu_item),
      FeedbackItem("control_list_item", R.string.individual_sound_control_list_item),
      FeedbackItem("control_tree_item", R.string.individual_sound_control_tree_item),
    )

  /**
   * The braille keyboard's vibrations, which play without a sound. Their switches are named after
   * the vibrations, as sound themes name them.
   */
  val BRAILLE_KEYBOARD_VIBRATIONS =
    listOf(
      FeedbackItem("braille_keyboard_character", R.string.individual_vibration_braille_character),
      FeedbackItem("braille_keyboard_space", R.string.individual_vibration_braille_space),
      FeedbackItem("braille_keyboard_new_line", R.string.individual_vibration_braille_new_line),
      FeedbackItem("braille_keyboard_hold", R.string.individual_vibration_braille_hold),
      FeedbackItem("braille_keyboard_gesture", R.string.individual_vibration_braille_gesture),
      FeedbackItem(
        "braille_keyboard_nothing_to_delete",
        R.string.individual_vibration_braille_nothing_to_delete,
      ),
    )

  /**
   * One switch for each sound's vibration, in the same order and with the same titles as the
   * sounds, one for announcements and one for each of the braille keyboard's vibrations, which
   * vibrate without a sound.
   */
  val VIBRATIONS =
    listOf(
      FeedbackItem("view_hovered_pattern", R.string.individual_vibration_focus),
      FeedbackItem("view_actionable_pattern", R.string.individual_vibration_focus_actionable),
      FeedbackItem("view_entered_pattern", R.string.individual_sound_view_entered),
      FeedbackItem("view_clicked_pattern", R.string.individual_vibration_clicked),
      FeedbackItem("view_long_clicked_pattern", R.string.individual_vibration_long_clicked),
      FeedbackItem(
        "scroll_pattern",
        R.string.individual_sound_scroll_tone,
        listOf("scroll_pattern", "scroll_item_pattern"),
      ),
      FeedbackItem("list_entered_pattern", R.string.individual_sound_chime_up),
      FeedbackItem("list_exited_pattern", R.string.individual_sound_chime_down),
      FeedbackItem("complete_pattern", R.string.individual_sound_complete),
      FeedbackItem("window_state_pattern", R.string.individual_vibration_window_state),
      FeedbackItem("gesture_detection_repeated_pattern", R.string.individual_vibration_gesture),
      FeedbackItem("gesture_end_pattern", R.string.individual_sound_gesture_end),
      FeedbackItem("typo_pattern", R.string.individual_vibration_typo),
      FeedbackItem("hyperlink_pattern", R.string.individual_sound_hyperlink),
      FeedbackItem("formatting_pattern", R.string.individual_sound_formatting),
      FeedbackItem("screen_off_pattern", R.string.individual_sound_screen_off),
      FeedbackItem("loading_pattern", R.string.individual_sound_loading),
      FeedbackItem("browse_mode_on_pattern", R.string.individual_sound_browse_mode_on),
      FeedbackItem("browse_mode_off_pattern", R.string.individual_sound_browse_mode_off),
      FeedbackItem(
        "radial_menu_pattern",
        R.string.individual_sound_radial_menu,
        (1..8).map { "radial_menu_${it}_pattern" },
      ),
      FeedbackItem("notification_pattern", R.string.individual_vibration_notification),
      *BRAILLE_KEYBOARD_VIBRATIONS.toTypedArray(),
      // Control vibrations come only from sound themes.
      FeedbackItem("control_button_pattern", R.string.individual_sound_control_button),
      FeedbackItem("control_checkbox_pattern", R.string.individual_sound_control_checkbox),
      FeedbackItem("control_radio_button_pattern", R.string.individual_sound_control_radio_button),
      FeedbackItem("control_edit_text_pattern", R.string.individual_sound_control_edit_text),
      FeedbackItem("control_combo_box_pattern", R.string.individual_sound_control_combo_box),
      FeedbackItem("control_slider_pattern", R.string.individual_sound_control_slider),
      FeedbackItem("control_image_pattern", R.string.individual_sound_control_image),
      FeedbackItem("control_clock_pattern", R.string.individual_sound_control_clock),
      FeedbackItem("control_tab_pattern", R.string.individual_sound_control_tab),
      FeedbackItem("control_menu_item_pattern", R.string.individual_sound_control_menu_item),
      FeedbackItem("control_list_item_pattern", R.string.individual_sound_control_list_item),
      FeedbackItem("control_tree_item_pattern", R.string.individual_sound_control_tree_item),
    )

  /**
   * The Selection vibration switch from before every sound had its own vibration. Selection now
   * plays the vibration of its sound, the actionable focus vibration.
   */
  private const val OLD_SELECTION_VIBRATION = "view_focused_or_selected_pattern"
  private const val SELECTION_VIBRATION = "view_actionable_pattern"

  /**
   * Screen off used to play the volume beep and its vibration, under switches named Volume change,
   * though no volume change plays them. Set once screen off has been given those switches' state.
   */
  private const val OLD_VOLUME_SOUND = "volume_beep"
  private const val OLD_VOLUME_VIBRATION = "volume_pattern"
  private const val PREF_SCREEN_OFF_SPLIT = "pref_individual_screen_off_split"

  /**
   * Carries over switches that no longer exist, once: if Selection was turned off, the vibration
   * selection now plays is turned off instead. And if Volume change was turned off, which only
   * silenced screen off, Screen off is turned off instead.
   */
  fun migrate(prefs: SharedPreferences) {
    val muted = stringSet(prefs, PREF_MUTED_VIBRATIONS)
    if (OLD_SELECTION_VIBRATION in muted) {
      prefs
        .edit()
        .putStringSet(PREF_MUTED_VIBRATIONS, muted - OLD_SELECTION_VIBRATION + SELECTION_VIBRATION)
        .apply()
    }
    if (!prefs.getBoolean(PREF_SCREEN_OFF_SPLIT, false)) {
      val mutedSounds = stringSet(prefs, PREF_MUTED_SOUNDS)
      val mutedVibrations = stringSet(prefs, PREF_MUTED_VIBRATIONS)
      prefs
        .edit()
        .putStringSet(
          PREF_MUTED_SOUNDS,
          if (OLD_VOLUME_SOUND in mutedSounds) mutedSounds - OLD_VOLUME_SOUND + "screen_off"
          else mutedSounds,
        )
        .putStringSet(
          PREF_MUTED_VIBRATIONS,
          if (OLD_VOLUME_VIBRATION in mutedVibrations) {
            mutedVibrations - OLD_VOLUME_VIBRATION + "screen_off_pattern"
          } else {
            mutedVibrations
          },
        )
        .putBoolean(PREF_SCREEN_OFF_SPLIT, true)
        .apply()
    }
  }

  fun isSoundOn(prefs: SharedPreferences, item: FeedbackItem): Boolean =
    item.key !in stringSet(prefs, PREF_MUTED_SOUNDS)

  fun setSoundOn(prefs: SharedPreferences, item: FeedbackItem, on: Boolean) =
    setOn(prefs, PREF_MUTED_SOUNDS, item, on)

  fun isVibrationOn(prefs: SharedPreferences, item: FeedbackItem): Boolean =
    item.key !in stringSet(prefs, PREF_MUTED_VIBRATIONS)

  fun setVibrationOn(prefs: SharedPreferences, item: FeedbackItem, on: Boolean) =
    setOn(prefs, PREF_MUTED_VIBRATIONS, item, on)

  /** Resource names of the sounds that are turned off, for the feedback controller. */
  fun mutedSoundResources(prefs: SharedPreferences): Set<String> =
    mutedResources(prefs, PREF_MUTED_SOUNDS, SOUNDS)

  /** Resource names of the vibration patterns that are turned off, for the feedback controller. */
  fun mutedVibrationResources(prefs: SharedPreferences): Set<String> =
    mutedResources(prefs, PREF_MUTED_VIBRATIONS, VIBRATIONS)

  private fun setOn(prefs: SharedPreferences, pref: String, item: FeedbackItem, on: Boolean) {
    val muted = stringSet(prefs, pref)
    prefs.edit().putStringSet(pref, if (on) muted - item.key else muted + item.key).apply()
  }

  private fun mutedResources(
    prefs: SharedPreferences,
    pref: String,
    items: List<FeedbackItem>,
  ): Set<String> {
    val muted = stringSet(prefs, pref)
    return items.filter { it.key in muted }.flatMap { it.resourceNames }.toSet()
  }

  // Copied, because the set the preferences return must not be changed.
  private fun stringSet(prefs: SharedPreferences, pref: String): Set<String> =
    prefs.getStringSet(pref, null)?.toSet() ?: emptySet()
}
