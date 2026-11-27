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

import java.util.Locale
import java.util.regex.Pattern

/**
 * Turns what was said into [VoiceStep]s, without running anything.
 *
 * A command can chain several, joined by words such as "then" or "and". Each part is a custom voice
 * command, the name of a command, or one of the phrase patterns in [Phrases]. Patterns are words
 * with placeholders in braces, such as "call {who}", and optional words in brackets, such as
 * "describe [the|this] image". The texts that placeholders take keep the case they were said in.
 */
class VoiceGrammar(phrases: Phrases, private val resolver: Resolver, private val locale: Locale) {

  /** The words and patterns of one language. */
  class Phrases(
    /** Words that join commands in a chain, such as "and then". */
    val chain: List<String>,
    /** Words that join commands when each part is a command, such as "and". */
    val and: List<String>,
    /** Words said before or after a command that don't change it, such as "please". */
    val fillersStart: List<String>,
    val fillersEnd: List<String>,
    /** Whole commands that stop the command running, such as "stop". */
    val cancel: List<String>,
    val sendImageDescription: List<String>,
    val sendScreenDescription: List<String>,
    /** Sends the result of the step before, as in "send it to {who}". */
    val sendPrevious: List<String>,
    /** Message patterns with {who}, {text} and {app}, or {whoandtext}. */
    val message: List<String>,
    val call: List<String>,
    val describeScreen: List<String>,
    val describeImage: List<String>,
    val copy: List<String>,
    val wait: List<String>,
    val type: List<String>,
    /** Runs a command by its name, as in "run {command}". */
    val run: List<String>,
    val open: List<String>,
    val tap: List<String>,
    /** The phrases that move the cursor in, select, copy, cut and delete the text of a field. */
    val text: TextPhrases = TextPhrases(),
    /**
     * Whether text editing phrases fall back to activating an item ("select Settings") or to the
     * voice commands that came with TalkBack when no text field has input focus.
     */
    val textFallback: Boolean = true,
  )

  /**
   * The text editing phrases. [move], [select], [copy], [cut] and [delete] have a {target}: a
   * pattern of [next], [previous], [start] or [end], or, but for [move], a phrase in the text.
   * [next] and [previous] have {units}, a number of [units], such as "3 words"; [start] and [end]
   * have a {unit}, or none for the whole text. [numbers] are the words of 1, 2 and on, each item
   * with its words separated by |.
   */
  class TextPhrases(
    val move: List<String> = emptyList(),
    val select: List<String> = emptyList(),
    val copy: List<String> = emptyList(),
    val cut: List<String> = emptyList(),
    val delete: List<String> = emptyList(),
    val next: List<String> = emptyList(),
    val previous: List<String> = emptyList(),
    val start: List<String> = emptyList(),
    val end: List<String> = emptyList(),
    val units: Map<TextUnit, List<String>> = emptyMap(),
    val numbers: List<String> = emptyList(),
  )

  /** What the grammar looks up while it reads a command. Called off the main thread. */
  interface Resolver {
    /** The custom voice command with the phrase [normalized], or null. */
    fun custom(normalized: String): VoiceStep.Command?

    /** The command named [normalized], or null. */
    fun command(normalized: String): VoiceStep.Command?

    /** The app named [name], or null when no app has that name. */
    fun app(name: String): VoiceStep.OpenApp?
  }

  /**
   * What was said, as [steps] when every part of it is understood. [parts] is the number of parts
   * the chain has, [unparsed] the parts not understood, and [joined] whether it has joining words.
   */
  class Result(
    val steps: List<VoiceStep>,
    val unparsed: List<String>,
    val parts: Int,
    val joined: Boolean,
    val cancel: Boolean = false,
  ) {
    val complete: Boolean
      get() = unparsed.isEmpty() && steps.isNotEmpty()
  }

  private class Template(val pattern: Pattern, val names: List<String>)

  /** Languages written without spaces between words, whose recognizers return none. */
  private val spaceless = locale.language in SPACELESS_LANGUAGES

  /** What separates two words of a phrase, or a word from a placeholder. */
  private val separator = if (spaceless) "[\\s,:]*" else SEPARATOR

  /** The space around joining words: none needed in [spaceless] languages. */
  private val gap = if (spaceless) "\\s*" else "\\s+"

  private val chainSplit = splitter(phrases.chain, punctuation = "[,;.]")
  private val andSplit = splitter(phrases.and, punctuation = ",")
  private val fillersStart = phrases.fillersStart.map { normalize(it, locale) }.sortedByDescending { it.length }
  private val fillersEnd = phrases.fillersEnd.map { normalize(it, locale) }.sortedByDescending { it.length }
  private val cancel = phrases.cancel.map { normalize(it, locale) }.toSet()
  private val sendImageDescription = phrases.sendImageDescription.map(::template)
  private val sendScreenDescription = phrases.sendScreenDescription.map(::template)
  private val sendPrevious = phrases.sendPrevious.map(::template)
  private val message = phrases.message.map(::template)
  private val call = phrases.call.map(::template)
  private val describeScreen = phrases.describeScreen.map(::template)
  private val describeImage = phrases.describeImage.map(::template)
  private val copy = phrases.copy.map(::template)
  private val wait = phrases.wait.map(::template)
  private val type = phrases.type.map(::template)
  // The words before the text of the type patterns, such as "type" of "type {text}".
  private val typeWords =
    phrases.type.map { normalize(it.substringBefore('{').substringBefore('['), locale) }.filter { it.isNotEmpty() }
  private val run = phrases.run.map(::template)
  private val open = phrases.open.map(::template)
  private val tap = phrases.tap.map(::template)
  private val textFallback = phrases.textFallback
  // Move comes last, as its patterns include the target on its own.
  private val textOperations =
    listOf(
      TextOperation.SELECT to phrases.text.select,
      TextOperation.COPY to phrases.text.copy,
      TextOperation.CUT to phrases.text.cut,
      TextOperation.DELETE to phrases.text.delete,
      TextOperation.MOVE to phrases.text.move,
    ).map { (operation, patterns) -> operation to patterns.map(::template) }
  private val textNext = phrases.text.next.map(::template)
  private val textPrevious = phrases.text.previous.map(::template)
  private val textStart = phrases.text.start.map(::template)
  private val textEnd = phrases.text.end.map(::template)
  // Longest first, so that "text field" is read before "field".
  private val unitWords: List<Pair<TextUnit, String>> =
    phrases.text.units
      .flatMap { (unit, words) -> words.map { unit to normalize(it, locale) } }
      .filter { it.second.isNotEmpty() }
      .sortedByDescending { it.second.length }
  private val numberWords: Map<String, Int> =
    HashMap<String, Int>().apply {
      phrases.text.numbers.forEachIndexed { index, item ->
        for (word in item.split('|')) {
          val normalized = normalize(word, locale)
          if (normalized.isNotEmpty()) putIfAbsent(normalized, index + 1)
        }
      }
    }

  /** Reads [said]. */
  fun parse(said: String): Result {
    val text = trimText(said)
    // The patterns are matched with backtracking, so a long text is left to the other commands.
    if (text.isEmpty() || text.length > MAX_TEXT) {
      return Result(emptyList(), listOf(said), parts = 1, joined = false)
    }
    if (normalize(text, locale) in cancel) {
      return Result(emptyList(), emptyList(), parts = 1, joined = false, cancel = true)
    }
    val cache = HashMap<String, List<VoiceStep>?>()
    val segment = { part: String -> cache.getOrPut("s:$part") { parseSegment(part) } }
    val joinedPart = { part: String -> cache.getOrPut("j:$part") { bestSplit(part, pieces(part, andSplit), segment) } }
    val parts = pieces(text, chainSplit)
    val joined = parts.size > 1 || pieces(text, andSplit).size > 1
    bestSplit(text, parts, joinedPart)?.let { return Result(it, emptyList(), parts.size, joined) }
    // Some part isn't understood: each part is a command of its own.
    val steps = mutableListOf<VoiceStep>()
    val unparsed = mutableListOf<String>()
    for (range in parts) {
      val part = trimText(text.substring(range.first, range.second))
      val partSteps = joinedPart(part)
      if (partSteps == null) {
        unparsed.add(part)
        steps.add(VoiceStep.Legacy(part))
      } else {
        steps.addAll(partSteps)
      }
    }
    return Result(steps, unparsed, parts.size, joined)
  }

  /**
   * [said] as the phrase of a custom voice command: without the words such as "please" and the
   * punctuation around it, which [parse] leaves out before it looks up custom voice commands. Null
   * when nothing is left, when it is longer than [CustomVoiceCommands.MAX_PHRASE], or when it starts
   * like a type pattern, as its words are text to type.
   */
  fun customPhrase(said: String): String? {
    val text = trimPunctuation(stripFillers(trimText(said)))
    if (text.isEmpty() || text.length > CustomVoiceCommands.MAX_PHRASE) return null
    val normalized = normalize(text, locale)
    if (normalized.isEmpty() || match(type, text) != null) return null
    val typing = typeWords.any { normalized == it || normalized.startsWith(if (spaceless) it else "$it ") }
    return if (typing) null else text
  }

  /**
   * Reads [text], whose pieces between joining words are [pieces], as consecutive groups of pieces
   * that [parse] each understands, with as many groups as possible: "open Chrome and call Sam" is
   * two commands, "tap Salt and pepper" one. Returns null when there is no such grouping.
   */
  private fun bestSplit(
    text: String,
    pieces: List<Pair<Int, Int>>,
    parse: (String) -> List<VoiceStep>?,
  ): List<VoiceStep>? {
    val count = pieces.size
    if (count <= 1 || count > MAX_PIECES) return parse(trimText(text))
    // best[i]: the most groups that pieces i and after make, and their steps.
    val best = arrayOfNulls<Pair<Int, List<VoiceStep>>>(count + 1)
    best[count] = 0 to emptyList()
    for (i in count - 1 downTo 0) {
      for (j in i until count) {
        val rest = best[j + 1] ?: continue
        val steps = parse(trimText(text.substring(pieces[i].first, pieces[j].second))) ?: continue
        val groups = rest.first + 1
        if (best[i].let { it == null || groups > it.first }) best[i] = groups to steps + rest.second
      }
    }
    return best[0]?.second
  }

  /**
   * The pieces of [text] between the joining words that [splitter] finds, each as the range from
   * its start and its end.
   */
  private fun pieces(text: String, splitter: Pattern?): List<Pair<Int, Int>> {
    if (splitter == null) return listOf(0 to text.length)
    val ranges = mutableListOf<Pair<Int, Int>>()
    val matcher = splitter.matcher(text)
    var start = 0
    while (matcher.find()) {
      if (text.substring(start, matcher.start()).isNotBlank()) ranges.add(start to matcher.start())
      start = matcher.end()
    }
    if (text.substring(start).isNotBlank()) ranges.add(start to text.length)
    return ranges.ifEmpty { listOf(0 to text.length) }
  }

  /** Reads [segment] as one command. Returns null when it isn't understood. */
  internal fun parseSegment(segment: String): List<VoiceStep>? {
    val text = stripFillers(trimText(segment))
    if (text.isEmpty()) return null
    val normalized = normalize(text, locale)
    resolver.custom(normalized)?.let { return listOf(it) }
    resolver.command(normalized)?.let { return listOf(it) }

    match(sendImageDescription, text)?.let { m ->
      return listOf(VoiceStep.Describe(screen = false), VoiceStep.Message(who = m["who"], usePrevious = true))
    }
    match(sendScreenDescription, text)?.let { m ->
      return listOf(VoiceStep.Describe(screen = true), VoiceStep.Message(who = m["who"], usePrevious = true))
    }
    match(sendPrevious, text)?.let { m ->
      return listOf(VoiceStep.Message(who = m["who"], app = m["app"], usePrevious = true))
    }
    match(message, text)?.let { m ->
      return listOf(
        VoiceStep.Message(who = m["who"], whoAndText = m["whoandtext"], text = m["text"], app = m["app"])
      )
    }
    match(call, text)?.let { m -> m["who"]?.let { return listOf(VoiceStep.Call(it)) } }
    if (match(describeScreen, text) != null) return listOf(VoiceStep.Describe(screen = true))
    if (match(describeImage, text) != null) return listOf(VoiceStep.Describe(screen = false))
    if (match(copy, text) != null) return listOf(VoiceStep.Copy)
    match(wait, text)?.let { m ->
      val seconds = m["n"]?.let { parseNumber(it) ?: return null } ?: 1
      return listOf(VoiceStep.Wait(seconds.coerceIn(1, MAX_WAIT_SECONDS) * 1000L))
    }
    match(type, text)?.let { m -> m["text"]?.let { return listOf(VoiceStep.TypeText(it)) } }
    match(run, text)?.let { m ->
      val name = normalize(m["command"].orEmpty(), locale)
      (resolver.custom(name) ?: resolver.command(name))?.let { return listOf(it) }
    }
    // Before open and tap, which "start of line" and "select a word" would match too.
    textEdit(text)?.let { return listOf(it) }
    match(open, text)?.let { m ->
      val name = m["app"].orEmpty()
      resolver.app(name)?.let { return listOf(it) }
      resolver.command(normalize(name, locale))?.let { return listOf(it) }
    }
    match(tap, text)?.let { m -> m["label"]?.let { return listOf(VoiceStep.Tap(it)) } }
    return null
  }

  /** Reads [text] as a text editing phrase, or returns null. */
  private fun textEdit(text: String): VoiceStep.TextEdit? {
    for ((operation, templates) in textOperations) {
      for (groups in matches(templates, text)) {
        val targetText = groups["target"] ?: continue
        textTarget(targetText)?.let { target ->
          val fallback = if (textFallback) VoiceStep.Legacy(text) else null
          return VoiceStep.TextEdit(operation, target, text, fallback)
        }
        if (operation == TextOperation.MOVE) continue
        // Outside text fields, "select" and a text still activates the item with that text.
        val fallback =
          when {
            !textFallback -> null
            operation == TextOperation.SELECT -> VoiceStep.Tap(targetText)
            else -> VoiceStep.Legacy(text)
          }
        return VoiceStep.TextEdit(operation, TextTarget.Phrase(targetText), text, fallback)
      }
    }
    return null
  }

  /** Reads [said] as units, or the start or end of a unit, from the cursor; or returns null. */
  private fun textTarget(said: String): TextTarget? {
    for (groups in matches(textNext, said)) {
      val (unit, count) = units(groups["units"]) ?: continue
      if (unit != TextUnit.TEXT) return TextTarget.Units(unit, count, forward = true)
    }
    for (groups in matches(textPrevious, said)) {
      val (unit, count) = units(groups["units"]) ?: continue
      if (unit != TextUnit.TEXT) return TextTarget.Units(unit, count, forward = false)
    }
    for ((templates, end) in listOf(textStart to false, textEnd to true)) {
      for (groups in matches(templates, said)) {
        // A pattern without a unit is the start or end of the whole text.
        val unit = if ("unit" in groups) unit(groups["unit"]) ?: continue else TextUnit.TEXT
        if (unit != TextUnit.CHARACTER) return TextTarget.Boundary(unit, end)
      }
    }
    return null
  }

  /** Reads [said] as a number and a unit, such as "3 words" or "five words", or a unit alone. */
  private fun units(said: String?): Pair<TextUnit, Int>? {
    val normalized = normalize(said ?: return null, locale)
    for ((unit, word) in unitWords) {
      if (normalized == word) return unit to 1
      val prefix =
        when {
          spaceless && normalized.endsWith(word) -> normalized.dropLast(word.length)
          normalized.endsWith(" $word") -> normalized.dropLast(word.length + 1)
          else -> continue
        }.trim()
      val count = count(prefix) ?: continue
      return unit to count
    }
    return null
  }

  /** Reads [said] as a unit alone, such as "word" or "text field". */
  private fun unit(said: String?): TextUnit? {
    val normalized = normalize(said ?: return null, locale)
    return unitWords.firstOrNull { it.second == normalized }?.first
  }

  /** A count said in digits or in words, from 1 up to [VoiceTextRanges.MAX_COUNT]. */
  private fun count(said: String): Int? {
    if (said.isEmpty()) return null
    val number = said.toIntOrNull() ?: numberWords[said] ?: return null
    return if (number < 1) null else minOf(number, VoiceTextRanges.MAX_COUNT)
  }

  private fun stripFillers(text: String): String {
    var result = text
    var changed = true
    while (changed) {
      changed = false
      for (filler in fillersStart) {
        val rest = dropWords(result, filler, start = true)
        if (rest != null) {
          result = rest
          changed = true
        }
      }
      for (filler in fillersEnd) {
        val rest = dropWords(result, filler, start = false)
        if (rest != null) {
          result = rest
          changed = true
        }
      }
    }
    return result
  }

  /**
   * Returns [text] without the words [words] at its start or end, or null when they aren't there
   * or are all of it.
   */
  private fun dropWords(text: String, words: String, start: Boolean): String? {
    if (words.isEmpty()) return null
    val pattern =
      if (start) Pattern.compile("^" + wordsRegex(words) + "[\\s,]" + (if (spaceless) "*" else "+") + "(.+)$", FLAGS)
      else Pattern.compile("^(.+?)[\\s,]" + (if (spaceless) "*" else "+") + wordsRegex(words) + "[\\p{P}\\s]*$", FLAGS)
    val matcher = pattern.matcher(text)
    return if (matcher.matches()) trimText(matcher.group(1)!!).ifEmpty { null } else null
  }

  private fun match(templates: List<Template>, text: String): Map<String, String>? {
    for (template in templates) {
      val matcher = template.pattern.matcher(text)
      if (!matcher.matches()) continue
      val groups =
        template.names.associateWith {
          val group = matcher.group(it) ?: ""
          if (it == "text" || it == "whoandtext") trimText(group) else trimPunctuation(group)
        }
      if (groups.values.any { it.isEmpty() }) continue
      return groups
    }
    return null
  }

  /** The groups of each of [templates] that matches [text], in order. */
  private fun matches(templates: List<Template>, text: String): Sequence<Map<String, String>> =
    templates.asSequence().mapNotNull { match(listOf(it), text) }

  private fun template(text: String): Template {
    val names = mutableListOf<String>()
    val regex = StringBuilder("^")
    val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    for ((index, token) in tokens.withIndex()) {
      val last = index == tokens.size - 1
      when {
        token.startsWith("{") && token.endsWith("}") -> {
          val name = token.substring(1, token.length - 1).lowercase(Locale.ROOT)
          names.add(name)
          // The last placeholder takes the rest, with the punctuation said at the end.
          regex.append("(?<").append(name).append(if (last) ">.+)" else ">.+?)")
          if (!last) regex.append(separator)
        }
        token.startsWith("[") && token.endsWith("]") -> {
          val alternatives =
            token.substring(1, token.length - 1).split('|').joinToString("|") { wordsRegex(it) }
          if (last) {
            // The separator before it is part of the optional words.
            if (regex.endsWith(separator)) regex.setLength(regex.length - separator.length)
            regex.append("(?:").append(separator).append("(?:").append(alternatives).append("))?")
          } else {
            regex.append("(?:(?:").append(alternatives).append(")").append(separator).append(")?")
          }
        }
        else -> {
          regex.append(wordsRegex(token))
          if (!last) regex.append(separator)
        }
      }
    }
    regex.append("[\\p{P}\\s]*$")
    return Template(Pattern.compile(regex.toString(), FLAGS), names)
  }

  private fun splitter(words: List<String>, punctuation: String): Pattern? {
    val alternatives = words.filter { it.isNotBlank() }.sortedByDescending { it.length }.map { wordsRegex(it.trim()) }
    val joiners = alternatives.joinToString("|")
    val regex =
      when {
        joiners.isEmpty() -> "\\s*$punctuation$gap"
        else -> "\\s*$punctuation?$gap(?:$joiners)$gap|\\s*$punctuation$gap"
      }
    return Pattern.compile(regex, FLAGS)
  }

  companion object {
    private const val FLAGS = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    private const val SEPARATOR = "[\\s,:]+"

    /** Chinese, Japanese, Thai, Lao, Khmer and Burmese, which put no spaces between words. */
    private val SPACELESS_LANGUAGES = setOf("zh", "ja", "th", "lo", "km", "my", "yue")
    private const val MAX_WAIT_SECONDS = 60

    /** The most pieces between joining words that are grouped into commands. */
    private const val MAX_PIECES = 12

    /** The longest text read as a command. */
    private const val MAX_TEXT = 1_000

    /** Lower case, apostrophes dropped, other punctuation as spaces, single spaces. */
    @JvmStatic
    fun normalize(text: String, locale: Locale): String =
      text
        .lowercase(locale)
        .replace(Regex("['’]"), "")
        .replace(Regex("[\\p{P}\\p{S}]"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    /** [text] without punctuation and spaces around it, as recognition and AI correction add. */
    @JvmStatic
    fun trimPunctuation(text: String): String = text.replace(Regex("^[\\p{P}\\s]+|[\\p{P}\\s]+$"), "")

    /**
     * [text] without spaces and joining punctuation around it, keeping question and exclamation
     * marks, for texts that are typed or sent.
     */
    @JvmStatic
    fun trimText(text: String): String = text.replace(Regex("^[\\s,;:.]+|[\\s,;:.]+$"), "")

    private fun parseNumber(text: String): Int? = text.trim().toIntOrNull()
  }

  /** Words that match [words], with any spaces between them and any apostrophe or none. */
  private fun wordsRegex(words: String): String =
    words.trim().split(Regex("\\s+")).joinToString(separator) { word ->
      word.split(Regex("['’]")).joinToString("['’]?") { if (it.isEmpty()) "" else Pattern.quote(it) }
    }
}
