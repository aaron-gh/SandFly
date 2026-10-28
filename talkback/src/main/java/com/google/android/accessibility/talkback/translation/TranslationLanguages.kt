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

import android.icu.util.ULocale
import java.util.Locale

/**
 * The languages that text can be translated from and into, by BCP 47 tag. Chinese is zh-Hans or
 * zh-Hant, since every service tells the two scripts apart. Each service gets its own codes from
 * [TranslationApis].
 */
object TranslationLanguages {
  const val CHINESE_SIMPLIFIED = "zh-Hans"
  const val CHINESE_TRADITIONAL = "zh-Hant"

  /** The languages of Google Cloud Translation, which the other services' lists fall within. */
  val ALL: List<String> =
    listOf(
      "af", "ak", "am", "ar", "as", "ay", "az", "be", "bg", "bho", "bm", "bn", "bs", "ca", "ceb",
      "ckb", "co", "cs", "cy", "da", "de", "doi", "dv", "ee", "el", "en", "eo", "es", "et", "eu",
      "fa", "fi", "fil", "fr", "fy", "ga", "gd", "gl", "gn", "gom", "gu", "ha", "haw", "he", "hi",
      "hmn", "hr", "ht", "hu", "hy", "id", "ig", "ilo", "is", "it", "ja", "jv", "ka", "kk", "km",
      "kn", "ko", "kri", "ku", "ky", "la", "lb", "lg", "ln", "lo", "lt", "lus", "lv", "mai", "mg",
      "mi", "mk", "ml", "mn", "mni", "mr", "ms", "mt", "my", "ne", "nl", "no", "nso", "ny", "om",
      "or", "pa", "pl", "ps", "pt", "qu", "ro", "ru", "rw", "sa", "sd", "si", "sk", "sl", "sm",
      "sn", "so", "sq", "sr", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tr",
      "ts", "tt", "ug", "uk", "ur", "uz", "vi", "xh", "yi", "yo", CHINESE_SIMPLIFIED,
      CHINESE_TRADITIONAL, "zu",
    )

  /** Old and alternative codes that services and Android use, and the code in [ALL] for each. */
  private val ALIASES =
    mapOf(
      "iw" to "he",
      "in" to "id",
      "ji" to "yi",
      "jw" to "jv",
      "tl" to "fil",
      "nb" to "no",
      "nn" to "no",
      "zh" to CHINESE_SIMPLIFIED,
      "zh-cn" to CHINESE_SIMPLIFIED,
      "zh-sg" to CHINESE_SIMPLIFIED,
      "zh-hans" to CHINESE_SIMPLIFIED,
      "zh-tw" to CHINESE_TRADITIONAL,
      "zh-hk" to CHINESE_TRADITIONAL,
      "zh-mo" to CHINESE_TRADITIONAL,
      "zh-hant" to CHINESE_TRADITIONAL,
      "zt" to CHINESE_TRADITIONAL,
      "mni-mtei" to "mni",
    )

  /**
   * The code in [ALL] for a [code] that a service or Android gives, such as "EN-GB" or "zh-TW", or
   * null when it isn't one of them.
   */
  @JvmStatic
  fun normalize(code: String?): String? {
    val lower = code?.trim()?.replace('_', '-')?.lowercase(Locale.ROOT)
    if (lower.isNullOrEmpty() || lower == "und" || lower == "auto") return null
    ALIASES[lower]?.let { return it }
    ALL.firstOrNull { it.lowercase(Locale.ROOT) == lower }?.let { return it }
    val base = lower.substringBefore('-')
    if (base == "zh") {
      // A script or region after zh tells the two apart: zh-Hant-TW, zh-Hans-CN.
      return if ("hant" in lower || lower.endsWith("-tw") || lower.endsWith("-hk")) CHINESE_TRADITIONAL
      else CHINESE_SIMPLIFIED
    }
    return ALIASES[base] ?: ALL.firstOrNull { it == base }
  }

  /** The language of [locale], such as the device's, or English when it isn't in [ALL]. */
  @JvmStatic
  fun of(locale: Locale): String =
    normalize(if (locale.language == "zh") chineseTag(locale) else locale.language) ?: "en"

  private fun chineseTag(locale: Locale): String =
    when {
      locale.script.isNotEmpty() -> "zh-${locale.script}"
      locale.country.isNotEmpty() -> "zh-${locale.country}"
      else -> "zh"
    }

  /** The name of [code] in [inLocale], such as "German" or "Deutsch". */
  @JvmStatic
  fun name(code: String, inLocale: Locale = Locale.getDefault()): String {
    // "Chinese (Simplified)" rather than Android's "Chinese (Simplified Han)".
    val name =
      if (code == CHINESE_SIMPLIFIED || code == CHINESE_TRADITIONAL) {
        ULocale.forLanguageTag(code).getDisplayName(ULocale.forLocale(inLocale))
      } else {
        Locale.forLanguageTag(code).getDisplayName(inLocale)
      }
    return if (name.isEmpty() || name.equals(code, ignoreCase = true)) code
    else name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(inLocale) else it.toString() }
  }

  /** The [ALL] languages, or those of [only], by name in the device's language. */
  fun sorted(only: Set<String>? = null): List<String> {
    val locale = Locale.getDefault()
    return ALL.filter { only == null || it in only }.sortedBy { name(it, locale).lowercase(locale) }
  }

  /** Whether [a] and [b] are the same language, as when a service detects the target language. */
  @JvmStatic
  fun same(a: String?, b: String?): Boolean = a != null && normalize(a) == normalize(b)
}
