/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScriptSpeechChainTest {
  @Test
  fun queuedTimeAndEarlierListenersShareOneBudget() {
    var clock = 10L // Ten milliseconds already spent waiting in the queue.
    val budgets = mutableListOf<Long>()
    val result = rewriteSpeechChain("original", listOf(1, 2), 30, { clock }) { _, text, budget ->
      budgets.add(budget)
      clock += 8
      "$text changed"
    }
    assertEquals(listOf(20L, 12L), budgets)
    assertEquals("original changed changed", result)
  }

  @Test
  fun expiredQueuedRequestNeverCallsAnyListener() {
    var calls = 0
    val result = rewriteSpeechChain("original", listOf(1), 30, { 30L }) { _, _, _ ->
      calls++
      "changed"
    }
    assertNull(result)
    assertEquals(0, calls)
  }

  @Test
  fun overrunDiscardsTheLateRewriteAndDoesNotRunLaterListeners() {
    var clock = 0L
    var calls = 0
    val result = rewriteSpeechChain("original", listOf(1, 2), 30, { clock }) { _, _, _ ->
      calls++
      clock = 31
      ""
    }
    assertNull(result)
    assertEquals(1, calls)
  }

  @Test
  fun timelySuppressionAndUnchangedResultsRemainDistinct() {
    assertEquals("", rewriteSpeechChain("original", listOf(1), 30, { 0L }) { _, _, _ -> "" })
    assertNull(rewriteSpeechChain("original", listOf(1), 30, { 0L }) { _, _, _ -> null })
    assertNull(rewriteSpeechChain("original", listOf(1), 30, { 0L }) { _, text, _ -> text })
  }
}
