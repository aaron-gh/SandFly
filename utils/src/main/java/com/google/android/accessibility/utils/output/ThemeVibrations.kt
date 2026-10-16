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

package com.google.android.accessibility.utils.output

import android.media.AudioAttributes
import android.os.Vibrator

/**
 * The vibrations of the sound theme in use, by name, for the vibrations that are played outside
 * [FeedbackController], such as the braille keyboard's and direct touch's. Patterns are in the
 * format [HapticPatternParser] reads, and an empty pattern plays no vibration.
 */
object ThemeVibrations {
  @Volatile private var patterns: Map<String, IntArray> = emptyMap()

  @JvmStatic
  fun set(patternsByName: Map<String, IntArray>) {
    patterns = HashMap(patternsByName)
  }

  /** The theme's pattern for [name], or null if the theme leaves it as it is. */
  @JvmStatic fun get(name: String): IntArray? = patterns[name]

  /**
   * Plays the theme's vibration for [name] and returns true, or returns false if the theme leaves
   * it as it is. A vibration the theme turns off plays nothing and returns true. [attributes], if
   * given, are those the vibration it replaces plays with.
   */
  @JvmStatic
  @JvmOverloads
  fun play(vibrator: Vibrator, name: String, attributes: AudioAttributes? = null): Boolean {
    val pattern = patterns[name] ?: return false
    if (pattern.isEmpty()) return true
    // Starting a vibration waits for the system, so it is done off the caller's thread, in order
    // with Backtalk's other vibrations.
    FeedbackController.VIBRATION_EXECUTOR.execute {
      try {
        if (!vibrator.hasVibrator()) return@execute
        val effect = HapticPatternParser(vibrator).parse(pattern)
        @Suppress("DEPRECATION") // The attributes overload is the one that reaches API 26.
        if (attributes != null) vibrator.vibrate(effect, attributes) else vibrator.vibrate(effect)
      } catch (e: RuntimeException) {
        // A theme's pattern that the device refuses plays nothing.
      }
    }
    return true
  }
}
