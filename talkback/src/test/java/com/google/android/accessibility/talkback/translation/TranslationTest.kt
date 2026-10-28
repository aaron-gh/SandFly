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

package com.google.android.accessibility.talkback.translation

import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TranslationTest {
  private val prefs = SharedPreferencesUtils.getSharedPreferences(RuntimeEnvironment.getApplication())

  @Test
  fun normalize_mapsServiceAndAndroidCodes() {
    assertEquals("en", TranslationLanguages.normalize("EN-GB"))
    assertEquals("he", TranslationLanguages.normalize("iw"))
    assertEquals("fil", TranslationLanguages.normalize("tl"))
    assertEquals("no", TranslationLanguages.normalize("nb"))
    assertEquals(TranslationLanguages.CHINESE_SIMPLIFIED, TranslationLanguages.normalize("ZH"))
    assertEquals(TranslationLanguages.CHINESE_SIMPLIFIED, TranslationLanguages.normalize("zh-CN"))
    assertEquals(TranslationLanguages.CHINESE_TRADITIONAL, TranslationLanguages.normalize("zh-TW"))
    assertEquals(TranslationLanguages.CHINESE_TRADITIONAL, TranslationLanguages.normalize("zh-Hant-HK"))
    assertEquals("mni", TranslationLanguages.normalize("mni-Mtei"))
    assertNull(TranslationLanguages.normalize("und"))
    assertNull(TranslationLanguages.normalize("auto"))
    assertNull(TranslationLanguages.normalize("xx"))
    assertNull(TranslationLanguages.normalize(""))
  }

  @Test
  fun of_givesTheDeviceLanguage() {
    assertEquals("de", TranslationLanguages.of(Locale.GERMANY))
    assertEquals(TranslationLanguages.CHINESE_TRADITIONAL, TranslationLanguages.of(Locale.TRADITIONAL_CHINESE))
    assertEquals(TranslationLanguages.CHINESE_SIMPLIFIED, TranslationLanguages.of(Locale.CHINA))
    assertEquals("en", TranslationLanguages.of(Locale.forLanguageTag("tlh")))
  }

  @Test
  fun same_ignoresDialects() {
    assertTrue(TranslationLanguages.same("EN-US", "en"))
    assertFalse(TranslationLanguages.same("zh-TW", "zh-CN"))
    assertFalse(TranslationLanguages.same(null, "en"))
  }

  @Test
  fun googleCloud_sendsTheKeyInAHeader() {
    val request = TranslationApis.googleCloud("key", "Hallo", null, TranslationLanguages.CHINESE_TRADITIONAL)
    assertEquals("key", request.headers["X-Goog-Api-Key"])
    assertFalse(request.url.contains("key"))
    val body = JSONObject(request.body)
    assertEquals("zh-TW", body.getString("target"))
    assertEquals("text", body.getString("format"))
    assertFalse(body.has("source"))
    val answer = """{"data":{"translations":[{"translatedText":"你好","detectedSourceLanguage":"de"}]}}"""
    assertEquals(Translation("你好", "de", "zh-Hant"), TranslationApis.parseGoogleCloud(answer, "zh-Hant"))
  }

  @Test
  fun deepL_choosesTheFreeApiAndAnEnglishVariant() {
    val free = TranslationApis.deepL("abc:fx", "Hallo", "de", "en", Locale.UK)
    assertEquals("https://api-free.deepl.com/v2/translate", free.url)
    assertEquals("DeepL-Auth-Key abc:fx", free.headers["Authorization"])
    val body = JSONObject(free.body)
    assertEquals("EN-GB", body.getString("target_lang"))
    assertEquals("DE", body.getString("source_lang"))
    assertEquals("Hallo", body.getJSONArray("text").getString(0))
    val pro = TranslationApis.deepL("abc", "Hallo", null, "en", Locale.US)
    assertEquals("https://api.deepl.com/v2/translate", pro.url)
    assertEquals("EN-US", JSONObject(pro.body).getString("target_lang"))
    assertEquals("ZH-HANT", TranslationApis.deepLTarget(TranslationLanguages.CHINESE_TRADITIONAL, Locale.US))
    assertEquals("ZH", TranslationApis.deepLSource(TranslationLanguages.CHINESE_TRADITIONAL))
    val answer = """{"translations":[{"detected_source_language":"DE","text":"Hello"}]}"""
    assertEquals(Translation("Hello", "de", "en"), TranslationApis.parseDeepL(answer, "en"))
  }

  @Test
  fun microsoft_putsLanguagesInTheAddress() {
    val request = TranslationApis.microsoft("key", "westeurope", "Hallo", "no", "en")
    assertEquals(
      "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=en&from=nb",
      request.url,
    )
    assertEquals("key", request.headers["Ocp-Apim-Subscription-Key"])
    assertEquals("westeurope", request.headers["Ocp-Apim-Subscription-Region"])
    assertEquals("Hallo", JSONArray(request.body).getJSONObject(0).getString("Text"))
    val global = TranslationApis.microsoft("key", "", "Hallo", null, "en")
    assertFalse(global.headers.containsKey("Ocp-Apim-Subscription-Region"))
    assertFalse(global.url.contains("from="))
    val answer = """[{"detectedLanguage":{"language":"de","score":1.0},"translations":[{"text":"Hello","to":"en"}]}]"""
    assertEquals(Translation("Hello", "de", "en"), TranslationApis.parseMicrosoft(answer, "en"))
  }

  @Test
  fun libreTranslate_detectsWithoutASource() {
    val request = TranslationApis.libreTranslate("https://example.com/", "", "Hallo", null, "fil")
    assertEquals("https://example.com/translate", request.url)
    val body = JSONObject(request.body)
    assertEquals("auto", body.getString("source"))
    assertEquals("tl", body.getString("target"))
    assertFalse(body.has("api_key"))
    assertTrue(JSONObject(TranslationApis.libreTranslate("https://example.com", "k", "Hallo", "de", "en").body).has("api_key"))
    val answer = """{"detectedLanguage":{"confidence":90,"language":"de"},"translatedText":"Hello"}"""
    assertEquals(Translation("Hello", "de", "en"), TranslationApis.parseLibreTranslate(answer, "en"))
  }

  @Test
  fun failure_readsTheServicesErrors() {
    assertEquals(
      TranslationFailure(TranslationFailure.Kind.API_KEY, "API key not valid"),
      TranslationApis.failure(403, """{"error":{"code":403,"message":"API key not valid"}}"""),
    )
    assertEquals(TranslationFailure.Kind.QUOTA, TranslationApis.failure(456, """{"message":"Quota exceeded"}""").kind)
    assertEquals("Wrong", TranslationApis.failure(400, """{"error":"Wrong"}""").detail)
    assertEquals("HTTP 502", TranslationApis.failure(502, "<html>").detail)
  }

  @Test
  fun aiPrompt_namesTheLanguagesInEnglish() {
    val prompt = AiTranslationPrompt.prompt("Hallo", null, "en", "de")
    assertTrue(prompt.contains("into English"))
    assertTrue(prompt.contains("already in English, translate it into German"))
    assertTrue(prompt.endsWith("Hallo"))
    assertTrue(AiTranslationPrompt.prompt("Hallo", "de", "en", null).contains("The text is in German"))
  }

  @Test
  fun aiAnswer_readsJsonOrPlainText() {
    assertEquals(
      Translation("Hello", "de", "en"),
      AiTranslationPrompt.parse("```json\n{\"from\": \"de\", \"to\": \"en\", \"translation\": \"Hello\"}\n```", "fr"),
    )
    assertEquals(Translation("Bonjour", null, "fr"), AiTranslationPrompt.parse(" Bonjour ", "fr"))
  }

  @Test
  fun settings_defaultToDetectingIntoTheDeviceLanguage() {
    assertNull(TranslationSettings.source(prefs))
    assertEquals(TranslationLanguages.of(Locale.getDefault()), TranslationSettings.target(prefs))
    assertTrue(TranslationSettings.followsDevice(prefs))
    assertNull(TranslationSettings.secondTarget(prefs))
    assertEquals(TranslationService.ON_DEVICE, TranslationSettings.service(prefs, onDeviceAvailable = true))
    assertEquals(TranslationService.AI, TranslationSettings.service(prefs, onDeviceAvailable = false))
  }

  @Test
  fun swap_exchangesTheLanguages() {
    TranslationSettings.setSource(prefs, "de")
    TranslationSettings.setTarget(prefs, "en")
    TranslationSettings.swap(prefs)
    assertEquals("en", TranslationSettings.source(prefs))
    assertEquals("de", TranslationSettings.target(prefs))
  }

  @Test
  fun missing_needsAKeyOrAnAddress() {
    assertEquals(TranslationSettings.Missing.API_KEY, TranslationSettings.missing(prefs, TranslationService.DEEPL))
    TranslationSettings.setApiKey(prefs, TranslationService.DEEPL, " abc:fx\n")
    assertEquals("abc:fx", TranslationSettings.apiKey(prefs, TranslationService.DEEPL))
    assertNull(TranslationSettings.missing(prefs, TranslationService.DEEPL))
    assertNull(TranslationSettings.missing(prefs, TranslationService.LIBRETRANSLATE))
    TranslationSettings.setLibreTranslateAddress(prefs, "http://192.168.1.2:5000")
    assertEquals(TranslationSettings.Missing.ADDRESS, TranslationSettings.missing(prefs, TranslationService.LIBRETRANSLATE))
  }

  @Test
  fun everyLanguageHasAName() {
    for (code in TranslationLanguages.ALL) {
      assertEquals(code, TranslationLanguages.normalize(code))
    }
    assertEquals("German", TranslationLanguages.name("de", Locale.ENGLISH))
    assertEquals("Chinese (Traditional)", TranslationLanguages.name(TranslationLanguages.CHINESE_TRADITIONAL, Locale.ENGLISH))
    assertEquals("Chinese (Simplified)", TranslationLanguages.name(TranslationLanguages.CHINESE_SIMPLIFIED, Locale.ENGLISH))
  }
}
