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

import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The display sizes of the default display, as percentages of its default density, as Android's
 * Display size setting offers them.
 *
 * [initial] is the density `wm density reset` returns to, [default] the density 100% is, which
 * differs from [initial] when the screen resolution is lowered, [current] the density now, and
 * [smallestPx] the display's smaller side in pixels.
 */
class DisplaySizes(val initial: Int, val default: Int, val current: Int, private val smallestPx: Int) {

  /** The size now. */
  val currentPercent: Int = percentOf(current)

  /** The largest size, which keeps the smaller side at least 320 dp, as Android's largest does. */
  val max: Int

  /**
   * The smallest size, which keeps the smaller side under 600 dp, where apps change to their
   * tablet layouts. A display that is already that wide gets Android's smallest size, 85%.
   */
  val min: Int

  /** The sizes the quick settings panel offers: Android's, within [min] and [max]. */
  val options: List<Int>

  init {
    val largestDensity = minOf(smallestPx / 2, ShizukuCommand.MAX_DENSITY)
    var largest = maxOf(floor(largestDensity * 100.0 / default).toInt(), 100)
    while (largest > 100 && density(largest) > largestDensity) largest--
    max = largest

    min =
      if (widthDp(default) >= TABLET_DP) {
        SMALL
      } else {
        // Wider than this many dp would reach 600 dp.
        val smallestDensity = maxOf(floor(smallestPx * DP / TABLET_DP).toInt() + 1, ShizukuCommand.MIN_DENSITY)
        var smallest = minOf(ceil(smallestDensity * 100.0 / default).toInt(), 100)
        while (smallest > 1 && density(smallest - 1) >= smallestDensity) smallest--
        while (smallest < 100 && density(smallest) < smallestDensity) smallest++
        smallest
      }

    val larger = ((max - 100) / LARGER_STEP).coerceIn(0, MAX_LARGER)
    options =
      (listOf(SMALL).filter { it >= min } + 100 +
          (1..larger).map { 100 + ((max - 100) * it / larger.toDouble()).roundToInt() })
        .distinct()
  }

  /** The density in dpi of [percent]. */
  fun density(percent: Int): Int = (default * percent / 100.0).roundToInt()

  private fun percentOf(density: Int): Int = (density * 100.0 / default).roundToInt()

  private fun widthDp(density: Int): Double = smallestPx * DP / density

  /** The [ShizukuCommand.DISPLAY_DENSITY] argument for [percent]. */
  fun argument(percent: Int): String =
    density(percent).let { if (it == initial) ShizukuCommand.RESET else it.toString() }

  companion object {
    private const val DP = 160.0
    private const val TABLET_DP = 600.0
    /** Android's Small display size. */
    private const val SMALL = 85
    /** Android's sizes above the default are at least this many percent apart, and at most 3. */
    private const val LARGER_STEP = 9
    private const val MAX_LARGER = 3
    /** The secure setting `wm density` writes, empty when the density is the device's own. */
    private const val DENSITY_FORCED = "display_density_forced"

    /**
     * The default display's sizes, or null when its density can't be read. When the density was
     * changed, 100% is the device's own density, scaled down when a lowered screen resolution is
     * set with a smaller size than the display's.
     */
    fun of(context: Context): DisplaySizes? {
      val display =
        context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION") // The display's own metrics, not a window's.
      display.getRealMetrics(metrics)
      val smallest = min(metrics.widthPixels, metrics.heightPixels)
      val current = metrics.densityDpi
      if (smallest <= 0 || current <= 0) return null
      val forced =
        try {
          Settings.Secure.getString(context.contentResolver, DENSITY_FORCED)
        } catch (e: RuntimeException) {
          // Some versions don't let apps read it; take it as changed.
          "?"
        }
      if (forced.isNullOrEmpty()) {
        return DisplaySizes(current, current, current, smallest)
      }
      val initial = DisplayMetrics.DENSITY_DEVICE_STABLE.takeIf { it > 0 } ?: return null
      val mode = display.mode
      val physicalSmallest = min(mode.physicalWidth, mode.physicalHeight)
      val default =
        if (physicalSmallest > smallest) (initial * smallest.toDouble() / physicalSmallest).roundToInt()
        else initial
      return DisplaySizes(initial, default, current, smallest)
    }
  }
}
