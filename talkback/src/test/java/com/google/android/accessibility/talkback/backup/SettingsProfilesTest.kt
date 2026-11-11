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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsProfilesTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val braille = SharedPreferencesUtils.getSharedPreferences(context, "braille_keyboard")

  @Before
  fun setUp() {
    SettingsProfiles.resetStore()
  }

  @After
  fun tearDown() {
    SettingsProfiles.resetStore()
  }

  @Test
  fun aProfileHoldsSettingsButNotKeysOrState() {
    prefs
      .edit()
      .putBoolean("a_switch", true)
      .putString("pref_gemini_api_key", "secret")
      .putLong("update_last_check_time", 5)
      .putBoolean("has_training_exit", true)
      .putInt("talkback_enabled_count", 3)
      .putBoolean("pref_announce_keyboard_shown_hidden", true)
      .commit()
    braille.edit().putString("code", "EN_UEB").commit()
    context
      .createDeviceProtectedStorageContext()
      .getSharedPreferences("backtalk_scripts", Context.MODE_PRIVATE)
      .edit()
      .putString("installed", "script")
      .commit()

    val json = SettingsBackup.exportProfile(context)
    assertTrue(json.contains("a_switch"))
    assertTrue(json.contains("EN_UEB"))
    assertTrue(json.contains("pref_announce_keyboard_shown_hidden"))
    assertFalse(json.contains("secret"))
    assertFalse(json.contains("update_last_check_time"))
    assertFalse(json.contains("has_training_exit"))
    assertFalse(json.contains("talkback_enabled_count"))
    assertFalse(json.contains("backtalk_scripts"))
  }

  @Test
  fun applyingAProfileKeepsKeysStateAndOtherFiles() {
    prefs.edit().clear().putBoolean("a_switch", true).commit()
    val json = SettingsBackup.exportProfile(context)

    val scripts =
      context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences("backtalk_scripts", Context.MODE_PRIVATE)
    scripts.edit().putString("installed", "script").commit()
    prefs
      .edit()
      .putBoolean("a_switch", false)
      .putBoolean("added_later", true)
      .putString("pref_gemini_api_key", "mine")
      .putBoolean("has_training_exit", true)
      .commit()
    assertTrue(SettingsBackup.importProfile(context, json))

    assertTrue(prefs.getBoolean("a_switch", false))
    assertFalse(prefs.contains("added_later"))
    assertEquals("mine", prefs.getString("pref_gemini_api_key", null))
    assertTrue(prefs.getBoolean("has_training_exit", false))
    assertEquals("script", scripts.getString("installed", null))
  }

  @Test
  fun aProfileSavedBeforeABrailleSettingClearsIt() {
    braille.edit().clear().commit()
    prefs.edit().clear().putBoolean("a_switch", true).commit()
    val json = SettingsBackup.exportProfile(context)
    braille.edit().putString("code", "EN_UEB").commit()

    assertTrue(SettingsBackup.importProfile(context, json))
    assertFalse(braille.contains("code"))
  }

  @Test
  fun theFirstUseMakesDefaultFromTheSettings() {
    prefs.edit().clear().putInt("rate", 3).commit()
    val store = SettingsProfiles.ready(context)
    val active = store.active()
    assertNotNull(active)
    assertEquals(context.getString(R.string.settings_profile_default), active!!.name)
    assertTrue(store.settings(active.id)!!.contains("rate"))
  }

  @Test
  fun switchingSavesTheProfileInUseThenAppliesTheOther() {
    prefs.edit().clear().putInt("rate", 1).commit()
    val default = SettingsProfiles.ready(context).active()!!
    val demo = SettingsProfiles.add(context, "Demo")!!
    // Changed in Default after Demo was made.
    prefs.edit().putInt("rate", 2).commit()

    assertEquals(
      context.getString(R.string.settings_profile_in_use_announcement, "Demo"),
      SettingsProfiles.switchNow(context, demo.id),
    )
    assertEquals(1, prefs.getInt("rate", 0))
    prefs.edit().putInt("rate", 5).commit()

    SettingsProfiles.switchNow(context, default.id)
    assertEquals(2, prefs.getInt("rate", 0))
    SettingsProfiles.switchNow(context, demo.id)
    assertEquals(5, prefs.getInt("rate", 0))
    assertEquals(demo, SettingsProfiles.store(context).active())
  }

  @Test
  fun aDeletedProfileIsNotApplied() {
    prefs.edit().clear().putInt("rate", 1).commit()
    SettingsProfiles.ready(context)
    assertEquals(
      context.getString(R.string.settings_profile_not_changed),
      SettingsProfiles.switchNow(context, "00000000-0000-0000-0000-000000000000"),
    )
    assertEquals(1, prefs.getInt("rate", 0))
  }
}
