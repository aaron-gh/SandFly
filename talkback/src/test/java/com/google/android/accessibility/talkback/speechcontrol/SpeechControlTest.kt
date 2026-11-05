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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.provider.Settings
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
    KeyboardSpeech.clear()
  }

  /** Installs [APP], declaring the permission to speak while the device is locked if [declares]. */
  private fun installApp(declares: Boolean) {
    val info =
      PackageInfo().apply {
        packageName = APP
        applicationInfo = ApplicationInfo().apply { packageName = APP }
        requestedPermissions =
          if (declares) arrayOf(SpeechControl.permissionWhenLocked(context)) else arrayOf()
      }
    shadowOf(context.packageManager).installPackage(info)
  }

  private fun allowAndLock() {
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, true)
    shadowOf(context.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
  }

  private fun useKeyboard(packageName: String) {
    Settings.Secure.putString(
      context.contentResolver,
      Settings.Secure.DEFAULT_INPUT_METHOD,
      "$packageName/.KeyboardService",
    )
  }

  @Test
  fun speak_saysTheCurrentKeyboardsSpeechWithoutBeingAllowed() {
    useKeyboard(APP)
    assertTrue(SpeechControl.speak(listOf(APP), "Hello", interrupt = true))
    assertEquals(1, given.size)
  }

  @Test
  fun speak_saysTheCurrentKeyboardsSpeechWhileTheDeviceIsLocked() {
    useKeyboard(APP)
    shadowOf(context.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(true)
    assertTrue(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(1, given.size)
  }

  @Test
  fun speak_doesNotRateLimitTheCurrentKeyboard() {
    useKeyboard(APP)
    repeat(20) { assertTrue(SpeechControl.speak(listOf(APP), "a", interrupt = true)) }
    assertEquals(20, given.size)
  }

  @Test
  fun speak_forgetsTheAnswerForTheCurrentKeyboard() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    useKeyboard(APP)
    SpeechControl.speak(listOf(APP), "Hello", interrupt = true)
    assertEquals(SpeechControlApps.State.UNKNOWN, SpeechControlApps.state(prefs, APP))
    assertTrue(SpeechControlApps.answered(prefs).isEmpty())
  }

  @Test
  fun setTypingSpoken_forgetsTheAnswerForTheCurrentKeyboard() {
    SpeechControlApps.setAllowed(prefs, APP, false)
    useKeyboard(APP)
    SpeechControl.setTypingSpoken(context, listOf(APP), true)
    assertTrue(SpeechControlApps.answered(prefs).isEmpty())
  }

  @Test
  fun setTypingSpoken_keepsTheAnswerForAnAppThatIsNotTheCurrentKeyboard() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    useKeyboard("com.example.keyboard")
    SpeechControl.setTypingSpoken(context, listOf(APP), true)
    assertEquals(SpeechControlApps.State.ALLOWED, SpeechControlApps.state(prefs, APP))
  }

  @Test
  fun speak_treatsAnAppThatIsNotTheCurrentKeyboardAsAnyOther() {
    useKeyboard("com.example.keyboard")
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun setTypingSpoken_suppressesEchoOnlyForTheCurrentKeyboard() {
    useKeyboard(APP)
    assertTrue(SpeechControl.setTypingSpoken(context, listOf(APP), true))
    assertTrue(KeyboardSpeech.isTypingSpoken(context))

    useKeyboard("com.example.keyboard")
    assertFalse(KeyboardSpeech.isTypingSpoken(context))
  }

  @Test
  fun setTypingSpoken_isRecordedBeforeSandFlyStarts() {
    SpeechControl.detach()
    useKeyboard(APP)
    assertTrue(SpeechControl.setTypingSpoken(context, listOf(APP), true))
    assertTrue(KeyboardSpeech.isTypingSpoken(context))
  }

  @Test
  fun setTypingSpoken_isIgnoredFromAnAppThatIsNotTheCurrentKeyboard() {
    useKeyboard("com.example.keyboard")
    assertFalse(SpeechControl.setTypingSpoken(context, listOf(APP), true))
    assertFalse(KeyboardSpeech.isTypingSpoken(context))
  }

  @Test
  fun setTypingSpoken_canBeTurnedOff() {
    useKeyboard(APP)
    SpeechControl.setTypingSpoken(context, listOf(APP), true)
    SpeechControl.setTypingSpoken(context, listOf(APP), false)
    assertFalse(KeyboardSpeech.isTypingSpoken(context))
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
  fun speak_saysNothingWhileLockedForAnAppThatDoesNotDeclareIt() {
    installApp(declares = false)
    allowAndLock()
    SpeechControlApps.setAllowedWhileLocked(prefs, APP, true)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun speak_saysNothingWhileLockedUntilTheUserAllowsItWhileLocked() {
    installApp(declares = true)
    allowAndLock()
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
    assertEquals(SpeechControlApps.State.UNKNOWN, SpeechControlApps.lockedState(prefs, APP))
  }

  @Test
  fun speak_saysTextWhileLockedForAnAppAllowedWhileLocked() {
    installApp(declares = true)
    allowAndLock()
    SpeechControlApps.setAllowedWhileLocked(prefs, APP, true)
    assertTrue(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(1, given.size)
  }

  @Test
  fun speak_saysNothingWhileLockedForAnAppBlockedWhileLocked() {
    installApp(declares = true)
    allowAndLock()
    SpeechControlApps.setAllowedWhileLocked(prefs, APP, false)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun speak_whileLockedStillNeedsTheAppAllowed() {
    installApp(declares = true)
    allowAndLock()
    SpeechControlApps.setAllowed(prefs, APP, false)
    SpeechControlApps.setAllowedWhileLocked(prefs, APP, true)
    assertFalse(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(0, given.size)
  }

  @Test
  fun speak_unlockedNeedsNoAnswerForWhileLocked() {
    installApp(declares = true)
    prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, true).apply()
    SpeechControlApps.setAllowed(prefs, APP, true)
    assertTrue(SpeechControl.speak(listOf(APP), "Hello", interrupt = false))
    assertEquals(1, given.size)
  }

  @Test
  fun setAllowed_doesNotAllowAnAppWhileLocked() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    assertEquals(SpeechControlApps.State.UNKNOWN, SpeechControlApps.lockedState(prefs, APP))
  }

  @Test
  fun forget_forgetsTheAnswerForWhileLocked() {
    SpeechControlApps.setAllowed(prefs, APP, true)
    SpeechControlApps.setAllowedWhileLocked(prefs, APP, true)
    SpeechControlApps.forget(prefs, APP)
    assertEquals(SpeechControlApps.State.UNKNOWN, SpeechControlApps.lockedState(prefs, APP))
  }

  @Test
  fun declaresSpeakWhenLocked_readsTheAppsManifest() {
    installApp(declares = true)
    assertTrue(SpeechControl.declaresSpeakWhenLocked(context, APP))
    installApp(declares = false)
    assertFalse(SpeechControl.declaresSpeakWhenLocked(context, APP))
    assertFalse(SpeechControl.declaresSpeakWhenLocked(context, "com.example.missing"))
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
