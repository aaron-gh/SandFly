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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reading controls > Reading control lists: named lists of reading controls, apart from the main
 * list. Each list has its own Next item and Previous item commands, which select a control as the
 * current reading control. Each control is named by its preference key on the Reading controls
 * screen.
 */
object ReadingControlLists {
  private const val KEY = "pref_reading_control_lists"
  // Start and end the value of a list's commands. They must never change once released.
  private const val PREFIX = "READING_CONTROL_LIST:"
  private const val NEXT = ":NEXT"
  private const val PREVIOUS = ":PREVIOUS"

  /** A list: its name and the preference keys of its reading controls. */
  data class ControlList(val id: String, val name: String, val controls: List<String>)

  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  @JvmStatic
  fun valueOf(id: String, next: Boolean): String = PREFIX + id + if (next) NEXT else PREVIOUS

  @JvmStatic
  fun isCommand(value: String?): Boolean =
    value != null && value.startsWith(PREFIX) && (value.endsWith(NEXT) || value.endsWith(PREVIOUS))

  @JvmStatic fun isNext(value: String): Boolean = value.endsWith(NEXT)

  @JvmStatic
  fun idOf(value: String): String =
    value.removePrefix(PREFIX).removeSuffix(NEXT).removeSuffix(PREVIOUS)

  /** The name of the command saved as [value], or null when it or its list is not there. */
  @JvmStatic
  fun label(context: Context, prefs: SharedPreferences, value: String?): String? {
    if (!isCommand(value)) return null
    val list = read(prefs).firstOrNull { it.id == idOf(value!!) } ?: return null
    return commandLabel(context, list.name, isNext(value!!))
  }

  @JvmStatic
  fun commandLabel(context: Context, name: String, next: Boolean): String =
    context.getString(
      if (next) R.string.shortcut_next_in_reading_control_list
      else R.string.shortcut_previous_in_reading_control_list,
      name,
    )

  /** The preference keys of the reading controls in the list [id], or null without that list. */
  @JvmStatic
  fun controls(prefs: SharedPreferences, id: String): List<String>? =
    read(prefs).firstOrNull { it.id == id }?.controls

  @JvmStatic fun read(prefs: SharedPreferences): List<ControlList> = fromJson(prefs.getString(KEY, null))

  @JvmStatic
  fun write(prefs: SharedPreferences, lists: List<ControlList>) {
    prefs.edit().putString(KEY, toJson(lists)).apply()
  }

  internal fun toJson(lists: List<ControlList>): String =
    JSONArray()
      .apply {
        for (list in lists) {
          put(
            JSONObject()
              .put("id", list.id)
              .put("name", list.name)
              .put("controls", JSONArray(list.controls))
          )
        }
      }
      .toString()

  internal fun fromJson(json: String?): List<ControlList> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val controls = item.getJSONArray("controls")
        ControlList(
          item.getString("id"),
          item.getString("name"),
          (0 until controls.length()).map { controls.getString(it) },
        )
      }
    } catch (e: JSONException) {
      emptyList()
    }
  }
}
