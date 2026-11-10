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

import com.google.android.accessibility.talkback.R
import java.util.function.Function
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end tests for [ProcessorPhoneticLetters.getPhoneticLetterWithMap] against the real
 * phonetic_letters.json resource, covering locale normalization, node lookup and fallbacks
 * together with the actual data.
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

  @Test
  fun zhHansCnReadsZhCnLettersWithPosition() {
    val loader = realPhoneticLetterMap()
    assertEquals(
      "第1个字母, alpha",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "a", loader)?.toString(),
    )
    assertEquals(
      "第26个字母, zulu",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-Hans-CN", "Z", loader)?.toString(),
    )
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
    assertEquals(
      "Alfa",
      ProcessorPhoneticLetters.getPhoneticLetterWithMap("zh-TW", "a", realPhoneticLetterMap())
        ?.toString(),
    )
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
