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

package com.google.android.accessibility.utils.output

import android.os.Vibrator

/**
 * The Vibration strength and Vibration type in use, for vibrations played outside
 * [FeedbackController]: theme vibrations and the braille keyboard.
 */
object VibrationSettings {
  @Volatile @JvmStatic var strength = 1f
  @Volatile @JvmStatic var type: String? = null

  /** A parser for [vibrator] with the current strength and type. */
  @JvmStatic
  fun parser(vibrator: Vibrator): HapticPatternParser =
    HapticPatternParser(vibrator).apply {
      setStrength(strength)
      setType(type)
    }
}
