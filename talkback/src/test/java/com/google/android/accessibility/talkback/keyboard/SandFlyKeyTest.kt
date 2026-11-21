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
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.tutorial.TutorialHooks
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.ServiceStateListener
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
import org.robolectric.Shadows.shadowOf
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

/** Shortcuts pressed with each kind of SandFly key, as KeyComboManager matches them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SandFlyKeyTest {
  private lateinit var context: Context
  private val keys = mutableListOf<String?>()
  private var time = 1_000L

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    TutorialHooks.listener =
      object : TutorialHooks.Listener {
        override fun onGesture(gestureId: Int, action: String) = false

        override fun onKeys(keys: String?, command: String): Boolean {
          this@SandFlyKeyTest.keys += keys
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
    return KeyComboManager.create(context).apply {
      onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
    }
  }

  private fun KeyComboManager.key(action: Int, keyCode: Int, metaState: Int = 0): Boolean {
    time += 10
    return onKeyEvent(KeyEvent(time, time, action, keyCode, 0, metaState), null)
  }

  private fun KeyComboManager.down(keyCode: Int, metaState: Int = 0) =
    key(KeyEvent.ACTION_DOWN, keyCode, metaState)

  private fun KeyComboManager.up(keyCode: Int, metaState: Int = 0) =
    key(KeyEvent.ACTION_UP, keyCode, metaState)

  @Test
  fun insertHeldIsTheSandFlyKey() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.INSERT)
    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT))
    manager.up(KeyEvent.KEYCODE_DPAD_RIGHT)
    assertTrue(manager.up(KeyEvent.KEYCODE_INSERT))
    assertEquals(listOf<String?>("Insert + Arrow Right"), keys)

    // Without Insert, the arrow goes to the app.
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertEquals(1, keys.size)
  }

  @Test
  fun numpadZeroWithNumLockOnTypesZero() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.NUMPAD_INSERT)
    val numLock = KeyEvent.META_NUM_LOCK_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_NUMPAD_0, numLock))
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT, numLock))
    manager.up(KeyEvent.KEYCODE_DPAD_RIGHT, numLock)
    manager.up(KeyEvent.KEYCODE_NUMPAD_0, numLock)
    assertTrue(keys.isEmpty())

    assertTrue(manager.down(KeyEvent.KEYCODE_NUMPAD_0))
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertEquals(listOf<String?>("Numpad Insert + Arrow Right"), keys)
  }

  @Test
  fun actionIsNoLongerTheSandFlyKeyWhenInsertIs() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.INSERT)
    val meta = KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_META_LEFT, meta))
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT, meta))
    assertTrue(keys.isEmpty())
  }

  @Test
  fun voiceOverKeymapUsesCtrlAlt() {
    val manager = manager(Keymap.VOICEOVER, SandFlyKey.CTRL_ALT)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    val ctrlAlt = ctrl or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl)
    manager.down(KeyEvent.KEYCODE_ALT_LEFT, ctrlAlt)
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT, ctrlAlt))
    assertEquals(listOf<String?>("Ctrl + Alt + Arrow Right"), keys)
  }

  @Test
  fun ctrlAloneIsNotCtrlAlt() {
    val manager = manager(Keymap.VOICEOVER, SandFlyKey.CTRL_ALT)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl)
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT, ctrl))
    assertTrue(keys.isEmpty())
  }

  @Test
  fun nvdaKeymapHasNoTwoKeyShortcuts() {
    val manager = manager(Keymap.NVDA, SandFlyKey.INSERT)
    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    assertTrue(manager.down(KeyEvent.KEYCODE_N))
    // Robolectric names letter keys by number.
    assertEquals(listOf<String?>("Insert + ${KeyEvent.KEYCODE_N}"), keys)
    manager.up(KeyEvent.KEYCODE_N)
    // Insert + O is a shortcut of its own, not the start of Insert + O, then a key.
    assertFalse(manager.isSequenceKeyInfraSupported)
  }

  @Test
  fun jawsKeymapTurnsBrowseModeOnWithInsertZ() {
    val manager = manager(Keymap.JAWS, SandFlyKey.INSERT)
    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    assertTrue(manager.down(KeyEvent.KEYCODE_Z))
    assertEquals(listOf<String?>("Insert + ${KeyEvent.KEYCODE_Z}"), keys)
  }

  @Test
  fun nvdaKeymapUsesTheChosenSandFlyKey() {
    val manager = manager(Keymap.NVDA, SandFlyKey.META)
    val meta = KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
    manager.down(KeyEvent.KEYCODE_META_LEFT, meta)
    assertTrue(manager.down(KeyEvent.KEYCODE_N, meta))
    assertEquals(listOf<String?>("Meta + ${KeyEvent.KEYCODE_N}"), keys)
  }

  @Test
  fun eachKeymapKeepsItsOwnSandFlyKey() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(Keymap.VOICEOVER.sandFlyKeyPref, context.getString(SandFlyKey.INSERT.valueRes))
      .commit()
    assertEquals(SandFlyKey.META, manager(Keymap.SANDFLY).sandFlyKey)
    assertEquals(SandFlyKey.INSERT, manager(Keymap.VOICEOVER).sandFlyKey)
    assertEquals(SandFlyKey.INSERT, manager(Keymap.NVDA).sandFlyKey)
  }

  @Test
  fun aKeyTheKeymapDoesNotOfferIsNotUsed() {
    // Ctrl + Alt would drop SandFly keymap shortcuts, so the SandFly keymap keeps Meta.
    assertEquals(SandFlyKey.META, manager(Keymap.SANDFLY, SandFlyKey.CTRL_ALT).sandFlyKey)
    assertEquals(SandFlyKey.CTRL_ALT, manager(Keymap.VOICEOVER, SandFlyKey.META).sandFlyKey)
  }

  @Test
  fun arrowKeyNavigationLeavesInsertArrowShortcuts() {
    ArrowKeyNav.reset(context)
    ArrowKeyNav.setOn(context, true)
    val manager = manager(Keymap.NVDA, SandFlyKey.INSERT)
    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_DOWN))
    // NVDA + Down reads from the focused item.
    assertEquals(listOf<String?>("Insert + Arrow Down"), keys)
  }

  @Test
  fun defaultKeymapIsKeptAsACustomKeymap() {
    // An older version's Default keymap: Alt as its modifier and Alt + J to activate.
    KeyComboPersister(context, "default_key_combo_model")
      .saveKeyCombo(
        context.getString(R.string.keycombo_shortcut_perform_click),
        KeyCombo(0, 0, KeyEvent.KEYCODE_J, true),
      )
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_select_keymap_key), "default_keymap")
      .putString("pref_default_keymap_trigger_modifier", "trigger_modifier_alt")
      .commit()
    val manager = KeyComboManager.create(context)
    assertTrue(manager.isCustomKeymap)
    assertEquals("Custom", (manager.keyComboModel as NewKeyComboModel).customKeymap?.name)
    assertEquals(SandFlyKey.ALT, manager.sandFlyKey)
    val click = context.getString(R.string.keycombo_shortcut_perform_click)
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_J, true), manager.keyComboModel.getKeyComboForKey(click))
    // Resetting it goes back to it as it was kept.
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_J, true), manager.keyComboModel.getDefaultKeyCombo(click))
    // Commands the Default keymap didn't have are unassigned.
    assertEquals(
      0L,
      manager.keyComboModel
        .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_paragraph))
        .keyComboCode,
    )
  }

  @Test
  fun customKeymapTakesKeysWithoutTheSandFlyKey() {
    val custom =
      CustomKeymaps.create(
        context,
        "Mine",
        usesSequences = false,
        SandFlyKey.META,
        mapOf(
          context.getString(R.string.keycombo_shortcut_perform_click) to
            KeyCombo(0, 0, KeyEvent.KEYCODE_F2, false)
        ),
      )
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_select_keymap_key), custom.value)
      .commit()
    val manager = KeyComboManager.create(context).apply {
      onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
    }
    assertTrue(manager.keyComboModel.isEligibleKeyCombo(KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_N, false)))
    // F2 alone activates, outside browse mode; Ctrl alone and other keys go to the app.
    assertTrue(manager.down(KeyEvent.KEYCODE_F2))
    assertEquals(1, keys.size)
    manager.up(KeyEvent.KEYCODE_F2)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl))
    assertFalse(manager.down(KeyEvent.KEYCODE_C, ctrl))
    assertEquals(1, keys.size)

    CustomKeymaps.delete(context, custom)
    assertTrue(CustomKeymaps.all(context).isEmpty())
  }

  /** Makes [manager] find SandFly focus and input focus on a text field being edited. */
  private fun editTextField(manager: KeyComboManager) {
    val field =
      AccessibilityNodeInfoCompat.wrap(
        AccessibilityNodeInfo.obtain().apply {
          isFocused = true
          isEditable = true
        }
      )
    val monitor =
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getInputFocus() = field

        override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean) = field
      }
    KeyComboManager::class.java.getDeclaredField("accessibilityFocusMonitor").apply {
      isAccessible = true
      set(manager, monitor)
    }
  }

  private fun customKeymapWithCtrlHome(arrowsNavigate: Boolean): KeyComboManager {
    val custom =
      CustomKeymaps.create(
        context,
        "Mine",
        usesSequences = false,
        SandFlyKey.INSERT,
        mapOf(
          context.getString(R.string.keycombo_shortcut_navigate_first) to
            KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_MOVE_HOME, false)
        ),
        arrowsNavigate,
      )
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_select_keymap_key), custom.value)
      .commit()
    return KeyComboManager.create(context).apply {
      onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
      editTextField(this)
    }
  }

  @Test
  fun customKeymapCtrlShortcutWorksInATextFieldBeingEdited() {
    val manager = customKeymapWithCtrlHome(arrowsNavigate = false)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl)
    assertTrue(manager.down(KeyEvent.KEYCODE_MOVE_HOME, ctrl))
    assertEquals(1, keys.size)
  }

  @Test
  fun customKeymapWhoseArrowsNavigateLeavesCtrlHomeToATextFieldBeingEdited() {
    val manager = customKeymapWithCtrlHome(arrowsNavigate = true)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl)
    assertFalse(manager.down(KeyEvent.KEYCODE_MOVE_HOME, ctrl))
    assertTrue(keys.isEmpty())
  }

  @Test
  fun browseModeToggleIsWrittenAsTheSandFlyKeyTwice() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.META)
    val combo =
      manager.keyComboModel.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
      )
    assertEquals("Meta twice", manager.getKeyComboStringRepresentation(combo))
  }

  @Test
  fun keyboardInputOffPassesEveryKeyExceptItsToggle() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.INSERT)
    manager.toggleKeyboardInput()
    assertTrue(manager.isKeyboardInputOff)

    // The SandFly key and its shortcuts go to the app.
    assertFalse(manager.down(KeyEvent.KEYCODE_INSERT))
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertFalse(manager.up(KeyEvent.KEYCODE_DPAD_RIGHT))
    assertFalse(manager.up(KeyEvent.KEYCODE_INSERT))
    assertEquals(emptyList<String?>(), keys)

    // SandFly key + Shift + K turns keyboard input on; the keys pressed before it go to the app.
    assertFalse(manager.down(KeyEvent.KEYCODE_INSERT))
    assertFalse(manager.down(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON))
    assertTrue(manager.down(KeyEvent.KEYCODE_K, KeyEvent.META_SHIFT_ON))
    assertFalse(manager.isKeyboardInputOff)
    assertTrue(manager.up(KeyEvent.KEYCODE_K, KeyEvent.META_SHIFT_ON))
    assertFalse(manager.up(KeyEvent.KEYCODE_SHIFT_LEFT))
    assertFalse(manager.up(KeyEvent.KEYCODE_INSERT))

    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    assertTrue(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT))
    manager.up(KeyEvent.KEYCODE_DPAD_RIGHT)
    assertTrue(manager.up(KeyEvent.KEYCODE_INSERT))
    assertEquals(listOf<String?>("Insert + Arrow Right"), keys)
  }

  @Test
  fun keysHeldWhenKeyboardInputTurnsOffAreReleasedWhereTheyWerePressed() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.INSERT)
    assertTrue(manager.down(KeyEvent.KEYCODE_INSERT))
    manager.toggleKeyboardInput()
    assertTrue(manager.up(KeyEvent.KEYCODE_INSERT))
    // The next press goes to the app.
    assertFalse(manager.down(KeyEvent.KEYCODE_INSERT))
    manager.toggleKeyboardInput()
    assertFalse(manager.isKeyboardInputOff)
    assertFalse(manager.up(KeyEvent.KEYCODE_INSERT))
  }

  @Test
  fun keyboardInputOffWithMetaPassesMetaShortcuts() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.META)
    manager.toggleKeyboardInput()
    assertFalse(manager.down(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_ON))
    assertFalse(manager.down(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_META_ON))
    assertFalse(manager.up(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_META_ON))
    assertFalse(manager.up(KeyEvent.KEYCODE_META_LEFT))
    assertTrue(
      manager.down(KeyEvent.KEYCODE_K, KeyEvent.META_META_ON or KeyEvent.META_SHIFT_ON)
    )
    assertFalse(manager.isKeyboardInputOff)
    assertEquals(emptyList<String?>(), keys)
  }

  /** A key's name as shortcuts are spoken, which Robolectric gives as its number. */
  private fun name(keyCode: Int) =
    KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')

  @Test
  fun keyboardPhoneSymAloneMovesAndShiftSymMovesBack() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM))
    assertTrue(manager.up(KeyEvent.KEYCODE_SYM))
    val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_SHIFT_LEFT, shift))
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM, shift))
    manager.up(KeyEvent.KEYCODE_SYM, shift)
    manager.up(KeyEvent.KEYCODE_SHIFT_LEFT)
    assertEquals(listOf<String?>("Sym", "Sym + Shift"), keys)
    // Sym held with a key is that key's shortcut, not Sym alone.
    manager.down(KeyEvent.KEYCODE_SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_K))
    manager.up(KeyEvent.KEYCODE_K)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals("Sym + " + name(KeyEvent.KEYCODE_K), keys.last())
    assertEquals(3, keys.size)
  }

  @Test
  fun keyboardPhoneLettersSpaceAndEnterAloneAreTyping() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    for (keyCode in listOf(KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER)) {
      assertFalse(manager.down(keyCode))
      assertFalse(manager.up(keyCode))
      time += 1000
    }
    assertTrue(keys.isEmpty())
  }

  @Test
  fun keyboardPhonePressesWorkInATextFieldBeingEdited() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    editTextField(manager)
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM))
    assertTrue(manager.up(KeyEvent.KEYCODE_SYM))
    manager.down(KeyEvent.KEYCODE_SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_K))
    manager.up(KeyEvent.KEYCODE_K)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals(listOf<String?>("Sym", "Sym + " + name(KeyEvent.KEYCODE_K)), keys)
  }

  /** Makes [manager] find input focus on a text field and SandFly focus on another item. */
  private fun focusAwayFromTextField(manager: KeyComboManager) {
    val field =
      AccessibilityNodeInfoCompat.wrap(
        AccessibilityNodeInfo.obtain().apply {
          isFocused = true
          isEditable = true
          text = "field"
        }
      )
    val option =
      AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain().apply { text = "option" })
    val monitor =
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getInputFocus() = field

        override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean) = option
      }
    KeyComboManager::class.java.getDeclaredField("accessibilityFocusMonitor").apply {
      isAccessible = true
      set(manager, monitor)
    }
  }

  /** Presses Sym + K, a SandFly shortcut in both keyboard phone keymaps, to move around. */
  private fun KeyComboManager.moveWithSym() {
    down(KeyEvent.KEYCODE_SYM)
    assertTrue(down(KeyEvent.KEYCODE_K))
    up(KeyEvent.KEYCODE_K)
    up(KeyEvent.KEYCODE_SYM)
  }

  @Test
  fun keyboardPhoneEnterActivatesAfterMovingAndSendsAfterTyping() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      keys.clear()
      val manager = manager(keymap, SandFlyKey.SYM)
      focusAwayFromTextField(manager)
      // Not moved yet, with a text field taking input: Enter goes to it.
      assertFalse(manager.down(KeyEvent.KEYCODE_ENTER))
      manager.up(KeyEvent.KEYCODE_ENTER)
      // Moved to an option while a draft waits in the text field: Enter activates it.
      time += 1000
      manager.moveWithSym()
      assertTrue(manager.down(KeyEvent.KEYCODE_ENTER))
      manager.up(KeyEvent.KEYCODE_ENTER)
      assertEquals(name(KeyEvent.KEYCODE_ENTER), keys.last())
      assertEquals(2, keys.size)
      // Typed since: Enter goes to the app, which sends.
      time += 1000
      assertFalse(manager.down(KeyEvent.KEYCODE_O))
      manager.up(KeyEvent.KEYCODE_O)
      time += 1000
      assertFalse(manager.down(KeyEvent.KEYCODE_ENTER))
      manager.up(KeyEvent.KEYCODE_ENTER)
      assertEquals(2, keys.size)
    }
  }

  @Test
  fun singleKeysTypeIntoTheTextFieldWithInputFocusUntilSandFlyMoves() {
    val manager = manager(Keymap.KEYBOARD_PHONE_SINGLE_KEYS, SandFlyKey.SYM)
    focusAwayFromTextField(manager)
    // M, the SandFly menu, types an m into the message instead.
    assertFalse(manager.down(KeyEvent.KEYCODE_M))
    manager.up(KeyEvent.KEYCODE_M)
    assertTrue(keys.isEmpty())
    // After a SandFly shortcut, single keys are shortcuts until a key is typed again.
    time += 1000
    manager.moveWithSym()
    assertTrue(manager.down(KeyEvent.KEYCODE_M))
    manager.up(KeyEvent.KEYCODE_M)
    assertEquals(name(KeyEvent.KEYCODE_M), keys.last())
    time += 1000
    assertFalse(manager.down(KeyEvent.KEYCODE_O))
    manager.up(KeyEvent.KEYCODE_O)
    time += 1000
    assertFalse(manager.down(KeyEvent.KEYCODE_M))
    assertEquals(2, keys.size)
  }

  @Test
  fun keyboardPhoneKeymapsHaveArrowKeyNavigationOn() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      manager(keymap, SandFlyKey.SYM)
      assertTrue(ArrowKeyNav.isAvailable(context))
      assertTrue(ArrowKeyNav.isOn(context))
      ArrowKeyNav.setOn(context, false)
      assertFalse(ArrowKeyNav.isOn(context))
    }
    manager(Keymap.SANDFLY)
    assertTrue(ArrowKeyNav.isAvailable(context))
    assertFalse(ArrowKeyNav.isOn(context))
    manager(Keymap.NVDA)
    assertFalse(ArrowKeyNav.isAvailable(context))
  }

  @Test
  fun shortcutsWhileEditingTakeTheirKeysOnlyInATextFieldBeingEdited() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      keys.clear()
      val manager = manager(keymap, SandFlyKey.SYM)
      manager.setCommandPerformer { _, _ -> }
      val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
      // Outside a text field, Alt + Space is the app's.
      manager.down(KeyEvent.KEYCODE_ALT_LEFT, alt)
      assertFalse(manager.down(KeyEvent.KEYCODE_SPACE, alt))
      manager.up(KeyEvent.KEYCODE_SPACE, alt)
      manager.up(KeyEvent.KEYCODE_ALT_LEFT)
      assertTrue(keys.isEmpty())
      // In one being edited, it chooses a symbol, and its repeats and release are taken too.
      time += 1000
      editTextField(manager)
      manager.down(KeyEvent.KEYCODE_ALT_LEFT, alt)
      assertTrue(manager.down(KeyEvent.KEYCODE_SPACE, alt))
      assertTrue(manager.up(KeyEvent.KEYCODE_SPACE, alt))
      manager.up(KeyEvent.KEYCODE_ALT_LEFT)
      assertEquals(1, keys.size)
      assertTrue(keys.last()!!.endsWith("while editing"))
    }
  }

  @Test
  fun keyboardPhoneCommandDefaultsWorkWithoutBeingSet() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    manager.setCommandPerformer { _, _ -> }
    manager.down(KeyEvent.KEYCODE_SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_L))
    manager.up(KeyEvent.KEYCODE_L)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals("Sym + " + name(KeyEvent.KEYCODE_L), keys.last())
  }

  @Test
  fun keysPressedWhileAChooserIsOpenGoToIt() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    var choosing = false
    SandFlyKeyboardHost.keyboard =
      object : SandFlyKeyboardHost.Keyboard {
        override val isEditing = true
        override val isChoosing
          get() = choosing

        override fun openChooser(kind: CharacterChooser.Kind) = true

        override val isEditingModeOn = false

        override fun toggleEditingMode() = false
      }
    try {
      // The shortcut that opens a chooser is released as usual.
      manager.down(KeyEvent.KEYCODE_SYM)
      assertTrue(manager.down(KeyEvent.KEYCODE_K))
      choosing = true
      assertTrue(manager.up(KeyEvent.KEYCODE_K))
      manager.up(KeyEvent.KEYCODE_SYM)
      assertEquals(1, keys.size)
      // Keys pressed while it is open are its own, up to their release.
      time += 1000
      assertFalse(manager.down(KeyEvent.KEYCODE_SYM))
      choosing = false
      assertFalse(manager.up(KeyEvent.KEYCODE_SYM))
      assertEquals(1, keys.size)
      // Then SandFly has the keys again.
      time += 1000
      assertTrue(manager.down(KeyEvent.KEYCODE_SYM))
      assertTrue(manager.up(KeyEvent.KEYCODE_SYM))
      assertEquals(2, keys.size)
    } finally {
      SandFlyKeyboardHost.keyboard = null
    }
  }

  @Test
  fun keyboardPhoneTrackpadPressIsEnter() {
    for (keyCode in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
      keys.clear()
      val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
      focusAwayFromTextField(manager)
      assertFalse(manager.down(keyCode))
      manager.up(keyCode)
      time += 1000
      manager.moveWithSym()
      assertTrue(manager.down(keyCode))
      manager.up(keyCode)
      assertEquals(name(KeyEvent.KEYCODE_ENTER), keys.last())
      time += 1000
    }
  }

  @Test
  fun keyboardPhoneEnterGoesToTheTextFieldWithSandFlyFocus() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    editTextField(manager)
    manager.moveWithSym()
    keys.clear()
    assertFalse(manager.down(KeyEvent.KEYCODE_ENTER))
    assertTrue(keys.isEmpty())
  }

  @Test
  fun keyboardPhoneAltTwiceTakesOnlyTheSecondPress() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_ALT_RIGHT, alt))
    assertFalse(manager.up(KeyEvent.KEYCODE_ALT_RIGHT))
    assertTrue(manager.down(KeyEvent.KEYCODE_ALT_RIGHT, alt))
    assertTrue(manager.up(KeyEvent.KEYCODE_ALT_RIGHT))
    assertEquals(listOf<String?>("Alt twice"), keys)
  }

  @Test
  fun keyboardPhoneAltShiftHeldChoosesTheNextReadingControl() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
    val altShift = alt or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    manager.down(KeyEvent.KEYCODE_ALT_RIGHT, alt)
    manager.down(KeyEvent.KEYCODE_SHIFT_LEFT, altShift)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(KeyPresses.HOLD_MS + 50))
    assertEquals(listOf<String?>("Alt + Shift held"), keys)
    manager.up(KeyEvent.KEYCODE_SHIFT_LEFT, alt)
    manager.up(KeyEvent.KEYCODE_ALT_RIGHT)
    assertEquals(1, keys.size)
    // Alt + Shift pressed briefly does nothing.
    time += 1000
    manager.down(KeyEvent.KEYCODE_ALT_RIGHT, alt)
    manager.down(KeyEvent.KEYCODE_SHIFT_LEFT, altShift)
    manager.up(KeyEvent.KEYCODE_SHIFT_LEFT, alt)
    manager.up(KeyEvent.KEYCODE_ALT_RIGHT)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(KeyPresses.HOLD_MS + 50))
    assertEquals(1, keys.size)
  }

  @Test
  fun keyboardPhoneSingleKeyKeymapTypesInTextFieldsAndSymLeavesThem() {
    val manager = manager(Keymap.KEYBOARD_PHONE_SINGLE_KEYS, SandFlyKey.SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_SPACE))
    manager.up(KeyEvent.KEYCODE_SPACE)
    assertTrue(manager.down(KeyEvent.KEYCODE_K))
    manager.up(KeyEvent.KEYCODE_K)
    // O has no shortcut: typing.
    assertFalse(manager.down(KeyEvent.KEYCODE_O))
    manager.up(KeyEvent.KEYCODE_O)
    assertEquals(listOf<String?>(name(KeyEvent.KEYCODE_SPACE), name(KeyEvent.KEYCODE_K)), keys)

    editTextField(manager)
    time += 1000
    assertFalse(manager.down(KeyEvent.KEYCODE_SPACE))
    manager.up(KeyEvent.KEYCODE_SPACE)
    // Sym + Space moves out of the text field.
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM))
    assertTrue(manager.down(KeyEvent.KEYCODE_SPACE))
    manager.up(KeyEvent.KEYCODE_SPACE)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals(name(KeyEvent.KEYCODE_SPACE), keys.last())
    assertEquals(3, keys.size)
  }

  @Test
  fun keyboardPhonePassNextKeyGivesSymToTheKeyboard() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    editTextField(manager)
    manager.down(KeyEvent.KEYCODE_SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_P))
    // Sym + P is Pass the next key, which the tutorial hook here takes before it runs.
    assertEquals(listOf<String?>("Sym + " + name(KeyEvent.KEYCODE_P)), keys)
    val state = KeyComboManager::class.java.getDeclaredField("keyComboPassThroughState")
    state.isAccessible = true
    val states = state.type.enumConstants!!.associateBy { (it as Enum<*>).name }
    state.set(manager, states["PENDING_PASS_THROUGH_SHORTCUT_RELEASE"])
    manager.up(KeyEvent.KEYCODE_P)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals(states["ACTIVE"], state.get(manager))
    // The next press, Sym with a letter for a symbol, goes to the keyboard.
    assertFalse(manager.down(KeyEvent.KEYCODE_SYM))
    assertFalse(manager.down(KeyEvent.KEYCODE_A))
    manager.up(KeyEvent.KEYCODE_A)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals(1, keys.size)
  }

  @Test
  fun keyboardPhoneCtrlAsSandFlyKeyLeavesSymToTheKeyboard() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.CTRL)
    editTextField(manager)
    val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    assertTrue(manager.down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl))
    assertTrue(manager.up(KeyEvent.KEYCODE_CTRL_LEFT))
    assertEquals(listOf<String?>("Ctrl"), keys)
    time += 1000
    assertFalse(manager.down(KeyEvent.KEYCODE_SYM))
    assertFalse(manager.down(KeyEvent.KEYCODE_A))
    manager.up(KeyEvent.KEYCODE_A)
    assertFalse(manager.up(KeyEvent.KEYCODE_SYM))
    assertEquals(1, keys.size)
  }

  @Test
  fun rightShiftHeldIsTheSandFlyKeyWithoutShift() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_SHIFT)
    val rightShift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON
    assertTrue(manager.down(KeyEvent.KEYCODE_SHIFT_RIGHT, rightShift))
    assertTrue(manager.down(KeyEvent.KEYCODE_K, rightShift))
    manager.up(KeyEvent.KEYCODE_K, rightShift)
    manager.up(KeyEvent.KEYCODE_SHIFT_RIGHT)
    assertEquals(listOf<String?>("Right Shift + " + name(KeyEvent.KEYCODE_K)), keys)
  }

  @Test
  fun keyHeldDownRunsItsShortcutOutsideTypingAndPressedBrieflyItsOwn() {
    val manager = manager(Keymap.KEYBOARD_PHONE_SINGLE_KEYS, SandFlyKey.SYM)
    val readFromTop = context.getString(R.string.keycombo_shortcut_other_read_from_top)
    val kHeld = KeyCombo(0, 0, KeyEvent.KEYCODE_K, false).withPressType(KeyCombo.HELD_DOWN)
    manager.keyComboModel.saveKeyCombo(readFromTop, kHeld)
    // Held: its shortcut, once, and the key doesn't reach the app.
    assertTrue(manager.down(KeyEvent.KEYCODE_K))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(KeyPresses.HOLD_MS + 50))
    assertTrue(manager.up(KeyEvent.KEYCODE_K))
    assertEquals(listOf<String?>(name(KeyEvent.KEYCODE_K) + " held"), keys)
    // Pressed briefly: K's own shortcut.
    time += 1000
    assertTrue(manager.down(KeyEvent.KEYCODE_K))
    assertTrue(manager.up(KeyEvent.KEYCODE_K))
    assertEquals(name(KeyEvent.KEYCODE_K), keys.last())
    // Typing: K goes to the text field, held or not.
    editTextField(manager)
    time += 1000
    assertFalse(manager.down(KeyEvent.KEYCODE_K))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(KeyPresses.HOLD_MS + 50))
    manager.up(KeyEvent.KEYCODE_K)
    assertEquals(2, keys.size)
  }

  @Test
  fun symInShortcutsWhereAltIsTheSandFlyKey() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.ALT)
    val model = manager.keyComboModel
    val sym = KeyEvent.META_SYM_ON
    model.saveKeyCombo(
      context.getString(R.string.keycombo_shortcut_other_read_from_top),
      KeyCombo(sym, 0, KeyEvent.KEYCODE_K, false),
    )
    model.saveKeyCombo(
      context.getString(R.string.keycombo_shortcut_global_announce_battery_state),
      KeyCombo.modifierKeys(sym, false),
    )
    editTextField(manager)
    // Sym + K works in a text field too. Sym alone has a shortcut, so Sym's press is SandFly's.
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM, sym))
    assertTrue(manager.down(KeyEvent.KEYCODE_K, sym))
    manager.up(KeyEvent.KEYCODE_K, sym)
    manager.up(KeyEvent.KEYCODE_SYM)
    // Sym alone.
    time += 1000
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM, sym))
    assertTrue(manager.up(KeyEvent.KEYCODE_SYM))
    assertEquals(listOf<String?>("Sym + " + name(KeyEvent.KEYCODE_K), "Sym"), keys)
  }

  @Test
  fun keyboardPhoneSandFlyKeyShortcuts() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    assertTrue(manager.down(KeyEvent.KEYCODE_SYM))
    assertTrue(manager.down(KeyEvent.KEYCODE_P))
    manager.up(KeyEvent.KEYCODE_P)
    manager.up(KeyEvent.KEYCODE_SYM)
    assertEquals(listOf<String?>("Sym + " + name(KeyEvent.KEYCODE_P)), keys)
    // P alone has no shortcut.
    assertFalse(manager.down(KeyEvent.KEYCODE_P))
  }

  @Test
  fun rightAltHeldIsTheSandFlyKeyWithoutAlt() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.RIGHT_ALT)
    editTextField(manager)
    val rightAlt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
    assertTrue(manager.down(KeyEvent.KEYCODE_ALT_RIGHT, rightAlt))
    assertTrue(manager.down(KeyEvent.KEYCODE_K, rightAlt))
    manager.up(KeyEvent.KEYCODE_K, rightAlt)
    manager.up(KeyEvent.KEYCODE_ALT_RIGHT)
    assertEquals(listOf<String?>("Right Alt + " + name(KeyEvent.KEYCODE_K)), keys)

    // Left Alt with a letter types a digit.
    val leftAlt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    assertFalse(manager.down(KeyEvent.KEYCODE_ALT_LEFT, leftAlt))
    assertFalse(manager.down(KeyEvent.KEYCODE_K, leftAlt))
    assertEquals(1, keys.size)
  }

  @Test
  fun altAsKeyboardPhoneSandFlyKeyTypesInTextFields() {
    val manager = manager(Keymap.KEYBOARD_PHONE, SandFlyKey.ALT)
    editTextField(manager)
    val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
    manager.down(KeyEvent.KEYCODE_ALT_RIGHT, alt)
    // Alt + U types 1 on the Unihertz Titan.
    assertFalse(manager.down(KeyEvent.KEYCODE_U, alt))
    manager.up(KeyEvent.KEYCODE_U, alt)
    // Alt + Backspace is Back.
    assertTrue(manager.down(KeyEvent.KEYCODE_DEL, alt))
    manager.up(KeyEvent.KEYCODE_DEL, alt)
    manager.up(KeyEvent.KEYCODE_ALT_RIGHT)
    assertEquals(listOf<String?>("Alt + Backspace"), keys)
  }
}
