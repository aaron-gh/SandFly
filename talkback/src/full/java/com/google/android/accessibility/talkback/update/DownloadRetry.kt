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

/** A download failure that another attempt would not fix, such as HTTP 404. */
internal class PermanentDownloadException(message: String) : IOException(message)

/** Runs the attempts of an update download. */
internal object DownloadRetry {
  /** The wait before each attempt after the first. */
  private val DELAYS_MS = longArrayOf(2_000L, 5_000L)

  /** Returns whether a later attempt can get a different answer to HTTP status [code]. */
  fun isRetryableStatus(code: Int): Boolean = code == 408 || code == 429 || code in 500..599

  /** Throws if [received] bytes are fewer or more than the [expected] size, when that is known. */
  @Throws(IOException::class)
  fun checkSize(expected: Long, received: Long) {
    if (expected > 0 && received != expected) {
      throw IOException("Downloaded $received of $expected bytes")
    }
  }

  /**
   * Returns the result of [attempt], and repeats it after an [IOException] other than a
   * [PermanentDownloadException]. Throws the exception of the last attempt.
   */
  @Throws(IOException::class)
  fun <T> run(sleep: (Long) -> Unit = { Thread.sleep(it) }, attempt: () -> T): T {
    var retries = 0
    while (true) {
      try {
        return attempt()
      } catch (e: PermanentDownloadException) {
        throw e
      } catch (e: IOException) {
        if (retries == DELAYS_MS.size) {
          throw e
        }
        sleep(DELAYS_MS[retries++])
      }
    }
  }
}
