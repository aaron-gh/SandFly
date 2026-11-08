/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.update

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRetryTest {
  private val slept = mutableListOf<Long>()
  private val sleep: (Long) -> Unit = { slept += it }

  @Test
  fun firstAttemptSucceeds_doesNotWait() {
    assertEquals("apk", DownloadRetry.run(sleep) { "apk" })
    assertTrue(slept.isEmpty())
  }

  @Test
  fun failedAttempts_areRepeatedWithWaits() {
    var attempts = 0
    val result =
      DownloadRetry.run(sleep) {
        if (++attempts < 3) throw IOException("HTTP 504")
        "apk"
      }
    assertEquals("apk", result)
    assertEquals(3, attempts)
    assertEquals(listOf(2_000L, 5_000L), slept)
  }

  @Test
  fun everyAttemptFails_throwsTheLastFailure() {
    var attempts = 0
    val thrown =
      assertThrows(IOException::class.java) {
        DownloadRetry.run<String>(sleep) { throw IOException("attempt ${++attempts}") }
      }
    assertEquals("attempt 3", thrown.message)
    assertEquals(3, attempts)
  }

  @Test
  fun permanentFailure_isNotRepeated() {
    var attempts = 0
    assertThrows(PermanentDownloadException::class.java) {
      DownloadRetry.run<String>(sleep) {
        attempts++
        throw PermanentDownloadException("HTTP 404")
      }
    }
    assertEquals(1, attempts)
    assertTrue(slept.isEmpty())
  }

  @Test
  fun retryableStatuses() {
    listOf(408, 429, 500, 502, 503, 504).forEach { assertTrue(DownloadRetry.isRetryableStatus(it)) }
    listOf(200, 301, 400, 403, 404, 410).forEach { assertFalse(DownloadRetry.isRetryableStatus(it)) }
  }

  @Test
  fun checkSize_acceptsTheExpectedOrUnknownSize() {
    DownloadRetry.checkSize(expected = 100, received = 100)
    DownloadRetry.checkSize(expected = -1, received = 100)
  }

  @Test
  fun checkSize_rejectsShortAndLongDownloads() {
    assertThrows(IOException::class.java) { DownloadRetry.checkSize(expected = 100, received = 99) }
    assertThrows(IOException::class.java) { DownloadRetry.checkSize(expected = 100, received = 101) }
  }
}
