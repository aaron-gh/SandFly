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
package com.google.android.accessibility.talkback.keyboard

import android.os.SystemClock
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.utils.output.SpeechController
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** The SandFly keyboard's suggestion said right after the keyboard echo of a key. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuggestionSpeechTest {
  private val spoken = mutableListOf<Pair<String, Int>>()
  private var stops = 0

  @Before
  fun setUp() {
    SandFlyKeyboardHost.resetSuggestionSpeech()
    ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
    SandFlyKeyboardHost.feedback = { feedback: Feedback ->
      val speech = feedback.failovers().first().speech()
      if (speech != null) {
        spoken += speech.text().toString() to speech.options()!!.mQueueMode
      }
      true
    }
    SandFlyKeyboardHost.suggestionStopper = { stops++ }
  }

  @After
  fun tearDown() {
    SandFlyKeyboardHost.resetSuggestionSpeech()
    SandFlyKeyboardHost.feedback = null
    SandFlyKeyboardHost.suggestionStopper = null
  }

  @Test
  fun waitsForTheTextChangeOfItsKey() {
    val keyTime = SystemClock.uptimeMillis()
    SandFlyKeyboardHost.speakSuggestionAfterEcho("receive", keyTime)
    assertTrue(spoken.isEmpty())
    SandFlyKeyboardHost.onTextChangeHandled()
    assertEquals(listOf("receive" to SpeechController.QUEUE_MODE_QUEUE), spoken)
  }

  @Test
  fun spokenAtOnceAfterTheTextChangeWasHandled() {
    val keyTime = SystemClock.uptimeMillis()
    ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
    SandFlyKeyboardHost.onTextChangeHandled()
    SandFlyKeyboardHost.speakSuggestionAfterEcho("receive", keyTime)
    assertEquals(listOf("receive" to SpeechController.QUEUE_MODE_QUEUE), spoken)
  }

  @Test
  fun spokenWhenNoTextChangeComes() {
    ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
    SandFlyKeyboardHost.speakSuggestionAfterEcho("receive", SystemClock.uptimeMillis())
    ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS)
    assertEquals(listOf("receive" to SpeechController.QUEUE_MODE_QUEUE), spoken)
  }

  @Test
  fun nextKeyStopsIt() {
    ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
    SandFlyKeyboardHost.speakSuggestionAfterEcho("receive", SystemClock.uptimeMillis())
    SandFlyKeyboardHost.stopSuggestion()
    SandFlyKeyboardHost.onTextChangeHandled()
    ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS)
    assertTrue(spoken.isEmpty())
    SandFlyKeyboardHost.speakSuggestion("keyboard")
    SandFlyKeyboardHost.stopSuggestion()
    assertEquals(1, stops)
  }
}
