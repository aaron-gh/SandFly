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
import android.view.KeyEvent
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditBufferUnContractedNumberFieldTest {
  private val spoken = mutableListOf<String>()
  private val speaker = TalkBackSpeaker { text, _, _ -> spoken += text.toString() }

  @Test
  fun numberSignMakesALetterADigit() {
    assertTrue(EditBufferUnContracted.numberSignMakesDigit(UEB, NUMBER_SIGN, cell("1")))
  }

  @Test
  fun noNumberSignBeforeCellsThatAreNoDigitAfterIt() {
    assertFalse(EditBufferUnContracted.numberSignMakesDigit(UEB, NUMBER_SIGN, cell("36")))
    assertFalse(EditBufferUnContracted.numberSignMakesDigit(UEB, NUMBER_SIGN, NUMBER_SIGN))
  }

  @Test
  fun noNumberSignWhereDigitsNeedNone() {
    val digitsWithoutSign = translator { word ->
      if (word == BrailleWord(cell("1"))) "1" else UEB.translateToPrint(word)
    }
    assertFalse(
      EditBufferUnContracted.numberSignMakesDigit(digitsWithoutSign, NUMBER_SIGN, cell("1"))
    )
  }

  @Test
  fun noNumberSignWhereTheCellIsACharacterOfItsOwn() {
    val signIsPrint = translator { word -> word.toList().joinToString("") { "#" } }
    assertFalse(EditBufferUnContracted.numberSignMakesDigit(signIsPrint, NUMBER_SIGN, cell("1")))
  }

  @Test
  fun numberFieldTypesDigitsWithoutNumberSign() {
    val field = Field()
    val buffer = buffer()
    buffer.appendBraille(field.connection(NUMBER), cell("1"))
    buffer.appendBraille(field.connection(NUMBER), cell("12"))
    assertEquals("12", field.text.toString())
  }

  @Test
  fun numberFieldKeepsAMinusSignAtTheStart() {
    val field = Field()
    buffer().appendBraille(field.connection(NUMBER), cell("36"))
    assertEquals("-", field.text.toString())
  }

  @Test
  fun textFieldGetsNoNumberSign() {
    val field = Field()
    buffer().appendBraille(field.connection(TEXT), cell("1"))
    assertEquals("a", field.composing)
  }

  @Test
  fun numberSignTypedByHandIsDeletedOnItsOwn() {
    val field = Field()
    val buffer = buffer()
    buffer.appendBraille(field.connection(NUMBER), cell("1"))
    buffer.appendBraille(field.connection(NUMBER), cell("12"))
    buffer.appendBraille(field.connection(NUMBER), NUMBER_SIGN)
    buffer.deleteCharacterBackward(field.connection(NUMBER))
    assertEquals("12", field.text.toString())
    buffer.appendBraille(field.connection(NUMBER), cell("14"))
    assertEquals("123", field.text.toString())
  }

  @Test
  fun automaticNumberSignGoesWithTheLastDigit() {
    val field = Field()
    val buffer = buffer()
    field.text.append("9")
    buffer.appendBraille(field.connection(NUMBER), cell("1"))
    buffer.deleteCharacterBackward(field.connection(NUMBER))
    assertEquals("9", field.text.toString())
    buffer.deleteCharacterBackward(field.connection(NUMBER))
    assertEquals("", field.text.toString())
  }

  @Test
  fun fieldThatHidesItsTextGetsEcho() {
    val field = Field(hidesText = true)
    val buffer = buffer()
    buffer.appendBraille(field.connection(NUMBER), cell("1"))
    assertEquals(listOf("1"), spoken)
    buffer.deleteCharacterBackward(field.connection(NUMBER))
    assertEquals(listOf("1", "1 deleted"), spoken)
  }

  @Test
  fun fieldThatHidesItsTextGetsNoEchoWhenCharacterEchoIsOff() {
    val field = Field(hidesText = true)
    buffer().appendBraille(field.connection(NUMBER, ImeConnection.AnnounceType.SILENCE), cell("1"))
    assertEquals(emptyList<String>(), spoken)
  }

  @Test
  fun ordinaryNumberFieldGetsNoEchoFromTheKeyboard() {
    val field = Field()
    buffer().appendBraille(field.connection(NUMBER), cell("1"))
    assertEquals(emptyList<String>(), spoken)
  }

  private fun buffer(): EditBufferUnContracted =
    object : EditBufferUnContracted(RuntimeEnvironment.getApplication(), UEB, speaker) {}

  /** A field that takes committed text and the delete key, and may return none of its text. */
  private class Field(private val hidesText: Boolean = false) {
    val text = StringBuilder()
    var composing = ""

    fun connection(
      inputType: Int,
      announceType: ImeConnection.AnnounceType = ImeConnection.AnnounceType.NORMAL,
    ): ImeConnection {
      val editorInfo = EditorInfo()
      editorInfo.inputType = inputType
      editorInfo.packageName = "com.example.bank"
      val inputConnection =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) {
          _,
          method,
          args ->
          when (method.name) {
            "commitText" -> {
              text.append(args[0] as CharSequence)
              true
            }
            "setComposingText" -> {
              composing = args[0].toString()
              true
            }
            "sendKeyEvent" -> {
              val event = args[0] as KeyEvent
              if (
                event.action == KeyEvent.ACTION_DOWN &&
                  event.keyCode == KeyEvent.KEYCODE_DEL &&
                  text.isNotEmpty()
              ) {
                text.setLength(text.length - 1)
              }
              true
            }
            "getTextBeforeCursor" -> if (hidesText) "" else text.toString()
            "getTextAfterCursor" -> ""
            else -> if (method.returnType == Boolean::class.javaPrimitiveType) true else null
          }
        } as InputConnection
      return ImeConnection(inputConnection, editorInfo, announceType)
    }
  }

  private companion object {
    val NUMBER_SIGN = cell("3456")
    const val NUMBER = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
    const val TEXT = InputType.TYPE_CLASS_TEXT

    val LETTERS =
      mapOf(
        "1" to 'a',
        "12" to 'b',
        "14" to 'c',
        "145" to 'd',
        "15" to 'e',
        "124" to 'f',
        "1245" to 'g',
        "125" to 'h',
        "24" to 'i',
        "245" to 'j',
      )

    fun translator(toPrint: (BrailleWord) -> String): BrailleTranslator =
      object : BrailleTranslator {
        override fun translateToPrint(brailleWord: BrailleWord): String = toPrint(brailleWord)

        override fun translateToPrintPartial(brailleWord: BrailleWord): String =
          toPrint(brailleWord)

        override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult? = null
      }

    /** Letters a to j, which are the digits 1 to 0 after a number sign, and a minus sign. */
    val UEB = translator { brailleWord ->
      val out = StringBuilder()
      var numeric = false
      for (character in brailleWord.toList()) {
        val dots = character.toString()
        val letter = LETTERS[dots]
        when {
          character == NUMBER_SIGN -> numeric = true
          letter != null && numeric -> out.append(if (letter == 'j') '0' else letter - 'a' + 1)
          letter != null -> out.append(letter)
          dots == "36" -> {
            numeric = false
            out.append('-')
          }
          else -> numeric = false
        }
      }
      out.toString()
    }

    fun cell(dots: String) = BrailleCharacter(dots)
  }
}
