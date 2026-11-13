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

package com.google.android.accessibility.talkback.granularediting

import java.text.BreakIterator
import java.util.Locale

/** How granular editing splits text into pieces. */
enum class Granularity(
  /** What goes between a piece and text inserted before or after it. */
  val separator: String
) {
  CHARACTERS(""),
  WORDS(" "),
  SENTENCES(" "),
  LINES("\n"),
  PARAGRAPHS("\n\n");

  companion object {
    /** The granularity called [name], or [WORDS] when there is none. */
    fun fromName(name: String?): Granularity = values().firstOrNull { it.name == name } ?: WORDS
  }
}

/**
 * A piece of the text: [coreStart] to [coreEnd] is what is shown and edited. For lines, [coreEnd]
 * to [end] is the line break that ends the line, which goes with the line when it is deleted;
 * otherwise [start] is [coreStart] and [end] is [coreEnd]. The pieces of a text cover all of it,
 * in order, white space included: white space between words, sentences and paragraphs is a piece
 * of its own, and a blank line is a line with no text.
 */
data class Piece(val start: Int, val coreStart: Int, val coreEnd: Int, val end: Int) {
  val isBlank: Boolean
    get() = coreStart == coreEnd
}

/** A kind of white space character, for naming white space pieces. */
enum class WhiteSpace {
  SPACE,
  TAB,
  NEW_LINE,
}

/** Splits text into pieces and edits it a piece at a time. */
object TextPieces {

  fun split(text: String, granularity: Granularity, locale: Locale = Locale.getDefault()): List<Piece> {
    if (text.isEmpty()) return emptyList()
    val bounds =
      when (granularity) {
        // Each character, white space included, is a piece of its own.
        Granularity.CHARACTERS ->
          return breaks(text, BreakIterator.getCharacterInstance(locale)).zipWithNext { a, b ->
            Piece(a, a, b, b)
          }
        Granularity.LINES -> return lines(text)
        Granularity.WORDS -> breaks(text, BreakIterator.getWordInstance(locale))
        // A sentence also ends at the end of a line, such as a heading's.
        Granularity.SENTENCES ->
          (breaks(text, BreakIterator.getSentenceInstance(locale)) + lineBreaks(text))
            .distinct()
            .sorted()
        Granularity.PARAGRAPHS -> paragraphBreaks(text)
      }
    return separateWhiteSpace(text, bounds)
  }

  /** Whether [piece] is white space only, such as the space between two words. */
  fun isWhiteSpace(text: String, piece: Piece): Boolean =
    !piece.isBlank && (piece.coreStart until piece.coreEnd).all { text[it].isWhitespace() }

  /**
   * The white space in [white], in order, as runs of one kind and their lengths. A carriage return
   * and line feed together are one new line.
   */
  fun whiteSpaceRuns(white: String): List<Pair<WhiteSpace, Int>> {
    val runs = ArrayList<Pair<WhiteSpace, Int>>()
    var index = 0
    while (index < white.length) {
      val char = white[index]
      val kind =
        when (char) {
          '\t' -> WhiteSpace.TAB
          '\n',
          '\r',
          '\u000B',
          '\u000C',
          '\u0085',
          '\u2028',
          '\u2029' -> WhiteSpace.NEW_LINE
          else -> WhiteSpace.SPACE
        }
      index += if (char == '\r' && white.getOrNull(index + 1) == '\n') 2 else 1
      if (runs.lastOrNull()?.first == kind) {
        runs[runs.size - 1] = kind to runs.last().second + 1
      } else {
        runs += kind to 1
      }
    }
    return runs
  }

  /**
   * The text with the shown part of [piece] replaced by [replacement]. A line break at the end of a
   * replacement for a line is dropped, as the line already has a line break of its own.
   */
  fun replace(
    text: String,
    piece: Piece,
    replacement: String,
    granularity: Granularity? = null,
  ): String {
    val shown =
      if (granularity == Granularity.LINES) {
        replacement.removeSuffix("\n").removeSuffix("\r")
      } else {
        replacement
      }
    return text.replaceRange(piece.coreStart, piece.coreEnd, shown)
  }

  /**
   * The text without [pieces]. Deleting the last line, which has no line break of its own, also
   * deletes the line break before it, which would otherwise leave a blank line in its place.
   */
  fun delete(text: String, pieces: Collection<Piece>, granularity: Granularity): String {
    val ranges = pieces.map { it.start to it.end }.sortedBy { it.first }
    val merged = ArrayList<Pair<Int, Int>>()
    for (range in ranges) {
      val last = merged.lastOrNull()
      if (last != null && range.first <= last.second) {
        merged[merged.size - 1] = last.first to maxOf(last.second, range.second)
      } else {
        merged += range
      }
    }
    val lastLine = pieces.any { it.end == text.length && it.coreEnd == it.end }
    if (granularity == Granularity.LINES && lastLine && merged.isNotEmpty()) {
      val (from, to) = merged.last()
      merged[merged.size - 1] = from - lineBreakBefore(text, from) to to
    }
    val builder = StringBuilder(text)
    merged.asReversed().forEach { (from, to) -> builder.delete(from, to) }
    return builder.toString()
  }

  /**
   * Inserts [inserted] before [piece], with the granularity's separator between them unless there
   * is white space there already.
   */
  fun insertBefore(text: String, piece: Piece, inserted: String, granularity: Granularity): String =
    if (granularity == Granularity.LINES) {
      text.substring(0, piece.start) + inserted + granularity.separator + text.substring(piece.start)
    } else {
      insertAt(text, piece.coreStart, inserted, granularity)
    }

  /**
   * Inserts [inserted] after [piece], with the granularity's separator between them unless there
   * is white space there already. A line goes on a new line after the line.
   */
  fun insertAfter(text: String, piece: Piece, inserted: String, granularity: Granularity): String =
    if (granularity == Granularity.LINES) {
      text.substring(0, piece.coreEnd) + granularity.separator + inserted + text.substring(piece.coreEnd)
    } else {
      insertAt(text, piece.coreEnd, inserted, granularity)
    }

  /** The text at the end of [text], after the granularity's separator if there is text before it. */
  fun append(text: String, inserted: String, granularity: Granularity): String =
    if (text.isEmpty() || text.last().isWhitespace()) text + inserted
    else text + granularity.separator + inserted

  /**
   * The text of [pieces], in the order of the text, as it is in the text. Pieces that aren't next
   * to each other in the text are joined by the granularity's separator, unless one of them ends
   * or starts with white space. The last piece's line break is left out.
   */
  fun join(text: String, pieces: Collection<Piece>, granularity: Granularity): String {
    val sorted = pieces.sortedBy { it.start }
    val builder = StringBuilder()
    sorted.forEachIndexed { index, piece ->
      if (index > 0) {
        val previous = sorted[index - 1]
        val joined = previous.end == piece.start
        val white =
          builder.lastOrNull()?.isWhitespace() != false ||
            text.getOrNull(piece.coreStart)?.isWhitespace() != false
        if (!joined && !white) builder.append(granularity.separator)
      }
      builder.append(text, piece.coreStart, piece.coreEnd)
      if (index < sorted.size - 1) builder.append(text, piece.coreEnd, piece.end)
    }
    return builder.toString()
  }

  /** Replaces every [find] in [text] with [replacement], ignoring case, and counts them. */
  fun replaceAll(text: String, find: String, replacement: String): Pair<String, Int> {
    if (find.isEmpty()) return text to 0
    val regex = Regex(Regex.escape(find), RegexOption.IGNORE_CASE)
    val count = regex.findAll(text).count()
    return text.replace(regex, Regex.escapeReplacement(replacement)) to count
  }

  private fun breaks(text: String, iterator: BreakIterator): List<Int> {
    iterator.setText(text)
    val bounds = ArrayList<Int>()
    var at = iterator.first()
    while (at != BreakIterator.DONE) {
      bounds += at
      at = iterator.next()
    }
    return bounds
  }

  /** Each line ends after its line break. */
  private fun lineBreaks(text: String): List<Int> {
    val bounds = arrayListOf(0)
    text.forEachIndexed { index, char -> if (char == '\n') bounds += index + 1 }
    if (bounds.last() != text.length) bounds += text.length
    return bounds
  }

  /** Each paragraph ends after a blank line, that is two line breaks with only white space between. */
  private fun paragraphBreaks(text: String): List<Int> {
    val bounds = arrayListOf(0)
    Regex("\\n[ \\t\\r]*\\n\\s*").findAll(text).forEach { bounds += it.range.last + 1 }
    if (bounds.last() != text.length) bounds += text.length
    return bounds.distinct()
  }

  /**
   * Turns the segments between [bounds] into pieces, with the white space at their ends as pieces
   * of their own. White space next to white space is one piece.
   */
  private fun separateWhiteSpace(text: String, bounds: List<Int>): List<Piece> {
    val parts = ArrayList<Pair<Int, Int>>()
    fun add(from: Int, to: Int) {
      if (from >= to) return
      val last = parts.lastOrNull()
      val white = text[from].isWhitespace()
      if (last != null && white && text[last.first].isWhitespace() && last.second == from) {
        parts[parts.size - 1] = last.first to to
      } else {
        parts += from to to
      }
    }
    for ((segmentStart, segmentEnd) in bounds.zipWithNext()) {
      var coreStart = segmentStart
      var coreEnd = segmentEnd
      while (coreStart < coreEnd && text[coreStart].isWhitespace()) coreStart++
      while (coreEnd > coreStart && text[coreEnd - 1].isWhitespace()) coreEnd--
      if (coreStart == coreEnd) {
        add(segmentStart, segmentEnd)
        continue
      }
      add(segmentStart, coreStart)
      add(coreStart, coreEnd)
      add(coreEnd, segmentEnd)
    }
    return parts.map { (from, to) -> Piece(from, from, to, to) }
  }

  /**
   * Each line as it is, leading and trailing spaces included, with its line break after the shown
   * part. A text that ends with a line break ends with a blank line.
   */
  private fun lines(text: String): List<Piece> {
    val pieces = ArrayList<Piece>()
    val bounds = lineBreaks(text)
    for ((lineStart, lineEnd) in bounds.zipWithNext()) {
      var coreEnd = lineEnd
      if (coreEnd > lineStart && text[coreEnd - 1] == '\n') {
        coreEnd--
        if (coreEnd > lineStart && text[coreEnd - 1] == '\r') coreEnd--
      }
      pieces += Piece(lineStart, lineStart, coreEnd, lineEnd)
    }
    if (text.endsWith('\n')) pieces += Piece(text.length, text.length, text.length, text.length)
    return pieces
  }

  /** The length of the line break that ends just before [offset]: 2 for "\r\n", 1, or 0. */
  private fun lineBreakBefore(text: String, offset: Int): Int =
    when {
      offset >= 2 && text[offset - 1] == '\n' && text[offset - 2] == '\r' -> 2
      offset >= 1 && text[offset - 1] == '\n' -> 1
      else -> 0
    }

  /**
   * Inserts [inserted] at [offset], with the granularity's separator on each side where the text
   * next to it isn't white space. For words, no separator goes before closing punctuation or after
   * opening punctuation.
   */
  private fun insertAt(text: String, offset: Int, inserted: String, granularity: Granularity): String {
    val before = text.getOrNull(offset - 1)
    val after = text.getOrNull(offset)
    val words = granularity == Granularity.WORDS
    val left = before != null && !before.isWhitespace() && !(words && isOpening(before))
    val right = after != null && !after.isWhitespace() && !(words && isClosing(after))
    return text.substring(0, offset) +
      (if (left) granularity.separator else "") +
      inserted +
      (if (right) granularity.separator else "") +
      text.substring(offset)
  }

  private fun isOpening(char: Char): Boolean =
    when (Character.getType(char).toByte()) {
      Character.START_PUNCTUATION,
      Character.INITIAL_QUOTE_PUNCTUATION -> true
      else -> false
    }

  private fun isClosing(char: Char): Boolean =
    when (Character.getType(char).toByte()) {
      Character.END_PUNCTUATION,
      Character.FINAL_QUOTE_PUNCTUATION,
      Character.OTHER_PUNCTUATION -> true
      else -> false
    }
}
