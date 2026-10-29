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

import java.io.File
import java.text.Normalizer

/**
 * The analysis libkuraji's JTalkDicAnalyzer gives: [MecabTagger] over the JTalk extended
 * dictionary in [directory], with the input widened as the dictionary expects and the features
 * corrected by [MecabCorrect]. Gives feature lines for [Translator2].
 */
internal class JapaneseAnalyzer(directory: File) {
  private val tagger = MecabTagger(directory)

  fun analyze(text: String): List<String> =
    MecabCorrect.correctFeatures(tagger.parse(toMecabText(text))) { tagger.parse(toMecabText(it)) }

  companion object {
    /**
     * NFKC-normalized, then ASCII widened to the fullwidth forms the dictionary's unknown-word
     * classes were built for, as libkuraji's _to_mecab_text.
     */
    fun toMecabText(text: String): String {
      val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
      val out = StringBuilder(normalized.length)
      for (c in normalized) {
        out.append(
          when (c) {
            ' ' -> '　'
            '-' -> '−'
            '~' -> '〜'
            '\\' -> '￥'
            '�' -> '？'
            in '!'..'~' -> c + 0xFEE0
            else -> c
          }
        )
      }
      return out.toString()
    }
  }
}
