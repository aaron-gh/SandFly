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
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.PasswordSpeech.Change
import com.google.android.accessibility.talkback.keyboard.PasswordSpeech.Field
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PasswordSpeechTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    SharedPreferencesUtils.getSharedPreferences(context).edit { clear() }
  }

  private fun at(text: String, cursor: Int = text.length) = Field(text, cursor, cursor)

  private fun said(before: Field, after: Field, sayCapital: Boolean = true): String? =
    PasswordSpeech.change(before, after)?.let {
      PasswordSpeech.describe(context, it, sayCapital).text.toString()
    }

  private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

  @Test
  fun typedCharacterIsNamedAsTypingNamesIt() {
    assertEquals("capital P", said(at(""), at("P")))
    assertEquals("a", said(at("P"), at("Pa")))
    assertEquals(string(R.string.symbol_at_sign), said(at("Pa"), at("Pa@")))
    assertEquals("1", said(at("Pa@"), at("Pa@1")))
    assertEquals(string(R.string.symbol_space), said(at("a"), at("a ")))
    // A character typed between others, and a letter typed again.
    assertEquals("s", said(at("pas", 3), at("pass", 4)))
    assertEquals("x", said(at("ab", 1), at("axb", 2)))
  }

  @Test
  fun capitalFollowsTheCapitalLettersSetting() {
    // With pitch or sound for capitals, the letter alone, so that speech changes its pitch.
    assertEquals("P", said(at(""), at("P"), sayCapital = false))
  }

  @Test
  fun deletedCharacterIsNamedAsDeletedText() {
    val change = PasswordSpeech.change(at("Pa@"), at("Pa"))
    assertTrue(change is Change.Deleted)
    val said = PasswordSpeech.describe(context, change!!, sayCapital = true)
    assertEquals(
      string(R.string.template_text_removed, string(R.string.symbol_at_sign)),
      said.text.toString(),
    )
    assertEquals(1.2f, said.pitch)
    // Backspace before the end, and Delete, which leaves the cursor where it is.
    assertEquals(
      string(R.string.template_text_removed, "capital P"),
      said(at("Pab", 1), at("ab", 0)),
    )
    assertEquals(string(R.string.template_text_removed, "b"), said(at("Pab", 2), at("Pa", 2)))
  }

  @Test
  fun replacedCharacterSaysBoth() {
    // A held key putting its Alt character in place of the one it typed.
    assertEquals(
      string(R.string.template_text_replaced, string(R.string.symbol_at_sign), "a"),
      said(at("Pa"), at("P@")),
    )
  }

  @Test
  fun cursorMoveSaysTheCharacterPassedOver() {
    assertEquals("1", said(at("Pa@1", 4), at("Pa@1", 3)))
    assertEquals(string(R.string.symbol_at_sign), said(at("Pa@1", 2), at("Pa@1", 3)))
    assertEquals("capital P", said(at("Pa@1", 1), at("Pa@1", 0)))
  }

  @Test
  fun otherChangesSayNothing() {
    // Pasted or filled in, cleared, moved further, selected, or unchanged.
    assertNull(PasswordSpeech.change(at(""), at("Password")))
    assertNull(PasswordSpeech.change(at("Password"), at("")))
    assertNull(PasswordSpeech.change(at("Pa@1", 4), at("Pa@1", 0)))
    assertNull(PasswordSpeech.change(at("Pa@1", 4), Field("Pa@1", 3, 4)))
    assertNull(PasswordSpeech.change(at("Pa@1"), at("Pa@1")))
    // Half of an emoji.
    assertNull(PasswordSpeech.change(at("a"), at("a😀")))
    assertNull(PasswordSpeech.change(at("😀"), at("😁")))
  }

  @Test
  fun spellingGivesEachCharacterWithItsPlace() {
    val spelled = PasswordSpeech.spell(context, "Pa@1").toString()
    assertEquals(
      listOf(
          "1 capital P",
          "2 a",
          "3 " + string(R.string.symbol_at_sign),
          "4 1",
        )
        .joinToString(", "),
      spelled,
    )
    // An emoji is one place.
    assertEquals("1 a, 2 😀, 3 b", PasswordSpeech.spell(context, "a😀b").toString())
  }

  @Test
  fun echoSaysNothingWithTheSettingOff() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    assertFalse(PasswordSpeech.isOn(context, prefs))
    val echo = PasswordSpeech.Echo(context)
    assertNull(echo.follow(at("P"), on = false, password = true, sayCapital = true))
    assertNull(echo.follow(at("Pa"), on = false, password = true, sayCapital = true))
    assertFalse(echo.isFollowing)

    prefs.edit { putBoolean(context.getString(R.string.pref_speak_password_characters_key), true) }
    assertTrue(PasswordSpeech.isOn(context, prefs))
  }

  @Test
  fun echoSaysNothingOutsidePasswordFields() {
    val echo = PasswordSpeech.Echo(context)
    assertNull(echo.follow(at("P"), on = true, password = false, sayCapital = true))
    assertNull(echo.follow(at("Pa"), on = true, password = false, sayCapital = true))
    assertFalse(echo.isFollowing)
  }

  @Test
  fun echoSaysEachChangeAfterTheFirstReading() {
    val echo = PasswordSpeech.Echo(context)
    // The first reading only starts following the field.
    assertNull(echo.follow(at("Pa"), on = true, password = true, sayCapital = true))
    assertTrue(echo.isFollowing)
    assertEquals("b", echo.follow(at("Pab"), true, true, true)?.text.toString())
    // The same reading again, as the selection and text change reports both bring, says nothing.
    assertNull(echo.follow(at("Pab"), true, true, true))
    assertEquals(
      string(R.string.template_text_removed, "b"),
      echo.follow(at("Pa"), true, true, true)?.text.toString(),
    )
    // A field whose text can't be read stops it.
    assertNull(echo.follow(null, true, true, true))
    assertFalse(echo.isFollowing)
  }

  @Test
  fun capitalLettersSettingIsRead() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val key = context.getString(R.string.pref_capital_letters_key)
    prefs.edit { putString(key, "1") }
    assertTrue(PasswordSpeech.saysCapital(context, prefs))
    prefs.edit { putString(key, "2") }
    assertFalse(PasswordSpeech.saysCapital(context, prefs))
  }
}
