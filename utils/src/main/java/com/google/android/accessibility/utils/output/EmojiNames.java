/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Finds emoji in text and names them in one language, from Unicode CLDR. An emoji can be a
 * sequence of characters: a skin tone, a family joined by zero-width joiners, a flag or a keycap
 * is one emoji with one name, so the longest sequence that is an emoji wins.
 *
 * <p>Variation selector 16, which asks for emoji style, is ignored when matching, since apps send
 * emoji both with and without it. A character that is shown as text unless that selector follows
 * it, such as © or ❤, is only an emoji when it does follow.
 *
 * <p>The emoji are listed once, in an {@link Index} that every language shares, and each language
 * holds only its names. The files are made by tools/emoji_names/make_emoji_names.py, which
 * describes their format.
 */
final class EmojiNames {
  /** The folder in the app's assets that holds the emoji and a file of names for each language. */
  static final String ASSET_FOLDER = "emoji_names";

  /** The file in {@link #ASSET_FOLDER} that lists the emoji. */
  static final String INDEX_FILE = "emoji";

  /** The licence of the data, in {@link #ASSET_FOLDER}. */
  static final String LICENSE_FILE = "LICENSE";

  /**
   * The extension of the files in the source, which are gzipped text. The Android Gradle plugin
   * unzips assets that end in .gz and drops the extension, so the app reads plain text, which the
   * APK compresses as usual.
   */
  static final String EXTENSION = ".gz";

  /** The first line of a regional file, followed by the language whose names it changes. */
  static final String PARENT = "@parent\t";

  private static final char VARIATION_SELECTOR_TEXT = '\uFE0E';
  private static final char VARIATION_SELECTOR_EMOJI = '\uFE0F';
  private static final char ZERO_WIDTH_JOINER = '\u200D';
  private static final char COMBINING_KEYCAP = '\u20E3';

  /** An emoji in text, from {@code start} to {@code end}, and where it is in the {@link Index}. */
  record Match(int start, int end, int emoji) {}

  /** The emoji, which every language shares, and how to find them in text. */
  static final class Index {
    /** Where each emoji is in the list, by the emoji without variation selectors. */
    private final Map<String, Integer> positions = new HashMap<>();

    /** Every shorter start of an emoji, so matching stops when none can follow. */
    private final Set<String> prefixes = new HashSet<>();

    /** Single characters that are only emoji when variation selector 16 follows them. */
    private final Set<String> textStyle = new HashSet<>();

    /** The characters emoji start with. */
    private final BitSet starts = new BitSet();

    /** The characters that start an emoji of more than one character, such as ❤ in ❤‍🔥. */
    private final BitSet startsSequences = new BitSet();

    /** The characters in {@link #textStyle}. */
    private final BitSet textStyleCharacters = new BitSet();

    private int size;

    /** Reads the list of emoji, one fully qualified emoji a line. */
    static Index read(BufferedReader reader) throws IOException {
      Index index = new Index();
      String line;
      while ((line = reader.readLine()) != null) {
        index.add(line);
      }
      return index;
    }

    /** Adds the next emoji, in its fully qualified form. Empty lines keep their place. */
    void add(String emoji) {
      int position = size++;
      String key = withoutSelectors(emoji);
      if (key.isEmpty()) {
        return;
      }
      positions.put(key, position);
      int first = key.codePointAt(0);
      starts.set(first);
      int firstLength = Character.charCount(first);
      if (key.length() == firstLength && emoji.length() > key.length()) {
        textStyle.add(key);
        textStyleCharacters.set(first);
      }
      if (key.length() > firstLength) {
        startsSequences.set(first);
      }
      for (int i = firstLength; i < key.length(); i += Character.charCount(key.codePointAt(i))) {
        prefixes.add(key.substring(0, i));
      }
    }

    /** Returns how many emoji there are, which is how many names a language has. */
    int size() {
      return size;
    }

    /** Returns whether {@code text} may hold an emoji: whether a character emoji start with is. */
    boolean mayHaveEmoji(CharSequence text) {
      for (int i = 0; i < text.length(); i++) {
        if (mayStartEmoji(text, i)) {
          return true;
        }
      }
      return false;
    }

    /** Returns whether an emoji may start at {@code i}. */
    boolean mayStartEmoji(CharSequence text, int i) {
      char c = text.charAt(i);
      if (c == '#' || c == '*' || (c >= '0' && c <= '9')) {
        return isKeycap(text, i);
      }
      int codePoint = Character.codePointAt(text, i);
      if (!starts.get(codePoint)) {
        return false;
      }
      // Such as ™, which is text unless variation selector 16 follows it.
      if (textStyleCharacters.get(codePoint) && !startsSequences.get(codePoint)) {
        int next = i + Character.charCount(codePoint);
        return next < text.length() && text.charAt(next) == VARIATION_SELECTOR_EMOJI;
      }
      return true;
    }

    /** Returns the emoji in {@code text}, in order. */
    List<Match> findAll(CharSequence text) {
      List<Match> matches = new ArrayList<>();
      int i = 0;
      while (i < text.length()) {
        @Nullable Match match = mayStartEmoji(text, i) ? find(text, i) : null;
        if (match != null) {
          matches.add(match);
          i = match.end();
        } else {
          i += Character.charCount(Character.codePointAt(text, i));
        }
      }
      return matches;
    }

    /** Returns the longest emoji that starts at {@code start}, or null if none does. */
    @Nullable Match find(CharSequence text, int start) {
      StringBuilder key = new StringBuilder();
      int end = -1;
      int emoji = -1;
      int i = start;
      while (i < text.length()) {
        int codePoint = Character.codePointAt(text, i);
        if (codePoint == VARIATION_SELECTOR_EMOJI) {
          i++;
          if (end == i - 1) {
            end = i;
          }
          continue;
        }
        key.appendCodePoint(codePoint);
        i += Character.charCount(codePoint);
        String candidate = key.toString();
        @Nullable Integer found = positions.get(candidate);
        if (found != null
            && (!textStyle.contains(candidate)
                || (i < text.length() && text.charAt(i) == VARIATION_SELECTOR_EMOJI))) {
          end = i;
          emoji = found;
        }
        if (!prefixes.contains(candidate)) {
          break;
        }
      }
      if (emoji < 0) {
        return null;
      }
      // An emoji asked to look like text is still the same emoji.
      if (end < text.length() && text.charAt(end) == VARIATION_SELECTOR_TEXT) {
        end++;
      }
      return new Match(start, end, emoji);
    }
  }

  /** Names by their emoji's place in the {@link Index}, null where there is none. */
  private final String[] names;

  private boolean empty = true;

  EmojiNames(Index index) {
    names = new String[index.size()];
  }

  /**
   * Adds the names in a names file, replacing any the file has its own for. Its {@link #PARENT}
   * line is skipped: the caller reads the parent's names first.
   */
  void read(BufferedReader reader) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      int tab = line.indexOf('\t');
      if (tab <= 0 || tab == line.length() - 1 || line.startsWith(PARENT)) {
        continue;
      }
      int emoji;
      try {
        emoji = Integer.parseInt(line.substring(0, tab));
      } catch (NumberFormatException e) {
        continue;
      }
      if (emoji >= 0 && emoji < names.length) {
        names[emoji] = line.substring(tab + 1);
        empty = false;
      }
    }
  }

  boolean isEmpty() {
    return empty;
  }

  /** Returns the name of the emoji at this place in the {@link Index}, or null if it has none. */
  @Nullable String name(int emoji) {
    return emoji >= 0 && emoji < names.length ? names[emoji] : null;
  }

  /**
   * Returns whether {@code text} may hold an emoji, before the {@link Index} is loaded, so that
   * most text is passed on untouched. It allows more than the index, which then checks.
   */
  static boolean mayHaveEmoji(CharSequence text) {
    for (int i = 0; i < text.length(); i++) {
      if (mayStartEmoji(text, i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether an emoji may start at {@code i}, before the {@link Index} is loaded: the
   * ranges emoji are in, and keycaps.
   */
  static boolean mayStartEmoji(CharSequence text, int i) {
    char c = text.charAt(i);
    if (c == '©' || c == '®' || (c >= '‼' && c <= '⭕')) {
      return true;
    }
    if (c == '〰' || c == '〽' || c == '㊗' || c == '㊙') {
      return true;
    }
    if (c >= '\uD83C' && c <= '\uD83E') {
      return true;
    }
    if (c == '#' || c == '*' || (c >= '0' && c <= '9')) {
      return isKeycap(text, i);
    }
    return false;
  }

  /**
   * Returns whether the character at {@code i} may be part of an emoji: a character an emoji may
   * start with, or one that joins or changes one, such as a zero-width joiner, a variation
   * selector, a keycap, a skin tone or a tag. Text between characters that can't be part of one
   * starts and ends with whole emoji.
   */
  static boolean mayBePartOfEmoji(CharSequence text, int i) {
    char c = text.charAt(i);
    return Character.isSurrogate(c)
        || c == ZERO_WIDTH_JOINER
        || c == VARIATION_SELECTOR_EMOJI
        || c == VARIATION_SELECTOR_TEXT
        || c == COMBINING_KEYCAP
        || c == '#'
        || c == '*'
        || (c >= '0' && c <= '9')
        || mayStartEmoji(text, i);
  }

  /** Returns whether a keycap starts at {@code i}, with or without variation selector 16. */
  private static boolean isKeycap(CharSequence text, int i) {
    int next = i + 1;
    if (next < text.length() && text.charAt(next) == VARIATION_SELECTOR_EMOJI) {
      next++;
    }
    return next < text.length() && text.charAt(next) == COMBINING_KEYCAP;
  }

  private static String withoutSelectors(String emoji) {
    return emoji.replace(String.valueOf(VARIATION_SELECTOR_EMOJI), "");
  }

  /**
   * Returns the names files to try for {@code locale}, best first, as CLDR locales. Android and
   * CLDR name some languages differently, and Chinese is told apart by script, not by country.
   */
  static List<String> filesFor(Locale locale) {
    // The language tag, unlike getLanguage(), gives "he", "id" and "yi" rather than old codes.
    String language = locale.toLanguageTag().split("-", 2)[0];
    switch (language) {
      case "tl" -> language = "fil";
      case "nb" -> language = "no";
      default -> {}
    }
    String script = locale.getScript();
    String region = locale.getCountry();
    if (language.equals("zh") && script.isEmpty()) {
      script = region.equals("TW") || region.equals("HK") || region.equals("MO") ? "Hant" : "Hans";
    }
    if (language.equals("yue") && script.isEmpty()) {
      script = region.equals("CN") ? "Hans" : "Hant";
    }
    List<String> files = new ArrayList<>();
    if (!script.isEmpty()) {
      if (!region.isEmpty()) {
        files.add(language + "-" + script + "-" + region);
      }
      files.add(language + "-" + script);
    }
    if (!region.isEmpty()) {
      files.add(language + "-" + region);
      if (language.equals("es") && !region.equals("ES")) {
        files.add("es-419");
      }
    }
    files.add(language);
    return files;
  }
}
