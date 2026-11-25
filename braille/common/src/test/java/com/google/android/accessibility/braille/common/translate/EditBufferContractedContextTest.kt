/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.common.translate

import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import com.google.android.accessibility.braille.common.ImeConnection
import com.google.android.accessibility.braille.common.TalkBackSpeaker
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.braille.translate.GoogleBrailleTranslator
import com.google.android.accessibility.braille.translate.TranslationResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditBufferContractedContextTest {
  private val context = RuntimeEnvironment.getApplication()
  private val cells = mapOf(
    'a' to BrailleCharacter("1"),
    'b' to BrailleCharacter("12"),
    'c' to BrailleCharacter("14"),
    'k' to BrailleCharacter("13"),
    'l' to BrailleCharacter("123"),
    'n' to BrailleCharacter("1345"),
    'o' to BrailleCharacter("135"),
    'w' to BrailleCharacter("2456"),
    ' ' to BrailleCharacter.EMPTY_CELL,
  )
  private val translator = object : BrailleTranslator {
    override fun translateToPrint(word: BrailleWord): String =
      translateToPrint(word, "", "")

    override fun translateToPrint(word: BrailleWord, before: String, after: String): String {
      if (word.isEmpty) return ""
      val input = before + translateToPrintPartial(word) + after
      val translated = input.split(' ').joinToString(" ") { if (it == "k") "knowledge" else it }
      return translated.substring(before.length, translated.length - after.length)
    }

    override fun translateToPrintPartial(word: BrailleWord): String =
      (0 until word.size()).joinToString("") { index ->
        cells.entries.first { it.value == word.get(index) }.key.toString()
      }

    override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult? = null
  }
  private val buffer =
    EditBufferUeb2(
      context,
      GoogleBrailleTranslator(translator, null),
      TalkBackSpeaker { _, _, _ -> },
    )

  @Test
  fun correctsTheLastLetterOfACommittedWord() {
    val field = Field("bacl")
    buffer.deleteCharacterBackward(field.connection)
    type("k", field)
    buffer.appendSpace(field.connection)

    assertEquals("back ", field.contents.toString())
  }

  @Test
  fun correctsTheLastCellOfAWordStillInTheBuffer() {
    val field = Field("")
    type("bacl", field)
    buffer.deleteCharacterBackward(field.connection)
    type("k", field)
    buffer.commit(field.connection)

    assertEquals("back", field.contents.toString())
  }

  @Test
  fun usesTextToTheRightAndKeepsTheCursorAfterTheInsertion() {
    val field = Field("now", cursor = 0)
    type("k", field)
    buffer.commit(field.connection)

    assertEquals("know", field.contents.toString())
    assertEquals(1, Selection.getSelectionStart(field.contents))
  }

  @Test
  fun expandsAStandaloneCellBetweenSpaces() {
    val field = Field("bac now", cursor = 4)
    type("k", field)
    buffer.appendSpace(field.connection)

    assertEquals("bac knowledge now", field.contents.toString())
  }

  @Test
  fun aSpaceSplitsTheBufferedTextAtItsCursor() {
    val field = Field("bac")
    type("kk", field)
    buffer.moveHoldingsCursor(field.connection, 1)
    buffer.appendSpace(field.connection)

    assertEquals("back knowledge", field.contents.toString())
    assertEquals(5, Selection.getSelectionStart(field.contents))
  }

  @Test
  fun usesBothSidesWhenInsertingInsideAWord() {
    val field = Field("bacnow", cursor = 3)
    type("k", field)
    buffer.commit(field.connection)

    assertEquals("backnow", field.contents.toString())
  }

  private fun type(letters: String, field: Field) {
    for (letter in letters) {
      buffer.appendBraille(field.connection, cells.getValue(letter))
    }
  }

  private inner class Field(text: String, cursor: Int = text.length) {
    val contents = SpannableStringBuilder(text).also { Selection.setSelection(it, cursor) }
    private val input = object : BaseInputConnection(View(context), true) {
      override fun getEditable() = contents

      override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
          deleteSurroundingText(1, 0)
        }
        return true
      }

      override fun getExtractedText(request: ExtractedTextRequest, flags: Int) =
        ExtractedText().apply {
          this.text = contents
          selectionStart = Selection.getSelectionStart(contents)
          selectionEnd = Selection.getSelectionEnd(contents)
        }
    }
    val connection =
      ImeConnection(
        input,
        EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT },
        ImeConnection.AnnounceType.SILENCE,
      )
  }
}
