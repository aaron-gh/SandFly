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

package com.google.android.accessibility.braille.translate.liblouis

import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.braille.translate.TranslationResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecondaryTableTranslatorTest {
  private val calls = mutableListOf<String>()

  /** Gives [cellsPerChar] cells of dots 1 for each character, or dots 2 for the cursor's. */
  private inner class Fake(private val name: String, private val cellsPerChar: Int) :
    BrailleTranslator {
    override fun translateToPrint(brailleWord: BrailleWord): String = name

    override fun translateToPrintPartial(brailleWord: BrailleWord): String = "$name partial"

    override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult {
      calls += "$name [$text] $cursorPosition"
      val cells = BrailleWord()
      val textToBraille = mutableListOf<Int>()
      val brailleToText = mutableListOf<Int>()
      for (i in text.indices) {
        textToBraille += cells.size()
        repeat(cellsPerChar) {
          cells.append(BrailleCharacter(if (i == cursorPosition) "2" else "1"))
          brailleToText += i
        }
      }
      return TranslationResult.builder()
        .setText(text)
        .setCells(cells)
        .setTextToBraillePositions(textToBraille)
        .setBrailleToTextPositions(brailleToText)
        .setCursorBytePosition(
          if (cursorPosition in text.indices) cursorPosition * cellsPerChar else -1
        )
        .build()
    }
  }

  /** The main table defines everything but Han characters, Cyrillic and Greek capitals. */
  private inner class Definitions : SecondaryTableTranslator.CharacterDefinitions {
    var generation = 0
    val checked = mutableListOf<String>()

    override fun defined(text: String): BooleanArray {
      checked += text
      return BooleanArray(text.length) {
        val c = text[it]
        Character.UnicodeScript.of(c.code) != Character.UnicodeScript.HAN &&
          Character.UnicodeScript.of(c.code) != Character.UnicodeScript.CYRILLIC &&
          !(Character.UnicodeScript.of(c.code) == Character.UnicodeScript.GREEK &&
            Character.isUpperCase(c))
      }
    }

    override fun generation(): Int = generation
  }

  private val main = Fake("main", 1)
  private val secondary = Fake("secondary", 2)
  private val definitions = Definitions()
  private var compileChecks = 0
  private var compiles = true

  private fun translator() =
    SecondaryTableTranslator(
      main,
      secondary,
      definitions,
      {
        compileChecks++
        compiles
      },
      "test",
    )

  @Test
  fun textTheMainTableDefinesIsTranslatedWhole() {
    val result = translator().translate("abc def", 2)
    assertEquals(listOf("main [abc def] 2"), calls)
    assertEquals(7, result.cells().size())
    assertEquals(0, compileChecks)
  }

  @Test
  fun undefinedCharactersAreTranslatedWithTheSecondaryTable() {
    val result = translator().translate("ab中文 cd", -1)
    assertEquals(listOf("main [ab] -1", "secondary [中文] -1", "main [ cd] -1"), calls)
    assertEquals(2 + 4 + 3, result.cells().size())
    assertEquals(listOf(0, 1, 2, 4, 6, 7, 8), result.textToBraillePositions())
    assertEquals(listOf(0, 1, 2, 2, 3, 3, 4, 5, 6), result.brailleToTextPositions())
    assertEquals(-1, result.cursorBytePosition())
  }

  @Test
  fun whitespaceBetweenUndefinedCharactersJoinsTheirRuns() {
    translator().translate("a中 文b", -1)
    assertEquals(listOf("main [a] -1", "secondary [中 文] -1", "main [b] -1"), calls)
  }

  @Test
  fun lettersOfTheSameScriptJoinTheRun() {
    // Only the capital is undefined, but the whole Greek word goes to the secondary table.
    translator().translate("x Καλη y", -1)
    assertEquals(listOf("main [x ] -1", "secondary [Καλη] -1", "main [ y] -1"), calls)
  }

  @Test
  fun lettersOfOtherScriptsStayWithTheMainTable() {
    translator().translate("Android系统", -1)
    assertEquals(listOf("main [Android] -1", "secondary [系统] -1"), calls)
  }

  @Test
  fun cursorIsMappedInItsRun() {
    val result = translator().translate("ab中c", 2)
    assertEquals(listOf("main [ab] -1", "secondary [中] 0", "main [c] -1"), calls)
    assertEquals(2, result.cursorBytePosition())
    assertEquals(BrailleCharacter("2"), result.cells().get(2))
    assertEquals(5, translator().translate("ab中c", 4).cursorBytePosition())
  }

  @Test
  fun cursorAfterTheMainTableRun() {
    assertEquals(3, translator().translate("中a", 2).cursorBytePosition())
    assertEquals(2, translator().translate("中a", 1).cursorBytePosition())
  }

  @Test
  fun brailleUnicodeStaysWithTheMainTable() {
    val translator =
      SecondaryTableTranslator(
        main,
        secondary,
        object : SecondaryTableTranslator.CharacterDefinitions {
          override fun defined(text: String) = BooleanArray(text.length)

          override fun generation() = 0
        },
        { true },
        "test",
      )
    assertNull(translator.secondaryCharacters("⠁ ⠃"))
  }

  @Test
  fun secondaryTableThatDoesNotCompileIsLeftOut() {
    compiles = false
    val translator = translator()
    translator.translate("ab中", -1)
    translator.translate("文", -1)
    assertEquals(listOf("main [ab中] -1", "main [文] -1"), calls)
    assertEquals(1, compileChecks)
  }

  @Test
  fun definitionsAreCheckedOncePerCharacter() {
    val translator = translator()
    translator.translate("abba中", -1)
    translator.translate("ab中c", -1)
    assertEquals(listOf("ab中", "c"), definitions.checked)
    definitions.generation++
    translator.translate("ab", -1)
    assertEquals(listOf("ab中", "c", "ab"), definitions.checked)
  }

  @Test
  fun unknownDefinitionsTranslateWithTheMainTable() {
    val translator =
      SecondaryTableTranslator(
        main,
        secondary,
        object : SecondaryTableTranslator.CharacterDefinitions {
          override fun defined(text: String): BooleanArray? = null

          override fun generation() = 0
        },
        { true },
        "test",
      )
    translator.translate("ab中", 1)
    assertEquals(listOf("main [ab中] 1"), calls)
  }

  @Test
  fun brailleIsReadWithTheMainTable() {
    assertEquals("main", translator().translateToPrint(BrailleWord()))
    assertEquals("main partial", translator().translateToPrintPartial(BrailleWord()))
  }

  @Test
  fun charactersCheckedAreBounded() {
    val translator = translator()
    val text = buildString {
      for (i in 0 until SecondaryTableTranslator.MAX_CACHED_CHARACTERS + 10) {
        append((0x4e00 + i).toChar())
      }
    }
    translator.translate(text, -1)
    translator.translate(text.substring(0, 10), -1)
    // The first characters were removed from the cache and are checked again.
    assertEquals(text.substring(0, 10), definitions.checked.last())
    assertArrayEquals(BooleanArray(10) { true }, translator.secondaryCharacters(text.substring(0, 10)))
  }
}
