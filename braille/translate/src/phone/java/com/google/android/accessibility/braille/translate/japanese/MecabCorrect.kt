/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) mecab_correct.py at 307f3dd.
 * Copyright (C) 2010-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import java.text.Normalizer

/**
 * Post-processes MeCab feature lines (one CSV line per morpheme: the surface, then the features of
 * the JTalk extended dictionary) the way nvdajp's Mecab_correctFeatures does, so that the analysis
 * matches the one libkuraji's rules were written against.
 */
internal object MecabCorrect {
  // romanji -> katakana table (from roma2kana.py, re-licensed BSD)
  private val ROMA_DIC =
    listOf(
      "ccha" to "ッチャ", "cchi" to "ッチ", "cchu" to "ッチュ", "cche" to "ッチェ", "ccho" to "ッチョ",
      "bba" to "ッバ", "bbi" to "ッビ", "bbu" to "ッブ", "bbe" to "ッベ", "bbo" to "ッボ",
      "dda" to "ッダ", "ddi" to "ッジ", "ddu" to "ッヅ", "dde" to "ッデ", "ddo" to "ッド",
      "ffa" to "ッファ", "ffi" to "ッフィ", "ffu" to "ッフ", "ffe" to "ッフェ", "ffo" to "ッフォ",
      "gga" to "ッガ", "ggi" to "ッギ", "ggu" to "ッグ", "gge" to "ッゲ", "ggo" to "ッゴ",
      "hha" to "ッハ", "hhi" to "ッヒ", "hhu" to "ッフ", "hhe" to "ッヘ", "hho" to "ッホ",
      "jja" to "ッジャ", "jji" to "ッジ", "jju" to "ッジュ", "jje" to "ッジェ", "jjo" to "ッジョ",
      "kka" to "ッカ", "kki" to "ッキ", "kku" to "ック", "kke" to "ッケ", "kko" to "ッコ",
      "ppa" to "ッパ", "ppi" to "ッピ", "ppu" to "ップ", "ppe" to "ッペ", "ppo" to "ッポ",
      "ssa" to "ッサ", "ssi" to "ッシ", "ssu" to "ッス", "sse" to "ッセ", "sso" to "ッソ",
      "tta" to "ッタ", "tti" to "ッチ", "ttu" to "ッツ", "tte" to "ッテ", "tto" to "ット",
      "zza" to "ッザ", "zzi" to "ッジ", "zzu" to "ッズ", "zze" to "ッゼ", "zzo" to "ッゾ",
      "cha" to "チャ", "chu" to "チュ", "cho" to "チョ",
      "tsu" to "ツ",
      "tya" to "チャ", "tyu" to "チュ", "tyo" to "チョ",
      "jya" to "ジャ", "jyu" to "ジュ", "jyo" to "ジョ",
      "kya" to "キャ", "kyu" to "キュ", "kyo" to "キョ",
      "gya" to "ギャ", "gyu" to "ギュ", "gyo" to "ギョ",
      "shi" to "シ",
      "sya" to "シャ", "syu" to "シュ", "syo" to "ショ",
      "sha" to "シャ", "shu" to "シュ", "sho" to "ショ",
      "chi" to "チ",
      "nya" to "ニャ", "nyu" to "ニュ", "nyo" to "ニョ",
      "hya" to "ヒャ", "hyu" to "ヒュ", "hyo" to "ヒョ",
      "pya" to "ピャ", "pyu" to "ピュ", "pyo" to "ピョ",
      "mya" to "ミャ", "myu" to "ミュ", "myo" to "ミョ",
      "rya" to "リャ", "ryu" to "リュ", "ryo" to "リョ",
      "sa" to "サ", "si" to "シ", "ka" to "カ", "ki" to "キ",
      "ku" to "ク", "ke" to "ケ", "ko" to "コ",
      "ga" to "ガ", "gi" to "ギ", "gu" to "グ", "ge" to "ゲ", "go" to "ゴ",
      "su" to "ス", "se" to "セ", "so" to "ソ",
      "za" to "ザ", "zi" to "ジ", "ji" to "ジ", "zu" to "ズ",
      "ze" to "ゼ", "zo" to "ゾ",
      "ja" to "ジャ", "ju" to "ジュ", "jo" to "ジョ",
      "ta" to "タ", "ti" to "チ", "tu" to "ツ", "te" to "テ", "to" to "ト",
      "da" to "ダ", "di" to "ヂ", "du" to "ヅ", "de" to "デ", "do" to "ド",
      "na" to "ナ", "ni" to "ニ", "nu" to "ヌ", "ne" to "ネ", "no" to "ノ",
      "nn" to "ン",
      "ha" to "ハ", "hi" to "ヒ", "hu" to "フ", "he" to "ヘ", "ho" to "ホ",
      "fa" to "ファ", "fi" to "フィ", "fu" to "フ", "fe" to "フェ", "fo" to "フォ",
      "ba" to "バ", "bi" to "ビ", "bu" to "ブ", "be" to "ベ", "bo" to "ボ",
      "pa" to "パ", "pi" to "ピ", "pu" to "プ", "pe" to "ペ", "po" to "ポ",
      "ma" to "マ", "mi" to "ミ", "mu" to "ム", "me" to "メ", "mo" to "モ",
      "ya" to "ヤ", "yu" to "ユ", "yo" to "ヨ",
      "ra" to "ラ", "ri" to "リ", "ru" to "ル", "re" to "レ", "ro" to "ロ",
      "wa" to "ワ", "wi" to "ウィ", "wo" to "オ",
      "a" to "ア", "i" to "イ", "u" to "ウ", "e" to "エ", "o" to "オ",
      "n" to "ン",
    )

  fun getKanaFromRoma(roma: String): String? {
    var kana = Normalizer.normalize(roma, Normalizer.Form.NFKC)
    if (kana == "youtube") return null
    for ((from, to) in ROMA_DIC) kana = kana.replace(from, to)
    return if (kana.all { it in 'ァ'..'ヾ' }) kana else null
  }

  // spellchar table (from _nvdajp_spellchar.py, re-licensed BSD)
  private val SPELLCHAR_PATTERNS: List<Pair<Regex, String>> =
    buildList {
      for (c in 'Ａ'..'Ｚ') add(Regex(c.toString()) to ('A' + (c - 'Ａ')).toString())
      for (c in 'ａ'..'ｚ') add(Regex(c.toString()) to ('a' + (c - 'ａ')).toString())
      for (c in '０'..'９') add(Regex(c.toString()) to ('0' + (c - '０')).toString())
      for ((p, r) in
        listOf(
          "0" to "ゼロ ", "1" to "イチ ", "2" to "ニイ ", "3" to "サン ", "4" to "ヨン ",
          "5" to "ゴオ ", "6" to "ロク ", "7" to "ナナ ", "8" to "ハチ ", "9" to "キュウ ",
          "(a|A)" to "エイ ", "(b|B)" to "ビイー ", "(c|C)" to "シイ ", "(d|D)" to "ディイ ",
          "(e|E)" to "イイー ", "(f|F)" to "エフ ", "(g|G)" to "ジイ ", "(h|H)" to "エイチ ",
          "(i|I)" to "アイ ", "(j|J)" to "ジェイ ", "(k|K)" to "ケイ ", "(l|L)" to "エル ",
          "(m|M)" to "エム ", "(n|N)" to "エヌ ", "(o|O)" to "オオ ", "(p|P)" to "ピイイ ",
          "(q|Q)" to "キュウ ", "(r|R)" to "アール ", "(s|S)" to "エス ", "(t|T)" to "ティイ ",
          "(u|U)" to "ユウ ", "(v|V)" to "ブイ ", "(w|W)" to "ダブリュウ ",
          "(x|X)" to "エックス ", "(y|Y)" to "ワイ ", "(z|Z)" to "ゼッド ",
        )) {
        add(Regex(p) to r)
      }
    }

  fun convertSpellChar(msg: String): String {
    var text = msg
    for ((pattern, replacement) in SPELLCHAR_PATTERNS) {
      text = pattern.replace(text, Regex.escapeReplacement(replacement))
    }
    return text
  }

  private val RE_FULLSHAPE_ALPHA = Regex("^[Ａ-Ｚａ-ｚ]+$")

  // Single fullwidth characters that fugashi's libmecab emits as 名詞,サ変接続
  // (unknown word) but the nvdajp MeCab fork emits as 記号,括弧閉 via unk.dic.
  // Used by correct_features to restore the 記号 class for these characters.
  private val BRACKET_CLOSE_CHARS = setOf("＂", "＇")

  private val FALLBACK_TABLE =
    mapOf(
      "＂" to "”,記号,括弧閉,*,*,*,*,”,”,”,*/*,*",
      "＇" to "’,記号,括弧閉,*,*,*,*,’,’,’,*/*,*",
      "｀" to "‘,記号,括弧開,*,*,*,*,‘,‘,‘,*/*,*",
    )

  /** The characters (code points) of [s], as Python iterates a string. */
  private fun chars(s: String): List<String> {
    val result = ArrayList<String>(s.length)
    var i = 0
    while (i < s.length) {
      val n = Character.charCount(s.codePointAt(i))
      result.add(s.substring(i, i + n))
      i += n
    }
    return result
  }

  /** Python's len(): the number of code points. */
  private fun len(s: String): Int = s.codePointCount(0, s.length)

  /** Python's str.isalnum() for one character. */
  private fun isAlnum(c: String): Boolean {
    val cp = c.codePointAt(0)
    if (Character.isLetter(cp)) return true
    return when (Character.getType(cp).toByte()) {
      Character.DECIMAL_DIGIT_NUMBER,
      Character.LETTER_NUMBER,
      Character.OTHER_NUMBER -> true
      else -> false
    }
  }

  private fun splitSurface(surface: String): List<String> {
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    for (c in chars(surface)) {
      if (isAlnum(c)) {
        current.append(c)
      } else {
        if (current.isNotEmpty()) {
          chunks.add(current.toString())
          current.setLength(0)
        }
        chunks.add(c)
      }
    }
    if (current.isNotEmpty()) chunks.add(current.toString())
    return chunks
  }

  fun getMoraCount(s: String): Int {
    val m = s.split("/")
    if (m.size == 2) {
      val m2 = m[1]
      if (m2 != "*") return m2.toIntOrNull() ?: 0
    }
    return 0
  }

  private fun shouldWorkAroundLatinWordPostfix(
    ar3: List<String>?,
    ar2: List<String>?,
    ar: List<String>,
  ): Boolean =
    !(ar3 != null && ar3[0] == "　" && ar2 != null && ar2[0] == "’") &&
      ar2 != null &&
      ar[0] in setOf("ｓ", "ｄ", "ｅｄ", "ｒ", "ｔｉｎｇ", "ｔ")

  private fun makeFeatureFromLatinWordAndPostfix(
    org: String,
    ar: List<String>,
    symbol: String = "",
  ): String {
    val hyokiBase = ar[0]
    var yomiBase = if (ar.size > 8) ar[8] else convertSpellChar(hyokiBase).replace(" ", "")
    var pronBase = if (ar.size > 9) ar[9] else convertSpellChar(hyokiBase).replace(" ", "")
    val hin1 = ar[1]
    val hin2 = ar[2]
    val hin3 = ar[3]
    var postfix = ""
    if (org == "ｓ") {
      postfix = "ズ"
      if (hyokiBase.endsWith("ｐ") || hyokiBase.endsWith("ｋｅ") || hyokiBase.endsWith("ｒｋ")) {
        postfix = "ス"
      } else if (hyokiBase.endsWith("ｔｈａｔ")) {
        postfix = "ツ"
        yomiBase = yomiBase.dropLast(2)
        pronBase = pronBase.dropLast(2)
      } else if (hyokiBase.endsWith("ｗｏｒｄ")) {
        postfix = "ズ"
        yomiBase = yomiBase.dropLast(1)
        pronBase = pronBase.dropLast(1)
      }
    } else if (org == "ｔ") {
      postfix = "ト"
    } else if (org == "ｄ" || org == "ｅｄ") {
      if (hyokiBase.endsWith("ｔｅ") && yomiBase.endsWith("ト")) {
        postfix = "ティド"
        yomiBase = yomiBase.dropLast(1)
        pronBase = pronBase.dropLast(1)
      } else {
        postfix = "ド"
      }
    } else if (org == "ｒ") {
      postfix = "ア"
      if (hyokiBase.endsWith("ｓｅ")) {
        postfix = "ザー"
        yomiBase = yomiBase.dropLast(1)
        pronBase = pronBase.dropLast(1)
      }
    } else if (hyokiBase.endsWith("ｔ") && yomiBase.endsWith("ト") && org == "ｔｉｎｇ") {
      postfix = "ティング"
      yomiBase = yomiBase.dropLast(1)
      pronBase = pronBase.dropLast(1)
    }
    val hyoki = hyokiBase + symbol + org
    val yomi = yomiBase + postfix
    val pron = pronBase + postfix
    val mora = if (ar.size > 10) getMoraCount(ar[10]) + 1 else len(pron)
    return "$hyoki,$hin1,$hin2,$hin3,*,*,*,$hyoki,$yomi,$pron,0/$mora,C0"
  }

  private fun makeBraillePatternReading(s: String): String {
    val n = s.codePointAt(0) - 0x2800
    if (n == 0) return "マスアケ"
    val ar = StringBuilder()
    if (n and 0x01 != 0) ar.append("イチ")
    if (n and 0x02 != 0) ar.append("ニー")
    if (n and 0x04 != 0) ar.append("サン")
    if (n and 0x08 != 0) ar.append("ヨン")
    if (n and 0x10 != 0) ar.append("ゴー")
    if (n and 0x20 != 0) ar.append("ロク")
    if (n and 0x40 != 0) ar.append("ナナ")
    if (n and 0x80 != 0) ar.append("ハチ")
    return ar.toString() + "ノテン"
  }

  private fun fields(line: String): MutableList<String> = line.split(",").toMutableList()

  /**
   * Post-processes MeCab feature lines (one CSV string per morpheme). [reparse] re-analyzes
   * single characters to fill in missing readings of unknown words (PATTERN 1/2).
   */
  fun correctFeatures(lines: List<String>, reparse: ((String) -> List<String>)?): List<String> {
    // operate on a mutable copy of split fields
    var parsed: MutableList<MutableList<String>> =
      lines.map { if (it.isNotEmpty()) fields(it) else mutableListOf() }.toMutableList()

    val newParsed = mutableListOf<MutableList<String>>()
    for (ar in parsed) {
      if (ar.isEmpty()) continue
      if (
        ar.size > 7 &&
          len(ar[0]) > 1 &&
          ar[1] == "名詞" &&
          ar[2] == "サ変接続" &&
          ar[7] == "*" &&
          chars(ar[0]).any { !isAlnum(it) } &&
          chars(ar[0]).toSet().size > 1
      ) {
        for (chunk in splitSurface(ar[0])) {
          val fallback = FALLBACK_TABLE[chunk]
          if (len(chunk) == 1 && fallback != null) {
            newParsed.add(fields(fallback))
          } else if (reparse != null) {
            for (sl in reparse(chunk)) newParsed.add(fields(sl))
          } else {
            newParsed.add(mutableListOf(chunk, "名詞", "サ変接続", "*", "*", "*", "*", "*"))
          }
        }
      } else {
        newParsed.add(ar)
      }
    }
    parsed = newParsed
    val n = parsed.size

    for (pos in 0 until n) {
      val ar = parsed[pos]
      if (ar.isEmpty()) continue
      val ar2 = if (pos >= 1 && parsed[pos - 1].isNotEmpty()) parsed[pos - 1] else null
      val ar3 = if (pos >= 2 && parsed[pos - 2].isNotEmpty()) parsed[pos - 2] else null
      if (
        ar3 != null &&
          ar2 != null &&
          RE_FULLSHAPE_ALPHA.matches(ar3[0]) &&
          RE_FULLSHAPE_ALPHA.matches(ar2[0]) &&
          RE_FULLSHAPE_ALPHA.matches(ar[0])
      ) {
        val hyoki = ar3[0] + ar2[0] + ar[0]
        val yomi = getKanaFromRoma(hyoki)
        if (!yomi.isNullOrEmpty()) {
          val feature = "$hyoki,名詞,固有名詞,*,*,*,*,$hyoki,$yomi,$yomi,0/${len(yomi)},C0"
          parsed[pos - 2] = fields(feature)
          parsed[pos - 1] = fields(",,,*,*,*,*")
          parsed[pos] = fields(feature)
        }
      } else if (
        ar.size > 7 &&
          len(ar[0]) == 1 &&
          ar[1] == "名詞" &&
          ar[2] == "サ変接続" &&
          ar[7] == "*" &&
          ar[0] in FALLBACK_TABLE
      ) {
        parsed[pos] = fields(FALLBACK_TABLE.getValue(ar[0]))
      } else if (
        (ar.size > 2 && ar[2] == "数" && ar.size > 7 && ar[7] == "*") ||
          (ar.size > 2 && ar[1] == "名詞" && ar[2] == "サ変接続" && ar.size > 7 && ar[7] == "*")
      ) {
        if (reparse != null) {
          val hyoki = ar[0]
          val yomi = StringBuilder()
          val pron = StringBuilder()
          var mora = 0
          for (c in chars(hyoki)) {
            for (sl in reparse(c)) {
              var sub: List<String> = sl.split(",")
              if (sub.size > 7 && sub[7] == "*" && c in FALLBACK_TABLE) {
                sub = FALLBACK_TABLE.getValue(c).split(",")
              }
              if (sub.size > 10) {
                yomi.append(sub[8])
                pron.append(sub[9])
                mora += getMoraCount(sub[10])
              }
            }
          }
          parsed[pos] = fields("$hyoki,名詞,普通名詞,*,*,*,*,$hyoki,$yomi,$pron,0/$mora,C0")
        }
      } else if (
        ar.size > 7 && len(ar[0]) == 1 && ar[1] == "名詞" && ar[2] == "サ変接続" && ar[7] == "*"
      ) {
        // Single-character unknown words that should be 記号 (symbols).
        //
        // The fugashi-bundled libmecab emits some fullwidth punctuation
        // (notably ＂ U+FF02 and ＇ U+FF07) as 名詞,サ変接続 unknown words
        // instead of 記号, because its unk.dic lacks the mapping that the
        // nvdajp MeCab fork has. The fixture was recorded against the
        // nvdajp fork, which outputs these as 記号,括弧閉. Map them here
        // so the analysis matches.
        val hyoki = ar[0]
        val hin2 = if (hyoki in BRACKET_CLOSE_CHARS) "括弧閉" else "一般"
        parsed[pos] = fields("$hyoki,記号,$hin2,*,*,*,*,$hyoki,$hyoki,$hyoki,*/*,*")
      } else if (ar2 != null && ar[0] == "ー" && ar.size > 2 && ar[1] == "名詞" && ar[2] == "一般") {
        if (ar2.size > 10) {
          val hyoki = ar2[0] + "ー"
          val yomi = ar2[8] + "ー"
          val pron = ar2[9] + "ー"
          val mora = getMoraCount(ar2[10]) + 1
          parsed[pos - 1] =
            fields("$hyoki,${ar2[1]},${ar2[2]},*,*,*,*,$hyoki,$yomi,$pron,0/$mora,C0")
        } else if (ar3 != null && ar3.size > 10 && ar3[1] != "記号") {
          val hyoki = ar3[0] + ar2[0] + "ー"
          val yomi = ar3[8] + ar2[0] + "ー"
          val pron = ar3[9] + ar2[0] + "ー"
          val mora = getMoraCount(ar3[10]) + len(ar2[0]) + 1
          parsed[pos - 2] =
            fields("$hyoki,${ar3[1]},${ar3[2]},*,*,*,*,$hyoki,$yomi,$pron,0/$mora,C0")
        }
      } else if (shouldWorkAroundLatinWordPostfix(ar3, ar2, ar)) {
        if (ar3 != null && ar2 != null && (ar2[0] == "'" || ar2[0] == "’")) {
          parsed[pos - 2] = fields(",,,*,*,*,*")
          parsed[pos - 1] = fields(",,,*,*,*,*")
          parsed[pos] = fields(makeFeatureFromLatinWordAndPostfix(ar[0], ar3, symbol = "'"))
        } else if (
          ar2 != null && ar2.size > 10 && RE_FULLSHAPE_ALPHA.matches(ar2[0]) && len(ar2[0]) > 1
        ) {
          parsed[pos - 1] = fields(",,,*,*,*,*")
          parsed[pos] = fields(makeFeatureFromLatinWordAndPostfix(ar[0], ar2))
        }
      } else if (
        ar2 != null && RE_FULLSHAPE_ALPHA.matches(ar[0]) && RE_FULLSHAPE_ALPHA.matches(ar2[0])
      ) {
        val hyoki = ar2[0] + ar[0]
        val yomi = getKanaFromRoma(hyoki)
        if (!yomi.isNullOrEmpty()) {
          parsed[pos - 1] = fields(",,,*,*,*,*")
          parsed[pos] = fields("$hyoki,名詞,固有名詞,*,*,*,*,$hyoki,$yomi,$yomi,0/${len(yomi)},C0")
        }
      } else if (RE_FULLSHAPE_ALPHA.matches(ar[0]) && ar.size > 7 && ar[7] == "*") {
        val roma = ar[0]
        val kana = getKanaFromRoma(roma)
        if (!kana.isNullOrEmpty()) {
          parsed[pos] = fields("$roma,名詞,固有名詞,*,*,*,*,$roma,$kana,$kana,0/${len(kana)},C0")
        }
      } else if (len(ar[0]) == 1 && ar[0].codePointAt(0) in 0x2800..0x28FF && ar.size > 9) {
        val reading = makeBraillePatternReading(ar[0])
        ar[8] = reading
        ar[9] = reading
      }
    }
    // rejoin to CSV strings
    return parsed.map { it.joinToString(",") }
  }
}
