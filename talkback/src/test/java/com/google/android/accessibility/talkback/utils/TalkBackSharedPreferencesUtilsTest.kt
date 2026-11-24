package com.google.android.accessibility.talkback.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TalkBackSharedPreferencesUtilsTest {
  @Test
  fun keyComboPreferencesAreRecognised() {
    val code = TalkBackSharedPreferencesUtils.getKeyForKeyComboCode("keyboard_phone", "next")
    val modifier =
      TalkBackSharedPreferencesUtils.getKeyForTriggerModifierUsed("keyboard_phone", "next", true)
    val oldModifier =
      TalkBackSharedPreferencesUtils.getKeyForTriggerModifierUsed("keyboard_phone", "next", false)
    assertTrue(TalkBackSharedPreferencesUtils.isKeyComboPreference(code))
    assertTrue(TalkBackSharedPreferencesUtils.isKeyComboPreference(modifier))
    assertTrue(TalkBackSharedPreferencesUtils.isKeyComboPreference(oldModifier))
  }

  @Test
  fun otherPreferencesAreNot() {
    assertFalse(TalkBackSharedPreferencesUtils.isKeyComboPreference("pref_shortcut_down_and_right"))
    assertFalse(TalkBackSharedPreferencesUtils.isKeyComboPreference("pref_select_keymap"))
    assertFalse(TalkBackSharedPreferencesUtils.isKeyComboPreference(null))
  }
}
