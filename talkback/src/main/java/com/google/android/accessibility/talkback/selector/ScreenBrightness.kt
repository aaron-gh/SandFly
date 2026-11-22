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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.accessibility.talkback.quicksettings.BrightnessCurve
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reads and changes the system screen brightness setting. Percentages use the same perceptual
 * scale as the system brightness slider, over the setting range of this device.
 */
internal object ScreenBrightness {
  private const val TAG = "ScreenBrightness"
  private const val MAX_PERCENT = 100
  private const val PERCENT_STEP = 10

  private val deviceCurve by lazy { BrightnessCurve.forDevice() }

  @JvmStatic fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

  @JvmStatic
  fun requestWritePermission(context: Context) {
    val intent =
      Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      // Such as on watches and TVs that have no Modify system settings screen.
      LogUtils.w(TAG, "No screen to allow changing the brightness: %s", e)
    }
  }

  @JvmStatic
  fun isAdaptive(context: Context): Boolean =
    Settings.System.getInt(
      context.contentResolver,
      Settings.System.SCREEN_BRIGHTNESS_MODE,
      Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
    ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

  @JvmStatic
  fun getPercent(context: Context): Int = settingToPercent(getSetting(context), deviceCurve)

  /**
   * Moves brightness about one step. The dim end of the curve is coarse, so this picks the setting
   * closest to the next step. Returns false if brightness is already at the limit.
   */
  @JvmStatic
  fun adjust(context: Context, increase: Boolean): Boolean {
    val newSetting = nextSetting(getSetting(context), increase, deviceCurve) ?: return false
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
  internal fun nextSetting(
    oldSetting: Int,
    increase: Boolean,
    curve: BrightnessCurve = BrightnessCurve(),
  ): Int? {
    val current = oldSetting.coerceIn(curve.min, curve.max)
    if (current == (if (increase) curve.max else curve.min)) {
      return null
    }
    val direction = if (increase) 1 else -1
    val targetPercent =
      (settingToPercent(current, curve).toFloat() / PERCENT_STEP).roundToInt() * PERCENT_STEP +
        direction * PERCENT_STEP
    val candidates = if (increase) (current + 1)..curve.max else (current - 1) downTo curve.min
    // Ties go to the farthest setting, so a step always moves past values that round the same.
    return candidates.minWith(
      compareBy<Int> { abs(settingToPercent(it, curve) - targetPercent) }
        .thenByDescending { abs(it - current) }
    )
  }

  private fun getSetting(context: Context): Int =
    Settings.System.getInt(
      context.contentResolver,
      Settings.System.SCREEN_BRIGHTNESS,
      deviceCurve.max,
    )

  internal fun settingToPercent(
    setting: Int,
    curve: BrightnessCurve = BrightnessCurve(),
  ): Int = (curve.toSlider(setting) * MAX_PERCENT).roundToInt()
}
