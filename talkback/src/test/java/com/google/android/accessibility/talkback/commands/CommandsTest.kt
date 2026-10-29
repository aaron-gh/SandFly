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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommandsTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Other tests may or may not have done this already, depending on the order they run in.
    FormFactorUtils.initialize(context)
  }

  private fun allCommands() = Commands.categories(context).flatMap { it.commands }

  @Test
  fun everyCommandIsListedOnce() {
    val values = allCommands().map { it.value }
    assertEquals(values.size, values.toSet().size)
  }

  @Test
  fun everyCategoryHasCommands() {
    Commands.categories(context).forEach { assertTrue(it.commands.isNotEmpty()) }
  }

  @Test
  fun everyDefaultGestureActionIsACommand() {
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    val defaults =
      context.resources.getStringArray(R.array.pref_shortcut_keys).mapNotNull { key ->
        val id = context.resources.getIdentifier(key + "_default", "string", context.packageName)
        if (id == 0) null else context.getString(id)
      }
    for (value in defaults.filter { it != unassigned }) {
      assertNotNull("No command for $value", Commands.label(context, value))
    }
  }

  @Test
  fun keyboardOnlyCommandsRunTheirKeyboardAction() {
    val value = Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.SHOW_HEADING_LIST)
    assertEquals(
      TalkBackPhysicalKeyboardShortcut.SHOW_HEADING_LIST,
      Commands.keyboardActionOf(context, value),
    )
    assertNotNull(Commands.find(context, value))
  }

  @Test
  fun gestureCommandsKeepTheirKeyboardAction() {
    val next = Commands.find(context, context.getString(R.string.shortcut_value_next))
    assertEquals(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT, next?.keyboardAction)
    val heading = Commands.find(context, "NEXT_HEADING")
    assertEquals(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_HEADING, heading?.keyboardAction)
  }

  @Test
  fun shortcutKeysNameTheirCommand() {
    val value = context.getString(R.string.shortcut_value_read_notifications)
    val key = Commands.shortcutKey(value)
    assertEquals(value, Commands.valueOfShortcutKey(key))
    assertNull(Commands.valueOfShortcutKey("keycombo_shortcut_global_back"))
    assertEquals(
      context.getString(R.string.shortcut_read_notifications),
      Commands.labelOfKeyboardKey(context, key),
    )
  }

  @Test
  fun keyboardKeysOfLinkedCommandsNameTheCommand() {
    val key = context.getString(R.string.keycombo_shortcut_navigate_next_default)
    assertEquals(context.getString(R.string.shortcut_next), Commands.labelOfKeyboardKey(context, key))
  }

  @Test
  fun keyboardActionsRunTheCommandThatHasThem() {
    assertEquals(
      context.getString(R.string.shortcut_value_next),
      Commands.valueOfKeyboardAction(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT),
    )
    assertEquals(
      Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_BELOW),
      Commands.valueOfKeyboardAction(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_BELOW),
    )
  }
}
