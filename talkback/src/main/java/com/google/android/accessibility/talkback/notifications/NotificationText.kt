/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.notifications

import java.net.URI

/** Turns a notification into what is said, and cleans that text up. Pure functions, for tests. */
object NotificationText {
  /** The placeholders of a speech format, which the settings list for the user. */
  val PLACEHOLDERS =
    listOf("app", "title", "text", "content", "bigtext", "subtext", "ticker", "category", "time")

  private val PLACEHOLDER = Regex("\\{(\\w+)\\}")
  private val URL = Regex("(?i)\\b(?:https?://|www\\.)[^\\s<>\"]+")
  private val TRAILING_URL_PUNCTUATION = Regex("[.,;:!?)\\]}'\"]+$")
  private val SENTENCE_END = Regex("(?<=[.!?…])\\s+")
  private val SPACES = Regex("[ \\t]+")

  /**
   * Fills [format]'s placeholders from [info]. Unknown placeholders are left as they are, and
   * the separators around placeholders that are empty are dropped, so "{title}, {text}" with no
   * title says only the text.
   */
  fun render(
    format: String,
    info: NotificationInfo,
    spokenAppName: String,
    categoryName: String,
    time: String,
  ): String {
    val filled =
      PLACEHOLDER.replace(format) { match ->
        when (match.groupValues[1].lowercase()) {
          "app" -> spokenAppName
          "title" -> info.title
          "text" -> textOf(info)
          "content" -> info.content
          "bigtext" -> info.bigText.ifEmpty { textOf(info) }
          "subtext" -> info.subText
          "ticker" -> info.ticker
          "category" -> categoryName
          "time" -> time
          else -> match.value
        }
      }
    return tidySeparators(filled)
  }

  /** The text, or the ticker when there is no text and the ticker isn't only the title. */
  fun textOf(info: NotificationInfo): String =
    when {
      info.text.isNotBlank() -> info.text
      info.lines.isNotEmpty() -> info.lines.last()
      info.ticker.isNotBlank() && info.ticker.trim() != info.title.trim() -> info.ticker
      else -> ""
    }

  /** Drops separators left with nothing between them, and spaces doubled by empty values. */
  fun tidySeparators(text: String): String {
    var result = text.lines().joinToString(" ") { it.trim() }
    result = SPACES.replace(result, " ")
    // ", ," and ": ," left by empty placeholders.
    result = Regex("\\s*([,;:])(?:\\s*[,;:])+").replace(result) { it.groupValues[1] }
    result = Regex("\\s+([,;:.!?])").replace(result, "$1")
    result = Regex("^[\\s,;:.\\-–—]+").replace(result, "")
    result = Regex("[\\s,;:\\-–—]+$").replace(result, "")
    return result.trim()
  }

  /** Applies [replacements] in order. */
  fun replace(text: String, replacements: List<Replacement>): String =
    replacements.fold(text) { acc, replacement -> replacement.apply(acc) }

  /** Reads links as [handling] says: whole, only the site's name, as [replacement], or not. */
  fun links(text: String, handling: UrlHandling, replacement: String): String =
    when (handling) {
      UrlHandling.FULL -> text
      else ->
        URL.replace(text) { match ->
          val trailing = TRAILING_URL_PUNCTUATION.find(match.value)?.value.orEmpty()
          val url = match.value.removeSuffix(trailing)
          when (handling) {
            UrlHandling.DOMAIN -> domainOf(url) + trailing
            UrlHandling.REPLACE -> replacement + trailing
            else -> trailing
          }
        }.let { tidySeparators(it) }
    }

  /** The site of [url], without "www.", or [url] when it has none. */
  fun domainOf(url: String): String {
    val withScheme = if (url.contains("://")) url else "https://$url"
    val host =
      try {
        URI(withScheme).host
      } catch (e: Exception) {
        null
      }
    return host?.removePrefix("www.") ?: url
  }

  /** Removes emoji, with the joiners and variation selectors that hold them together. */
  fun removeEmoji(text: String): String {
    val result = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
      val codePoint = text.codePointAt(i)
      if (!isEmoji(codePoint)) result.appendCodePoint(codePoint)
      i += Character.charCount(codePoint)
    }
    return SPACES.replace(result.toString(), " ").trim()
  }

  private fun isEmoji(c: Int): Boolean =
    c in 0x1F000..0x1FAFF ||
      c in 0x2600..0x27BF ||
      c in 0x2B00..0x2BFF ||
      c in 0x1F1E6..0x1F1FF ||
      c in 0xE0020..0xE007F ||
      c in 0xFE00..0xFE0F ||
      c == 0x200D ||
      c == 0x20E3 ||
      c in 0x2300..0x23FF ||
      c == 0x3030 ||
      c == 0x303D ||
      c == 0x3297 ||
      c == 0x3299

  /** Splits numbers of [minDigits] digits or more, so "123456" is read digit by digit. */
  fun separateDigits(text: String, minDigits: Int, separator: String): String =
    Regex("\\d{$minDigits,}").replace(text) { it.value.toCharArray().joinToString(separator) }

  /**
   * Cuts [text] to [maxWords] words, [maxSentences] sentences and [maxCharacters] characters,
   * each when it is more than 0, and marks the cut with an ellipsis.
   */
  fun cap(text: String, maxWords: Int, maxSentences: Int, maxCharacters: Int): String {
    var result = text
    var cut = false
    if (maxSentences > 0) {
      val sentences = result.split(SENTENCE_END)
      if (sentences.size > maxSentences) {
        result = sentences.take(maxSentences).joinToString(" ")
        cut = true
      }
    }
    if (maxWords > 0) {
      val words = result.split(Regex("\\s+")).filter { it.isNotEmpty() }
      if (words.size > maxWords) {
        result = words.take(maxWords).joinToString(" ")
        cut = true
      }
    }
    if (maxCharacters in 1 until result.length) {
      val space = result.lastIndexOf(' ', maxCharacters).takeIf { it > maxCharacters / 2 }
      result = result.substring(0, space ?: maxCharacters).trimEnd()
      cut = true
    }
    if (!cut) return result
    val trimmed = result.trimEnd(',', ';', ':', ' ')
    // A cut after a whole sentence ends as the sentence does.
    return if (trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?') || trimmed.endsWith('…')) {
      trimmed
    } else {
      "$trimmed…"
    }
  }

  /**
   * [text] with each number replaced by #, so that notifications that differ only in numbers,
   * such as a download's percentage, count as the same.
   */
  fun withoutNumbers(text: String): String = Regex("\\d+([.,:]\\d+)*").replace(text, "#")

  /**
   * The part of [text] after the start it shares with [previous], when both start with the same
   * words up to a separator, such as a group's name in "Family: Ann: hello" and "Family: Ann:
   * bye". Returns [text] when nothing is shared or nothing would be left.
   */
  fun withoutSharedPrefix(text: String, previous: String): String {
    var shared = 0
    val limit = minOf(text.length, previous.length)
    while (shared < limit && text[shared].equals(previous[shared], ignoreCase = true)) shared++
    if (shared == 0) return text
    // Back to the end of the last whole part both have, after a separator.
    val boundary = text.lastIndexOfAny(charArrayOf(',', ':', ';', '.', '-', '–'), shared - 1)
    if (boundary <= 0) return text
    val rest = text.substring(boundary + 1).trim().trimStart(',', ':', ';', '-', '–').trim()
    return rest.ifEmpty { text }
  }

  /** A regular expression that matches [text] with any numbers in place of its own numbers. */
  fun similarPattern(text: String): String =
    text.trim().split(Regex("\\d+")).joinToString("\\d+") { Regex.escape(it) }
}
