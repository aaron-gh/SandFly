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
package com.google.android.accessibility.talkback.recognition

import com.google.android.accessibility.talkback.actor.gemini.GeminiFailure
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RequestDescriberTest {
  private val asked = mutableListOf<Pair<String, String>>()
  private val answers = mutableListOf<(String?, GeminiFailure?) -> Unit>()
  private val results = mutableListOf<String>()
  private val failures = mutableListOf<GeminiFailure?>()
  private var cancels = 0

  private fun describer(minIntervalMs: Long = 0) = RequestDescriber(
    prompt = { modes, above -> "prompt $modes $above" },
    ask = { prompt, jpeg, done -> asked.add(prompt to jpeg); answers.add(done) },
    cancel = { cancels++ },
    minIntervalMs = minIntervalMs,
    maxResultAgeMs = 30_000,
    onResult = { results.add(it) },
    onFailure = { failures.add(it) },
  )

  @Test fun oneFrameAtATimeAndTheNextOnceAnswered() {
    val describer = describer()
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    describer.describe("b", setOf(RecognitionMode.SCENES), true)
    assertEquals(listOf("prompt [SCENES] true" to "a"), asked)
    assertFalse(describer.canRequest())
    answers.last()("A chair", null)
    assertEquals(listOf("A chair"), results)
    assertTrue(describer.canRequest())
  }

  @Test fun textOnlyIsNeverAsked() {
    describer().describe("a", setOf(RecognitionMode.TEXT), true)
    assertTrue(asked.isEmpty())
  }

  @Test fun waitsTheIntervalOfAPaidService() {
    val describer = describer(minIntervalMs = 4_000)
    describer.describe("a", setOf(RecognitionMode.PEOPLE), true)
    answers.last()("Two people", null)
    assertFalse(describer.canRequest())
    ShadowSystemClock.advanceBy(Duration.ofSeconds(4))
    assertTrue(describer.canRequest())
  }

  @Test fun oldAnswersAreDropped() {
    val describer = describer()
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
    answers.last()("An old scene", null)
    assertTrue(results.isEmpty())
    assertTrue(describer.canRequest())
  }

  @Test fun aRejectedKeyStopsAtOnce() {
    val describer = describer()
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    val failure = GeminiFailure(GeminiFailure.Kind.API_KEY)
    answers.last()(null, failure)
    assertEquals(listOf(failure), failures)
    assertFalse(describer.canRequest())
  }

  @Test fun otherFailuresStopAfterThreeInARow() {
    val describer = describer()
    repeat(2) {
      describer.describe("a", setOf(RecognitionMode.SCENES), true)
      answers.last()(null, GeminiFailure(GeminiFailure.Kind.BUSY))
    }
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    answers.last()("A desk", null)
    repeat(2) {
      describer.describe("a", setOf(RecognitionMode.SCENES), true)
      answers.last()(null, null)
    }
    assertTrue(failures.isEmpty())
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    answers.last()(null, GeminiFailure(GeminiFailure.Kind.TIMEOUT))
    assertEquals(1, failures.size)
  }

  @Test fun closingCancelsAndDropsTheLateAnswer() {
    val describer = describer()
    describer.describe("a", setOf(RecognitionMode.SCENES), true)
    describer.close()
    assertEquals(1, cancels)
    answers.last()("Too late", null)
    assertTrue(results.isEmpty())
    assertFalse(describer.canRequest())
  }
}
