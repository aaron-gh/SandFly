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
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
  private val rate = "default/rate"
  private val other = "default/other"
  private val app = "org.telegram.messenger"

  @Before
  fun setUp() {
    SettingsProfiles.reset()
    prefs.edit().clear().putInt("rate", 1).commit()
  }

  @After
  fun tearDown() {
    SettingsProfiles.reset()
  }

  private fun profile(
    name: String,
    settings: Map<String, Any?>,
    triggers: SettingsProfileStore.Triggers = SettingsProfileStore.Triggers(apps = setOf(app)),
  ): SettingsProfileStore.Profile {
    val store = SettingsProfiles.ready(context)
    val profile = store.add(name, settings)!!
    store.setTriggers(profile.id, triggers)
    return store.profile(profile.id)!!
  }

  private fun inApp(packageName: String) {
    SettingsProfiles.setContext(packageName)
    SettingsProfiles.applyNow(context)
  }

  @Test
  fun profilesHoldSettingsButNotKeysOrState() {
    prefs
      .edit()
      .putBoolean("a_switch", true)
      .putString("pref_gemini_api_key", "secret")
      .putLong("update_last_check_time", 5)
      .putBoolean("has_training_exit", true)
      .putInt("talkback_enabled_count", 3)
      .commit()
    braille.edit().putString("code", "EN_UEB").commit()
    context
      .createDeviceProtectedStorageContext()
      .getSharedPreferences("backtalk_scripts", Context.MODE_PRIVATE)
      .edit()
      .putString("installed", "script")
      .commit()

    val values = SettingsBackup.profileValues(context)
    assertEquals(true, values["default/a_switch"])
    assertEquals("EN_UEB", values["braille_keyboard/code"])
    assertFalse(values.keys.any { it.contains("api_key") })
    assertFalse("default/update_last_check_time" in values)
    assertFalse("default/has_training_exit" in values)
    assertFalse("default/talkback_enabled_count" in values)
    assertFalse(values.keys.any { it.startsWith("backtalk_scripts/") })
    assertFalse(SettingsBackup.isProfileKey(context, "default/pref_gemini_api_key"))
    assertFalse(SettingsBackup.isProfileKey(context, "backtalk_scripts/installed"))
  }

  @Test
  fun aProfileSetsItsSettingsInItsAppsOnly() {
    profile("Messaging", mapOf(rate to 80, other to true))

    inApp(app)
    assertEquals(80, prefs.getInt("rate", 0))
    assertTrue(prefs.getBoolean("other", false))

    inApp("com.android.chrome")
    assertEquals(1, prefs.getInt("rate", 0))
    assertFalse(prefs.contains("other"))
    assertTrue(SettingsProfiles.inUse().isEmpty())
  }

  @Test
  fun aProfileCanUnsetASetting() {
    prefs.edit().putBoolean("other", true).commit()
    profile("Messaging", mapOf(other to null))
    inApp(app)
    assertFalse(prefs.contains("other"))
    inApp("")
    assertTrue(prefs.getBoolean("other", false))
  }

  @Test
  fun aSettingChangedWhileAProfileSetsItGoesToTheProfile() {
    val messaging = profile("Messaging", mapOf(rate to 80))
    inApp(app)
    prefs.edit().putInt("rate", 90).commit()

    inApp("")
    assertEquals(1, prefs.getInt("rate", 0))
    assertEquals(mapOf(rate to 90), SettingsProfiles.store(context).settings(messaging.id))
    inApp(app)
    assertEquals(90, prefs.getInt("rate", 0))
  }

  @Test
  fun otherSettingsChangedWhileAProfileIsInUseApplyEverywhere() {
    val messaging = profile("Messaging", mapOf(rate to 80))
    inApp(app)
    prefs.edit().putBoolean("other", true).commit()

    inApp("")
    assertTrue(prefs.getBoolean("other", false))
    assertEquals(mapOf(rate to 80), SettingsProfiles.store(context).settings(messaging.id))
  }

  @Test
  fun theChosenProfileWinsThenTheOneHigherInTheList() {
    profile("Messaging", mapOf(rate to 10))
    val web = profile("Web", mapOf(rate to 20), SettingsProfileStore.Triggers(apps = setOf(app), web = true))

    inApp(app)
    assertEquals(10, prefs.getInt("rate", 0))

    assertEquals(
      context.getString(R.string.settings_profile_in_use_announcement, "Web"),
      SettingsProfiles.setManualNow(context, web.id),
    )
    assertEquals(20, prefs.getInt("rate", 0))

    assertEquals(
      context.getString(R.string.settings_profile_none),
      SettingsProfiles.setManualNow(context, null),
    )
    assertEquals(10, prefs.getInt("rate", 0))
  }

  @Test
  fun contextsUseTheirProfiles() {
    profile("Web", mapOf(rate to 20), SettingsProfileStore.Triggers(web = true))
    profile("Reading", mapOf(rate to 30), SettingsProfileStore.Triggers(reading = true))
    profile("Editing", mapOf(rate to 40), SettingsProfileStore.Triggers(editing = true))

    SettingsProfiles.setContext(app, web = true)
    SettingsProfiles.applyNow(context)
    assertEquals(20, prefs.getInt("rate", 0))
    SettingsProfiles.setContext(app, reading = true)
    SettingsProfiles.applyNow(context)
    assertEquals(30, prefs.getInt("rate", 0))
    SettingsProfiles.setContext(app, editing = true)
    SettingsProfiles.applyNow(context)
    assertEquals(40, prefs.getInt("rate", 0))
  }

  @Test
  fun profilesUsedAutomaticallyAreNotUsedInSandFly() {
    profile("Web", mapOf(rate to 20), SettingsProfileStore.Triggers(web = true))
    SettingsProfiles.setContext(context.packageName, web = true)
    SettingsProfiles.applyNow(context)
    assertEquals(1, prefs.getInt("rate", 0))
  }

  @Test
  fun changingAProfilesSettingsSavesEveryChangeToIt() {
    val messaging = profile("Messaging", mapOf(rate to 80))
    SettingsProfiles.setContext(context.packageName)

    assertEquals(
      context.getString(R.string.settings_profile_editing, "Messaging"),
      SettingsProfiles.startEditingNow(context, messaging.id),
    )
    assertEquals(80, prefs.getInt("rate", 0))
    prefs.edit().putInt("rate", 70).putBoolean("other", true).commit()

    assertEquals(
      context.getString(R.string.settings_profile_edit_saved, "Messaging"),
      SettingsProfiles.finishEditingNow(context),
    )
    assertEquals(
      mapOf(rate to 70, other to true),
      SettingsProfiles.store(context).settings(messaging.id),
    )
    // Without the profile, as before.
    assertEquals(1, prefs.getInt("rate", 0))
    assertFalse(prefs.contains("other"))
    assertNull(SettingsProfiles.finishEditingNow(context))

    inApp(app)
    assertEquals(70, prefs.getInt("rate", 0))
    assertTrue(prefs.getBoolean("other", false))
  }

  @Test
  fun removingASettingRestoresIt() {
    val messaging = profile("Messaging", mapOf(rate to 80, other to true))
    inApp(app)
    assertTrue(SettingsProfiles.removeSetting(context, messaging.id, rate))
    assertEquals(1, prefs.getInt("rate", 0))
    assertTrue(prefs.getBoolean("other", false))
  }

  @Test
  fun deletingAProfileInUseRestoresItsSettings() {
    val messaging = profile("Messaging", mapOf(rate to 80))
    inApp(app)
    assertTrue(SettingsProfiles.delete(context, messaging.id))
    assertEquals(1, prefs.getInt("rate", 0))
    assertTrue(SettingsProfiles.store(context).applied().isEmpty())
  }

  @Test
  fun exportedSettingsAreTheSettingsWithoutProfiles() {
    profile("Messaging", mapOf(rate to 80, other to true))
    inApp(app)

    val device = JSONObject(SettingsBackup.export(context)).getJSONObject("files").getJSONObject("device")
    val main = device.getJSONObject("default")
    assertEquals(1, main.getJSONObject("rate").getInt("value"))
    assertFalse(main.has("other"))
  }

  @Test
  fun aValueOfAnotherTypeIsNotApplied() {
    profile("Messaging", mapOf(rate to "fast"))
    inApp(app)
    assertEquals(1, prefs.getInt("rate", 0))
  }

  @Test
  fun profilesOfVersion1BecomeTheirDifferences() {
    prefs.edit().putBoolean("other", true).commit()
    val directory = File(context.createDeviceProtectedStorageContext().filesDir, "settings_profiles")
    directory.mkdirs()
    File(directory, "profiles.json")
      .writeText(
        """{"version": 1, "profiles": [{"id": "abc-1", "name": "Default"},
           {"id": "abc-2", "name": "Demo"}], "active": "abc-1"}"""
      )
    File(directory, "abc-1.json").writeText(SettingsBackup.export(context))
    File(directory, "abc-2.json")
      .writeText(
        """{"format": "sandfly-settings", "version": 1, "files": {"device": {"default":
           {"rate": {"type": "int", "value": 2}}}}}"""
      )

    val store = SettingsProfiles.ready(context)
    assertEquals(listOf("Demo"), store.profiles().map { it.name })
    assertEquals(mapOf(rate to 2, other to null), store.settings("abc-2"))
    assertNull(store.manual())
    assertEquals(1, prefs.getInt("rate", 0))
  }
}
