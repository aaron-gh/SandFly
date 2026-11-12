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

package com.google.android.accessibility.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueStepsTest {
  @Test
  fun outsideRun_stepsGoAroundTheEnds() {
    assertEquals(0, ValueSteps.step(2, 3, forward = true))
    assertEquals(2, ValueSteps.step(0, 3, forward = false))
    assertEquals(1, ValueSteps.step(0, 3, forward = true))
  }

  @Test
  fun withWrapping_stepsGoAroundTheEndsAndDontStop() {
    var index = -1
    assertFalse(ValueSteps.run(true) { index = ValueSteps.step(2, 3, forward = true) })
    assertEquals(0, index)
  }

  @Test
  fun withoutWrapping_stepsStayAtTheLastAndFirstValue() {
    var index = -1
    assertTrue(ValueSteps.run(false) { index = ValueSteps.step(2, 3, forward = true) })
    assertEquals(2, index)
    assertTrue(ValueSteps.run(false) { index = ValueSteps.step(0, 3, forward = false) })
    assertEquals(0, index)
  }

  @Test
  fun withoutWrapping_stepsAwayFromTheEndsMove() {
    var index = -1
    assertFalse(ValueSteps.run(false) { index = ValueSteps.step(1, 3, forward = true) })
    assertEquals(2, index)
    assertFalse(ValueSteps.run(false) { index = ValueSteps.step(2, 3, forward = false) })
    assertEquals(1, index)
  }

  @Test
  fun withoutWrapping_aSingleValueStays() {
    var index = -1
    assertTrue(ValueSteps.run(false) { index = ValueSteps.step(0, 1, forward = true) })
    assertEquals(0, index)
  }

  @Test
  fun withoutWrapping_anIndexOutsideTheListGoesAroundAsBefore() {
    var index = -2
    assertFalse(ValueSteps.run(false) { index = ValueSteps.step(-1, 3, forward = true) })
    assertEquals(0, index)
  }

  @Test
  fun afterRun_stepsGoAroundAgain() {
    ValueSteps.run(false) { ValueSteps.step(2, 3, forward = true) }
    assertEquals(0, ValueSteps.step(2, 3, forward = true))
  }

  @Test
  fun aNestedRun_reportsOnlyItsOwnStops() {
    var inner = false
    val outer =
      ValueSteps.run(false) { inner = ValueSteps.run(false) { ValueSteps.step(2, 3, true) } }
    assertTrue(inner)
    assertFalse(outer)
  }
}
