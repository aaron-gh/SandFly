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
package com.google.android.accessibility.talkback.keyboard.words

import java.util.Locale

/**
 * The languages of the SandFly keyboard's word lists, from TapType: the word list file in
 * `assets/word_lists`, letters typed for others that the keyboard may lack, and words always
 * written with a capital.
 */
enum class WordLanguage(
  /** Stable identifier, stored in settings and the name of the word list file. */
  val code: String,
  private val localeTag: String,
  /** The name of the language in that language. */
  val displayName: String,
  /** The key layout used where the keyboard's own can't be read. */
  val defaultLayout: KeyLayout,
  /**
   * Letters that a search tries in place of a typed one at a small cost, so that words with letters
   * the keyboard lacks are found: typed 'a' also matches 'á'.
   */
  val variants: Map<Char, List<Char>> = emptyMap(),
  /** Lowercase words always written with a capital, with or without a suffix after '. */
  private val capitalizedWords: Set<String> = emptySet(),
) {
  EN_US("en_us", "en-US", "English (US)", KeyLayout.QWERTY, capitalizedWords = setOf("i")),
  EN_GB("en_gb", "en-GB", "English (UK)", KeyLayout.QWERTY, capitalizedWords = setOf("i")),
  DE(
    "de",
    "de-DE",
    "Deutsch",
    KeyLayout.QWERTZ,
    variants = mapOf('a' to listOf('ä'), 'o' to listOf('ö'), 'u' to listOf('ü'), 's' to listOf('ß')),
  ),
  ES(
    "es",
    "es-ES",
    "Español",
    KeyLayout.QWERTY,
    variants =
      mapOf(
        'a' to listOf('á'),
        'e' to listOf('é'),
        'i' to listOf('í'),
        'n' to listOf('ñ'),
        'o' to listOf('ó'),
        'u' to listOf('ú', 'ü'),
      ),
  ),
  FR(
    "fr",
    "fr-FR",
    "Français",
    KeyLayout.AZERTY,
    variants =
      mapOf(
        'a' to listOf('à', 'â', 'æ'),
        'e' to listOf('é', 'è', 'ê', 'ë'),
        'i' to listOf('î', 'ï'),
        'o' to listOf('ô', 'œ'),
        'u' to listOf('ù', 'û', 'ü'),
        'c' to listOf('ç'),
        'y' to listOf('ÿ'),
      ),
  ),
  PT_BR("pt_br", "pt-BR", "Português (Brasil)", KeyLayout.QWERTY, variants = PORTUGUESE_VARIANTS),
  PT_PT("pt_pt", "pt-PT", "Português (Portugal)", KeyLayout.QWERTY, variants = PORTUGUESE_VARIANTS),
  HU(
    "hu",
    "hu-HU",
    "Magyar",
    KeyLayout.QWERTZ,
    variants =
      mapOf(
        'a' to listOf('á'),
        'e' to listOf('é'),
        'i' to listOf('í'),
        'o' to listOf('ó', 'ö', 'ő'),
        'u' to listOf('ú', 'ü', 'ű'),
      ),
  ),
  TR(
    "tr",
    "tr-TR",
    "Türkçe",
    KeyLayout.QWERTY,
    variants =
      mapOf(
        'c' to listOf('ç'),
        'g' to listOf('ğ'),
        'i' to listOf('ı'),
        'ı' to listOf('i'),
        'o' to listOf('ö'),
        's' to listOf('ş'),
        'u' to listOf('ü'),
      ),
  ),
  RU("ru", "ru-RU", "Русский", KeyLayout.RUSSIAN, variants = mapOf('е' to listOf('ё'))),
  UK("uk", "uk-UA", "Українська", KeyLayout.UKRAINIAN, variants = mapOf('г' to listOf('ґ'))),
  AR(
    "ar",
    "ar",
    "العربية",
    KeyLayout.ARABIC,
    // Hamza and the other letter forms often typed alike.
    variants =
      mapOf(
        'ا' to listOf('أ', 'إ', 'آ'),
        'د' to listOf('ذ'),
        'ه' to listOf('ة'),
        'ي' to listOf('ى', 'ئ'),
        'ى' to listOf('ي'),
        'و' to listOf('ؤ'),
      ),
  );

  val locale: Locale
    get() = Locale.forLanguageTag(localeTag)

  /** The name of the word list file in the word list assets. */
  val fileName: String
    get() = "$code.txt"

  fun lowercase(text: String): String = text.lowercase(locale)

  fun uppercase(text: String): String = text.uppercase(locale)

  /** [word] with its first letter a capital, by this language's rules (Turkish i becomes İ). */
  fun capitalize(word: String): String = word.replaceFirstChar { it.titlecase(locale) }

  /** [word], in lowercase, with the capital it is always written with, or null if it has none. */
  fun fixedCapitalization(word: String): String? {
    if (word.substringBefore('\'') !in capitalizedWords) return null
    return capitalize(word)
  }

  companion object {
    /**
     * The language for [locale]: the same language and country, else the first of the language, or
     * null where there is no word list for it.
     */
    fun forLocale(locale: Locale): WordLanguage? {
      if (locale.language.isEmpty()) return null
      return entries.firstOrNull {
        it.locale.language == locale.language && it.locale.country == locale.country
      } ?: entries.firstOrNull { it.locale.language == locale.language }
    }

    fun fromCode(code: String?): WordLanguage? = entries.firstOrNull { it.code == code }
  }
}

private val PORTUGUESE_VARIANTS =
  mapOf(
    'a' to listOf('á', 'â', 'ã', 'à'),
    'e' to listOf('é', 'ê'),
    'i' to listOf('í'),
    'o' to listOf('ó', 'ô', 'õ'),
    'u' to listOf('ú', 'ü'),
    'c' to listOf('ç'),
  )
