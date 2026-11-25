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

package com.google.android.accessibility.utils.output

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Report #592: U+2800, the space of Unicode braille text, was spoken as "braille dots". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleBlankCellSpeechTest {
  private fun spoken(text: String): String =
    SpeechControllerImpl.replaceBrailleSymbolByDescription(
        RuntimeEnvironment.getApplication(),
        text,
      )
      .toString()

  @Test
  fun blankCell_isNamed() {
    assertEquals("blank braille cell ", spoken("⠀"))
  }

  @Test
  fun blankCellBetweenCells_isNamed() {
    assertEquals("braille dot 1 blank braille cell braille dots 12 ", spoken("⠁⠀⠃"))
  }
}
