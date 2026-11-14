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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

  @Test
  fun guess_withoutIdentification_returnsNull() {
    val unavailable = WordLanguageGuesser { _, _ -> false }
    assertNull(unavailable.guess("Hola", candidates))
    assertNull(unavailable.guess("Hola", candidates))
  }
}
