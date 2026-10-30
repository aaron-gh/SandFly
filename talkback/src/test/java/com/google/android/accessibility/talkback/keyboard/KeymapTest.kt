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

  private fun choose(keymap: Keymap, sandFlyKey: SandFlyKey) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(keymap.sandFlyKeyPref, context.getString(sandFlyKey.valueRes))
      .commit()
  }

  @Test
  fun eachKeymapUsesItsOwnKeyUntilAnotherIsChosen() {
    assertEquals(SandFlyKey.META, NewKeyComboModel(context, Keymap.SANDFLY).sandFlyKey)
    assertEquals(SandFlyKey.CTRL_ALT, NewKeyComboModel(context, Keymap.VOICEOVER).sandFlyKey)
    val nvda = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(SandFlyKey.INSERT, nvda.sandFlyKey)
    // NVDA's Insert + Shift + Right.
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true),
      nvda.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_default)),
    )
  }

  @Test
  fun keymapsOfferTheirOwnKeys() {
    assertEquals(
      listOf(SandFlyKey.CTRL_ALT, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
      Keymap.VOICEOVER.sandFlyKeys,
    )
    assertEquals(
      listOf(SandFlyKey.META, SandFlyKey.INSERT, SandFlyKey.NUMPAD_INSERT),
      Keymap.SANDFLY.sandFlyKeys,
    )
    for (keymap in listOf(Keymap.NVDA, Keymap.JAWS, Keymap.ORCA)) {
      assertFalse(SandFlyKey.CTRL_ALT in keymap.sandFlyKeys)
      assertFalse(SandFlyKey.CTRL_META in keymap.sandFlyKeys)
    }
  }

  @Test
  fun voiceOverCommandShortcutsWithInsert() {
    choose(Keymap.VOICEOVER, SandFlyKey.INSERT)
    val model = NewKeyComboModel(context, Keymap.VOICEOVER)
    // VO + Command + Right is Insert + Meta + Right.
    assertEquals(
      KeyCombo(KeyEvent.META_META_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true),
      model.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_global_scroll_forward_reading_menu)
      ),
    )
  }

  @Test
  fun chosenSandFlyKeyIsUsed() {
    choose(Keymap.SANDFLY, SandFlyKey.INSERT)
    val model = NewKeyComboModel(context, Keymap.SANDFLY)
    assertEquals(SandFlyKey.INSERT, model.sandFlyKey)
    assertEquals(KeyComboModel.HELD_KEY_MODIFIER, model.triggerModifier)
    // Insert alone can't be a shortcut.
    assertFalse(model.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_INSERT, true)))
    assertTrue(model.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_J, true)))
  }

  /** The defaults of [keymap] with [sandFlyKey], unassigned ones left out. */
  private fun defaults(keymap: Keymap, sandFlyKey: SandFlyKey): Map<String, KeyCombo> {
    choose(keymap, sandFlyKey)
    val model = NewKeyComboModel(context, keymap)
    return model.keyComboMap.keys
      .associateWith { model.getDefaultKeyCombo(it) }
      .filterValues { it.keyComboCode != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong() }
  }

  @Test
  fun everyOfferedSandFlyKeyKeepsEveryDefault() {
    for (keymap in Keymap.entries) {
      val own = defaults(keymap, keymap.nativeKey).keys
      for (sandFlyKey in keymap.sandFlyKeys) {
        assertEquals("$keymap with $sandFlyKey", own, defaults(keymap, sandFlyKey).keys)
      }
    }
  }

  @Test
  fun defaultShortcutsAreUniqueAndPressableWithEveryOfferedKey() {
    for (keymap in Keymap.entries) {
      for (sandFlyKey in keymap.sandFlyKeys) {
        val seen = mutableMapOf<KeyCombo, String>()
        for ((key, combo) in defaults(keymap, sandFlyKey)) {
          val other = seen.put(combo, key)
          assertEquals("$keymap with $sandFlyKey: $key and $other share $combo", null, other)
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
  fun browseModeTogglesWithTheSandFlyKeyTwiceOnlyWhereAssigned() {
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)
    val unassigned = KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()
    assertTrue(NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).keyComboCode != unassigned)
    assertEquals(unassigned, NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key).keyComboCode)
    assertEquals(unassigned, NewKeyComboModel(context, Keymap.VOICEOVER).getKeyComboForKey(key).keyComboCode)
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
