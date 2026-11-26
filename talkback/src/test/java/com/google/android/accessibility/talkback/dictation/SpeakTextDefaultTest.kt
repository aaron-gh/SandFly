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

package com.google.android.accessibility.talkback.dictation

import android.content.Context
import android.content.SharedPreferences
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
class SpeakTextDefaultTest {
  private lateinit var prefs: SharedPreferences

  @Before
  fun setUp() {
    prefs =
      RuntimeEnvironment.getApplication()
        .getSharedPreferences("speak_text_default_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  @Test
  fun onWhenNeverSaved() {
    DictationSettings.migrateSpeakTextDefault(prefs)
    assertTrue(DictationSettings.speaksText(prefs))
  }

  @Test
  fun savedOldDefaultBecomesOn() {
    prefs.edit().putBoolean(DictationSettings.PREF_SPEAK_TEXT, false).commit()
    DictationSettings.migrateSpeakTextDefault(prefs)
    assertTrue(DictationSettings.speaksText(prefs))
  }

  @Test
  fun savedOnStaysOn() {
    prefs.edit().putBoolean(DictationSettings.PREF_SPEAK_TEXT, true).commit()
    DictationSettings.migrateSpeakTextDefault(prefs)
    assertTrue(DictationSettings.speaksText(prefs))
  }

  @Test
  fun offChosenAfterTheMigrationStaysOff() {
    DictationSettings.migrateSpeakTextDefault(prefs)
    prefs.edit().putBoolean(DictationSettings.PREF_SPEAK_TEXT, false).commit()
    DictationSettings.migrateSpeakTextDefault(prefs)
    assertFalse(DictationSettings.speaksText(prefs))
  }
}
