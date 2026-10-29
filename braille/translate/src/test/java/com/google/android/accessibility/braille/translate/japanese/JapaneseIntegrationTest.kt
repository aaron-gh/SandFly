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

package com.google.android.accessibility.braille.translate.japanese

import java.io.File
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * libkuraji's integration test: the harness with the real dictionary instead of the recorded
 * analysis. Needs the extracted JTalk dictionary (jtalk-dic-v1.2.1.zip from
 * nishimotz/libkuraji-jtalk-dic) in the directory named by the JTALK_DIC environment variable;
 * skipped without it.
 */
class JapaneseIntegrationTest {
  private val directory = System.getenv("JTALK_DIC")?.let(::File)

  @Test
  fun wordSeparationWithTheDictionary() {
    assumeTrue(directory != null && File(directory, "sys.dic").exists())
    val translator = Translator2(JapaneseAnalyzer(directory!!)::analyze)
    val failures = ArrayList<String>()
    var count = 0
    for ((file, nabcc) in listOf("harness.json" to false, "nabccHarness.json" to true)) {
      val cases = JSONArray(javaClass.getResource("/libkuraji/$file")!!.readText(Charsets.UTF_8))
      for (i in 0 until cases.length()) {
        val case = cases.getJSONObject(i)
        if (!case.has("text") || !case.has("input")) continue
        count++
        val text = case.getString("text")
        val actual = translator.translateWithInPos2(text, nabcc = nabcc || case.optString("mode") == "NABCC").text
        if (actual != case.getString("input")) {
          failures.add("$file[$i] $text\n  expected ${case.getString("input")}\n  actual   $actual")
        }
      }
    }
    // harness.json and nabccHarness.json at 307f3dd have 1685 cases.
    assertEquals(1685, count)
    assertEquals(
      "${failures.size} of $count differ:\n" + failures.take(25).joinToString("\n"),
      0,
      failures.size,
    )
  }
}
