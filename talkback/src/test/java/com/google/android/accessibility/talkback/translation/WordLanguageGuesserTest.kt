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

package com.google.android.accessibility.talkback.translation

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WordLanguageGuesserTest {
  private val started = mutableListOf<Pair<String, (List<Pair<String, Float>>?) -> Unit>>()
  private val guesser = WordLanguageGuesser { text, done -> started += text to done; true }
  private val candidates = listOf("es", "pt")

  @Test
  fun guess_doesNotWait_andUsesTheResultNextTime() {
    assertNull(guesser.guess("Olá, tudo bem?", candidates))
    assertEquals(1, started.size)

    started.single().second(listOf("pt" to 0.9f))
    assertEquals("pt", guesser.guess("Olá, tudo bem?", candidates))
    assertEquals(1, started.size)
  }

  @Test
  fun guess_startsEachTextOnce() {
    guesser.guess("Hola", candidates)
    guesser.guess("Hola", candidates)
    assertEquals(1, started.size)
  }

  @Test
  fun guess_afterAFailure_identifiesNothingForAWhile() {
    guesser.guess("Hola", candidates)
    started.single().second(null)
    assertNull(guesser.guess("Hola", candidates))
    assertNull(guesser.guess("Adiós", candidates))
    assertEquals(1, started.size)
  }

  /**
   * A guesser whose identification answers Portuguese from another thread after [millis], and
   * releases [answers] each time it has answered.
   */
  private fun answeringAfter(millis: Long, answers: CountDownLatch) =
    WordLanguageGuesser { _, done ->
      Thread {
          Thread.sleep(millis)
          done(listOf("pt" to 0.9f))
          answers.countDown()
        }
        .start()
      true
    }

  @Test
  fun guess_waitsInTheUtterance_onceIdentificationHasAnswered() {
    val answers = CountDownLatch(1)
    val quick = answeringAfter(millis = 1, answers)
    // The first answer loads the model, so speech doesn't wait for it.
    quick.startUtterance()
    assertNull(quick.guess("Olá", candidates))
    assertTrue(answers.await(1, TimeUnit.SECONDS))

    quick.startUtterance()
    assertEquals("pt", quick.guess("Olá, tudo bem?", candidates))
  }

  @Test
  fun guess_withoutAnUtterance_doesNotWait() {
    val answers = CountDownLatch(2)
    val quick = answeringAfter(millis = 1, answers)
    quick.guess("Olá", candidates)
    quick.guess("Olá, tudo bem?", candidates)
    assertTrue(answers.await(1, TimeUnit.SECONDS))
    assertNull(quick.guess("Bom dia", candidates))
  }

  @Test
  fun guess_waitsAtMostTheUtterancesTimeInAll() {
    var calls = 0
    val firstAnswersAtOnce =
      WordLanguageGuesser { _, done ->
        if (calls++ == 0) {
          done(listOf("pt" to 0.9f))
        } else {
          Thread {
              Thread.sleep(1_000)
              done(listOf("es" to 0.9f))
            }
            .start()
        }
        true
      }
    firstAnswersAtOnce.guess("Olá", candidates)
    firstAnswersAtOnce.startUtterance()

    var start = System.nanoTime()
    assertNull(firstAnswersAtOnce.guess("Hola, ¿qué tal?", candidates))
    assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(500))
    start = System.nanoTime()
    assertNull(firstAnswersAtOnce.guess("Buenos días", candidates))
    assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(10))
  }

  @Test
  fun guess_withoutIdentification_returnsNull() {
    val unavailable = WordLanguageGuesser { _, _ -> false }
    assertNull(unavailable.guess("Hola", candidates))
    assertNull(unavailable.guess("Hola", candidates))
  }
}
