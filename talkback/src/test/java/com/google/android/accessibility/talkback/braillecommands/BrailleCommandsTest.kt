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
package com.google.android.accessibility.talkback.braillecommands

import android.content.Context
import com.google.android.accessibility.talkback.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleCommandsTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = context.getSharedPreferences("braille_commands_test", Context.MODE_PRIVATE)
  private val next = context.getString(R.string.shortcut_value_next)
  private val previous = context.getString(R.string.shortcut_value_previous)
  private val home = context.getString(R.string.shortcut_value_home)

  @After
  fun forget() {
    prefs.edit().clear().apply()
  }

  @Test
  fun builtInCellsApplyUntilDefaultIsChanged() {
    assertEquals(next, BrailleCommands.action(context, prefs, "4", null))
    assertEquals(previous, BrailleCommands.action(context, prefs, "1", null))
    assertNull(BrailleCommands.action(context, prefs, "3", null))
  }

  @Test
  fun builtInCellsAreUniqueAndValid() {
    val cells = BrailleCommands.builtIn(context).keys
    assertTrue(cells.all { BrailleCommands.normalize(it) == it })
  }

  @Test
  fun assigningKeepsTheOtherBuiltInCells() {
    BrailleCommands.assign(context, prefs, BrailleCommands.DEFAULT_SET, "3", home)
    assertEquals(home, BrailleCommands.action(context, prefs, "3", null))
    assertEquals(next, BrailleCommands.action(context, prefs, "4", null))
  }

  @Test
  fun collisionNamesTheCommandTheCellRunsInItsSet() {
    assertEquals(next, BrailleCommands.collision(context, prefs, BrailleCommands.DEFAULT_SET, "4"))
    assertNull(BrailleCommands.collision(context, prefs, BrailleCommands.DEFAULT_SET, "3"))
    // An app's set starts empty: Default's cells are no collision there.
    assertNull(BrailleCommands.collision(context, prefs, BrailleCommands.appSet("app"), "4"))
  }

  @Test
  fun profileComesBeforeAppAndAppBeforeDefault() {
    BrailleCommands.assign(context, prefs, BrailleCommands.appSet("app"), "4", home)
    assertEquals(home, BrailleCommands.action(context, prefs, "4", "app"))
    assertEquals(next, BrailleCommands.action(context, prefs, "4", "other"))

    val config = BrailleCommands.read(prefs)
    BrailleCommands.write(
      prefs,
      config.copy(profiles = listOf(BrailleCommands.Profile("p", "Reading", mapOf("4" to previous)))),
    )
    assertEquals(home, BrailleCommands.action(context, prefs, "4", "app"))
    BrailleCommands.select(context, prefs, "p")
    assertEquals(previous, BrailleCommands.action(context, prefs, "4", "app"))
    // Cells the profile leaves out run what they run without it.
    assertEquals(previous, BrailleCommands.action(context, prefs, "1", "app"))
  }

  @Test
  fun profilesCycleWithDefaultFirst() {
    BrailleCommands.write(
      prefs,
      BrailleCommands.Config(
        null,
        emptyMap(),
        listOf(
          BrailleCommands.Profile("a", "A", emptyMap()),
          BrailleCommands.Profile("b", "B", emptyMap()),
        ),
      ),
    )
    assertEquals("a", BrailleCommands.adjacentId(prefs, next = true))
    BrailleCommands.select(context, prefs, "b")
    assertEquals("", BrailleCommands.adjacentId(prefs, next = true))
    assertEquals("a", BrailleCommands.adjacentId(prefs, next = false))
    assertNull(BrailleCommands.select(context, prefs, "deleted"))
  }

  @Test
  fun profileCommandsNameTheirProfile() {
    BrailleCommands.write(
      prefs,
      BrailleCommands.Config(null, emptyMap(), listOf(BrailleCommands.Profile("a", "Reading", emptyMap()))),
    )
    val value = BrailleCommands.valueOf("a")
    assertTrue(BrailleCommands.isAction(value))
    assertEquals("a", BrailleCommands.idOf(value))
    assertEquals(
      context.getString(R.string.shortcut_use_braille_command_profile, "Reading"),
      BrailleCommands.label(context, prefs, value),
    )
    assertNull(BrailleCommands.label(context, prefs, BrailleCommands.valueOf("gone")))
    assertFalse(BrailleCommands.isAction(next))
  }

  @Test
  fun removingACommandTakesItOffEveryCell() {
    BrailleCommands.assign(context, prefs, BrailleCommands.appSet("app"), "36", next)
    BrailleCommands.removeCommand(context, prefs, next)
    assertNull(BrailleCommands.action(context, prefs, "4", null))
    assertNull(BrailleCommands.action(context, prefs, "36", "app")?.takeIf { it == next })
    assertEquals(previous, BrailleCommands.action(context, prefs, "1", null))
  }

  @Test
  fun resetBringsBackBuiltInCells() {
    BrailleCommands.remove(context, prefs, BrailleCommands.DEFAULT_SET, "4")
    assertNull(BrailleCommands.action(context, prefs, "4", null))
    BrailleCommands.resetDefaults(prefs)
    assertEquals(next, BrailleCommands.action(context, prefs, "4", null))
  }

  @Test
  fun savedCellsAreReadBackInDotOrder() {
    val json = """{"default":{"41":"NEXT","x":"HOME","":"BACK"},"apps":{},"profiles":[]}"""
    val config = BrailleCommands.fromJson(json)
    assertEquals(mapOf("14" to "NEXT"), config.defaults)
    assertEquals(config, BrailleCommands.fromJson(BrailleCommands.toJson(config)))
  }

  @Test
  fun cellsOfACommandAreInOrder() {
    BrailleCommands.assign(context, prefs, BrailleCommands.DEFAULT_SET, "25", next)
    assertEquals(listOf("25", "4"), BrailleCommands.cellsOf(context, prefs, next))
  }
}
