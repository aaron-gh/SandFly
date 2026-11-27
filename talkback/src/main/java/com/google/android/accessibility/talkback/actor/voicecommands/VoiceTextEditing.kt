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

package com.google.android.accessibility.talkback.actor.voicecommands

import com.google.android.accessibility.talkback.granularediting.Granularity
import com.google.android.accessibility.talkback.granularediting.TextPieces
import java.text.BreakIterator
import java.util.Locale
import java.util.regex.Pattern

/** What a text editing voice command does with the text it names. */
enum class TextOperation {
  /** Moves the cursor and says the text passed over. */
  MOVE,
  SELECT,
  COPY,
  CUT,
  DELETE,
}

/** The pieces of text that text editing voice commands count. */
enum class TextUnit {
  CHARACTER,
  WORD,
  SENTENCE,
  LINE,
  PARAGRAPH,
  /** The whole text of the field, for its start and end. */
  TEXT,
}

/** The text a text editing voice command acts on. */
sealed class TextTarget {
  /** From the cursor over [count] units, forward or back. */
  data class Units(val unit: TextUnit, val count: Int, val forward: Boolean) : TextTarget()

  /** From the cursor to the start, or the [end], of a unit. */
  data class Boundary(val unit: TextUnit, val end: Boolean) : TextTarget()

  /** The occurrence of [text] in the field nearest to the cursor. */
  data class Phrase(val text: String) : TextTarget()
}

/**
 * The ranges of text that text editing voice commands act on, from the text of the field and its
 * selection, without Android.
 */
object VoiceTextRanges {
  /** What a text editing voice command does in the text. */
  sealed class Plan {
    /** Moves the cursor from [from] to [to]. */
    data class Move(val from: Int, val to: Int) : Plan()

    /** Selects, copies, cuts or deletes [start] to [end]. */
    data class Edit(val start: Int, val end: Int) : Plan()

    /** Nothing to act on: the cursor is at the start, or the [end], of the text. */
    data class AtEdge(val end: Boolean) : Plan()

    /** The phrase isn't in the text. */
    data object NotFound : Plan()
  }

  /** The most units a voice command counts. */
  const val MAX_COUNT = 1_000

  /** Anything between the words of a phrase in the text: not a letter or digit. */
  private const val GAP = "[^\\p{L}\\p{N}]+"

  /**
   * What [operation] on [target] does in [text], whose selection is [selectionStart] to
   * [selectionEnd]; a cursor is a selection with both the same. A selection of -1 is the end of the
   * text. Moves forward start at the end of the selection, moves back at its start.
   */
  @JvmStatic
  fun plan(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    operation: TextOperation,
    target: TextTarget,
    locale: Locale,
  ): Plan {
    val (low, high) = selection(text, selectionStart, selectionEnd)
    if (target is TextTarget.Phrase) {
      if (operation == TextOperation.MOVE) return Plan.NotFound
      val found = findPhrase(text, target.text, high) ?: return Plan.NotFound
      return Plan.Edit(found.first, found.second)
    }
    val forward =
      when (target) {
        is TextTarget.Units -> target.forward
        is TextTarget.Boundary -> target.end
        is TextTarget.Phrase -> true
      }
    val from = if (forward) high else low
    val to = destination(text, from, target, locale) ?: return Plan.AtEdge(end = forward)
    if (to == from) return Plan.AtEdge(end = forward)
    if (operation == TextOperation.MOVE) return Plan.Move(from, to)
    var start = minOf(from, to)
    var end = maxOf(from, to)
    val unit =
      when (target) {
        is TextTarget.Units -> target.unit
        is TextTarget.Boundary -> target.unit
        is TextTarget.Phrase -> TextUnit.TEXT
      }
    if (unit != TextUnit.CHARACTER) {
      if (operation == TextOperation.DELETE) {
        if (target is TextTarget.Units) {
          val tidied = tidyDeletion(text, start, end, unit)
          start = tidied.first
          end = tidied.second
        }
      } else {
        // Selected, copied and cut text is the words, without the white space around them.
        var trimmedStart = start
        var trimmedEnd = end
        while (trimmedStart < trimmedEnd && text[trimmedStart].isWhitespace()) trimmedStart++
        while (trimmedEnd > trimmedStart && text[trimmedEnd - 1].isWhitespace()) trimmedEnd--
        if (trimmedStart < trimmedEnd) {
          start = trimmedStart
          end = trimmedEnd
        }
      }
    }
    return Plan.Edit(start, end)
  }

  /** The selection in [text] in order, with -1 or one past the text as the end of the text. */
  private fun selection(text: String, selectionStart: Int, selectionEnd: Int): Pair<Int, Int> {
    fun clamp(index: Int) = if (index < 0 || index > text.length) text.length else index
    val start = clamp(selectionStart)
    val end = clamp(selectionEnd)
    return minOf(start, end) to maxOf(start, end)
  }

  /**
   * Where the cursor at [from] goes for [target]: the end of the [TextTarget.Units.count]th unit
   * that ends after it, or the start of the one that starts before it, going back. A start or end
   * goes to the start of the unit the cursor is in or after, or the end of the unit it is in or
   * before; at that start or end already, the one before or after. Null when there is no unit
   * there.
   */
  @JvmStatic
  fun destination(text: String, from: Int, target: TextTarget, locale: Locale): Int? =
    when (target) {
      is TextTarget.Units -> {
        val count = target.count.coerceIn(1, MAX_COUNT)
        when {
          target.unit == TextUnit.CHARACTER -> characters(text, from, count, target.forward, locale)
          target.forward -> {
            val after = units(text, target.unit, locale).filter { it.second > from }
            after.getOrNull(minOf(count, after.size) - 1)?.second
          }
          else -> {
            val before = units(text, target.unit, locale).filter { it.first < from }
            before.getOrNull(before.size - minOf(count, before.size))?.first
          }
        }
      }
      is TextTarget.Boundary -> {
        val units = units(text, target.unit, locale)
        if (target.end) units.firstOrNull { it.second > from }?.second
        else units.lastOrNull { it.first < from }?.first
      }
      is TextTarget.Phrase -> null
    }

  /**
   * The offset [count] characters after [from], or before it, or the end or start of [text] when
   * there are fewer. Null when there are none. Only the characters passed over are read, so that
   * a long text isn't split into its characters.
   */
  private fun characters(text: String, from: Int, count: Int, forward: Boolean, locale: Locale): Int? {
    if (text.isEmpty()) return null
    val iterator = BreakIterator.getCharacterInstance(locale)
    iterator.setText(text)
    val start = from.coerceIn(0, text.length)
    var at = start
    for (i in 0 until count) {
      val next = if (forward) iterator.following(at) else iterator.preceding(at)
      if (next == BreakIterator.DONE) break
      at = next
    }
    return if (at == start) null else at
  }

  /**
   * The units of [text], as their start and end, in order: characters with white space, words
   * (pieces with a letter or digit), sentences and paragraphs without the white space between them,
   * lines without their line breaks, blank lines included, and all of the text.
   */
  @JvmStatic
  fun units(text: String, unit: TextUnit, locale: Locale): List<Pair<Int, Int>> {
    if (text.isEmpty()) return emptyList()
    return when (unit) {
      TextUnit.CHARACTER -> TextPieces.split(text, Granularity.CHARACTERS, locale).map { it.coreStart to it.coreEnd }
      TextUnit.WORD ->
        TextPieces.split(text, Granularity.WORDS, locale)
          .flatMap { piece -> splitAtSentenceEnds(text, piece.coreStart, piece.coreEnd) }
          .filter { (start, end) -> (start until end).any { text[it].isLetterOrDigit() } }
      TextUnit.SENTENCE ->
        TextPieces.split(text, Granularity.SENTENCES, locale)
          .filterNot { TextPieces.isWhiteSpace(text, it) }
          .map { it.coreStart to it.coreEnd }
      TextUnit.LINE -> TextPieces.split(text, Granularity.LINES, locale).map { it.coreStart to it.coreEnd }
      TextUnit.PARAGRAPH ->
        TextPieces.split(text, Granularity.PARAGRAPHS, locale)
          .filterNot { TextPieces.isWhiteSpace(text, it) }
          .map { it.coreStart to it.coreEnd }
      TextUnit.TEXT -> listOf(0 to text.length)
    }
  }

  /**
   * [start] to [end] of [text], split after each full stop, question mark or exclamation mark
   * between a letter and a capital letter, as in "one.Two", which text with no space after its
   * sentences has, and which Android reads as one word.
   */
  private fun splitAtSentenceEnds(text: String, start: Int, end: Int): List<Pair<Int, Int>> {
    val parts = ArrayList<Pair<Int, Int>>()
    var from = start
    for (index in start + 1 until end - 1) {
      if (
        text[index] in SENTENCE_ENDS && text[index - 1].isLetter() && text[index + 1].isUpperCase()
      ) {
        parts += from to index + 1
        from = index + 1
      }
    }
    parts += from to end
    return parts
  }

  private const val SENTENCE_ENDS = ".?!"

  /**
   * [start] to [end], deleted whole units, with the white space that would be left over: the white
   * space after them when the text before them is white space or the start, else the white space
   * before them. A range with white space at either end already is left as it is.
   */
  private fun tidyDeletion(text: String, start: Int, end: Int, unit: TextUnit): Pair<Int, Int> {
    if (start >= end || text[start].isWhitespace() || text[end - 1].isWhitespace()) return start to end
    if (start > 0 && !text[start - 1].isWhitespace()) return start to end
    val horizontal = { c: Char -> c == ' ' || c == '\t' || c == ' ' }
    return when (unit) {
      TextUnit.LINE -> {
        val after =
          when {
            text.startsWith("\r\n", end) -> 2
            text.startsWith("\n", end) -> 1
            else -> 0
          }
        if (after > 0) return start to end + after
        val before =
          when {
            start >= 2 && text[start - 1] == '\n' && text[start - 2] == '\r' -> 2
            start >= 1 && text[start - 1] == '\n' -> 1
            else -> 0
          }
        start - before to end
      }
      TextUnit.PARAGRAPH -> {
        var after = end
        while (after < text.length && text[after].isWhitespace()) after++
        if (after > end) return start to after
        var before = start
        while (before > 0 && text[before - 1].isWhitespace()) before--
        before to end
      }
      else -> {
        var after = end
        while (after < text.length && horizontal(text[after])) after++
        if (after > end) return start to after
        // Before a line break, punctuation or the end of the text, the space before them goes.
        var before = start
        while (before > 0 && horizontal(text[before - 1])) before--
        before to end
      }
    }
  }

  /**
   * The occurrence of [phrase] in [text] nearest to [cursor], ignoring case, with any characters
   * that aren't letters or digits between its words: whole words first, else inside words. Of two
   * as near, the one before the cursor. Null when there is none.
   */
  @JvmStatic
  fun findPhrase(text: String, phrase: String, cursor: Int): Pair<Int, Int>? {
    val words =
      phrase.trim().split(Regex("\\s+")).map(VoiceGrammar::trimPunctuation).filter { it.isNotEmpty() }
    if (words.isEmpty() || text.isEmpty()) return null
    val body =
      words.joinToString(GAP) { word ->
        word.split(Regex("['’]")).joinToString("['’]?") { if (it.isEmpty()) "" else Pattern.quote(it) }
      }
    val flags = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    val whole = Pattern.compile("(?<![\\p{L}\\p{N}])(?:$body)(?![\\p{L}\\p{N}])", flags)
    return nearest(text, whole, cursor) ?: nearest(text, Pattern.compile(body, flags), cursor)
  }

  private fun nearest(text: String, pattern: Pattern, cursor: Int): Pair<Int, Int>? {
    val matcher = pattern.matcher(text)
    var best: Pair<Int, Int>? = null
    var bestDistance = Int.MAX_VALUE
    while (matcher.find()) {
      val start = matcher.start()
      val end = matcher.end()
      if (start == end) continue
      val distance =
        when {
          cursor in start..end -> 0
          cursor < start -> start - cursor
          else -> cursor - end
        }
      // Matches come in order, so of two as near the one before the cursor is kept.
      if (distance < bestDistance) {
        best = start to end
        bestDistance = distance
      }
      if (start > cursor && distance > bestDistance) break
    }
    return best
  }
}
