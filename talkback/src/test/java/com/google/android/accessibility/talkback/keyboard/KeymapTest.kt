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
import com.google.android.accessibility.utils.FormFactorUtils
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
class KeymapTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  @Test
  fun removedKeymapsBecomeTheSandFlyKeymap() {
    assertEquals(Keymap.SANDFLY, Keymap.of(context, "default_keymap"))
    assertEquals(Keymap.SANDFLY, Keymap.of(context, "classic_keymap"))
    assertEquals(Keymap.SANDFLY, Keymap.of(context, null))
    assertEquals(Keymap.SANDFLY, Keymap.of(context, "new_keymap"))
    assertEquals(Keymap.VOICEOVER, Keymap.of(context, "voiceover_keymap"))
  }

  private fun choose(sandFlyKey: SandFlyKey) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_sandfly_key), context.getString(sandFlyKey.valueRes))
      .commit()
  }

  @Test
  fun everyKeymapUsesMetaUntilAnotherKeyIsChosen() {
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(SandFlyKey.META, model.sandFlyKey)
    assertEquals(KeyEvent.META_META_ON, model.triggerModifier)
    // NVDA's Insert + Shift + Right.
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true),
      model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_default)),
    )
  }

  @Test
  fun voiceOverCommandShortcutsKeepTheirKeysWithMeta() {
    choose(SandFlyKey.META)
    val model = NewKeyComboModel(context, Keymap.VOICEOVER)
    // VO + Command + Right is Ctrl + Alt + Meta + Right; with Meta as the SandFly key, Ctrl + Alt
    // are the other keys.
    assertEquals(
      KeyCombo(KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true),
      model.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_global_scroll_forward_reading_menu)
      ),
    )
  }

  @Test
  fun chosenSandFlyKeyIsUsed() {
    choose(SandFlyKey.INSERT)
    val model = NewKeyComboModel(context, Keymap.SANDFLY)
    assertEquals(SandFlyKey.INSERT, model.sandFlyKey)
    assertEquals(KeyComboModel.HELD_KEY_MODIFIER, model.triggerModifier)
    // Insert alone can't be a shortcut.
    assertFalse(model.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_INSERT, true)))
    assertTrue(model.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_J, true)))
  }

  @Test
  fun defaultShortcutsAreUniqueWithEverySandFlyKey() {
    for (keymap in Keymap.entries) {
      for (sandFlyKey in keymap.sandFlyKeys) {
        choose(sandFlyKey)
        val model = NewKeyComboModel(context, keymap)
        val seen = mutableMapOf<KeyCombo, String>()
        for (key in model.keyComboMap.keys) {
          val combo = model.getDefaultKeyCombo(key)
          if (combo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) continue
          val other = seen.put(combo, key)
          assertEquals("$keymap with $sandFlyKey: $key and $other share $combo", null, other)
        }
        println("$keymap with $sandFlyKey: ${seen.size} default shortcuts")
      }
    }
  }

  @Test
  fun defaultShortcutsCanBePressedWithEverySandFlyKey() {
    for (keymap in Keymap.entries) {
      for (sandFlyKey in keymap.sandFlyKeys) {
        choose(sandFlyKey)
        val model = NewKeyComboModel(context, keymap)
        for (key in model.keyComboMap.keys) {
          val combo = model.getDefaultKeyCombo(key)
          if (combo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) continue
          val shared = combo.modifiers and sandFlyKey.modifiers
          if (combo.isTriggerModifierUsed) {
            assertEquals("$keymap with $sandFlyKey: $key", 0, shared)
          } else {
            assertTrue("$keymap with $sandFlyKey: $key", shared != sandFlyKey.modifiers)
          }
        }
      }
    }
  }

  @Test
  fun screenReaderKeymapsOfferOneKeySandFlyKeysOnly() {
    for (keymap in listOf(Keymap.NVDA, Keymap.JAWS, Keymap.ORCA)) {
      assertFalse(SandFlyKey.CTRL_ALT in keymap.sandFlyKeys)
      assertFalse(SandFlyKey.CTRL_META in keymap.sandFlyKeys)
      assertTrue(SandFlyKey.ALT in keymap.sandFlyKeys)
    }
    assertEquals(SandFlyKey.entries, Keymap.SANDFLY.sandFlyKeys)
    choose(SandFlyKey.CTRL_ALT)
    // The keymap's own key, where it doesn't offer the chosen one.
    assertEquals(SandFlyKey.INSERT, NewKeyComboModel(context, Keymap.NVDA).sandFlyKey)
    assertEquals(SandFlyKey.CTRL_ALT, NewKeyComboModel(context, Keymap.VOICEOVER).sandFlyKey)
  }

  @Test
  fun browseModeTogglesWithTheSandFlyKeyTwiceOnlyWhereAssigned() {
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)
    val unassigned = KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()
    choose(SandFlyKey.META)
    assertTrue(NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).keyComboCode != unassigned)
    assertEquals(unassigned, NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key).keyComboCode)
    choose(SandFlyKey.ALT)
    assertTrue(NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).keyComboCode != unassigned)
    choose(SandFlyKey.CTRL_ALT)
    assertEquals(unassigned, NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).keyComboCode)
  }

  @Test
  fun numpadZeroIsInsertOnlyWithNumLockOff() {
    val numLockOff = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_NUMPAD_0, 0, 0)
    val numLockOn =
      KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_NUMPAD_0, 0, KeyEvent.META_NUM_LOCK_ON)
    assertTrue(SandFlyKey.NUMPAD_INSERT.isKey(numLockOff))
    assertFalse(SandFlyKey.NUMPAD_INSERT.isKey(numLockOn))
    assertFalse(SandFlyKey.META.isKey(numLockOff))
  }
}
