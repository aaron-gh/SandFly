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

/** Custom characters that change the text of the cells before them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditBufferUnContractedComposingTest {
  private val speaker = TalkBackSpeaker { _, _, _ -> }
  private val buffer =
    object : EditBufferUnContracted(RuntimeEnvironment.getApplication(), POLISH, speaker) {}

  @Test
  fun customCharacterReplacesTheTextOfItsFirstCell() {
    val field = Field()
    buffer.appendBraille(field.connection(TEXT), cell("6"))
    assertEquals("_", field.text.toString())
    buffer.appendBraille(field.connection(TEXT), cell("2356"))
    assertEquals(")", field.text.toString())
    buffer.appendSpace(field.connection(TEXT))
    assertEquals(") ", field.text.toString())
  }

  @Test
  fun customCharacterReplacesTheTextOfItsFirstCellAfterAWord() {
    val field = Field()
    buffer.appendBraille(field.connection(TEXT), cell("1"))
    buffer.appendBraille(field.connection(TEXT), cell("6"))
    buffer.appendBraille(field.connection(TEXT), cell("2356"))
    assertEquals("a)", field.text.toString())
    buffer.appendBraille(field.connection(TEXT), cell("12"))
    assertEquals("a)b", field.text.toString())
  }

  @Test
  fun customCharacterReplacesCommittedTextInAPasswordField() {
    val field = Field()
    buffer.appendBraille(field.connection(PASSWORD), cell("1"))
    buffer.appendBraille(field.connection(PASSWORD), cell("6"))
    assertEquals("a_", field.text.toString())
    buffer.appendBraille(field.connection(PASSWORD), cell("2356"))
    assertEquals("a)", field.text.toString())
    assertEquals(-1, field.composingStart)
  }

  @Test
  fun commonPrefixKeepsSurrogatePairs() {
    assertEquals(2, EditBufferUnContracted.commonPrefixLength("ab", "abc"))
    assertEquals(0, EditBufferUnContracted.commonPrefixLength("_", ")"))
    assertEquals(1, EditBufferUnContracted.commonPrefixLength("a😀", "a😁"))
  }

  /** A field with a cursor and composing text, like an EditText. */
  private class Field {
    val text = StringBuilder()
    var cursor = 0
    var composingStart = -1
    var composingEnd = -1

    private fun replace(newText: CharSequence, composing: Boolean) {
      val start = if (composingStart >= 0) composingStart else cursor
      val end = if (composingStart >= 0) composingEnd else cursor
      text.replace(start, end, newText.toString())
      cursor = start + newText.length
      if (composing) {
        composingStart = start
        composingEnd = cursor
      } else {
        composingStart = -1
        composingEnd = -1
      }
    }

    fun connection(inputType: Int): ImeConnection {
      val editorInfo = EditorInfo()
      editorInfo.inputType = inputType
      val inputConnection =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) {
          _,
          method,
          args ->
          when (method.name) {
            "commitText" -> {
              replace(args[0] as CharSequence, composing = false)
              true
            }
            "setComposingText" -> {
              replace(args[0] as CharSequence, composing = true)
              true
            }
            "finishComposingText" -> {
              composingStart = -1
              composingEnd = -1
              true
            }
            "deleteSurroundingText" -> {
              val before = minOf(args[0] as Int, cursor)
              text.delete(cursor - before, cursor)
              cursor -= before
              true
            }
            "getTextBeforeCursor" -> text.substring(0, cursor)
            "getTextAfterCursor" -> text.substring(cursor)
            else -> if (method.returnType == Boolean::class.javaPrimitiveType) true else null
          }
        } as InputConnection
      return ImeConnection(inputConnection, editorInfo, ImeConnection.AnnounceType.NORMAL)
    }
  }

  private companion object {
    const val TEXT = InputType.TYPE_CLASS_TEXT
    const val PASSWORD = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    val LETTERS = mapOf("1" to 'a', "12" to 'b', "14" to 'c')

    /**
     * Letters a to c, dot 6 as a low line, and dots 6-2356 as a custom right parenthesis, as in the
     * Polish table with the custom character of the report.
     */
    val POLISH: BrailleTranslator =
      object : BrailleTranslator {
        override fun translateToPrint(brailleWord: BrailleWord): String {
          val cells = brailleWord.toList().map { it.toString() }
          val out = StringBuilder()
          var i = 0
          while (i < cells.size) {
            val dots = cells[i]
            when {
              dots == "6" && cells.getOrNull(i + 1) == "2356" -> {
                out.append(')')
                i++
              }
              dots == "6" -> out.append('_')
              dots == "2356" -> out.append('(')
              else -> LETTERS[dots]?.let { out.append(it) }
            }
            i++
          }
          return out.toString()
        }

        override fun translateToPrintPartial(brailleWord: BrailleWord): String =
          translateToPrint(brailleWord)

        override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult? = null
      }

    fun cell(dots: String) = BrailleCharacter(dots)
  }
}
