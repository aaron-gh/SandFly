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
import com.google.android.accessibility.talkback.commands.CommandBindings
import com.google.android.accessibility.talkback.commands.Commands
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
      nvda.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_object_next)),
    )
  }

  @Test
  fun computerKeymapsMoveWithArrowsAndTab() {
    for (keymap in listOf(Keymap.NVDA, Keymap.JAWS, Keymap.ORCA)) {
      assertTrue(keymap.arrowsNavigate)
      val model = NewKeyComboModel(context, keymap)
      fun key(res: Int) = model.getKeyComboForKey(context.getString(res))
      fun plain(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, false)
      assertEquals(plain(0, KeyEvent.KEYCODE_DPAD_DOWN), key(R.string.keycombo_shortcut_navigate_next_default))
      assertEquals(plain(0, KeyEvent.KEYCODE_DPAD_UP), key(R.string.keycombo_shortcut_navigate_previous_default))
      assertEquals(plain(0, KeyEvent.KEYCODE_DPAD_RIGHT), key(R.string.keycombo_shortcut_navigate_next_character))
      assertEquals(plain(0, KeyEvent.KEYCODE_DPAD_LEFT), key(R.string.keycombo_shortcut_navigate_previous_character))
      assertEquals(
        plain(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_RIGHT),
        key(R.string.keycombo_shortcut_navigate_next_word),
      )
      assertEquals(
        plain(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_DOWN),
        key(R.string.keycombo_shortcut_navigate_next_paragraph),
      )
      assertEquals(plain(0, KeyEvent.KEYCODE_TAB), key(R.string.keycombo_shortcut_navigate_next_focusable))
      assertEquals(
        plain(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_TAB),
        key(R.string.keycombo_shortcut_navigate_previous_focusable),
      )
    }
    assertFalse(Keymap.SANDFLY.arrowsNavigate)
    assertFalse(Keymap.VOICEOVER.arrowsNavigate)
  }

  @Test
  fun nvdaReviewAndObjectKeysAreItsLaptopKeys() {
    val model = NewKeyComboModel(context, Keymap.NVDA)
    fun key(res: Int) = model.getKeyComboForKey(context.getString(res))
    fun nvda(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, true)
    assertEquals(nvda(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_DPAD_UP), key(R.string.keycombo_shortcut_object_parent))
    assertEquals(nvda(0, KeyEvent.KEYCODE_DPAD_DOWN), key(R.string.keycombo_shortcut_review_next_line))
    assertEquals(nvda(0, KeyEvent.KEYCODE_PERIOD), key(R.string.keycombo_shortcut_review_current_character))
    assertEquals(nvda(0, KeyEvent.KEYCODE_R), key(R.string.keycombo_shortcut_recognize_screen_text))
    assertEquals(nvda(0, KeyEvent.KEYCODE_A), key(R.string.keycombo_shortcut_other_read_from_cursor_item))
  }

  @Test
  fun savedOldDefaultsGiveWayToNewOnes() {
    val persister = KeyComboPersister(context, Keymap.NVDA.persisterPrefix)
    val nextItem = context.getString(R.string.keycombo_shortcut_navigate_next_default)
    val readFromCursor = context.getString(R.string.keycombo_shortcut_other_read_from_cursor_item)
    // Version 1 saved its default for moving to the next item; the user changed reading from the
    // cursor.
    persister.saveKeyCombo(nextItem, KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true))
    persister.saveKeyCombo(readFromCursor, KeyCombo(0, 0, KeyEvent.KEYCODE_F9, true))
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_DPAD_DOWN, false), model.getKeyComboForKey(nextItem))
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_F9, true), model.getKeyComboForKey(readFromCursor))
    // Only once: a later change stays.
    persister.saveKeyCombo(nextItem, KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true))
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_DPAD_RIGHT, true),
      NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(nextItem),
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
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
    assertTrue(NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).isSandFlyKeyPressedTwice)
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_SPACE, true),
      NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key),
    )
    assertFalse(NewKeyComboModel(context, Keymap.VOICEOVER).getKeyComboForKey(key).isSandFlyKeyPressedTwice)
  }

  @Test
  fun bothOldBrowseModeShortcutsBecomeTheSandFlyKeyTwice() {
    val persister = KeyComboPersister(context, Keymap.SANDFLY.persisterPrefix)
    val oldKey = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
    persister.saveKeyCombo(oldKey, KeyCombo.sandFlyKeyPressedTwice())
    persister.saveKeyCombo(key, KeyCombo(0, 0, KeyEvent.KEYCODE_Q, true))
    assertTrue(NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key).isSandFlyKeyPressedTwice)
    assertFalse(persister.contains(oldKey))
  }

  @Test
  fun aChangedBrowseModeShortcutStays() {
    val persister = KeyComboPersister(context, Keymap.SANDFLY.persisterPrefix)
    val oldKey = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode)
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode_extra)
    persister.saveKeyCombo(oldKey, KeyCombo.sandFlyKeyPressedTwice())
    persister.saveKeyCombo(key, KeyCombo(0, 0, KeyEvent.KEYCODE_B, true))
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_B, true),
      NewKeyComboModel(context, Keymap.SANDFLY).getKeyComboForKey(key),
    )
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

  @Test
  fun keyboardOnlyCommandsKeepShortcutsUnderTheirActions() {
    val urlKey = context.getString(R.string.keycombo_shortcut_other_read_current_url)
    val oldKey = Commands.shortcutKey(Commands.KEYBOARD_PREFIX + urlKey)
    val persister = KeyComboPersister(context, Keymap.SANDFLY.persisterPrefix)
    val shortcut = KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_U, true)
    persister.saveKeyCombo(oldKey, shortcut)
    val model = NewKeyComboModel(context, Keymap.SANDFLY)
    assertEquals(shortcut, model.getKeyComboForKey(urlKey))
    assertFalse(persister.contains(oldKey))
    val command = Commands.find(context, Commands.KEYBOARD_PREFIX + urlKey)!!
    assertEquals(urlKey, CommandBindings.keyboardKey(context, model, command))
  }

  @Test
  fun computerKeymapsGoBackWithEscapeAndHomeWithShowDesktop() {
    fun plain(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, false)
    val home = mapOf(
      Keymap.NVDA to KeyEvent.META_META_ON,
      Keymap.JAWS to KeyEvent.META_META_ON,
      Keymap.ORCA to (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON),
    )
    for ((keymap, modifiers) in home) {
      val model = NewKeyComboModel(context, keymap)
      assertEquals(
        plain(0, KeyEvent.KEYCODE_ESCAPE),
        model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_global_back)),
      )
      assertEquals(
        plain(modifiers, KeyEvent.KEYCODE_D),
        model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_global_home)),
      )
    }
  }

  @Test
  fun version2SavedSystemKeysGiveWayToNewOnes() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(Keymap.JAWS.persisterPrefix + "_defaults_version", 2).commit()
    val persister = KeyComboPersister(context, Keymap.JAWS.persisterPrefix)
    val back = context.getString(R.string.keycombo_shortcut_global_back)
    persister.saveKeyCombo(back, KeyCombo())
    val model = NewKeyComboModel(context, Keymap.JAWS)
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_ESCAPE, false), model.getKeyComboForKey(back))
  }

  @Test
  fun showDesktopWithMetaAsSandFlyKeyIsSandFlyKeyPlusD() {
    choose(Keymap.NVDA, SandFlyKey.META)
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_D, true),
      NewKeyComboModel(context, Keymap.NVDA)
        .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_global_home)),
    )
  }

  @Test
  fun computerKeymapsPassKeysToTheApp() {
    fun sandFly(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, true)
    val keys =
      mapOf(
        Keymap.NVDA to (sandFly(0, KeyEvent.KEYCODE_F2) to sandFly(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_SPACE)),
        Keymap.JAWS to (sandFly(0, KeyEvent.KEYCODE_3) to sandFly(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_Z)),
        Keymap.ORCA to (sandFly(0, KeyEvent.KEYCODE_DEL) to sandFly(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_A)),
      )
    for ((keymap, expected) in keys) {
      val model = NewKeyComboModel(context, keymap)
      assertEquals(
        expected.first,
        model.getKeyComboForKey(
          context.getString(R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once)
        ),
      )
      assertEquals(
        expected.second,
        model.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_toggle_pass_keys_to_app)),
      )
    }
  }

  @Test
  fun passingKeysIsRememberedForEachApp() {
    assertFalse(PassKeysApps.isOn(context, "com.example.game"))
    assertTrue(PassKeysApps.toggle(context, "com.example.game"))
    assertTrue(PassKeysApps.isOn(context, "com.example.game"))
    assertFalse(PassKeysApps.isOn(context, "com.example.other"))
    assertFalse(PassKeysApps.toggle(context, "com.example.game"))
    assertFalse(PassKeysApps.isOn(context, "com.example.game"))
  }

  @Test
  fun keyboardInputTurnsOnAndOffInEveryKeymapWithEveryOfferedKey() {
    val key = context.getString(R.string.keycombo_shortcut_toggle_keyboard_input)
    for (keymap in Keymap.entries) {
      for (sandFlyKey in keymap.sandFlyKeys) {
        SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
        choose(keymap, sandFlyKey)
        val combo = NewKeyComboModel(context, keymap).getKeyComboForKey(key)
        assertTrue("$keymap with $sandFlyKey", combo.isTriggerModifierUsed)
        assertTrue("$keymap with $sandFlyKey", combo.keyCode != 0)
      }
    }
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_K, true),
      NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key),
    )
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_SEMICOLON, true),
      NewKeyComboModel(context, Keymap.VOICEOVER).getKeyComboForKey(key),
    )
    assertEquals(
      KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_SEMICOLON, true),
      NewKeyComboModel(context, Keymap.ORCA).getKeyComboForKey(key),
    )
  }

  @Test
  fun nvdaVoiceSettingsAndLastSpokenPhraseKeys() {
    fun nvda(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, true)
    val repeat = context.getString(R.string.keycombo_shortcut_other_repeat_last_spoken_phrase)
    val copy = context.getString(R.string.keycombo_shortcut_other_copy_last_spoken_phrase)
    val tts = context.getString(R.string.keycombo_shortcut_other_open_tts_settings)
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(nvda(0, KeyEvent.KEYCODE_X), model.getKeyComboForKey(repeat))
    assertEquals(nvda(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_X), model.getKeyComboForKey(copy))
    assertEquals(nvda(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_V), model.getKeyComboForKey(tts))
  }

  @Test
  fun nvdaParagraphKeysAreCtrlArrowsAndPInBrowseMode() {
    fun plain(modifiers: Int, keyCode: Int) = KeyCombo(modifiers, 0, keyCode, false)
    val model = NewKeyComboModel(context, Keymap.NVDA)
    fun key(res: Int) = model.getKeyComboForKey(context.getString(res))
    assertEquals(
      plain(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_DOWN),
      key(R.string.keycombo_shortcut_navigate_next_paragraph),
    )
    assertEquals(
      plain(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_DPAD_UP),
      key(R.string.keycombo_shortcut_navigate_previous_paragraph),
    )
    assertEquals(plain(0, KeyEvent.KEYCODE_P), key(R.string.keycombo_shortcut_navigate_next_paragraph_extra))
    assertEquals(
      plain(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_P),
      key(R.string.keycombo_shortcut_navigate_previous_paragraph_extra),
    )
    // Other keymaps have no second paragraph keys.
    for (keymap in listOf(Keymap.SANDFLY, Keymap.JAWS, Keymap.ORCA, Keymap.VOICEOVER)) {
      assertEquals(
        KeyCombo(),
        NewKeyComboModel(context, keymap)
          .getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_paragraph_extra)),
      )
    }
  }

  @Test
  fun savedNvdaKeymapGetsPForParagraphsAndKeepsCtrlArrows() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(Keymap.NVDA.persisterPrefix + "_defaults_version", 6).commit()
    val persister = KeyComboPersister(context, Keymap.NVDA.persisterPrefix)
    val next = context.getString(R.string.keycombo_shortcut_navigate_next_paragraph)
    val ctrlDown = KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_DPAD_DOWN, false)
    persister.saveKeyCombo(next, ctrlDown)
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(ctrlDown, model.getKeyComboForKey(next))
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_P, false),
      model.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_navigate_next_paragraph_extra)
      ),
    )
  }

  @Test
  fun pGivenToAnotherCommandIsntTakenForParagraphs() {
    val persister = KeyComboPersister(context, Keymap.NVDA.persisterPrefix)
    val heading = context.getString(R.string.keycombo_shortcut_navigate_next_heading)
    val p = KeyCombo(0, 0, KeyEvent.KEYCODE_P, false)
    persister.saveKeyCombo(heading, p)
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(p, model.getKeyComboForKey(heading))
    assertEquals(
      KeyCombo(),
      model.getKeyComboForKey(
        context.getString(R.string.keycombo_shortcut_navigate_next_paragraph_extra)
      ),
    )
  }

  @Test
  fun secondParagraphKeysAreListedWithTheParagraphCommands() {
    val commands = Commands.categories(context).flatMap { it.commands }
    val next =
      commands.first { it.keyboardAction == TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_PARAGRAPH }
    assertEquals(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_PARAGRAPH_EXTRA, next.secondKeyboardAction)
    // A key press of the second key is the same command.
    assertEquals(
      next.value,
      Commands.valueOfKeyboardAction(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_PARAGRAPH_EXTRA),
    )
    assertEquals(
      "Navigate to next paragraph, second key",
      Commands.labelOfKeyboardKey(
        context,
        context.getString(R.string.keycombo_shortcut_navigate_next_paragraph_extra),
      ),
    )
  }

  @Test
  fun version4SavedSpeechKeysGiveWayToNewOnes() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(Keymap.NVDA.persisterPrefix + "_defaults_version", 4).commit()
    val persister = KeyComboPersister(context, Keymap.NVDA.persisterPrefix)
    val copy = context.getString(R.string.keycombo_shortcut_other_copy_last_spoken_phrase)
    val tts = context.getString(R.string.keycombo_shortcut_other_open_tts_settings)
    persister.saveKeyCombo(copy, KeyCombo())
    // The user gave Insert + Ctrl + V to another command.
    val other = context.getString(R.string.keycombo_shortcut_global_announce_current_title)
    persister.saveKeyCombo(other, KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_V, true))
    persister.saveKeyCombo(tts, KeyCombo())
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(
      KeyCombo(KeyEvent.META_CTRL_ON, 0, KeyEvent.KEYCODE_X, true),
      model.getKeyComboForKey(copy),
    )
    assertEquals(KeyCombo(), model.getKeyComboForKey(tts))
  }

  @Test
  fun repeatLastSpokenPhraseShortcutMovesToItsKeyboardAction() {
    val value = context.getString(R.string.shortcut_value_repeat_last_spoken_phrase)
    val oldKey = Commands.shortcutKey(value)
    val actionKey = context.getString(R.string.keycombo_shortcut_other_repeat_last_spoken_phrase)
    val persister = KeyComboPersister(context, Keymap.SANDFLY.persisterPrefix)
    val shortcut = KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_L, true)
    persister.saveKeyCombo(oldKey, shortcut)
    val model = NewKeyComboModel(context, Keymap.SANDFLY)
    assertEquals(shortcut, model.getKeyComboForKey(actionKey))
    assertFalse(persister.contains(oldKey))
    val command = Commands.find(context, value)!!
    assertEquals(actionKey, CommandBindings.keyboardKey(context, model, command))
  }

  @Test
  fun customKeymapCommandShortcutsMoveToTheirActionsWithTheirDefaults() {
    val repeatKey = context.getString(R.string.keycombo_shortcut_other_repeat_last_spoken_phrase)
    val repeatOld =
      Commands.shortcutKey(context.getString(R.string.shortcut_value_repeat_last_spoken_phrase))
    val urlKey = context.getString(R.string.keycombo_shortcut_other_read_current_url)
    val urlOld = Commands.shortcutKey(Commands.KEYBOARD_PREFIX + urlKey)
    val repeat = KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_L, true)
    val url = KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_U, true)
    val custom =
      CustomKeymaps.create(
        context,
        "Mine",
        usesSequences = false,
        SandFlyKey.META,
        mapOf(repeatOld to repeat, urlOld to url),
      )
    val model = NewKeyComboModel(context, custom)
    assertEquals(repeat, model.getKeyComboForKey(repeatKey))
    assertEquals(repeat, model.getDefaultKeyCombo(repeatKey))
    assertEquals(url, model.getKeyComboForKey(urlKey))
    assertEquals(url, model.getDefaultKeyCombo(urlKey))
    for (prefix in listOf(custom.persisterPrefix, custom.originalPersisterPrefix)) {
      val persister = KeyComboPersister(context, prefix)
      assertFalse(prefix, persister.contains(repeatOld))
      assertFalse(prefix, persister.contains(urlOld))
    }
    assertFalse(model.keyComboMap.containsKey(repeatOld))
    CustomKeymaps.delete(context, custom)
  }

  @Test
  fun keyboardPhoneKeymapTakesKeysWithoutTheSandFlyKey() {
    val phone = NewKeyComboModel(context, Keymap.KEYBOARD_PHONE)
    assertTrue(phone.plainKeys())
    assertTrue(phone.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_Y, false)))
    assertEquals(
      KeyCombo.modifierKeys(0, true),
      phone.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_navigate_next_default)),
    )
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_ENTER, false),
      phone.getKeyComboForKey(context.getString(R.string.keycombo_shortcut_perform_click)),
    )
    val sandFly = NewKeyComboModel(context, Keymap.SANDFLY)
    assertFalse(sandFly.plainKeys())
    assertFalse(sandFly.isEligibleKeyCombo(KeyCombo(0, 0, KeyEvent.KEYCODE_Y, false)))
  }

  @Test
  fun firstKeyboardPhoneSingleKeysGiveWayToPresses() {
    choose(Keymap.KEYBOARD_PHONE, SandFlyKey.SYM)
    val prefix = Keymap.KEYBOARD_PHONE.persisterPrefix
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(prefix + "_defaults_version", 5).commit()
    val persister = KeyComboPersister(context, prefix)
    val back = context.getString(R.string.keycombo_shortcut_global_back)
    val nextItem = context.getString(R.string.keycombo_shortcut_navigate_next_default)
    val playPause = context.getString(R.string.keycombo_shortcut_global_play_pause_media)
    val title = context.getString(R.string.keycombo_shortcut_global_announce_current_title)
    // The first keyboard phone keymap saved Backspace alone for Back, Space for the next item and
    // nothing for play and pause; the user changed the window title to Sym + Y.
    persister.saveKeyCombo(back, KeyCombo(0, 0, KeyEvent.KEYCODE_DEL, false))
    persister.saveKeyCombo(nextItem, KeyCombo(0, 0, KeyEvent.KEYCODE_SPACE, false))
    persister.saveKeyCombo(playPause, KeyCombo())
    persister.saveKeyCombo(title, KeyCombo(0, 0, KeyEvent.KEYCODE_Y, true))
    val model = NewKeyComboModel(context, Keymap.KEYBOARD_PHONE)
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_DEL, true), model.getKeyComboForKey(back))
    assertEquals(KeyCombo.modifierKeys(0, true), model.getKeyComboForKey(nextItem))
    assertEquals(
      KeyCombo.modifierKeys(KeyEvent.META_ALT_ON, false).withPressType(KeyCombo.PRESSED_TWICE),
      model.getKeyComboForKey(playPause),
    )
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_Y, true), model.getKeyComboForKey(title))
    // The single-key keymap keeps Backspace alone for Back.
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_DEL, false),
      NewKeyComboModel(context, Keymap.KEYBOARD_PHONE_SINGLE_KEYS).getKeyComboForKey(back),
    )
  }

  @Test
  fun keyboardPhones() {
    assertTrue(KeyboardPhones.isKeyboardPhone("Unihertz", "Titan Pocket"))
    assertTrue(KeyboardPhones.isKeyboardPhone("Unihertz", "TITAN 2"))
    assertFalse(KeyboardPhones.isKeyboardPhone("Unihertz", "Jelly Star"))
    assertTrue(KeyboardPhones.isKeyboardPhone("BlackBerry", "BBF100-1"))
    assertFalse(KeyboardPhones.isKeyboardPhone("BlackBerry", "BBD100-1"))
    assertTrue(KeyboardPhones.isKeyboardPhone("Zinwa", "Q25"))
    assertTrue(KeyboardPhones.isKeyboardPhone("Xelex", "Q25"))
    assertFalse(KeyboardPhones.isKeyboardPhone("Xelex", "X10"))
    assertFalse(KeyboardPhones.isKeyboardPhone("Google", "Pixel 9"))
    // Keys typing doesn't need come first, so that Sym stays the keyboard's.
    assertEquals(SandFlyKey.RIGHT_SHIFT, KeyboardPhones.sandFlyKey("Zinwa", "Q25", hasSym = false))
    assertEquals(SandFlyKey.RIGHT_SHIFT, KeyboardPhones.sandFlyKey("Xelex", "Q25", hasSym = false))
    assertEquals(
      SandFlyKey.RIGHT_SHIFT,
      KeyboardPhones.sandFlyKey("Unihertz", "Titan 2 Elite", hasSym = true, fnIsCtrl = true),
    )
    assertEquals(
      SandFlyKey.CTRL,
      KeyboardPhones.sandFlyKey("Unihertz", "Titan 2", hasSym = true, fnIsCtrl = true),
    )
    assertEquals(SandFlyKey.SYM, KeyboardPhones.sandFlyKey("Unihertz", "Titan 2", hasSym = true))
    assertEquals(
      SandFlyKey.RIGHT_SHIFT,
      KeyboardPhones.sandFlyKey("BlackBerry", "BBF100-1", hasSym = true, hasRightShift = true),
    )
    assertEquals(SandFlyKey.SYM, KeyboardPhones.sandFlyKey("BlackBerry", "BBF100-1", hasSym = true))
    assertEquals(SandFlyKey.ALT, KeyboardPhones.sandFlyKey("Unihertz", "Titan", hasSym = false))
  }

  @Test
  fun keyboardPhoneDefaultsNeedNoCtrlOrMeta() {
    val model = NewKeyComboModel(context, Keymap.KEYBOARD_PHONE)
    for (key in model.keyComboMap.keys) {
      val combo = model.getDefaultKeyCombo(key)
      val modifiers = combo.modifiers
      assertEquals(key, 0, modifiers and (KeyEvent.META_CTRL_ON or KeyEvent.META_META_ON))
      val keyCode = combo.keyCode
      assertFalse(key, keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT)
      // No letter or Space on its own, which typing needs; Enter follows typing.
      if (
        !combo.isTriggerModifierUsed &&
          !combo.isModifierKeys &&
          !combo.isPressedTwice &&
          !combo.isWhileEditing
      ) {
        assertEquals(key, KeyEvent.KEYCODE_ENTER, combo.keyCode)
      }
    }
  }

  @Test
  fun keyboardPhoneKeymapsSpellThePasswordFieldWithSandFlyKeyY() {
    val key = Commands.shortcutKey(context.getString(R.string.shortcut_value_spell_password_field))
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      for (sandFlyKey in keymap.sandFlyKeys) {
        assertEquals(
          "$keymap with $sandFlyKey",
          KeyCombo(0, 0, KeyEvent.KEYCODE_Y, true),
          defaults(keymap, sandFlyKey)[key],
        )
      }
    }
    // Other keymaps have no default.
    assertEquals(null, defaults(Keymap.SANDFLY, Keymap.SANDFLY.nativeKey)[key])
  }

  /** The keyboard help key of each computer screen reader keymap, with its SandFly key. */
  private val helpKeys =
    mapOf(
      Keymap.VOICEOVER to KeyEvent.KEYCODE_K,
      Keymap.NVDA to KeyEvent.KEYCODE_1,
      Keymap.JAWS to KeyEvent.KEYCODE_1,
      Keymap.ORCA to KeyEvent.KEYCODE_H,
    )

  @Test
  fun keyboardHelpKeysTurnKeyboardPracticeOnOrOff() {
    val practice = context.getString(R.string.keycombo_shortcut_toggle_key_practice)
    val lesson = context.getString(R.string.keycombo_shortcut_global_show_learn_mode_page)
    for ((keymap, keyCode) in helpKeys) {
      SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
      val model = NewKeyComboModel(context, keymap)
      assertEquals(keymap.name, KeyCombo(0, 0, keyCode, true), model.getKeyComboForKey(practice))
      assertEquals(keymap.name, KeyCombo(), model.getKeyComboForKey(lesson))
    }
  }

  @Test
  fun version7SavedHelpKeysMoveToKeyboardPractice() {
    val practice = context.getString(R.string.keycombo_shortcut_toggle_key_practice)
    val lesson = context.getString(R.string.keycombo_shortcut_global_show_learn_mode_page)
    for ((keymap, keyCode) in helpKeys) {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      prefs.edit().clear().putInt(keymap.persisterPrefix + "_defaults_version", 7).commit()
      val persister = KeyComboPersister(context, keymap.persisterPrefix)
      persister.saveKeyCombo(lesson, KeyCombo(0, 0, keyCode, true))
      persister.saveKeyCombo(practice, KeyCombo())
      val model = NewKeyComboModel(context, keymap)
      assertEquals(keymap.name, KeyCombo(0, 0, keyCode, true), model.getKeyComboForKey(practice))
      assertEquals(keymap.name, KeyCombo(), model.getKeyComboForKey(lesson))
    }
  }

  @Test
  fun changedHelpKeyStaysWithPracticeKeyboardShortcuts() {
    val practice = context.getString(R.string.keycombo_shortcut_toggle_key_practice)
    val lesson = context.getString(R.string.keycombo_shortcut_global_show_learn_mode_page)
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(Keymap.NVDA.persisterPrefix + "_defaults_version", 7).commit()
    val persister = KeyComboPersister(context, Keymap.NVDA.persisterPrefix)
    val changed = KeyCombo(KeyEvent.META_SHIFT_ON, 0, KeyEvent.KEYCODE_1, true)
    persister.saveKeyCombo(lesson, changed)
    persister.saveKeyCombo(practice, KeyCombo())
    val model = NewKeyComboModel(context, Keymap.NVDA)
    assertEquals(changed, model.getKeyComboForKey(lesson))
    assertEquals(KeyCombo(0, 0, KeyEvent.KEYCODE_1, true), model.getKeyComboForKey(practice))
  }

  @Test
  fun nvdaKeymapStartsAndEndsSelectionModeWithInsertF9() {
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_selection)
    val f9 = KeyCombo(0, 0, KeyEvent.KEYCODE_F9, true)
    assertEquals(f9, NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key))
  }

  @Test
  fun version7SavedNvdaKeymapGetsInsertF9ForSelectionMode() {
    val key = context.getString(R.string.keycombo_shortcut_other_toggle_selection)
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putInt(Keymap.NVDA.persisterPrefix + "_defaults_version", 7).commit()
    KeyComboPersister(context, Keymap.NVDA.persisterPrefix).saveKeyCombo(key, KeyCombo())
    assertEquals(
      KeyCombo(0, 0, KeyEvent.KEYCODE_F9, true),
      NewKeyComboModel(context, Keymap.NVDA).getKeyComboForKey(key),
    )
  }
}
