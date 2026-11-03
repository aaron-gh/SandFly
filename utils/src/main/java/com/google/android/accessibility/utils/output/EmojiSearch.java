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

package com.google.android.accessibility.utils.output;

import android.content.Context;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.BufferedReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Finds emoji by words of their names, in one language, from the Unicode CLDR names that emoji
 * are spoken with. Skin tone variants and the parts emoji are made of, such as a hair style, are
 * left out, so that each emoji is found once.
 */
public final class EmojiSearch {
  private static final String TAG = "EmojiSearch";

  /** An emoji and its name. */
  public record Emoji(String emoji, String name) {}

  /** The emoji, in Unicode's order, with their names made ready to compare. */
  private final List<Emoji> emoji;

  private final List<String> keys;
  private final Locale language;

  private EmojiSearch(List<Emoji> emoji, Locale language) {
    this.emoji = emoji;
    this.language = language;
    keys = new ArrayList<>(emoji.size());
    for (Emoji each : emoji) {
      keys.add(normalize(each.name(), language));
    }
  }

  /** Makes a search of {@code emoji}, for tests. */
  static EmojiSearch of(List<Emoji> emoji, Locale language) {
    return new EmojiSearch(new ArrayList<>(emoji), language);
  }

  /**
   * Reads the emoji and their names in {@code language}, or English names if there are none for
   * it. Blocks while it reads, so call it off the main thread. With no emoji to read, finds none.
   */
  public static EmojiSearch read(Context context, Locale language) {
    List<String> all = new ArrayList<>();
    try (BufferedReader reader =
        EmojiSpeech.open(context.getAssets(), EmojiNames.INDEX_FILE)) {
      String line;
      while ((line = reader.readLine()) != null) {
        all.add(line);
      }
    } catch (Exception e) {
      LogUtils.e(TAG, "Can't read the emoji: %s", e);
      return new EmojiSearch(new ArrayList<>(), language);
    }
    EmojiNames names = EmojiSpeech.readNames(context, language, all.size());
    List<Emoji> emoji = new ArrayList<>();
    for (int i = 0; i < all.size(); i++) {
      @Nullable String name = names.name(i);
      if (name != null && isOffered(all.get(i))) {
        emoji.add(new Emoji(all.get(i), name));
      }
    }
    return new EmojiSearch(emoji, language);
  }

  /**
   * Whether an emoji is offered: not a skin tone variant of another, nor a part that emoji are
   * made of on its own, such as a skin tone or a hair style.
   */
  static boolean isOffered(String emoji) {
    if (emoji.isEmpty()) {
      return false;
    }
    for (int i = 0; i < emoji.length(); ) {
      int codePoint = emoji.codePointAt(i);
      // Skin tones.
      if (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) {
        return false;
      }
      i += Character.charCount(codePoint);
    }
    int first = emoji.codePointAt(0);
    // Red hair, curly hair, bald and white hair, on their own.
    return !(emoji.length() == Character.charCount(first) && first >= 0x1F9B0 && first <= 0x1F9B3);
  }

  /**
   * Returns at most {@code max} emoji whose names hold every word of {@code query}, anywhere in
   * them, ignoring case and accents. Names that are the query come first, then names that start
   * with it, then names where each word of it starts a word, then the rest, each in Unicode's
   * order.
   */
  public List<Emoji> find(String query, int max) {
    String key = normalize(query, language);
    if (key.isEmpty() || max <= 0) {
      return new ArrayList<>();
    }
    List<String> words = Arrays.asList(key.split(" "));
    List<int[]> found = new ArrayList<>();
    for (int i = 0; i < keys.size(); i++) {
      String name = keys.get(i);
      if (!containsAll(name, words)) {
        continue;
      }
      found.add(new int[] {rank(name, key, words), i});
    }
    found.sort(Comparator.<int[]>comparingInt(entry -> entry[0]).thenComparingInt(e -> e[1]));
    List<Emoji> result = new ArrayList<>();
    for (int i = 0; i < found.size() && i < max; i++) {
      result.add(emoji.get(found.get(i)[1]));
    }
    return result;
  }

  private static boolean containsAll(String name, List<String> words) {
    for (String word : words) {
      if (!name.contains(word)) {
        return false;
      }
    }
    return true;
  }

  private static int rank(String name, String key, List<String> words) {
    if (name.equals(key)) {
      return 0;
    }
    if (name.startsWith(key)) {
      return 1;
    }
    String spaced = " " + name;
    for (String word : words) {
      if (!spaced.contains(" " + word)) {
        return 3;
      }
    }
    return 2;
  }

  /** Returns {@code text} as names are compared with it: see {@link #normalize(String, Locale)}. */
  public String normalize(String text) {
    return normalize(text, language);
  }

  /**
   * Returns {@code text} in lower case, without accents, with each run of other characters than
   * letters and digits as one space, and no space at either end.
   */
  static String normalize(String text, Locale language) {
    String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    StringBuilder result = new StringBuilder(decomposed.length());
    boolean space = false;
    for (int i = 0; i < decomposed.length(); ) {
      int codePoint = decomposed.codePointAt(i);
      i += Character.charCount(codePoint);
      if (Character.getType(codePoint) == Character.NON_SPACING_MARK) {
        continue;
      }
      if (Character.isLetterOrDigit(codePoint)) {
        if (space && result.length() > 0) {
          result.append(' ');
        }
        space = false;
        result.appendCodePoint(codePoint);
      } else {
        space = true;
      }
    }
    return result.toString().toLowerCase(language);
  }
}
