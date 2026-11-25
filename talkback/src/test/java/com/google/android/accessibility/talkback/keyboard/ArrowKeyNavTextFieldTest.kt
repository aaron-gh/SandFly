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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Arrow key navigation on text fields: moving onto a text field reads it, and the arrow keys go to
 * it once it is activated or typed into.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArrowKeyNavTextFieldTest {
  private lateinit var context: Context
  /** Commands run, as the tutorial hook sees them. */
  private val commands = mutableListOf<String>()
  private var time = 1_000L

  private lateinit var field: AccessibilityNodeInfoCompat
  private lateinit var button: AccessibilityNodeInfoCompat
  private var inputFocusNode: AccessibilityNodeInfoCompat? = null
  private var sandFlyFocusNode: AccessibilityNodeInfoCompat? = null

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    TutorialHooks.listener =
      object : TutorialHooks.Listener {
        override fun onGesture(gestureId: Int, action: String) = false

        override fun onKeys(keys: String?, command: String): Boolean {
          commands += command
          return true
        }

        override fun onSystemKeys(keyCode: Int) = false

        override fun onBrowseMode(enabled: Boolean) {}

        override fun onContinuousReading(active: Boolean) {}
      }
    field =
      AccessibilityNodeInfoCompat.wrap(
        AccessibilityNodeInfo.obtain(View(context)).apply {
          isFocusable = true
          isFocused = true
          isEditable = true
          text = "Search"
        }
      )
    button =
      AccessibilityNodeInfoCompat.wrap(
        AccessibilityNodeInfo.obtain(View(context)).apply { text = "Chat" }
      )
    inputFocusNode = field
    sandFlyFocusNode = button
  }

  @After
  fun tearDown() {
    TutorialHooks.listener = null
  }

  private fun manager(keymap: Keymap, sandFlyKey: SandFlyKey? = null): KeyComboManager {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context).edit()
    prefs.putString(
      context.getString(R.string.pref_select_keymap_key),
      context.getString(keymap.valueRes),
    )
    if (sandFlyKey != null) {
      prefs.putString(keymap.sandFlyKeyPref, context.getString(sandFlyKey.valueRes))
    }
    prefs.commit()
    val monitor =
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getInputFocus(): AccessibilityNodeInfoCompat? = inputFocusNode

        override fun getAccessibilityFocus(
          useInputFocusIfEmpty: Boolean
        ): AccessibilityNodeInfoCompat? = sandFlyFocusNode
      }
    val service =
      object : AccessibilityService() {
        override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

        override fun onInterrupt() {}
      }
    // Only arrow key navigation's own commands run here, which the tutorial hook takes.
    val mapper =
      object : KeyComboMapper(
        service,
        { false },
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        monitor,
        null,
        null,
        null,
      ) {
        override fun interruptByKeyCombo(performedAction: TalkBackPhysicalKeyboardShortcut) {}
      }
    val manager = KeyComboManager.create(context)
    manager.onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
    manager.testing_setKeyComboMapper(mapper)
    KeyComboManager::class.java.getDeclaredField("accessibilityFocusMonitor").apply {
      isAccessible = true
      set(manager, monitor)
    }
    return manager
  }

  private fun KeyComboManager.key(action: Int, keyCode: Int): Boolean {
    time += 10
    return onKeyEvent(KeyEvent(time, time, action, keyCode, 0, 0), null)
  }

  private fun KeyComboManager.down(keyCode: Int) = key(KeyEvent.ACTION_DOWN, keyCode)

  private fun KeyComboManager.up(keyCode: Int) = key(KeyEvent.ACTION_UP, keyCode)

  /** Presses and releases [keyCode]; returns whether SandFly took the press. */
  private fun KeyComboManager.press(keyCode: Int): Boolean {
    val taken = down(keyCode)
    up(keyCode)
    time += 1000
    return taken
  }

  private fun KeyComboManager.event(type: Int, source: AccessibilityNodeInfoCompat? = null) {
    val event = AccessibilityEvent.obtain(type)
    if (source != null) {
      shadowOf(event).setSourceNode(source.unwrap())
    }
    onAccessibilityEvent(event, null)
  }

  /** Moves SandFly focus to [node], as the command just run does. */
  private fun KeyComboManager.focus(node: AccessibilityNodeInfoCompat) {
    sandFlyFocusNode = node
    event(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, node)
  }

  /** Moves with Right Arrow from the button to the text field with input focus. */
  private fun KeyComboManager.moveToField() {
    focus(button)
    assertTrue(press(KeyEvent.KEYCODE_DPAD_RIGHT))
    focus(field)
  }

  @Test
  fun keyboardPhoneArrowKeysMovePastATextFieldWithInputFocus() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      commands.clear()
      sandFlyFocusNode = button
      val manager = manager(keymap, SandFlyKey.SYM)
      manager.moveToField()
      assertTrue(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
      assertTrue(manager.press(KeyEvent.KEYCODE_DPAD_LEFT))
      assertEquals(listOf("NEXT", "NEXT", "PREVIOUS"), commands)
    }
  }

  @Test
  fun keyboardPhoneEnterActivatesATextFieldMovedToAndArrowKeysThenEdit() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    manager.moveToField()
    // Enter clicks the field instead of going to it.
    assertTrue(manager.press(KeyEvent.KEYCODE_ENTER))
    assertEquals(2, commands.size)
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_LEFT))
    assertEquals(2, commands.size)
    // Keys pressed together still run their command.
    manager.down(KeyEvent.KEYCODE_DPAD_UP)
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_DOWN))
    manager.up(KeyEvent.KEYCODE_DPAD_DOWN)
    manager.up(KeyEvent.KEYCODE_DPAD_UP)
    assertEquals("PERFORM_CLICK_ACTION", commands.last())
  }

  @Test
  fun typingIntoATextFieldMovedToStartsEditing() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    manager.moveToField()
    assertFalse(manager.press(KeyEvent.KEYCODE_A))
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_LEFT))
    assertEquals(1, commands.size)
  }

  @Test
  fun keyboardPhoneNewScreenWithInputFocusInATextFieldLeavesArrowKeysToNavigation() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    manager.event(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    manager.focus(button)
    // Letters type into the field, which has input focus.
    assertFalse(manager.press(KeyEvent.KEYCODE_A))
    // Once typed into, the arrow keys go to it.
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertTrue(commands.isEmpty())

    manager.event(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    manager.focus(field)
    manager.focus(button)
    assertTrue(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertEquals(listOf("NEXT"), commands)
  }

  @Test
  fun aTextFieldThatANewScreenPutsSandFlyFocusOnIsEdited() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    manager.event(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    manager.focus(field)
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertTrue(commands.isEmpty())
  }

  @Test
  fun sandFlyKeymapUpDownOrEnterOnATextFieldMovedToStartsEditing() {
    ArrowKeyNav.setOn(context, true)
    val manager = manager(Keymap.SANDFLY)
    assertTrue(ArrowKeyNav.isOn(context))
    manager.moveToField()
    assertTrue(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    // Up Arrow + Down Arrow clicks.
    manager.down(KeyEvent.KEYCODE_DPAD_UP)
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_DOWN))
    manager.up(KeyEvent.KEYCODE_DPAD_DOWN)
    manager.up(KeyEvent.KEYCODE_DPAD_UP)
    time += 1000
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertEquals(listOf("NEXT", "NEXT", "PERFORM_CLICK_ACTION"), commands)

    // Moved away and back, Enter, which goes to the field, starts editing it.
    commands.clear()
    manager.moveToField()
    assertTrue(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertFalse(manager.press(KeyEvent.KEYCODE_ENTER))
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertEquals(listOf("NEXT", "NEXT"), commands)
  }

  @Test
  fun aClickOnATextFieldMovedToStartsEditing() {
    ArrowKeyNav.setOn(context, true)
    val manager = manager(Keymap.SANDFLY)
    manager.moveToField()
    manager.event(AccessibilityEvent.TYPE_VIEW_CLICKED, field)
    assertFalse(manager.press(KeyEvent.KEYCODE_DPAD_RIGHT))
  }
}
