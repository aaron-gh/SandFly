/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.translate.liblouis;

import android.content.Context;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslatorFactory;
import com.google.android.accessibility.braille.translate.japanese.JapaneseBrailleTranslator;

/** Creates Translator instances that delegate to the LibLouis braille translation library. */
public class LibLouis implements TranslatorFactory {

  @Override
  public BrailleTranslator create(Context context, String codeName, boolean contractedMode) {
    switch (codeName) {
      case "AFRIKAANS" -> {
        return getBrailleTranslator(
            context, codeName, contractedMode, "afr-za-g1.ctb", "afr-za-g2.ctb");
      }
      case "ARABIC" -> {
        LibLouisTranslator uncontracted = new LibLouisTranslator(context, codeName, "ar-ar-g1.utb");
        LibLouisTranslator contracted = new LibLouisTranslator(context, codeName, "ar-ar-g2.ctb");
        if (contractedMode) {
          return new ExpandableContractedTranslator(uncontracted, contracted);
        }
        return uncontracted;
      }
      case "ARABIC_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "ar-ar-comp8.utb");
      }
      case "BULGARIAN" -> {
        return new LibLouisTranslator(context, codeName, "bg.utb");
      }
      case "BULGARIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "bg.tbl");
      }
      case "CANTONESE" -> {
        return new LibLouisTranslator(context, codeName, "zh-hk.ctb");
      }
      case "CHINESE_TAIWAN" -> {
        return new LibLouisTranslator(context, codeName, "zh-tw.ctb");
      }
      case "CHINESE_CHINA_CURRENT_WITH_TONES" -> {
        return new LibLouisTranslator(context, codeName, "zhcn-g1.ctb");
      }
      case "CHINESE_CHINA_CURRENT_WITHOUT_TONES" -> {
        return new LibLouisTranslator(context, codeName, "zh_CHN.tbl");
      }
      case "CHINESE_CHINA_TWO_CELLS" -> {
        return new LibLouisTranslator(context, codeName, "zhcn-g2.ctb");
      }
      case "CHINESE_CHINA_COMMON" -> {
        return new LibLouisTranslator(context, codeName, "zhcn-cbs.ctb");
      }
      case "CATALAN" -> {
        return new LibLouisTranslator(context, codeName, "ca.tbl");
      }
      case "CROATIAN" -> {
        return new LibLouisTranslator(context, codeName, "hr-g1.tbl");
      }
      case "CROATIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "hr-comp8.tbl");
      }
      case "CZECH" -> {
        return new LibLouisTranslator(context, codeName, "cs.tbl");
      }
      case "CZECH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "cs-comp8.utb");
      }
      case "DANISH" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "da-dk-g16.ctb", "da-dk-g26.ctb");
      }
      case "DANISH_8" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "da-dk-g18.ctb", "da-dk-g28.ctb");
      }
      case "DANISH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "da-dk-g08.ctb");
      }
      case "DUTCH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "nl-comp8.utb");
      }
      case "DUTCH_NL" -> {
        return new LibLouisTranslator(context, codeName, "nl-NL-g0.utb");
      }
      case "EN_UK" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "en-gb-g1.utb", "en_GB.tbl");
      }
      case "EN_IN" -> {
        return new LibLouisTranslator(context, codeName, "en-in-g1.ctb");
      }
      case "EN_NABCC" -> {
        return new LibLouisTranslator(context, codeName, "en-nabcc.utb");
      }
      case "EN_COMP6" -> {
        return new LibLouisTranslator(context, codeName, "en-us-comp6.ctb");
      }
      case "UEB" -> {
        return getBrailleTranslator(
            contractedMode,
            new LibLouisTranslatorUeb1(context, codeName),
            new LibLouisTranslatorUeb2(context, codeName));
      }
      case "EN_US_EBAE" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "en-us-g1.ctb", "en_US.tbl");
      }
      case "ESTONIAN" -> {
        return new LibLouisTranslator(context, codeName, "et-g0.utb");
      }
      case "FINNISH" -> {
        return new LibLouisTranslator(context, codeName, "fi.utb");
      }
      case "FINNISH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "fi-fi-8dot.ctb");
      }
      case "FRENCH" -> {
        return getBrailleTranslator(
            contractedMode,
            new LibLouisTranslatorFrench(context, codeName),
            new LibLouisTranslator(context, codeName, "fr-bfu-g2.ctb"));
      }
      case "FRENCH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "fr-bfu-comp8.utb");
      }
      case "GERMAN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "de-g0.utb", "de-g2.ctb");
      }
      case "GERMAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "de-de-comp8.ctb");
      }
      case "GREEK" -> {
        return new LibLouisTranslator(context, codeName, "el.ctb");
      }
      case "GREEK_INTERNATIONAL" -> {
        return new LibLouisTranslator(context, codeName, "grc-international-en.utb");
      }
      case "GUJARATI" -> {
        return new LibLouisTranslator(context, codeName, "gu.tbl");
      }
      case "HEBREW" -> {
        return new LibLouisTranslator(context, codeName, "he-IL.utb");
      }
      case "HEBREW_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "he-IL-comp8.utb");
      }
      case "HINDI" -> {
        return new LibLouisTranslator(context, codeName, "hi.tbl");
      }
      case "HUNGARIAN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "hu.tbl", "hu-hu-g2.ctb");
      }
      case "HUNGARIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "hu-hu-comp8.ctb");
      }
      case "ICELANDIC" -> {
        return new LibLouisTranslator(context, codeName, "is.tbl");
      }
      case "ICELANDIC_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "is.ctb");
      }
      case "INDONESIAN" -> {
        return getBrailleTranslator(
            context, codeName, contractedMode, "id-id-g1.utb", "id-id-g2.ctb");
      }
      case "ITALIAN_COMP6" -> {
        return new LibLouisTranslator(context, codeName, "it.tbl");
      }
      case "ITALIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "it-it-comp8.utb");
      }
      case "JAPANESE" -> {
        return new LibLouisTranslator(context, codeName, "ja-kantenji-ucs2.utb");
      }
      case "JAPANESE_KANA" -> {
        // Kana cells are the same in kantenji, so input is read back with its table.
        return new JapaneseBrailleTranslator(
            context, new LibLouisTranslator(context, codeName, "ja-kantenji-ucs2.utb"));
      }
      case "KANNADA" -> {
        return new LibLouisTranslator(context, codeName, "kn.tbl");
      }
      case "KHMER" -> {
        return new LibLouisTranslator(context, codeName, "km-g1.utb");
      }
      case "KOREAN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "ko-g1.ctb", "ko-g2.ctb");
      }
      case "KOREAN_2006" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "ko-2006-g1.ctb", "ko-2006-g2.ctb");
      }
      case "KURDISH" -> {
        return new LibLouisTranslator(context, codeName, "ckb.tbl");
      }
      case "LITHUANIAN" -> {
        return new LibLouisTranslator(context, codeName, "lt-6dot.utb");
      }
      case "LITHUANIAN_8" -> {
        return new LibLouisTranslator(context, codeName, "lt-8dot.utb");
      }
      case "LATVIAN" -> {
        return new LibLouisTranslator(context, codeName, "lv.tbl");
      }
      case "MALAYALAM_IN" -> {
        return new LibLouisTranslator(context, codeName, "ml.tbl");
      }
      case "MARATHI_IN" -> {
        return new LibLouisTranslator(context, codeName, "mr.tbl");
      }
      case "MONGOLIAN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "mn-MN-g1.utb", "mn-MN-g2.ctb");
      }
      case "NEPALI" -> {
        return new LibLouisTranslator(context, codeName, "ne.ctb");
      }
      case "NEPALI_IN" -> {
        return new LibLouisTranslator(context, codeName, "ne.tbl");
      }
      case "NORWEGIAN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "no-no-g0.utb", "no-no-g3.ctb");
      }
      case "NORWEGIAN_8" -> {
        return new LibLouisTranslator(context, codeName, "no-no-8dot.utb");
      }
      case "NORWEGIAN_8_6FALLBACK" -> {
        return new LibLouisTranslator(context, codeName, "no-no-8dot-fallback-6dot-g0.utb");
      }
      case "NORWEGIAN_8_NO" -> {
        return new LibLouisTranslator(context, codeName, "no-no-generic.ctb");
      }
      case "NORWEGIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "no-no-comp8.ctb");
      }
      case "PERSIAN" -> {
        return new LibLouisTranslator(context, codeName, "fa-ir-g1.utb");
      }
      case "PERSIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "fa-ir-comp8.ctb");
      }
      case "POLISH" -> {
        return new LibLouisTranslator(context, codeName, "pl.tbl");
      }
      case "POLISH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "pl-pl-comp8.ctb");
      }
      case "PORTUGUESE" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "pt-pt-g1.utb", "pt.tbl");
      }
      case "PORTUGUESE_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "pt-pt-comp8.ctb");
      }
      case "PUNJABI" -> {
        return new LibLouisTranslator(context, codeName, "pa.tbl");
      }
      case "ROMANIAN" -> {
        return new LibLouisTranslator(context, codeName, "ro-g0-sandfly.utb");
      }
      case "ROMANIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "ro.tbl");
      }
      case "RUSSIAN" -> {
        return new LibLouisTranslator(context, codeName, "ru-ru-g1.ctb");
      }
      case "RUSSIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "ru-comp8.utb");
      }
      case "RUSSIAN_DETAILED" -> {
        return new LibLouisTranslator(context, codeName, "ru-litbrl-detailed.utb");
      }
      case "SERBIAN" -> {
        return new LibLouisTranslator(context, codeName, "sr-g1.ctb");
      }
      case "SINDHI_IN" -> {
        return new LibLouisTranslator(context, codeName, "sd.tbl");
      }
      case "SINHALA" -> {
        return new LibLouisTranslator(context, codeName, "sin.utb");
      }
      case "SLOVAK" -> {
        return new LibLouisTranslator(context, codeName, "sk-g1.ctb");
      }
      case "SPANISH" -> {
        return getBrailleTranslator(
            contractedMode,
            new LibLouisTranslatorSpanish(context, codeName),
            new LibLouisTranslator(context, codeName, "es-g2.ctb"));
      }
      case "SPANISH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "Es-Es-G0.utb");
      }
      case "SWEDEN" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "sv-g0.utb", "sv-g2.ctb");
      }
      case "SWEDEN_8" -> {
        return new LibLouisTranslator(context, codeName, "se-se.ctb");
      }
      case "SWEDISH_COMP8_1989" -> {
        return new LibLouisTranslator(context, codeName, "sv-1989.ctb");
      }
      case "SWEDISH_COMP8_1996" -> {
        return new LibLouisTranslator(context, codeName, "sv-1996.ctb");
      }
      case "TAMIL" -> {
        return new LibLouisTranslator(context, codeName, "ta-ta-g1.ctb");
      }
      case "TELUGU_IN" -> {
        return new LibLouisTranslator(context, codeName, "te.tbl");
      }
      case "TURKISH_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "tr.tbl");
      }
      case "TURKISH" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "tr-g1.ctb", "tr-g2.tbl");
      }
      case "UKRAINIAN" -> {
        return new LibLouisTranslator(context, codeName, "uk.utb");
      }
      case "UKRAINIAN_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "uk-comp.utb");
      }
      case "UNICODE_BRAILLE", "UNICODE_BRAILLE_8" -> {
        // Back-translates each cell to the Unicode braille character with its dots.
        return new LibLouisTranslator(context, codeName, "unicode-braille.utb");
      }
      case "URDU" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "ur-pk-g1.utb", "ur-pk-g2.ctb");
      }
      case "VIETNAMESE" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "vi-vn-g0.utb", "vi-vn-g2.ctb");
      }
      case "VIETNAMESE_COMP8" -> {
        return new LibLouisTranslator(context, codeName, "vi-cb8.utb");
      }
      case "WELSH" -> {
        return getBrailleTranslator(context, codeName, contractedMode, "cy-cy-g1.utb", "cy.tbl");
      }
      default -> {}
    }
    throw new IllegalArgumentException("unrecognized code " + codeName);
  }

  @Override
  public String getLibraryName() {
    return this.getClass().getSimpleName();
  }

  private BrailleTranslator getBrailleTranslator(
      Context context,
      String codeName,
      boolean contractedMode,
      String grade1Table,
      String grade2Table) {
    return getBrailleTranslator(
        contractedMode,
        new LibLouisTranslator(context, codeName, grade1Table),
        new LibLouisTranslator(context, codeName, grade2Table));
  }

  private BrailleTranslator getBrailleTranslator(
      boolean contractedMode, LibLouisTranslator uncontracted, LibLouisTranslator contracted) {
    if (contractedMode) {
      return new ExpandableContractedTranslator(uncontracted, contracted);
    }
    return uncontracted;
  }
}
