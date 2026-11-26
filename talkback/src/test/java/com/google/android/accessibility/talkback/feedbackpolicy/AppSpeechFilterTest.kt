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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.media.AudioAttributes
import android.media.AudioManager
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter.MODE_ALL_APPS
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter.MODE_CHOSEN_APPS
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter.MODE_OFF
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter.Source
import com.google.android.accessibility.talkback.feedbackpolicy.AppSpeechFilter.State
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSpeechFilterTest {
  private val gemini = "com.google.android.apps.bard"
  private val other = "com.example.other"
  private val own = "com.example.sandfly"
  private val corvus = "sk.stopka.corvus"

  @Test
  fun defaults_muteNothing() {
    val state = State(ownPackage = own)
    assertFalse(state.active)
    for (source in Source.values()) {
      assertFalse(AppSpeechFilter.mutes(state, source, gemini))
      assertFalse(AppSpeechFilter.mutes(state, source, null))
    }
    // Chosen apps without any apps does nothing.
    assertFalse(State(mode = MODE_CHOSEN_APPS, ownPackage = own).active)
  }

  @Test
  fun ignoredPackages_alwaysMuteAppSpeechOnly() {
    val viewtalk = "com.oplus.viewtalk"
    assertTrue(viewtalk in AppSpeechFilter.IGNORED_PACKAGES)
    assertTrue(AppSpeechFilter.alwaysMutes(Source.APP_SPEECH, viewtalk))
    assertFalse(AppSpeechFilter.alwaysMutes(Source.APP_FOCUS, viewtalk))
    assertFalse(AppSpeechFilter.alwaysMutes(Source.USER_FOCUS, viewtalk))
    assertFalse(AppSpeechFilter.alwaysMutes(Source.TEXT_EDIT, viewtalk))
    assertFalse(AppSpeechFilter.alwaysMutes(Source.APP_SPEECH, other))
    assertFalse(AppSpeechFilter.alwaysMutes(Source.APP_SPEECH, null))
  }

  @Test
  fun isIgnoredPackage_matchesIgnoredPackagesOnly() {
    assertTrue(AppSpeechFilter.isIgnoredPackage("com.oplus.viewtalk"))
    assertTrue(AppSpeechFilter.isIgnoredPackage(StringBuilder("com.oplus.viewtalk")))
    assertFalse(AppSpeechFilter.isIgnoredPackage("com.oplus.viewtalk.other"))
    assertFalse(AppSpeechFilter.isIgnoredPackage(other))
    assertFalse(AppSpeechFilter.isIgnoredPackage(null))
  }

  @Test
  fun inAllApps_mutesAppFocusAndAppSpeech_notUserFocusOrTyping() {
    val state = State(mode = MODE_ALL_APPS, ownPackage = own)
    assertTrue(state.active)
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_FOCUS, gemini))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, other))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, null))
    assertFalse(AppSpeechFilter.mutes(state, Source.USER_FOCUS, gemini))
    assertFalse(AppSpeechFilter.mutes(state, Source.TEXT_EDIT, gemini))
  }

  @Test
  fun ownScreens_areAlwaysSpoken() {
    val state = State(mode = MODE_ALL_APPS, assistantPlaying = true, ownPackage = own)
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_FOCUS, own))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_SPEECH, own))
  }

  @Test
  fun inChosenApps_mutesOnlyTheChosenApps() {
    val state = State(mode = MODE_CHOSEN_APPS, apps = setOf(gemini), ownPackage = own)
    assertTrue(state.active)
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_FOCUS, gemini))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, gemini))
    assertFalse(AppSpeechFilter.mutes(state, Source.USER_FOCUS, gemini))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_FOCUS, other))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_SPEECH, null))
  }

  @Test
  fun assistantAudio_mutesAppFocusInAllApps_notUserFocus() {
    val state = State(mode = MODE_OFF, assistantPlaying = true, ownPackage = own)
    assertTrue(state.active)
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_FOCUS, other))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, null))
    assertFalse(AppSpeechFilter.mutes(state, Source.USER_FOCUS, other))
    assertFalse(AppSpeechFilter.mutes(state, Source.TEXT_EDIT, other))
  }

  @Test
  fun assistantShown_mutesAppFocusInAllApps_notUserFocusOrOwnScreens() {
    val state =
      State(mode = MODE_CHOSEN_APPS, apps = setOf(other), assistantShown = true, ownPackage = own)
    assertTrue(state.active)
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_FOCUS, gemini))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, "com.android.systemui"))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_SPEECH, null))
    assertFalse(AppSpeechFilter.mutes(state, Source.USER_FOCUS, gemini))
    assertFalse(AppSpeechFilter.mutes(state, Source.TEXT_EDIT, gemini))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_FOCUS, own))
    assertTrue(State(assistantShown = true, ownPackage = own).active)
  }

  @Test
  fun voiceInteractionWindows_haveNoAccessibilityWindowType() {
    assertTrue(AppSpeechFilter.isVoiceInteractionType(-1))
    assertFalse(AppSpeechFilter.isVoiceInteractionType(AccessibilityWindowInfo.TYPE_APPLICATION))
    // App overlays, such as chat heads and screen filters, are system windows.
    assertFalse(AppSpeechFilter.isVoiceInteractionType(AccessibilityWindowInfo.TYPE_SYSTEM))
    assertFalse(
      AppSpeechFilter.isVoiceInteractionType(AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY)
    )
    assertFalse(AppSpeechFilter.isVoiceInteractionType(AccessibilityWindowInfo.TYPE_INPUT_METHOD))
  }

  @Test
  fun assistantWindowPackage_isNotSystemUiOrOwn() {
    assertTrue(AppSpeechFilter.isAssistantWindowPackage("com.google.android.googlequicksearchbox", own))
    assertTrue(AppSpeechFilter.isAssistantWindowPackage(null, own))
    assertFalse(AppSpeechFilter.isAssistantWindowPackage("com.android.systemui", own))
    assertFalse(AppSpeechFilter.isAssistantWindowPackage(own, own))
  }

  @Test
  fun keyboardWithOwnSpeech_mutesItsKeysAndTextEditing() {
    val state = State(keyboardPackage = corvus, ownPackage = own)
    assertTrue(state.active)
    assertTrue(AppSpeechFilter.mutes(state, Source.USER_FOCUS, corvus))
    assertTrue(AppSpeechFilter.mutes(state, Source.APP_FOCUS, corvus))
    assertTrue(AppSpeechFilter.mutes(state, Source.TEXT_EDIT, other))
    // Items outside the keyboard are spoken as usual.
    assertFalse(AppSpeechFilter.mutes(state, Source.USER_FOCUS, other))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_FOCUS, other))
    assertFalse(AppSpeechFilter.mutes(state, Source.APP_SPEECH, other))
  }

  @Test
  fun isMovedByUser_countsTouchNavigationAndKeys() {
    assertTrue(AppSpeechFilter.isMovedByUser(FocusActionInfo.TOUCH_EXPLORATION))
    assertTrue(AppSpeechFilter.isMovedByUser(FocusActionInfo.LOGICAL_NAVIGATION))
    assertTrue(AppSpeechFilter.isMovedByUser(FocusActionInfo.KEYBOARD_SHORTCUT_REFOCUS))
    assertFalse(AppSpeechFilter.isMovedByUser(FocusActionInfo.UNKNOWN))
    assertFalse(AppSpeechFilter.isMovedByUser(FocusActionInfo.MANUAL_SCROLL))
    assertFalse(AppSpeechFilter.isMovedByUser(FocusActionInfo.FOCUS_SYNCHRONIZATION))
    assertFalse(AppSpeechFilter.isMovedByUser(FocusActionInfo.SCREEN_STATE_CHANGE))
    assertFalse(AppSpeechFilter.isMovedByUser(FocusActionInfo.ENSURE_ON_SCREEN))
  }

  @Test
  fun step_goesThroughTheChoices() {
    assertEquals(MODE_ALL_APPS, AppSpeechFilter.step(MODE_OFF, true))
    assertEquals(MODE_CHOSEN_APPS, AppSpeechFilter.step(MODE_ALL_APPS, true))
    assertEquals(MODE_OFF, AppSpeechFilter.step(MODE_CHOSEN_APPS, true))
    assertEquals(MODE_CHOSEN_APPS, AppSpeechFilter.step(MODE_OFF, false))
    assertEquals(MODE_ALL_APPS, AppSpeechFilter.step("unknown", true))
  }

  @Test
  fun toggle_turnsOffAndBackOn() {
    assertEquals(MODE_OFF, AppSpeechFilter.toggle(MODE_ALL_APPS, hasApps = true))
    assertEquals(MODE_OFF, AppSpeechFilter.toggle(MODE_CHOSEN_APPS, hasApps = false))
    assertEquals(MODE_CHOSEN_APPS, AppSpeechFilter.toggle(MODE_OFF, hasApps = true))
    assertEquals(MODE_ALL_APPS, AppSpeechFilter.toggle(MODE_OFF, hasApps = false))
  }

  @Test
  fun keyboardPackageOf_matchesChosenKeyboardsOnly() {
    val keyboards = setOf(AppSpeechFilter.normalizeInputMethod("sk.stopka.corvus/sk.stopka.corvus.Ime"))
    assertEquals(corvus, AppSpeechFilter.keyboardPackageOf("sk.stopka.corvus/.Ime", keyboards))
    assertEquals(corvus, AppSpeechFilter.keyboardPackageOf("sk.stopka.corvus/sk.stopka.corvus.Ime", keyboards))
    assertNull(AppSpeechFilter.keyboardPackageOf("com.google.android.inputmethod.latin/.LatinIME", keyboards))
    assertNull(AppSpeechFilter.keyboardPackageOf(null, keyboards))
    assertNull(AppSpeechFilter.keyboardPackageOf("", keyboards))
  }

  @Test
  fun othersPlayAssistantOrCall_leavesOutOwnAudio() {
    val accessibility = AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
    val call = AudioAttributes.USAGE_VOICE_COMMUNICATION
    val assistant = AudioAttributes.USAGE_ASSISTANT
    val normal = AudioManager.MODE_NORMAL
    val inCall = AudioManager.MODE_IN_COMMUNICATION
    assertTrue(AppSpeechFilter.othersPlayAssistantOrCall(listOf(assistant), accessibility, normal))
    assertTrue(AppSpeechFilter.othersPlayAssistantOrCall(listOf(call), accessibility, inCall))
    assertTrue(AppSpeechFilter.othersPlayAssistantOrCall(listOf(call), accessibility, normal))
    assertFalse(
      AppSpeechFilter.othersPlayAssistantOrCall(
        listOf(accessibility, AudioAttributes.USAGE_MEDIA),
        accessibility,
        normal,
      )
    )
    // SandFly speech on the call volume plays as a call itself.
    assertFalse(AppSpeechFilter.othersPlayAssistantOrCall(listOf(call), call, normal))
    assertTrue(AppSpeechFilter.othersPlayAssistantOrCall(listOf(call), call, inCall))
    assertFalse(AppSpeechFilter.othersPlayAssistantOrCall(emptyList(), accessibility, normal))
  }

  @Test
  fun settings_readModesAppsAndKeyboards() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().clear().commit()
    assertEquals(MODE_OFF, AppSpeechFilter.mode(context, prefs))
    assertEquals(
      context.getString(R.string.speak_user_focus_off),
      AppSpeechFilter.modeName(context, MODE_OFF),
    )
    AppSpeechFilter.setMode(context, prefs, MODE_CHOSEN_APPS)
    assertEquals(MODE_CHOSEN_APPS, AppSpeechFilter.mode(context, prefs))
    assertEquals(
      context.getString(R.string.speak_user_focus_chosen_apps),
      AppSpeechFilter.modeName(context, MODE_CHOSEN_APPS),
    )
    prefs.edit().putString(context.getString(R.string.pref_speak_user_focus_key), "bad").commit()
    assertEquals(MODE_OFF, AppSpeechFilter.mode(context, prefs))
    // The values in the settings list are the filter's modes, in order.
    assertEquals(
      AppSpeechFilter.MODES,
      context.resources.getStringArray(R.array.pref_speak_user_focus_values).toList(),
    )

    assertTrue(AppSpeechFilter.apps(context, prefs).isEmpty())
    AppSpeechFilter.setApps(context, prefs, setOf(gemini))
    assertEquals(setOf(gemini), AppSpeechFilter.apps(context, prefs))
    assertTrue(AppSpeechFilter.keyboards(context, prefs).isEmpty())
    AppSpeechFilter.setKeyboards(context, prefs, setOf("sk.stopka.corvus/.Ime"))
    assertEquals(setOf("sk.stopka.corvus/.Ime"), AppSpeechFilter.keyboards(context, prefs))
  }
}
