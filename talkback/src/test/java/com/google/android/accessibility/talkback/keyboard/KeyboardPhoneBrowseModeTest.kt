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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut as Key
import com.google.android.accessibility.talkback.tutorial.TutorialHooks
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.ServiceStateListener
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Browse mode in the keyboard phone single-key keymap: the SandFly keymap's letters. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardPhoneBrowseModeTest {
  private lateinit var context: Context
  private lateinit var manager: KeyComboManager
  /** The commands that keys ran. */
  private val ran = mutableListOf<String>()
  private var time = 1_000L

  private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
  private val rightShift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON

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

  private fun start(sandFlyKey: SandFlyKey = SandFlyKey.RIGHT_SHIFT) {
    val keymap = Keymap.KEYBOARD_PHONE_SINGLE_KEYS
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
      }
  }

  private fun browseMode(on: Boolean) {
    ReflectionHelpers.setField(manager, "browseModeEnabled", on)
  }

  private fun key(action: Int, keyCode: Int, metaState: Int = 0): Boolean {
    time += 10
    return manager.onKeyEvent(KeyEvent(time, time, action, keyCode, 0, metaState), null)
  }

  /** Presses and releases [keyCode] with [metaState], then waits, so the next press is new. */
  private fun press(keyCode: Int, metaState: Int = 0): Boolean {
    val taken = key(KeyEvent.ACTION_DOWN, keyCode, metaState)
    key(KeyEvent.ACTION_UP, keyCode, metaState)
    time += 1_000
    return taken
  }

  /** Presses [keyCode] with the SandFly key, Right Shift, held down. */
  private fun pressWithSandFlyKey(keyCode: Int) {
    key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift)
    key(KeyEvent.ACTION_DOWN, keyCode, rightShift)
    key(KeyEvent.ACTION_UP, keyCode, rightShift)
    key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_RIGHT)
    time += 1_000
  }

  private fun value(action: Key): String = Commands.valueOfKeyboardAction(context, action)

  /** The command the last key ran. */
  private fun last(): String = ran.last()

  @Test
  fun lettersOutsideBrowseModeKeepTheirShortcuts() {
    start()
    press(KeyEvent.KEYCODE_B)
    assertEquals(value(Key.NAVIGATE_LAST), last())
    press(KeyEvent.KEYCODE_T)
    assertEquals(value(Key.NAVIGATE_FIRST), last())
    press(KeyEvent.KEYCODE_U)
    assertEquals(value(Key.NAVIGATE_NEXT_LINK), last())
    press(KeyEvent.KEYCODE_H)
    assertEquals(value(Key.NAVIGATE_NEXT_HEADING), last())
    // C has no shortcut outside browse mode: it goes to the app.
    val count = ran.size
    assertFalse(press(KeyEvent.KEYCODE_C))
    assertEquals(count, ran.size)
  }

  @Test
  fun lettersInBrowseModeMoveAsInTheSandFlyKeymap() {
    start()
    browseMode(true)
    press(KeyEvent.KEYCODE_B)
    assertEquals(value(Key.NAVIGATE_NEXT_BUTTON), last())
    press(KeyEvent.KEYCODE_B, shift)
    assertEquals(value(Key.NAVIGATE_PREVIOUS_BUTTON), last())
    press(KeyEvent.KEYCODE_C)
    assertEquals(value(Key.NAVIGATE_NEXT_COMBOBOX), last())
    press(KeyEvent.KEYCODE_T)
    assertEquals(value(Key.NAVIGATE_NEXT_TABLE), last())
    // K is the link key, as in the SandFly keymap, and U the unvisited link key.
    press(KeyEvent.KEYCODE_K)
    assertEquals(value(Key.NAVIGATE_NEXT_LINK), last())
    press(KeyEvent.KEYCODE_U)
    assertEquals(value(Key.NAVIGATE_NEXT_UNVISITED_LINK), last())
    // Keys without a shortcut in browse mode keep theirs.
    press(KeyEvent.KEYCODE_H)
    assertEquals(value(Key.NAVIGATE_NEXT_HEADING), last())
    press(KeyEvent.KEYCODE_SPACE)
    assertEquals(value(Key.NAVIGATE_NEXT_DEFAULT), last())
    press(KeyEvent.KEYCODE_J)
    assertEquals(value(Key.SELECT_PREVIOUS_READING_MENU), last())
  }

  @Test
  fun sandFlyKeyWithALetterInBrowseModeRunsItsShortcutOutsideBrowseMode() {
    start()
    browseMode(true)
    pressWithSandFlyKey(KeyEvent.KEYCODE_B)
    assertEquals(value(Key.NAVIGATE_LAST), last())
    pressWithSandFlyKey(KeyEvent.KEYCODE_U)
    assertEquals(value(Key.NAVIGATE_NEXT_LINK), last())
    pressWithSandFlyKey(KeyEvent.KEYCODE_K)
    assertEquals(value(Key.ADJUST_READING_MENU_DOWN), last())
    // A SandFly key shortcut of its own stays.
    pressWithSandFlyKey(KeyEvent.KEYCODE_V)
    assertEquals(value(Key.TOUCH_HOLD_FOCUS), last())
  }

  @Test
  fun sandFlyKeyPressedTwiceTurnsBrowseModeOnOrOff() {
    start()
    key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift)
    key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_RIGHT)
    key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift)
    key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_RIGHT)
    assertEquals(value(Key.TOGGLE_BROWSE_MODE_EXTRA), last())
  }

  @Test
  fun withAltAsTheSandFlyKeyAltTwiceStaysPlayOrPause() {
    val keymap = Keymap.KEYBOARD_PHONE_SINGLE_KEYS
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(keymap.sandFlyKeyPref, context.getString(SandFlyKey.ALT.valueRes))
      .commit()
    val model = NewKeyComboModel(context, keymap)
    assertEquals(
      KeyCombo.modifierKeys(0, true).withPressType(KeyCombo.PRESSED_TWICE),
      model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_global_play_pause_media)),
    )
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_Q, true),
      model.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
      ),
    )
  }

  @Test
  fun browseModeShortcutsAreShownAndWrittenWithInBrowseMode() {
    start()
    val button =
      NewKeyComboModel(context, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)
        .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_button))
    assertTrue(button.isInBrowseMode)
    assertEquals("B in browse mode", KeyText.of(button))
    assertEquals(button, KeyText.parse("B in browse mode"))
    assertEquals(null, KeyText.parse("SandFly+B in browse mode"))
  }

  @Test
  fun browseModeShortcutsAreEligibleOnlyInKeyboardPhoneKeymaps() {
    val b = KeyCombo(0, 0, KeyEvent.KEYCODE_B, false).withInBrowseMode(true)
    assertTrue(NewKeyComboModel(context, Keymap.KEYBOARD_PHONE_SINGLE_KEYS).isEligibleKeyCombo(b))
    assertTrue(NewKeyComboModel(context, Keymap.KEYBOARD_PHONE).isEligibleKeyCombo(b))
    assertFalse(NewKeyComboModel(context, Keymap.SANDFLY).isEligibleKeyCombo(b))
    assertFalse(
      NewKeyComboModel(context, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)
        .isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_B, true).withInBrowseMode(true))
    )
  }

  @Test
  fun savedSingleKeyKeymapGetsBrowseModeKeys() {
    val keymap = Keymap.KEYBOARD_PHONE_SINGLE_KEYS
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(keymap.sandFlyKeyPref, context.getString(SandFlyKey.SYM.valueRes))
      .putInt(keymap.persisterPrefix + "_defaults_version", 6)
      .commit()
    val persister = KeyComboPersister(context, keymap.persisterPrefix)
    val button = context.getString(R.string.keycombo_shortcut_navigate_next_button)
    val toggle = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
    val table = context.getString(R.string.keycombo_shortcut_navigate_next_table)
    persister.saveKeyCombo(button, KeyCombo())
    persister.saveKeyCombo(toggle, KeyCombo())
    // The user gave Ctrl + T to the next table.
    persister.saveKeyCombo(table, KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_T, false))
    val model = NewKeyComboModel(context, keymap)
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_B, false).withInBrowseMode(true),
      model.getKeyComboForKey(button),
    )
    assertEquals(
      KeyCombo.modifierKeys(0, true).withPressType(KeyCombo.PRESSED_TWICE),
      model.getKeyComboForKey(toggle),
    )
    assertEquals(
      KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_T, false),
      model.getKeyComboForKey(table),
    )
  }

  @Test
  fun secondLinkKeysAreListedWithTheLinkCommands() {
    val next =
      Commands.categories(context).flatMap { it.commands }.first {
        it.keyboardAction == Key.NAVIGATE_NEXT_LINK
      }
    assertEquals(Key.NAVIGATE_NEXT_LINK_EXTRA, next.secondKeyboardAction)
    assertEquals(next.value, value(Key.NAVIGATE_NEXT_LINK_EXTRA))
  }
}
