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

import android.content.Context
import com.google.android.accessibility.talkback.R
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Sorts pixels into named colors for the Color detection mode, and sums up which colors cover the
 * centre of a frame. A color is a family, such as blue, and a shade: plain, dark or light.
 */
internal object ColorClassifier {
  const val BLACK = 0
  const val GRAY = 1
  const val WHITE = 2
  const val RED = 3
  const val ORANGE = 4
  const val YELLOW = 5
  const val GREEN = 6
  const val TURQUOISE = 7
  const val BLUE = 8
  const val PURPLE = 9
  const val PINK = 10
  const val BROWN = 11
  const val FAMILIES = 12

  const val PLAIN = 0
  const val DARK = 1
  const val LIGHT = 2
  const val SHADES = 3

  /** The number of colors: [FAMILIES] times [SHADES]. */
  const val COLORS = FAMILIES * SHADES

  /** Too few pixels were read. */
  const val NONE = 0

  /** No color covers enough of the centre. */
  const val MIXED = 1

  /** The fewest pixels a result is made from. */
  private const val MIN_SAMPLES = 16

  /** Below this brightness (HSV value), a pixel is black. */
  private const val BLACK_V = 0.12f
  /** Below this saturation or chroma, a pixel is gray or white. */
  private const val GRAY_S = 0.15f
  private const val GRAY_CHROMA = 16
  private const val DARK_GRAY_V = 0.4f
  private const val GRAY_V = 0.7f
  private const val LIGHT_GRAY_V = 0.88f
  /** Below this brightness, a color is dark. */
  private const val DARK_V = 0.4f
  /** Below this saturation and above this brightness, a color is light. */
  private const val LIGHT_S = 0.45f
  private const val LIGHT_V = 0.7f
  /** Below this brightness, orange is brown; below the second, dark brown. */
  private const val BROWN_V = 0.62f
  private const val DARK_BROWN_V = 0.3f

  /** The share of the pixels, in percent, that one color must cover to be named alone. */
  private const val SINGLE_PERCENT = 50
  /** The shares that two colors must cover, each and together, to be named together. */
  private const val SECOND_PERCENT = 25
  private const val PAIR_PERCENT = 65

  fun color(family: Int, shade: Int): Int = family * SHADES + shade

  /** The color of a pixel, with [r], [g] and [b] from 0 to 255. */
  fun classify(r: Int, g: Int, b: Int): Int {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val v = max / 255f
    if (v < BLACK_V) return color(BLACK, PLAIN)
    val chroma = max - min
    val s = chroma.toFloat() / max
    if (s < GRAY_S || chroma < GRAY_CHROMA) {
      return when {
        v < DARK_GRAY_V -> color(GRAY, DARK)
        v < GRAY_V -> color(GRAY, PLAIN)
        v < LIGHT_GRAY_V -> color(GRAY, LIGHT)
        else -> color(WHITE, PLAIN)
      }
    }
    var hue =
      60f *
        when (max) {
          r -> (g - b).toFloat() / chroma
          g -> (b - r).toFloat() / chroma + 2
          else -> (r - g).toFloat() / chroma + 4
        }
    if (hue < 0) hue += 360f
    var family =
      when {
        hue < 15f || hue >= 345f -> RED
        hue < 40f -> ORANGE
        hue < 70f -> YELLOW
        hue < 165f -> GREEN
        hue < 195f -> TURQUOISE
        hue < 255f -> BLUE
        hue < 290f -> PURPLE
        else -> PINK
      }
    var shade =
      when {
        v < DARK_V -> DARK
        s < LIGHT_S && v > LIGHT_V -> LIGHT
        else -> PLAIN
      }
    if (family == ORANGE && v < BROWN_V) {
      family = BROWN
      shade = if (v < DARK_BROWN_V) DARK else PLAIN
    } else if (family == RED && shade == LIGHT) {
      family = PINK
      shade = PLAIN
    }
    return color(family, shade)
  }

  /**
   * Sums up [counts], the pixels of each color: [MIXED], [NONE], or a result of one or two colors
   * for [first] and [second]. [families] is scratch space of [FAMILIES] entries.
   */
  fun summarize(counts: IntArray, families: IntArray): Int {
    var total = 0
    for (family in 0 until FAMILIES) {
      val sum = counts[family * SHADES] + counts[family * SHADES + 1] + counts[family * SHADES + 2]
      families[family] = sum
      total += sum
    }
    if (total < MIN_SAMPLES) return NONE
    var top = 0
    var next = -1
    for (family in 1 until FAMILIES) {
      if (families[family] > families[top]) {
        next = top
        top = family
      } else if (next < 0 || families[family] > families[next]) {
        next = family
      }
    }
    val topCount = families[top]
    val nextCount = if (next < 0) 0 else families[next]
    return when {
      nextCount * 100 >= total * SECOND_PERCENT && (topCount + nextCount) * 100 >= total * PAIR_PERCENT ->
        result(shadeOf(counts, top), shadeOf(counts, next))
      topCount * 100 >= total * SINGLE_PERCENT -> result(shadeOf(counts, top), -1)
      else -> MIXED
    }
  }

  /** The result naming [color], and [other] after it unless it is -1. */
  fun result(color: Int, other: Int): Int = (color + 1) * 100 + (other + 1)

  /** The first color of [result], or -1 for [MIXED] and [NONE]. */
  fun first(result: Int): Int = if (result < 100) -1 else result / 100 - 1

  /** The second color of [result], or -1. */
  fun second(result: Int): Int = if (result < 100) -1 else result % 100 - 1

  /** The most common shade of [family], plain on a tie. */
  private fun shadeOf(counts: IntArray, family: Int): Int {
    val base = family * SHADES
    var shade = PLAIN
    for (candidate in DARK..LIGHT) if (counts[base + candidate] > counts[base + shade]) shade = candidate
    return base + shade
  }
}

/**
 * Reads the colors of the centre of YUV_420_888 frames. Keeps its counts between frames, so it
 * allocates nothing per frame. One thread only.
 */
internal class ColorSampler {
  private val counts = IntArray(ColorClassifier.COLORS)
  private val families = IntArray(ColorClassifier.FAMILIES)

  /**
   * The colors of the centre of a frame of [width] by [height] pixels: a result of
   * [ColorClassifier.summarize]. [y] is the luma plane; [u] and [v] are the chroma planes at half
   * the size, which share their strides. Reads with absolute gets, so the positions of the buffers
   * are left as they were; pixels past the end of a buffer are skipped.
   */
  fun measure(
    y: ByteBuffer,
    yRowStride: Int,
    yPixelStride: Int,
    u: ByteBuffer,
    v: ByteBuffer,
    uvRowStride: Int,
    uvPixelStride: Int,
    width: Int,
    height: Int,
  ): Int {
    counts.fill(0)
    if (width < GRID || height < GRID) return ColorClassifier.NONE
    val left = (width * REGION_START).toInt()
    val top = (height * REGION_START).toInt()
    val spanX = (width * (1 - 2 * REGION_START)).toInt()
    val spanY = (height * (1 - 2 * REGION_START)).toInt()
    for (row in 0 until GRID) {
      val py = top + (row * 2 + 1) * spanY / (GRID * 2)
      for (column in 0 until GRID) {
        val px = left + (column * 2 + 1) * spanX / (GRID * 2)
        val yIndex = py * yRowStride + px * yPixelStride
        val uvIndex = (py / 2) * uvRowStride + (px / 2) * uvPixelStride
        if (yIndex >= y.limit() || uvIndex >= u.limit() || uvIndex >= v.limit()) continue
        val luma = y.get(yIndex).toInt() and 0xFF
        val cb = (u.get(uvIndex).toInt() and 0xFF) - 128
        val cr = (v.get(uvIndex).toInt() and 0xFF) - 128
        // Full-range BT.601, as camera frames use.
        val r = (luma + 1.402f * cr).toInt().coerceIn(0, 255)
        val g = (luma - 0.344136f * cb - 0.714136f * cr).toInt().coerceIn(0, 255)
        val b = (luma + 1.772f * cb).toInt().coerceIn(0, 255)
        counts[ColorClassifier.classify(r, g, b)]++
      }
    }
    return ColorClassifier.summarize(counts, families)
  }

  private companion object {
    /** Pixels read in each direction. */
    const val GRID = 24
    /** The centre is the middle 40% of the frame in each direction. */
    const val REGION_START = 0.3f
  }
}

/**
 * Decides when a Color detection result is spoken: once it has held for [stableCount] frames in a
 * row, when it differs from the last one spoken, and at least [minIntervalMs] after it. One thread
 * only.
 */
internal class ColorAnnouncer(
  private val stableCount: Int = 3,
  private val minIntervalMs: Long = 1_500L,
) {
  private var candidate = ColorClassifier.NONE
  private var seen = 0
  private var spoken = ColorClassifier.NONE
  private var spokenAt = 0L
  private var anySpoken = false

  /** Takes the [result] of a frame at [now]. Returns the result to speak, or [NOTHING]. */
  fun update(result: Int, now: Long): Int {
    if (result == candidate) seen++
    else {
      candidate = result
      seen = 1
    }
    if (result == ColorClassifier.NONE || seen < stableCount) return NOTHING
    if (anySpoken && (result == spoken || now - spokenAt < minIntervalMs)) return NOTHING
    anySpoken = true
    spoken = result
    spokenAt = now
    return result
  }

  companion object {
    const val NOTHING = -1
  }
}

/** The names of Color detection results. */
internal object ColorNames {
  private val NAMES =
    IntArray(ColorClassifier.COLORS).also {
      fun set(family: Int, plain: Int, dark: Int = plain, light: Int = plain) {
        it[ColorClassifier.color(family, ColorClassifier.PLAIN)] = plain
        it[ColorClassifier.color(family, ColorClassifier.DARK)] = dark
        it[ColorClassifier.color(family, ColorClassifier.LIGHT)] = light
      }
      set(ColorClassifier.BLACK, R.string.live_recognition_color_black)
      set(
        ColorClassifier.GRAY,
        R.string.live_recognition_color_gray,
        R.string.live_recognition_color_dark_gray,
        R.string.live_recognition_color_light_gray,
      )
      set(ColorClassifier.WHITE, R.string.live_recognition_color_white)
      set(ColorClassifier.RED, R.string.live_recognition_color_red, dark = R.string.live_recognition_color_dark_red)
      set(ColorClassifier.ORANGE, R.string.live_recognition_color_orange, light = R.string.live_recognition_color_light_orange)
      set(
        ColorClassifier.YELLOW,
        R.string.live_recognition_color_yellow,
        R.string.live_recognition_color_dark_yellow,
        R.string.live_recognition_color_light_yellow,
      )
      set(
        ColorClassifier.GREEN,
        R.string.live_recognition_color_green,
        R.string.live_recognition_color_dark_green,
        R.string.live_recognition_color_light_green,
      )
      set(
        ColorClassifier.TURQUOISE,
        R.string.live_recognition_color_turquoise,
        R.string.live_recognition_color_dark_turquoise,
        R.string.live_recognition_color_light_turquoise,
      )
      set(
        ColorClassifier.BLUE,
        R.string.live_recognition_color_blue,
        R.string.live_recognition_color_dark_blue,
        R.string.live_recognition_color_light_blue,
      )
      set(
        ColorClassifier.PURPLE,
        R.string.live_recognition_color_purple,
        R.string.live_recognition_color_dark_purple,
        R.string.live_recognition_color_light_purple,
      )
      set(
        ColorClassifier.PINK,
        R.string.live_recognition_color_pink,
        R.string.live_recognition_color_dark_pink,
        R.string.live_recognition_color_light_pink,
      )
      set(ColorClassifier.BROWN, R.string.live_recognition_color_brown, dark = R.string.live_recognition_color_dark_brown)
    }

  /** The text of [result], such as "Dark blue" or "Red and white". */
  fun describe(context: Context, result: Int): String {
    val first = ColorClassifier.first(result)
    val text =
      when {
        first < 0 -> context.getString(R.string.live_recognition_color_mixed)
        ColorClassifier.second(result) < 0 -> context.getString(NAMES[first])
        else ->
          context.getString(
            R.string.live_recognition_color_pair,
            context.getString(NAMES[first]),
            context.getString(NAMES[ColorClassifier.second(result)]),
          )
      }
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
  }
}
