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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FeatureSupport

/**
 * Gestures > Gesture layout: the gestures the Gestures screen starts from, SandFly's own or
 * VoiceOver's. Each layout keeps its own changes: a gesture of the VoiceOver layout is saved under
 * its preference key with [VOICEOVER_SUFFIX], before any gesture set. Gesture profiles and the
 * gestures of apps name gestures by their keys without a layout, so they apply to both.
 */
object GestureLayout {

  /** The layout in use, [SANDFLY] or [VOICEOVER]. Unset until chosen. */
  const val KEY = "pref_gesture_layout"

  const val SANDFLY = "sandfly"
  const val VOICEOVER = "voiceover"

  /**
   * Whether two-finger swipes run commands rather than passing to the app, where they scroll. Unset,
   * it follows the layout: on for VoiceOver.
   */
  const val TWO_FINGER_SWIPES_KEY = "pref_handle_two_finger_swipes"

  private const val VOICEOVER_SUFFIX = "_voiceover"

  // Set once a layout is chosen in the first-run dialog or on the Gestures screen. KEY alone isn't
  // enough: the Gestures screen's list saves it when the screen opens.
  private const val CHOSEN_KEY = "pref_gesture_layout_chosen"

  // Set while the first-run dialog waits for a choice, so that it opens again if left with Home.
  private const val ASK_PENDING_KEY = "pref_gesture_layout_ask_pending"

  /** Whether a layout has been chosen, in the first-run dialog or on the Gestures screen. */
  @JvmStatic fun isChosen(prefs: SharedPreferences): Boolean = prefs.getBoolean(CHOSEN_KEY, false)

  /** Whether the first-run dialog opened and was left without a choice. */
  @JvmStatic
  fun isAskPending(prefs: SharedPreferences): Boolean = prefs.getBoolean(ASK_PENDING_KEY, false)

  /** Notes that the first-run dialog opens, until a layout is chosen. */
  @JvmStatic
  fun markAskPending(prefs: SharedPreferences) {
    prefs.edit().putBoolean(ASK_PENDING_KEY, true).apply()
  }

  /** The layout in use. */
  @JvmStatic
  fun current(prefs: SharedPreferences): String =
    if (prefs.getString(KEY, SANDFLY) == VOICEOVER) VOICEOVER else SANDFLY

  @JvmStatic fun isVoiceOver(prefs: SharedPreferences): Boolean = current(prefs) == VOICEOVER

  /**
   * Starts using [layout], and has two-finger swipes run commands with VoiceOver's gestures, which
   * read with them, and scroll with SandFly's.
   */
  @JvmStatic
  fun select(prefs: SharedPreferences, layout: String) {
    val chosen = if (layout == VOICEOVER) VOICEOVER else SANDFLY
    prefs
      .edit()
      .putString(KEY, chosen)
      .putBoolean(TWO_FINGER_SWIPES_KEY, chosen == VOICEOVER)
      .putBoolean(CHOSEN_KEY, true)
      .remove(ASK_PENDING_KEY)
      .apply()
  }

  /** Whether two-finger swipes run commands in SandFly instead of scrolling in the app. */
  @JvmStatic
  fun handlesTwoFingerSwipes(prefs: SharedPreferences): Boolean =
    FeatureSupport.isMultiFingerGestureSupported() &&
      prefs.getBoolean(TWO_FINGER_SWIPES_KEY, isVoiceOver(prefs))

  /**
   * Returns the key under which the layout in use keeps the command of the gesture with preference
   * key [baseKey], which has no layout or gesture set.
   */
  @JvmStatic
  fun key(prefs: SharedPreferences, baseKey: String): String = key(current(prefs), baseKey)

  /** Returns the key under which [layout] keeps the command of the gesture [baseKey]. */
  @JvmStatic
  fun key(layout: String, baseKey: String): String =
    if (layout == VOICEOVER) baseKey + VOICEOVER_SUFFIX else baseKey

  /**
   * Returns the command the gesture [baseKey] runs until changed in the layout in use: [sandFlyDefault]
   * with SandFly's gestures, VoiceOver's command or Unassigned with VoiceOver's.
   */
  @JvmStatic
  fun defaultValue(
    context: Context,
    prefs: SharedPreferences,
    baseKey: String,
    sandFlyDefault: String,
  ): String =
    if (isVoiceOver(prefs)) VoiceOverGestures.defaultValue(context, baseKey) else sandFlyDefault

  /** Returns the command the gesture [baseKey] runs in the layout in use, before gesture sets. */
  @JvmStatic
  fun read(
    context: Context,
    prefs: SharedPreferences,
    baseKey: String,
    sandFlyDefault: String,
  ): String {
    val fallback = defaultValue(context, prefs, baseKey, sandFlyDefault)
    return prefs.getString(key(prefs, baseKey), fallback) ?: fallback
  }
}

/**
 * The commands of VoiceOver's gestures on iPhone, by the preference key of the SandFly gesture that
 * matches each. Every other gesture is Unassigned in the VoiceOver layout.
 */
object VoiceOverGestures {

  /** Pairs of a gesture's preference key and its command's value. */
  private val DEFAULTS =
    listOf(
      // One finger: swipes move by item or by the reading control (the rotor).
      R.string.pref_shortcut_left_key to R.string.shortcut_value_previous,
      R.string.pref_shortcut_right_key to R.string.shortcut_value_next,
      R.string.pref_shortcut_up_key to R.string.shortcut_value_selected_setting_previous_action,
      R.string.pref_shortcut_down_key to R.string.shortcut_value_selected_setting_next_action,
      R.string.pref_shortcut_1finger_2tap_key to
        R.string.shortcut_value_action_setting_activate_current_action,
      R.string.pref_shortcut_1finger_2tap_hold_key to R.string.shortcut_value_perform_long_click_action,
      // Two fingers.
      R.string.pref_shortcut_2finger_1tap_key to R.string.shortcut_value_pause_or_resume_feedback,
      // Magic Tap.
      R.string.pref_shortcut_2finger_2tap_key to R.string.shortcut_value_dictate_or_media_control,
      // Item Chooser.
      R.string.pref_shortcut_2finger_3tap_key to R.string.shortcut_value_screen_search,
      R.string.pref_shortcut_2finger_swipe_up_key to R.string.shortcut_value_read_from_top,
      R.string.pref_shortcut_2finger_swipe_down_key to R.string.shortcut_value_read_from_current,
      R.string.pref_shortcut_2finger_scrub_key to R.string.shortcut_value_back,
      // The rotor.
      R.string.pref_shortcut_2finger_rotate_clockwise_key to R.string.shortcut_value_select_next_setting,
      R.string.pref_shortcut_2finger_rotate_counterclockwise_key to
        R.string.shortcut_value_select_previous_setting,
      // Three fingers. A swipe moves the content with the fingers, a page at a time.
      R.string.pref_shortcut_3finger_1tap_key to R.string.shortcut_value_speak_position,
      R.string.pref_shortcut_3finger_2tap_key to R.string.shortcut_value_toggle_voice_feedback,
      // Screen Curtain.
      R.string.pref_shortcut_3finger_3tap_key to R.string.shortcut_value_show_hide_screen,
      R.string.pref_shortcut_3finger_4tap_key to R.string.shortcut_value_copy_last_spoken_phrase,
      R.string.pref_shortcut_3finger_swipe_up_key to R.string.shortcut_value_scroll_down,
      R.string.pref_shortcut_3finger_swipe_down_key to R.string.shortcut_value_scroll_up,
      R.string.pref_shortcut_3finger_swipe_left_key to R.string.shortcut_value_scroll_right,
      R.string.pref_shortcut_3finger_swipe_right_key to R.string.shortcut_value_scroll_left,
      // Four fingers. A double tap opens VoiceOver help, where gestures are named, not performed.
      R.string.pref_shortcut_4finger_1tap_top_key to R.string.shortcut_value_first_in_screen,
      R.string.pref_shortcut_4finger_1tap_bottom_key to R.string.shortcut_value_last_in_screen,
      R.string.pref_shortcut_4finger_2tap_key to R.string.shortcut_value_practice_gestures,
      // Sliding from an edge: Home, App Switcher, Notification Center and Control Center.
      R.string.pref_shortcut_edge_bottom_up_key to R.string.shortcut_value_home,
      R.string.pref_shortcut_edge_bottom_long_up_key to R.string.shortcut_value_overview,
      // From the top, a short slide opens quick settings and a long one notifications.
      R.string.pref_shortcut_edge_top_left_down_key to R.string.shortcut_value_quick_settings,
      R.string.pref_shortcut_edge_top_right_down_key to R.string.shortcut_value_quick_settings,
      R.string.pref_shortcut_edge_top_left_long_down_key to R.string.shortcut_value_notifications,
      R.string.pref_shortcut_edge_top_right_long_down_key to R.string.shortcut_value_notifications,
    )

  @Volatile private var byKey: Map<String, String>? = null

  /** Returns the command of the gesture [baseKey] in the VoiceOver layout, or Unassigned. */
  @JvmStatic
  fun defaultValue(context: Context, baseKey: String): String =
    defaults(context)[baseKey] ?: context.getString(R.string.shortcut_value_unassigned)

  /** The VoiceOver layout's commands by gesture preference key. */
  @JvmStatic
  fun defaults(context: Context): Map<String, String> =
    byKey
      ?: DEFAULTS.associate { (key, value) -> context.getString(key) to context.getString(value) }
        .also { byKey = it }
}
