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

package com.google.android.accessibility.talkback.quicksettings

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The curve of the system's brightness slider, from SystemUI's BrightnessUtils, so that a
 * brightness percentage means what it means in the system's quick settings. The slider moves
 * through a gamma space, and the setting holds a linear value from 1 to 255.
 */
object BrightnessCurve {
  const val MIN = 1
  const val MAX = 255
  private const val R = 0.5
  private const val A = 0.17883277
  private const val B = 0.28466892
  private const val C = 0.55991073

  /** The slider's position, from 0 to 1, for a brightness setting. */
  fun toSlider(setting: Int): Double {
    val normalized = (setting.coerceIn(MIN, MAX) - MIN).toDouble() / (MAX - MIN) * 12
    val gamma = if (normalized <= 1) sqrt(normalized) * R else A * ln(normalized - B) + C
    return gamma.coerceIn(0.0, 1.0)
  }

  /** The brightness setting for a slider position from 0 to 1. */
  fun toSetting(slider: Double): Int {
    val position = slider.coerceIn(0.0, 1.0)
    val linear = if (position <= R) (position / R) * (position / R) else exp((position - C) / A) + B
    return (MIN + (MAX - MIN) * (linear / 12)).roundToInt().coerceIn(MIN, MAX)
  }
}
