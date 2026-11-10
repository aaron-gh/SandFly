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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandMenus.Command
import com.google.android.accessibility.talkback.commands.CommandMenus.Menu
import com.google.android.accessibility.talkback.commands.CommandMenus.Place
import com.google.android.accessibility.talkback.commands.CommandMenus.Submenu
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.utils.FormFactorUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommandMenusTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences
  private val top = Place(CommandMenus.SANDFLY_MENU, null)

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    prefs = context.getSharedPreferences("command_menus_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  private fun sandfly() = CommandMenus.items(context, prefs, CommandMenus.SANDFLY_MENU)

  private fun readFromTop() = context.getString(R.string.shortcut_value_read_from_top)

  private fun readFromCurrent() = context.getString(R.string.shortcut_value_read_from_current)

  @Test
  fun defaultMenuFollowsTheDefaultsOfTheOldSwitches() {
    val expected =
      MenuCommands.rows(context)
        .filter { it.showKey == 0 || context.resources.getBoolean(it.showDefault) }
        .map { Command(it.value) }
    assertEquals(expected, sandfly())
  }

  @Test
  fun oldSwitchesAndOrderCarryOver() {
    val rows = MenuCommands.rows(context)
    val off = rows.first { it.value == readFromTop() }
    val first = rows.first { it.value == readFromCurrent() }
    prefs.edit().putBoolean(context.getString(off.showKey), false).commit()
    ItemOrder.save(prefs, ItemOrder.SANDFLY_MENU_KEY, listOf(first.key) + rows.map { it.key }.filter { it != first.key })
    val menu = sandfly()
    assertEquals(Command(first.value), menu.first())
    assertFalse(Command(off.value) in menu)
  }

  @Test
  fun savedTreeIsReadBackAndKeepsTheOldSwitchesInStep() {
    val tree = listOf(Submenu("s", "Reading", listOf(Command(readFromTop()))), Command("X"))
    CommandMenus.setChildren(context, prefs, top, tree)
    assertEquals(tree, sandfly())
    val row = MenuCommands.rows(context).first { it.value == readFromTop() }
    val other = MenuCommands.rows(context).first { it.value == readFromCurrent() }
    assertTrue(prefs.getBoolean(context.getString(row.showKey), false))
    assertFalse(prefs.getBoolean(context.getString(other.showKey), true))
  }

  @Test
  fun aDefaultItemOfANewerVersionIsAddedAfterTheOneBeforeIt() {
    assertEquals(
      listOf(Command("a"), Command("b"), Command("z")),
      CommandMenus.withNewDefaults(listOf(Command("a"), Command("z")), listOf("a", "b", "c"), listOf("b")),
    )
    assertEquals(
      listOf(Command("z"), Command("b")),
      CommandMenus.withNewDefaults(listOf(Command("z")), listOf("a", "b"), listOf("b")),
    )
  }

  @Test
  fun resetRestoresTheDefaults() {
    val defaults = sandfly()
    CommandMenus.setChildren(context, prefs, top, listOf(Command("X")))
    CommandMenus.resetSandfly(context, prefs)
    assertEquals(defaults, sandfly())
  }

  @Test
  fun itemsMoveBetweenMenusAndSubmenus() {
    val menu = Menu("m", "AI", listOf(Submenu("deep", "Deep", emptyList()), Command("a")))
    CommandMenus.writeCustom(prefs, listOf(menu))
    val from = Place("m", null)
    assertTrue(CommandMenus.moveTo(context, prefs, from, 1, Place("m", "deep")))
    assertEquals(
      listOf(Submenu("deep", "Deep", listOf(Command("a")))),
      CommandMenus.items(context, prefs, "m"),
    )
    CommandMenus.setChildren(context, prefs, top, listOf(Command("b")))
    assertTrue(CommandMenus.moveTo(context, prefs, top, 0, Place("m", "deep")))
    assertEquals(emptyList<CommandMenus.Node>(), sandfly())
    assertEquals(
      listOf(Command("a"), Command("b")),
      CommandMenus.children(context, prefs, Place("m", "deep")),
    )
  }

  @Test
  fun aCommandAlreadyThereIsNotAddedTwice() {
    CommandMenus.writeCustom(prefs, listOf(Menu("m", "AI", listOf(Command("a")))))
    CommandMenus.setChildren(context, prefs, top, listOf(Command("a")))
    assertTrue(CommandMenus.moveTo(context, prefs, top, 0, Place("m", null)))
    assertEquals(listOf(Command("a")), CommandMenus.items(context, prefs, "m"))
  }

  @Test
  fun aSubmenuCannotMoveIntoItself() {
    val inner = Submenu("inner", "Inner", emptyList())
    val outer = Submenu("outer", "Outer", listOf(inner))
    CommandMenus.writeCustom(prefs, listOf(Menu("m", "AI", listOf(outer))))
    val places = CommandMenus.destinations(context, prefs, Place("m", null), outer).map { it.place }
    assertFalse(Place("m", "outer") in places)
    assertFalse(Place("m", "inner") in places)
    assertFalse(Place("m", null) in places)
    assertTrue(top in places)
  }

  @Test
  fun customMenusExpandAndStopAtACycle() {
    val a = Menu("a", "A", listOf(Command(CommandMenus.commandOf("b")), Command("x")))
    val b = Menu("b", "B", listOf(Command(CommandMenus.commandOf("a")), Command("y")))
    CommandMenus.writeCustom(prefs, listOf(a, b))
    assertEquals(
      listOf(Submenu("b", "B", listOf(Command("y"))), Command("x")),
      CommandMenus.expandedMenu(prefs, "a")!!.items,
    )
  }

  @Test
  fun depthIsLimitedWhenReadingAFile() {
    var json = JSONArray().put(JSONObject().put("command", "x"))
    repeat(100) {
      json = JSONArray().put(JSONObject().put("id", "i$it").put("name", "n").put("items", json))
    }
    var depth = 0
    var nodes = CommandMenus.nodesFromJson(json)
    while (nodes.isNotEmpty()) {
      depth++
      nodes = (nodes[0] as Submenu).children
    }
    assertEquals(CommandMenus.MAX_DEPTH, depth)
  }

  @Test
  fun badSavedTextGivesNoMenus() {
    assertEquals(emptyList<Menu>(), CommandMenus.customFromJson("{not json"))
    prefs.edit().putString("pref_sandfly_menu_tree", "[]").commit()
    assertNotNull(sandfly())
  }

  @Test
  fun customMenusAreCommandsInACategory() {
    CommandMenus.writeCustom(prefs, listOf(Menu("m", "Frequent", emptyList())))
    assertEquals("Frequent", CommandMenus.label(prefs, CommandMenus.commandOf("m")))
  }

  @Test
  fun menuOnlyCommandsHaveNames() {
    val unavailable = setOf(context.getString(R.string.shortcut_value_system_actions_menu), context.getString(R.string.shortcut_value_toggle_vibration))
    for (row in MenuCommands.rows(context).filter { it.menuOnly && it.value !in unavailable }) {
      assertNotNull("No command for ${row.value}", Commands.label(context, row.value))
    }
  }
}
