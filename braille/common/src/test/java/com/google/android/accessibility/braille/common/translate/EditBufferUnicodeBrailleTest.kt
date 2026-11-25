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

package com.google.android.accessibility.braille.common.translate

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.android.accessibility.braille.common.ImeConnection
import com.google.android.accessibility.braille.common.TalkBackSpeaker
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.braille.translate.TranslationResult
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Unicode braille tables type with the uncontracted edit buffer and a translator that gives
 * each cell its Unicode braille character, as unicode-braille.utb does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditBufferUnicodeBrailleTest {
  private val spoken = mutableListOf<String>()
  private val speaker = TalkBackSpeaker { text, _, _ -> spoken += text.toString() }
  private val committed = StringBuilder()
  private var composing = ""

  private val unicodeBraille =
    object : BrailleTranslator {
      override fun translateToPrint(brailleWord: BrailleWord): String =
        brailleWord.toList().joinToString("") { it.asUnicodeChar().toString() }

      override fun translateToPrintPartial(brailleWord: BrailleWord): String =
        translateToPrint(brailleWord)

      override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult? = null
    }

  private val buffer =
    BrailleLanguages.Code.UNICODE_BRAILLE.createEditBuffer(
      RuntimeEnvironment.getApplication(),
      speaker,
      unicodeBraille,
      /* contractedMode= */ false,
    )

  @Test
  fun cellsTypeTheirUnicodeBrailleCharacters() {
    buffer.appendBraille(connection(TEXT), BrailleCharacter("2345"))
    buffer.appendBraille(connection(TEXT), BrailleCharacter("16"))
    assertEquals("⠞⠡", composing)
  }

  @Test
  fun eightDotCellsTypeTheirUnicodeBrailleCharacters() {
    buffer.appendBraille(connection(TEXT), BrailleCharacter("12345678"))
    buffer.appendBraille(connection(TEXT), BrailleCharacter("7"))
    assertEquals("⣿⡀", composing)
  }

  @Test
  fun spaceTypesTheBlankBrailleCell() {
    buffer.appendBraille(connection(TEXT), BrailleCharacter("1"))
    buffer.appendSpace(connection(TEXT))
    assertEquals("\u2800", committed.toString())
  }

  @Test
  fun eightDotTableSpaceTypesTheBlankBrailleCell() {
    val eightDot =
      BrailleLanguages.Code.UNICODE_BRAILLE_8.createEditBuffer(
        RuntimeEnvironment.getApplication(),
        speaker,
        unicodeBraille,
        /* contractedMode= */ false,
      )
    eightDot.appendSpace(connection(TEXT))
    assertEquals("\u2800", committed.toString())
  }

  @Test
  fun otherTablesTypeASpace() {
    val english =
      BrailleLanguages.Code.EN_NABCC.createEditBuffer(
        RuntimeEnvironment.getApplication(),
        speaker,
        unicodeBraille,
        /* contractedMode= */ false,
      )
    english.appendSpace(connection(TEXT))
    assertEquals(" ", committed.toString())
  }

  @Test
  fun capitalAndNumberSignsAreCharactersOfTheirOwn() {
    buffer.appendBraille(connection(TEXT), BrailleCharacter("6"))
    buffer.appendBraille(connection(TEXT), BrailleCharacter("3456"))
    assertEquals("⠠⠼", composing)
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun numberFieldGetsNoNumberSign() {
    buffer.appendBraille(connection(NUMBER), BrailleCharacter("1"))
    assertEquals("⠁", committed.toString())
  }

  private fun connection(inputType: Int): ImeConnection {
    val editorInfo = EditorInfo()
    editorInfo.inputType = inputType
    val inputConnection =
      Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) {
        _,
        method,
        args ->
        when (method.name) {
          "commitText" -> {
            committed.append(args[0] as CharSequence)
            true
          }
          "setComposingText" -> {
            composing = args[0].toString()
            true
          }
          "getTextBeforeCursor" -> committed.toString()
          "getTextAfterCursor" -> ""
          else -> if (method.returnType == Boolean::class.javaPrimitiveType) true else null
        }
      } as InputConnection
    return ImeConnection(inputConnection, editorInfo, ImeConnection.AnnounceType.NORMAL)
  }

  private companion object {
    const val TEXT = InputType.TYPE_CLASS_TEXT
    const val NUMBER = InputType.TYPE_CLASS_NUMBER
  }
}
