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

package com.google.android.accessibility.talkback.notificationlist

import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R

/** A part of a notification that Backtalk's notification list speaks. */
enum class NotificationPart(@StringRes val title: Int) {
  TITLE(R.string.notification_part_title),
  TEXT(R.string.notification_part_text),
  APP(R.string.notification_part_app),
  TIME(R.string.notification_part_time),
  PLAYBACK(R.string.notification_part_playback),
}

/**
 * Which parts of a notification are spoken, and in what order. The order holds every part,
 * including turned off ones, so that a part keeps its place when it is turned back on. Parts added
 * in later versions go at the end and are on.
 */
object NotificationParts {
  private const val PREF_ORDER = "pref_notification_part_order"
  private const val PREF_DISABLED = "pref_notification_parts_disabled"
  private const val SEPARATOR = ","

  fun order(prefs: SharedPreferences): List<NotificationPart> {
    val saved = parse(prefs.getString(PREF_ORDER, null))
    return saved + NotificationPart.entries.filterNot { it in saved }
  }

  fun enabledParts(prefs: SharedPreferences): List<NotificationPart> {
    val disabled = parse(prefs.getString(PREF_DISABLED, null))
    return order(prefs).filterNot { it in disabled }
  }

  fun isEnabled(prefs: SharedPreferences, part: NotificationPart): Boolean =
    part !in parse(prefs.getString(PREF_DISABLED, null))

  fun setEnabled(prefs: SharedPreferences, part: NotificationPart, enabled: Boolean) {
    val disabled = parse(prefs.getString(PREF_DISABLED, null)).toMutableList()
    disabled.remove(part)
    if (!enabled) {
      disabled += part
    }
    prefs.edit().putString(PREF_DISABLED, format(disabled)).apply()
  }

  /** Moves a part by [offset] places. Returns its new index, or null if it cannot move. */
  fun move(prefs: SharedPreferences, part: NotificationPart, offset: Int): Int? {
    val order = order(prefs).toMutableList()
    val newIndex = order.indexOf(part) + offset
    if (newIndex !in order.indices) {
      return null
    }
    order.remove(part)
    order.add(newIndex, part)
    prefs.edit().putString(PREF_ORDER, format(order)).apply()
    return newIndex
  }

  private fun parse(value: String?): List<NotificationPart> =
    value
      .orEmpty()
      .split(SEPARATOR)
      .mapNotNull { name -> NotificationPart.entries.firstOrNull { it.name == name } }
      .distinct()

  private fun format(parts: List<NotificationPart>): String =
    parts.joinToString(SEPARATOR) { it.name }
}
