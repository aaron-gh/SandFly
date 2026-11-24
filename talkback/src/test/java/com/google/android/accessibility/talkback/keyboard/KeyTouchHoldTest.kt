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
import android.os.Looper
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.tutorial.TutorialHooks
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.ServiceStateListener
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Hold a touch on the focused item: held while its keys are down, or latched after a short press. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyTouchHoldTest {
  private lateinit var context: Context
  private lateinit var manager: KeyComboManager
  /** What the command said. */
  private val spoken = mutableListOf<String>()
  /** The commands that keys ran. */
  private val ran = mutableListOf<String>()
  private var time = 1_000L

  private val rightShift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON

  /** A touch the fake toucher holds. */
  private class Touch(val stopped: () -> Boolean, val ended: () -> Unit)

  private val touches = mutableListOf<Touch>()
  /** What the fake toucher returns. */
  private var startResult = KeyTouchHold.Start.STARTED

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    TutorialHooks.listener =
      object : TutorialHooks.Listener {
        override fun onGesture(gestureId: Int, action: String) = false

        override fun onKeys(keys: String?, command: String): Boolean {
          ran += command
          // The commands' keyboard actions, which need the running service, run here.
          when (command) {
            touchHoldValue() -> manager.toggleTouchHold()
            practiceValue() -> manager.toggleKeyPractice()
          }
          return true
        }

        override fun onSystemKeys(keyCode: Int) = false

        override fun onBrowseMode(enabled: Boolean) {}

        override fun onContinuousReading(active: Boolean) {}
      }
  }

  @After
  fun tearDown() {
    TutorialHooks.listener = null
  }

  private fun start(keymap: Keymap = Keymap.KEYBOARD_PHONE) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(
        context.getString(R.string.pref_select_keymap_key),
        context.getString(keymap.valueRes),
      )
      .putString(keymap.sandFlyKeyPref, context.getString(SandFlyKey.RIGHT_SHIFT.valueRes))
      .commit()
    manager =
      KeyComboManager.create(context).apply {
        onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
        testing_setTouchHoldSpeech { spoken += it }
        testing_setKeyPracticeSpeech { spoken += it }
        testing_setTouchHoldToucher { stopped, ended ->
          if (startResult == KeyTouchHold.Start.STARTED) touches += Touch(stopped, ended)
          startResult
        }
      }
  }

  private fun key(action: Int, keyCode: Int, metaState: Int = 0, repeat: Int = 0): Boolean {
    time += 10
    return manager.onKeyEvent(KeyEvent(time, time, action, keyCode, repeat, metaState), null)
  }

  private fun down(keyCode: Int, metaState: Int = 0) = key(KeyEvent.ACTION_DOWN, keyCode, metaState)

  private fun up(keyCode: Int, metaState: Int = 0) = key(KeyEvent.ACTION_UP, keyCode, metaState)

  /** Presses Right Shift, the SandFly key, + V, holds them for [holdMs], and releases them. */
  private fun pressSandFlyV(holdMs: Long = 100) {
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    time += holdMs
    assertTrue(up(KeyEvent.KEYCODE_V, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    time += 1_000
  }

  private fun touchHoldValue() =
    Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.TOUCH_HOLD_FOCUS)

  private fun practiceValue() =
    Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_PRACTICE)

  private fun latch() {
    pressSandFlyV()
    assertTrue(manager.isTouchHoldLatched)
    assertEquals(listOf("Touch held"), spoken)
    spoken.clear()
  }

  @Test
  fun keyboardPhoneKeymapsHoldATouchWithSandFlyV() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
      val model = NewKeyComboModel(context, keymap)
      val keys = KeyCombo(KeyComboModel.NO_MODIFIER, 0, KeyEvent.KEYCODE_V, true)
      assertEquals(
        keys,
        model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_touch_hold_focus)),
      )
      // No other command has the same keys.
      assertEquals(
        listOf(context.getString(R.string.keycombo_shortcut_touch_hold_focus)),
        model.keyComboMap.filterValues { it == keys }.keys.toList(),
      )
    }
  }

  @Test
  fun touchStaysDownWhileKeysAreHeldAndLiftsWhenReleased() {
    start()
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    assertEquals(1, touches.size)
    assertTrue(manager.isTouchHeld)
    // Repeats of the held key are taken and don't run the command again.
    for (repeat in 1..20) {
      assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, rightShift, repeat))
    }
    time += 3_000
    assertFalse(touches[0].stopped())
    assertEquals(listOf(touchHoldValue()), ran)
    // Released: the touch lifts, without a word.
    assertTrue(up(KeyEvent.KEYCODE_V, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertTrue(touches[0].stopped())
    assertFalse(manager.isTouchHeld)
    touches[0].ended()
    assertTrue(spoken.isEmpty())
    assertEquals(1, touches.size)
  }

  @Test
  fun releasingTheSandFlyKeyFirstKeepsTheTouchUntilVIsReleased() {
    start()
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    time += 1_000
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    // V repeats without the SandFly key don't reach the app.
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, repeat = 1))
    assertFalse(touches[0].stopped())
    time += 1_000
    assertTrue(up(KeyEvent.KEYCODE_V))
    assertTrue(touches[0].stopped())
    assertTrue(spoken.isEmpty())
  }

  @Test
  fun shortPressLatchesAndSecondPressLifts() {
    start()
    latch()
    assertFalse(touches[0].stopped())
    // Other keys still work: Right Shift + W runs its command.
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_W, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_W, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertEquals(2, ran.size)
    assertFalse(touches[0].stopped())
    assertTrue(manager.isTouchHeld)

    // The shortcut again lifts it, and its repeats don't hold a new touch.
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    assertEquals(listOf("Touch lifted"), spoken)
    assertTrue(touches[0].stopped())
    assertFalse(manager.isTouchHeld)
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, rightShift, repeat = 1))
    assertTrue(up(KeyEvent.KEYCODE_V, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertEquals(1, touches.size)
    // The lift ends quietly: it was already spoken.
    touches[0].ended()
    assertEquals(listOf("Touch lifted"), spoken)
  }

  @Test
  fun backOrEscapeLiftsALatchedTouch() {
    for (keyCode in listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE)) {
      start()
      spoken.clear()
      touches.clear()
      latch()
      assertTrue(down(keyCode))
      assertEquals(listOf("Touch lifted"), spoken)
      assertTrue(touches[0].stopped())
      assertTrue(key(KeyEvent.ACTION_DOWN, keyCode, repeat = 1))
      assertTrue(up(keyCode))
      assertFalse(manager.isTouchHeld)
      // Afterwards, Back goes on as usual.
      assertFalse(down(keyCode))
      assertFalse(up(keyCode))
    }
  }

  @Test
  fun backWhileKeysAreHeldLiftsTheTouch() {
    start()
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    time += 1_000
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertTrue(down(KeyEvent.KEYCODE_BACK))
    assertEquals(listOf("Touch lifted"), spoken)
    assertTrue(touches[0].stopped())
    assertTrue(up(KeyEvent.KEYCODE_BACK))
    assertTrue(up(KeyEvent.KEYCODE_V))
    assertFalse(manager.isTouchHeld)
    assertEquals(listOf("Touch lifted"), spoken)
  }

  @Test
  fun touchLiftsAtTheMaximum() {
    start()
    latch()
    val idle = shadowOf(Looper.getMainLooper())
    idle.idleFor(Duration.ofMillis(KeyTouchHold.MAX_HOLD_MS - 1_000))
    assertFalse(touches[0].stopped())
    idle.idleFor(Duration.ofMillis(1_000))
    assertTrue(touches[0].stopped())
    touches[0].ended()
    assertEquals(listOf("Touch lifted"), spoken)
    assertFalse(manager.isTouchHeld)
  }

  @Test
  fun touchLiftsWhenSandFlyIsSuspended() {
    start()
    latch()
    manager.onServiceStateChanged(ServiceStateListener.SERVICE_STATE_INACTIVE)
    assertTrue(touches[0].stopped())
    assertFalse(manager.isTouchHeld)
    touches[0].ended()
    assertTrue(spoken.isEmpty())
  }

  @Test
  fun touchEndedElsewhereIsSpoken() {
    start()
    latch()
    // The item went, or another touch cancelled this one.
    touches[0].ended()
    assertEquals(listOf("Touch lifted"), spoken)
    assertFalse(manager.isTouchHeld)
    // The next press holds a new touch.
    pressSandFlyV()
    assertEquals(2, touches.size)
    assertTrue(manager.isTouchHoldLatched)
  }

  @Test
  fun commandRunWithoutKeysLatches() {
    start()
    manager.toggleTouchHold()
    assertTrue(manager.isTouchHoldLatched)
    assertEquals(listOf("Touch held"), spoken)
    manager.toggleTouchHold()
    assertFalse(manager.isTouchHeld)
    assertEquals(listOf("Touch held", "Touch lifted"), spoken)
    assertTrue(touches[0].stopped())
  }

  @Test
  fun withoutAFocusedItemOrSupportNothingIsHeld() {
    start()
    startResult = KeyTouchHold.Start.NO_FOCUS
    pressSandFlyV()
    assertEquals(listOf("No item has focus"), spoken)
    assertFalse(manager.isTouchHeld)
    startResult = KeyTouchHold.Start.UNAVAILABLE
    pressSandFlyV()
    assertEquals(listOf("No item has focus", "Not available"), spoken)
    assertFalse(manager.isTouchHeld)
    assertTrue(touches.isEmpty())
  }

  @Test
  fun keyboardPracticeNamesTheCommand() {
    start()
    manager.toggleKeyPractice()
    spoken.clear()
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_V, rightShift))
    assertTrue(spoken.last().endsWith(", Hold a touch on the focused item"))
    assertTrue(up(KeyEvent.KEYCODE_V, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertTrue(touches.isEmpty())
  }
}
