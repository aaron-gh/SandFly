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
package com.google.android.accessibility.talkback.keyboard.words

import kotlin.math.sqrt

/**
 * Where the letters are on a keyboard, from rows of characters, for the keys next to a key: a
 * typed letter is more likely a neighbour's by mistake than a letter further away. Distances are in
 * keys, one key across and one row down; each row starts [Row.offset] keys from the left. From
 * TapType's keyboard layouts.
 */
class KeyLayout(rows: List<Row>) {
  class Row(val chars: String, val offset: Float = 0f)

  private val x = HashMap<Char, Float>()
  private val y = HashMap<Char, Float>()

  init {
    rows.forEachIndexed { rowIndex, row ->
      row.chars.forEachIndexed { i, c ->
        // A character on two keys keeps its first place; NUL marks a key without a letter.
        if (c != '\u0000' && c !in x) {
          x[c] = row.offset + i
          y[c] = rowIndex.toFloat()
        }
      }
    }
  }

  /** Whether the layout has [c]. */
  operator fun contains(c: Char): Boolean = c in x

  /** The distance between the keys of [a] and [b] in keys, or null if either isn't on a key. */
  fun distance(a: Char, b: Char): Float? {
    val ax = x[a] ?: return null
    val bx = x[b] ?: return null
    val dx = ax - bx
    val dy = y.getValue(a) - y.getValue(b)
    return sqrt(dx * dx + dy * dy)
  }

  /** Whether the keys of [a] and [b] are next to each other, across, above or below. */
  fun areNeighbours(a: Char, b: Char): Boolean {
    if (a == b) return false
    val d = distance(a, b) ?: return false
    return d <= NEIGHBOUR_DISTANCE
  }

  companion object {
    /** Keys next to each other across are 1 apart; above and below with half a key's offset, 1.12. */
    private const val NEIGHBOUR_DISTANCE = 1.2f

    /** The offsets of the three letter rows of a physical keyboard, in keys. */
    val PHYSICAL_OFFSETS = floatArrayOf(0f, 0.5f, 1f)

    /** A layout from the characters of the three letter rows of a physical keyboard. */
    fun physical(rows: List<String>): KeyLayout =
      KeyLayout(rows.mapIndexed { i, chars -> Row(chars, PHYSICAL_OFFSETS.getOrElse(i) { 1f }) })

    val QWERTY = physical(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"))
    val QWERTZ = physical(listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnm"))
    val AZERTY = physical(listOf("azertyuiop", "qsdfghjklm", "wxcvbn"))
    val RUSSIAN = physical(listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"))
    val UKRAINIAN = physical(listOf("йцукенгшщзхї", "фівапролджє", "ячсмитьбю"))
    val ARABIC = physical(listOf("ضصثقفغعهخحجد", "شسيبلاتنمكط", "ئءؤرىةوزظ"))
  }
}
