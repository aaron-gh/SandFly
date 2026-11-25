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

package com.google.android.accessibility.talkback.speech

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpeechCacheRetryTest {
  @Test
  fun failuresThatFailAgainAreKeptOnce() {
    val failures = mutableListOf("a", "b", "c")
    val retried = mutableListOf<String>()

    // Each retry fails again and adds its speech back, as the synthesis callback does.
    repeat(10) {
      SpeechCacheController.retryEach(failures) {
        retried += it
        failures += it
      }
    }

    assertEquals(listOf("a", "b", "c"), failures)
    assertEquals(30, retried.size)
  }

  @Test
  fun failuresThatSucceedAreNotRetriedAgain() {
    val failures = mutableListOf("a", "b")
    val retried = mutableListOf<String>()

    SpeechCacheController.retryEach(failures) { retried += it }
    SpeechCacheController.retryEach(failures) { retried += it }

    assertEquals(emptyList<String>(), failures)
    assertEquals(listOf("a", "b"), retried)
  }
}
