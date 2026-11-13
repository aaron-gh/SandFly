package com.google.android.accessibility.utils.ocr

import com.google.android.accessibility.utils.ocr.LanguageTextRecognizer.Script
import java.util.Locale
import org.junit.Assert.assertEquals
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
    assertEquals(Script.LATIN, scriptFor("ru-RU"))
    assertEquals(Script.LATIN, scriptFor("ar-EG"))
    assertEquals(Script.LATIN, scriptFor())
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
