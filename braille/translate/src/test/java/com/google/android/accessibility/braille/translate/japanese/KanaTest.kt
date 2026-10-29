/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) tests/test_translator1.py at
 * 307f3dd.
 * Copyright (C) 2019-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KanaTest {

  private fun load(name: String): JSONArray {
    val stream =
      javaClass.classLoader!!.getResourceAsStream("libkuraji/$name")
        ?: error("Missing test resource libkuraji/$name")
    return JSONArray(stream.bufferedReader(Charsets.UTF_8).readText())
  }

  private fun JSONArray.toIntList(): List<Int> = List(length()) { getInt(it) }

  @Test
  fun kanaToBraille() {
    val failures = mutableListOf<String>()
    var count = 0
    for ((file, defaultMode) in listOf("harness.json" to "", "nabccHarness.json" to "NABCC")) {
      val cases = load(file)
      for (idx in 0 until cases.length()) {
        val t: JSONObject = cases.getJSONObject(idx)
        if (!t.has("input") || !t.has("output")) continue
        count++
        val nabcc = t.optString("mode", defaultMode) == "NABCC"
        val input = t.getString("input")
        val (result, inpos) = Kana.translateWithPos(input, nabcc)
        val expected = t.getString("output")
        val problem =
          when {
            result != expected -> "got $result expected $expected"
            result.length != inpos.size -> "inpos size ${inpos.size} for ${result.length} cells"
            t.has("inpos1") && inpos != t.getJSONArray("inpos1").toIntList() ->
              "inpos $inpos expected ${t.getJSONArray("inpos1").toIntList()}"
            else -> null
          }
        if (problem != null) failures.add("$file-$idx \"$input\": $problem")
      }
    }
    if (failures.isNotEmpty()) {
      fail(
        "${failures.size} of $count cases failed:\n" + failures.take(20).joinToString("\n")
      )
    }
    assertTrue(count > 0)
  }

  @Test
  fun underscoreTranslation() {
    // Single underscore: dot 5, dots 3-6 (⠐⠤)
    assertEquals("⠐⠤" to listOf(0, 0), Kana.translateWithPos("_"))
    // Underscore in uppercase identifier
    assertEquals("⠰⠠⠁⠐⠤⠰⠠⠃" to listOf(0, 0, 0, 1, 1, 2, 2, 2), Kana.translateWithPos("A_B"))
    // Underscore in lowercase identifier
    assertEquals("⠰⠁⠐⠤⠰⠃" to listOf(0, 0, 1, 1, 2, 2), Kana.translateWithPos("a_b"))
    // Underscore between kana words
    assertTrue("⠐⠤" in Kana.translateWithPos("テスト_テスト").first)
  }

  @Test
  fun readmejpSymbols() {
    // readmejp.md: |$ 半角ドル |56-1456 |⠰⠹|
    assertEquals("⠰⠹" to listOf(0, 0), Kana.translateWithPos("$"))
    // readmejp.md: || 縦棒 |2356 |⠶|
    assertEquals("⠶" to listOf(0), Kana.translateWithPos("|"))
    // fullwidth vertical bar ｜
    assertEquals("⠶" to listOf(0), Kana.translateWithPos("｜"))
    // readmejp.md: |; 半角セミコロン |23 |⠆|
    assertEquals("⠆" to listOf(0), Kana.translateWithPos(";"))
    // readmejp.md: |\ 半角円 |16 |⠡|
    assertEquals("⠡" to listOf(0), Kana.translateWithPos("\\"))
  }

  @Test
  fun nfkcMapKeepsOriginalPositions() {
    assertEquals("a...b" to listOf(0, 1, 1, 1, 2), UnicodeUtil.nfkcNormalizeWithMap("a…b"))
    assertEquals("10" to listOf(0, 0), UnicodeUtil.nfkcNormalizeWithMap("⑩"))
    // halfwidth voiced mark composes with its base
    assertEquals("ガ" to listOf(0), UnicodeUtil.nfkcNormalizeWithMap("ｶﾞ"))
  }

  @Test
  fun unicodeNormalizeFolds() {
    assertEquals("abc", UnicodeUtil.unicodeNormalize("abc"))
    assertEquals("A -1", UnicodeUtil.unicodeNormalize("Ａ −１"))
    assertEquals("cafe", UnicodeUtil.unicodeNormalize("café"))
  }
}
