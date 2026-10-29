/*
 * Ported from libkuraji (https://github.com/nishimotz/libkuraji) unicodeutil.py at 307f3dd.
 * Copyright (C) 2019-2026 Takuya Nishimoto.
 * Licensed under the BSD 3-Clause License, which is in
 * braille/translate/src/phone/assets/japanese-braille/LICENSE-libkuraji.
 */

package com.google.android.accessibility.braille.translate.japanese

import java.text.Normalizer

/**
 * NFKC normalization with position maps. Originally _nvdajp_unicode.py in NVDA Japanese (nvdajp).
 *
 * Positions are UTF-16 indices, as in Kotlin strings; libkuraji's are code point indices. They
 * differ only for characters outside the BMP.
 */
internal object UnicodeUtil {

  /**
   * Returns the NFKC-normalized string together with a map from each normalized character back to
   * its position in the original string. NFKC changes the character count for some characters,
   * e.g. U+2026 HORIZONTAL ELLIPSIS -> "..." and U+2469 CIRCLED NUMBER TEN -> "10", which would
   * break braille position mapping (nvdajp issues #117, #328).
   */
  fun nfkcNormalizeWithMap(s: String): Pair<String, List<Int>> {
    val out = StringBuilder()
    val nmap = ArrayList<Int>(s.length)
    var i = 0
    val n = s.length
    while (i < n) {
      var j = i + Character.charCount(s.codePointAt(i))
      // Normalize combining characters, and the halfwidth voiced/semi-voiced sound marks
      // (U+FF9E/U+FF9F, which NFKC turns into combining characters), together with their base
      // character; normalizing them separately would prevent composition and change the result.
      while (j < n && (isCombining(s.codePointAt(j)) || s[j] == 'ﾞ' || s[j] == 'ﾟ')) {
        j += Character.charCount(s.codePointAt(j))
      }
      val seg = nfkc(s.substring(i, j))
      out.append(seg)
      repeat(seg.length) { nmap.add(i) }
      i = j
    }
    var result = out.toString()
    if (nfkc(result) != result) {
      // The concatenation of per-segment results is not NFKC-stable. Fall back to whole-string
      // normalization and keep the legacy position behavior (positions in the normalized string,
      // with the tail clamped to the last character of the original string).
      result = nfkc(s)
      nmap.clear()
      for (p in result.indices) nmap.add(minOf(p, n - 1))
    }
    return result to nmap
  }

  /** Characters folded before NFKC normalization. */
  private val PRE_NFKC_MAP: Map<Char, String> =
    mapOf(
      ' ' to " ", // no-break space
      ' ' to " ", // en space
      ' ' to " ", // em space
      ' ' to " ", // three-per-em space
      ' ' to " ", // four-per-em space
      ' ' to " ", // six-per-em space
      ' ' to " ", // figure space
      ' ' to " ", // punctuation space
      ' ' to " ", // thin space
      ' ' to " ", // hair space
      // U+200B (zero width space) is kept: used as the tab placeholder
      '‎' to "", // left-to-right mark
      '‏' to "", // right-to-left mark
      '�' to "", // replacement character
      // converted to fullwidth by Mecab_text2mecab(); NFKC cannot restore it
      '．' to ".", // fullwidth full stop
    )

  /** Characters folded after NFKC normalization. */
  private val POST_NFKC_MAP: Map<Char, String> =
    mapOf(
      '−' to "-", // minus sign
      '¥' to "\\", // yen sign
      '〜' to "~", // wave dash
      // accented Latin letters folded to ASCII
      'À' to "A", 'Á' to "A", 'Â' to "A", 'Ä' to "A", 'Å' to "A", 'Æ' to "AE",
      'Ç' to "C", 'È' to "E", 'É' to "E", 'Ê' to "E", 'Ë' to "E",
      'Ì' to "I", 'Í' to "I", 'Î' to "I", 'Ï' to "I", 'Ñ' to "N",
      'Ò' to "O", 'Ó' to "O", 'Ô' to "O", 'Ö' to "O", 'Ø' to "O",
      'Ù' to "U", 'Ú' to "U", 'Û' to "U", 'Ü' to "U", 'Ý' to "Y",
      'à' to "a", 'á' to "a", 'â' to "a", 'ä' to "a", 'å' to "a", 'æ' to "ae",
      'ç' to "c", 'è' to "e", 'é' to "e", 'ê' to "e", 'ë' to "e",
      'ì' to "i", 'í' to "i", 'î' to "i", 'ï' to "i", 'ñ' to "n",
      'ò' to "o", 'ó' to "o", 'ô' to "o", 'ö' to "o", 'ø' to "o",
      'ù' to "u", 'ú' to "u", 'û' to "u", 'ü' to "u", 'ý' to "y", 'ÿ' to "y",
      'Œ' to "OE", 'œ' to "oe", 'Ÿ' to "Y",
    )

  fun unicodeNormalize(s: String): String {
    if (s.all { it.code < 0x80 }) {
      // every folded character is non-ASCII and NFKC is the identity for ASCII, so nothing to do
      return s
    }
    return translate(nfkc(translate(s, PRE_NFKC_MAP)), POST_NFKC_MAP)
  }

  fun nfkc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)

  /**
   * Python's unicodedata.combining(c) != 0, approximated by the nonspacing mark category, since
   * the JVM has no canonical combining class lookup.
   */
  private fun isCombining(codePoint: Int): Boolean =
    Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt()

  private fun translate(s: String, map: Map<Char, String>): String {
    if (s.none { it in map }) return s
    val out = StringBuilder(s.length)
    for (c in s) out.append(map[c] ?: c)
    return out.toString()
  }
}
