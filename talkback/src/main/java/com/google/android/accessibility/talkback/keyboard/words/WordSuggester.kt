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

import java.util.PriorityQueue
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Suggestions and corrections for a word typed on a physical keyboard, from a [WordList] and the
 * words the user typed ([LearnedWords]).
 *
 * Corrections come from an edit distance search through the list's trie, where the mistakes made
 * on a keyboard cost less than others: a neighbouring key ([KeyLayout]), a letter typed twice or
 * once for two, two letters swapped, a missing apostrophe, a letter without its accent. Completions
 * are the words starting with the typed letters. Both are ranked by the words' counts, less a part
 * for their cost and for the letters a completion adds.
 */
class WordSuggester(
  val language: WordLanguage,
  private val words: WordList,
  /** The words the user typed, which [WordData] saves. */
  val learned: LearnedWords,
) {
  /** The keyboard's layout, for neighbouring keys. */
  var layout: KeyLayout = language.defaultLayout

  /** A word found for a typed word: its edit cost, the letters it adds, and its rank. */
  class Candidate(val word: String, val cost: Float, val added: Int, val count: Long) {
    val score: Double =
      FREQUENCY_WEIGHT * ln(max(count, 1L).toDouble()) - COST_WEIGHT * cost - ADDED_WEIGHT * added

    override fun toString(): String = "$word($cost, $added, $count)"
  }

  init {
    for (word in learned.addedWords()) {
      if (word !in words) words.put(word, addedCount())
    }
  }

  /**
   * Up to [max] words for [typed], corrections and completions, best first, with the capitals
   * [typed] has, and without [typed] itself.
   */
  fun suggestions(typed: String, max: Int = MAX_SUGGESTIONS): List<String> {
    if (!isTypedWord(typed)) return emptyList()
    val lower = language.lowercase(normalized(typed))
    val best = HashMap<String, Candidate>()
    fun offer(candidate: Candidate) {
      if (candidate.word == lower) return
      val previous = best[candidate.word]
      if (previous == null || candidate.score > previous.score) best[candidate.word] = candidate
    }
    if (lower.length >= 2) corrections(lower).forEach(::offer)
    completions(lower, max * 2).forEach(::offer)
    val fixed = language.fixedCapitalization(lower)
    val result = LinkedHashSet<String>()
    if (fixed != null && fixed != typed) result += fixed
    best.values
      .sortedByDescending { it.score }
      .map { format(it.word, typed) }
      .filterTo(result) { it != typed }
    return result.take(max)
  }

  /**
   * The word [typed] should be replaced with when it ends, or null to leave it. [sentenceStart] is
   * whether it starts a sentence, where a capital doesn't make it a name.
   */
  fun correction(typed: String, sentenceStart: Boolean): String? {
    if (!isTypedWord(typed)) return null
    val lower = language.lowercase(normalized(typed))
    // Words always written with a capital, such as English I and I'm.
    val fixed = language.fixedCapitalization(lower)
    if (fixed != null && typed == lower && fixed != typed) return fixed
    if (learned.isAdded(lower)) return null
    // Capitals after the first letter, as in acronyms, names such as McDonald and words typed in
    // capitals, are as meant.
    if (typed.drop(1).any { it.isUpperCase() }) return null
    // A capital within a sentence makes a name.
    if (typed[0].isUpperCase() && !sentenceStart) return null
    val typedCount = effectiveCount(lower)
    val candidates = corrections(lower).filter { it.word != lower }
    if (candidates.isEmpty()) return null
    // A missing apostrophe, as in dont and im, where the word with it is more common.
    val bare = lower.replace("'", "")
    candidates
      .filter { it.word != lower && it.word.replace("'", "") == bare }
      .maxByOrNull { it.count }
      ?.let { if (it.count >= typedCount * APOSTROPHE_RATIO) return format(it.word, typed) }
    if (lower.length < MIN_CORRECTED_LENGTH) return null
    if (typedCount >= words.commonCount) return null
    val best = candidates.maxBy { it.score }
    if (typedCount > 0) {
      if (best.count < typedCount * requiredRatio(best.cost)) return null
    } else if (best.cost > maxUnlistedCost(lower.length)) {
      // A word not in the list, such as a name, is corrected only for one mistake.
      return null
    }
    return format(best.word, typed)
  }

  /** Whether [word], in any case, is a word of the list or one the user added. */
  fun isWord(word: String): Boolean = language.lowercase(normalized(word)) in words

  /** Counts a use of [word], typed or chosen, so that it ranks higher. */
  fun recordUse(word: String) {
    val lower = language.lowercase(normalized(word))
    if (lower in words) learned.recordUse(lower)
  }

  /** Adds [word], whose correction the user undid, so that it is never corrected again. */
  fun addWord(word: String) {
    val lower = language.lowercase(normalized(word))
    if (!WordList.isListWord(lower)) return
    learned.add(lower)
    if (lower !in words) words.put(lower, addedCount())
  }

  /** [candidate], a word of the list in lowercase, with the capitals of [typed]. */
  fun format(candidate: String, typed: String): String {
    val letters = typed.filter { it.isLetter() }
    return when {
      letters.length >= 2 && letters.all { it.isUpperCase() } -> language.uppercase(candidate)
      typed.firstOrNull()?.isUpperCase() == true ->
        language.capitalize(words.capitalForm(candidate) ?: candidate)
      else ->
        words.capitalForm(candidate) ?: language.fixedCapitalization(candidate) ?: candidate
    }
  }

  /** The corrections of [lower] within the cost its length allows. */
  fun corrections(lower: String): List<Candidate> {
    val typed = lower.toCharArray()
    val n = typed.size
    val maxCost = maxCost(n)
    val maxDepth = n + MAX_ADDED_BY_CORRECTION
    val rows = Array(maxDepth + 1) { FloatArray(n + 1) }
    val path = CharArray(maxDepth)
    for (j in 1..n) rows[0][j] = rows[0][j - 1] + deleteCost(typed, j - 1)
    val found = ArrayList<Candidate>()
    var visited = 0

    fun visit(node: Int, depth: Int) {
      var child = words.firstChild(node)
      while (child != WordList.NONE) {
        if (++visited > MAX_VISITED) return
        val c = words.charOf(child)
        val previous = rows[depth]
        val row = rows[depth + 1]
        val before = if (depth > 0) path[depth - 1] else NO_CHAR
        row[0] = previous[0] + insertCost(c, before)
        var rowMin = row[0]
        for (j in 1..n) {
          val t = typed[j - 1]
          var cost = previous[j - 1] + substituteCost(t, c)
          cost = min(cost, previous[j] + insertCost(c, before))
          cost = min(cost, row[j - 1] + deleteCost(typed, j - 1))
          if (depth > 0 && j > 1 && t == before && typed[j - 2] == c && t != c) {
            cost = min(cost, rows[depth - 1][j - 2] + TRANSPOSE_COST)
          }
          row[j] = cost
          if (cost < rowMin) rowMin = cost
        }
        path[depth] = c
        val count = words.countAt(child)
        if (count > 0 && row[n] <= maxCost) {
          val word = String(path, 0, depth + 1)
          found += Candidate(word, row[n], 0, effectiveCount(word, count))
        }
        if (rowMin <= maxCost && depth + 1 < maxDepth) visit(child, depth + 1)
        child = words.nextSibling(child)
      }
    }

    visit(words.root, 0)
    return found
  }

  /** Up to [max] words starting with [lower], longer than it, best first. */
  fun completions(lower: String, max: Int): List<Candidate> {
    val start = words.find(lower)
    if (start == WordList.NONE) return emptyList()
    val top = PriorityQueue<Candidate>(max + 1, compareBy { it.score })
    val path = StringBuilder(lower)
    var visited = 0

    fun visit(node: Int, added: Int) {
      var child = words.firstChild(node)
      while (child != WordList.NONE) {
        if (++visited > MAX_VISITED) return
        path.append(words.charOf(child))
        val count = words.countAt(child)
        if (count > 0) {
          val word = path.toString()
          top += Candidate(word, 0f, added + 1, effectiveCount(word, count))
          if (top.size > max) top.poll()
        }
        if (added + 1 < MAX_COMPLETION_LETTERS) visit(child, added + 1)
        path.setLength(path.length - 1)
        child = words.nextSibling(child)
      }
    }

    visit(start, 0)
    return top.sortedByDescending { it.score }
  }

  private fun substituteCost(typed: Char, word: Char): Float =
    when {
      typed == word -> 0f
      language.variants[typed]?.contains(word) == true -> VARIANT_COST
      language.variants[word]?.contains(typed) == true -> VARIANT_COST
      layout.areNeighbours(typed, word) -> NEIGHBOUR_COST
      else -> 1f
    }

  /** The cost of [c], in the word, missing from what was typed after [before]. */
  private fun insertCost(c: Char, before: Char): Float =
    when (c) {
      '\'' -> APOSTROPHE_COST
      before -> DOUBLE_COST
      else -> 1f
    }

  /** The cost of the typed character at [index] not being in the word. */
  private fun deleteCost(typed: CharArray, index: Int): Float {
    val c = typed[index]
    val before = if (index > 0) typed[index - 1] else NO_CHAR
    val after = if (index + 1 < typed.size) typed[index + 1] else NO_CHAR
    return when {
      c == '\'' -> APOSTROPHE_COST
      c == before -> DOUBLE_COST
      layout.areNeighbours(c, before) || layout.areNeighbours(c, after) -> EXTRA_NEIGHBOUR_COST
      else -> 1f
    }
  }

  private fun effectiveCount(word: String, listCount: Long = words.count(word)): Long {
    if (listCount <= 0) return 0
    return listCount + learned.uses(word).toLong() * useBoost()
  }

  /** A use adds half the count of the common words, so that a few uses rank a word high. */
  private fun useBoost(): Long = commonOrDefault() / 2

  /** Words the user added count as much as the least common of the common words. */
  private fun addedCount(): Long = commonOrDefault()

  private fun commonOrDefault(): Long =
    if (words.commonCount == Long.MAX_VALUE) DEFAULT_COMMON_COUNT else words.commonCount

  companion object {
    const val MAX_SUGGESTIONS = 8

    private const val FREQUENCY_WEIGHT = 0.5
    private const val COST_WEIGHT = 3.0
    private const val ADDED_WEIGHT = 0.3

    private const val NEIGHBOUR_COST = 0.5f
    private const val EXTRA_NEIGHBOUR_COST = 0.6f
    private const val VARIANT_COST = 0.2f
    private const val DOUBLE_COST = 0.5f
    private const val APOSTROPHE_COST = 0.3f
    private const val TRANSPOSE_COST = 0.6f

    /**
     * How many more times a word with an apostrophe must be used than the same word typed without
     * it to replace it: im and dont are replaced, as 20 and 32 times rarer than I'm and don't in
     * the English list, hell and shell are not, as 4.3 and 4.7 times rarer than he'll and she'll.
     */
    private const val APOSTROPHE_RATIO = 15L

    /**
     * How many more times a correction of one mistake must be used than the typed word, where that
     * is in the list too, as word lists have common misspellings: in the English list, recieve,
     * wierd and definately are 63, 14 and 9.2 times rarer than their corrections, words such as
     * dessert, quiet and trail at most 4.7 times rarer than desert, quite and trial.
     */
    private const val CORRECTION_RATIO = 8.0

    /** Words shorter than this are corrected only for their apostrophe or capital. */
    private const val MIN_CORRECTED_LENGTH = 3

    private const val MAX_ADDED_BY_CORRECTION = 4
    private const val MAX_COMPLETION_LETTERS = 12

    /** The most trie nodes one search visits, so that a search ends quickly on any list. */
    private const val MAX_VISITED = 400_000

    private const val DEFAULT_COMMON_COUNT = 1_000L
    private const val NO_CHAR = '\u0000'

    /** The highest edit cost of a correction of a word of [length] letters. */
    fun maxCost(length: Int): Float =
      when {
        length <= 2 -> 0.6f
        length <= 4 -> 1f
        length <= 7 -> 1.6f
        else -> 2.1f
      }

    /**
     * How many times more common a correction of [cost] must be than the typed word, where that is
     * in the list: [CORRECTION_RATIO] for one mistake, ten times more for each mistake after it.
     */
    fun requiredRatio(cost: Float): Double =
      if (cost <= 1f) CORRECTION_RATIO else CORRECTION_RATIO * 10.0.pow((cost - 1f).toDouble())

    /** The highest cost of a correction of a word not in the list: one mistake, two in long words. */
    fun maxUnlistedCost(length: Int): Float = if (length >= 8) 1.6f else 1f

    /** [word] with the apostrophe of word lists, ', for the typographic one. */
    fun normalized(word: String): String = word.replace('\u2019', '\'')

    /** Whether [typed] is a word that suggestions and corrections are made for. */
    fun isTypedWord(typed: String): Boolean =
      typed.isNotEmpty() &&
        typed.length <= WordList.MAX_WORD_LENGTH &&
        WordList.isListWord(normalized(typed))
  }
}
