/*
 * Copyright 2026 SandFly contributors
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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslationResult;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.Character.UnicodeScript;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Translates text to braille with a main translator, and the runs of characters that the main
 * table doesn't define, with the letters of the same script next to them, with a secondary
 * translator. Whitespace between two runs joins them. Braille is back-translated with the main
 * translator alone.
 *
 * <p>Each run is translated alone, so the main table's rules never see the secondary table's
 * characters, and the secondary table's rules don't change the braille of the main table's text.
 */
public class SecondaryTableTranslator implements BrailleTranslator {
  private static final String TAG = "SecondaryTableTranslator";

  /** The most characters whose definition in the main table is kept. */
  @VisibleForTesting static final int MAX_CACHED_CHARACTERS = 1024;

  /** The characters that a table defines. */
  interface CharacterDefinitions {
    /** Whether the table defines each UTF-16 unit of [text], or null if that isn't known. */
    @Nullable
    boolean[] defined(String text);

    /** Changes whenever the definitions can change, such as after custom characters change. */
    int generation();
  }

  private final BrailleTranslator main;
  private final BrailleTranslator secondary;
  private final CharacterDefinitions mainDefinitions;
  private final BooleanSupplier secondaryCompiles;
  private final String description;

  /** Null until the secondary table's tables are checked. */
  @Nullable private volatile Boolean secondaryUsable;

  /** Whether the main table defines each character, as last checked. */
  private final Map<Character, Boolean> cache =
      new LinkedHashMap<>(16, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Character, Boolean> eldest) {
          return size() > MAX_CACHED_CHARACTERS;
        }
      };

  private int cacheGeneration;

  /**
   * [secondaryCompiles] is checked once, before the first text that needs the secondary table, and
   * text is translated with [main] alone if it returns false. [description] names the tables in
   * the log.
   */
  @VisibleForTesting
  SecondaryTableTranslator(
      BrailleTranslator main,
      BrailleTranslator secondary,
      CharacterDefinitions mainDefinitions,
      BooleanSupplier secondaryCompiles,
      String description) {
    this.main = main;
    this.secondary = secondary;
    this.mainDefinitions = mainDefinitions;
    this.secondaryCompiles = secondaryCompiles;
    this.description = description;
    cacheGeneration = mainDefinitions.generation();
  }

  @Override
  public String translateToPrint(BrailleWord brailleWord) {
    return main.translateToPrint(brailleWord);
  }

  @Override
  public String translateToPrint(BrailleWord brailleWord, String before, String after) {
    return main.translateToPrint(brailleWord, before, after);
  }

  @Override
  public String translateToPrintPartial(BrailleWord brailleWord) {
    return main.translateToPrintPartial(brailleWord);
  }

  @Override
  public TranslationResult translate(CharSequence text, int cursorPosition) {
    boolean[] fromSecondary = secondaryCharacters(text.toString());
    if (fromSecondary == null || !isSecondaryUsable()) {
      return main.translate(text, cursorPosition);
    }
    BrailleWord cells = new BrailleWord();
    List<Integer> textToBraille = new ArrayList<>(text.length());
    List<Integer> brailleToText = new ArrayList<>();
    int cursorBytePosition = -1;
    int start = 0;
    while (start < text.length()) {
      boolean secondaryRun = fromSecondary[start];
      int end = start + 1;
      while (end < text.length() && fromSecondary[end] == secondaryRun) {
        end++;
      }
      CharSequence run = text.subSequence(start, end);
      boolean cursorInRun = start <= cursorPosition && cursorPosition < end;
      TranslationResult result =
          (secondaryRun ? secondary : main)
              .translate(run, cursorInRun ? cursorPosition - start : -1);
      if (result == null
          || result.textToBraillePositions().size() != run.length()
          || result.brailleToTextPositions().size() != result.cells().size()) {
        return main.translate(text, cursorPosition);
      }
      int cellOffset = cells.size();
      cells.append(result.cells());
      for (int position : result.textToBraillePositions()) {
        textToBraille.add(cellOffset + position);
      }
      for (int position : result.brailleToTextPositions()) {
        brailleToText.add(start + position);
      }
      if (cursorInRun && result.cursorBytePosition() >= 0) {
        cursorBytePosition = cellOffset + result.cursorBytePosition();
      }
      start = end;
    }
    if (cursorPosition >= text.length()) {
      // Past the end of the text, as liblouis gives it.
      cursorBytePosition = cells.size();
    }
    return TranslationResult.builder()
        .setText(text)
        .setCells(cells)
        .setTextToBraillePositions(textToBraille)
        .setBrailleToTextPositions(brailleToText)
        .setCursorBytePosition(cursorBytePosition)
        .build();
  }

  /**
   * Which characters of [text] are translated with the secondary table, or null if none are: those
   * the main table doesn't define, the letters of the same script next to them, so that a word
   * isn't split between the tables, and whitespace between two of them. Braille Unicode characters
   * stay with the main table, which shows them as their cells.
   */
  @VisibleForTesting
  @Nullable
  boolean[] secondaryCharacters(String text) {
    boolean[] defined = mainDefined(text);
    if (defined == null) {
      return null;
    }
    int length = text.length();
    boolean[] undefined = new boolean[length];
    boolean any = false;
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (!defined[i] && !Character.isWhitespace(c) && !isBrailleUnicode(c)) {
        undefined[i] = true;
        any = true;
      }
    }
    if (!any) {
      return null;
    }
    boolean[] fromSecondary = undefined.clone();
    for (int i = 0; i < length; i++) {
      if (!undefined[i]) {
        continue;
      }
      UnicodeScript script = UnicodeScript.of(text.charAt(i));
      if (script == UnicodeScript.COMMON
          || script == UnicodeScript.INHERITED
          || script == UnicodeScript.UNKNOWN) {
        continue;
      }
      for (int j = i - 1; j >= 0 && !fromSecondary[j] && isOfScript(text.charAt(j), script); j--) {
        fromSecondary[j] = true;
      }
      for (int j = i + 1; j < length && !undefined[j] && isOfScript(text.charAt(j), script); j++) {
        fromSecondary[j] = true;
      }
    }
    int lastSecondary = -1;
    for (int i = 0; i < length; i++) {
      if (!fromSecondary[i]) {
        continue;
      }
      if (lastSecondary >= 0 && lastSecondary < i - 1 && onlyWhitespace(text, lastSecondary + 1, i)) {
        for (int j = lastSecondary + 1; j < i; j++) {
          fromSecondary[j] = true;
        }
      }
      lastSecondary = i;
    }
    return fromSecondary;
  }

  /** Whether [c] is a letter of [script] or a combining mark. */
  private static boolean isOfScript(char c, UnicodeScript script) {
    UnicodeScript charScript = UnicodeScript.of(c);
    return charScript == script || charScript == UnicodeScript.INHERITED;
  }

  private static boolean onlyWhitespace(String text, int start, int end) {
    for (int i = start; i < end; i++) {
      if (!Character.isWhitespace(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /** Whether the main table defines each character of [text], or null if unknown. */
  @Nullable
  private boolean[] mainDefined(String text) {
    boolean[] defined = new boolean[text.length()];
    Set<Character> unknown = new LinkedHashSet<>();
    synchronized (cache) {
      int generation = mainDefinitions.generation();
      if (generation != cacheGeneration) {
        cache.clear();
        cacheGeneration = generation;
      }
      for (int i = 0; i < text.length(); i++) {
        Boolean known = cache.get(text.charAt(i));
        if (known == null) {
          unknown.add(text.charAt(i));
        } else {
          defined[i] = known;
        }
      }
    }
    if (unknown.isEmpty()) {
      return defined;
    }
    StringBuilder chars = new StringBuilder(unknown.size());
    for (char c : unknown) {
      chars.append(c);
    }
    // Checked outside the cache's lock, since liblouis's lock is held while tables are written.
    boolean[] checked = mainDefinitions.defined(chars.toString());
    if (checked == null || checked.length != chars.length()) {
      return null;
    }
    Map<Character, Boolean> found = new LinkedHashMap<>();
    for (int i = 0; i < chars.length(); i++) {
      found.put(chars.charAt(i), checked[i]);
    }
    synchronized (cache) {
      cache.putAll(found);
    }
    for (int i = 0; i < text.length(); i++) {
      Boolean known = found.get(text.charAt(i));
      if (known != null) {
        defined[i] = known;
      }
    }
    return defined;
  }

  private boolean isSecondaryUsable() {
    Boolean usable = secondaryUsable;
    if (usable == null) {
      usable = secondaryCompiles.getAsBoolean();
      if (!usable) {
        LogUtils.w(TAG, "Secondary table doesn't compile, left out: %s", description);
      }
      secondaryUsable = usable;
    }
    return usable;
  }

  private static boolean isBrailleUnicode(char c) {
    return c >= '⠀' && c <= '⣿';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SecondaryTableTranslator other)) {
      return false;
    }
    return main.equals(other.main) && secondary.equals(other.secondary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(main, secondary);
  }
}
