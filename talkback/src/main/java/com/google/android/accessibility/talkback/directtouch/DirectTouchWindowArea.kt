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

package com.google.android.accessibility.talkback.directtouch

import android.graphics.Rect
import kotlin.math.roundToInt

/** A length in a declaration: a fraction of the window's width or height, dp or pixels. */
data class AreaLength(val value: Float, val unit: Unit) {
  enum class Unit {
    FRACTION,
    DP,
    PX,
  }

  /** In pixels, for a window [size] pixels wide or tall on a display of [density]. */
  fun toPixels(size: Int, density: Float): Float =
    when (unit) {
      Unit.FRACTION -> value * size
      Unit.DP -> value * density
      Unit.PX -> value
    }

  companion object {
    /**
     * "50%" or "0.5" is a fraction, "48dp" is dp and "120px" is pixels. Null when the value isn't
     * a number or is less than 0. A fraction above 1 is 1.
     */
    fun parse(text: String): AreaLength? {
      val value = text.trim().lowercase()
      val (number, unit) =
        when {
          value.endsWith("%") -> value.dropLast(1).trim().toFloatOrNull()?.div(100f) to Unit.FRACTION
          value.endsWith("dip") -> value.dropLast(3).trim().toFloatOrNull() to Unit.DP
          value.endsWith("dp") -> value.dropLast(2).trim().toFloatOrNull() to Unit.DP
          value.endsWith("px") -> value.dropLast(2).trim().toFloatOrNull() to Unit.PX
          else -> value.toFloatOrNull() to Unit.FRACTION
        }
      if (number == null || !number.isFinite() || number < 0f) {
        return null
      }
      return AreaLength(if (unit == Unit.FRACTION) number.coerceAtMost(1f) else number, unit)
    }
  }
}

/**
 * Part of an app's window. Each edge is measured from the window's left or top edge. An axis with
 * a size and one edge reaches from that edge, and an edge that is left out is the window's own.
 */
data class WindowArea(
  val left: AreaLength? = null,
  val top: AreaLength? = null,
  val right: AreaLength? = null,
  val bottom: AreaLength? = null,
  val width: AreaLength? = null,
  val height: AreaLength? = null,
) {
  /** Whether the area is empty whatever the window, such as with its top below its bottom. */
  val isEmpty: Boolean
    get() = emptyAxis(left, right, width) || emptyAxis(top, bottom, height)

  /** The area in screen pixels for a [window] on a display of [density], or null when empty. */
  fun resolve(window: Rect, density: Float): Rect? {
    val (left, right) = axis(this.left, this.right, width, window.width(), density)
    val (top, bottom) = axis(this.top, this.bottom, height, window.height(), density)
    val rect =
      Rect(
        window.left + left.roundToInt(),
        window.top + top.roundToInt(),
        window.left + right.roundToInt(),
        window.top + bottom.roundToInt(),
      )
    return rect.takeIf { it.intersect(window) && !it.isEmpty }
  }

  private companion object {
    /** The start and end of an axis [size] pixels long, from the window's start. */
    fun axis(
      start: AreaLength?,
      end: AreaLength?,
      length: AreaLength?,
      size: Int,
      density: Float,
    ): Pair<Float, Float> {
      val from = start?.toPixels(size, density)
      val to = end?.toPixels(size, density)
      val span = length?.toPixels(size, density)
      return when {
        from != null && to != null -> from to to
        from != null && span != null -> from to from + span
        to != null && span != null -> to - span to to
        from != null -> from to size.toFloat()
        to != null -> 0f to to
        span != null -> 0f to span
        else -> 0f to size.toFloat()
      }
    }

    /** Whether both edges are fractions with the start at or after the end, or the size is 0. */
    fun emptyAxis(start: AreaLength?, end: AreaLength?, length: AreaLength?): Boolean {
      if (start != null && end != null) {
        return start.unit == end.unit && start.value >= end.value
      }
      return length != null && length.value == 0f
    }
  }
}
