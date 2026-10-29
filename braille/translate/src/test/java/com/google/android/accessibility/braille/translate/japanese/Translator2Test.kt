/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) tests/test_translator2.py at
 * 307f3dd. Copyright (C) 2012-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class Translator2Test {
  /**
   * Replays the MeCab feature lines recorded from the reference configuration (JTalk extended
   * dictionary), see mecabFixture.json.
   */
  private val translator = Translator2 { text ->
    FIXTURE[text] ?: throw NoSuchElementException("no recorded analysis for \"$text\"")
  }

  @Test
  fun wordSeparation() {
    val failures = mutableListOf<String>()
    var count = 0
    for ((file, defaultMode) in listOf("harness.json" to "", "nabccHarness.json" to "NABCC")) {
      val cases = array(file)
      for (index in 0 until cases.length()) {
        val case = cases.getJSONObject(index)
        if (!case.has("text") || !case.has("input")) continue
        count++
        val id = "${file.substringBefore('.')}-$index"
        val text = case.getString("text")
        val nabcc = case.optString("mode", defaultMode) == "NABCC"
        try {
          val result = translator.translateWithInPos2(text, nabcc = nabcc)
          val problems = mutableListOf<String>()
          if (result.text != case.getString("input")) {
            problems.add("text \"${result.text}\", expected \"${case.getString("input")}\"")
          }
          if (case.has("inpos2") && result.inpos2 != ints(case.getJSONArray("inpos2"))) {
            problems.add("inpos2 ${result.inpos2}, expected ${case.getJSONArray("inpos2")}")
          }
          val (inpos, _) =
            Translator2.mergePositionMap(
              result.inpos1,
              result.inpos2,
              result.braille.length,
              text.length,
            )
          if (case.has("inpos") && inpos != ints(case.getJSONArray("inpos"))) {
            problems.add("inpos $inpos, expected ${case.getJSONArray("inpos")}")
          }
          if (case.has("outpos")) {
            val outpos = Translator2.makeOutPos(inpos, text.length, result.braille.length)
            if (outpos != ints(case.getJSONArray("outpos"))) {
              problems.add("outpos $outpos, expected ${case.getJSONArray("outpos")}")
            }
          }
          if (problems.isNotEmpty()) failures.add("$id \"$text\": ${problems.joinToString("; ")}")
        } catch (e: RuntimeException) {
          failures.add("$id \"$text\": $e")
        }
      }
    }
    // harness.json and nabccHarness.json at 307f3dd have 1685 cases.
    assertEquals(1685, count)
    report(count, failures)
  }

  @Test
  fun bulletBraillePattern() {
    // List bullet (•, U+2022) should map to U+2810 (dot 5) in braille (nvdajp#723).
    for ((text, expected) in
      listOf(
        "•" to "⠐",
        "•項目" to "⠐ ⠪⠒⠾⠩",
        "• 項目" to "⠐ ⠪⠒⠾⠩",
        "• item" to "⠐ ⠰⠊⠞⠑⠍",
        "• 1番目" to "⠐ ⠼⠁⠐⠥⠴⠿",
      )) {
      assertEquals(text, expected, translator.translate(text).braille)
    }
  }

  @Test
  fun kanjiNumbers() {
    assertEquals(1 to "25", Translator2.kansuji2arabic("二十五"))
    assertEquals(1 to "2400", Translator2.kansuji2arabic("二千四百"))
    assertEquals(0 to null, Translator2.kansuji2arabic("五月"))
  }

  private fun report(count: Int, failures: List<String>) {
    if (failures.isEmpty()) return
    fail(
      "${failures.size} of $count cases failed:\n" + failures.take(20).joinToString("\n")
    )
  }

  private companion object {
    fun read(name: String): String =
      Translator2Test::class.java.classLoader!!
        .getResourceAsStream("libkuraji/$name")!!
        .use { it.readBytes().toString(Charsets.UTF_8) }

    fun array(name: String) = JSONArray(read(name))

    fun ints(array: JSONArray): List<Int> = List(array.length()) { array.getInt(it) }

    val FIXTURE: Map<String, List<String>> by lazy {
      val json = JSONObject(read("mecabFixture.json"))
      json.keys().asSequence().associateWith { key ->
        val lines = json.getJSONArray(key)
        List(lines.length()) { lines.getString(it) }
      }
    }
  }
}
