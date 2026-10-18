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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.utils.VerbosityPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WindowNamesSettingTest {
  private val prefs = FakeSharedPreferences()
  private lateinit var res: Resources
  private lateinit var custom: String

  @Before
  fun setUp() {
    res = RuntimeEnvironment.getApplication().resources
    custom = res.getString(R.string.pref_verbosity_preset_value_custom)
  }

  private fun setPreset(preset: String) {
    prefs.edit().putString(res.getString(R.string.pref_verbosity_preset_key), preset).apply()
  }

  private fun setCustomSystemWindowNames(speak: Boolean) {
    prefs
      .edit()
      .putBoolean(
        VerbosityPreferences.toVerbosityPrefKey(
          custom,
          res.getString(R.string.pref_speak_system_window_titles_key),
        ),
        speak,
      )
      .apply()
  }

  private fun setFirstItemInterrupts(interrupts: Boolean) {
    prefs
      .edit()
      .putBoolean(res.getString(R.string.pref_first_item_interrupts_window_name_key), interrupts)
      .apply()
  }

  @Test
  fun defaultIsAlways() {
    assertEquals(WindowNamesSetting.ALWAYS, WindowNamesSetting.read(prefs, res))
    WindowNamesSetting.migrate(prefs, res)
    assertEquals(WindowNamesSetting.ALWAYS, WindowNamesSetting.read(prefs, res))
  }

  @Test
  fun unknownValueReadsAsTheDefault() {
    prefs.edit().putString(res.getString(R.string.pref_window_names_key), "loud").apply()
    assertEquals(WindowNamesSetting.ALWAYS, WindowNamesSetting.read(prefs, res))
  }

  @Test
  fun migrateTurnsSystemWindowNamesOffIntoWindowOpensOnly() {
    setPreset(custom)
    setCustomSystemWindowNames(false)
    WindowNamesSetting.migrate(prefs, res)
    assertEquals(WindowNamesSetting.WINDOW_OPENS, WindowNamesSetting.read(prefs, res))
  }

  @Test
  fun migrateTakesTheLowPresetAsWindowOpensOnly() {
    setPreset(res.getString(R.string.pref_verbosity_preset_value_low))
    WindowNamesSetting.migrate(prefs, res)
    assertEquals(WindowNamesSetting.WINDOW_OPENS, WindowNamesSetting.read(prefs, res))
  }

  @Test
  fun migrateKeepsTheFirstItemCuttingOffTheName() {
    setPreset(custom)
    setCustomSystemWindowNames(false)
    setFirstItemInterrupts(true)
    WindowNamesSetting.migrate(prefs, res)
    assertEquals(WindowNamesSetting.UNTIL_FIRST_ITEM, WindowNamesSetting.read(prefs, res))
  }

  @Test
  fun migrateKeepsAChoiceAlreadyMade() {
    prefs
      .edit()
      .putString(res.getString(R.string.pref_window_names_key), WindowNamesSetting.NEVER)
      .apply()
    setFirstItemInterrupts(true)
    WindowNamesSetting.migrate(prefs, res)
    assertEquals(WindowNamesSetting.NEVER, WindowNamesSetting.read(prefs, res))
  }
}
