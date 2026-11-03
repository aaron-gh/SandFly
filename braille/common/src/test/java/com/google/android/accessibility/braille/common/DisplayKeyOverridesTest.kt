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

package com.google.android.accessibility.braille.common

import com.google.android.accessibility.braille.common.DisplayKeyOverrides.NONE
import com.google.android.accessibility.braille.common.DisplayKeyOverrides.chordKey
import com.google.android.accessibility.braille.common.DisplayKeyOverrides.commandKey
import com.google.android.accessibility.braille.common.DisplayKeyOverrides.commandTarget
import com.google.android.accessibility.braille.common.DisplayKeyOverrides.screenReaderTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayKeyOverridesTest {
  private val back = 90
  private val home = 91

  @Test
  fun nothingSavedKeepsTheDefaults() {
    val overrides = DisplayKeyOverrides.parse(null)
    assertTrue(overrides.isEmpty)
    assertEquals(commandTarget(back), overrides.resolve(commandKey(back)))
    assertEquals(NONE, overrides.resolve(chordKey(0b111)))
    assertNull(overrides.get(commandKey(back)))
    assertEquals(listOf(commandKey(back)), overrides.keysFor(commandTarget(back)))
    assertFalse(overrides.isChanged(commandTarget(back)))
  }

  @Test
  fun assigningAChordMovesTheCommandOffItsOwnKey() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), chordKey(0b11))

    assertEquals(commandTarget(back), overrides.resolve(chordKey(0b11)))
    assertEquals(NONE, overrides.resolve(commandKey(back)))
    assertEquals(listOf(chordKey(0b11)), overrides.keysFor(commandTarget(back)))
    assertTrue(overrides.isChanged(commandTarget(back)))
  }

  @Test
  fun assigningAnotherCommandsKeyTakesItFromThatCommand() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), commandKey(home))

    assertEquals(commandTarget(back), overrides.resolve(commandKey(home)))
    assertEquals(NONE, overrides.resolve(commandKey(back)))
    assertEquals(emptyList<String>(), overrides.keysFor(commandTarget(home)))
  }

  @Test
  fun assigningTheOwnKeyBackRemovesTheOverride() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), chordKey(5))
    overrides.assign(commandTarget(back), commandKey(back))

    assertEquals(commandTarget(back), overrides.resolve(commandKey(back)))
    assertEquals(NONE, overrides.resolve(chordKey(5)))
    assertTrue(overrides.isEmpty)
  }

  @Test
  fun screenReaderCommandsHaveNoKeysUntilAssigned() {
    val overrides = DisplayKeyOverrides.parse(null)
    val menu = screenReaderTarget("KEYCOMBO:open_menu")
    assertEquals(emptyList<String>(), overrides.keysFor(menu))

    overrides.assign(menu, chordKey(0x3F))
    assertEquals(menu, overrides.resolve(chordKey(0x3F)))
    assertEquals(listOf(chordKey(0x3F)), overrides.keysFor(menu))

    overrides.reset(menu)
    assertTrue(overrides.isEmpty)
  }

  @Test
  fun resetGivesBackTheOwnKeyAndReleasesAssignedKeys() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), commandKey(home))
    overrides.assign(commandTarget(home), chordKey(9))

    overrides.reset(commandTarget(back))

    assertEquals(commandTarget(back), overrides.resolve(commandKey(back)))
    assertEquals(commandTarget(home), overrides.resolve(commandKey(home)))
    assertEquals(commandTarget(home), overrides.resolve(chordKey(9)))
  }

  @Test
  fun resetTakesTheOwnKeyBackFromTheCommandItWasMovedTo() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), chordKey(3))
    overrides.assign(commandTarget(home), commandKey(back))

    assertEquals(commandTarget(home), overrides.targetLosingKeyOnReset(commandTarget(back)))
    overrides.reset(commandTarget(back))

    assertEquals(commandTarget(back), overrides.resolve(commandKey(back)))
    assertEquals(NONE, overrides.resolve(chordKey(3)))
    assertEquals(emptyList<String>(), overrides.keysFor(commandTarget(home)))
  }

  @Test
  fun resetTakesNoKeyWhenTheOwnKeyRunsNothingOrItsCommand() {
    val overrides = DisplayKeyOverrides.parse(null)
    assertNull(overrides.targetLosingKeyOnReset(commandTarget(back)))

    overrides.assign(commandTarget(back), chordKey(3))
    assertNull(overrides.targetLosingKeyOnReset(commandTarget(back)))

    val menu = screenReaderTarget("KEYCOMBO:open_menu")
    overrides.assign(menu, commandKey(home))
    assertNull(overrides.targetLosingKeyOnReset(menu))
    assertEquals(menu, overrides.targetLosingKeyOnReset(commandTarget(home)))
  }

  @Test
  fun savedTextReadsBack() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(commandTarget(back), chordKey(0xFF))
    overrides.assign(screenReaderTarget("a;b=c%d:é"), commandKey(home))

    val read = DisplayKeyOverrides.parse(overrides.serialize())

    assertEquals(commandTarget(back), read.resolve(chordKey(0xFF)))
    assertEquals(NONE, read.resolve(commandKey(back)))
    assertEquals(screenReaderTarget("a;b=c%d:é"), read.resolve(commandKey(home)))
    assertEquals(overrides.serialize(), read.serialize())
  }

  @Test
  fun malformedEntriesAreSkipped() {
    val read =
      DisplayKeyOverrides.parse(
        "v1;c90=s3;s0=c91;s256=c91;c012=c91;c-1=c91;x=c91;=c91;s7;s8=%zz;s9=c91;s9=c92;" +
          "c91=a%3A;s10=c91"
      )

    // Only s9 (first value kept) and s10 are valid; the rest are skipped.
    assertEquals(commandTarget(91), read.resolve(chordKey(9)))
    assertEquals(commandTarget(91), read.resolve(chordKey(10)))
    assertEquals(commandTarget(90), read.resolve(commandKey(90)))
    assertEquals(commandTarget(91), read.resolve(commandKey(91)))
    assertNull(read.get(chordKey(8)))
  }

  @Test
  fun otherVersionsAndOversizedTextAreIgnored() {
    assertTrue(DisplayKeyOverrides.parse("v2;s9=c91").isEmpty)
    assertTrue(DisplayKeyOverrides.parse("s9=c91").isEmpty)
    assertTrue(DisplayKeyOverrides.parse("").isEmpty)
    assertTrue(
      DisplayKeyOverrides.parse("v1;s9=c91" + ";".repeat(DisplayKeyOverrides.MAX_LENGTH)).isEmpty
    )
  }

  @Test
  fun entriesAreBounded() {
    val text = StringBuilder("v1")
    for (command in 0 until DisplayKeyOverrides.MAX_ENTRIES + 50) {
      text.append(";c").append(command).append("=-")
    }
    val read = DisplayKeyOverrides.parse(text.toString())
    assertEquals(NONE, read.resolve(commandKey(DisplayKeyOverrides.MAX_ENTRIES - 1)))
    assertEquals(
      commandTarget(DisplayKeyOverrides.MAX_ENTRIES),
      read.resolve(commandKey(DisplayKeyOverrides.MAX_ENTRIES)),
    )
  }

  @Test
  fun overlongScreenReaderValuesAreRejected() {
    val overrides = DisplayKeyOverrides.parse(null)
    overrides.assign(
      screenReaderTarget("x".repeat(DisplayKeyOverrides.MAX_VALUE_LENGTH + 1)),
      chordKey(1),
    )
    assertTrue(overrides.isEmpty)
  }
}
