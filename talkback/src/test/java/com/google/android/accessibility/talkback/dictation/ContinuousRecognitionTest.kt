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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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
}
