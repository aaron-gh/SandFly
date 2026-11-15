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
import com.google.android.accessibility.utils.output.VoiceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CodeSettingTitlesTest {
  private lateinit var context: Context
  private lateinit var screens: Map<String, Pair<String, List<String>>>

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    screens =
      mapOf(
        context.getString(R.string.pref_voice_profiles_screen_key) to
          ("Voice profiles" to listOf("Speech"))
      )
  }

  @Test
  fun voiceProfileInUseIsOnTheVoiceProfilesScreen() {
    assertEquals(
      context.getString(R.string.title_pref_voice_profile_in_use) to listOf("Speech", "Voice profiles"),
      CodeSettingTitles.of(context, VoiceProfiles.PREF_ACTIVE, screens),
    )
  }

  @Test
  fun voiceProfileSettingIsUnderTheProfilesName() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(VoiceProfiles.key("3", VoiceProfiles.NAME), "Reading")
      .commit()
    assertEquals(
      context.getString(R.string.title_pref_talkback_speech_rate) to
        listOf("Speech", "Voice profiles", "Reading"),
      CodeSettingTitles.of(context, VoiceProfiles.key("3", VoiceProfiles.RATE), screens),
    )
  }

  @Test
  fun otherSettingsHaveNoTitle() {
    assertNull(CodeSettingTitles.of(context, "pref_unknown", screens))
    assertNull(CodeSettingTitles.of(context, VoiceProfiles.key("3", "unknown"), screens))
  }
}
