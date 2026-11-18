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

package com.google.android.accessibility.talkback.braille

import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters
import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters.Entry
import com.google.android.accessibility.braille.translate.liblouis.CustomCharacterTables
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomBrailleCharactersTest {

  @Test
  fun cellsAreWrittenWithDotsInOrderAndHyphens() {
    assertEquals("2356", CustomBrailleCharacters.normalizeCells("2356"))
    assertEquals("2356", CustomBrailleCharacters.normalizeCells("6532"))
    assertEquals("456-2356", CustomBrailleCharacters.normalizeCells(" 456-2356 "))
    assertEquals("456-2356", CustomBrailleCharacters.normalizeCells("456 2356"))
    assertEquals("456-2356-78", CustomBrailleCharacters.normalizeCells("456/2356 - 87"))
  }

  @Test
  fun cellsThatAreNotDotsAreRefused() {
    for (cells in listOf("", " ", "-", "2356-", "9", "0", "11", "123456789", "a", "1,2")) {
      assertNull(cells, CustomBrailleCharacters.normalizeCells(cells))
    }
    val maxCells = List(CustomBrailleCharacters.MAX_CELLS) { "1" }
    assertEquals(maxCells.joinToString("-"), CustomBrailleCharacters.normalizeCells(maxCells.joinToString("-")))
    assertNull(CustomBrailleCharacters.normalizeCells((maxCells + "1").joinToString("-")))
  }

  @Test
  fun entriesNeedTextAndATable() {
    assertEquals(Entry(")", "2356", "POLISH"), CustomBrailleCharacters.entry(")", "2356", "POLISH"))
    assertNull(CustomBrailleCharacters.entry("", "2356", "POLISH"))
    assertNull(CustomBrailleCharacters.entry("a\nb", "2356", "POLISH"))
    assertNull(CustomBrailleCharacters.entry("x".repeat(CustomBrailleCharacters.MAX_TEXT + 1), "2356", "POLISH"))
    assertNull(CustomBrailleCharacters.entry(")", "2356", ""))
    assertNull(CustomBrailleCharacters.entry(")", "2356", "pl.tbl"))
    assertNull(CustomBrailleCharacters.entry(")", "2356", "../POLISH"))
  }

  @Test
  fun jsonKeepsUsableEntriesOnly() {
    val entries = listOf(Entry(")", "56-2356", "POLISH"), Entry("itd.", "1256", "POLISH"))
    assertEquals(entries, CustomBrailleCharacters.fromJson(CustomBrailleCharacters.toJson(entries)))
    assertEquals(
      listOf(Entry(")", "2356", "POLISH")),
      CustomBrailleCharacters.fromJson(
        """[{"text":")","cells":"6532","table":"POLISH"},{"text":"x","cells":"9","table":"POLISH"},
           {"text":"y","cells":"1"},5]"""
      ),
    )
    assertEquals(emptyList<Entry>(), CustomBrailleCharacters.fromJson("not json"))
    assertEquals(emptyList<Entry>(), CustomBrailleCharacters.fromJson(null))
  }

  @Test
  fun rulesDefineSingleSymbolsAndEscapeAllText() {
    val rules =
      CustomCharacterTables.rules(
        listOf(
          Entry(")", "56-2356", "POLISH"),
          Entry("=", "3-2356", "POLISH"),
          Entry("€", "4-15", "POLISH"),
          Entry("a", "1-1", "POLISH"),
          Entry("i d#", "24-145", "POLISH"),
        )
      )
    assertEquals(
      """
      # SandFly custom braille characters
      punctuation \x0029 56-2356
      always \x0029 56-2356
      math \x003d 3-2356
      always \x003d 3-2356
      sign \x20ac 4-15
      always \x20ac 4-15
      always \x0061 1-1
      always \x0069\x0020\x0064\x0023 24-145

      """
        .trimIndent(),
      rules,
    )
  }
}
