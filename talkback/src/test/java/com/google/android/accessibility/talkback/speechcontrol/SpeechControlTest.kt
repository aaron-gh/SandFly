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

package com.google.android.accessibility.talkback.speechcontrol

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeechControlTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences
  private val given = mutableListOf<Feedback>()
  private val feedback = Pipeline.FeedbackReturner { given.add(it) }
  private var stops = 0

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().clear().apply()
    SpeechControl.attach(context, feedback) { stops++ }
  }

  @After
  fun tearDown() {
    SpeechControl.detach()
  }

  @Test
  fun speak_saysNothingWhileTheSettingIsOff() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun speak_asksAboutAnAppTheUserHasNotAnswered() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
    assertEquals(SpeechControlApps.State.UNKNOWN, SpeechControlApps.state(prefs, APP))
  }

  @Test
  fun speak_saysNothingForAnAppTheUserBlocked() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, false)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun speak_saysNothingWhileTheDeviceIsLocked() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, true)
    shadowOf(context.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun stop_stopsOnlySpeechFromOtherApps() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, true)
    assertTrue(SpeechControl.stop(listOf(APP)))
    assertEquals(1, stops)
  }

  @Test
  fun stop_doesNothingForAnAppTheUserBlocked() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, false)
    assertFalse(SpeechControl.stop(listOf(APP)))
    assertEquals(0, stops)
  }

  @Test
  fun setAllowed_movesAnAppBetweenAllowedAndBlocked() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    assertEquals(SpeechControlApps.State.ALLOWED, SpeechControlApps.state(prefs, APP))
    SpeechControlApps.setAllowed(prefs, APP, false)
    assertEquals(SpeechControlApps.State.DENIED, SpeechControlApps.state(prefs, APP))
    assertEquals(setOf(APP), SpeechControlApps.answered(prefs))
  }

  private companion object {
    const val APP = "com.example.speaker"
  }
}
