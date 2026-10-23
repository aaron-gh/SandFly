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

package com.google.android.accessibility.talkback.selector

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingControlLettersTest {
  @Test
  fun defaultsAreUniqueAndNamedInTheReport() {
    val letters = ReadingControlLetters.DEFAULTS.values
    assertEquals(letters.size, letters.toSet().size)
    val prefs = FakeSharedPreferences()
    assertEquals('L', ReadingControlLetters.letterOf(prefs, Setting.GRANULARITY_LINES))
    assertEquals('K', ReadingControlLetters.letterOf(prefs, Setting.GRANULARITY_LINKS))
    assertEquals('A', ReadingControlLetters.letterOf(prefs, Setting.GRANULARITY_WEB_LANDMARKS))
    assertNull(ReadingControlLetters.letterOf(prefs, Setting.ACTIONS))
  }

  @Test
  fun lettersCanBeChangedClearedAndReset() {
    val prefs = FakeSharedPreferences()
    ReadingControlLetters.setLetter(prefs, Setting.ACTIONS, 'q')
    assertEquals('Q', ReadingControlLetters.letterOf(prefs, Setting.ACTIONS))
    ReadingControlLetters.setLetter(prefs, Setting.GRANULARITY_LINES, null)
    assertNull(ReadingControlLetters.letterOf(prefs, Setting.GRANULARITY_LINES))
    ReadingControlLetters.setLetter(prefs, Setting.GRANULARITY_LINES, 'L')
    assertEquals('L', ReadingControlLetters.letterOf(prefs, Setting.GRANULARITY_LINES))
    assertFalse(prefs.contains(ReadingControlLetters.prefKey(Setting.GRANULARITY_LINES)))
  }
}
