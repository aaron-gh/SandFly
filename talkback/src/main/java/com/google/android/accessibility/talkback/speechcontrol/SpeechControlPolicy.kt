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

package com.google.android.accessibility.talkback.speechcontrol

/**
 * The limits on what other apps can ask Backtalk to say, kept apart from Android so that they can
 * be tested on their own.
 */
object SpeechControlPolicy {
  private const val MIN_SCALE = 0.25f
  private const val MAX_SCALE = 4f

  /** Rate and pitch, as multiples of the user's own. */
  fun clampScale(value: Float): Float =
    if (value.isNaN()) 1f else value.coerceIn(MIN_SCALE, MAX_SCALE)

  fun clampVolume(value: Float): Float = if (value.isNaN()) 1f else value.coerceIn(0f, 1f)

  /**
   * The text to say, as plain text with no styling an app could use to change how Backtalk speaks
   * it, and trimmed. Null if there is nothing to say.
   */
  fun textToSay(text: CharSequence?): String? = text?.toString()?.trim()?.ifEmpty { null }

  /**
   * Lets each app speak a few times at once, then about once a second, so that an app can't keep
   * Backtalk talking over everything else.
   */
  class RateLimiter(
    private val burst: Int = DEFAULT_BURST,
    private val refillMs: Long = DEFAULT_REFILL_MS,
  ) {
    private class Bucket(var tokens: Double, var updatedMs: Long)

    private val buckets = HashMap<String, Bucket>()

    /** Whether [app] may speak at [nowMs], using up one of its turns if so. */
    fun tryAcquire(app: String, nowMs: Long): Boolean {
      val bucket = buckets.getOrPut(app) { Bucket(burst.toDouble(), nowMs) }
      val elapsed = (nowMs - bucket.updatedMs).coerceAtLeast(0)
      bucket.tokens = (bucket.tokens + elapsed.toDouble() / refillMs).coerceAtMost(burst.toDouble())
      bucket.updatedMs = nowMs
      if (bucket.tokens < 1) return false
      bucket.tokens -= 1
      return true
    }

    fun clear() = buckets.clear()

    companion object {
      const val DEFAULT_BURST = 5
      const val DEFAULT_REFILL_MS = 1000L
    }
  }
}
