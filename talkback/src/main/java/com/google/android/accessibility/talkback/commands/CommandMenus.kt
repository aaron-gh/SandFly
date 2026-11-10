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

package com.google.android.accessibility.talkback.commands

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.ItemOrder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The menus made of commands: the SandFly menu and the custom menus. A menu holds commands and
 * submenus, which hold the same, to [MAX_DEPTH] levels.
 *
 * Until the SandFly menu is first changed here, it comes from the old switches and saved order,
 * which watches still edit.
 */
object CommandMenus {
  /** A command or a submenu in a menu. */
  sealed class Node

  /** The command saved as [value]. */
  data class Command(val value: String) : Node()

  /** A submenu. [id] is unique among all the menus. */
  data class Submenu(val id: String, val name: String, val children: List<Node>) : Node()

  /** A custom menu. */
  data class Menu(val id: String, val name: String, val items: List<Node>)

  /** A list of items: the top of the menu [menuId], or the submenu [parent] in it. */
  data class Place(val menuId: String, val parent: String?)

  /** A place to move an item to, and its name, with the submenus above it. */
  data class Destination(val place: Place, val path: String)

  /** The ID of the SandFly menu. */
  const val SANDFLY_MENU = "sandfly"

  /** The most levels of submenus kept and shown. */
  const val MAX_DEPTH = 12

  private const val TREE_KEY = "pref_sandfly_menu_tree"
  private const val CUSTOM_KEY = "pref_custom_menus"
  private const val COMMAND_PREFIX = "MENU:"
  private const val PATH_SEPARATOR = " > "

  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  /** Returns the value of the command that opens the custom menu [id]. */
  @JvmStatic fun commandOf(id: String): String = COMMAND_PREFIX + id

  /** Returns whether [value] is the command of a custom menu. */
  @JvmStatic fun isCommand(value: String?): Boolean = value != null && value.startsWith(COMMAND_PREFIX)

  @JvmStatic fun idOf(value: String): String = value.removePrefix(COMMAND_PREFIX)

  /** Returns the name of the custom menu whose command is [value], or null. */
  @JvmStatic
  fun label(prefs: SharedPreferences, value: String?): String? =
    if (isCommand(value)) custom(prefs).firstOrNull { it.id == idOf(value!!) }?.name else null

  /** Returns the custom menus. */
  @JvmStatic fun custom(prefs: SharedPreferences): List<Menu> = customFromJson(prefs.getString(CUSTOM_KEY, null))

  @JvmStatic
  fun writeCustom(prefs: SharedPreferences, menus: List<Menu>) {
    prefs.edit { putString(CUSTOM_KEY, customToJson(menus)) }
  }

  /** Returns the top of the menu [menuId], or an empty list when there is no such menu. */
  @JvmStatic
  fun items(context: Context, prefs: SharedPreferences, menuId: String): List<Node> =
    if (menuId == SANDFLY_MENU) sandflyItems(context, prefs)
    else custom(prefs).firstOrNull { it.id == menuId }?.items.orEmpty()

  /** Returns the list at [place], or an empty list when it is gone. */
  fun children(context: Context, prefs: SharedPreferences, place: Place): List<Node> =
    childrenOf(items(context, prefs, place.menuId), place.parent).orEmpty()

  /** Replaces the list at [place]. */
  fun setChildren(context: Context, prefs: SharedPreferences, place: Place, children: List<Node>) {
    write(context, prefs, place.menuId, withChildren(items(context, prefs, place.menuId), place.parent, children))
  }

  /** Puts the SandFly menu back to its default items, in the default order. */
  fun resetSandfly(context: Context, prefs: SharedPreferences) {
    prefs.edit {
      remove(TREE_KEY)
      remove(ItemOrder.SANDFLY_MENU_KEY)
      for (row in MenuCommands.rows(context)) {
        if (row.showKey != 0) remove(context.getString(row.showKey))
      }
    }
  }

  private fun write(context: Context, prefs: SharedPreferences, menuId: String, items: List<Node>) {
    if (menuId != SANDFLY_MENU) {
      writeCustom(prefs, custom(prefs).map { if (it.id == menuId) it.copy(items = items) else it })
      return
    }
    val rows = MenuCommands.rows(context)
    val json = JSONObject().put("seen", JSONArray(rows.map { it.value })).put("items", nodesToJson(items))
    val present = commandsIn(items)
    prefs.edit {
      putString(TREE_KEY, json.toString())
      // The hint that actions are available still follows these.
      for (row in rows) {
        if (row.showKey != 0) putBoolean(context.getString(row.showKey), row.value in present)
      }
    }
  }

  private fun sandflyItems(context: Context, prefs: SharedPreferences): List<Node> {
    val rows = MenuCommands.rows(context)
    val saved = prefs.getString(TREE_KEY, null) ?: return defaults(context, prefs)
    return try {
      val json = JSONObject(saved)
      val seen = json.getJSONArray("seen").let { a -> (0 until a.length()).map { a.getString(it) } }.toSet()
      val fresh = rows.filter { it.value !in seen && isDefault(context, prefs, it) }.map { it.value }
      withNewDefaults(nodesFromJson(json.getJSONArray("items")), rows.map { it.value }, fresh)
    } catch (e: JSONException) {
      defaults(context, prefs)
    }
  }

  private fun isDefault(context: Context, prefs: SharedPreferences, row: MenuCommands.Row): Boolean =
    row.showKey == 0 ||
      prefs.getBoolean(context.getString(row.showKey), context.resources.getBoolean(row.showDefault))

  private fun defaults(context: Context, prefs: SharedPreferences): List<Node> {
    val rows = MenuCommands.rows(context)
    val byKey = rows.associateBy { it.key }
    return ItemOrder.ordered(prefs, ItemOrder.SANDFLY_MENU_KEY, rows.map { it.key })
      .mapNotNull { byKey[it] }
      .filter { isDefault(context, prefs, it) }
      .map { Command(it.value) }
  }

  /** Adds each of [fresh], commands of a newer version, after the one before it in [order]. */
  internal fun withNewDefaults(nodes: List<Node>, order: List<String>, fresh: List<String>): List<Node> {
    val result = nodes.toMutableList()
    for (value in fresh) {
      val before = order.getOrNull(order.indexOf(value) - 1)
      val index = result.indexOf(Command(before.orEmpty()))
      result.add(if (index < 0) result.size else index + 1, Command(value))
    }
    return result
  }

  /** Returns the children of the submenu [parent], or [nodes] for null, or null when it is gone. */
  internal fun childrenOf(nodes: List<Node>, parent: String?): List<Node>? {
    if (parent == null) return nodes
    for (node in nodes) {
      if (node !is Submenu) continue
      if (node.id == parent) return node.children
      childrenOf(node.children, parent)?.let {
        return it
      }
    }
    return null
  }

  /** Returns [nodes] with the children of the submenu [parent], or the top for null, replaced. */
  internal fun withChildren(nodes: List<Node>, parent: String?, children: List<Node>): List<Node> =
    if (parent == null) {
      children
    } else {
      nodes.map { node ->
        when {
          node !is Submenu -> node
          node.id == parent -> node.copy(children = children)
          else -> node.copy(children = withChildren(node.children, parent, children))
        }
      }
    }

  /** Returns the commands in [nodes] and their submenus. */
  @JvmStatic
  fun commandsIn(nodes: List<Node>): Set<String> {
    val found = LinkedHashSet<String>()
    fun add(list: List<Node>) {
      for (node in list) {
        when (node) {
          is Command -> found += node.value
          is Submenu -> add(node.children)
        }
      }
    }
    add(nodes)
    return found
  }

  /**
   * Returns [nodes] with each custom menu's command replaced by that menu as a submenu. A menu
   * inside itself is left out.
   */
  @JvmStatic
  fun expanded(prefs: SharedPreferences, nodes: List<Node>): List<Node> =
    expanded(nodes, custom(prefs).associateBy { it.id }, emptySet(), 0)

  /** Returns the custom menu [menuId] with custom menus in it expanded, or null without it. */
  @JvmStatic
  fun expandedMenu(prefs: SharedPreferences, menuId: String): Menu? {
    val menus = custom(prefs).associateBy { it.id }
    val menu = menus[menuId] ?: return null
    return menu.copy(items = expanded(menu.items, menus, setOf(menuId), 0))
  }

  private fun expanded(
    nodes: List<Node>,
    menus: Map<String, Menu>,
    open: Set<String>,
    depth: Int,
  ): List<Node> {
    if (depth >= MAX_DEPTH) return emptyList()
    return nodes.mapNotNull { node ->
      when {
        node is Submenu -> node.copy(children = expanded(node.children, menus, open, depth + 1))
        node is Command && isCommand(node.value) -> {
          val id = idOf(node.value)
          val menu = menus[id]
          if (menu == null || id in open) null
          else Submenu(id, menu.name, expanded(menu.items, menus, open + id, depth + 1))
        }
        else -> node
      }
    }
  }

  /** Returns the places [moving], now at [from], can go, leaving out [from] and [moving] itself. */
  fun destinations(
    context: Context,
    prefs: SharedPreferences,
    from: Place,
    moving: Node,
  ): List<Destination> {
    val result = ArrayList<Destination>()
    fun add(menuId: String, nodes: List<Node>, parent: String?, path: String) {
      val place = Place(menuId, parent)
      if (place != from) result += Destination(place, path)
      for (node in nodes) {
        if (node is Submenu && node != moving) add(menuId, node.children, node.id, path + PATH_SEPARATOR + node.name)
      }
    }
    add(SANDFLY_MENU, sandflyItems(context, prefs), null, context.getString(R.string.title_pref_category_context_menu))
    for (menu in custom(prefs)) {
      add(menu.id, menu.items, null, menu.name)
    }
    return result
  }

  /**
   * Moves the item at [index] of [from] to the end of [to]. A command already at [to] isn't added
   * there twice. Returns false when the item is gone.
   */
  fun moveTo(context: Context, prefs: SharedPreferences, from: Place, index: Int, to: Place): Boolean {
    val source = children(context, prefs, from)
    val node = source.getOrNull(index) ?: return false
    setChildren(context, prefs, from, source.filterIndexed { i, _ -> i != index })
    val target = children(context, prefs, to)
    if (node !in target) {
      setChildren(context, prefs, to, target + node)
    }
    return true
  }

  internal fun nodesToJson(nodes: List<Node>): JSONArray =
    JSONArray().apply {
      for (node in nodes) {
        put(
          when (node) {
            is Command -> JSONObject().put("command", node.value)
            is Submenu ->
              JSONObject().put("id", node.id).put("name", node.name).put("items", nodesToJson(node.children))
          }
        )
      }
    }

  // Stops at MAX_DEPTH, as settings can be restored from a file.
  internal fun nodesFromJson(array: JSONArray, depth: Int = 0): List<Node> {
    if (depth >= MAX_DEPTH) return emptyList()
    return (0 until array.length()).mapNotNull { index ->
      val item = array.getJSONObject(index)
      when {
        item.has("command") -> Command(item.getString("command"))
        item.has("items") ->
          Submenu(item.getString("id"), item.getString("name"), nodesFromJson(item.getJSONArray("items"), depth + 1))
        else -> null
      }
    }
  }

  internal fun customToJson(menus: List<Menu>): String =
    JSONArray()
      .apply {
        for (menu in menus) {
          put(JSONObject().put("id", menu.id).put("name", menu.name).put("items", nodesToJson(menu.items)))
        }
      }
      .toString()

  internal fun customFromJson(json: String?): List<Menu> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        Menu(item.getString("id"), item.getString("name"), nodesFromJson(item.getJSONArray("items")))
      }
    } catch (e: JSONException) {
      emptyList()
    }
  }
}
