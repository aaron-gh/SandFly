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

package com.google.android.accessibility.talkback.speech

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.accessibility.utils.output.LanguageVoices
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
class LanguageSwitchCommandsTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  @Test
  fun switchLanguagesCommandTurnsTheSettingTheEngineReadsOffAndOn() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val key = context.getString(R.string.pref_switch_languages_key)
    assertEquals(FailoverTextToSpeech.PREF_SWITCH_LANGUAGES_KEY, key)
    prefs.edit().remove(key).commit()
    // On by default, so the first use turns it off.
    assertFalse(LanguageSwitchCommands.toggleSwitchLanguages(context, prefs))
    assertFalse(prefs.getBoolean(key, true))
    assertTrue(LanguageSwitchCommands.toggleSwitchLanguages(context, prefs))
    assertTrue(prefs.getBoolean(key, false))
  }

  @Test
  fun identifyFromWordsCommandTurnsTheSettingOnAndOff() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().remove(LanguageVoices.PREF_IDENTIFY_FROM_WORDS).commit()
    // Off by default, so the first use turns it on.
    assertTrue(LanguageSwitchCommands.toggleIdentifyFromWords(prefs))
    assertTrue(prefs.getBoolean(LanguageVoices.PREF_IDENTIFY_FROM_WORDS, false))
    assertFalse(LanguageSwitchCommands.toggleIdentifyFromWords(prefs))
    assertFalse(prefs.getBoolean(LanguageVoices.PREF_IDENTIFY_FROM_WORDS, true))
  }
}
