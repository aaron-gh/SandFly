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
package com.google.android.accessibility.talkback.keyboard

import android.icu.lang.UCharacter
import java.util.Locale

/**
 * A list of characters to choose from by typing words of their names: emoji, or symbols. Recently
 * chosen ones come first. Symbols whose names have every typed word at the start of one of theirs
 * are listed in their order; emoji are found by [search], which has their names in the user's
 * language, once something is typed.
 */
class CharacterChooser(
  val kind: Kind,
  private val items: List<Item>,
  private val recent: List<Item>,
  private val search: ((String) -> List<Item>)? = null,
) {
  /** What is chosen. */
  enum class Kind {
    EMOJI,
    SYMBOL,
  }

  /** A character, or a sequence shown as one, and its name. */
  data class Item(val text: String, val name: String)

  private val order: List<Item> = recent + items.filter { item -> recent.none { it.text == item.text } }

  /** The words typed so far. */
  val query = StringBuilder()

  var matches: List<Item> = order
    private set

  /** The position of the item chosen now among [matches]. */
  var index = 0
    private set

  val current: Item?
    get() = matches.getOrNull(index)

  /** Adds [character] to the typed words and lists the matches again. */
  fun type(character: Char) {
    if (query.length < MAX_QUERY) {
      query.append(character)
      filter()
    }
  }

  /** Deletes the last character typed. Returns false when nothing was typed. */
  fun deleteLast(): Boolean {
    if (query.isEmpty()) {
      return false
    }
    query.setLength(query.length - 1)
    filter()
    return true
  }

  /** Moves to the next match, back to the first after the last. */
  fun next() {
    if (matches.isNotEmpty()) index = (index + 1) % matches.size
  }

  /** Moves to the previous match, on to the last before the first. */
  fun previous() {
    if (matches.isNotEmpty()) index = (index - 1 + matches.size) % matches.size
  }

  private fun filter() {
    val typed = query.toString()
    val search = search
    matches =
      if (search == null || typed.isBlank()) {
        order.filter { matches(it.name, typed) }
      } else {
        val found = search(typed)
        recent.filter { item -> found.any { it.text == item.text } } +
          found.filter { item -> recent.none { it.text == item.text } }
      }
    index = 0
  }

  companion object {
    private const val MAX_QUERY = 40

    /** Symbols that keyboards without them type with difficulty, in the order offered. */
    private const val SYMBOLS =
      ".,?!'\"-_:;@#&*/\\()[]{}<>+=%$€£¥¢^~`|•…–—«»„“”‘’¿¡§¶©®™°±×÷√∞≈≠≤≥µ¹²³½¼¾→←↑↓✓✗♥☺"

    /** Whether every word of [query] starts a word of [name]. */
    internal fun matches(name: String, query: String): Boolean {
      val words = query.lowercase(Locale.ROOT).split(' ').filter { it.isNotEmpty() }
      if (words.isEmpty()) {
        return true
      }
      val nameWords = name.lowercase(Locale.ROOT).split(' ', '-')
      return words.all { word -> nameWords.any { it.startsWith(word) } }
    }

    /** The symbols, with their Unicode names. */
    @JvmStatic
    fun symbols(): List<Item> = SYMBOLS.map { Item(it.toString(), nameOf(it.code)) }

    private fun nameOf(codePoint: Int): String =
      UCharacter.getName(codePoint)?.lowercase(Locale.ROOT) ?: String(Character.toChars(codePoint))
  }
}
