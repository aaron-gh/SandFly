/*
 * Copyright 2020 Google Inc.
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

import static java.util.stream.Collectors.joining;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslationResult;
import com.google.android.accessibility.braille.translate.liblouis.LouisTranslation.TranslationMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A LibLouis translator based on the {@link LibLouisTranslator#tableName}. */
public class LibLouisTranslator implements BrailleTranslator {
  private final Map<BrailleWord, String> bypassMap;
  private final Map<BrailleCharacter, String> commutativityMap;
  private final String tableName;

  /** The braille table this translator is part of, whose custom characters it uses, or null. */
  @Nullable private final String code;

  private final LiblouisLoader loader;

  public LibLouisTranslator(Context context, String tableName) {
    this(context, /* code= */ null, tableName);
  }

  /**
   * A translator with [tableName], in the braille table [code], such as "POLISH", whose custom
   * braille characters take precedence over the table's rules.
   */
  public LibLouisTranslator(Context context, @Nullable String code, String tableName) {
    this.tableName = tableName;
    this.code = code;
    loader = LiblouisLoader.getInstance(context);
    if (code != null) {
      CustomCharacterTables.start(context);
    }
    bypassMap = new LinkedHashMap<>();
    commutativityMap = new LinkedHashMap<>();
  }

  /**
   * Returns a map that allows the backing translator to be bypassed, in case the {@link
   * BrailleWord} is present as a key in the map, in which case the corresponding String value is
   * used as the translation result.
   */
  protected Map<BrailleWord, String> getBypassMap() {
    return bypassMap;
  }

  /**
   * Returns a map that allows the backing translator's translation of certain {@link
   * BrailleCharacter} to be overridden.
   *
   * <p>For example, for Spanish, LibLouis wrongly translates "26" as the number 5, when it actually
   * should be translated as a question mark. To fix this mis-translation, fill the commutativity
   * map with the key-value pair ("26", "?").
   *
   * <p>Note that overriding the translation in this manner can only occur when the concatenation
   * and translation operators commute for the given input phrase.
   */
  protected Map<BrailleCharacter, String> getCommutativityMap() {
    return commutativityMap;
  }

  /** Modifies the result from the backing translator, post-translation. */
  protected String transformPostTranslation(String translation) {
    return translation;
  }

  @Override
  public String translateToPrint(BrailleWord brailleWord) {
    if (!loader.isExtracted()) {
      return "";
    }
    return translateToPrintInternal(brailleWord, /* partial= */ false);
  }

  @Override
  public String translateToPrintPartial(BrailleWord brailleWord) {
    if (!loader.isExtracted()) {
      return "";
    }
    return translateToPrintInternal(brailleWord, /* partial= */ true);
  }

  @Override
  public TranslationResult translate(CharSequence text, int cursorPosition) {
    if (!loader.isExtracted()) {
      return TranslationResult.createUnknown(text, cursorPosition);
    }
    return LouisTranslation.translate(
        text, CustomCharacterTables.tableList(code, tableName), cursorPosition);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LibLouisTranslator libLouisTranslator)) {
      return false;
    }
    return tableName.equals(libLouisTranslator.tableName)
        && Objects.equals(code, libLouisTranslator.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, code);
  }

  String translateToPrintWithContext(
      BrailleWord brailleWord, BrailleWord contextualWord, int[] range, String before, String after) {
    if (!loader.isExtracted()) {
      return "";
    }
    String bypass = getBypassTranslation(brailleWord);
    if (bypass != null) {
      return bypass;
    }

    String translated = translateToPrintDirect(contextualWord, /* partial= */ false, range);
    int start = range[0];
    int end = range[1];
    if (start == before.length()
        && end == translated.length() - after.length()
        && start < end
        && translated.startsWith(before)
        && translated.endsWith(after)) {
      return transformPostTranslation(
          transformUsingCommutativityMap(brailleWord, translated.substring(start, end)));
    }

    // If a rule changed or absorbed some of the existing text, translate the inserted cells
    // alone in partial mode to avoid expanding standalone wordsigns.
    return translateToPrintPartial(brailleWord);
  }

  @Nullable
  private String getBypassTranslation(BrailleWord brailleWord) {
    // Custom characters take precedence over the words this translator corrects.
    return CustomCharacterTables.containsCustomCells(code, brailleWord)
        ? null
        : getBypassMap().get(brailleWord);
  }

  private String translateToPrintInternal(BrailleWord brailleWord, boolean partial) {
    String bypassValueOrNull = getBypassTranslation(brailleWord);
    if (bypassValueOrNull != null) {
      return bypassValueOrNull;
    }

    String translation = translateToPrintDirect(brailleWord, partial);
    translation = transformUsingCommutativityMap(brailleWord, translation);
    translation = transformPostTranslation(translation);
    return translation;
  }

  private String translateToPrintDirect(BrailleWord brailleWord, boolean partial) {
    return translateToPrintDirect(brailleWord, partial, null);
  }

  private String translateToPrintDirect(BrailleWord brailleWord, boolean partial, int[] range) {
    int flags =
        LouisTranslation.TranslationMode.NO_UNDEFINED_DOTS
            | LouisTranslation.TranslationMode.DOTS_IO;
    if (partial) {
      flags |= TranslationMode.PARTIAL_TRANSLATE;
    }
    return LouisTranslation.backTranslate(
        brailleWord.toByteArray(), CustomCharacterTables.tableList(code, tableName), flags, range);
  }

  private String transformUsingCommutativityMap(BrailleWord brailleWord, String translationRaw) {
    Map<BrailleCharacter, String> replacements = commutativityMap;
    if (!brailleWord.containsAny(replacements.keySet())) {
      return translationRaw;
    }
    if (CustomCharacterTables.containsCustomCells(code, brailleWord)) {
      // A cell that is a custom character keeps the custom character's text.
      replacements = new LinkedHashMap<>(commutativityMap);
      replacements
          .keySet()
          .removeIf(cell -> CustomCharacterTables.isCustomCells(code, new BrailleWord(cell)));
      if (!brailleWord.containsAny(replacements.keySet())) {
        return translationRaw;
      }
    }
    List<BrailleWord> tokens = brailleWord.tokenize(replacements.keySet());
    String translateThenConcatenate =
        tokens.stream()
            .map(brailleWord1 -> translateToPrintDirect(brailleWord1, false))
            .collect(joining());
    if (!translationRaw.equals(translateThenConcatenate)) {
      return translationRaw;
    }
    // Hooray - the translate and concatenate operations commute.  That means that we can
    // translate token-by-token, which allows keys in the commutativityMap to be translated
    // to their corresponding commutativityMap values.
    StringBuilder translationSB = new StringBuilder();
    for (BrailleWord token : tokens) {
      String tokenTranslation;
      if (token.size() == 1 && replacements.containsKey(token.get(0))) {
        tokenTranslation = replacements.get(token.get(0));
      } else {
        tokenTranslation = translateToPrintDirect(token, false);
      }
      translationSB.append(tokenTranslation);
    }
    return translationSB.toString();
  }
}
