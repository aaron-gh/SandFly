package com.google.android.accessibility.utils.ocr

import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTextRecognizerTest {

  private fun scriptFor(vararg tags: String): Script =
    LanguageTextRecognizer.scriptFor(tags.map(Locale::forLanguageTag))

  @Test
  fun chineseLanguagesUseTheChineseRecognizer() {
    assertEquals(Script.CHINESE, scriptFor("zh-CN"))
    assertEquals(Script.CHINESE, scriptFor("zh-Hant-TW"))
    assertEquals(Script.CHINESE, scriptFor("yue-HK"))
  }

  @Test
  fun otherScriptsUseTheirRecognizers() {
    assertEquals(Script.JAPANESE, scriptFor("ja-JP"))
    assertEquals(Script.KOREAN, scriptFor("ko-KR"))
    assertEquals(Script.DEVANAGARI, scriptFor("hi-IN"))
    assertEquals(Script.DEVANAGARI, scriptFor("mr-IN"))
    assertEquals(Script.DEVANAGARI, scriptFor("ne-NP"))
  }

  @Test
  fun latinLettersAndScriptsWithoutARecognizerUseLatin() {
    assertEquals(Script.LATIN, scriptFor("en-US"))
    assertEquals(Script.LATIN, scriptFor("de-DE", "fr-FR"))
    assertEquals(Script.LATIN, scriptFor("ar-EG"))
    assertEquals(Script.LATIN, scriptFor())
  }

  @Test
  fun languagesWrittenInCyrillicUseTheCyrillicRecognizer() {
    assertEquals(Script.CYRILLIC, scriptFor("ru-RU"))
    assertEquals(Script.CYRILLIC, scriptFor("uk-UA"))
    assertEquals(Script.CYRILLIC, scriptFor("sr-RS"))
    assertEquals(Script.CYRILLIC, scriptFor("en-US", "bg-BG"))
    assertEquals(Script.CYRILLIC, scriptFor("uz-Cyrl-UZ"))
  }

  @Test
  fun cyrillicLanguagesInLatinLettersUseLatin() {
    assertEquals(Script.LATIN, scriptFor("sr-Latn-RS"))
    assertEquals(Script.LATIN, scriptFor("uz-UZ"))
    assertEquals(Script.LATIN, scriptFor("az-AZ"))
    assertEquals(Script.LATIN, scriptFor("mn-Mong-CN"))
  }

  @Test
  fun eachCyrillicLanguageHasATesseractModel() {
    fun model(tag: String) = CyrillicOcr.modelFor(Locale.forLanguageTag(tag))
    assertEquals("rus", model("ru-RU"))
    assertEquals("ukr", model("uk"))
    assertEquals("srp", model("sr-Cyrl-RS"))
    assertEquals("uzb_cyrl", model("uz-Cyrl"))
    // Other languages in Cyrillic have the Russian one.
    assertEquals(CyrillicOcr.DEFAULT_MODEL, model("ba-RU"))
    assertEquals(null, model("sr-Latn"))
    assertEquals(null, model("en-US"))
  }

  @Test
  fun latinLettersAreReadWithTheCyrillicModelWhenDownloaded() {
    assertEquals("rus+eng", CyrillicOcr.languages("rus", latinInstalled = true))
    assertEquals("rus", CyrillicOcr.languages("rus", latinInstalled = false))
    assertEquals("ru", CyrillicOcr.languageOf("rus"))
    assertEquals("und", CyrillicOcr.languageOf("xyz"))
  }

  @Test
  fun onlyDownloadedModelsAreLoaded() {
    assertTrue(CyrillicOcr.isValidLanguages("rus+eng"))
    assertTrue(CyrillicOcr.isValidLanguages("ukr"))
    assertFalse(CyrillicOcr.isValidLanguages("rus+ukr+eng"))
    assertFalse(CyrillicOcr.isValidLanguages("../../files/x"))
    assertFalse(CyrillicOcr.isValidLanguages(""))
  }

  @Test
  fun aScriptSubtagWinsOverTheLanguage() {
    assertEquals(Script.LATIN, scriptFor("zh-Latn"))
    assertEquals(Script.DEVANAGARI, scriptFor("sd-Deva-IN"))
  }

  @Test
  fun theFirstLanguageInAnotherScriptIsUsed() {
    // The speech language is English and the phone's languages are English, then Chinese.
    assertEquals(Script.CHINESE, scriptFor("en-US", "en-US", "zh-CN"))
    assertEquals(Script.JAPANESE, scriptFor("ja-JP", "zh-CN"))
    assertEquals(Script.KOREAN, scriptFor("fr-FR", "ko-KR", "hi-IN"))
  }

  @Test
  fun wordsOfChineseAndJapaneseAreJoinedWithoutSpaces() {
    assertEquals("你好世界", OcrController.removeSpacesBetweenIdeographs("你好 世界"))
    assertEquals("你好，世界。", OcrController.removeSpacesBetweenIdeographs("你好 ， 世界 。"))
    assertEquals("ひらがなとカタカナ", OcrController.removeSpacesBetweenIdeographs("ひらがな と カタカナ"))
  }

  @Test
  fun spacesNextToOtherLettersStay() {
    assertEquals("Hello world", OcrController.removeSpacesBetweenIdeographs("Hello world"))
    assertEquals("你好 Android 世界", OcrController.removeSpacesBetweenIdeographs("你好 Android 世界"))
    assertEquals("안녕 하세요", OcrController.removeSpacesBetweenIdeographs("안녕 하세요"))
    assertEquals("12 34", OcrController.removeSpacesBetweenIdeographs("12 34"))
  }
}
