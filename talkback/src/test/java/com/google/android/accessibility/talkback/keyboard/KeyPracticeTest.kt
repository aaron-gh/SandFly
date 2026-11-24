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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Keyboard practice, which says each key pressed and its command instead of running it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyPracticeTest {
  private lateinit var context: Context
  private lateinit var manager: KeyComboManager
  /** What keyboard practice said. */
  private val spoken = mutableListOf<String>()
  /** The commands that keys ran. */
  private val ran = mutableListOf<String>()
  private var time = 1_000L

  private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
  private val rightShift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON
  private val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON

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
          // The command's keyboard action, which needs the running service, run here.
          if (command == toggleValue()) {
            manager.toggleKeyPractice()
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

  private fun start(keymap: Keymap, sandFlyKey: SandFlyKey) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(
        context.getString(R.string.pref_select_keymap_key),
        context.getString(keymap.valueRes),
      )
      .putString(keymap.sandFlyKeyPref, context.getString(sandFlyKey.valueRes))
      .commit()
    manager =
      KeyComboManager.create(context).apply {
        onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
        testing_setKeyPracticeSpeech { spoken += it }
      }
  }

  private fun key(action: Int, keyCode: Int, metaState: Int = 0, repeat: Int = 0): Boolean {
    time += 10
    return manager.onKeyEvent(KeyEvent(time, time, action, keyCode, repeat, metaState), null)
  }

  private fun down(keyCode: Int, metaState: Int = 0) = key(KeyEvent.ACTION_DOWN, keyCode, metaState)

  private fun up(keyCode: Int, metaState: Int = 0) = key(KeyEvent.ACTION_UP, keyCode, metaState)

  /** Presses and releases [keyCode] with [metaState], then waits, so the next press is new. */
  private fun press(keyCode: Int, metaState: Int = 0) {
    assertTrue(down(keyCode, metaState))
    assertTrue(up(keyCode, metaState))
    time += 1_000
  }

  private fun toggleValue() =
    Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_PRACTICE)

  /** A key's name as shortcuts are spoken, which Robolectric gives as its number. */
  private fun name(keyCode: Int) =
    KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')

  /** The name of the command of the keyboard [action]. */
  private fun command(action: TalkBackPhysicalKeyboardShortcut): String =
    Commands.label(context, Commands.valueOfKeyboardAction(context, action))!!

  private fun practiceOn() {
    manager.toggleKeyPractice()
    assertTrue(manager.isKeyPracticeOn)
    assertEquals("Keyboard practice on", spoken.last())
    spoken.clear()
  }

  @Test
  fun keyboardPhoneKeymapsTurnPracticeOnAndOffWithSandFlyShiftK() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
      assertEquals(
        KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_K, true),
        NewKeyComboModel(context, keymap)
          .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_toggle_key_practice)),
      )
    }
  }

  @Test
  fun keysAreSpokenWithTheirCommandsAndNothingRuns() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    practiceOn()

    // The SandFly key alone moves to the next item.
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertEquals(
      "Right Shift, SandFly key, " +
        command(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT),
      spoken.last(),
    )
    // With a letter: its shortcut.
    assertTrue(down(KeyEvent.KEYCODE_J, rightShift))
    assertEquals(
      "Right Shift + " +
        name(KeyEvent.KEYCODE_J) +
        ", " +
        command(TalkBackPhysicalKeyboardShortcut.SELECT_PREVIOUS_READING_MENU),
      spoken.last(),
    )
    assertTrue(up(KeyEvent.KEYCODE_J, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    time += 1_000

    // A letter alone, with no command, and Alt alone.
    press(KeyEvent.KEYCODE_A)
    assertEquals(name(KeyEvent.KEYCODE_A), spoken.last())
    press(KeyEvent.KEYCODE_ALT_LEFT, alt)
    assertEquals("Alt", spoken.last())
    press(KeyEvent.KEYCODE_SHIFT_LEFT, shift)
    assertEquals("Shift", spoken.last())
    assertTrue(ran.isEmpty())
  }

  @Test
  fun repeatsAreNotSpokenAgain() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    practiceOn()
    assertTrue(down(KeyEvent.KEYCODE_A))
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, repeat = 1))
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, repeat = 2))
    assertTrue(up(KeyEvent.KEYCODE_A))
    assertEquals(listOf(name(KeyEvent.KEYCODE_A)), spoken)
  }

  @Test
  fun keyPressedTwiceIsSpokenWithTheCommandOfTwoPresses() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    val twice =
      KeyCombo.modifierKeys(KeyEvent.META_ALT_ON, false).withPressType(KeyCombo.PRESSED_TWICE)
    val key = manager.keyComboModel.getKeyForKeyCombo(twice)
    assertNotNull("Alt twice has a command", key)
    practiceOn()
    assertTrue(down(KeyEvent.KEYCODE_ALT_LEFT, alt))
    assertTrue(up(KeyEvent.KEYCODE_ALT_LEFT))
    assertTrue(down(KeyEvent.KEYCODE_ALT_LEFT, alt))
    assertEquals(
      "Alt twice, " +
        command(TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.resources, key)),
      spoken.last(),
    )
    assertTrue(up(KeyEvent.KEYCODE_ALT_LEFT))
    assertTrue(ran.isEmpty())
  }

  @Test
  fun modifierKeysHeldAreSpokenWithTheirCommand() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    practiceOn()
    assertTrue(down(KeyEvent.KEYCODE_ALT_LEFT, alt))
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_LEFT, alt or shift))
    assertEquals("Alt + Shift", spoken.last())
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(KeyPresses.HOLD_MS + 100))
    assertEquals(
      "Alt + Shift held, " +
        command(TalkBackPhysicalKeyboardShortcut.SELECT_NEXT_READING_MENU),
      spoken.last(),
    )
    up(KeyEvent.KEYCODE_SHIFT_LEFT, alt)
    up(KeyEvent.KEYCODE_ALT_LEFT)
    assertTrue(ran.isEmpty())
  }

  @Test
  fun arrowKeysAreSpokenWithTheirArrowKeyNavigationCommands() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    practiceOn()
    press(KeyEvent.KEYCODE_DPAD_RIGHT)
    assertEquals("Arrow Right, " + Commands.label(context, "NEXT"), spoken.last())
    assertTrue(down(KeyEvent.KEYCODE_DPAD_UP))
    assertTrue(down(KeyEvent.KEYCODE_DPAD_DOWN))
    assertEquals(
      "Arrow Up + Arrow Down, " + Commands.label(context, "PERFORM_CLICK_ACTION"),
      spoken.last(),
    )
    up(KeyEvent.KEYCODE_DPAD_DOWN)
    up(KeyEvent.KEYCODE_DPAD_UP)
    assertTrue(ran.isEmpty())
  }

  @Test
  fun theSecondKeyOfTwoInARowIsSpokenWithTheirCommand() {
    start(Keymap.SANDFLY, SandFlyKey.META)
    val (key, sequence) =
      manager.keyComboModel.keyComboMap.entries.first {
        it.value.prefixKeyCode != 0 && it.value.isTriggerModifierUsed && it.value.modifiers == 0
      }
    practiceOn()
    val meta = KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
    down(KeyEvent.KEYCODE_META_LEFT, meta)
    assertEquals("Meta, SandFly key", spoken.last())
    assertTrue(down(sequence.prefixKeyCode, meta))
    assertEquals("Meta + " + name(sequence.prefixKeyCode), spoken.last())
    up(sequence.prefixKeyCode, meta)
    assertTrue(down(sequence.keyCode, meta))
    assertEquals(
      manager.getKeyComboStringRepresentation(sequence) +
        ", " +
        command(TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.resources, key)),
      spoken.last(),
    )
    up(sequence.keyCode, meta)
    up(KeyEvent.KEYCODE_META_LEFT)
    assertTrue(ran.isEmpty())
  }

  @Test
  fun itsOwnKeysTurnPracticeOffAndTheirReleaseIsTaken() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    // Turned on by its keys, which are released where they were pressed.
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    down(KeyEvent.KEYCODE_SHIFT_LEFT, rightShift or shift)
    assertTrue(down(KeyEvent.KEYCODE_K, rightShift or shift))
    assertTrue(manager.isKeyPracticeOn)
    assertEquals(listOf("Keyboard practice on"), spoken)
    up(KeyEvent.KEYCODE_K, rightShift or shift)
    up(KeyEvent.KEYCODE_SHIFT_LEFT, rightShift)
    up(KeyEvent.KEYCODE_SHIFT_RIGHT)
    assertEquals(1, ran.size)
    time += 1_000

    // In practice, the same keys turn it off.
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(down(KeyEvent.KEYCODE_SHIFT_LEFT, rightShift or shift))
    assertTrue(down(KeyEvent.KEYCODE_K, rightShift or shift))
    assertFalse(manager.isKeyPracticeOn)
    assertEquals("Keyboard practice off", spoken.last())
    assertTrue(up(KeyEvent.KEYCODE_K, rightShift or shift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_LEFT, rightShift))
    assertTrue(up(KeyEvent.KEYCODE_SHIFT_RIGHT))
    assertEquals(1, ran.size)
    time += 1_000

    // Shortcuts run again.
    down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift)
    assertTrue(down(KeyEvent.KEYCODE_J, rightShift))
    up(KeyEvent.KEYCODE_J, rightShift)
    up(KeyEvent.KEYCODE_SHIFT_RIGHT)
    assertEquals(2, ran.size)
  }

  @Test
  fun backAndEscapeTurnPracticeOff() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    for (keyCode in listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE)) {
      practiceOn()
      assertTrue(down(keyCode))
      assertFalse(manager.isKeyPracticeOn)
      assertEquals(listOf("Keyboard practice off"), spoken)
      assertTrue(up(keyCode))
      spoken.clear()
      time += 1_000
    }
    // Back goes to Android again.
    assertFalse(down(KeyEvent.KEYCODE_BACK))
    assertFalse(up(KeyEvent.KEYCODE_BACK))
  }

  @Test
  fun keysHeldWhenPracticeTurnsOnAreReleasedWhereTheyWerePressed() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    // A letter typed into an app.
    assertFalse(down(KeyEvent.KEYCODE_A))
    practiceOn()
    assertFalse(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, repeat = 1))
    assertFalse(up(KeyEvent.KEYCODE_A))
    assertTrue(spoken.isEmpty())
  }

  @Test
  fun practiceEndsWhenSandFlyIsSuspended() {
    start(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    practiceOn()
    manager.onServiceStateChanged(ServiceStateListener.SERVICE_STATE_INACTIVE)
    assertFalse(manager.isKeyPracticeOn)
    manager.onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
    assertFalse(down(KeyEvent.KEYCODE_A))
  }

  @Test
  fun practiceIsACommandInTheSandFlyCategory() {
    assertEquals("Turn keyboard practice on or off", Commands.label(context, toggleValue()))
  }
}
