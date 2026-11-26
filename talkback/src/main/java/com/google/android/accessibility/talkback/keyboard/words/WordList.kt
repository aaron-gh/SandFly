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

import java.io.BufferedReader

/**
 * The words of a language with their counts, in a trie held in arrays: each node is an index, with
 * its character, first child, next sibling and count. About 20 bytes a node, so that a list of
 * 80,000 words takes a few megabytes of SandFly's memory, not the tens a node object each would.
 *
 * Words are kept in lowercase; a word listed with capitals, a name, keeps that form as its
 * [capitalForm]. Only the main thread uses a list once it is loaded.
 */
class WordList private constructor() {
  private var chars = CharArray(INITIAL_NODES)
  private var firstChild = IntArray(INITIAL_NODES) { NONE }
  private var nextSibling = IntArray(INITIAL_NODES) { NONE }
  private var counts = LongArray(INITIAL_NODES)
  private var size = 1
  private val capitals = HashMap<Int, String>()

  /** The count from which words are common enough that auto-correct leaves them as typed. */
  var commonCount = Long.MAX_VALUE
    private set

  /** The number of nodes, for tests. */
  val nodeCount: Int
    get() = size

  /** The root node, for searches. */
  val root: Int
    get() = ROOT

  /** Adds [word], in lowercase, with [count], or sets its count if it is in the list. */
  fun put(word: String, count: Long) {
    if (word.isEmpty() || count <= 0) return
    var node = ROOT
    for (c in word) {
      node = childOrAdd(node, c)
    }
    counts[node] = count
  }

  /** Removes [word] from the list, which keeps its nodes. */
  fun remove(word: String) {
    val node = find(word)
    if (node != NONE) counts[node] = 0
  }

  /** The node of [prefix], or [NONE]. */
  fun find(prefix: String): Int {
    var node = ROOT
    for (c in prefix) {
      node = child(node, c)
      if (node == NONE) return NONE
    }
    return node
  }

  /** The count of [word], or 0 if it isn't a word of the list. */
  fun count(word: String): Long {
    val node = find(word)
    return if (node == NONE) 0 else counts[node]
  }

  operator fun contains(word: String): Boolean = count(word) > 0

  /** The form with capitals that [word] is listed in, such as "London", or null. */
  fun capitalForm(word: String): String? {
    val node = find(word)
    return if (node == NONE) null else capitals[node]
  }

  fun child(node: Int, c: Char): Int {
    var child = firstChild[node]
    while (child != NONE) {
      if (chars[child] == c) return child
      child = nextSibling[child]
    }
    return NONE
  }

  fun firstChild(node: Int): Int = firstChild[node]

  fun nextSibling(node: Int): Int = nextSibling[node]

  fun charOf(node: Int): Char = chars[node]

  /** The count of the word ending at [node], or 0 if no word ends there. */
  fun countAt(node: Int): Long = counts[node]

  private fun childOrAdd(node: Int, c: Char): Int {
    val existing = child(node, c)
    if (existing != NONE) return existing
    if (size == chars.size) grow()
    val added = size++
    chars[added] = c
    firstChild[added] = NONE
    nextSibling[added] = firstChild[node]
    counts[added] = 0
    firstChild[node] = added
    return added
  }

  private fun grow() {
    val capacity = chars.size * 2
    chars = chars.copyOf(capacity)
    firstChild = firstChild.copyOf(capacity)
    nextSibling = nextSibling.copyOf(capacity)
    counts = counts.copyOf(capacity)
  }

  /** Frees most of the room left at the end of the arrays when loading is done. */
  private fun trim() {
    val capacity = size + SPARE_NODES
    if (capacity >= chars.size) return
    chars = chars.copyOf(capacity)
    firstChild = firstChild.copyOf(capacity)
    nextSibling = nextSibling.copyOf(capacity)
    counts = counts.copyOf(capacity)
  }

  companion object {
    const val NONE = -1
    private const val ROOT = 0
    private const val INITIAL_NODES = 1 shl 16

    /** Room left after loading for the words the user adds. */
    private const val SPARE_NODES = 4_096

    /** The most words read from a list, and the longest word kept. */
    const val MAX_WORDS = 150_000
    const val MAX_WORD_LENGTH = 40

    /** The words from the most frequent down to this rank are common words. */
    const val COMMON_RANK = 5_000

    /**
     * Reads a word list of [language]: a word, a tab and its count per line, most frequent first.
     * Words with characters other than letters, marks and apostrophes are left out.
     */
    fun read(reader: BufferedReader, language: WordLanguage): WordList {
      val list = WordList()
      var words = 0
      while (words < MAX_WORDS) {
        val line = reader.readLine() ?: break
        val tab = line.indexOf('\t')
        if (tab <= 0 || tab > MAX_WORD_LENGTH) continue
        val original = line.substring(0, tab).trim()
        val count = line.substring(tab + 1).trim().toLongOrNull() ?: continue
        if (count <= 0 || !isListWord(original)) continue
        val word = language.lowercase(original)
        if (word.length > MAX_WORD_LENGTH) continue
        if (word in list) continue
        list.put(word, count)
        if (original != word) list.capitals[list.find(word)] = original
        words++
        if (words == COMMON_RANK) list.commonCount = count
      }
      if (words < COMMON_RANK) list.commonCount = Long.MAX_VALUE
      list.trim()
      return list
    }

    /** An empty list, for tests. */
    fun empty(): WordList = WordList()

    /** Whether [word] has only letters, marks and apostrophes, starting with a letter. */
    fun isListWord(word: String): Boolean =
      word.isNotEmpty() && word[0].isLetter() && word.all { isWordCharacter(it) }

    /** Letters, combining marks and the apostrophe make up words. */
    fun isWordCharacter(c: Char): Boolean =
      c.isLetter() || c == '\'' || Character.getType(c) == Character.NON_SPACING_MARK.toInt()
  }
}
