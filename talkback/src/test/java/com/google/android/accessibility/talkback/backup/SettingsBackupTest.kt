/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import com.google.android.accessibility.talkback.update.Updater
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsBackupTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val braille = SharedPreferencesUtils.getSharedPreferences(context, "braille_keyboard")

  @Test
  fun settingsComeBackAsTheyWere() {
    prefs
      .edit()
      .putBoolean("a_switch", true)
      .putInt("an_int", 3)
      .putLong("a_long", 1L shl 40)
      .putFloat("a_float", 1.5f)
      .putString("a_string", "x")
      .putStringSet("a_set", setOf("1", "2"))
      .commit()
    braille.edit().putString("code", "EN_UEB").commit()
    val json = SettingsBackup.export(context)

    prefs.edit().clear().putBoolean("added_later", true).commit()
    braille.edit().clear().commit()
    assertTrue(SettingsBackup.import(context, json))

    assertEquals(true, prefs.getBoolean("a_switch", false))
    assertEquals(3, prefs.getInt("an_int", 0))
    assertEquals(1L shl 40, prefs.getLong("a_long", 0))
    assertEquals(1.5f, prefs.getFloat("a_float", 0f))
    assertEquals("x", prefs.getString("a_string", null))
    assertEquals(setOf("1", "2"), prefs.getStringSet("a_set", null))
    assertFalse(prefs.contains("added_later"))
    assertEquals("EN_UEB", braille.getString("code", null))
  }

  @Test
  fun apiKeysAndTheUpdaterStateStayOnThePhone() {
    prefs
      .edit()
      .putString("pref_gemini_api_key", "secret")
      .putString("pref_ai_claude_api_key", "secret")
      .putLong(Updater.PREF_LAST_CHECK_TIME, 5)
      .commit()
    val json = SettingsBackup.export(context)
    assertFalse(json.contains("secret"))
    assertFalse(json.contains(Updater.PREF_LAST_CHECK_TIME))

    prefs.edit().putString("pref_gemini_api_key", "mine").putString("pref_ai_claude_api_key", "mine").commit()
    assertTrue(SettingsBackup.import(context, json))
    assertEquals("mine", prefs.getString("pref_gemini_api_key", null))
    assertEquals("mine", prefs.getString("pref_ai_claude_api_key", null))
    assertEquals(5, prefs.getLong(Updater.PREF_LAST_CHECK_TIME, 0))
  }

  @Test
  fun valuesOfAnotherTypeKeepThePhonesOwn() {
    prefs.edit().clear().putString("a_number", "3").putBoolean("a_switch", true).commit()
    val json =
      """
      {"format": "sandfly-settings", "version": 1, "files": {"device": {"default": {
        "a_number": {"type": "string", "value": "fast"},
        "a_switch": {"type": "int", "value": 1},
        "pref_tts_overlay": {"type": "string", "value": "yes"},
        "a_new_one": {"type": "int", "value": 2}
      }}}}
      """
    assertTrue(SettingsBackup.import(context, json))
    assertEquals("3", prefs.getString("a_number", null))
    assertEquals(true, prefs.getBoolean("a_switch", false))
    assertFalse(prefs.contains("pref_tts_overlay"))
    assertEquals(2, prefs.getInt("a_new_one", 0))
  }

  @Test
  fun otherFilesAreRefusedAndChangeNothing() {
    prefs.edit().putBoolean("kept", true).commit()
    assertFalse(SettingsBackup.import(context, "{}"))
    assertFalse(SettingsBackup.import(context, "not json"))
    assertFalse(
      SettingsBackup.import(
        context,
        """{"format":"sandfly-settings","version":1,"files":{"device":{"default":{"k":{"type":"x"}}}}}""",
      )
    )
    assertTrue(prefs.getBoolean("kept", false))
  }

  @Test
  fun aFileNameCannotLeaveTheSettingsFolder() {
    assertTrue(
      SettingsBackup.import(
        context,
        """{"format":"sandfly-settings","version":1,"files":{"device":{"../x":{}}}}""",
      )
    )
  }

  @Test
  fun aBacktalkFileIsImported() {
    prefs
      .edit()
      .clear()
      .putBoolean("sandfly_only", true)
      .putString("pref_gemini_api_key", "k")
      .commit()
    braille.edit().clear().commit()
    val backtalk =
      """{"format":1,"stores":{""" +
        """"main":{"a_switch":{"t":"bool","v":true},"a_float":{"t":"float","v":1.5},""" +
        """"a_set":{"t":"set","v":["1","2"]},"pref_gemini_api_key":{"t":"string","v":"x"},""" +
        """"tutorial_shown":{"t":"bool","v":true}},""" +
        """"braille_keyboard":{"code":{"t":"string","v":"EN_UEB"}}}}"""

    assertTrue(SettingsBackup.import(context, backtalk))

    assertEquals(true, prefs.getBoolean("a_switch", false))
    assertEquals(1.5f, prefs.getFloat("a_float", 0f))
    assertEquals(setOf("1", "2"), prefs.getStringSet("a_set", null))
    assertEquals("EN_UEB", braille.getString("code", null))
    // SandFly's own settings and the key stay; Backtalk's state isn't brought over.
    assertTrue(prefs.getBoolean("sandfly_only", false))
    assertEquals("k", prefs.getString("pref_gemini_api_key", null))
    assertFalse(prefs.contains("tutorial_shown"))
  }

  @Test
  fun aDamagedBacktalkFileChangesNothing() {
    prefs.edit().clear().putBoolean("a_switch", false).commit()
    val damaged =
      """{"format":1,"stores":{"main":{"a_switch":{"t":"bool","v":true},"b":{"t":"what"}}}}"""

    assertFalse(SettingsBackup.import(context, damaged))
    assertFalse(prefs.getBoolean("a_switch", true))
  }
}
