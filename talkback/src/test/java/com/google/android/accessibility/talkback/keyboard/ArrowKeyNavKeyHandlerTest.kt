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

package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_N
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArrowKeyNavKeyHandlerTest {
  private lateinit var context: Context
  private lateinit var handler: ArrowKeyNavKeyHandler
  private val performed = mutableListOf<String>()
  private var editingText = false
  private var time = 1_000L

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    ArrowKeyNav.reset(context)
    ArrowKeyNav.setOn(context, false)
    handler =
      ArrowKeyNavKeyHandler(
        context,
        object : ArrowKeyNavKeyHandler.Callbacks {
          override fun perform(command: String) {
            performed += command
            if (command == ArrowKeyNav.TOGGLE) {
              ArrowKeyNav.setOn(context, !ArrowKeyNav.isOn(context))
            }
          }

          override fun isEditingText() = editingText
        },
      )
  }

  private fun down(keyCode: Int, metaState: Int = 0): Boolean {
    time += 10
    return handler.onKeyEvent(
      KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, metaState),
      /* canAct= */ true,
    )
  }

  private fun up(keyCode: Int, metaState: Int = 0): Boolean {
    time += 10
    return handler.onKeyEvent(
      KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, metaState),
      /* canAct= */ true,
    )
  }

  @Test
  fun off_arrowsPass() {
    assertFalse(down(KEYCODE_DPAD_RIGHT))
    assertFalse(up(KEYCODE_DPAD_RIGHT))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun off_leftAndRightTogether_turnsOn() {
    assertFalse(down(KEYCODE_DPAD_LEFT))
    assertTrue(down(KEYCODE_DPAD_RIGHT))
    assertTrue(up(KEYCODE_DPAD_RIGHT))
    // The left arrow's down went to the app, so its up does too.
    assertFalse(up(KEYCODE_DPAD_LEFT))
    assertEquals(listOf(ArrowKeyNav.TOGGLE), performed)
    assertTrue(ArrowKeyNav.isOn(context))
  }

  @Test
  fun on_rightAlone_nextItemOnRelease() {
    ArrowKeyNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_RIGHT))
    assertTrue(performed.isEmpty())
    assertTrue(up(KEYCODE_DPAD_RIGHT))
    assertEquals(listOf("NEXT"), performed)
  }

  @Test
  fun on_upAndDownTogether_clicksOnly() {
    ArrowKeyNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_UP))
    assertTrue(down(KEYCODE_DPAD_DOWN))
    assertTrue(up(KEYCODE_DPAD_UP))
    assertTrue(up(KEYCODE_DPAD_DOWN))
    assertEquals(listOf("PERFORM_CLICK_ACTION"), performed)
  }

  @Test
  fun on_rotorKeys_eitherOrder() {
    ArrowKeyNav.setOn(context, true)
    down(KEYCODE_DPAD_UP)
    down(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_UP)
    down(KEYCODE_DPAD_LEFT)
    down(KEYCODE_DPAD_UP)
    up(KEYCODE_DPAD_UP)
    up(KEYCODE_DPAD_LEFT)
    assertEquals(
      listOf("SELECT_NEXT_SETTING", "SELECT_PREVIOUS_SETTING"),
      performed,
    )
  }

  @Test
  fun on_modifierCommands_actOnPress() {
    ArrowKeyNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_UP, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON))
    assertEquals(listOf("FIRST_IN_SCREEN"), performed)
    assertTrue(up(KEYCODE_DPAD_UP, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON))
    assertTrue(down(KEYCODE_DPAD_DOWN, KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON))
    assertEquals(listOf("FIRST_IN_SCREEN", "SCROLL_FORWARD"), performed)
  }

  @Test
  fun on_unassignedKeysPass() {
    ArrowKeyNav.setOn(context, true)
    assertFalse(down(KEYCODE_N))
    assertFalse(up(KEYCODE_N))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun editingText_singleArrowsPass_toggleWorks() {
    ArrowKeyNav.setOn(context, true)
    editingText = true
    assertFalse(down(KEYCODE_DPAD_LEFT))
    assertTrue(performed.isEmpty())
    assertTrue(down(KEYCODE_DPAD_RIGHT))
    up(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_LEFT)
    assertEquals(listOf(ArrowKeyNav.TOGGLE), performed)
    assertFalse(ArrowKeyNav.isOn(context))
  }

  @Test
  fun arrowKeysRunTheCommandAssignedToThem() {
    ArrowKeyNav.setOn(context, true)
    prefs().edit().putString(ArrowKeyNav.prefKey(ArrowKeys.RIGHT), "READ_FROM_TOP").apply()
    down(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_RIGHT)
    assertEquals(listOf("READ_FROM_TOP"), performed)
  }

  @Test
  fun unassignedArrowKeysPass() {
    ArrowKeyNav.setOn(context, true)
    prefs().edit().putString(ArrowKeyNav.prefKey(ArrowKeys.RIGHT), ArrowKeyNav.UNASSIGNED).apply()
    assertFalse(down(KEYCODE_DPAD_RIGHT))
    assertFalse(up(KEYCODE_DPAD_RIGHT))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun twoKeys_storedInEitherOrder() {
    assertEquals(
      ArrowKeyNav.keys(0, KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN),
      ArrowKeyNav.keys(0, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_UP),
    )
    assertEquals(
      ArrowKeys.UP_DOWN,
      ArrowKeyNav.arrowKeysFor(ArrowKeyNav.keys(0, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_UP)),
    )
  }

  @Test
  fun migrate_movesSavedKeysToArrowKeys() {
    val prefs = prefs()
    // Next item had been moved to Control + Right arrow.
    prefs
      .edit()
      .putLong("arrow_key_nav_keys_next_item", ArrowKeyNav.keys(KeyEvent.META_CTRL_ON, KEYCODE_DPAD_RIGHT))
      .apply()
    ArrowKeyNav.migrate(prefs)
    assertEquals("NEXT", ArrowKeyNav.commandFor(context, ArrowKeys.CTRL_RIGHT))
    assertEquals(ArrowKeyNav.UNASSIGNED, ArrowKeyNav.commandFor(context, ArrowKeys.RIGHT))
    assertEquals("PREVIOUS", ArrowKeyNav.commandFor(context, ArrowKeys.LEFT))
    assertFalse(prefs.contains("arrow_key_nav_keys_next_item"))
  }

  private fun prefs() = SharedPreferencesUtils.getSharedPreferences(context)
}
