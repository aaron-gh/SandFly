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

package com.google.android.accessibility.talkback.eventprocessor

import android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED
import com.google.android.accessibility.talkback.eventprocessor.AccessibilityEventProcessor.isLockScreenSpeechAfterScreenOn
import com.google.android.accessibility.talkback.monitor.ScreenOnRecorder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenSpeechTest {
  private val systemUi = "com.android.systemui"

  @Test
  fun systemUiAnnouncementAndFocus_justAfterScreenOn_areLockScreenSpeech() {
    assertTrue(isLockScreenSpeechAfterScreenOn(TYPE_ANNOUNCEMENT, systemUi, 200, false))
    assertTrue(isLockScreenSpeechAfterScreenOn(TYPE_VIEW_ACCESSIBILITY_FOCUSED, systemUi, 1200, false))
    assertTrue(isLockScreenSpeechAfterScreenOn(TYPE_VIEW_SELECTED, systemUi, 40, false))
  }

  @Test
  fun afterTouch_orLaterOrFromAnotherApp_isNotLockScreenSpeech() {
    assertFalse(isLockScreenSpeechAfterScreenOn(TYPE_VIEW_ACCESSIBILITY_FOCUSED, systemUi, 1200, true))
    assertFalse(isLockScreenSpeechAfterScreenOn(TYPE_ANNOUNCEMENT, systemUi, 3000, false))
    assertFalse(isLockScreenSpeechAfterScreenOn(TYPE_ANNOUNCEMENT, "com.example", 200, false))
    assertFalse(isLockScreenSpeechAfterScreenOn(TYPE_VIEW_CLICKED, systemUi, 200, false))
  }

  /** Display on at 1 000, then lock screen events, with the broadcast last and an old wake before. */
  @Test
  fun eventsBeforeBroadcast_fallInsideLockScreenWindow() {
    val recorder = ScreenOnRecorder()
    recorder.onScreenOnSignal(-59_000)
    recorder.onScreenOff()

    recorder.onScreenOnSignal(1_000)
    val announcement = 1_087 - recorder.screenOnUptime
    val selection = 1_121 - recorder.screenOnUptime
    recorder.onScreenOnSignal(1_140)

    assertTrue(isLockScreenSpeechAfterScreenOn(TYPE_ANNOUNCEMENT, systemUi, announcement, false))
    assertTrue(isLockScreenSpeechAfterScreenOn(TYPE_VIEW_SELECTED, systemUi, selection, false))
  }
}
