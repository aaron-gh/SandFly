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
import com.google.android.accessibility.talkback.R

/**
 * The commands of the BlackBerry 10 Screen Reader's gestures (OS 10.3), from BlackBerry's user
 * guides, by the preference key of the SandFly gesture that matches each. Every other gesture is
 * Unassigned in the BlackBerry layout.
 *
 * Not mapped, as SandFly has no such gesture: two-finger swipe up or down and hold (top and bottom
 * of a page), three-finger swipe down and hold (read from the current position), and the
 * two-finger back-and-forth swipes of text editing (cut, copy, paste).
 */
object BlackBerryGestures {

  /** Pairs of a gesture's preference key and its command's value. */
  private val DEFAULTS =
    listOf(
      // One finger: right and left move to the next and previous item, by the size of text chunk
      // that up and down choose.
      R.string.pref_shortcut_right_key to R.string.shortcut_value_next_by_navigation_setting,
      R.string.pref_shortcut_left_key to R.string.shortcut_value_previous_by_navigation_setting,
      R.string.pref_shortcut_up_key to R.string.shortcut_value_next_granularity,
      R.string.pref_shortcut_down_key to R.string.shortcut_value_previous_granularity,
      R.string.pref_shortcut_1finger_2tap_key to
        R.string.shortcut_value_action_setting_activate_current_action,
      R.string.pref_shortcut_1finger_2tap_hold_key to R.string.shortcut_value_perform_long_click_action,
      // Down and to the left stops reading; down and to the right opens Quick Tasks.
      R.string.pref_shortcut_down_and_left_key to R.string.shortcut_value_pause_or_resume_feedback,
      R.string.pref_shortcut_down_and_right_key to R.string.shortcut_value_quick_tasks,
      // Quickly down and up reads the time and date, up and down the status bar.
      R.string.pref_shortcut_down_and_up_key to R.string.shortcut_value_announce_current_time_and_date,
      R.string.pref_shortcut_up_and_down_key to R.string.shortcut_value_announce_status,
      // Two fingers. A tap says where the focus is; a double tap is the app's default action, which
      // answers and ends calls; a double tap and hold ends a call.
      R.string.pref_shortcut_2finger_1tap_key to R.string.shortcut_value_announce_current_title,
      R.string.pref_shortcut_2finger_2tap_key to R.string.shortcut_value_dictate_or_media_control,
      R.string.pref_shortcut_2finger_2tap_hold_key to R.string.shortcut_value_end_call,
      // Two-finger swipe up scrolls toward the top, down toward the bottom.
      R.string.pref_shortcut_2finger_swipe_up_key to R.string.shortcut_value_scroll_back,
      R.string.pref_shortcut_2finger_swipe_down_key to R.string.shortcut_value_scroll_forward,
      // OS 10.2: two fingers down and to the left mute the Screen Reader.
      R.string.pref_shortcut_2finger_down_and_left_key to R.string.shortcut_value_toggle_voice_feedback,
      // Three fingers. Down reads from the top (OS 10.3.0); right and left move to the next and
      // previous page, as for days in the Calendar.
      R.string.pref_shortcut_3finger_swipe_down_key to R.string.shortcut_value_read_from_top,
      R.string.pref_shortcut_3finger_swipe_right_key to R.string.shortcut_value_scroll_left,
      R.string.pref_shortcut_3finger_swipe_left_key to R.string.shortcut_value_scroll_right,
      // Edges, as on BlackBerry 10 without the Screen Reader: up from the bottom minimizes the app,
      // further up shows the apps open; down from the top opens the settings, further the
      // notifications.
      R.string.pref_shortcut_edge_bottom_up_key to R.string.shortcut_value_home,
      R.string.pref_shortcut_edge_bottom_long_up_key to R.string.shortcut_value_overview,
      R.string.pref_shortcut_edge_top_left_down_key to R.string.shortcut_value_quick_settings,
      R.string.pref_shortcut_edge_top_right_down_key to R.string.shortcut_value_quick_settings,
      R.string.pref_shortcut_edge_top_left_long_down_key to R.string.shortcut_value_notifications,
      R.string.pref_shortcut_edge_top_right_long_down_key to R.string.shortcut_value_notifications,
    )

  @Volatile private var byKey: Map<String, String>? = null

  /** Returns the command of the gesture [baseKey] in the BlackBerry layout, or Unassigned. */
  @JvmStatic
  fun defaultValue(context: Context, baseKey: String): String =
    defaults(context)[baseKey] ?: context.getString(R.string.shortcut_value_unassigned)

  /** The BlackBerry layout's commands by gesture preference key. */
  @JvmStatic
  fun defaults(context: Context): Map<String, String> =
    byKey
      ?: DEFAULTS.associate { (key, value) -> context.getString(key) to context.getString(value) }
        .also { byKey = it }
}
