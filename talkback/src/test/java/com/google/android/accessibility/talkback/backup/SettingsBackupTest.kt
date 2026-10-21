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
      .putLong(Updater.PREF_LAST_CHECK_TIME, 5)
      .commit()
    val json = SettingsBackup.export(context)
    assertFalse(json.contains("secret"))
    assertFalse(json.contains(Updater.PREF_LAST_CHECK_TIME))

    prefs.edit().putString("pref_gemini_api_key", "mine").commit()
    assertTrue(SettingsBackup.import(context, json))
    assertEquals("mine", prefs.getString("pref_gemini_api_key", null))
    assertEquals(5, prefs.getLong(Updater.PREF_LAST_CHECK_TIME, 0))
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
}
