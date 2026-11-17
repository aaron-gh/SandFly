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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Steps and tests for an app's controls, such as a video player's keys. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MacroControlStepsTest {
  @Test
  fun actOnItemAndFocusInside_areKeptAndDescribed() {
    val inPlayer = Macros.Condition(listOf(Macros.Clause(Macros.FocusInside("watch_player", field = Macros.Field.RESOURCE_ID))))
    val steps =
      listOf(
        Macros.If(inPlayer),
        Macros.ActOnItem("SeekBar", Macros.ItemAct.SCROLL_FORWARD, times = 4, regex = false),
        Macros.Else,
        Macros.ActOnItem("Send", Macros.ItemAct.LONG_PRESS),
        Macros.EndIf,
      )
    val macro = Macros.Macro("m", "Seek", steps)
    val read = Macros.macroFromJson(Macros.macroToJson(macro))!!
    assertEquals(steps, read.steps)
    val context = RuntimeEnvironment.getApplication()
    assertEquals("Scroll forward: SeekBar, 4 times", Macros.describe(context, steps[1]).toString())
    assertEquals("Long-press: Send", Macros.describe(context, steps[3]).toString())
  }
}
