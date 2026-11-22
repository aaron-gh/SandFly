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

package com.google.android.accessibility.talkback.keyboard

import com.google.android.accessibility.talkback.keyboard.DoublePressCharacters.Entry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a second press of a key types in place of the first. */
class DoublePressCharactersTest {
  private val entries = mapOf('g' to "ğ", 'o' to "ö", 'i' to "ı")

  private fun replace(typed: Char, before: String?, spacePeriod: Boolean = false) =
    DoublePressCharacters.replacement(typed, before, entries, spacePeriod, Locale.ROOT)

  @Test
  fun keyWithAnEntryReplacesItsCharacter() {
    assertEquals("ğ", replace('g', "ag"))
    assertEquals("ö", replace('o', "o"))
  }

  @Test
  fun capitalFirstPressGivesTheCapital() {
    assertEquals("Ğ", replace('G', "G"))
    assertEquals("Ö", replace('O', "xO"))
  }

  @Test
  fun capitalUsesTheLocale() {
    assertEquals("I", DoublePressCharacters.replacement('I', "I", entries, false, Locale.ROOT))
    assertEquals(
      "İ",
      DoublePressCharacters.replacement('A', "A", mapOf('a' to "i"), false, Locale.forLanguageTag("tr")),
    )
  }

  @Test
  fun keyWithoutAnEntryTypesAsUsual() {
    assertNull(replace('e', "e"))
  }

  @Test
  fun nothingWhenTheTypedCharacterIsntBeforeTheCursor() {
    assertNull(replace('g', "gx"))
    assertNull(replace('g', ""))
    assertNull(replace('g', null))
  }

  @Test
  fun doubleSpaceTypesAPeriodAfterAWord() {
    assertEquals(". ", replace(' ', "d ", spacePeriod = true))
    assertEquals(". ", replace(' ', "7 ", spacePeriod = true))
    assertNull(replace(' ', "d ", spacePeriod = false))
    assertNull(replace(' ', ". ", spacePeriod = true))
    assertNull(replace(' ', "  ", spacePeriod = true))
    assertNull(replace(' ', " ", spacePeriod = true))
  }

  @Test
  fun appliesOnlyToKeysThatCanBeReplaced() {
    assertTrue(DoublePressCharacters.applies('G', entries, false))
    assertFalse(DoublePressCharacters.applies('e', entries, true))
    assertTrue(DoublePressCharacters.applies(' ', entries, true))
    assertFalse(DoublePressCharacters.applies(' ', entries, false))
  }

  @Test
  fun entriesRoundTripAndBadLinesAreSkipped() {
    val list = listOf(Entry('g', "ğ"), Entry('e', "ä"))
    assertEquals(list, DoublePressCharacters.parse(DoublePressCharacters.format(list)))
    assertEquals(
      listOf(Entry('g', "ğ")),
      DoublePressCharacters.parse("G\tğ\nxx\ty\n \tz\nq\t\nr\t" + "a".repeat(17)),
    )
    assertEquals(emptyList<Entry>(), DoublePressCharacters.parse(null))
  }

  @Test
  fun aKeyIsOneCharacterOtherThanASpace() {
    assertEquals('g', DoublePressCharacters.normalizeKey("G"))
    assertNull(DoublePressCharacters.normalizeKey(" "))
    assertNull(DoublePressCharacters.normalizeKey("gg"))
    assertNull(DoublePressCharacters.normalizeKey(""))
  }
}
