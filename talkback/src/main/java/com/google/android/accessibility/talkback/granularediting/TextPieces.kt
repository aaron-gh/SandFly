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
  PARAGRAPHS("\n\n"),
}

/**
 * A piece of the text: [coreStart] to [coreEnd] is what is shown and edited, and the white space
 * around it, up to [start] and [end], goes with it when it is deleted. The pieces of a text cover
 * all of it, in order.
 */
data class Piece(val start: Int, val coreStart: Int, val coreEnd: Int, val end: Int)

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
        Granularity.WORDS -> breaks(text, BreakIterator.getWordInstance(locale))
        // A sentence also ends at the end of a line, such as a heading's.
        Granularity.SENTENCES ->
          (breaks(text, BreakIterator.getSentenceInstance(locale)) + lineBreaks(text))
            .distinct()
            .sorted()
        Granularity.LINES -> lineBreaks(text)
        Granularity.PARAGRAPHS -> paragraphBreaks(text)
      }
    return attachWhiteSpace(text, bounds)
  }

  /** The text with the shown part of [piece] replaced by [replacement]. */
  fun replace(text: String, piece: Piece, replacement: String): String =
    text.replaceRange(piece.coreStart, piece.coreEnd, replacement)

  /**
   * The text without [pieces], and the white space that goes with them. Deleting up to the end of
   * the text also deletes the white space before, which would otherwise be left at the end.
   */
  fun delete(text: String, pieces: Collection<Piece>): String {
    val builder = StringBuilder(text)
    pieces.sortedByDescending { it.start }.forEach { builder.delete(it.start, it.end) }
    val toEnd = pieces.any { it.end == text.length }
    return if (toEnd) builder.trimEnd().toString() else builder.toString()
  }

  fun insertBefore(text: String, piece: Piece, inserted: String, granularity: Granularity): String =
    text.substring(0, piece.coreStart) + inserted + granularity.separator + text.substring(piece.coreStart)

  fun insertAfter(text: String, piece: Piece, inserted: String, granularity: Granularity): String =
    text.substring(0, piece.coreEnd) + granularity.separator + inserted + text.substring(piece.coreEnd)

  /** The text at the end of [text], after the granularity's separator if there is text before it. */
  fun append(text: String, inserted: String, granularity: Granularity): String =
    if (text.isEmpty() || text.last().isWhitespace()) text + inserted
    else text + granularity.separator + inserted

  /** The shown parts of [pieces], in the order of the text, each followed by its white space. */
  fun join(text: String, pieces: Collection<Piece>): String {
    val sorted = pieces.sortedBy { it.start }
    val builder = StringBuilder()
    sorted.forEachIndexed { index, piece ->
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
   * Turns the segments between [bounds] into pieces, with the white space at their ends as part
   * of the piece rather than shown. White space with no text gives no piece of its own: it goes
   * with the piece before it, or at the start of the text, with the first piece.
   */
  private fun attachWhiteSpace(text: String, bounds: List<Int>): List<Piece> {
    val pieces = ArrayList<Piece>()
    var leading = -1
    for ((segmentStart, segmentEnd) in bounds.zipWithNext()) {
      var coreStart = segmentStart
      var coreEnd = segmentEnd
      while (coreStart < coreEnd && text[coreStart].isWhitespace()) coreStart++
      while (coreEnd > coreStart && text[coreEnd - 1].isWhitespace()) coreEnd--
      if (coreStart == coreEnd) {
        if (pieces.isEmpty()) {
          if (leading < 0) leading = segmentStart
        } else {
          pieces[pieces.size - 1] = pieces.last().copy(end = segmentEnd)
        }
        continue
      }
      val start = if (pieces.isEmpty() && leading >= 0) leading else segmentStart
      pieces += Piece(start, coreStart, coreEnd, segmentEnd)
    }
    return pieces
  }
}
