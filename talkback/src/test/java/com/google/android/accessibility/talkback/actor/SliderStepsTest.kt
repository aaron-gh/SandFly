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

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_PERCENT
import com.google.android.accessibility.talkback.actor.SliderSteps.STEP_APP
import com.google.android.accessibility.talkback.actor.SliderSteps.STEP_PERCENT
import com.google.android.accessibility.talkback.actor.SliderSteps.STEP_SMALLEST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** GitHub #281: Slider step, Skip steps that don't change the value, and Enter value. */
class SliderStepsTest {

  @Test
  fun appStepIsLeftToTheApp() {
    assertFalse(SliderSteps.isOwnStep(STEP_APP))
    assertFalse(SliderSteps.isOwnStep(null))
    assertFalse(SliderSteps.isOwnStep("something else"))
    assertNull(SliderSteps.next(STEP_APP, RANGE_TYPE_INT, 0f, 100f, 50f, true))
    assertEquals(0.0, SliderSteps.step(STEP_APP, RANGE_TYPE_INT, 0f, 100f), 0.0)
  }

  @Test
  fun smallestStepOfWholeNumbersIsOne() {
    assertEquals(51f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 50f, true))
    assertEquals(49f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 50f, false))
    assertEquals(161f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 1000f, 160f, true))
    assertEquals(-4f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, -10f, 10f, -5f, true))
  }

  @Test
  fun smallestStepWithoutWholeNumbersIsOnePercent() {
    assertEquals(0.51f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_FLOAT, 0f, 1f, 0.5f, true)!!, 1e-6f)
    assertEquals(51f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_PERCENT, 0f, 100f, 50f, true))
  }

  @Test
  fun percentStepOfWholeNumbersIsAWholeNumber() {
    // 1% of 1000 is 10.
    assertEquals(170f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 1000f, 160f, true))
    // 1% of 10 is less than 1, so the step is 1.
    assertEquals(6f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 10f, 5f, true))
    // 1% of 255 rounds to 3.
    assertEquals(3.0, SliderSteps.step(STEP_PERCENT, RANGE_TYPE_INT, 0f, 255f), 0.0)
    assertEquals(0.02f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_FLOAT, 0f, 2f, 0f, true)!!, 1e-6f)
  }

  @Test
  fun valuesOffTheStepsMoveOntoThem() {
    assertEquals(170f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 1000f, 163f, true))
    assertEquals(160f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 1000f, 163f, false))
    assertEquals(34f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_FLOAT, 0f, 100f, 33.3f, true))
    assertEquals(33f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_FLOAT, 0f, 100f, 33.3f, false))
  }

  @Test
  fun stepsStayInTheRange() {
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 100f, true))
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 0f, false))
    assertEquals(100f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 1000f, 99.5f, true))
    assertEquals(1000f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_INT, 0f, 1000f, 995f, true))
    assertEquals(1f, SliderSteps.next(STEP_PERCENT, RANGE_TYPE_FLOAT, 0f, 1f, 0.995f, true))
    // A current value outside the range comes back into it, or is left to the app.
    assertEquals(1f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, -20f, true))
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 150f, true))
    assertEquals(99f, SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f, 150f, false))
  }

  @Test
  fun invalidRangesAreLeftToTheApp() {
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_INT, 100f, 0f, 50f))
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_INT, 5f, 5f, 5f))
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_FLOAT, Float.NaN, 1f, 0.5f))
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_FLOAT, 0f, Float.POSITIVE_INFINITY, 0.5f))
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_FLOAT, 0f, 1f, Float.NaN))
    assertFalse(SliderSteps.isValidRange(3, 0f, 1f, 0.5f))
    // No whole number between the ends.
    assertFalse(SliderSteps.isValidRange(RANGE_TYPE_INT, 0.2f, 0.8f, 0.5f))
    assertTrue(SliderSteps.isValidRange(RANGE_TYPE_FLOAT, -Float.MAX_VALUE, Float.MAX_VALUE, 0f))
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 100f, 0f, 50f, true))
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0.2f, 0.8f, 0.5f, true))
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_FLOAT, 0f, 1f, Float.NaN, true))
  }

  @Test
  fun hugeRangesStayFinite() {
    val up = SliderSteps.next(STEP_PERCENT, RANGE_TYPE_FLOAT, -Float.MAX_VALUE, Float.MAX_VALUE, 0f, true)
    assertTrue(up != null && up.isFinite() && up > 0f)
    // A step of 1 is lost in a float this large, so the app's step is used.
    assertNull(SliderSteps.next(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 1e12f, 5e11f, true))
    assertEquals(SliderSteps.MAX_SKIPPED_STEPS, SliderSteps.skipLimit(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 1e12f))
  }

  @Test
  fun skipLimitIsTheStepsOfTheRangeAtMost200() {
    assertEquals(100, SliderSteps.skipLimit(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 100f))
    assertEquals(10, SliderSteps.skipLimit(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 10f))
    assertEquals(100, SliderSteps.skipLimit(STEP_PERCENT, RANGE_TYPE_FLOAT, 0f, 1f))
    assertEquals(200, SliderSteps.skipLimit(STEP_SMALLEST, RANGE_TYPE_INT, 0f, 1000f))
    assertEquals(200, SliderSteps.skipLimit(STEP_APP, RANGE_TYPE_INT, 0f, 100f))
    assertEquals(200, SliderSteps.skipLimit(STEP_SMALLEST, RANGE_TYPE_INT, 10f, 0f))
  }

  @Test
  fun atEndInTheDirectionOfTheStep() {
    assertTrue(SliderSteps.isAtEnd(0f, 100f, 100f, true))
    assertFalse(SliderSteps.isAtEnd(0f, 100f, 100f, false))
    assertTrue(SliderSteps.isAtEnd(0f, 100f, 0f, false))
    assertFalse(SliderSteps.isAtEnd(0f, 100f, 50f, true))
  }

  @Test
  fun spokenValueIsTheStateDescriptionThenTextThenCurrent() {
    assertEquals(
      SliderSteps.spokenValue("160 ml", "x", 1f),
      SliderSteps.spokenValue("160 ml", "y", 2f),
    )
    assertNotEquals(
      SliderSteps.spokenValue("160 ml", null, 1f),
      SliderSteps.spokenValue("165 ml", null, 1f),
    )
    assertEquals(SliderSteps.spokenValue("", "Strong", 1f), SliderSteps.spokenValue(null, "Strong", 2f))
    assertNotEquals(SliderSteps.spokenValue(null, null, 1f), SliderSteps.spokenValue(null, null, 2f))
  }

  @Test
  fun typedValuesMustBeNumbersInTheRange() {
    assertEquals(160f, SliderSteps.parseTyped(" 160 ", RANGE_TYPE_INT, 0f, 1000f))
    assertEquals(161f, SliderSteps.parseTyped("160.6", RANGE_TYPE_INT, 0f, 1000f))
    assertEquals(0.5f, SliderSteps.parseTyped("0,5", RANGE_TYPE_FLOAT, 0f, 1f))
    assertEquals(-3f, SliderSteps.parseTyped("-3", RANGE_TYPE_FLOAT, -10f, 10f))
    assertNull(SliderSteps.parseTyped("1001", RANGE_TYPE_INT, 0f, 1000f))
    assertNull(SliderSteps.parseTyped("-1", RANGE_TYPE_INT, 0f, 1000f))
    assertNull(SliderSteps.parseTyped("", RANGE_TYPE_INT, 0f, 1000f))
    assertNull(SliderSteps.parseTyped("abc", RANGE_TYPE_INT, 0f, 1000f))
    assertNull(SliderSteps.parseTyped("NaN", RANGE_TYPE_FLOAT, 0f, 1f))
    assertNull(SliderSteps.parseTyped("Infinity", RANGE_TYPE_FLOAT, 0f, 1f))
    assertNull(SliderSteps.parseTyped("1".repeat(40), RANGE_TYPE_FLOAT, 0f, Float.MAX_VALUE))
    assertNull(SliderSteps.parseTyped("5", RANGE_TYPE_INT, 10f, 0f))
  }

  @Test
  fun formatsValuesSoThatTheyCanBeTypedBack() {
    assertEquals("160", SliderSteps.format(160f, RANGE_TYPE_INT))
    assertEquals("100", SliderSteps.format(100f, RANGE_TYPE_FLOAT))
    assertEquals("0.5", SliderSteps.format(0.5f, RANGE_TYPE_FLOAT))
    assertEquals("-2.25", SliderSteps.format(-2.25f, RANGE_TYPE_FLOAT))
    assertEquals(0.1f, SliderSteps.parseTyped(SliderSteps.format(0.1f, RANGE_TYPE_FLOAT), RANGE_TYPE_FLOAT, 0f, 1f))
    assertEquals("", SliderSteps.format(Float.NaN, RANGE_TYPE_FLOAT))
  }
}
