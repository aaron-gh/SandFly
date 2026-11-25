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

package com.google.android.accessibility.braille.translate.liblouis

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Unicode braille tables back-translate with unicode-braille.utb, which must give each of the
 * 256 cells the character U+2800 plus its dots, dot 1 being bit 0.
 */
class UnicodeBrailleTableTest {
  private val rules =
    File("src/phone/tables/unicode-braille.utb")
      .readLines()
      .map { it.substringBefore('#').trim() }
      .filter { it.isNotEmpty() }
      .map { it.split(Regex("\\s+")) }

  @Test
  fun everyCellIsItsUnicodeBrailleCharacter() {
    val characters = mutableSetOf<Int>()
    for (rule in rules) {
      assertEquals("sign", rule[0])
      val character = rule[1].removePrefix("\\x").toInt(16)
      val dots =
        if (rule[2] == "0") 0 else rule[2].fold(0) { bits, dot -> bits or (1 shl (dot - '1')) }
      assertEquals(rule.joinToString(" "), 0x2800 + dots, character)
      characters += character
    }
    assertEquals((0x2800..0x28FF).toSet(), characters)
  }

  @Test
  fun tableHasOneRulePerCell() {
    assertEquals(256, rules.size)
  }
}
