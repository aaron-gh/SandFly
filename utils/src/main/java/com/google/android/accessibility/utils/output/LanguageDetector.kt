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

package com.google.android.accessibility.utils.output

import android.icu.lang.UCharacter
import android.icu.lang.UScript
import android.icu.util.ULocale
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds the languages of text that apps don't mark, from its writing.
 *
 * Text is split where its script changes, and at the end of each sentence. Each piece is then in:
 * 1. the one language, of the voice's and those added in [LanguageVoices], written in its script;
 * 2. if several are, the one whose letters it uses where the others don't, such as і for Ukrainian;
 * 3. else the one [LanguageGuesser] finds from its words, if that is on and the piece is long;
 * 4. else the language set for the script, or the voice's, or the first added;
 * 5. if none is, the language most often written in the script, such as Greek for Greek letters.
 *
 * Spaces, punctuation and Western digits go with the piece before them, or at the start of the text
 * with the piece after. Digits of a script go with it. Kana make Chinese characters around them
 * Japanese, and Hangul make them Korean.
 */
object LanguageDetector {

  /** A piece shorter than this many letters isn't given to [LanguageGuesser]. */
  const val MIN_GUESS_LETTERS = 12

  // Script groups for the Chinese, Japanese and Korean scripts.
  private const val HAN = "Hani"
  private const val KANA = "Kana"
  private const val HANGUL = "Hang"

  private val languageScripts = ConcurrentHashMap<String, Set<String>>()
  private val scriptLanguages = ConcurrentHashMap<String, String>()

  /**
   * Returns the runs of [text] in another language than [voice], each with the language it is in.
   * Text in the voice's language, and text with no letters, are in no run.
   */
  @JvmStatic
  fun detect(
    text: CharSequence,
    voice: Locale,
    state: LanguageVoices.State,
    guesser: LanguageGuesser?,
  ): List<LanguageSwitch.Run> {
    val tokens = tokens(text)
    if (tokens.isEmpty()) return emptyList()
    val pieces = pieces(text, tokens)
    val known = LinkedHashMap<String, Locale>()
    known[voice.language] = voice
    for (language in state.languages) known.putIfAbsent(language.language, language)
    val runs = ArrayList<LanguageSwitch.Run>()
    for ((i, piece) in pieces.withIndex()) {
      val language = languageOf(text, piece, voice, known, state, guesser)
      val locale = if (language == null || language.language == voice.language) null else language
      val start = if (i == 0) 0 else piece.start
      val end = if (i == pieces.size - 1) text.length else pieces[i + 1].start
      val last = runs.lastOrNull()
      if (last != null && last.end() == start && last.locale() == locale) {
        runs[runs.size - 1] = LanguageSwitch.Run(last.start(), end, locale)
      } else {
        runs += LanguageSwitch.Run(start, end, locale)
      }
    }
    return runs.filter { it.locale() != null }
  }

  /** A word, or a run of digits, in one script. */
  private class Token(var script: String, val start: Int, var end: Int)

  /** Words in one script within one sentence. */
  private class Piece(val script: String, val start: Int, var end: Int) {
    val words = ArrayList<Token>()
  }

  private fun tokens(text: CharSequence): List<Token> {
    val tokens = ArrayList<Token>()
    var i = 0
    var current: Token? = null
    while (i < text.length) {
      val cp = Character.codePointAt(text, i)
      val next = i + Character.charCount(cp)
      val type = Character.getType(cp)
      val mark =
        type == Character.NON_SPACING_MARK.toInt() ||
          type == Character.COMBINING_SPACING_MARK.toInt() ||
          type == Character.ENCLOSING_MARK.toInt()
      val script = if (mark) null else scriptOf(cp)
      if (mark && current != null && current.end == i) {
        current.end = next
      } else if (script != null && current != null && current.end == i && current.script == script) {
        current.end = next
      } else if (script != null) {
        current = Token(script, i, next).also { tokens += it }
      } else {
        current = null
      }
      i = next
    }
    // Chinese characters next to kana are Japanese, and next to Hangul Korean.
    for (pass in 0 until 2) {
      for ((index, token) in tokens.withIndex()) {
        if (token.script != HAN) continue
        val neighbours = listOfNotNull(tokens.getOrNull(index - 1), tokens.getOrNull(index + 1))
        when {
          neighbours.any { it.script == KANA } -> token.script = KANA
          neighbours.any { it.script == HANGUL } -> token.script = HANGUL
        }
      }
    }
    return tokens
  }

  /** The script group of [cp], or null for one with no script, such as a space or a Western digit. */
  private fun scriptOf(cp: Int): String? {
    // Arabic-Indic digits are common to several scripts, but read in Arabic.
    if (cp in 0x0660..0x0669) return "Arab"
    if (!Character.isLetter(cp) && !(Character.isDigit(cp) && cp > 0x7F)) return null
    return when (val script = UScript.getScript(cp)) {
      UScript.COMMON, UScript.INHERITED, UScript.UNKNOWN -> null
      UScript.HIRAGANA, UScript.KATAKANA, UScript.KATAKANA_OR_HIRAGANA -> KANA
      UScript.HAN, UScript.BOPOMOFO -> HAN
      UScript.HANGUL -> HANGUL
      else -> UScript.getShortName(script)
    }
  }

  private fun pieces(text: CharSequence, tokens: List<Token>): List<Piece> {
    val pieces = ArrayList<Piece>()
    for (token in tokens) {
      val last = pieces.lastOrNull()
      if (last != null && last.script == token.script && !endsSentence(text, last.end, token.start)) {
        last.end = token.end
        last.words += token
      } else {
        pieces += Piece(token.script, token.start, token.end).also { it.words += token }
      }
    }
    return pieces
  }

  private fun endsSentence(text: CharSequence, start: Int, end: Int): Boolean {
    for (i in start until end) {
      when (text[i]) {
        '.', '!', '?', '\n', '。', '！', '？', '؟', '۔', '।' -> return true
      }
      // Where speech settings took out a full stop, it left a space behind it.
      if (i > start && text[i] == ' ' && text[i - 1] == ' ') return true
    }
    return false
  }

  private fun languageOf(
    text: CharSequence,
    piece: Piece,
    voice: Locale,
    known: Map<String, Locale>,
    state: LanguageVoices.State,
    guesser: LanguageGuesser?,
  ): Locale? {
    val candidates = known.values.filter { piece.script in scriptsOf(it) }
    val set = state.scriptLanguages[piece.script]
    if (candidates.isEmpty()) {
      return set ?: likelyLanguage(piece.script)?.let { known[it] ?: Locale.forLanguageTag(it) }
    }
    if (candidates.size == 1) return candidates[0]
    val base =
      candidates.firstOrNull { it.language == set?.language }
        ?: candidates.firstOrNull { it.language == voice.language }
        ?: candidates[0]
    byLetters(text, piece, candidates, base)?.let {
      return it
    }
    if (state.identifyFromWords && guesser != null) {
      val pieceText = text.subSequence(piece.start, piece.end).toString()
      if (pieceText.count { Character.isLetter(it) } >= MIN_GUESS_LETTERS) {
        val guessed = guesser.guess(pieceText, candidates.map { it.language })
        candidates.firstOrNull { it.language == guessed }?.let {
          return it
        }
      }
    }
    return base
  }

  /**
   * The language whose letters the words of [piece] use where the other [candidates] don't, if
   * that is so for enough of its words; else null. A single word, such as a borrowed one, doesn't
   * change the language of a longer piece.
   */
  private fun byLetters(
    text: CharSequence,
    piece: Piece,
    candidates: List<Locale>,
    base: Locale,
  ): Locale? {
    val votes = HashMap<String, Int>()
    for (word in piece.words) {
      val letters = letters(text, word)
      if (letters.isEmpty()) continue
      val fits =
        candidates.filter { language ->
          // A language whose letters aren't known fits every word, so it never blocks another.
          val known = ExemplarLetters.of(language.language, piece.script)
          known == null || letters.all { it in known }
        }
      if (fits.size == 1) votes.merge(fits[0].language, 1, Int::plus)
    }
    val best = votes.maxByOrNull { it.value } ?: return null
    val forBase = votes[base.language] ?: 0
    if (best.key != base.language &&
      best.value > forBase &&
      (best.value >= 2 || best.value * 3 >= piece.words.size)
    ) {
      return candidates.first { it.language == best.key }
    }
    return if (forBase > 0) base else null
  }

  /** The lower-case letters of [word], composed, without marks. */
  private fun letters(text: CharSequence, word: Token): List<Int> {
    val composed = Normalizer.normalize(text.subSequence(word.start, word.end), Normalizer.Form.NFC)
    val letters = ArrayList<Int>()
    var i = 0
    while (i < composed.length) {
      val cp = composed.codePointAt(i)
      if (Character.isLetter(cp)) letters += UCharacter.toLowerCase(cp)
      i += Character.charCount(cp)
    }
    return letters
  }

  /** The script groups [language] is written in. */
  @JvmStatic
  fun scriptsOf(language: Locale): Set<String> =
    languageScripts.getOrPut(language.toLanguageTag()) {
      val script =
        language.script.ifEmpty {
          try {
            ULocale.addLikelySubtags(ULocale.forLocale(language)).script
          } catch (e: RuntimeException) {
            ""
          }
        }
      when {
        script.isEmpty() -> emptySet()
        script == "Hans" || script == "Hant" || script == HAN -> setOf(HAN)
        script == "Jpan" -> setOf(KANA, HAN)
        script == "Kore" -> setOf(HANGUL, HAN)
        // Serbian is written in both.
        language.language == "sr" -> setOf("Cyrl", "Latn")
        else -> setOf(script)
      }
    }

  /** The language most often written in [script], or null. */
  private fun likelyLanguage(script: String): String? =
    scriptLanguages
      .getOrPut(script) {
        val tag = if (script == KANA) "Jpan" else if (script == HANGUL) "Kore" else script
        try {
          ULocale.addLikelySubtags(ULocale("und_$tag")).language
        } catch (e: RuntimeException) {
          ""
        }
      }
      .takeIf { it.isNotEmpty() && it != "und" }

  /** The scripts that two or more of [languages] are written in, each with those languages. */
  @JvmStatic
  fun sharedScripts(languages: List<Locale>): Map<String, List<Locale>> {
    val byScript = LinkedHashMap<String, MutableList<Locale>>()
    for (language in languages.distinctBy { it.language }) {
      for (script in scriptsOf(language)) byScript.getOrPut(script) { ArrayList() } += language
    }
    return byScript.filterValues { it.size >= 2 }
  }
}
