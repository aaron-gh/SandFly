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

package com.google.android.accessibility.talkback.preference

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * The order the user gave the items of a list such as the reading controls or the SandFly menu,
 * saved as their setting keys. Whether an item is in the list stays in its own switch setting.
 */
object ItemOrder {
  /** Setting with the order of the reading controls. */
  const val READING_CONTROLS_KEY = "pref_reading_controls_order"

  /** Setting with the order of the SandFly menu. */
  const val SANDFLY_MENU_KEY = "pref_sandfly_menu_order"

  private const val SEPARATOR = ","

  /** Returns the keys saved under [orderKey], or an empty list when there is no order. */
  @JvmStatic
  fun stored(prefs: SharedPreferences, orderKey: String): List<String> =
    stored(prefs.getString(orderKey, null))

  @JvmStatic
  fun stored(value: String?): List<String> =
    value?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

  /**
   * Returns [defaults] in the saved order. A key that isn't saved, such as one added in a newer
   * version, goes after the key it follows in [defaults]. Saved keys not in [defaults] are left out.
   */
  @JvmStatic
  fun ordered(stored: List<String>, defaults: List<String>): List<String> {
    val known = defaults.toSet()
    val result = stored.filter { it in known }.distinct().toMutableList()
    var previous: String? = null
    for (key in defaults) {
      if (key !in result) {
        val index = previous?.let { result.indexOf(it) + 1 } ?: 0
        result.add(index, key)
      }
      previous = key
    }
    return result
  }

  /** Returns [defaults] in the order saved under [orderKey]. */
  @JvmStatic
  fun ordered(prefs: SharedPreferences, orderKey: String, defaults: List<String>): List<String> =
    ordered(stored(prefs, orderKey), defaults)

  /** Saves [keys] as the order under [orderKey]. */
  @JvmStatic
  fun save(prefs: SharedPreferences, orderKey: String, keys: List<String>) {
    prefs.edit { putString(orderKey, keys.joinToString(SEPARATOR)) }
  }
}
