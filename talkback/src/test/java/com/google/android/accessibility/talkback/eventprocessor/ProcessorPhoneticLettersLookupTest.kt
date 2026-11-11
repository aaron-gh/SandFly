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

package com.google.android.accessibility.talkback.eventprocessor

import android.text.Spanned
import android.text.style.LocaleSpan
import com.google.android.accessibility.talkback.R
import java.util.Locale
import java.util.function.Function
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end tests for [ProcessorPhoneticLetters.getPhoneticLetterWithMap] covering locale
 * normalization, node lookup, fallbacks and the attached [LocaleSpan]. Tests that pin spoken
 * values use synthetic data so they stay independent of the JSON contents; the real resource is
 * exercised separately to keep an end-to-end path over the shipped data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessorPhoneticLettersLookupTest {

  /** Loads the real phonetic_letters.json as a locale -> (letter -> description) lookup. */
  private fun realPhoneticLetterMap(): Function<String, Map<String, String>> {
    val resources = RuntimeEnvironment.getApplication().resources
    val raw =
      resources.openRawResource(R.raw.phonetic_letters).use { stream ->
        stream.bufferedReader(Charsets.UTF_8).readText()
      }
    val locales = JSONObject(raw)
    val byLocale = mutableMapOf<String, Map<String, String>>()
    val localeKeys = locales.keys()
    while (localeKeys.hasNext()) {
      val locale = localeKeys.next()
      val node = locales.getJSONObject(locale)
      val letters = mutableMapOf<String, String>()
      val letterKeys = node.keys()
      while (letterKeys.hasNext()) {
        val letter = letterKeys.next()
        letters[letter] = node.getString(letter)
      }
      byLocale[locale] = letters
    }
    // Unknown locales resolve to an empty map, matching the loader's behavior.
    return Function { locale -> byLocale[locale] ?: emptyMap() }
  }

  /** Returns the locale carried by the single [LocaleSpan] of the result, if any. */
  private fun localeSpanOf(cs: CharSequence?): Locale? {
    assertNotNull(cs)
    val spans = (cs as Spanned).getSpans(0, cs.length, LocaleSpan::class.java)
    assertEquals(1, spans.size)
    return spans[0].locale
  }

  @Test
  fun zhHansCnReadsZhCnLettersWithPosition() {
    assertEquals(
      "第1个字母，alpha",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "a", realPhoneticLetterMap())
        ?.toString(),
    )
    assertEquals(
      "第26个字母，zulu",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "Z", realPhoneticLetterMap())
        ?.toString(),
    )
  }

  @Test
  fun zhHansCnMarksResultWithOriginalLocale() {
    val cs =
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "a", realPhoneticLetterMap())
    assertEquals(Locale.forLanguageTag("zh-Hans-CN"), localeSpanOf(cs))
  }

  @Test
  fun zhHansCnFallsBackToZhNodeForChineseCharacters() {
    // Synthetic loader: the zh-CN node has no entry for the character, so the lookup must fall
    // back to the zh node through the base-language path. Synthetic data keeps this test
    // independent of the JSON contents, which differ between branches.
    val loader =
      Function { locale: String ->
        when (locale) {
          "zh-CN" -> mapOf("a" to "alpha")
          "zh" -> mapOf("大" to "大小")
          else -> emptyMap<String, String>()
        }
      }
    assertEquals(
      "大小",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "大", loader)?.toString(),
    )
  }

  @Test
  fun zhTwLettersFallBackToZhNode() {
    // The zh-TW node has no Latin letters, so they come from the zh node.
    val loader =
      Function { locale: String ->
        when (locale) {
          "zh" -> mapOf("a" to "Alfa")
          else -> emptyMap<String, String>()
        }
      }
    assertEquals(
      "Alfa",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-TW", "a", loader)?.toString(),
    )
  }

  @Test
  fun zhHkSharesZhTwNodeButKeepsItsOwnLocaleSpan() {
    // zh-HK resolves to the zh-TW node and its base-language fallback, but the utterance must
    // still be marked with the original zh-HK locale so TTS keeps the user's voice.
    val loader =
      Function { locale: String ->
        when (locale) {
          "zh" -> mapOf("a" to "Alfa")
          else -> emptyMap<String, String>()
        }
      }
    val cs = ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-HK", "a", loader)
    assertEquals("Alfa", cs?.toString())
    assertEquals(Locale.forLanguageTag("zh-HK"), localeSpanOf(cs))
  }

  @Test
  fun nonChineseLocaleUsesItsOwnNode() {
    assertEquals(
      "alpha",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("en-US", "A", realPhoneticLetterMap())
        ?.toString(),
    )
  }
}
