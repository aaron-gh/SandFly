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
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
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
class EdgeMenusTest {
  private val prefs =
    RuntimeEnvironment.getApplication().getSharedPreferences("edge_menus_test", Context.MODE_PRIVATE)

  @Test
  fun menuKeepsTwentyCommandsAndDropsMore() {
    prefs.edit().clear().commit()
    val commands = (1..21).map { "COMMAND_$it" }
    EdgeMenus.setItems(prefs, MenuEdge.RIGHT, commands)
    assertEquals(20, EdgeMenus.MAX_ITEMS)
    assertEquals(commands.take(20), EdgeMenus.items(prefs, MenuEdge.RIGHT, "UNASSIGNED"))
    assertNull(prefs.getString(EdgeMenus.itemKey(MenuEdge.RIGHT, 21), null))
  }

  @Test
  fun shorterMenuRemovesTheLaterPositions() {
    prefs.edit().clear().commit()
    EdgeMenus.setItems(prefs, MenuEdge.LEFT, (1..20).map { "COMMAND_$it" })
    EdgeMenus.setItems(prefs, MenuEdge.LEFT, listOf("COMMAND_1", "COMMAND_2"))
    assertEquals(listOf("COMMAND_1", "COMMAND_2"), EdgeMenus.items(prefs, MenuEdge.LEFT, "UNASSIGNED"))
    assertNull(prefs.getString(EdgeMenus.itemKey(MenuEdge.LEFT, 20), null))
  }

  @Test
  fun horizontalSwitchIsReadWithTheMenu() {
    prefs.edit().clear().commit()
    EdgeMenus.setItems(prefs, MenuEdge.LEFT, listOf("COMMAND_1"))
    prefs
      .edit()
      .putBoolean(EdgeMenus.switchKey(MenuEdge.LEFT), true)
      .putBoolean(EdgeMenus.horizontalKey(MenuEdge.LEFT), true)
      .commit()
    EdgeMenus.setItems(prefs, MenuEdge.LEFT, listOf("COMMAND_1", "COMMAND_2"))
    assertTrue(EdgeMenus.isKey(EdgeMenus.horizontalKey(MenuEdge.LEFT)))
    val left = EdgeMenus.read(prefs, MenuEdge.LEFT, "UNASSIGNED")
    assertTrue(left.horizontal)
    assertEquals(2, left.size)
    assertFalse(EdgeMenus.read(prefs, MenuEdge.RIGHT, "UNASSIGNED").horizontal)
  }
}
