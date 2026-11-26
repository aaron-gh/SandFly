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

import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.words.WordLanguage
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * The SandFly keyboard's auto-correct and word suggestions in a text field, with the app's typing
 * done as Android's text fields do it: keys the keyboard leaves type into the field, and each
 * change of the cursor comes back as a selection update.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WordInputTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences
  private lateinit var field: Field
  private lateinit var input: WordInput
  private val said = mutableListOf<String>()

  /** A text field: its text, and the input connection the keyboard edits it through. */
  private class Field(context: Context) : BaseInputConnection(View(context), true) {
    val text: Editable = SpannableStringBuilder().also { Selection.setSelection(it, 0) }

    override fun getEditable(): Editable = text

    val cursor: Int
      get() = Selection.getSelectionStart(text)
  }

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit {
      putBoolean(context.getString(R.string.pref_sandfly_keyboard_autocorrect_key), true)
      putBoolean(context.getString(R.string.pref_sandfly_keyboard_suggestions_key), true)
      putString(
        context.getString(R.string.pref_sandfly_keyboard_word_language_key),
        WordLanguage.EN_US.code,
      )
    }
    field = Field(context)
    input = WordInput(context, prefs) { field }
    input.announce = { text, immediately -> said += if (immediately) "now: $text" else text }
    start(InputType.TYPE_CLASS_TEXT)
    waitForList()
  }

  private fun start(inputType: Int) {
    val info =
      EditorInfo().apply {
        this.inputType = inputType
        initialSelStart = field.cursor
        initialSelEnd = field.cursor
      }
    input.onStartInput(info, restarting = false)
  }

  /** Reads the word list, which happens on a thread of its own. */
  private fun waitForList() {
    val deadline = System.currentTimeMillis() + 20_000
    while (System.currentTimeMillis() < deadline) {
      ShadowLooper.idleMainLooper()
      if (WordData.suggester(context, WordLanguage.EN_US) != null) return
      Thread.sleep(20)
    }
    throw AssertionError("Word list not read")
  }

  private fun event(keyCode: Int, metaState: Int = 0): KeyEvent =
    KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0)

  /** Presses [keyCode]; where the keyboard leaves it, the app types it. Returns whether taken. */
  private fun press(keyCode: Int, metaState: Int = 0): Boolean {
    val before = field.cursor
    val event = event(keyCode, metaState)
    val taken = input.onKeyDown(keyCode, event, field)
    if (!taken) {
      when (keyCode) {
        KeyEvent.KEYCODE_DEL -> field.deleteSurroundingText(1, 0)
        KeyEvent.KEYCODE_DPAD_LEFT -> Selection.setSelection(field.text, maxOf(0, before - 1))
        else -> {
          val c = event.unicodeChar
          if (c != 0) field.commitText(c.toChar().toString(), 1)
        }
      }
    }
    update()
    return taken
  }

  /** Sends a selection update, as fields do after each change and some after none. */
  private fun update() {
    input.onUpdateSelection(field.cursor, field.cursor, -1)
  }

  private fun type(text: String) {
    for (c in text) {
      val keyCode =
        when (c) {
          ' ' -> KeyEvent.KEYCODE_SPACE
          '.' -> KeyEvent.KEYCODE_PERIOD
          ',' -> KeyEvent.KEYCODE_COMMA
          in 'a'..'z' -> KeyEvent.KEYCODE_A + (c - 'a')
          else -> throw IllegalArgumentException("$c")
        }
      press(keyCode)
    }
  }

  @Test
  fun spaceCorrectsMisspelledWord() {
    type("i recieve ")
    assertEquals("I receive ", field.text.toString())
  }

  @Test
  fun punctuationCorrectsAndIsTyped() {
    type("teh.")
    assertEquals("the.", field.text.toString())
  }

  @Test
  fun deleteUndoesCorrectionAndWordIsKept() {
    type("thier ")
    assertEquals("their ", field.text.toString())
    assertTrue(press(KeyEvent.KEYCODE_DEL))
    assertEquals("thier", field.text.toString())
    type(" thier ")
    assertEquals("thier thier ", field.text.toString())
  }

  @Test
  fun wordsAsTypedStay() {
    type("from the form ")
    assertEquals("from the form ", field.text.toString())
  }

  @Test
  fun downGoesThroughSuggestionsAndEnterTypesOne() {
    type("keyb")
    assertTrue(input.takesKey(KeyEvent.KEYCODE_DPAD_DOWN))
    assertTrue(press(KeyEvent.KEYCODE_DPAD_DOWN))
    assertTrue(input.takesKey(KeyEvent.KEYCODE_ENTER))
    assertTrue(press(KeyEvent.KEYCODE_ENTER))
    assertEquals("keyboard ", field.text.toString())
    // After the space, Up and Down are the app's again.
    assertFalse(input.takesKey(KeyEvent.KEYCODE_DPAD_DOWN))
    assertFalse(press(KeyEvent.KEYCODE_DPAD_DOWN))
  }

  @Test
  fun commaTypesTheSuggestionChosen() {
    type("keyb")
    press(KeyEvent.KEYCODE_DPAD_DOWN)
    press(KeyEvent.KEYCODE_COMMA)
    assertEquals("keyboard,", field.text.toString())
  }

  @Test
  fun aLetterLeavesTheListAndTypes() {
    type("keyb")
    press(KeyEvent.KEYCODE_DPAD_DOWN)
    assertFalse(press(KeyEvent.KEYCODE_O))
    assertEquals("keybo", field.text.toString())
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
  }

  @Test
  fun backClosesTheList() {
    type("keyb")
    press(KeyEvent.KEYCODE_DPAD_DOWN)
    assertTrue(press(KeyEvent.KEYCODE_BACK))
    assertEquals("keyb", field.text.toString())
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
  }

  @Test
  fun movingTheCursorEndsTheWord() {
    type("keyb")
    press(KeyEvent.KEYCODE_DPAD_LEFT)
    assertFalse(input.takesKey(KeyEvent.KEYCODE_DPAD_DOWN))
    assertFalse(input.hasWord())
  }

  @Test
  fun commandsStepAndType() {
    type("keyb")
    val said = input.step(next = true, field, editing = true)
    assertTrue(said, said!!.startsWith("keyboard"))
    assertTrue(input.typeSuggestion(field, editing = true))
    assertEquals("keyboard ", field.text.toString())
  }

  @Test
  fun nothingInPasswordOrEmailFields() {
    start(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
    type("teh ")
    assertEquals("teh ", field.text.toString())
    start(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
    type("teh ")
    assertEquals("teh teh ", field.text.toString())
  }

  @Test
  fun pauseSaysTheCorrectionSpaceWouldType() {
    type("recieve")
    assertTrue(said.isEmpty())
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    assertEquals(listOf("receive, correction"), said)
  }

  @Test
  fun pauseSaysTheFirstSuggestion() {
    // "keyb" alone would be corrected to "key" by Space, which the pause says instead.
    type("keyboa")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    assertEquals(listOf("keyboard"), said)
  }

  @Test
  fun typingOnBeforeThePauseSaysNothing() {
    type("keyb")
    ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
    type("o")
    ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
    assertTrue(said.toString(), said.isEmpty())
    type(" ")
    ShadowLooper.idleMainLooper(2_000, TimeUnit.MILLISECONDS)
    assertTrue(said.toString(), said.isEmpty())
  }

  @Test
  fun immediatelyAfterEachKey() {
    prefs.edit {
      putString(
        context.getString(R.string.pref_sandfly_keyboard_suggestion_pause_key),
        "immediately",
      )
    }
    type("rec")
    ShadowLooper.idleMainLooper()
    assertTrue(said.toString(), said.isNotEmpty() && said.all { it.startsWith("now: ") })
    said.clear()
    type("ieve")
    ShadowLooper.idleMainLooper()
    assertEquals("now: receive, correction", said.last())
    said.clear()
    type(" ")
    ShadowLooper.idleMainLooper()
    assertTrue(said.toString(), said.isEmpty())
  }

  private fun enterTypesSuggestion(on: Boolean) {
    prefs.edit {
      putBoolean(context.getString(R.string.pref_sandfly_keyboard_enter_types_suggestion_key), on)
    }
  }

  @Test
  fun enterTypesTheSuggestionSaid() {
    enterTypesSuggestion(true)
    type("keyboa")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    assertEquals(listOf("keyboard"), said)
    assertTrue(input.takesKey(KeyEvent.KEYCODE_ENTER))
    assertTrue(press(KeyEvent.KEYCODE_ENTER))
    assertEquals("keyboard ", field.text.toString())
    // The next Enter is the app's.
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
    assertFalse(press(KeyEvent.KEYCODE_ENTER))
  }

  @Test
  fun enterTypesTheCorrectionSaid() {
    enterTypesSuggestion(true)
    type("recieve")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    assertTrue(press(KeyEvent.KEYCODE_ENTER))
    assertEquals("receive ", field.text.toString())
  }

  @Test
  fun enterIsTheAppsBeforeASuggestionIsSaidOrAfterTypingOn() {
    enterTypesSuggestion(true)
    type("keyboa")
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    type("r")
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
  }

  @Test
  fun enterTypesTheSuggestionOffByDefault() {
    type("keyboa")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    assertFalse(input.takesKey(KeyEvent.KEYCODE_ENTER))
    assertFalse(press(KeyEvent.KEYCODE_ENTER))
  }

  @Test
  fun downAfterTheSuggestionSaidGoesOnToTheNext() {
    enterTypesSuggestion(true)
    type("keyb")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    // "key, correction" was said; Down goes to the suggestion after it.
    val first = said.single().substringBefore(",")
    val step = input.step(next = true, field, editing = true)!!
    assertTrue(step, step.endsWith("2 of " + step.substringAfterLast(" of ")))
    assertFalse(step, step.startsWith(first + ","))
  }

  @Test
  fun withEnterNotTypingItDownStartsAtTheFirstAndUpAtTheLast() {
    type("keyb")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    val spoken = said.single().substringBefore(",")
    val down = input.step(next = true, field, editing = true)!!
    assertTrue(down, down.startsWith("$spoken, 1 of "))
    press(KeyEvent.KEYCODE_BACK)
    type("o")
    ShadowLooper.idleMainLooper(1_000, TimeUnit.MILLISECONDS)
    val up = input.step(next = false, field, editing = true)!!
    val size = up.substringAfterLast(" of ")
    assertTrue(up, up.endsWith(", $size of $size"))
  }

  @Test
  fun aTrackpadSwipeMovesOnce() {
    type("keyb")
    val down = { time: Long ->
      KeyEvent(
        time,
        time,
        KeyEvent.ACTION_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
      )
    }
    assertTrue(input.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, down(1_000), field))
    assertTrue(input.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, down(1_068), field))
    assertTrue(input.onKeyDown(KeyEvent.KEYCODE_ENTER, event(KeyEvent.KEYCODE_ENTER), field))
    // The first suggestion, not the second.
    val first = WordData.suggester(context, WordLanguage.EN_US)!!.suggestions("keyb").first()
    assertEquals("$first ", field.text.toString())
  }

  @Test
  fun pauseOff() {
    prefs.edit {
      putString(context.getString(R.string.pref_sandfly_keyboard_suggestion_pause_key), "0")
    }
    type("recieve")
    ShadowLooper.idleMainLooper(2_000, TimeUnit.MILLISECONDS)
    assertTrue(said.isEmpty())
  }

  @Test
  fun offByDefault() {
    prefs.edit {
      remove(context.getString(R.string.pref_sandfly_keyboard_autocorrect_key))
      remove(context.getString(R.string.pref_sandfly_keyboard_suggestions_key))
    }
    type("teh keyb")
    assertFalse(input.takesKey(KeyEvent.KEYCODE_DPAD_DOWN))
    type(" ")
    assertEquals("teh keyb ", field.text.toString())
  }
}
