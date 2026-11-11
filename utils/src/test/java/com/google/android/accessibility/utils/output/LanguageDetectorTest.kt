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

package com.google.android.accessibility.utils.output

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguageDetectorTest {

  private val english = Locale.forLanguageTag("en-GB")

  private fun state(
    vararg languages: String,
    scripts: Map<String, String> = emptyMap(),
    identify: Boolean = false,
  ) =
    LanguageVoices.State(
      languages.map { Locale.forLanguageTag(it) },
      emptyMap(),
      scripts.mapValues { Locale.forLanguageTag(it.value) },
      identify,
    )

  /** Each run as "text=language". */
  private fun detect(
    text: String,
    voice: Locale = english,
    state: LanguageVoices.State = state(),
    guesser: LanguageGuesser? = null,
  ): List<String> =
    LanguageDetector.detect(text, voice, state, guesser).map {
      text.substring(it.start(), it.end()) + "=" + it.locale()!!.toLanguageTag()
    }

  @Test
  fun voiceLanguageAndTextWithoutLettersHaveNoRuns() {
    assertEquals(listOf<String>(), detect("Hello world, room 5."))
    assertEquals(listOf<String>(), detect("12:30 — 😀"))
  }

  @Test
  fun anotherScriptIsItsUsualLanguage() {
    assertEquals(listOf("مرحبا بكم =ar"), detect("Hello مرحبا بكم world"))
    assertEquals(listOf("Καλημέρα=el"), detect("Καλημέρα"))
    assertEquals(listOf("שלום, =he"), detect("שלום, John"))
  }

  @Test
  fun punctuationAndDigitsGoWithTheTextBefore_orAfterAtTheStart() {
    assertEquals(listOf("مرحبا 123!=ar"), detect("Hi مرحبا 123!"))
    assertEquals(listOf("123 مرحبا =ar"), detect("123 مرحبا Hi"))
  }

  @Test
  fun scriptDigitsAreTheirScriptsLanguage() {
    assertEquals(listOf("١٢٣=ar"), detect("١٢٣"))
  }

  @Test
  fun voiceInAnotherScriptSwitchesForLatin() {
    val arabic = Locale.forLanguageTag("ar")
    assertEquals(listOf("Settings=en"), detect("الإعدادات Settings", voice = arabic))
  }

  @Test
  fun addedLanguageIsUsedForItsScript() {
    assertEquals(listOf("Привет=uk"), detect("Привет", state = state("uk")))
  }

  @Test
  fun lettersOnlyOneLanguageUsesDecide() {
    val russian = Locale.forLanguageTag("ru")
    assertEquals(listOf("Привіт, як справи=uk"), detect("Привіт, як справи", russian, state("uk")))
    assertEquals(listOf<String>(), detect("Привет, как дела", russian, state("uk")))
    val arabic = Locale.forLanguageTag("ar")
    assertEquals(listOf("این یک پیام است=fa"), detect("این یک پیام است", arabic, state("fa")))
  }

  @Test
  fun oneBorrowedWordDoesNotSwitch() {
    assertEquals(listOf<String>(), detect("I went to the café yesterday", state = state("fr")))
    assertEquals(listOf("C'est déjà l'été à Paris=fr"), detect("C'est déjà l'été à Paris", state = state("fr")))
  }

  @Test
  fun eachSentenceIsDecidedOnItsOwn() {
    assertEquals(
      listOf("Où était la gare? =fr"),
      detect("Où était la gare? Over there.", state = state("fr")),
    )
  }

  @Test
  fun aDoubleSpaceEndsASentence() {
    assertEquals(
      listOf("Où était la gare  =fr"),
      detect("Où était la gare  Over there", state = state("fr")),
    )
  }

  @Test
  fun scriptLanguageIsUsedWhenLettersDontTell() {
    val serbianForLatin = state("sr", scripts = mapOf("Latn" to "sr"))
    assertEquals(listOf("Dobar dan=sr"), detect("Dobar dan", state = serbianForLatin))
  }

  @Test
  fun guesserTellsSameScriptLanguagesApart() {
    val french = LanguageGuesser { _, candidates -> "fr".takeIf { it in candidates } }
    val text = "Bonjour tout le monde, comment allez vous"
    assertEquals(listOf<String>(), detect(text, state = state("fr"), guesser = french))
    assertEquals(listOf("$text=fr"), detect(text, state = state("fr", identify = true), guesser = french))
    // Too short to guess.
    assertEquals(listOf<String>(), detect("Bonjour", state = state("fr", identify = true), guesser = french))
  }

  @Test
  fun kanaMakeChineseCharactersJapanese() {
    assertEquals(listOf("東京へ行く=ja"), detect("東京へ行く"))
    assertEquals(listOf("北京欢迎你=zh"), detect("北京欢迎你"))
    assertEquals(listOf("서울에 가요=ko"), detect("서울에 가요"))
  }

  @Test
  fun sharedScriptsListLanguagesWrittenInBoth() {
    val shared =
      LanguageDetector.sharedScripts(listOf(english, Locale.forLanguageTag("fr"), Locale.forLanguageTag("ar")))
    assertEquals(setOf("Latn"), shared.keys)
    assertEquals(listOf("en-GB", "fr"), shared["Latn"]!!.map { it.toLanguageTag() })
  }
}
