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

import android.os.Bundle
import android.speech.SpeechRecognizer
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContinuousRecognitionTest {
  private var done: String? = null

  private fun recognition(once: Boolean) =
    ContinuousRecognition(
      RuntimeEnvironment.getApplication(),
      null,
      onDone = { done = it },
      onFailed = {},
      once = once,
    )

  private fun results(text: String) =
    Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

  @Test
  fun once_endsWithTheFirstResult() {
    val recognition = recognition(once = true)
    recognition.onResults(results("next heading"))
    assertEquals("next heading", done)
  }

  @Test
  fun once_endsWhenNothingIsHeard() {
    val recognition = recognition(once = true)
    recognition.onError(SpeechRecognizer.ERROR_NO_MATCH)
    assertEquals("", done)
  }

  @Test
  fun once_keepsThePartialTextWhenTheRecognizerEndsWithout() {
    val recognition = recognition(once = true)
    recognition.onPartialResults(results("faster"))
    recognition.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
    assertEquals("faster", done)
  }

  @Test
  fun continuous_goesOnAfterAResultUntilStopped() {
    val recognition = recognition(once = false)
    recognition.onResults(results("first part"))
    assertNull(done)
    recognition.stop()
    recognition.onResults(results("second part"))
    assertEquals("first part second part", done)
  }

  /** Recognition with the own-sound filter on, counting the times speech is reported. */
  private fun filteredRecognition() =
    ContinuousRecognition(
      RuntimeEnvironment.getApplication(),
      null,
      onDone = { done = it },
      onFailed = {},
      onSpeech = { speech++ },
      filtersOwnSound = true,
    )

  private var speech = 0

  @Test
  fun filter_firstPartIsNotFiltered() {
    val recognition = filteredRecognition()
    // Before any part is restarted, partStartedAt is unset: the caller started this part itself.
    recognition.onBeginningOfSpeech()
    recognition.onEndOfSpeech()
    assertEquals(2, speech)
  }

  @Test
  fun filter_restartedPartIgnoresItsOwnSounds() {
    val recognition = filteredRecognition()
    recognition.onResults(results("first part")) // restarts listening: the window opens
    speech = 0
    recognition.onBeginningOfSpeech() // the start sound the recognizer reports as speech
    recognition.onEndOfSpeech()
    assertEquals(0, speech)
  }

  @Test
  fun filter_speechAfterTheWindowIsReported() {
    val recognition = filteredRecognition()
    recognition.onResults(results("first part"))
    speech = 0
    // Past the one second that a restarted part's own start sound takes to be reported.
    ShadowSystemClock.advanceBy(Duration.ofMillis(1_100))
    recognition.onBeginningOfSpeech()
    assertEquals(1, speech)
  }

  @Test
  fun filter_off_systemRecognizerKeepsSpeechEvents() {
    val recognition =
      ContinuousRecognition(
        RuntimeEnvironment.getApplication(),
        null,
        onDone = { done = it },
        onFailed = {},
        onSpeech = { speech++ },
      )
    recognition.onResults(results("first part"))
    speech = 0
    recognition.onBeginningOfSpeech()
    assertEquals(1, speech)
  }
}
