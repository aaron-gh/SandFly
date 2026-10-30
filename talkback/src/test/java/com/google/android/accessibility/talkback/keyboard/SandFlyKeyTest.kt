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

  @Test
  fun browseModeToggleIsWrittenAsTheSandFlyKeyTwice() {
    val manager = manager(Keymap.SANDFLY, SandFlyKey.META)
    val combo =
      manager.keyComboModel.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)
      )
    assertEquals("Meta twice", manager.getKeyComboStringRepresentation(combo))
  }
}
