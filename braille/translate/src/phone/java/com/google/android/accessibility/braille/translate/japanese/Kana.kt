/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) kana.py and limits.py at 307f3dd.
 * Copyright (C) 2019-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import java.text.Normalizer
import java.util.Locale

/** Inputs longer than this are rejected, as libkuraji does by default. */
internal const val MAX_INPUT_CHARS = 65536

/** Rejects inputs that exceed [MAX_INPUT_CHARS]. */
internal fun enforceMaxInputLength(text: String) {
  require(text.length <= MAX_INPUT_CHARS) {
    "input length ${text.length} exceeds limit of $MAX_INPUT_CHARS characters"
  }
}

/**
 * Kana-to-braille translation (libkuraji's translator1 stage).
 *
 * Input is katakana text already segmented by the upstream stage (word wrapping / masuake); output
 * is Unicode braille cells (U+2800..U+28FF) with U+0020 for blanks, plus a position map: for each
 * output cell, the index of the (normalized) input character it came from. Indices are UTF-16
 * indices; libkuraji's are code point indices.
 */
internal object Kana {

  private fun cell(vararg dots: Int): String {
    var value = 0
    for (dot in dots) value = value or (1 shl (dot - 1))
    return (0x2800 or value).toChar().toString()
  }

  // --- base kana tables ------------------------------------------------

  private val VOWEL_DOTS =
    mapOf(
      'A' to intArrayOf(1),
      'I' to intArrayOf(1, 2),
      'U' to intArrayOf(1, 4),
      'E' to intArrayOf(1, 2, 4),
      'O' to intArrayOf(2, 4),
    )

  private val ROW_EXTRA_DOTS =
    mapOf(
      "" to intArrayOf(),
      "K" to intArrayOf(6),
      "S" to intArrayOf(5, 6),
      "T" to intArrayOf(3, 5),
      "N" to intArrayOf(3),
      "H" to intArrayOf(3, 6),
      "M" to intArrayOf(3, 5, 6),
      "R" to intArrayOf(5),
    )

  private val ROW_KANA =
    linkedMapOf(
      "" to "アイウエオ",
      "K" to "カキクケコ",
      "S" to "サシスセソ",
      "T" to "タチツテト",
      "N" to "ナニヌネノ",
      "H" to "ハヒフヘホ",
      "M" to "マミムメモ",
      "R" to "ラリルレロ",
    )

  private fun rowCell(vowel: Char, row: String): String =
    cell(*(VOWEL_DOTS.getValue(vowel) + ROW_EXTRA_DOTS.getValue(row)))

  private val DAKUTEN = cell(5)
  private val HANDAKUTEN = cell(6)
  private val YOON = cell(4)
  private val YOON_DAKU = cell(4, 5)
  private val YOON_HANDAKU = cell(4, 6)
  /** Gairaion prefix (fa/wi/tu etc.). */
  private val SPECIAL_26 = cell(2, 6)
  /** Gairaion voiced prefix (va etc.). */
  private val SPECIAL_256 = cell(2, 5, 6)

  /** Single kana -> cell string. */
  private val KANA1: Map<Char, String> =
    HashMap<Char, String>().apply {
      for ((row, kana) in ROW_KANA) {
        for ((v, ch) in "AIUEO".zip(kana)) put(ch, rowCell(v, row))
      }
      put('ヤ', cell(3, 4))
      put('ユ', cell(3, 4, 6))
      put('ヨ', cell(3, 4, 5))
      put('ワ', cell(3))
      put('ヰ', cell(1, 2, 3))
      put('ヱ', cell(2, 3, 5))
      put('ヲ', cell(3, 5))
      put('ン', cell(3, 5, 6))
      put('ッ', cell(2))
      put('ー', cell(2, 5))
      // voiced kana -> dakuten prefix + base cell
      for ((voiced, base) in
        listOf(
          "ガギグゲゴ" to "カキクケコ",
          "ザジズゼゾ" to "サシスセソ",
          "ダヂヅデド" to "タチツテト",
          "バビブベボ" to "ハヒフヘホ",
          "ヴ" to "ウ",
        )) {
        for ((vc, bc) in voiced.zip(base)) put(vc, DAKUTEN + getValue(bc))
      }
      for ((vc, bc) in "パピプペポ".zip("ハヒフヘホ")) put(vc, HANDAKUTEN + getValue(bc))
    }

  // --- two-character kana (youon and gairaion) --------------------------

  private val SMALL_VOWEL = linkedMapOf('ャ' to 'A', 'ュ' to 'U', 'ョ' to 'O')

  /**
   * Two kana -> cells; the prefix cell maps to the first character, the remaining cells map to the
   * second character.
   */
  private val KANA2: Map<String, String> =
    HashMap<String, String>().apply {
      for ((table, prefix) in
        listOf(
          linkedMapOf(
            'キ' to "K",
            'シ' to "S",
            'チ' to "T",
            'ニ' to "N",
            'ヒ' to "H",
            'ミ' to "M",
            'リ' to "R",
          ) to YOON,
          linkedMapOf('ギ' to "K", 'ジ' to "S", 'ヂ' to "T", 'ビ' to "H") to YOON_DAKU,
          linkedMapOf('ピ' to "H") to YOON_HANDAKU,
        )) {
        for ((kc, row) in table) {
          for ((small, v) in SMALL_VOWEL) put("$kc$small", prefix + rowCell(v, row))
        }
      }

      fun k2(pair: String, prefix: String, base: Char) {
        put(pair, prefix + KANA1.getValue(base))
      }

      // gairaion (special sounds); derived from the harness expectations
      k2("イェ", YOON, 'エ')
      k2("ウィ", SPECIAL_26, 'イ')
      k2("ウェ", SPECIAL_26, 'エ')
      k2("ウォ", SPECIAL_26, 'オ')
      k2("キェ", YOON, 'ケ')
      k2("シェ", YOON, 'セ')
      k2("ジェ", YOON_DAKU, 'セ')
      k2("チェ", YOON, 'テ')
      k2("ティ", YOON, 'チ')
      k2("ディ", YOON_DAKU, 'チ')
      k2("トゥ", SPECIAL_26, 'ツ')
      k2("ドゥ", SPECIAL_256, 'ツ')
      k2("ニェ", YOON, 'ネ')
      k2("ヒェ", YOON, 'ヘ')
      k2("ファ", SPECIAL_26, 'ハ')
      k2("フィ", SPECIAL_26, 'ヒ')
      k2("フェ", SPECIAL_26, 'ヘ')
      k2("フォ", SPECIAL_26, 'ホ')
      k2("ヴァ", SPECIAL_256, 'ハ')
      k2("ヴィ", SPECIAL_256, 'ヒ')
      k2("ヴェ", SPECIAL_256, 'ヘ')
      k2("ヴォ", SPECIAL_256, 'ホ')
    }

  // --- punctuation and symbols ------------------------------------------

  /** Sentence punctuation: cell(s) followed by padding blanks unless at the end of the input. */
  private val SENTENCE_PUNCT: Map<Char, Pair<String, Int>> =
    mapOf(
      '。' to (cell(2, 5, 6) to 2),
      '？' to (cell(2, 6) to 1),
      '！' to (cell(2, 3, 5) to 1),
      '?' to (cell(2, 6) to 1),
      '!' to (cell(2, 3, 5) to 1),
      '、' to (cell(5, 6) to 1),
      '・' to (cell(5) to 1),
    )

  /** Enclosure marks. */
  private val ENCLOSURE: Map<Char, String> =
    HashMap<Char, String>().apply {
      put('(', cell(2, 3, 5, 6))
      put(')', cell(2, 3, 5, 6))
      put('（', cell(2, 3, 5, 6))
      put('）', cell(2, 3, 5, 6))
      put('「', cell(3, 6))
      put('」', cell(3, 6))
      put('『', cell(5, 6) + cell(3, 6))
      put('』', cell(3, 6) + cell(2, 3))
      for (c in "[{“‘〔〈《【〝｛") put(c, cell(5) + cell(2, 3, 5, 6))
      for (c in "]}”’〕〉》】〟｝") put(c, cell(2, 3, 5, 6) + cell(2))
    }

  /** Symbols in kana context. */
  private val SYMBOL: Map<Char, String> =
    mapOf(
      ',' to cell(3),
      '.' to cell(2, 5, 6),
      '-' to cell(3, 6),
      ':' to cell(5) + cell(2),
      '<' to cell(3, 5).repeat(2),
      '=' to cell(2, 5).repeat(2),
      '>' to cell(2, 6).repeat(2),
      '+' to cell(2, 6),
      '/' to cell(3, 4),
      '%' to cell(5, 6) + cell(1, 2, 3, 4),
      '@' to cell(5, 6) + cell(2, 4, 6),
      '#' to cell(5, 6) + cell(1, 4, 6),
      '*' to cell(5, 6) + cell(1, 6),
      '&' to cell(5, 6) + cell(1, 2, 3, 4, 6),
      '_' to cell(5) + cell(3, 6),
      '$' to cell(5, 6) + cell(1, 4, 5, 6),
      '|' to cell(2, 3, 5, 6),
      '｜' to cell(2, 3, 5, 6),
      ';' to cell(2, 3),
      '\\' to cell(1, 6),
    )

  /** Words spelled out for symbols in kana context. */
  private val LITERAL_SYMBOL: Map<Char, String> =
    mapOf(
      // yuubin bangou (postal mark), spelled inside explanation signs
      '〒' to "⠰⠶⠬⠒⠐⠧⠴ ⠐⠥⠴⠐⠪⠒⠶⠆"
    )

  private class Spaced(val cells: String, val before: Boolean, val after: Boolean)

  /** Symbols spelled with surrounding blanks in kana context. */
  private val SPACED_SYMBOL: Map<Char, Spaced> =
    mapOf(
      '→' to Spaced(cell(2, 5).repeat(2) + cell(1, 3, 5), before = true, after = true),
      '←' to Spaced(cell(2, 4, 6) + cell(2, 5).repeat(2), before = true, after = true),
      '※' to Spaced(cell(3, 5).repeat(2), before = false, after = true),
    )

  /** Shape and other symbols spelled as sign sequences. */
  private val SHAPE_SYMBOL: Map<Char, String> =
    mapOf(
      '☆' to cell(5, 6) + cell(2, 3, 4, 6) + cell(2),
      '★' to cell(5, 6) + cell(2, 3, 4, 6) + cell(2, 3),
      '○' to cell(6) + cell(1, 3, 5, 6) + cell(2),
      '●' to cell(6) + cell(1, 3, 5, 6) + cell(2, 3),
      '◎' to cell(6) + cell(1, 3, 5, 6) + cell(2, 5, 6),
      '□' to cell(6) + cell(1, 2, 5, 6) + cell(2),
      '■' to cell(6) + cell(1, 2, 5, 6) + cell(2, 3),
      '△' to cell(6) + cell(1, 5, 6) + cell(2),
      '▲' to cell(6) + cell(1, 5, 6) + cell(2, 3),
      '▽' to cell(5, 6) + cell(1, 5, 6) + cell(2),
      '▼' to cell(5, 6) + cell(1, 5, 6) + cell(2, 3),
      '◇' to cell(4, 6) + cell(1, 2, 3, 6) + cell(2),
      '◆' to cell(4, 6) + cell(1, 2, 3, 6) + cell(2, 3),
      '×' to cell(5, 6) + cell(1, 6) + cell(2),
    )

  // --- digits and Latin -------------------------------------------------

  private val NUMERIC_SIGN = cell(3, 4, 5, 6)
  private val DIGIT_CELLS: Map<Char, String> =
    mapOf(
      '1' to cell(1),
      '2' to cell(1, 2),
      '3' to cell(1, 4),
      '4' to cell(1, 4, 5),
      '5' to cell(1, 5),
      '6' to cell(1, 2, 4),
      '7' to cell(1, 2, 4, 5),
      '8' to cell(1, 2, 5),
      '9' to cell(2, 4),
      '0' to cell(2, 4, 5),
    )
  /** First connector sign. */
  private val TSUNAGI = cell(3, 6)
  private val FOREIGN_SIGN = cell(5, 6)
  private val CAPITAL_SIGN = cell(6)

  private val LATIN_DOTS: Map<Char, IntArray> =
    linkedMapOf(
      'a' to intArrayOf(1),
      'b' to intArrayOf(1, 2),
      'c' to intArrayOf(1, 4),
      'd' to intArrayOf(1, 4, 5),
      'e' to intArrayOf(1, 5),
      'f' to intArrayOf(1, 2, 4),
      'g' to intArrayOf(1, 2, 4, 5),
      'h' to intArrayOf(1, 2, 5),
      'i' to intArrayOf(2, 4),
      'j' to intArrayOf(2, 4, 5),
      'k' to intArrayOf(1, 3),
      'l' to intArrayOf(1, 2, 3),
      'm' to intArrayOf(1, 3, 4),
      'n' to intArrayOf(1, 3, 4, 5),
      'o' to intArrayOf(1, 3, 5),
      'p' to intArrayOf(1, 2, 3, 4),
      'q' to intArrayOf(1, 2, 3, 4, 5),
      'r' to intArrayOf(1, 2, 3, 5),
      's' to intArrayOf(2, 3, 4),
      't' to intArrayOf(2, 3, 4, 5),
      'u' to intArrayOf(1, 3, 6),
      'v' to intArrayOf(1, 2, 3, 6),
      'w' to intArrayOf(2, 4, 5, 6),
      'x' to intArrayOf(1, 3, 4, 6),
      'y' to intArrayOf(1, 3, 4, 5, 6),
      'z' to intArrayOf(1, 3, 5, 6),
    )
  val LATIN: Map<Char, String> = LATIN_DOTS.mapValues { (_, dots) -> cell(*dots) }

  /**
   * ASCII symbols inside a computer braille context (opened with dot-6 quote mark, i.e. the
   * two-cell sign U+2820 U+2826).
   */
  private val COMPUTER_SYMBOL: Map<Char, String> =
    linkedMapOf(
      '.' to cell(2, 5, 6),
      ',' to cell(2),
      '@' to cell(2, 4, 6),
      '-' to cell(3, 6),
      '\'' to cell(3),
      ':' to cell(5) + cell(2),
      ';' to cell(2, 3),
      '?' to cell(5) + cell(2, 3, 6),
      '!' to cell(2, 3, 5),
      '/' to cell(3, 4),
      '\\' to cell(1, 2, 4, 6),
      '_' to cell(5) + cell(3, 6),
      '~' to cell(5) + cell(1, 4),
      '&' to cell(1, 2, 3, 4, 6),
      '#' to cell(1, 4, 6),
      '+' to cell(3, 4, 6),
      '$' to cell(1, 4, 5, 6),
      '%' to cell(1, 2, 4, 5, 6),
      '*' to cell(1, 6),
      '|' to cell(1, 2, 5, 6),
      '^' to cell(4, 5),
      '<' to cell(3, 5).repeat(2),
      '>' to cell(2, 6).repeat(2),
      '=' to cell(2, 5).repeat(2),
      '{' to cell(1, 2, 6),
      '}' to cell(3, 4, 5),
      '[' to cell(1, 2, 3, 5, 6),
      ']' to cell(2, 3, 4, 5, 6),
    )

  /** ASCII symbols inside a foreign-language quote (plain quote mark). */
  private val QUOTE_SYMBOL: Map<Char, String> =
    LinkedHashMap(COMPUTER_SYMBOL).apply {
      put('?', cell(2, 3, 6))
      put('%', cell(5, 6) + cell(1, 2, 3, 4))
      put('*', cell(3, 5).repeat(2))
      put('+', cell(2, 6))
    }

  // --- Cyrillic and Greek letters ---------------------------------------
  // Lowercase letters map to international braille patterns; capitals add dot 7 (see U+2840
  // range).

  private val CYRILLIC_DOTS: Map<Char, IntArray> =
    linkedMapOf(
      'а' to intArrayOf(1),
      'б' to intArrayOf(1, 2),
      'в' to intArrayOf(2, 4, 5, 6),
      'г' to intArrayOf(1, 2, 4, 5),
      'д' to intArrayOf(1, 4, 5),
      'е' to intArrayOf(1, 5),
      'ж' to intArrayOf(2, 4, 5),
      'з' to intArrayOf(1, 3, 5, 6),
      'и' to intArrayOf(2, 4),
      'й' to intArrayOf(1, 2, 3, 4, 6),
      'к' to intArrayOf(1, 3),
      'л' to intArrayOf(1, 2, 3),
      'м' to intArrayOf(1, 3, 4),
      'н' to intArrayOf(1, 3, 4, 5),
      'о' to intArrayOf(1, 3, 5),
      'п' to intArrayOf(1, 2, 3, 4),
      'р' to intArrayOf(1, 2, 3, 5),
      'с' to intArrayOf(2, 3, 4),
      'т' to intArrayOf(2, 3, 4, 5),
      'у' to intArrayOf(1, 3, 6),
      'ф' to intArrayOf(1, 2, 4),
      'х' to intArrayOf(1, 2, 5),
      'ц' to intArrayOf(1, 4),
      'ч' to intArrayOf(1, 2, 3, 4, 5),
      'ш' to intArrayOf(1, 5, 6),
      'щ' to intArrayOf(1, 3, 4, 6),
      'ъ' to intArrayOf(1, 2, 3, 5, 6),
      'ы' to intArrayOf(2, 3, 4, 6),
      'ь' to intArrayOf(2, 3, 4, 5, 6),
      'э' to intArrayOf(2, 4, 6),
      'ю' to intArrayOf(1, 2, 5, 6),
      'я' to intArrayOf(1, 2, 4, 6),
      'ё' to intArrayOf(1, 6),
      'ѣ' to intArrayOf(3, 4, 5),
      'ѫ' to intArrayOf(2, 4, 6),
    )

  private val GREEK_DOTS: Map<Char, IntArray> =
    linkedMapOf(
      'α' to intArrayOf(1),
      'β' to intArrayOf(1, 2),
      'γ' to intArrayOf(1, 2, 4, 5),
      'δ' to intArrayOf(1, 4, 5),
      'ε' to intArrayOf(1, 5),
      'ζ' to intArrayOf(1, 3, 5, 6),
      'η' to intArrayOf(1, 5, 6),
      'θ' to intArrayOf(1, 4, 5, 6),
      'ι' to intArrayOf(2, 4),
      'κ' to intArrayOf(1, 3),
      'λ' to intArrayOf(1, 2, 3),
      'μ' to intArrayOf(1, 3, 4),
      'ν' to intArrayOf(1, 3, 4, 5),
      'ξ' to intArrayOf(1, 3, 4, 6),
      'ο' to intArrayOf(1, 3, 5),
      'π' to intArrayOf(1, 2, 3, 4),
      'ρ' to intArrayOf(1, 2, 3, 5),
      'σ' to intArrayOf(2, 3, 4),
      'ς' to intArrayOf(2, 3, 4),
      'τ' to intArrayOf(2, 3, 4, 5),
      'υ' to intArrayOf(1, 3, 6),
      'φ' to intArrayOf(1, 2, 4),
      'χ' to intArrayOf(1, 2, 3, 4, 6),
      'ψ' to intArrayOf(1, 3, 4, 5, 6),
      'ω' to intArrayOf(2, 4, 5, 6),
    )

  private val LETTER_CELLS: Map<String, String> =
    HashMap<String, String>().apply {
      for (dotsMap in listOf(CYRILLIC_DOTS, GREEK_DOTS)) {
        for ((ch, dots) in dotsMap) {
          val lower = ch.toString()
          put(lower, cell(*dots))
          val upper = lower.uppercase(Locale.ROOT)
          if (upper != lower) {
            // capital letters carry dot 7
            put(upper, (cell(*dots)[0].code or 0x40).toChar().toString())
          }
        }
      }
    }

  /** Inner cell of the quote mark. */
  private val FOREIGN_OPEN = cell(2, 3, 6)
  private val FOREIGN_CLOSE = cell(3, 5, 6)

  /** Kana requiring a connector sign right after a digit run. */
  private val TSUNAGI_KANA = "アイウエオラリルレロ".toSet()

  /** Letters confusable with digits (a-j) need a foreign sign after numbers. */
  private val DIGIT_LIKE_LETTERS = "abcdefghij".toSet()

  // --- NABCC (North American Braille Computer Code) ---------------------
  // In NABCC mode every ASCII character maps directly to one cell: lowercase letters use the
  // standard patterns, capitals add dot 7, digits are "dropped" (lowered) patterns without a
  // numeric sign.

  private val NABCC_DIGIT_DOTS: Map<Char, IntArray> =
    linkedMapOf(
      '1' to intArrayOf(2),
      '2' to intArrayOf(2, 3),
      '3' to intArrayOf(2, 5),
      '4' to intArrayOf(2, 5, 6),
      '5' to intArrayOf(2, 6),
      '6' to intArrayOf(2, 3, 5),
      '7' to intArrayOf(2, 3, 5, 6),
      '8' to intArrayOf(2, 3, 6),
      '9' to intArrayOf(3, 5),
      '0' to intArrayOf(3, 5, 6),
    )

  private val NABCC_SYMBOL_DOTS: Map<Char, IntArray> =
    linkedMapOf(
      ',' to intArrayOf(6),
      ';' to intArrayOf(5, 6),
      ':' to intArrayOf(1, 5, 6),
      '.' to intArrayOf(4, 6),
      '!' to intArrayOf(2, 3, 4, 6),
      '"' to intArrayOf(5),
      '\'' to intArrayOf(3),
      '(' to intArrayOf(1, 2, 3, 5, 6),
      ')' to intArrayOf(2, 3, 4, 5, 6),
      '-' to intArrayOf(3, 6),
      '_' to intArrayOf(4, 5, 6),
      '<' to intArrayOf(1, 2, 6),
      '=' to intArrayOf(1, 2, 3, 4, 5, 6),
      '>' to intArrayOf(3, 4, 5),
      '%' to intArrayOf(1, 4, 6),
      '+' to intArrayOf(3, 4, 6),
      '~' to intArrayOf(4, 5),
      '`' to intArrayOf(4),
      '&' to intArrayOf(1, 2, 3, 4, 6),
      '$' to intArrayOf(1, 2, 4, 6),
      '?' to intArrayOf(1, 4, 5, 6),
      '{' to intArrayOf(2, 4, 6),
      '[' to intArrayOf(2, 4, 6, 7),
      '}' to intArrayOf(1, 2, 4, 5, 6),
      ']' to intArrayOf(1, 2, 4, 5, 6, 7),
      '^' to intArrayOf(4, 5, 7),
      '@' to intArrayOf(4, 7),
      '#' to intArrayOf(3, 4, 5, 6),
      '\\' to intArrayOf(1, 2, 5, 6, 7),
      '|' to intArrayOf(1, 2, 5, 6),
      '/' to intArrayOf(3, 4),
      '*' to intArrayOf(1, 6),
    )

  val NABCC: Map<Char, String> =
    HashMap<Char, String>().apply {
      put(' ', " ")
      for ((ch, dots) in NABCC_DIGIT_DOTS) put(ch, cell(*dots))
      for ((ch, dots) in NABCC_SYMBOL_DOTS) put(ch, cell(*dots))
      for ((ch, braille) in LATIN) {
        put(ch, braille)
        put(ch.uppercaseChar(), (braille[0].code or 0x40).toChar().toString()) // capital: dot 7
      }
    }

  /** Number punctuation, by whether inside a quote/computer context. */
  private fun numberComma(inQuote: Boolean): String = if (inQuote) cell(2) else cell(3)

  private fun numberPoint(inQuote: Boolean): String = if (inQuote) cell(2, 5, 6) else cell(2)

  private fun isBraille(ch: Char): Boolean = ch.code in 0x2800..0x28FF

  private fun isLatin(ch: Char): Boolean = ch in 'a'..'z' || ch in 'A'..'Z'

  private fun isDigit(ch: Char): Boolean = ch in '0'..'9'

  /** Folds halfwidth katakana (with voiced marks) into fullwidth. */
  private fun normalize(text: String): String {
    if (text.none { it in '｡'..'ﾟ' }) return text
    val chars = ArrayList<String>()
    var i = 0
    while (i < text.length) {
      val cp = text.codePointAt(i)
      val ch = String(Character.toChars(cp))
      i += ch.length
      if (cp in 0xFF61..0xFF9F) {
        val norm = Normalizer.normalize(ch, Normalizer.Form.NFKC)
        if ((norm == "゙" || norm == "゚") && chars.isNotEmpty()) {
          val merged = Normalizer.normalize(chars.last() + norm, Normalizer.Form.NFKC)
          if (merged.codePointCount(0, merged.length) == 1) {
            chars[chars.size - 1] = merged
            continue
          }
        }
        var k = 0
        while (k < norm.length) {
          val c = norm.codePointAt(k)
          chars.add(String(Character.toChars(c)))
          k += Character.charCount(c)
        }
      } else {
        chars.add(ch)
      }
    }
    return chars.joinToString("")
  }

  /** First characters of two-character kana sequences. */
  private val KANA2_FIRST: Set<Char> = KANA2.keys.map { it[0] }.toSet()

  private enum class Context {
    /** Dot-6 quote. */
    COMPUTER,
    QUOTE,
  }

  /** Translates kana text into braille; returns (cells, input positions). */
  fun translateWithPos(text: String, nabcc: Boolean = false): Pair<String, List<Int>> {
    enforceMaxInputLength(text)
    val norm = normalize(text)
    // cell strings; joined at the end
    val out = ArrayList<String>()
    // one entry per cell
    val pos = ArrayList<Int>()
    // quote context: null (kana), COMPUTER (dot-6 quote), QUOTE
    var context: Context? = null
    // a foreign sign is still in effect (kana context)
    var latinActive = false
    var i = 0
    val n = norm.length

    fun emit(cells: String, index: Int) {
      out.add(cells)
      repeat(cells.length) { pos.add(index) }
    }

    fun prevChar(): Char? = if (i > 0) norm[i - 1] else null

    // True when the last meaningful character was a digit (skipping over a number-internal period
    // or comma).
    fun afterDigits(): Boolean {
      var j = i - 1
      while (j >= 0 && (norm[j] == '.' || norm[j] == ',')) j--
      return j >= 0 && isDigit(norm[j])
    }

    while (i < n) {
      val ch = norm[i]

      // fast path: kana (the common case)
      val two = if (ch in KANA2_FIRST && i + 2 <= n) norm.substring(i, i + 2) else ""
      val two2 = KANA2[two]
      if (two2 != null || ch in KANA1) {
        if (latinActive) latinActive = false
        if (i > 0 && isLatin(norm[i - 1])) emit(TSUNAGI, i - 1)
        if (two2 != null) {
          emit(two2.substring(0, 1), i)
          emit(two2.substring(1), i + 1)
          i += 2
        } else {
          emit(KANA1.getValue(ch), i)
          i += 1
        }
        continue
      }

      if (ch == ' ') {
        emit(" ", i)
        latinActive = false
        i += 1
        continue
      }

      if (isBraille(ch)) {
        if (ch.toString() == FOREIGN_OPEN) {
          context = if (prevChar()?.toString() == CAPITAL_SIGN) Context.COMPUTER else Context.QUOTE
        } else if (ch.toString() == FOREIGN_CLOSE) {
          context = null
        }
        emit(if (ch == '⠀') " " else ch.toString(), i)
        i += 1
        continue
      }

      if (nabcc && ch.code < 0x80) {
        val cells = NABCC[ch]
        if (cells != null) {
          emit(cells, i)
          i += 1
          continue
        }
      }

      if (isDigit(ch) || (ch == '\'' && context == null && i + 1 < n && isDigit(norm[i + 1]))) {
        latinActive = false
        // numeric sign, unless a literal numeric sign cell precedes
        if (!(out.isNotEmpty() && out.last().endsWith(NUMERIC_SIGN))) emit(NUMERIC_SIGN, i)
        if (ch == '\'') {
          // year abbreviation: apostrophe becomes dot 3 in a number
          emit(cell(3), i)
          i += 1
        }
        while (i < n) {
          val c = norm[i]
          if (isDigit(c)) {
            emit(DIGIT_CELLS.getValue(c), i)
            i += 1
          } else if (c == ',' && (i + 1 >= n || isDigit(norm[i + 1]))) {
            emit(numberComma(context != null), i)
            i += 1
          } else if (c == '.' && i + 1 < n && isDigit(norm[i + 1])) {
            emit(numberPoint(context != null), i)
            i += 1
          } else {
            break
          }
        }
        if (context == null && i < n) {
          val nxt = norm[i]
          var needsTsunagi = nxt in TSUNAGI_KANA
          if (!needsTsunagi && nxt in "ワヰヱヲ") {
            // wa-row kana: connector only when a-row kana follow in the same word (Tenyaku Tebiki
            // ch.4-4 1, remark 1)
            var j = i + 1
            while (j < n && norm[j] != ' ' && norm[j] != '　') {
              if (norm[j] in "アイウエオ") {
                needsTsunagi = true
                break
              }
              j++
            }
          }
          if (needsTsunagi) emit(TSUNAGI, i - 1)
        }
        continue
      }

      if (isLatin(ch)) {
        val start = i
        var j = i
        while (j < n && isLatin(norm[j])) j++
        val run = norm.substring(i, j)
        val first = run[0].lowercaseChar()
        if (context == null) {
          if (!latinActive) emit(FOREIGN_SIGN, start)
          latinActive = true
        } else if (
          first in DIGIT_LIKE_LETTERS &&
            (prevChar()?.let(::isDigit) == true || (context == Context.COMPUTER && afterDigits()))
        ) {
          // separate digit-like letters from a preceding number
          emit(FOREIGN_SIGN, start)
        }
        if (run.length > 1 && run.all { it.isUpperCase() }) {
          emit(CAPITAL_SIGN.repeat(2), start)
          for ((k, c) in run.withIndex()) emit(LATIN.getValue(c.lowercaseChar()), start + k)
        } else {
          for ((k, c) in run.withIndex()) {
            if (c.isUpperCase()) emit(CAPITAL_SIGN, start + k)
            emit(LATIN.getValue(c.lowercaseChar()), start + k)
          }
        }
        i = j
        continue
      }

      if (context != null) {
        val table = if (context == Context.COMPUTER) COMPUTER_SYMBOL else QUOTE_SYMBOL
        val cells = table[ch]
        if (cells != null) {
          emit(cells, i)
          i += 1
          continue
        }
      }

      // kana context from here on
      if (ch == '/' && latinActive) {
        // a slash does not end the foreign sign's scope
        emit(SYMBOL.getValue('/'), i)
        i += 1
        continue
      }

      if (ch == ',' && latinActive) {
        // comma between Latin items; the foreign sign scope ends
        emit(cell(2), i)
        latinActive = false
        i += 1
        continue
      }

      latinActive = false

      val punct = SENTENCE_PUNCT[ch]
      if (punct != null) {
        emit(punct.first, i)
        if (i + 1 < n) emit(" ".repeat(punct.second), i)
        i += 1
        continue
      }

      val literal = LITERAL_SYMBOL[ch]
      if (literal != null) {
        emit(literal, i)
        i += 1
        continue
      }

      val letter = LETTER_CELLS[ch.toString()]
      if (letter != null) {
        emit(letter, i)
        i += 1
        continue
      }

      val spaced = SPACED_SYMBOL[ch]
      if (spaced != null) {
        if (spaced.before && out.isNotEmpty() && !out.last().endsWith(" ")) emit(" ", i)
        emit(spaced.cells, i)
        if (spaced.after && i + 1 < n) emit(" ", i)
        i += 1
        continue
      }

      val shape = SHAPE_SYMBOL[ch]
      if (shape != null) {
        emit(shape, i)
        i += 1
        continue
      }

      val enclosure = ENCLOSURE[ch]
      if (enclosure != null) {
        emit(enclosure, i)
        i += 1
        continue
      }

      val symbol = SYMBOL[ch]
      if (symbol != null) {
        emit(symbol, i)
        i += 1
        continue
      }

      if (ch == ' ' || ch == '　') {
        emit(" ", i)
        i += 1
        continue
      }

      // unknown character: emit a placeholder cell so that the 1-input-char-to-1-output-cell
      // position mapping is preserved for scripts outside this translator's coverage (e.g.
      // Hebrew). translator2 replaces "□" with a space in its final output.
      emit("□", i)
      i += Character.charCount(norm.codePointAt(i))
    }

    return out.joinToString("") to pos
  }

  /** Compatibility alias matching the historical NVDAJP entry point name. */
  fun translateWithInPos(text: String, nabcc: Boolean = false): Pair<String, List<Int>> =
    translateWithPos(text, nabcc)
}
