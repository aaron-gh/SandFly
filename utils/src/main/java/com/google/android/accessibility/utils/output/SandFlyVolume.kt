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

import android.content.SharedPreferences
import kotlin.math.abs
import kotlin.math.tanh

/**
 * SandFly's own volume while media plays on a Bluetooth device. Speech and sounds then play at the
 * media volume, which is the device's volume, scaled by a percentage of SandFly's own: 80% plays at
 * 80% of the device's volume, and above 100% louder than other audio.
 *
 * Some Bluetooth devices tie the accessibility volume to another volume, such as the assistant
 * volume, so that it can't be set apart from it. This volume follows the device instead.
 */
object SandFlyVolume {
  /** Whether SandFly uses its own volume on Bluetooth. */
  const val PREF_ENABLED_KEY = "pref_sandfly_volume_bluetooth"
  const val ENABLED_DEFAULT = false

  /** SandFly's volume, in percent of the device's volume. */
  const val PREF_PERCENT_KEY = "pref_sandfly_volume_percent"
  const val PERCENT_DEFAULT = 100
  const val MIN_PERCENT = 5
  const val MAX_PERCENT = 200
  const val STEP_PERCENT = 5

  /**
   * Whether media plays on a Bluetooth device and no call is on, kept up to date by SandFly's
   * service. A preference, so that what follows the speech volume hears about it.
   */
  const val PREF_ON_BLUETOOTH_KEY = "pref_sandfly_volume_on_bluetooth"

  /** The keys whose changes change [isActive] or [gain]. */
  @JvmField
  val KEYS =
    setOf(
      PREF_ENABLED_KEY,
      PREF_PERCENT_KEY,
      PREF_ON_BLUETOOTH_KEY,
      FailoverTextToSpeech.PREF_ROUTE_FORCED_KEY,
    )

  // Up to this level, audio passes unchanged. Louder audio, which a boost makes, is rounded off
  // below full scale rather than cut, which would crackle.
  private const val LIMIT_KNEE = 0.8f

  /** The gain that SandFly's audio plays at: the volume while it is in use, 1 otherwise. */
  @Volatile
  @JvmStatic
  var gain = 1f
    private set

  /**
   * Whether SandFly's own volume is in use now. Not while **Audio output device** sends SandFly's
   * audio somewhere else, which only the accessibility volume follows.
   */
  @JvmStatic
  fun isActive(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREF_ENABLED_KEY, ENABLED_DEFAULT) &&
      prefs.getBoolean(PREF_ON_BLUETOOTH_KEY, false) &&
      !prefs.getBoolean(FailoverTextToSpeech.PREF_ROUTE_FORCED_KEY, false)

  @JvmStatic
  fun percent(prefs: SharedPreferences): Int =
    prefs.getInt(PREF_PERCENT_KEY, PERCENT_DEFAULT).coerceIn(MIN_PERCENT, MAX_PERCENT)

  /** Reads [gain] again from [prefs]. */
  @JvmStatic
  fun update(prefs: SharedPreferences) {
    gain = if (isActive(prefs)) percent(prefs) / 100f else 1f
  }

  /**
   * Raises or lowers the volume one step, and returns the new percentage, or null if it was already
   * at the end it moves towards.
   */
  @JvmStatic
  fun step(prefs: SharedPreferences, raise: Boolean): Int? {
    val current = percent(prefs)
    val next = stepped(current, raise)
    if (next == current) {
      return null
    }
    prefs.edit().putInt(PREF_PERCENT_KEY, next).apply()
    update(prefs)
    return next
  }

  /** [percent] one step up or down, kept in range and on the steps. */
  @JvmStatic
  fun stepped(percent: Int, raise: Boolean): Int {
    val onStep = (percent / STEP_PERCENT) * STEP_PERCENT
    val next =
      if (raise) {
        onStep + STEP_PERCENT
      } else if (onStep < percent) {
        onStep
      } else {
        onStep - STEP_PERCENT
      }
    return next.coerceIn(MIN_PERCENT, MAX_PERCENT)
  }

  /** The gain for speech engines and sound players, which can't play louder than full volume. */
  @JvmStatic
  fun gainUpToFull(): Float = minOf(gain, 1f)

  /** [sample] after [gain], with peaks above the knee rounded off below full scale. */
  @JvmStatic
  fun limit(sample: Float): Float {
    val level = abs(sample)
    if (level <= LIMIT_KNEE) {
      return sample
    }
    val over = (level - LIMIT_KNEE) / (1 - LIMIT_KNEE)
    val limited = LIMIT_KNEE + (1 - LIMIT_KNEE) * tanh(over)
    return if (sample < 0) -limited else limited
  }
}
