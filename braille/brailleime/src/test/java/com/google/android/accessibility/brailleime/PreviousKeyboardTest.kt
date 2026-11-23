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

package com.google.android.accessibility.brailleime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousKeyboardTest {
  private val pkg = "io.github.aaron_gh.sandfly"
  private val cls = "com.google.android.accessibility.brailleime.BrailleIme"

  @Test
  fun returnsOnlyWithTheSettingOnAndNoDisplay() {
    assertTrue(PreviousKeyboard.shouldReturn(true, false, true, true))
    assertFalse(PreviousKeyboard.shouldReturn(false, false, true, true))
    assertFalse(PreviousKeyboard.shouldReturn(true, true, true, true))
  }

  @Test
  fun doesNotReturnWithoutAnotherKeyboard() {
    assertFalse(PreviousKeyboard.shouldReturn(true, false, false, true))
  }

  @Test
  fun doesNotReturnOnceAnotherKeyboardIsCurrent() {
    assertFalse(PreviousKeyboard.shouldReturn(true, false, true, false))
  }

  @Test
  fun fullAndShortClassNamesAreTheSameKeyboard() {
    assertTrue(PreviousKeyboard.isCurrent("$pkg/$cls", pkg, cls))
    assertTrue(PreviousKeyboard.isCurrent("$pkg/.Ime", pkg, "$pkg.Ime"))
  }

  @Test
  fun anotherKeyboardIsNotCurrent() {
    assertFalse(
      PreviousKeyboard.isCurrent(
        "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
        pkg,
        cls,
      )
    )
    assertFalse(PreviousKeyboard.isCurrent(null, pkg, cls))
    assertFalse(PreviousKeyboard.isCurrent("", pkg, cls))
  }
}
