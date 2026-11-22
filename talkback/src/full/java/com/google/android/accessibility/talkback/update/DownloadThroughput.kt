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

/** The download speed over the last few seconds, for the time left. */
internal class DownloadThroughput {
  private val times = ArrayDeque<Long>()
  private val bytes = ArrayDeque<Long>()

  /** Adds the byte count [done] at [timeMs]. A gap or a restarted download starts over. */
  fun add(timeMs: Long, done: Long) {
    val last = times.lastOrNull()
    if (last != null && (timeMs < last || timeMs - last > WINDOW_MS || done < bytes.last())) {
      times.clear()
      bytes.clear()
    }
    times.addLast(timeMs)
    bytes.addLast(done)
    // Keep one sample at or before the start of the window.
    while (times.size > 2 && timeMs - times[1] >= WINDOW_MS) {
      times.removeFirst()
      bytes.removeFirst()
    }
  }

  /** Bytes per second, or null until the samples cover [MIN_SPAN_MS]. */
  fun bytesPerSecond(): Long? {
    if (times.size < 2) return null
    val span = times.last() - times.first()
    if (span < MIN_SPAN_MS) return null
    return (bytes.last() - bytes.first()) * 1000 / span
  }

  /** Seconds until [total] bytes at the current speed, or null when not known. */
  fun secondsLeft(done: Long, total: Long): Long? {
    if (total <= 0) return null
    val speed = bytesPerSecond()?.takeIf { it > 0 } ?: return null
    return ((total - done).coerceAtLeast(0) + speed - 1) / speed
  }

  companion object {
    const val WINDOW_MS = 5_000L
    const val MIN_SPAN_MS = 3_000L

    /** Rounds [seconds] above a minute to 10 seconds, so that the time left changes less often. */
    fun roundSecondsLeft(seconds: Long): Long =
      if (seconds <= 60) seconds else (seconds + 5) / 10 * 10
  }
}
