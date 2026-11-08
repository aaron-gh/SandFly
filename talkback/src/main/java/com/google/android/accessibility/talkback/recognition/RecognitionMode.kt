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

import com.google.android.accessibility.talkback.R

/** Live recognition modes, in reading control order. Each turns on and off by itself. */
enum class RecognitionMode(
  val label: Int,
  val usesAi: Boolean,
  val usesCamera: Boolean = true,
) {
  SCENES(R.string.live_recognition_scenes, true),
  TEXT(R.string.live_recognition_text, false),
  POINT_AND_SPEAK(R.string.live_recognition_point, true),
  PEOPLE(R.string.live_recognition_people, true),
  DOORS(R.string.live_recognition_doors, true),
  LIGHT(R.string.live_recognition_light, false, usesCamera = false);

  fun step(forward: Boolean): RecognitionMode =
    entries[Math.floorMod(ordinal + if (forward) 1 else -1, entries.size)]
}

/** Turns light sensor readings into the levels Live recognition speaks. */
internal object LightLevels {
  val LABELS =
    intArrayOf(
      R.string.live_recognition_light_dark,
      R.string.live_recognition_light_dim,
      R.string.live_recognition_light_normal,
      R.string.live_recognition_light_bright,
      R.string.live_recognition_light_very_bright,
    )
  // Lux at the top of each level but the last.
  private val LIMITS = floatArrayOf(5f, 50f, 500f, 5000f)
  private const val MARGIN = 0.2f

  /** The level of [lux]. To leave [current], the reading must pass its limit by [MARGIN]. */
  fun level(lux: Float, current: Int?): Int {
    if (current != null) {
      val low = if (current == 0) 0f else LIMITS[current - 1] * (1 - MARGIN)
      val high = if (current == LIMITS.size) Float.MAX_VALUE else LIMITS[current] * (1 + MARGIN)
      if (lux >= low && lux < high) return current
    }
    return LIMITS.indexOfFirst { lux < it }.let { if (it < 0) LIMITS.size else it }
  }
}

/** Suppresses repeated results without preventing a changed scene from being announced. */
internal class RecognitionAnnouncements {
  private val last = mutableMapOf<String, Pair<String, Long>>()

  fun shouldSpeak(source: String, text: String, now: Long): Boolean {
    val normalized = text.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
    if (normalized.isEmpty() || normalized.trimEnd('.', '!', '?') == "none") return false
    val previous = last[source]
    if (previous != null && (now - previous.second < MIN_INTERVAL_MS ||
        (normalized == previous.first && now - previous.second < REPEAT_INTERVAL_MS))) return false
    last[source] = normalized to now
    return true
  }

  fun clear() = last.clear()

  private companion object {
    const val MIN_INTERVAL_MS = 4_000L
    const val REPEAT_INTERVAL_MS = 30_000L
  }
}
