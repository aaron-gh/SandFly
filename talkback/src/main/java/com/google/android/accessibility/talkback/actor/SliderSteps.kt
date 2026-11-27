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

package com.google.android.accessibility.talkback.actor

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The steps of Slider step (GitHub #281): the value a slider is set to with ACTION_SET_PROGRESS
 * instead of the app's own step, and the checks of the slider's range, which comes from the app.
 *
 * - [STEP_APP], the default: the app's step, with ACTION_SCROLL_FORWARD and ACTION_SCROLL_BACKWARD.
 * - [STEP_PERCENT]: 1% of the range. On a range of whole numbers the step is a whole number, at
 *   least 1.
 * - [STEP_SMALLEST]: 1 on a range of whole numbers. Apps don't say the step of other ranges, so
 *   those use 1% of the range.
 *
 * A step goes to the next multiple of the step from the minimum, so that values off the steps, such
 * as one set by touch, move onto them. Values stay within the range.
 */
object SliderSteps {
  const val STEP_APP = "app"
  const val STEP_PERCENT = "percent"
  const val STEP_SMALLEST = "smallest"

  /** The most steps that one adjustment takes while skipping steps that don't change the value. */
  const val MAX_SKIPPED_STEPS = 200

  /** The most characters taken from a typed value. */
  const val MAX_TYPED_LENGTH = 32

  /** Room for float rounding when a value is compared with the steps. */
  private const val EPSILON = 1e-6

  /**
   * Whether a range of this [type] from [min] to [max] with the value [current] can be stepped and
   * set: a known type, finite values, and [max] above [min]. The current value may be outside the
   * range.
   */
  @JvmStatic
  fun isValidRange(type: Int, min: Float, max: Float, current: Float): Boolean {
    if (
      type != RangeInfoCompat.RANGE_TYPE_INT &&
        type != RangeInfoCompat.RANGE_TYPE_FLOAT &&
        type != RangeInfoCompat.RANGE_TYPE_PERCENT
    ) {
      return false
    }
    if (!min.isFinite() || !max.isFinite() || !current.isFinite()) return false
    val span = max.toDouble() - min.toDouble()
    if (!(span > 0) || !span.isFinite()) return false
    // A range of whole numbers needs a whole number in it.
    return type != RangeInfoCompat.RANGE_TYPE_INT || ceil(min.toDouble()) <= floor(max.toDouble())
  }

  /** Whether [mode] is one of SandFly's steps rather than the app's. */
  @JvmStatic fun isOwnStep(mode: String?): Boolean = mode == STEP_PERCENT || mode == STEP_SMALLEST

  /** The step of [mode] on a range from [min] to [max], or 0 for the app's step. */
  @JvmStatic
  fun step(mode: String?, type: Int, min: Float, max: Float): Double {
    val span = max.toDouble() - min.toDouble()
    if (!(span > 0) || !span.isFinite()) return 0.0
    val whole = type == RangeInfoCompat.RANGE_TYPE_INT
    return when (mode) {
      STEP_PERCENT -> if (whole) max(1.0, (span / 100).roundToLong().toDouble()) else span / 100
      STEP_SMALLEST -> if (whole) 1.0 else span / 100
      else -> 0.0
    }
  }

  /**
   * The value one step of [mode] up ([increase]) or down from [current], or null when there is
   * none: the app's step is in use, the range is not valid, the value is at the end of the range, or
   * the step is too small to change a float of this size.
   */
  @JvmStatic
  fun next(mode: String?, type: Int, min: Float, max: Float, current: Float, increase: Boolean): Float? {
    if (!isOwnStep(mode) || !isValidRange(type, min, max, current)) return null
    val step = step(mode, type, min, max)
    if (!(step > 0) || !step.isFinite()) return null
    val low = min.toDouble()
    val high = max.toDouble()
    val position = (current.toDouble().coerceIn(low, high) - low) / step
    val index = if (increase) floor(position + EPSILON) + 1 else ceil(position - EPSILON) - 1
    var target = (low + index * step).coerceIn(low, high)
    if (type == RangeInfoCompat.RANGE_TYPE_INT) {
      target = Math.round(target).toDouble().coerceIn(ceil(low), floor(high))
    }
    val result = target.toFloat()
    if (!result.isFinite()) return null
    val moves = if (increase) result > current else result < current
    return if (moves) result else null
  }

  /**
   * The most further steps taken while skipping steps that don't change the value: the number of
   * steps of [mode] in the range, and [MAX_SKIPPED_STEPS] at most and for the app's step.
   */
  @JvmStatic
  fun skipLimit(mode: String?, type: Int, min: Float, max: Float): Int {
    val step = step(mode, type, min, max)
    if (!(step > 0) || !step.isFinite()) return MAX_SKIPPED_STEPS
    val steps = ceil((max.toDouble() - min.toDouble()) / step)
    return if (steps.isFinite()) min(steps, MAX_SKIPPED_STEPS.toDouble()).toInt() else MAX_SKIPPED_STEPS
  }

  /** Whether [current] is at or past the end of the range in the direction of [increase]. */
  @JvmStatic
  fun isAtEnd(min: Float, max: Float, current: Float, increase: Boolean): Boolean =
    if (increase) current >= max else current <= min

  /**
   * The value that SandFly speaks for a slider, compared before and after a step: the state
   * description where the app sets one, otherwise the text, otherwise the current value of the range.
   * The content description is left out: it is usually the slider's name, which stays the same.
   */
  @JvmStatic
  fun spokenValue(stateDescription: CharSequence?, text: CharSequence?, current: Float): String =
    when {
      !stateDescription.isNullOrEmpty() -> "s:$stateDescription"
      !text.isNullOrEmpty() -> "t:$text"
      else -> "c:$current"
    }

  /**
   * The value typed in Enter value, or null when it isn't a number in the range from [min] to [max].
   * A comma is taken as a decimal point. On a range of whole numbers the value is rounded.
   */
  @JvmStatic
  fun parseTyped(typed: CharSequence?, type: Int, min: Float, max: Float): Float? {
    if (typed == null || typed.length > MAX_TYPED_LENGTH) return null
    if (!isValidRange(type, min, max, min)) return null
    val number = typed.toString().trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!number.isFinite()) return null
    val value =
      if (type == RangeInfoCompat.RANGE_TYPE_INT) Math.round(number).toDouble() else number
    if (value < min.toDouble() - EPSILON || value > max.toDouble() + EPSILON) return null
    return value.coerceIn(min.toDouble(), max.toDouble()).toFloat()
  }

  /**
   * [value] as shown in Enter value: whole numbers without decimals, others with six significant
   * digits at most, and no grouping, so that the text can be typed back.
   */
  @JvmStatic
  fun format(value: Float, type: Int): String {
    if (!value.isFinite()) return ""
    val number = value.toDouble()
    if (Math.abs(number) < 1e15 && (type == RangeInfoCompat.RANGE_TYPE_INT || number == floor(number))) {
      return Math.round(number).toString()
    }
    return BigDecimal(number).round(MathContext(6)).stripTrailingZeros().toPlainString()
  }
}
