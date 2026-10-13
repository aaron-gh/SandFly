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

package com.google.android.accessibility.talkback.eventprocessor

import android.content.res.Resources
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.utils.VerbosityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneticLetterModeTest {
  private val prefs = FakeSharedPreferences()
  private lateinit var res: Resources
  private lateinit var oldKey: String
  private lateinit var modeKey: String
  private lateinit var custom: String

  @Before
  fun setUp() {
    res = RuntimeEnvironment.getApplication().resources
    oldKey = res.getString(R.string.pref_phonetic_letters_key)
    modeKey = res.getString(R.string.pref_phonetic_letters_mode_key)
    custom = res.getString(R.string.pref_verbosity_preset_value_custom)
  }

  private fun setPreset(preset: String) {
    prefs.edit().putString(res.getString(R.string.pref_verbosity_preset_key), preset).apply()
  }

  @Test
  fun defaultIsWithTheLetter() {
    setPreset(custom)
    assertEquals(PhoneticLetterMode.AFTER_LETTER_DELAY, PhoneticLetterMode.read(prefs, res))
  }

  @Test
  fun highAndLowPresetsHaveFixedModes() {
    setPreset(res.getString(R.string.pref_verbosity_preset_value_high))
    assertEquals(PhoneticLetterMode.AFTER_LETTER_DELAY, PhoneticLetterMode.read(prefs, res))
    setPreset(res.getString(R.string.pref_verbosity_preset_value_low))
    assertEquals(PhoneticLetterMode.OFF, PhoneticLetterMode.read(prefs, res))
  }

  @Test
  fun customPresetReadsItsOwnMode() {
    setPreset(custom)
    prefs
      .edit()
      .putString(
        VerbosityPreferences.toVerbosityPrefKey(custom, modeKey),
        PhoneticLetterMode.INSTEAD_OF_LETTER,
      )
      .apply()
    assertEquals(PhoneticLetterMode.INSTEAD_OF_LETTER, PhoneticLetterMode.read(prefs, res))
  }

  @Test
  fun migrateTurnsOnAndOffIntoModes() {
    prefs
      .edit()
      .putBoolean(VerbosityPreferences.toVerbosityPrefKey(custom, oldKey), false)
      .putBoolean(oldKey, true)
      .apply()
    PhoneticLetterMode.migrate(prefs, res)
    setPreset(custom)
    assertEquals(PhoneticLetterMode.OFF, PhoneticLetterMode.read(prefs, res))
    assertEquals(PhoneticLetterMode.AFTER_LETTER_DELAY, prefs.getString(modeKey, null))
  }

  @Test
  fun migrateKeepsAModeAlreadyChosen() {
    val customOld = VerbosityPreferences.toVerbosityPrefKey(custom, oldKey)
    val customMode = VerbosityPreferences.toVerbosityPrefKey(custom, modeKey)
    prefs
      .edit()
      .putBoolean(customOld, false)
      .putString(customMode, PhoneticLetterMode.INSTEAD_OF_LETTER)
      .apply()
    PhoneticLetterMode.migrate(prefs, res)
    assertEquals(PhoneticLetterMode.INSTEAD_OF_LETTER, prefs.getString(customMode, null))
  }

  @Test
  fun migrateLeavesNewUsersAlone() {
    PhoneticLetterMode.migrate(prefs, res)
    assertFalse(prefs.contains(modeKey))
    assertFalse(prefs.contains(VerbosityPreferences.toVerbosityPrefKey(custom, modeKey)))
  }
}
