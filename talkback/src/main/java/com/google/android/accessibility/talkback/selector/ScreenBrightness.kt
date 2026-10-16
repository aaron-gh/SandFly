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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Reads and changes the system screen brightness setting. Percentages use the same perceptual
 * scale as the system brightness slider.
 */
internal object ScreenBrightness {
  private const val MIN_SETTING = 1
  private const val MAX_SETTING = 255
  private const val MAX_PERCENT = 100
  private const val PERCENT_STEP = 10

  // Hybrid log-gamma curve from SettingsLib BrightnessUtils, which the system slider uses.
  private const val HLG_R = 0.5f
  private const val HLG_A = 0.17883277f
  private const val HLG_B = 0.28466892f
  private const val HLG_C = 0.55991073f
  private const val HLG_SCALE = 12f

  @JvmStatic fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

  @JvmStatic
  fun requestWritePermission(context: Context) {
    val intent =
      Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  @JvmStatic
  fun isAdaptive(context: Context): Boolean =
    Settings.System.getInt(
      context.contentResolver,
      Settings.System.SCREEN_BRIGHTNESS_MODE,
      Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
    ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

  @JvmStatic fun getPercent(context: Context): Int = settingToPercent(getSetting(context))

  /**
   * Moves brightness about one step. The dim end of the curve is coarse, so this picks the setting
   * closest to the next step. Returns false if brightness is already at the limit.
   */
  @JvmStatic
  fun adjust(context: Context, increase: Boolean): Boolean {
    val newSetting = nextSetting(getSetting(context), increase) ?: return false
    return Settings.System.putInt(
      context.contentResolver,
      Settings.System.SCREEN_BRIGHTNESS,
      newSetting,
    )
  }

  /**
   * The setting one step brighter or dimmer than [oldSetting], or null if it is already at the
   * limit. Other apps can store any value, such as 0, so [oldSetting] is first brought into range.
   */
  internal fun nextSetting(oldSetting: Int, increase: Boolean): Int? {
    val current = oldSetting.coerceIn(MIN_SETTING, MAX_SETTING)
    if (current == (if (increase) MAX_SETTING else MIN_SETTING)) {
      return null
    }
    val direction = if (increase) 1 else -1
    val targetPercent =
      (settingToPercent(current).toFloat() / PERCENT_STEP).roundToInt() * PERCENT_STEP +
        direction * PERCENT_STEP
    val candidates = if (increase) (current + 1)..MAX_SETTING else (current - 1) downTo MIN_SETTING
    // Ties go to the farthest setting, so a step always moves past values that round the same.
    return candidates.minWith(
      compareBy<Int> { abs(settingToPercent(it) - targetPercent) }
        .thenByDescending { abs(it - current) }
    )
  }

  private fun getSetting(context: Context): Int =
    Settings.System.getInt(
      context.contentResolver,
      Settings.System.SCREEN_BRIGHTNESS,
      MAX_SETTING,
    )

  internal fun settingToPercent(setting: Int): Int {
    val scaled =
      HLG_SCALE * (setting.coerceIn(MIN_SETTING, MAX_SETTING) - MIN_SETTING) /
        (MAX_SETTING - MIN_SETTING)
    val gamma = if (scaled <= 1) sqrt(scaled) * HLG_R else HLG_A * ln(scaled - HLG_B) + HLG_C
    return (gamma * MAX_PERCENT).roundToInt()
  }
}
