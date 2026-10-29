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

package com.google.android.accessibility.braille.translate.japanese

import android.content.Context
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.braille.translate.TranslationResult
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Standard Japanese braille, by libkuraji's rules: kanji are read and words split with the JTalk
 * dictionary, then the kana are written in 6-dot braille. Until the dictionary is downloaded, and
 * for braille typed in, [fallback] translates.
 */
class JapaneseBrailleTranslator(context: Context, private val fallback: BrailleTranslator) :
  BrailleTranslator {
  private val context = context.applicationContext

  override fun translateToPrint(brailleWord: BrailleWord): String =
    fallback.translateToPrint(brailleWord)

  override fun translateToPrintPartial(brailleWord: BrailleWord): String =
    fallback.translateToPrintPartial(brailleWord)

  override fun translate(text: CharSequence, cursorPosition: Int): TranslationResult {
    val translator = translator() ?: return fallback.translate(text, cursorPosition)
    val translation =
      try {
        translator.translate(text.toString(), cursorPosition.coerceAtLeast(0))
      } catch (e: RuntimeException) {
        // Too long, or a rule failed on unusual text.
        LogUtils.e(TAG, "Translation failed: %s", e)
        return fallback.translate(text, cursorPosition)
      }
    return result(text, translation, cursorPosition)
  }

  private fun translator(): Translator2? {
    val analyzer = JapaneseDictionary.analyzer(context)
    if (analyzer == null) {
      JapaneseDictionary.download(context, /* retryNow= */ false)
      return null
    }
    return synchronized(Companion) {
      shared?.takeIf { it.first === analyzer }?.second
        ?: Translator2(analyzer::analyze).also { shared = analyzer to it }
    }
  }

  companion object {
    private const val TAG = "JapaneseBraille"
    /** One translator for the dictionary, since every Japanese translator uses the same one. */
    private var shared: Pair<JapaneseAnalyzer, Translator2>? = null

    internal fun result(
      text: CharSequence,
      translation: Translator2.Translation,
      cursorPosition: Int,
    ): TranslationResult {
      val cells = BrailleWord()
      for (c in translation.braille) {
        // A blank is the empty cell.
        cells.append(BrailleCharacter(if (c in '⠀'..'⣿') (c - '⠀').toByte() else 0))
      }
      val lastCell = (cells.size() - 1).coerceAtLeast(0)
      val lastChar = (text.length - 1).coerceAtLeast(0)
      return TranslationResult.builder()
        .setText(text)
        .setCells(cells)
        .setTextToBraillePositions(
          List(text.length) { i -> translation.outpos.getOrNull(i)?.coerceIn(0, lastCell) ?: lastCell }
        )
        .setBrailleToTextPositions(
          List(cells.size()) { j -> translation.inpos.getOrNull(j)?.coerceIn(0, lastChar) ?: lastChar }
        )
        .setCursorBytePosition(if (cursorPosition < 0) -1 else translation.cursor)
        .build()
    }
  }
}
