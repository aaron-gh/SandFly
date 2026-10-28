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

package com.google.android.accessibility.talkback.translation

import java.util.Locale
import org.json.JSONException
import org.json.JSONObject

/** The prompt that asks an AI model for a translation, and the reading of its answer. */
object AiTranslationPrompt {
  /**
   * Asks for [text] in [to], from [from] or a language the model detects. With [second], text that
   * is already in [to] is translated into [second] instead, in the same request.
   */
  fun prompt(text: String, from: String?, to: String, second: String?): String = buildString {
    append("Translate the text below into ").append(english(to)).append(". ")
    if (from != null) append("The text is in ").append(english(from)).append(". ")
    if (second != null) {
      append("If the text is already in ").append(english(to)).append(", translate it into ")
      append(english(second)).append(" instead. ")
    }
    append("Keep names, numbers and the meaning; add no notes or explanations. ")
    append("Answer with JSON only: {\"from\": \"<ISO 639-1 code of the language of the text>\", ")
    append("\"to\": \"<ISO 639-1 code of the language you translated into>\", ")
    append("\"translation\": \"<the translation>\"}\n\nText:\n")
    append(text)
  }

  /**
   * The translation in [answer]. A model that answers without JSON, as small on-device models can,
   * gives its whole answer as the translation, into [to].
   */
  fun parse(answer: String, to: String): Translation {
    val start = answer.indexOf('{')
    val end = answer.lastIndexOf('}')
    if (start in 0 until end) {
      try {
        val json = JSONObject(answer.substring(start, end + 1))
        val translation = json.optString("translation").trim()
        if (translation.isNotEmpty()) {
          return Translation(
            translation,
            TranslationLanguages.normalize(json.optString("from")),
            TranslationLanguages.normalize(json.optString("to")) ?: to,
          )
        }
      } catch (e: JSONException) {
        // Read as plain text below.
      }
    }
    return Translation(answer.trim(), null, to)
  }

  private fun english(code: String): String = TranslationLanguages.name(code, Locale.ENGLISH)
}
