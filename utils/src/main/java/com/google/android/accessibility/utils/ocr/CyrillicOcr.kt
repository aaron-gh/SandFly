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

package com.google.android.accessibility.utils.ocr

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * Where the Tesseract models for Cyrillic text recognition are kept, and which one a language
 * uses. ML Kit has no Cyrillic recognizer, so [LanguageTextRecognizer] uses Tesseract for
 * [LanguageTextRecognizer.Script.CYRILLIC] once the model of the language is downloaded.
 *
 * Models are Tesseract's `tessdata_fast` files, `<code>.traineddata`, in the app's no-backup files,
 * which the `:mlkit` process, where Tesseract runs, shares.
 */
object CyrillicOcr {
  /** The model of Latin letters, which is used with the Cyrillic one when it is downloaded. */
  const val LATIN_MODEL = "eng"

  /** The model of Cyrillic languages that have none of their own. */
  const val DEFAULT_MODEL = "rus"

  const val MODEL_SUFFIX = ".traineddata"

  /** The directory that Tesseract is given; it reads the models from [tessdataDir]. */
  @JvmStatic fun dataDir(context: Context): File = File(context.noBackupFilesDir, "tesseract")

  @JvmStatic fun tessdataDir(context: Context): File = File(dataDir(context), "tessdata")

  @JvmStatic
  fun modelFile(context: Context, model: String): File = File(tessdataDir(context), model + MODEL_SUFFIX)

  /** Whether [model] is downloaded. Partial downloads have another name, so they aren't counted. */
  @JvmStatic fun isInstalled(context: Context, model: String): Boolean = modelFile(context, model).isFile

  /**
   * The Tesseract model of [language], or [DEFAULT_MODEL] for another language written in Cyrillic,
   * or null for a language not written in Cyrillic.
   */
  @JvmStatic
  fun modelFor(language: Locale): String? {
    if (LanguageTextRecognizer.scriptOf(language) != LanguageTextRecognizer.Script.CYRILLIC) {
      return null
    }
    return MODELS[language.language] ?: DEFAULT_MODEL
  }

  /** The models Tesseract loads for [model]: with Latin letters too, when that model is there. */
  @JvmStatic
  fun languages(model: String, latinInstalled: Boolean): String =
    if (latinInstalled && model != LATIN_MODEL) "$model+$LATIN_MODEL" else model

  /** Whether [languages], as Tesseract takes them, names only models SandFly downloads. */
  @JvmStatic
  fun isValidLanguages(languages: String): Boolean {
    val models = languages.split('+')
    return models.size in 1..2 &&
      models.all { it == LATIN_MODEL || it in MODELS.values || it == DEFAULT_MODEL }
  }

  /** The BCP 47 tag of the language of [model], as ML Kit gives recognized languages. */
  @JvmStatic
  fun languageOf(model: String): String =
    MODELS.entries.firstOrNull { it.value == model }?.key ?: "und"

  /** ISO 639-1 codes of the languages written in Cyrillic that have a model, to the model. */
  private val MODELS =
    mapOf(
      "ru" to "rus",
      "uk" to "ukr",
      "be" to "bel",
      "bg" to "bul",
      "sr" to "srp",
      "mk" to "mkd",
      "kk" to "kaz",
      "ky" to "kir",
      "tg" to "tgk",
      "mn" to "mon",
      "tt" to "tat",
      "uz" to "uzb_cyrl",
      "az" to "aze_cyrl",
    )
}
