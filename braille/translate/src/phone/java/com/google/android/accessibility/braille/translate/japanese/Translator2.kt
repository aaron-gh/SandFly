/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) translator2.py at 307f3dd.
 * Copyright (C) 2012-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 *
 * Originally developed as part of NVDA Japanese (nvdajp); relicensed from GPL to BSD 3-Clause by
 * the copyright holder for libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import java.math.BigInteger

/**
 * Japanese braille translator, phase 2: word separation (masuake) by morphological analysis, then
 * kana to braille by [Kana]. [analyze] gives the MeCab feature lines of a text (the surface, then
 * the comma-separated features of the JTalk extended dictionary, corrected by [MecabCorrect]).
 *
 * Positions are UTF-16 indexes, where libkuraji's are code point indexes; the two differ only
 * around characters outside the Basic Multilingual Plane.
 */
internal class Translator2(private val analyze: (String) -> List<String>) {

  /** Translates the text inside foreign quotation marks with another table, such as grade 2. */
  fun interface ForeignTranslator {
    /** Returns the braille of [text], or null when it can't be translated. */
    fun translate(text: String): ForeignTranslation?
  }

  /**
   * [braille] is Unicode braille, or liblouis dotsIO cells (U+8000..U+80FF). [inPos] gives the
   * input position of each output cell, [outPos] the output position of each input character.
   */
  class ForeignTranslation(val braille: String, val inPos: List<Int>, val outPos: List<Int>)

  /**
   * The result of [translateWithInPos2]: [text] is the kana text with word spaces, [braille] its
   * braille, [inpos1] the position in [text] of each braille cell and [inpos2] the position in the
   * input of each character of [text].
   */
  data class Separation(
    val text: String,
    val braille: String,
    val inpos1: List<Int>,
    val inpos2: List<Int>,
  )

  /**
   * The result of [translate]: Unicode braille with U+0020 blanks, the input position of each
   * cell, the output position of each input character, and the cursor's position in the braille.
   */
  data class Translation(
    val braille: String,
    val inpos: List<Int>,
    val outpos: List<Int>,
    val cursor: Int,
  )

  internal class MecabMorph {
    var hyouki = "" // 表記
    var nhyouki = "" // Unicode 正規化された表記
    var hinshi1 = ""
    var hinshi2 = ""
    var hinshi3 = ""
    var hinshi4 = ""
    var type1 = ""
    var type2 = ""
    var kihon = ""
    var kana = ""
    var yomi = ""
    var accent = ""
    var output = ""
    var sepflag = false // この後でマスアケをするか？

    fun clone(): MecabMorph {
      val m = MecabMorph()
      m.hyouki = hyouki
      m.nhyouki = nhyouki
      m.hinshi1 = hinshi1
      m.hinshi2 = hinshi2
      m.hinshi3 = hinshi3
      m.hinshi4 = hinshi4
      m.type1 = type1
      m.type2 = type2
      m.kihon = kihon
      m.kana = kana
      m.yomi = yomi
      m.accent = accent
      m.output = output
      m.sepflag = sepflag
      return m
    }

    // 付属語
    fun isSubstantiveWord(): Boolean {
      if (hinshi1 == "記号") return false
      if (hinshi2 == "接頭") return true
      if (hinshi2 == "接尾") return true
      if (hinshi1 == "助動詞" && hyouki == "ない") return false
      if (hinshi1 == "名詞" && hyouki == "の") return true
      if (hinshi1 == "形容詞" && hyouki == "なく") return true
      if (hinshi1 == "助動詞" || hinshi1 == "助詞") return true
      return false
    }

    // 自立語
    fun isIndependentWord(): Boolean {
      if (hinshi1 == "記号") return false
      return !isSubstantiveWord()
    }
  }

  /**
   * Separates [inbuf] into words and translates it to braille. [useForeignQuotes] puts words in
   * Latin letters in foreign quotation marks; [foreign] then translates their inside.
   */
  fun translateWithInPos2(
    inbuf: String,
    nabcc: Boolean = false,
    foreign: ForeignTranslator? = null,
    foreignIsUsGrade2: Boolean = false,
    useForeignQuotes: Boolean = false,
  ): Separation {
    var outbuf: String
    var inpos2: List<Int>
    val alreadyBraille: Boolean
    // do not translate if string is unicode braille
    if (inbuf.all { it.code in 0x2800..0x28FF || it == ' ' }) {
      outbuf = inbuf
      inpos2 = inbuf.indices.toList()
      alreadyBraille = true
    } else {
      val separated = japaneseBrailleSeparate(inbuf, nabcc, useForeignQuotes)
      outbuf = separated.first
      inpos2 = separated.second
      alreadyBraille = false
    }
    // nvdajp: translator_louis — 外国語引用符内を liblouis 2級に変換。既に点字の入力はスキップ（no-op で位置がずれないように）。
    if (foreign != null && !alreadyBraille) {
      val applied = applyForeignTranslator(outbuf, inpos2, foreign, foreignIsUsGrade2)
      outbuf = applied.first
      inpos2 = applied.second
    }
    val (result, inpos1) = Kana.translateWithInPos(outbuf, nabcc)
    return Separation(outbuf, result.replace("□", " "), inpos1, inpos2)
  }

  /**
   * Translates [inbuf] to Unicode braille (U+0020 for blanks), with position maps, as liblouis
   * translate does. [cursorPos] is mapped to the braille.
   */
  fun translate(
    inbuf: String,
    cursorPos: Int = 0,
    nabcc: Boolean = false,
    foreign: ForeignTranslator? = null,
    foreignIsUsGrade2: Boolean = false,
    useForeignQuotes: Boolean = false,
  ): Translation {
    enforceMaxInputLength(inbuf)
    val separation =
      translateWithInPos2(inbuf, nabcc, foreign, foreignIsUsGrade2, useForeignQuotes)
    val outbuf = separation.braille
    val (inPos, outPos) =
      mergePositionMap(separation.inpos1, separation.inpos2, outbuf.length, inbuf.length)
    val cursor = if (outPos.isEmpty()) 0 else outPos[cursorPos.coerceIn(0, outPos.size - 1)]
    return Translation(outbuf, inPos, outPos, cursor)
  }

  private fun japaneseBrailleSeparate(
    inbuf: String,
    nabcc: Boolean,
    useForeignQuotes: Boolean,
  ): Pair<String, List<Int>> {
    var text = inbuf
    // The index in inbuf of each character of text, through the replacements that change its
    // length, so that the positions returned are inbuf's.
    val original = MutableList(inbuf.length) { it }
    // NVDA focus/review strings may contain CR/LF; MeCab path rejects them.
    if ('\r' in text || '\n' in text) {
      text = replaceMapped(text, original, "\r\n", "\n").replace("\r", "\n").replace("\n", " ")
    }
    if (RE_HALF_KATAKANA.matches(text)) {
      return text to inbufPositions(text.indices.toList(), original)
    }

    if (!nabcc && RE_MB_ALPHA_NUM_SPACE.matches(text)) {
      val outbuf = UnicodeUtil.unicodeNormalize(text)
      // fall through if contains alpha char (to be wrapped with foreign quote)
      if (useForeignQuotes && !outbuf.any { it.isLetter() }) {
        return outbuf to inbufPositions(outbuf.indices.toList(), original)
      }
      // legacy behavior: always return result here
      if (!useForeignQuotes) {
        return outbuf to inbufPositions(outbuf.indices.toList(), original)
      }
    }

    if (!nabcc && isGaiji(text) && ' ' in text.trimEnd()) {
      var rspaces = ""
      while (text.last() == ' ') {
        rspaces += " "
        text = text.dropLast(1)
      }
      val outbuf = "⠦" + UnicodeUtil.unicodeNormalize(text) + "⠴" + rspaces
      val inpos2 = mutableListOf(0)
      inpos2.addAll(outbuf.indices)
      inpos2.add(inpos2.last())
      return outbuf to inbufPositions(inpos2, original)
    }

    // 'あ゛ー' Unicode 正規化されて空白が入るので事前に補正する
    text = replaceMapped(text, original, "あ゛", "あ")
    text = text.replace("ヱ゛", "ヴェ")
    text = text.replace("ヲ゛", "ヴォ")
    text = text.replace("ワ゛", "ヴァ")

    // tab code
    text = text.replace("\t", TAB_CODE)

    // 'ふにゃ～'
    text = text.replace("ゃ～", "ゃー")

    // NFKC normalization changes the character count for some characters
    // (U+2026 HORIZONTAL ELLIPSIS -> "...", U+2469 CIRCLED NUMBER TEN -> "10"),
    // so the normalization inside the analyzer (text2mecab) would make inpos2
    // drift from the original text. Normalize here with a position map
    // instead; NFKC is idempotent, so the second normalization inside the
    // analyzer no longer changes the length. nvdajp issues #117, #328
    val (normalized, nfkcMap) = UnicodeUtil.nfkcNormalizeWithMap(text)
    text = normalized
    var li = mecabToMorphs(analyze(text)).filter { it.hyouki.isNotEmpty() }

    for (mo in li) {
      if (TAB_CODE in mo.nhyouki) {
        mo.hinshi1 = "記号"
        // mo.hinshi2 = '空白'
        mo.kana = mo.nhyouki
        mo.yomi = mo.nhyouki
        mo.output = mo.nhyouki
      }
    }

    for (mo in li) {
      if (mo.hinshi1 == "空白") {
        mo.output = " "
      } else if (mo.hyouki.startsWith("•")) {
        // List bullet (U+2022) should be rendered as a bullet mark (U+2810, dot 5) in braille (nvdajp#723)
        mo.output = "⠐"
      } else if (mo.hinshi2 == "数" && isDigit(mo.nhyouki)) {
        mo.output = mo.nhyouki
      } else if (RE_GREEK_CYRILLIC.matches(mo.nhyouki)) {
        // Greek and Cyrillic words are unknown to MeCab (no reading) and
        // would otherwise be dropped because their output stays empty.
        // Pass the characters through so that translator1 renders them
        // with Greek or Russian braille patterns. nvdajp issues #224, #456
        mo.output = mo.nhyouki
      }
    }

    li = replaceMorphs(li, CONNECTED_MORPHS)

    // before:
    // たー,たー,助動詞,*,*,*,*,*,たー,ター,ター,1/2,ター,0
    // ー,ー,名詞,一般,*,*,*,*,*,,,,,0
    // after:
    // た,た,助動詞,*,*,*,*,*,た,タ,タ,1/2,タ,0
    // ー,ー,名詞,一般,*,*,*,*,*,,,,ー,0

    // before: ３ー,名詞,数,*,*,*,*,３ー,サンー,サンー,1/3,C0
    // after:  ３,名詞,数,*,*,*,*,３,サン,サン,1/3,C0
    for (pos in 0 until li.size - 1) {
      val mo = li[pos]
      val mo2 = li[pos + 1]
      if ("ー" in mo.hyouki && mo2.hyouki == "ー") {
        mo.hyouki = mo.hyouki.replace("ー", "")
        mo.kihon = mo.hyouki
        mo.nhyouki = UnicodeUtil.unicodeNormalize(mo.hyouki)
        mo.kana = mo.kana.replace("ー", "")
        mo.yomi = mo.yomi.replace("ー", "")
        mo.output = if (mo.hinshi2 == "数") mo.nhyouki else mo.yomi
      }
    }

    // 動詞のウ音便
    // before:
    // 思う,思う,動詞,自立,*,*,五段・ワ行ウ音便,連用タ接続,思う,オモウ,オモウ,2/3,オモウ,0
    // て,て,助詞,接続助詞,*,*,*,*,て,テ,テ,0/1,テ,0
    // after:
    // 思う,思う,動詞,自立,*,*,五段・ワ行ウ音便,連用タ接続,思う,オモウ,オモウ,2/3,オモー,0
    // て,て,助詞,接続助詞,*,*,*,*,て,テ,テ,0/1,テ,0
    for (pos in 0 until li.size - 1) {
      val mo = li[pos]
      val mo2 = li[pos + 1]
      if (
        mo.hinshi1 == "動詞" &&
          mo.hyouki in setOf("思う", "吸う", "繕う") &&
          len(mo.yomi) > 1 &&
          mo.yomi.endsWith("ウ") &&
          mo2.yomi.take(1) in setOf("タ", "テ")
      ) {
        mo.output = mo.yomi.dropLast(1) + "ー"
        // https://github.com/nvdajp/nvdajpmiscdep/issues/42
        // https://github.com/nvdajp/nvdajpmiscdep/issues/63
      }
    }

    li = replaceDigitMorphs(li)
    li = rewriteNumber(li)

    // before: う,う,助動詞,*,*,*,ウ,ウ,0/1,ウ,0
    // after:  う,う,助動詞,*,*,*,ウ,ウ,0/1,ー,0
    for (mo in li) {
      if (mo.hyouki == "う" && mo.hinshi1 == "助動詞") mo.output = "ー"
    }

    // before: ａ,a,記号,アルファベット,*,*,エイ,エイ,1/2,エイ,0
    // after:  ａ,a,記号,アルファベット,*,*,エイ,エイ,1/2,a,0
    for (mo in li) {
      if (mo.hinshi2 == "アルファベット") mo.output = mo.nhyouki
    }

    li = replaceAlphabetMorphs(li, nabcc)
    if (useForeignQuotes && !nabcc) li = mergeParenthesizedAlphabetMorphs(li)

    for (mo in li) {
      if (mo.hyouki == "〝") {
        mo.hinshi1 = "記号"
        mo.hinshi2 = "括弧開"
      }
      if (mo.hyouki == "〟") {
        mo.hinshi1 = "記号"
        mo.hinshi2 = "括弧閉"
      }
      if (mo.hyouki == "々々々々") {
        mo.hinshi1 = "記号"
        mo.hinshi2 = "一般"
      }
      if (mo.hyouki == "〻") {
        // 303b 二の字点（にのじてん）
        mo.hinshi1 = "記号"
        mo.hinshi2 = "一般"
      }
    }

    for (mo in li) {
      if (mo.hinshi2 == "括弧開" || mo.hinshi2 == "括弧閉") mo.output = mo.nhyouki
    }

    // before: 　, ,記号,空白,*,*,　,　,*/*,　,0
    // after:  　, ,記号,空白,*,*,　,　,*/*, ,0
    for (mo in li) {
      if (mo.hyouki == "　") mo.output = " " // full shape space
    }

    // before: ー,ー,名詞,一般,*,*,*,*,*,,,,,0
    // after:  ー,ー,名詞,一般,*,*,*,*,*,,,,ー,0
    for (mo in li) {
      if (mo.hyouki == "ー" && mo.hinshi1 == "名詞") {
        mo.hinshi1 = "記号"
        mo.output = "ー"
      }
    }

    // 数字の前の全角アポストロフィを半角にする
    // before:
    // ’,’,記号,括弧閉,*,*,’,’,*/*,’,0
    // ０,0,名詞,数,*,*,ゼロ,ゼロ,1/2,0,0
    // after:
    // ’,’,記号,括弧閉,*,*,’,’,*/*,',0
    // ０,0,名詞,数,*,*,ゼロ,ゼロ,1/2,0,0
    for (pos in 0 until li.size - 1) {
      if (li[pos].hyouki == "’" && li[pos + 1].hinshi2 == "数") li[pos].output = "'"
    }

    // 算用数字ではさまれた読点を数符にする
    // before:
    // 二,二,名詞,数,*,*,2,2,1/2,2,0
    // 、,、,記号,読点,*,*,、,、,*/*,、,0
    // 三,三,名詞,数,*,*,3,3,1/2,3,0
    // after:
    // 二,二,名詞,数,*,*,2,2,1/2,2,0
    // 、,、,記号,読点,*,*,、,、,*/*,⠼,0
    // 三,三,名詞,数,*,*,3,3,1/2,3,0
    for (pos in 1 until li.size - 1) {
      if (isDigit(li[pos - 1].output) && li[pos].hyouki == "、" && isDigit(li[pos + 1].output)) {
        li[pos].output = if (nabcc) "." else "⠼"
      }
    }

    // 算用数字ではさまれた中点を数符にする
    if (!nabcc) {
      for (pos in 1 until li.size - 1) {
        if (isDigit(li[pos - 1].output) && li[pos].hyouki == "・" && isDigit(li[pos + 1].output)) {
          li[pos].output = "⠼"
        }
      }
    }

    // before: ａｂ,ab,名詞,一般,*,*,アブ,アブ,1/2,アブ,0
    // after:  ａｂ,ab,名詞,一般,*,*,アブ,アブ,1/2,ab,0
    // before: Ｎｏ．,No.,接頭詞,数接続,*,*,ナンバー,ナンバー,1/4,ナンバー,0
    // after:  Ｎｏ．,No.,接頭詞,数接続,*,*,ナンバー,ナンバー,1/4,No.,0
    for (mo in li) {
      if (RE_ASCII_CHARS.matches(mo.nhyouki)) mo.output = mo.nhyouki
    }

    // before: ヒロイノ,ヒロイノ,名詞,一般,*,*,,,,,0
    // after:  ヒロイノ,ヒロイノ,名詞,一般,*,*,,,,ヒロイノ,0
    // before: ィ,ィ,名詞,一般,*,*,,,,,0
    // after:  ィ,ィ,名詞,一般,*,*,,,,ィ,0
    // before: ぁ,ぁ,名詞,一般,*,*,,,,,0
    // after:  ぁ,ぁ,名詞,一般,*,*,,,,ァ,0
    for (mo in li) {
      if (mo.output.isEmpty() && mo.nhyouki != "ー") {
        if (RE_KATAKANA.matches(mo.nhyouki)) {
          mo.output = mo.nhyouki
        } else if (RE_HIRAGANA.matches(mo.nhyouki)) {
          mo.output = mo.nhyouki.map { it + 0x60 }.joinToString("")
        } else if (RE_ASCII_AND_SYMBOLS.matches(mo.nhyouki)) {
          // Fallback for ASCII letters/digits mixed with non-ASCII symbols
          // such as × (U+00D7): e.g. "a×b×c", "SEO×"
          // See https://github.com/nvdajp/nvdajp/issues/534
          mo.output = mo.nhyouki
        }
      }
    }

    // 単語が小文字カタカナのみであれば修正
    // 表記は修正せず should_separate() で小文字として判定される
    for (mo in li) {
      SMALL_KANA[mo.output]?.let { mo.output = it }
    }

    // 記号を Unicode 正規化
    // 踊り字の処理
    for (i in li.indices) {
      val mo = li[i]
      if (mo.hinshi1 == "記号" && mo.hinshi2 == "一般") {
        if (mo.hyouki == "〻") {
          mo.output = "ニノジテン"
        } else if (mo.hyouki == "ゝ" && i > 0) {
          mo.output = toNoDakuonKana(li[i - 1].output.takeLast(1))
        } else if (mo.hyouki == "ゞ" && i > 0) {
          mo.output = toDakuonKana(li[i - 1].output.takeLast(1))
        } else if (mo.hyouki == "ヽ" && i > 0) {
          mo.output = toNoDakuonKana(li[i - 1].output.takeLast(1))
        } else if (mo.hyouki == "ヾ" && i > 0) {
          mo.output = toDakuonKana(li[i - 1].output.takeLast(1))
        } else if (mo.hyouki == "々々々々" && i > 0) {
          mo.output = li[i - 1].output.repeat(4)
        } else if (mo.hyouki == "々々" && i > 0) {
          mo.output = li[i - 1].output.repeat(2)
        } else if (mo.hyouki == "々" && i > 0) {
          if (li[i - 1].hyouki.firstOrNull() == '々' && i > 1) {
            mo.output = li[i - 2].output
          } else if (len(li[i - 1].hyouki) == 1) {
            mo.output = li[i - 1].output
          } else {
            mo.output = "" // FIXME
          }
        } else {
          mo.output = mo.nhyouki
        }
      }
      if (mo.hyouki == "．" && mo.hinshi1 == "名詞" && mo.hinshi2 == "数") mo.output = "."
      if (mo.hyouki == "，" && mo.hinshi1 == "名詞" && mo.hinshi2 == "数") mo.output = ","
      if (mo.hinshi1 == "記号" && mo.hinshi2 == "句点" && mo.nhyouki == ".") mo.output = "."
      if (mo.hinshi1 == "記号" && mo.hinshi2 == "読点" && mo.nhyouki == ",") mo.output = ","
    }

    for (mo in li) {
      mo.nhyouki = UnicodeUtil.unicodeNormalize(mo.nhyouki)
      // 情報処理点字の開始記号と終了記号
      var info = false
      if (RE_INFORMATION.matches(mo.nhyouki) && '@' in mo.nhyouki && len(mo.nhyouki) > 1) {
        info = true
      }
      if ("://" in mo.nhyouki) info = true
      if ('\\' in mo.nhyouki) info = true
      if (mo.nhyouki.firstOrNull() == '[' && mo.nhyouki.lastOrNull() == ']') info = true
      if (info) {
        mo.output = if (nabcc) mo.nhyouki else "⠠⠦" + mo.nhyouki + "⠠⠴"
      } else if (
        // 外国語引用符
        // 空白をはさまない1単語は外国語引用符ではなく外字符で
        (RE_GAIJI.matches(mo.nhyouki) && (' ' in mo.nhyouki || '\'' in mo.nhyouki)) ||
          (useForeignQuotes && RE_GAIJI_WITH_PARENS.matches(mo.nhyouki)) ||
          ('.' in mo.nhyouki && len(mo.nhyouki) > 3) ||
          // "0's", "80's"
          RE_DIGIT_SINGLE_ALPHA.matches(mo.nhyouki)
      ) {
        mo.output = if (nabcc) mo.nhyouki else "⠦" + mo.nhyouki + "⠴"
      }
    }

    if (!nabcc) {
      for (mo in li) {
        // 情報処理点字でも外国語引用符でもなく output が & を含む場合は前後をあける
        if (!mo.output.startsWith("⠠⠦") && !mo.output.startsWith("⠦")) {
          if (mo.output == "&") {
            // &
            continue
          } else if (mo.output.startsWith("&")) {
            // &xx
            mo.output = mo.output.replace("&", "& ")
          } else if (mo.output.endsWith("&")) {
            // xx&
            mo.output = mo.output.replace("&", " &")
          } else {
            // xx&xx
            mo.output = mo.output.replace("&", " & ")
          }
        }
      }
    }

    if (nabcc) {
      for (mo in li) {
        mo.output = mo.output.replace("”", "\"").replace("’", "'").replace("‘", "`")
      }
    }

    // 日付の和語読み処理
    li = fixJapaneseDateMorphs(li)

    // 日本語の直後のコンマを '、' で解釈
    // before: ，,記号,読点,*,*,*,*,，,，,，,*/*,*
    // after:  、,記号,読点,*,*,*,*,、,、,、,*/*,*
    for (pos in 0 until li.size - 1) {
      val mo = li[pos]
      val mo2 = li[pos + 1]
      if (mo2.hyouki == "，" && mo.hinshi2 !in setOf("アルファベット", "数", "括弧閉")) {
        mo2.hyouki = "、"
        mo2.nhyouki = "、"
        mo2.output = "、"
      }
    }

    // 分かち書き判定
    for (i in 1 until li.size) {
      val prev2Mo = if (i - 2 >= 0) li[i - 2] else null
      val prevMo = li[i - 1]
      val nextMo = if (i + 1 < li.size) li[i + 1] else null
      li[i - 1].sepflag = shouldSeparate(prev2Mo, prevMo, li[i], nextMo, nabcc)
    }

    // do not translate if string is unicode braille
    for (i in li.indices) {
      val mo = li[i]
      if (mo.hyouki.all { it.code in 0x2800..0x28FF || it == '　' }) {
        mo.output = mo.hyouki.replace("　", " ")
        mo.sepflag = false
        if (i > 0) li[i - 1].sepflag = false
      }
    }

    var (outbuf, inpos2) = morphsToString(li, inbuf)

    // inpos2 holds positions in the normalized text; map them back to
    // positions in the original text.
    if (nfkcMap.isNotEmpty()) {
      val last = nfkcMap.size - 1
      inpos2 = inpos2.map { nfkcMap[minOf(it, last)] }.toMutableList()
    }

    outbuf = if (nabcc) outbuf.replace(TAB_CODE, "⡀") else outbuf.replace(TAB_CODE, " ")

    return outbuf to inbufPositions(inpos2, original)
  }

  /**
   * Replaces [old] with [new] in [text], with [map] holding each character's index in the original
   * text: the characters of a replacement take those of the text it replaced.
   */
  private fun replaceMapped(text: String, map: MutableList<Int>, old: String, new: String): String {
    if (old !in text) return text
    val out = StringBuilder()
    val outMap = ArrayList<Int>(map.size)
    var i = 0
    while (i < text.length) {
      if (text.startsWith(old, i)) {
        for (k in new.indices) outMap += map[i + minOf(k, old.length - 1)]
        out.append(new)
        i += old.length
      } else {
        out.append(text[i])
        outMap += map[i]
        i++
      }
    }
    map.clear()
    map.addAll(outMap)
    return out.toString()
  }

  /**
   * [positions] in the text after replacements, as positions in the original text by [map]. When
   * no replacement changed the length, they are left as they are, including any past its end.
   */
  private fun inbufPositions(positions: List<Int>, map: List<Int>): List<Int> =
    if (map.isEmpty() || map.withIndex().all { (i, v) -> i == v }) positions
    else positions.map { map[it.coerceIn(0, map.lastIndex)] }

  /**
   * Replaces the inside of each foreign quotation ⠦...⠴ in [outbuf] by its translation from
   * [foreign], and updates [inpos2] to match.
   */
  private fun applyForeignTranslator(
    outbuf: String,
    inpos2: List<Int>,
    foreign: ForeignTranslator,
    isUsGrade2: Boolean,
  ): Pair<String, List<Int>> {
    var text = outbuf
    var positions = inpos2.toMutableList()
    // inner ranges (start..end, end exclusive)
    val ranges = findForeignQuoteRanges(text)
    // Process from end to start so indices stay valid
    for ((start, end) in ranges.asReversed()) {
      val inner = text.substring(start, end)
      if (isUsGrade2 && RE_US_G2_DOTTED_TOKEN.matches(inner)) continue
      // leave this segment as-is on error
      val translation =
        try {
          foreign.translate(inner)
        } catch (e: RuntimeException) {
          null
        } ?: continue
      val grade2 = louisCellsToBrailleString(translation.braille)
      val oldSegment =
        if (start < positions.size) positions.subList(start, minOf(end, positions.size)).toList()
        else emptyList()
      val newSegment =
        mapLouisPositions(oldSegment, translation.inPos, translation.outPos, grade2.length)
      text = text.substring(0, start) + grade2 + text.substring(end)
      val updated = ArrayList<Int>(positions.size)
      updated.addAll(positions.subList(0, minOf(start, positions.size)))
      updated.addAll(newSegment)
      if (end < positions.size) updated.addAll(positions.subList(end, positions.size))
      positions = updated
    }
    return text to positions
  }

  internal companion object {
    private const val TAB_CODE = "​"

    // 外国語引用符は ⠦ (U+2826) ... ⠴ (U+2834)。情報処理用 ⠠⠦...⠠⠴ は対象外。
    private const val FOREIGN_OPEN = '⠦' // ⠦
    private const val FOREIGN_CLOSE = '⠴' // ⠴
    private const val INFO_PREFIX = '⠠' // ⠠

    private class Connected(
      val hyouki: String,
      val yomi: String,
      val accent: String,
      val hinshi1: String?,
      val hinshi2: String?,
      val hinshi3: String?,
    )

    private fun c(
      hyouki: String,
      yomi: String,
      accent: String,
      hinshi1: String?,
      hinshi2: String?,
      hinshi3: String?,
    ) = Connected(hyouki, yomi, accent, hinshi1, hinshi2, hinshi3)

    private val CONNECTED_MORPHS: Map<String, List<Connected>> =
      mapOf(
        "について" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("ついて", "ツイテ", "1/3", "動詞", "*", "*")),
        "により" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("より", "ヨリ", "0/2", "動詞", "*", "*")),
        "による" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("よる", "ヨル", "0/2", "動詞", "*", "*")),
        "において" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("おいて", "オイテ", "0/3", "動詞", "*", "*")),
        "における" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("おける", "オケル", "0/3", "動詞", "*", "*")),
        "によって" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("よって", "ヨッテ", "0/3", "動詞", "*", "*")),
        "にとって" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("とって", "トッテ", "0/3", "動詞", "*", "*")),
        "に対して" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("対して", "タイシテ", "1/4", "動詞", "*", "*")),
        "に関して" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("関して", "カンシテ", "1/4", "動詞", "*", "*")),
        "につき" to listOf(c("に", "ニ", "0/1", null, null, "*"), c("つき", "ツキ", "1/2", "動詞", "*", "*")),
        "という" to listOf(c("と", "ト", "0/1", null, null, "*"), c("いう", "イウ", "0/2", "動詞", "*", "*")),
        "どうして" to listOf(c("どう", "ドー", "0/2", null, null, "*"), c("して", "シテ", "0/2", "動詞", "*", "*")),
        "として" to listOf(c("と", "ト", "1/1", null, null, "*"), c("して", "シテ", "0/2", "動詞", "*", "*")),
        "なくなる" to listOf(c("なく", "ナク", "2/2", null, null, null), c("なる", "ナル", "1/2", "動詞", "自立", null)),
        "（日）" to
          listOf(c("（", "(", "*/*", "記号", "括弧開", "*"), c("日", "ニチ", "1/2", "名詞", "一般", null), c("）", ")", "*/*", "記号", "括弧閉", "*")),
        "（月）" to
          listOf(c("（", "(", "*/*", "記号", "括弧開", "*"), c("月", "ゲツ", "1/2", "名詞", "一般", null), c("）", ")", "*/*", "記号", "括弧閉", "*")),
        "（火）" to
          listOf(c("（", "(", "*/*", "記号", "括弧開", "*"), c("火", "カ", "1/1", "名詞", "一般", null), c("）", ")", "*/*", "記号", "括弧閉", "*")),
        "（水）" to
          listOf(c("（", "(", "*/*", "記号", "括弧開", "*"), c("水", "スイ", "1/2", "名詞", "一般", null), c("）", ")", "*/*", "記号", "括弧閉", "*")),
        "（木）" to
          listOf(c("（", "(", "*/*", "記号", "括弧開", "*"), c("木", "モク", "1/2", "名詞", "一般", null), c("）", ")", "*/*", "記号", "括弧閉", "*")),
      )

    private val RE_KANSUJI = Regex("^[一二三四五六七八九〇零十拾百千壱二参]+$")
    private val RE_ALPHA_OR_SINGLE = Regex("^[A-Za-z']+$")
    private val RE_ASCII_SYMBOLS =
      Regex("""^[\,\.\:\;\!\?\@\#\\\$\%\&\*\|\+\-\/\=\<\>\"'\^\`\_\~]+$""")
    private val RE_MB_ALPHA_NUM_SPACE = Regex("""^[0-9A-Za-z\- ０-９Ａ-Ｚａ-ｚ　]+$""")
    // Greek (U+0370-U+03FF) and Cyrillic (U+0400-U+04FF) letters
    private val RE_GREEK_CYRILLIC = Regex("^[Ͱ-ӿ]+$")
    private val RE_ASCII_CHARS = Regex("""^[A-Za-z0-9\.\,\-\+\:\/\~\?\&\%\#\*\$\; ]+$""")
    private val RE_ASCII_AND_SYMBOLS =
      Regex("""^[A-Za-z0-9\.\,\-\+\:\/\~\?\&\%\#\*\$\; ×]+$""")
    private val RE_INFORMATION = Regex("""^[A-Za-z0-9\+\@\/\#\$\%\&\*\;\.\<\>\-\_\{\}\[\] ]+$""")
    private val RE_GAIJI = Regex("""^[A-Za-z][A-Za-z0-9\,\.\+\-'\!\? ]+$""")
    private val RE_GAIJI_WITH_PARENS =
      Regex("""^[A-Za-z][A-Za-z0-9\,\.\+\-'\!\? ]*\([A-Za-z0-9\,\.\+\-'\!\? ]+\)$""")
    private val RE_PAREN_ASCII_BODY = Regex("""^[A-Za-z0-9\,\.\+\-'\!\? ]+$""")
    // US 2級で前置符・インジケータが入りやすい「ドット区切り英数字トークン」
    // 例: www.example.com, example.co.jp, v1.4, 1.0, Dr.Smith, 192.168.0.1
    // - 空白を含まない
    // - ドット `.` で区切られたラベルが 2 個以上
    // - 各ラベルは先頭が英数字、以降は英数字・ハイフン・アンダースコア
    private val RE_US_G2_DOTTED_TOKEN =
      Regex("""^[A-Za-z0-9][A-Za-z0-9_-]*(?:\.[A-Za-z0-9][A-Za-z0-9_-]*)+$""")
    private val RE_KATAKANA = Regex("^[ァ-ヾ]+$")
    private val RE_HIRAGANA = Regex("^[ぁ-ゞ]+$")
    private val RE_HALF_KATAKANA = Regex("^[ｦ-ﾟ]+$") // ff66 .. ff9f
    private val RE_DIGIT_SINGLE_ALPHA = Regex("^[0-9]+'[A-Za-z]+$")

    private val NO_DAKUON_DIC =
      mapOf(
        "ガ" to "カ", "ギ" to "キ", "グ" to "ク", "ゲ" to "ケ", "ゴ" to "コ",
        "ザ" to "サ", "ジ" to "シ", "ズ" to "ス", "ゼ" to "セ", "ゾ" to "ソ",
        "ダ" to "タ", "ヂ" to "チ", "ヅ" to "ツ", "デ" to "テ", "ド" to "ト",
        "バ" to "ハ", "ビ" to "ヒ", "ブ" to "フ", "ベ" to "ヘ", "ボ" to "ホ",
      )

    private val DAKUON_DIC = NO_DAKUON_DIC.entries.associate { (k, v) -> v to k }

    private val SMALL_KANA =
      mapOf(
        "ァ" to "ア", "ィ" to "イ", "ゥ" to "ウ", "ェ" to "エ", "ォ" to "オ", "ッ" to "ツ",
        "ャ" to "ヤ", "ュ" to "ユ", "ョ" to "ヨ", "ヮ" to "ワ", "ヵ" to "カ", "ヶ" to "ケ",
      )

    // 日付の和語読み処理
    // すでに output 属性に半角数字が格納されている前提

    // 後続する '日' と形態素を結合する
    private val WAGO_DIC =
      mapOf(
        "1" to "ツイタチ", "2" to "フツカ", "3" to "ミッカ", "4" to "ヨッカ", "5" to "イツカ",
        "6" to "ムイカ", "7" to "ナノカ", "8" to "ヨーカ", "9" to "ココノカ", "10" to "トオカ",
        "20" to "ハツカ",
      )

    /** Python's len(): the number of code points. */
    private fun len(s: String): Int = s.codePointCount(0, s.length)

    /** Python's str.isdigit(). */
    fun isDigit(s: String): Boolean {
      if (s.isEmpty()) return false
      var i = 0
      while (i < s.length) {
        val cp = s.codePointAt(i)
        if (!isDigitCodePoint(cp)) return false
        i += Character.charCount(cp)
      }
      return true
    }

    /** Whether the first character of [s] is a digit, as Python's s[0].isdigit(). */
    private fun startsWithDigit(s: String): Boolean =
      s.isNotEmpty() && isDigitCodePoint(s.codePointAt(0))

    private fun isDigitCodePoint(cp: Int): Boolean =
      when (Character.getType(cp).toByte()) {
        Character.DECIMAL_DIGIT_NUMBER -> true
        // Superscripts, circled digits and the like.
        Character.OTHER_NUMBER -> Character.getNumericValue(cp) in 0..9
        else -> false
      }

    private fun isAlphaOrSingle(s: String): Boolean = RE_ALPHA_OR_SINGLE.matches(s)

    private fun isGaiji(s: String): Boolean = RE_GAIJI.matches(s)

    private fun toNoDakuonKana(s: String): String = NO_DAKUON_DIC[s] ?: s

    private fun toDakuonKana(s: String): String = DAKUON_DIC[s] ?: s

    private fun updatePhoneticSymbols(mo: MecabMorph) {
      for (p in mo.yomi.indices) {
        // 点訳のてびき第3版 第2章 その1 1 5
        // ５、長音の書き表し方 (1), (2)
        // before: ああ,ああ,感動詞,*,*,*,アア,アー,1/2,アー,0
        // after:  ああ,ああ,感動詞,*,*,*,アア,アー,1/2,アア,0
        if (mo.yomi[p] == 'ー' && p < mo.kana.length && mo.kana[p] in "アイエオ") {
          mo.output = replaceAt(mo.output, p, mo.kana[p])
        }

        // 点訳のてびき第3版 第2章 その1 1 6
        // ６、「ジ　ズ　ジャ　ジュ　ジョ」と「ヂ　ヅ　ヂャ　ヂョ」の使い分け
        // before: 綴る,綴る,動詞,自立,*,*,ツヅル,ツズル,0/3,ツズル,0
        // after:  綴る,綴る,動詞,自立,*,*,ツヅル,ツズル,0/3,ツヅル,0
        val kanaChar = mo.kana.getOrNull(p)
        if ((mo.yomi[p] == 'ジ' && kanaChar == 'ヂ') || (mo.yomi[p] == 'ズ' && kanaChar == 'ヅ')) {
          mo.output = replaceAt(mo.output, p, kanaChar)
        }
      }
    }

    /** Python's s[:p] + c + s[p + 1:]. */
    private fun replaceAt(s: String, p: Int, c: Char): String =
      s.substring(0, minOf(p, s.length)) + c + if (p + 1 < s.length) s.substring(p + 1) else ""

    fun mecabToMorphs(featureLines: List<String>?): List<MecabMorph> {
      val li = mutableListOf<MecabMorph>()
      if (featureLines.isNullOrEmpty()) return li
      for (s in featureLines) {
        if (s.isEmpty()) continue
        val ar = s.split(",")
        val mo = MecabMorph()
        mo.hyouki = ar[0]
        mo.nhyouki = UnicodeUtil.unicodeNormalize(ar[0])
        mo.hinshi1 = ar.getOrElse(1) { "" }
        mo.hinshi2 = ar.getOrElse(2) { "" }
        if (ar.size > 3) {
          mo.hinshi3 = ar[3]
          mo.hinshi4 = ar.getOrElse(4) { "" }
        }
        if (ar.size > 5) mo.type1 = ar[5]
        if (ar.size > 6) mo.type2 = ar[6]
        if (ar.size > 7) mo.kihon = ar[7]
        if (ar.size > 9) {
          mo.kana = UnicodeUtil.unicodeNormalize(ar[8]) // "（ニチ）" -> "(ニチ)"
          // ありがとうございますー,感動詞,*,*,*,*,*,ありがとうございますー,アリガトウゴザイマスー,アリガトーゴザイマス’ー,0/1,C0
          val yomiRaw = UnicodeUtil.unicodeNormalize(ar[9])
          // Strip accent markers (’) from Japanese phonetic readings (which contain Katakana),
          // but preserve literal quote characters in punctuation/symbols.
          mo.yomi = if (yomiRaw.any { it.code in 0x30a0..0x30ff }) yomiRaw.replace("’", "") else yomiRaw
          mo.accent = ar.getOrElse(10) { "" }
          if (ar.size > 12) {
            // Mecab辞書の拡張フィールドの点訳表記があれば使用する
            mo.output = UnicodeUtil.unicodeNormalize(ar[12])
          } else {
            mo.output = mo.yomi
            updatePhoneticSymbols(mo)
          }
        }
        mo.sepflag = false
        li.add(mo)
      }
      return li
    }

    private fun replaceMorphs(
      li: List<MecabMorph>,
      dic: Map<String, List<Connected>>,
    ): List<MecabMorph> {
      val newLi = mutableListOf<MecabMorph>()
      for (mo in li) {
        val newMorphs = dic[mo.hyouki]
        if (newMorphs == null) {
          newLi.add(mo)
          continue
        }
        for (i in newMorphs) {
          val m = mo.clone()
          m.hyouki = i.hyouki // に
          m.nhyouki = UnicodeUtil.unicodeNormalize(i.hyouki) // に
          if (!i.hinshi1.isNullOrEmpty()) m.hinshi1 = i.hinshi1
          if (!i.hinshi2.isNullOrEmpty()) m.hinshi2 = i.hinshi2
          if (!i.hinshi3.isNullOrEmpty()) m.hinshi3 = i.hinshi3
          m.kana = i.hyouki
          m.yomi = UnicodeUtil.unicodeNormalize(i.yomi) // ニ
          m.output = m.yomi
          m.accent = i.accent // 0/1
          newLi.add(m)
        }
      }
      return newLi
    }

    // http://programminblog.blogspot.jp/2010/11/python.html
    /**
     * Returns 0 and null when [text] isn't a kanji number, 2 and null for an approximate number
     * with overlapping digits, and 1 with the number in digits otherwise.
     */
    fun kansuji2arabic(text: String): Pair<Int, String?> {
      if (!RE_KANSUJI.matches(text)) return 0 to null // 漢数字ではない場合
      var result = BigInteger.ZERO
      var prevDigit = BigInteger.ZERO
      var digit = BigInteger.ONE
      val numgroup = BigInteger.ONE
      var kanindex = text.length
      val ten = BigInteger.TEN
      val hundred = BigInteger.valueOf(100)
      val thousand = BigInteger.valueOf(1000)
      while (kanindex > 0) {
        val c = text[kanindex - 1]
        val c1 = text.getOrNull(kanindex)
        kanindex -= 1
        if (c in "〇零") {
          digit *= ten
        } else if (c in "十拾") {
          digit = ten
        } else if (c == '百') {
          if (digit == ten && c1 != null && c1 in "十拾") result += digit * numgroup
          digit = hundred
        } else if (c == '千') {
          if ((digit == ten && c1 != null && c1 in "十拾") || (digit == hundred && c1 == '百')) {
            result += digit * numgroup
          }
          digit = thousand
        } else {
          val value =
            when (c) {
              '壱', '一' -> 1
              '二', '弐' -> 2
              '三', '参' -> 3
              '四' -> 4
              '五' -> 5
              '六' -> 6
              '七' -> 7
              '八' -> 8
              '九' -> 9
              else -> 0
            }
          if (value != 0) result += BigInteger.valueOf(value.toLong()) * digit * numgroup
          digit *= ten
        }
        if (prevDigit > digit) return 2 to null // およその数で数が重なる場合
        prevDigit = digit
      }
      if (
        (digit == ten && text.first() in "十拾") ||
          (digit == hundred && text.first() == '百') ||
          (digit == thousand && text.first() == '千')
      ) {
        result += digit * numgroup
      }
      return 1 to result.toString() // 漢数字の場合
    }

    private fun rewriteNumber(li: List<MecabMorph>): List<MecabMorph> {
      val newLi = mutableListOf<MecabMorph>()
      for (mo in li) {
        val m = mo.clone()
        if (m.hinshi2 != "固有名詞") {
          val (flag, num) = kansuji2arabic(m.hyouki)
          if (flag == 1) {
            m.output = num!!
          } else if (flag == 2 && m.hyouki.length >= 2) {
            // 「二十二三」のような場合「二十二」「三」に分割
            val (flag1, num1) = kansuji2arabic(m.hyouki.dropLast(1))
            val (flag2, num2) = kansuji2arabic(m.hyouki.takeLast(1))
            if (flag1 == 1 && flag2 == 1) m.output = num1 + "⠼" + num2
          }
        }
        newLi.add(m)
      }
      return newLi
    }

    private fun concatinateMorphs(li: List<MecabMorph>): MecabMorph {
      val mo = li[0].clone()
      val s = StringBuilder()
      val y = StringBuilder()
      for (i in li) {
        s.append(i.hyouki)
        y.append(i.yomi)
      }
      mo.hyouki = s.toString()
      mo.nhyouki = mo.hyouki
      // mo.nhyouki = unicode_normalize(mo.nhyouki)
      mo.yomi = y.toString()
      mo.kana = mo.yomi
      mo.output = mo.yomi
      return mo
    }

    private fun replaceDigitMorphs(li: List<MecabMorph>): List<MecabMorph> {
      // handle digit number kanji characters
      // input:
      //  十,名詞,数
      //  七,名詞,数
      // output:
      //  十七,名詞,数
      // input:
      //  二,名詞,数
      //  十,名詞,数
      //  五,名詞,数
      // output:
      //  二十五,名詞,数
      // input:
      //  三,名詞,数,*,*,*,*,三,サン,サン,0/2,C3
      //  兆,名詞,数,*,*,*,*,兆,チョウ,チョー,1/2,C3
      //  二,名詞,数,*,*,*,*,二,ニ,ニ,1/1,C3
      //  千,名詞,数,*,*,*,*,千,セン,セン,1/2,C3
      //  四,名詞,数,*,*,*,*,四,ヨン,ヨン,1/2,C1
      //  百,名詞,数,*,*,*,*,百,ヒャク,ヒャク,2/2,C3
      //  万,名詞,数,*,*,*,*,万,マン,マン,1/2,C3
      // output:
      //  三,三,名詞,数,*,*,サン,サン,,サン,0
      //  兆,兆,名詞,数,*,*,チョー,チョー,,チョー,0
      //  二千四百,二千四百,名詞,数,*,*,ニセンヨンヒャク,ニセンヨンヒャク,,ニセンヨンヒャク,0
      //  万,万,名詞,数,*,*,マン,マン,,マン,0
      // (correct: 3チョー 2400マン)
      val newLi = mutableListOf<MecabMorph>()
      var numMorphs = mutableListOf<MecabMorph>()
      for (mo in li) {
        if (mo.hinshi2 == "数" && mo.hyouki == "，" && numMorphs.isNotEmpty()) {
          // カンマ
          newLi.add(concatinateMorphs(numMorphs))
          val m = mo.clone()
          m.yomi = ","
          m.output = ","
          newLi.add(concatinateMorphs(listOf(m)))
          numMorphs = mutableListOf()
        } else if (
          mo.hinshi2 == "数" &&
            !isDigit(mo.output) &&
            mo.hyouki !in setOf("・", "万", "億", "兆", "京", "．")
        ) {
          // 漢数字の結合
          numMorphs.add(mo)
        } else if (mo.hinshi2 == "数" && "０１２３４５６７８９".contains(mo.hyouki)) {
          // 算用数字の結合
          val m = mo.clone()
          val y = UnicodeUtil.unicodeNormalize(m.hyouki)
          m.output = y
          m.hyouki = y
          m.nhyouki = y
          m.yomi = y
          numMorphs.add(m)
        } else {
          if (numMorphs.isNotEmpty()) {
            newLi.add(concatinateMorphs(numMorphs))
            numMorphs = mutableListOf()
          }
          newLi.add(mo)
        }
      }
      if (numMorphs.isNotEmpty()) newLi.add(concatinateMorphs(numMorphs))
      return newLi
    }

    private fun replaceAlphabetMorphs(li: List<MecabMorph>, nabcc: Boolean): List<MecabMorph> {
      // アルファベットまたは記号だけで表記されている語を結合する
      // 情報処理点字の部分文字列になる記号を前後にまとめる
      // input:
      //  Ｂ,B,記号,アルファベット,*,*,ビー,ビー,1/2,B
      //  ａｓｉ,asi,名詞,一般,*,*,アシー,アシー,0/3,asi
      //  ｃ,c,記号,アルファベット,*,*,シー,シー,1/2,c
      // output:
      //  Ｂａｓｉｃ,Basic,名詞,アルファベット,*,*,ビーアシーシー,ビーアシーシー,1/2,Basic
      val newLi = mutableListOf<MecabMorph>()
      var alpMorphs = mutableListOf<MecabMorph>()
      for (pos in li.indices) {
        val mo = li[pos]
        val nextMo = if (pos < li.size - 1) li[pos + 1] else null
        val h = mo.nhyouki
        // Python's `h in "..."` on strings is a substring test, true for "" too.
        if (isAlphaOrSingle(h)) {
          alpMorphs.add(mo)
        } else if (h.isNotEmpty() && """,+@/#$%&*;<""".contains(h)) {
          alpMorphs.add(mo)
        } else if (h == "\\") {
          alpMorphs.add(mo)
        } else if (h.isNotEmpty() && h[0] in """,+@/#$%&*;""" && RE_ASCII_SYMBOLS.matches(h)) {
          alpMorphs.add(mo)
        } else if (
          alpMorphs.isNotEmpty() &&
            ",.".contains(h) &&
            ((nextMo != null && nextMo.nhyouki == " ") ||
              (nextMo != null && (nextMo.hinshi1 == "助詞" || nextMo.hinshi1 == "助動詞")) ||
              nextMo == null)
        ) {
          alpMorphs.add(mo)
        } else if (
          alpMorphs.isNotEmpty() && h == " " && nextMo != null && isAlphaOrSingle(nextMo.nhyouki)
        ) {
          alpMorphs.add(mo)
        } else if (alpMorphs.isNotEmpty() && isDigit(h)) {
          alpMorphs.add(mo)
        } else if (alpMorphs.isNotEmpty() && ",.:;!?@#\\$%&*|+-/=<>\"'^`_~{}[]，".contains(h)) {
          alpMorphs.add(mo)
        } else if (nabcc && "”’‘＿".contains(h)) {
          alpMorphs.add(mo)
        } else if (alpMorphs.isEmpty() && "[]".contains(h)) {
          alpMorphs.add(mo)
        } else {
          if (alpMorphs.isNotEmpty()) {
            newLi.add(concatenatedAlphabets(alpMorphs))
            alpMorphs = mutableListOf()
          }
          newLi.add(mo)
        }
      }
      if (alpMorphs.isNotEmpty()) newLi.add(concatenatedAlphabets(alpMorphs))
      return newLi
    }

    private fun concatenatedAlphabets(li: List<MecabMorph>): MecabMorph {
      val m = concatinateMorphs(li)
      m.nhyouki = UnicodeUtil.unicodeNormalize(m.nhyouki)
      m.output = m.nhyouki
      setPosOfAlphabets(m)
      return m
    }

    private fun setPosOfAlphabets(m: MecabMorph) {
      if (m.nhyouki == "," || m.nhyouki == "][") {
        m.hinshi1 = "記号"
        m.hinshi2 = "*"
      } else if (!"[]".contains(m.nhyouki)) {
        m.hinshi1 = "名詞"
        m.hinshi2 = "アルファベット"
      }
    }

    /** 英語句 + 括弧内英字（例: "NonVisual Desktop Access (NVDA)"）を 1 形態素にまとめる。 */
    private fun mergeParenthesizedAlphabetMorphs(li: List<MecabMorph>): List<MecabMorph> {
      val newLi = mutableListOf<MecabMorph>()
      var pos = 0
      while (pos < li.size) {
        // "foo (BAR)" pattern
        if (pos + 4 < li.size) {
          val mo0 = li[pos]
          if (
            RE_GAIJI.matches(mo0.nhyouki) &&
              ' ' in mo0.nhyouki &&
              li[pos + 1].nhyouki == " " &&
              li[pos + 2].nhyouki == "(" &&
              RE_PAREN_ASCII_BODY.matches(li[pos + 3].nhyouki) &&
              li[pos + 4].nhyouki == ")"
          ) {
            newLi.add(concatenatedAlphabets(li.subList(pos, pos + 5)))
            pos += 5
            continue
          }
        }
        // "foo(BAR)" pattern
        if (pos + 3 < li.size) {
          if (
            RE_GAIJI.matches(li[pos].nhyouki) &&
              li[pos + 1].nhyouki == "(" &&
              RE_PAREN_ASCII_BODY.matches(li[pos + 2].nhyouki) &&
              li[pos + 3].nhyouki == ")"
          ) {
            newLi.add(concatenatedAlphabets(li.subList(pos, pos + 4)))
            pos += 4
            continue
          }
        }
        newLi.add(li[pos])
        pos += 1
      }
      return newLi
    }

    private fun fixJapaneseDateMorphs(li: List<MecabMorph>): List<MecabMorph> {
      val newLi = mutableListOf<MecabMorph>()
      for (i in li.indices) {
        val prev2Mo = if (i - 2 >= 0) li[i - 2] else null
        val prevMo = if (i - 1 >= 0) li[i - 1] else null
        val mo = li[i]
        if (mo.hyouki == "日" && mo.hinshi3 == "助数詞" && prevMo != null) {
          if (prevMo.hyouki in setOf("14", "24", "十四", "一四", "二四", "二十四")) {
            li[i].output = "カ"
            newLi.add(li[i])
          } else if ((prev2Mo == null || prev2Mo.hyouki != "、") && prevMo.output in WAGO_DIC) {
            val m = mo.clone()
            m.hyouki = prevMo.hyouki + mo.hyouki
            m.nhyouki = prevMo.nhyouki + mo.nhyouki
            m.output = WAGO_DIC.getValue(prevMo.output)
            m.kana = m.output
            m.yomi = m.output
            m.hinshi2 = "日付"
            m.hinshi3 = "*"
            // FIXME: m.accent
            newLi.removeAt(newLi.size - 1)
            newLi.add(m)
          } else {
            newLi.add(li[i])
          }
        } else {
          newLi.add(li[i])
        }
      }
      return newLi
    }

    fun shouldSeparate(
      prev2Mo: MecabMorph?,
      prevMo: MecabMorph,
      mo: MecabMorph,
      nextMo: MecabMorph?,
      nabcc: Boolean = false,
    ): Boolean {
      // ##############################
      // True
      // ##############################

      // 括弧開の前
      // (あける)
      // 映画,映画,名詞,一般,*,*,エイガ,エイガ,0/3,エイガ,1
      // 「,「,記号,括弧開,*,*,「,「,*/*,「,0
      // (あけない)
      // 機関,名詞,一般,*,*,*,*,機関,キカン,キカン,1/3,C1
      // （,記号,括弧開,*,*,*,*,（,（,（,*/*,*
      if (prevMo.hinshi1 == "名詞" && mo.hinshi2 == "括弧開" && mo.nhyouki != "(") return true

      // )( -> あける
      // )陽が -> あける
      // '02 -> あけない
      if (prevMo.hinshi2 == "括弧閉" && prevMo.nhyouki != "’") {
        if (mo.hinshi2 == "括弧開") return true
        if (mo.hinshi1 == "名詞") return true
      }

      // 数字の前のマスアケ
      val moOutputIsdigit = isDigit(mo.output)
      if (
        moOutputIsdigit &&
          !nabcc &&
          prevMo.output != "⠼" &&
          prevMo.nhyouki !in
            setOf("-", "，", ".", "’", "、", ":", "：", ",", "ー", "(", "第", "築", "二男", "中")
      ) {
        return true
      }

      // 1月/1日
      if (moOutputIsdigit && startsWithDigit(prevMo.nhyouki) && prevMo.nhyouki.endsWith("月")) {
        return true
      }

      // 三,三,名詞,数,*,*,サン,サン,0/2,3,0
      // 兆,兆,名詞,数,*,*,チョウ,チョー,1/2,チョー,1
      // 二千四百,二千四百,名詞,数,*,*,ニセンヨンヒャク,ニセンヨンヒャク,1/1,2400,0
      // 万,万,名詞,数,*,*,マン,マン,1/2,マン,0
      if (moOutputIsdigit && prevMo.hyouki in setOf("億", "兆", "京")) return true

      // 外国語引用符、マスアケ、助詞、助動詞
      val isMoHinshi1JoshiOrJodoshi = mo.hinshi1 == "助詞" || mo.hinshi1 == "助動詞"
      if (isMoHinshi1JoshiOrJodoshi && prevMo.output.endsWith("⠴")) return true

      // アルファベットの後の助詞、助動詞
      // ＣＤ,CD,名詞,一般,*,*,シーディー,シーディー,3/4,シーディー,0
      // を,を,助詞,格助詞,一般,*,ヲ,ヲ,0/1,ヲ,0
      val isPrevMoNhyoukiAlphaOrSingle = isAlphaOrSingle(prevMo.nhyouki)
      if (isPrevMoNhyoukiAlphaOrSingle && isMoHinshi1JoshiOrJodoshi) return true
      if (nabcc && prevMo.hinshi2 == "アルファベット" && isMoHinshi1JoshiOrJodoshi) return true

      // ピリオドの後の助詞
      if (prevMo.nhyouki.endsWith(".") && mo.hinshi1 == "助詞") return true

      // ナンバーマークの後の助詞
      if (prevMo.nhyouki == "#" && mo.hinshi1 == "助詞") return true

      // 助数詞のあとにアラビア数字が来たらマスアケ
      // case 1:
      //  零,零,名詞,数,*,*,0,0,1/2,0,0
      //  時,時,名詞,接尾,助数詞,*,ジ,ジ,1/1,ジ,1
      //  十五,十五,名詞,数,*,*,15,15,1/3,15,0
      //  分,分,名詞,接尾,助数詞,*,フン,フン,1/2,フン,0
      // case 2:
      //  一,一,名詞,数,*,*,イチ,イチ,2/2,1,0
      //  人,人,名詞,接尾,助数詞,*,ニン,ニン,1/2,ニン,0
      //  当り,当り,名詞,接尾,一般,*,アタリ,アタリ,1/3,アタリ,1
      //  １,1,名詞,数,*,*,イチ,イチ,2/2,1,0
      //  ０,0,名詞,数,*,*,ゼロ,ゼロ,1/2,0,0
      //  個,個,名詞,接尾,助数詞,*,コ,コ,1/1,コ,0
      if (moOutputIsdigit && prevMo.hinshi1 == "名詞" && prevMo.hinshi2 == "接尾") return true

      if (prevMo.hinshi1 == "助詞" && mo.hinshi1 == "接頭詞") return true

      // 新/東京/名所
      if (
        prevMo.hinshi1 == "接頭詞" &&
          prevMo.hinshi2 == "名詞接続" &&
          mo.hinshi1 == "名詞" &&
          mo.hinshi2 == "固有名詞"
      ) {
        // 後白河
        if (!(prevMo.hyouki == "後" && prevMo.yomi == "ゴ")) return true
      }

      // 多かれ/少なかれ
      if (prevMo.hinshi1 == "形容詞" && mo.hinshi1 == "形容詞") {
        if (prevMo.type2 == "命令ｅ" && mo.type2 == "命令ｅ") return true
      }

      // 複合語（接頭語・接尾語・造語要素）【備考１】
      // 接頭語・接尾語・造語要素であっても、意味の理解を助ける場合には、
      // 発音上の切れ目を考慮して区切って書いてよい。
      if (
        prevMo.hinshi1 == "接頭詞" &&
          prevMo.hyouki in setOf("貴", "前", "当", "反", "非", "新", "要", "肝", "逆", "超") &&
          mo.hinshi1 == "名詞" &&
          mo.hinshi2 != "数"
      ) {
        return true
      }

      // 人名に続く「さん」「様」「君」「殿」「氏（し）」「氏（うじ）」は区切って書く
      // (名詞,固有名詞,人名 -> 名詞,接尾,人名)
      if (prevMo.hinshi2 == "固有名詞" && prevMo.hinshi3 == "人名") {
        if (mo.hinshi2 == "接尾" && mo.hinshi3 == "人名") return true
        if (mo.hyouki == "さん" || mo.hyouki == "知事") return true
      }

      // 地域
      // 永田町 １
      // 「岬」「峠」「半島」はマス空けが原則
      if (
        prevMo.hinshi2 == "固有名詞" &&
          prevMo.hinshi3 == "地域" &&
          (mo.hinshi2 == "数" || mo.hyouki in setOf("岬", "峠", "半島"))
      ) {
        return true
      }

      // 伊豆/大島
      if (prevMo.hinshi2 == "固有名詞" && mo.hinshi2 == "固有名詞") return true

      // 日/独/伊/3国同盟
      if (
        prevMo.hinshi2 == "固有名詞" &&
          prevMo.hinshi3 == "地域" &&
          mo.hinshi1 == "名詞" &&
          mo.hinshi2 == "一般"
      ) {
        if (!(mo.hyouki == "卿" && mo.yomi == "キョー") && !(mo.hyouki == "市" && mo.yomi == "シ")) {
          return true
        }
      }

      // 東京/都 千代田/区
      if (
        prevMo.hinshi2 == "接尾" &&
          prevMo.hinshi3 == "地域" &&
          mo.hinshi2 == "固有名詞" &&
          mo.hinshi3 == "地域"
      ) {
        return true
      }

      // 東京/都 交通/局
      if (
        prevMo.hinshi2 == "接尾" &&
          prevMo.hinshi3 == "地域" &&
          mo.hinshi1 == "名詞" &&
          mo.hinshi2 == "一般"
      ) {
        return true
      }

      // 聞き捨てならない キキズテ ナラナイ
      if (prevMo.hinshi1 == "動詞" && mo.hinshi1 == "動詞" && mo.hyouki == "なら") return true

      // お,黙り,なさい
      // 「お」がついて名詞化した語に「なさい・なさる」が続く場合は区切ってよい
      if (
        prev2Mo != null &&
          prev2Mo.hinshi1 == "接頭詞" &&
          prev2Mo.hyouki == "お" &&
          prevMo.hinshi1 == "動詞" &&
          prevMo.type2 == "連用形" &&
          mo.kihon == "なさる"
      ) {
        return true
      }

      if (prevMo.hinshi1 == "動詞" && prevMo.hinshi2 == "自立" && mo.hyouki == "および") return true

      // 障害,者/協会
      if (
        prev2Mo != null &&
          prev2Mo.hinshi1 == "名詞" &&
          prevMo.hyouki == "者" &&
          mo.hinshi1 == "名詞"
      ) {
        return true
      }

      // 世界/初
      if (prevMo.hinshi1 == "名詞" && prevMo.hinshi2 == "一般" && mo.hyouki == "初") return true

      // 40キロ レース
      if (prevMo.hinshi3 == "助数詞" && mo.hyouki == "レース") return true

      // 1回こっきり 1カイ コッキリ
      if (prevMo.hyouki == "回" && mo.hyouki == "こっきり") return true

      // Ｈ形コンベアー Hガタ コンベアー
      if (prevMo.hyouki.endsWith("形") && mo.hyouki == "コンベアー") return true

      // 漢字仮名交じり文 カンジ カナマジリブン
      if (mo.hyouki == "仮名" && mo.output == "カナ") {
        if (prevMo.hyouki == "漢字") return true
      }

      // 晴れ/所に より
      if (prevMo.hinshi1 == "名詞" && mo.hyouki == "所により") return true

      // 右/斜め/上
      if (mo.hyouki == "斜め") return true
      if (prevMo.hyouki == "斜め") return true

      // 一番/上
      if (prevMo.hyouki == "一番" && mo.hinshi1 == "名詞") return true

      // 一時/雨
      if (prevMo.hyouki == "一時" && mo.hinshi1 == "名詞") return true

      // とは/言う/ものの
      if (prevMo.hyouki == "言う" && mo.hyouki == "ものの") return true

      // 一脈/相通じる/ものが/ある
      if (prevMo.hyouki == "相通じる" && mo.hyouki == "もの") return true

      // 一審/判決
      if (prevMo.hyouki == "審" && mo.hyouki == "判決") return true

      // 意図/不明
      // 意味/不明
      if (mo.hyouki == "不明") return true

      // 今/現在
      if (prevMo.hyouki == "今" && mo.hyouki == "現在") return true

      // 癌予防 ガン ヨボー
      if (prevMo.hyouki == "癌" && mo.hyouki == "予防") return true

      // 旧華族 キュー カゾク
      // 旧街道 キューカイドー 点訳のてびき第3版 第3章 その2 2
      if (prevMo.hyouki == "旧" && mo.hyouki != "街道") return true

      // 禁転載 キン テンサイ
      if (prevMo.hyouki == "禁" && prevMo.output == "キン") return true

      // 休暇届 キューカ トドケ
      if (mo.hyouki == "届" && mo.output == "トドケ") return true

      // 500円強 500エン キョー
      if (mo.hyouki == "強" && mo.output == "キョー") return true

      // 父太郎 チチ タロー
      if (prevMo.hyouki == "父" && mo.hinshi2 == "固有名詞") return true

      if (prevMo.hinshi1 == "助動詞" && prevMo.hyouki == "で" && mo.hinshi1 == "助動詞") return true

      // 「の」（名詞,非自立）の後に名詞が続く場合にスペースを挿入
      // 例1: 映画「ラヂオの時間」 → エイガ 「ラジオノ ジカン」
      //  ラヂオ,名詞,一般 → ラジオ
      //  の,名詞,非自立 → ノ
      //  時間,名詞,副詞可能 → ジカン
      // 例2: 気を付けの姿勢 → キヲツケノ シセイ
      //  気を付け,名詞,一般 → キヲツケ
      //  の,名詞,非自立 → ノ
      //  姿勢,名詞,一般 → シセイ
      if (
        prevMo.hinshi1 == "名詞" &&
          prevMo.hinshi2 == "非自立" &&
          prevMo.hyouki == "の" &&
          mo.hinshi1 == "名詞" &&
          (mo.hinshi2 == "一般" || mo.hinshi2 == "副詞可能")
      ) {
        return true
      }

      // 感動詞の後に助動詞「ござい」が続く場合にスペースを挿入
      // 例: 有り難うございました → アリガトー ゴザイマシタ
      //  有り難う,感動詞 → アリガトー
      //  ござい,助動詞 → ゴザイ
      if (
        prevMo.hinshi1 == "感動詞" &&
          prevMo.hyouki == "有り難う" &&
          mo.hinshi1 == "助動詞" &&
          mo.hyouki == "ござい"
      ) {
        return true
      }

      // 仮名文字 カナモジ
      // 仮名タイプ カナタイプ
      // 仮名変換 カナ ヘンカン
      if (prevMo.hyouki == "仮名" && prevMo.output == "カナ") {
        if (len(mo.output.split(" ").last()) >= 4) return true
      }

      if (prevMo.hyouki == "擬似" && mo.hyouki == "コレラ") return true
      if (prevMo.hyouki == "火事" && mo.hyouki == "見舞い") return true
      if (prevMo.hyouki in KIKI) return true
      if (mo.hyouki in KIKI) return true

      // 人名に造語要素が続く場合で、2拍以下の場合は続ける
      // 自立性が強く、意味の理解を助ける場合は、前を区切って書く
      if (prevMo.hinshi1 == "名詞" && mo.hinshi1 == "名詞") {
        if (prevMo.hinshi4 == "姓" || prevMo.hinshi4 == "名" || prevMo.hinshi3 == "人名") {
          if (mo.hinshi2 == "接尾" && mo.hinshi3 == "人名") return true
          if (mo.hyouki in setOf("訳", "作", "談", "曲", "記", "絵", "アナ", "プロ")) return true
        }
      }

      //
      // 味気,味気,名詞,ナイ形容詞語幹,*,*,アジケ,アジケ,0/3,アジケ,0
      // ない,ない,助動詞,*,*,*,ナイ,ナイ,1/2,ナイ,0
      //
      // 良く,形容詞,自立,*,*,形容詞・アウオ段,連用テ接続,良い,ヨク,ヨク,1/2,C3
      // ない,助動詞,*,*,*,特殊・ナイ,基本形,ない,ナイ,ナイ,1/2,動詞%F3@0/形容詞%F2@1
      //
      // で,で,助動詞,*,*,*,デ,デ,1/1,デ,0
      // は,は,助詞,係助詞,*,*,ハ,ワ,0/1,ワ,1
      // なく,なく,助動詞,*,*,*,ナク,ナク,0/2,ナク,1
      //
      // 「問題ない」の「ない」は「点訳のてびき」では形容詞だがMecabでは助動詞
      // 形容詞「ない」は区切る
      // ただし前の語と複合している場合は前に続ける
      if (mo.hinshi1 == "助動詞" && (mo.kihon == "ない" || mo.kihon == "無い")) {
        if (prevMo.hinshi1 == "助詞" && prevMo.kihon == "は") return true
        if (prevMo.hinshi1 == "形容詞" && prevMo.kihon == "良い") return true
        if (
          prevMo.hinshi1 == "名詞" &&
            prevMo.hinshi2 == "ナイ形容詞語幹" &&
            (prevMo.kihon == "問題" || prevMo.kihon == "間違い")
        ) {
          return true
        }
        if (prevMo.hinshi2 == "副助詞") return true // じゃない
        if (prevMo.hinshi1 == "動詞" && prevMo.hinshi2 == "非自立" && prevMo.kihon == "てる") {
          return true // てない
        }
        if (prevMo.hinshi1 == "助動詞" && prevMo.kihon == "だ") return true // でない
      }
      if (mo.hinshi1 == "形容詞" && mo.kihon in setOf("ない", "無い", "悪い")) {
        if (prevMo.kihon !in setOf("隈", "心置き", "満遍", "決まり", "限")) return true
      }

      // ##############################
      // False
      // ##############################

      if (prevMo.hyouki.endsWith("　")) return false

      if (prevMo.hyouki == "ー") return false

      // ち,ち,名詞,一般,*,*,チ,チ,0/1,チ,0
      // ゅうりっぷ,ゅうりっぷ,名詞,一般,*,*,,,,ュウリップ,0
      if (mo.hyouki.isNotEmpty() && mo.hyouki[0] in "ーぁぃぅぇぉっゃゅょゎァィゥェォッャュョヮヵヶ") return false

      // 0/4月 -> 04月
      val prevMoOutputIsdigit = isDigit(prevMo.output)
      if (prevMoOutputIsdigit && startsWithDigit(mo.nhyouki)) return false
      // 3/03
      if (prevMoOutputIsdigit && mo.nhyouki.startsWith("/")) return false
      if (prevMoOutputIsdigit && mo.hinshi3 == "助数詞") return false
      if (prevMoOutputIsdigit && mo.nhyouki == "#") return false

      // アラビア数字のあとに単位がきたら続ける
      // 三十,三十,名詞,数,*,*,30,30,1/4,30,1
      // センチメートル,センチメートル,名詞,一般,*,*,センチメートル,センチメートル,4/7,センチメートル,0
      if (prevMoOutputIsdigit && (mo.hyouki == "センチメートル" || mo.hyouki == "楽章")) return false

      // カナ名詞の後のアルファベット名詞
      if (prevMo.hinshi1 == "名詞" && isAlphaOrSingle(mo.nhyouki)) {
        // キラーＴ細胞 キラー Tサイボー
        if (prevMo.hyouki != "キラー") return false
      }

      // アルファベットの後の名詞
      // V字
      if (isPrevMoNhyoukiAlphaOrSingle && (mo.hyouki == "細胞" || mo.hyouki == "字")) return false

      // https://github.com/nvdajp/nvdajpmiscdep/issues/67
      if (
        nabcc &&
          prevMo.hinshi2 == "アルファベット" &&
          (prevMo.nhyouki.endsWith("(") ||
            prevMo.nhyouki.endsWith("[") ||
            prevMo.nhyouki.endsWith("{")) &&
          mo.hinshi1 == "名詞"
      ) {
        return false
      }

      // 数字の後のアルファベット
      if (prevMo.hinshi2 == "数" && mo.hinshi2 == "アルファベット") {
        if (nabcc) {
          return false
        } else if (RE_ASCII_CHARS.matches(mo.nhyouki)) {
          return false
        }
      }

      if (prevMo.hinshi1 == "名詞" && mo.hinshi1 == "名詞" && mo.hinshi2 == "数") return false
      if (
        mo.hyouki == "嬢" &&
          (prevMo.hinshi4 == "姓" || prevMo.hinshi4 == "名" || prevMo.hinshi3 == "人名")
      ) {
        return false
      }
      // 京急,線 東急,線
      if (mo.hyouki == "線" && prevMo.hinshi3 == "組織") return false

      if (prevMo.hinshi1 == "動詞" && prevMo.hinshi2 == "自立") {
        if (mo.hinshi1 == "動詞" && mo.hinshi2 == "非自立") return false
      }

      if (prevMo.hinshi1 == "接頭詞" && mo.hinshi1 == "名詞") return false

      // その,その,連体詞,*,*,*,ソノ,ソノ,0/2,ソノ,1
      // よう,よう,名詞,非自立,助動詞語幹,*,ヨウ,ヨー,1/2,ヨー,0
      if (prevMo.hinshi1 == "連体詞" && mo.hinshi3 == "助動詞語幹") return false

      // 数%
      if (prevMo.hyouki == "数" && prevMo.yomi == "スー" && mo.hyouki == "％") return false

      if (prevMo.hyouki == "フェア" && mo.hyouki == "キャッチ") return false
      if (prevMo.hyouki == "擬古" && mo.hyouki == "文") return false
      if (prevMo.hyouki == "白" && mo.hyouki == "生地") return false
      if (prevMo.hyouki == "今日" && mo.hyouki == "限り") return false

      // 334万画素 334マンガソ
      if (prevMo.hyouki == "万" && mo.hyouki == "画素") return false

      // 薄ら笑い ウスラワライ
      if (prevMo.hyouki == "薄ら" && mo.hinshi1 == "名詞") return false

      // 扱い始め
      if (prevMo.hinshi1 == "名詞" && mo.hyouki == "始め") return false

      // 岩倉卿
      if (mo.hyouki == "卿" && mo.yomi == "キョー") return false

      // 京言葉
      if (prevMo.hyouki == "京" && prevMo.yomi == "キョー") return false

      // そう.な.ん.です.もの
      // そう.なん.だって
      if (prevMo.hyouki == "な" && mo.hyouki == "ん") return false
      if (prevMo.hyouki == "です" && mo.hyouki == "もの") return false
      if (prevMo.hyouki == "そう" && mo.hyouki == "なん") return false

      // 「・・ですこと」の「こと」は接尾語なので前に続ける
      if (prevMo.hyouki == "です" && mo.hyouki == "こと") return false

      // 否が/応でも
      if (prevMo.hyouki == "応" && mo.hyouki == "でも") return false

      // 「この程」「この度」
      // 「そのくせ」
      // 後ろの語と結びついて1語になっている場合は続ける
      if (prevMo.hyouki == "この" && (mo.hyouki == "程" || mo.hyouki == "度")) return false
      if (prevMo.hyouki == "その" && mo.hyouki in setOf("くせ", "うち", "まま")) return false
      if (prevMo.hyouki == "わが" && mo.hyouki == "まま") return false

      // 労,せ,ず
      if (prevMo.hinshi1 == "名詞" && mo.hyouki == "せ" && mo.kihon == "する") return false

      // いいんですけど
      if (prevMo.hinshi1 == "形容詞" && mo.hyouki == "ん") return false

      // 見/まごう
      if (prevMo.hinshi1 == "動詞" && prevMo.hyouki == "見") return false

      // お兄さん, お姉さん
      if (prevMo.hinshi1 == "接頭詞" && prevMo.hyouki == "お") return false

      // のように
      if (prevMo.hinshi1 == "助詞" && mo.hyouki == "よう") return false

      // 利かぬ気, 利かん気
      if (prevMo.hinshi1 == "助動詞" && mo.hyouki == "気") return false

      // 器量よし
      if (prevMo.hinshi1 == "名詞" && mo.hyouki == "よし") return false

      // 行ったきり
      if (prevMo.hinshi1 == "助動詞" && mo.hyouki == "きり") return false

      // 鍛冶,職人 カジショクニン
      if (len(prevMo.output) <= 2 && mo.hyouki == "職人") return false

      // 金の減り.加減 カネノ ヘリカゲン
      // 馬鹿さ.加減 バカサ カゲン
      if (mo.hyouki == "加減" && mo.yomi == "カゲン") {
        if (len(prevMo.output.split(" ").last()) < 3) return false
      }

      if (mo.hinshi1 == "助動詞" && (mo.kihon == "ない" || mo.kihon == "無い")) return false

      // 仮名文字 カナモジ
      // 仮名タイプ カナタイプ
      // 仮名変換 カナ ヘンカン
      if (prevMo.hyouki == "仮名" && prevMo.output == "カナ") return false

      // 面白おかしい (点訳のてびき第3版 第3章 その2 10. 複合形容詞は続ける)
      if (prevMo.hinshi1 == "形容詞" && mo.hinshi1 == "形容詞") return false

      if (mo.hinshi1 == "形容詞" && mo.kihon in setOf("ない", "無い", "悪い")) return false

      // 不幸,に,し,て
      // 今,に,し,て
      // 居,ながら,に,し,て
      // 労,せ,ず,し,て
      // 若く,し,て
      // 私,を,し,て
      // 「して」が文語的表現の助詞である場合は前に続けて書く
      if (mo.hyouki == "し" && mo.kihon == "する") {
        if (prevMo.hyouki == "ず" && prevMo.hinshi1 == "助動詞") return false
        if (prevMo.hinshi1 == "形容詞" && prevMo.type2 == "連用テ接続") return false
        if (prevMo.hinshi2 == "接続助詞") return false
        if (prevMo.type1 == "文語・ベシ") return false
        if (nextMo != null && nextMo.hyouki == "て") {
          if (prevMo.hyouki == "に" && prevMo.hinshi1 == "助詞") return false
          if (prev2Mo != null && prev2Mo.hyouki == "私" && prevMo.hyouki == "を") return false
        }
      }

      // ##############################
      // False/True
      // ##############################

      // ２字漢語 母子/年金 幾何/模様 基礎/医学 騎馬/武者
      if (
        mo.hinshi1 == "名詞" &&
          mo.hinshi2 == "一般" &&
          len(prevMo.hyouki) == 2 &&
          len(prevMo.yomi) == 2 &&
          len(mo.yomi) >= 3 &&
          !RE_KATAKANA.matches(mo.nhyouki)
      ) {
        return true
      }

      // 複合名詞内部の2拍以下は切らない
      if (
        prevMo.hinshi1 == "名詞" &&
          prevMo.hinshi2 != "数" &&
          prevMo.hinshi2 != "アルファベット" &&
          mo.hinshi1 == "名詞" &&
          mo.hinshi2 !in setOf("数", "アルファベット", "接尾")
      ) {
        if (len(prevMo.yomi) >= 4 && len(mo.yomi) >= 2) {
          if (mo.hyouki != "鍛冶") return true
        }
        if (len(mo.yomi) >= 4) {
          if (prevMo.hyouki != "右" && prevMo.hyouki != "花") return true
        }
        if (len(prevMo.yomi) <= 2 && len(mo.yomi) >= 3) return false
        if (len(prevMo.yomi) >= 3 && len(mo.yomi) <= 2) return false
      }

      if (prevMo.isSubstantiveWord() && mo.isIndependentWord()) return true
      if (prevMo.isIndependentWord() && mo.isIndependentWord()) return true
      return false
    }

    private val KIKI = setOf("危機", "機器", "記紀", "記事", "義務")

    private fun morphsToString(li: List<MecabMorph>, inbuf: String): Pair<String, MutableList<Int>> {
      val outbuf = StringBuilder()
      val inpos2 = mutableListOf<Int>()
      var p = 0
      for (mo in li) {
        if (mo.output.isEmpty()) continue
        val out = mo.output
        val outlen = out.length
        outbuf.append(out)
        val hyolen = mo.hyouki.length
        if (hyolen == outlen) {
          for (x in p until p + outlen) inpos2.add(x)
        } else if (out.startsWith("⠠⠦") && out.endsWith("⠠⠴")) {
          // 情報処理用点字の内側
          val c = outlen - 4
          repeat(2) { inpos2.add(p) }
          for (x in p until p + c) inpos2.add(x)
          repeat(2) { inpos2.add(p + c - 1) }
        } else if (out.startsWith("⠦") && out.endsWith("⠴")) {
          // 外国語引用符の内側
          val c = outlen - 2
          inpos2.add(p)
          for (x in p until p + c) inpos2.add(x)
          inpos2.add(p + c - 1)
        } else {
          // 表記と出力の文字数が変化する場合
          for (x in 0 until outlen) inpos2.add(p + (x.toDouble() * hyolen / outlen).toInt())
        }
        p += hyolen
        if (mo.sepflag) {
          outbuf.append(" ")
          // マスアケは直前の文字に対応
          inpos2.add(if (p > 0) p - 1 else p)
        }
      }
      // rstrip with inpos2
      if (inbuf.lastOrNull() != ' ') {
        while (outbuf.isNotEmpty() && outbuf.last() == ' ') {
          outbuf.setLength(outbuf.length - 1)
          inpos2.removeAt(inpos2.size - 1)
        }
      }
      return outbuf.toString() to inpos2
    }

    /** liblouis translate の出力文字列を outbuf 用（スペース + U+280x）に変換する。 */
    private fun louisCellsToBrailleString(cells: String): String {
      val result = StringBuilder()
      for (c in cells) {
        val code = c.code
        when {
          code == 0x20 -> result.append(' ')
          code in 0x2800..0x28FF -> result.append(if (code == 0x2800) ' ' else c)
          else -> {
            val cell = code and 0xFF
            result.append(if (cell == 0) ' ' else (0x2800 + cell).toChar())
          }
        }
      }
      return result.toString().replace('⠀', ' ')
    }

    /** 情報処理用 ⠠⠦...⠠⠴ を除外して、外国語引用符の inner 範囲を返す。 */
    private fun findForeignQuoteRanges(outbuf: String): List<Pair<Int, Int>> {
      var i = 0
      val ranges = mutableListOf<Pair<Int, Int>>()
      while (i < outbuf.length) {
        var openIdx = outbuf.indexOf(FOREIGN_OPEN, i)
        while (openIdx > 0 && outbuf[openIdx - 1] == INFO_PREFIX) {
          openIdx = outbuf.indexOf(FOREIGN_OPEN, openIdx + 1)
        }
        if (openIdx < 0) break
        var closeSearch = openIdx + 1
        var closeIdx = -1
        while (true) {
          val cand = outbuf.indexOf(FOREIGN_CLOSE, closeSearch)
          if (cand < 0) break
          if (cand == 0 || outbuf[cand - 1] != INFO_PREFIX) {
            closeIdx = cand
            break
          }
          closeSearch = cand + 1
        }
        if (closeIdx < 0) break
        ranges.add(openIdx + 1 to closeIdx)
        i = closeIdx + 1
      }
      return ranges
    }

    /** louis の inPos/outPos を使って inner 置換後の inpos2 セグメントを作る。 */
    private fun mapLouisPositions(
      oldSegmentInpos: List<Int>,
      louisInPos: List<Int>,
      louisOutPos: List<Int>,
      newLen: Int,
    ): List<Int> {
      if (newLen <= 0) return emptyList()
      if (oldSegmentInpos.isEmpty()) return List(newLen) { 0 }
      val maxInputIdx = oldSegmentInpos.size - 1

      fun fallbackFromOutPos(outIdx: Int): Int? {
        if (louisOutPos.isEmpty()) return null
        var candidate = 0
        for ((inIdx, mappedOut) in louisOutPos.withIndex()) {
          if (mappedOut <= outIdx) candidate = inIdx else break
        }
        return candidate
      }

      val result = ArrayList<Int>(newLen)
      for (outIdx in 0 until newLen) {
        var srcIdx = louisInPos.getOrNull(outIdx)
        if (srcIdx == null || srcIdx < 0) srcIdx = fallbackFromOutPos(outIdx)
        if (srcIdx == null) {
          srcIdx = if (newLen == 1) 0 else (outIdx * maxInputIdx) / (newLen - 1)
        }
        result.add(oldSegmentInpos[srcIdx.coerceIn(0, maxInputIdx)])
      }
      return result
    }

    fun makeOutPos(inPos: List<Int>, inlen: Int, outlen: Int): List<Int> {
      // make outPos
      val outPos = IntArray(inlen) { -1 }
      for (p in outlen - 1 downTo 0) {
        val i = inPos[p]
        if (i in 0 until inlen) outPos[i] = p
      }
      // fill skipped outPos
      var prev = 0
      for (p in 0 until inlen) {
        if (outPos[p] == -1) outPos[p] = prev else prev = outPos[p]
      }
      return outPos.toList()
    }

    fun mergePositionMap(
      inpos1: List<Int>,
      inpos2: List<Int>,
      outlen: Int,
      inlen: Int,
    ): Pair<List<Int>, List<Int>> {
      val inPos = List(outlen) { p -> inpos2[inpos1[p]] }
      return inPos to makeOutPos(inPos, inlen, outlen)
    }
  }
}
