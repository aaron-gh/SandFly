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

package com.google.android.accessibility.talkback.macros

import com.google.android.accessibility.talkback.quicksettings.Choice
import com.google.android.accessibility.talkback.quicksettings.ChoiceState
import com.google.android.accessibility.talkback.quicksettings.QuickControl
import com.google.android.accessibility.talkback.quicksettings.QuickControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceActionsChoiceTest {

  private val fontSizes =
    ChoiceState(
      Choice("100%", 100),
      listOf(85, 100, 115, 130, 150, 180, 200).map { Choice("$it%", it) },
    )
  private val free = 25..200

  private val timeouts =
    ChoiceState(
      Choice("30 seconds", 30_000),
      listOf(Choice("15 seconds", 15_000), Choice("30 seconds", 30_000), Choice("1 minute", 60_000)),
    )

  @Test
  fun aNumberInTheFreeRangeIsThatNumberAndNotPartOfALabel() {
    // "150%" contains "50", "50%" and "50.0" isn't an option.
    assertEquals(50, DeviceActions.choiceValue(fontSizes, "50", free))
    assertEquals(50, DeviceActions.choiceValue(fontSizes, "50%", free))
    assertEquals(50, DeviceActions.choiceValue(fontSizes, "50 %", free))
    assertEquals(50, DeviceActions.choiceValue(fontSizes, "50.0", free))
    assertEquals(30, DeviceActions.choiceValue(fontSizes, "30", free))
  }

  @Test
  fun aNumberIsRoundedAndOptionsStillMatch() {
    assertEquals(33, DeviceActions.choiceValue(fontSizes, "33.4", free))
    assertEquals(25, DeviceActions.choiceValue(fontSizes, "24.6", free))
    assertEquals(85, DeviceActions.choiceValue(fontSizes, "85", free))
    assertEquals(200, DeviceActions.choiceValue(fontSizes, "200%", free))
  }

  @Test
  fun aNumberOutsideTheFreeRangeIsRefused() {
    assertNull(DeviceActions.choiceValue(fontSizes, "24", free))
    assertNull(DeviceActions.choiceValue(fontSizes, "201", free))
    assertNull(DeviceActions.choiceValue(fontSizes, "-50", free))
    assertNull(DeviceActions.choiceValue(fontSizes, "NaN", free))
    assertNull(DeviceActions.choiceValue(fontSizes, "1e400", free))
  }

  @Test
  fun nextMovesToTheOptionAfterTheCurrentOneOrTheFirst() {
    assertEquals(115, DeviceActions.choiceValue(fontSizes, "next", free))
    val small = fontSizes.copy(current = Choice("50%", 50))
    assertEquals(85, DeviceActions.choiceValue(small, "Next", free))
  }

  @Test
  fun withoutAFreeRangeOnlyOptionsAreTaken() {
    assertEquals(60_000, DeviceActions.choiceValue(timeouts, "60000", null))
    assertEquals(60_000, DeviceActions.choiceValue(timeouts, "1 minute", null))
    assertEquals(15_000, DeviceActions.choiceValue(timeouts, "15", null))
    assertNull(DeviceActions.choiceValue(timeouts, "45000", null))
    assertNull(DeviceActions.choiceValue(timeouts, "hour", null))
  }

  @Test
  @Config(sdk = [34])
  fun fontSizeTakesTwentyFiveToTwoHundredFromAndroid14() {
    val controls = QuickControls(RuntimeEnvironment.getApplication())
    assertEquals(25..200, controls.freeChoiceValues(QuickControl.FONT_SIZE))
    assertNull(controls.freeChoiceValues(QuickControl.SCREEN_TIMEOUT))
  }

  @Test
  @Config(sdk = [33])
  fun fontSizeStopsAt130BeforeAndroid14() {
    val controls = QuickControls(RuntimeEnvironment.getApplication())
    assertEquals(25..130, controls.freeChoiceValues(QuickControl.FONT_SIZE))
  }
}
