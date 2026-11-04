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
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge

/**
 * The settings of the menus along the left and right edges: a switch for each edge, and the
 * commands of its menu in order, one setting for each position.
 */
object EdgeMenus {
  /** The most commands a menu has. */
  const val MAX_ITEMS = 10

  private const val PREFIX = "pref_edge_menu_"

  /** The key of the switch that makes [edge] a menu. */
  @JvmStatic fun switchKey(edge: MenuEdge): String = PREFIX + edge.name.lowercase()

  /** The key of the command at [position], from 1, in the menu along [edge]. */
  @JvmStatic fun itemKey(edge: MenuEdge, position: Int): String = "${switchKey(edge)}_$position"

  /** Whether [key] is one of the edge menus' settings. */
  @JvmStatic fun isKey(key: String?): Boolean = key != null && key.startsWith(PREFIX)

  /** The commands of the menu along [edge] in order, leaving out positions set to [unassigned]. */
  @JvmStatic
  fun items(prefs: SharedPreferences, edge: MenuEdge, unassigned: String): List<String> =
    (1..MAX_ITEMS).mapNotNull { position ->
      prefs.getString(itemKey(edge, position), null)?.takeIf { it.isNotEmpty() && it != unassigned }
    }

  /** Saves [items], at most [MAX_ITEMS] of them, as the menu along [edge], from position 1. */
  @JvmStatic
  fun setItems(prefs: SharedPreferences, edge: MenuEdge, items: List<String>) {
    val editor = prefs.edit()
    for (position in 1..MAX_ITEMS) {
      val item = items.getOrNull(position - 1)
      if (item == null) {
        editor.remove(itemKey(edge, position))
      } else {
        editor.putString(itemKey(edge, position), item)
      }
    }
    editor.apply()
  }

  /** The menu along [edge] as touch uses it: none while its switch is off or it has no commands. */
  @JvmStatic
  fun read(prefs: SharedPreferences, edge: MenuEdge, unassigned: String): Menu =
    if (prefs.getBoolean(switchKey(edge), false)) Menu(items(prefs, edge, unassigned)) else Menu.NONE

  /** The commands of a menu, with their names, which are all looked up when one is first needed. */
  class Menu(private val commands: List<String>) {
    private var names: List<String>? = null

    val size: Int
      get() = commands.size

    /** The command at [index], or null when there is none. */
    fun command(index: Int): String? = commands.getOrNull(index)

    /** The name of the command at [index], as the command lists show it. */
    fun name(context: Context, index: Int): String {
      if (index !in commands.indices) {
        return ""
      }
      val all =
        names
          ?: Commands.labels(context, commands)
            .mapIndexed { i, label -> label ?: commands[i] }
            .also { names = it }
      return all[index]
    }

    companion object {
      @JvmField val NONE = Menu(emptyList())
    }
  }
}
