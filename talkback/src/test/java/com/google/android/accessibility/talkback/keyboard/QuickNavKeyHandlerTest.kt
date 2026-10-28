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
class QuickNavKeyHandlerTest {
  private lateinit var context: Context
  private lateinit var handler: QuickNavKeyHandler
  private val performed = mutableListOf<QuickNavCommand>()
  private var editingText = false
  private var time = 1_000L

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    QuickNav.reset(context)
    QuickNav.setOn(context, false)
    handler =
      QuickNavKeyHandler(
        context,
        object : QuickNavKeyHandler.Callbacks {
          override fun perform(command: QuickNavCommand) {
            performed += command
            if (command == QuickNavCommand.TOGGLE) {
              QuickNav.setOn(context, !QuickNav.isOn(context))
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
    assertEquals(listOf(QuickNavCommand.TOGGLE), performed)
    assertTrue(QuickNav.isOn(context))
  }

  @Test
  fun on_rightAlone_nextItemOnRelease() {
    QuickNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_RIGHT))
    assertTrue(performed.isEmpty())
    assertTrue(up(KEYCODE_DPAD_RIGHT))
    assertEquals(listOf(QuickNavCommand.NEXT_ITEM), performed)
  }

  @Test
  fun on_upAndDownTogether_clicksOnly() {
    QuickNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_UP))
    assertTrue(down(KEYCODE_DPAD_DOWN))
    assertTrue(up(KEYCODE_DPAD_UP))
    assertTrue(up(KEYCODE_DPAD_DOWN))
    assertEquals(listOf(QuickNavCommand.CLICK), performed)
  }

  @Test
  fun on_rotorKeys_eitherOrder() {
    QuickNav.setOn(context, true)
    down(KEYCODE_DPAD_UP)
    down(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_UP)
    down(KEYCODE_DPAD_LEFT)
    down(KEYCODE_DPAD_UP)
    up(KEYCODE_DPAD_UP)
    up(KEYCODE_DPAD_LEFT)
    assertEquals(
      listOf(QuickNavCommand.NEXT_READING_CONTROL, QuickNavCommand.PREVIOUS_READING_CONTROL),
      performed,
    )
  }

  @Test
  fun on_modifierCommands_actOnPress() {
    QuickNav.setOn(context, true)
    assertTrue(down(KEYCODE_DPAD_UP, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON))
    assertEquals(listOf(QuickNavCommand.FIRST_ITEM), performed)
    assertTrue(up(KEYCODE_DPAD_UP, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON))
    assertTrue(down(KEYCODE_DPAD_DOWN, KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON))
    assertEquals(listOf(QuickNavCommand.FIRST_ITEM, QuickNavCommand.PAGE_DOWN), performed)
  }

  @Test
  fun on_unassignedKeysPass() {
    QuickNav.setOn(context, true)
    assertFalse(down(KEYCODE_N))
    assertFalse(up(KEYCODE_N))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun editingText_singleArrowsPass_toggleWorks() {
    QuickNav.setOn(context, true)
    editingText = true
    assertFalse(down(KEYCODE_DPAD_LEFT))
    assertTrue(performed.isEmpty())
    assertTrue(down(KEYCODE_DPAD_RIGHT))
    up(KEYCODE_DPAD_RIGHT)
    up(KEYCODE_DPAD_LEFT)
    assertEquals(listOf(QuickNavCommand.TOGGLE), performed)
    assertFalse(QuickNav.isOn(context))
  }

  @Test
  fun reassignedKeys() {
    QuickNav.setOn(context, true)
    QuickNav.save(context, QuickNavCommand.NEXT_ITEM, QuickNav.keys(0, KEYCODE_N))
    assertTrue(down(KEYCODE_N))
    assertEquals(listOf(QuickNavCommand.NEXT_ITEM), performed)
    up(KEYCODE_N)
    // Right is no longer assigned alone, so it goes to the app.
    assertFalse(down(KEYCODE_DPAD_RIGHT))
  }

  @Test
  fun twoKeys_storedInEitherOrder() {
    assertEquals(
      QuickNav.keys(0, KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN),
      QuickNav.keys(0, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_UP),
    )
    assertEquals(QuickNavCommand.CLICK, QuickNav.commandFor(context, QuickNavCommand.CLICK.defaultKeys))
  }
}
