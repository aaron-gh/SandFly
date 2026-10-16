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

import android.content.Context;
import android.content.res.AssetManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import com.google.android.accessibility.utils.R;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Speaks emoji the way the user chose: as the speech engine reads them, by their names from
 * Backtalk, or not at all. Some engines read emoji badly or not at all, and some users don't want
 * to hear them. Names come from {@link EmojiNames}, in the language the text is spoken in.
 */
public final class EmojiSpeech {
  private static final String TAG = "EmojiSpeech";

  /** The speech engine gets emoji as they are. */
  public static final String MODE_ENGINE = "engine";

  /** Backtalk replaces emoji with their names. */
  public static final String MODE_NAMES = "names";

  /** Backtalk leaves emoji out, unless the text is only emoji. */
  public static final String MODE_NONE = "none";

  /**
   * Most languages kept loaded: the voice's and those text switches to. Each holds only its names,
   * about 300 KB, as the emoji themselves are in the shared index.
   */
  private static final int MAX_LOADED = 4;

  /** Most regional files read on the way to a language's names, as for hi-Latn > en-IN > en. */
  private static final int MAX_PARENTS = 4;

  private static volatile String mode = MODE_ENGINE;

  /** How many times an emoji repeats before it is counted, or 0 to name each one. */
  private static volatile int repeatCount = 0;

  /** The app, for loading names where no context is at hand, such as text iterators. */
  private static volatile @Nullable Context appContext;

  /** The emoji, shared by every language, or null until loaded. */
  private static volatile EmojiNames.@Nullable Index index;

  private static final Object indexLock = new Object();

  /** Loaded names by file, least recently used first. Guarded by itself. */
  private static final LinkedHashMap<String, EmojiNames> loaded =
      new LinkedHashMap<>(MAX_LOADED + 1, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EmojiNames> eldest) {
          return size() > MAX_LOADED;
        }
      };

  /**
   * Names being loaded, by file, so that a second request for them waits for the first rather
   * than reading them again, and requests for other files don't wait. Guarded by {@link #loaded}.
   */
  private static final Map<String, FutureTask<EmojiNames>> loading = new HashMap<>();

  /** The names files in the assets, read once. Guarded by {@link #loaded}. */
  private static @Nullable Set<String> files;

  /**
   * Loads names ahead of the emoji that need them, on one low-priority thread that ends when idle,
   * so that speech on the main thread doesn't wait for them.
   */
  private static final ThreadPoolExecutor preloader =
      new ThreadPoolExecutor(
          0,
          1,
          10,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          runnable -> {
            Thread thread = new Thread(runnable, "EmojiNames");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
          });

  private EmojiSpeech() {}

  /** Sets how emoji are spoken, one of the MODE values. Unknown values leave it to the engine. */
  public static void setMode(Context context, String newMode) {
    Context app = context.getApplicationContext();
    appContext = app;
    mode =
        newMode.equals(MODE_NAMES) || newMode.equals(MODE_NONE) ? newMode : MODE_ENGINE;
    if (mode.equals(MODE_ENGINE)) {
      clear();
    } else {
      preload(app, LanguageSwitch.spokenLanguage(null));
    }
  }

  /**
   * The voice's language changed, so loads its names ahead of the first emoji spoken in it, while
   * Backtalk names or leaves out emoji.
   */
  static void onLanguageChanged() {
    @Nullable Context app = appContext;
    if (app != null && !mode.equals(MODE_ENGINE)) {
      preload(app, LanguageSwitch.spokenLanguage(null));
    }
  }

  /** Loads the emoji, and the names for {@code language}, on the preload thread. */
  private static void preload(Context app, Locale language) {
    preloader.execute(
        () -> {
          try {
            if (!mode.equals(MODE_ENGINE)) {
              namesFor(app, language);
            }
          } catch (Throwable e) {
            LogUtils.e(TAG, "Can't load emoji names: %s", e);
          }
        });
  }

  /** Lets go of the loaded emoji and names when the service stops; they outlive it otherwise. */
  public static void release() {
    mode = MODE_ENGINE;
    clear();
  }

  private static void clear() {
    synchronized (loaded) {
      loaded.clear();
    }
    index = null;
  }

  /**
   * Sets how many times an emoji named by Backtalk must repeat in a row to be spoken as a count,
   * such as "3 grinning face", or 0 to name each one.
   */
  public static void setRepeatCount(int count) {
    repeatCount = count;
  }

  /** Returns whether the speech engine reads emoji, so text should keep them. */
  public static boolean engineSpeaksEmoji() {
    return mode.equals(MODE_ENGINE);
  }

  /**
   * Marks speech that stands for other text: an emoji's name, or text with emoji in it. Copying
   * the last spoken phrase puts the original back, so it copies the emoji rather than their names.
   */
  public static final class OriginalText {
    private final String original;

    private OriginalText(String original) {
      this.original = original;
    }
  }

  /**
   * Returns {@code text} with its emoji replaced by their names or left out, as the setting says,
   * or {@code text} itself when nothing changes. Other spans are kept, and each replacement is
   * marked with {@link OriginalText}.
   */
  public static CharSequence process(Context context, CharSequence text) {
    String currentMode = mode;
    if (currentMode.equals(MODE_ENGINE) || TextUtils.isEmpty(text) || !mayHaveEmoji(text)) {
      return text;
    }
    try {
      SpannableStringBuilder result = new SpannableStringBuilder(text);
      // With None, text that is only emoji is still named, so that it isn't silent.
      boolean names = currentMode.equals(MODE_NAMES) || onlyEmoji(context, result);
      boolean changed = speakTtsSpanText(context, result, names);
      changed |= replaceEmoji(context, result, names);
      return changed ? result : text;
    } catch (RuntimeException e) {
      // Speech must never be lost: speak the text as it is.
      LogUtils.e(TAG, "Can't process emoji: %s", e);
      return text;
    }
  }

  /**
   * Returns the end of the emoji that starts at {@code start} in {@code text}, or -1 if none does
   * or the speech engine reads emoji itself.
   */
  public static int emojiEnd(Context context, CharSequence text, int start) {
    if (mode.equals(MODE_ENGINE) || !EmojiNames.mayStartEmoji(text, start)) {
      return -1;
    }
    EmojiNames.@Nullable Match match = index(context).find(text, start);
    return match == null ? -1 : match.end();
  }

  /**
   * Returns the start and end of the emoji that {@code index} is in, or null if it isn't in one
   * or the speech engine reads emoji itself. Some apps, such as Chrome, move by character through
   * the parts of a sequence, such as a skin tone or each person in a family, one at a time.
   */
  public static int @Nullable [] emojiAround(Context context, CharSequence text, int index) {
    if (mode.equals(MODE_ENGINE) || index < 0 || index >= text.length()) {
      return null;
    }
    // Matching from just after a character that can't be part of an emoji finds whole emoji, even
    // in a long run of them, where a flag's halves pair up from the start of the run.
    int start = index;
    while (start > 0 && EmojiNames.mayBePartOfEmoji(text, start - 1)) {
      start--;
    }
    EmojiNames.Index emoji = index(context);
    for (int i = start; i <= index; ) {
      EmojiNames.@Nullable Match match = emoji.mayStartEmoji(text, i) ? emoji.find(text, i) : null;
      if (match != null) {
        if (match.end() > index) {
          return new int[] {i, match.end()};
        }
        i = match.end();
      } else {
        i += Character.charCount(Character.codePointAt(text, i));
      }
    }
    return null;
  }

  /**
   * Returns the end of the emoji that starts at {@code index} when moving by word should stop on
   * emoji, which it does when Backtalk names them, or -1. Words otherwise start with a letter or
   * digit, so an emoji between spaces was skipped.
   */
  public static int emojiWordEnd(CharSequence text, int index) {
    @Nullable Context context = appContext;
    if (!mode.equals(MODE_NAMES) || context == null) {
      return -1;
    }
    return emojiEnd(context, text, index);
  }

  /** Returns whether moving by word stops on emoji, which it does when Backtalk names them. */
  public static boolean stopsOnEmojiWords() {
    return mode.equals(MODE_NAMES);
  }

  /** Returns whether moving by word should stop on emoji in {@code text}. */
  public static boolean hasEmojiWords(@Nullable CharSequence text) {
    return stopsOnEmojiWords() && text != null && mayHaveEmoji(text);
  }

  /**
   * Returns whether {@code text} may hold an emoji, without loading anything: exactly, once the
   * emoji are loaded, and by the ranges emoji are in before that.
   */
  private static boolean mayHaveEmoji(CharSequence text) {
    EmojiNames.@Nullable Index emoji = index;
    return emoji != null ? emoji.mayHaveEmoji(text) : EmojiNames.mayHaveEmoji(text);
  }

  /** Returns {@code text} with the text that speech replaced put back. */
  public static CharSequence restoreOriginalText(CharSequence text) {
    if (!(text instanceof Spanned spanned)) {
      return text;
    }
    OriginalText[] spans = spanned.getSpans(0, text.length(), OriginalText.class);
    if (spans.length == 0) {
      return text;
    }
    SpannableStringBuilder restored = new SpannableStringBuilder(text);
    // Replacements never overlap, so put them back from the last, keeping earlier ones in place.
    Arrays.sort(spans, (a, b) -> restored.getSpanStart(b) - restored.getSpanStart(a));
    for (OriginalText span : spans) {
      int start = restored.getSpanStart(span);
      int end = restored.getSpanEnd(span);
      restored.removeSpan(span);
      if (start >= 0 && end >= start) {
        restored.replace(start, end, span.original);
      }
    }
    return restored;
  }

  /**
   * Replaces the emoji in {@code text} that aren't already replaced, by their names or by a space.
   * Returns whether any were replaced.
   */
  private static boolean replaceEmoji(
      Context context, SpannableStringBuilder text, boolean names) {
    List<Found> matches = new ArrayList<>();
    for (Found match : findAll(context, text)) {
      if (!isReplaced(text, match.start(), match.end())) {
        matches.add(match);
      }
    }
    boolean changed = false;
    int countFrom = names ? repeatCount : 0;
    for (int last = matches.size() - 1; last >= 0; ) {
      // The same emoji repeated right after itself, counted when there are enough of them.
      int first = last;
      while (first > 0
          && matches.get(first - 1).end() == matches.get(first).start()
          && matches.get(first - 1).isSameEmoji(matches.get(first))) {
        first--;
      }
      int repeats = last - first + 1;
      if (countFrom < 2 || repeats < countFrom) {
        first = last;
        repeats = 1;
      }
      int start = matches.get(first).start();
      int end = matches.get(last).end();
      String replacement;
      if (names) {
        @Nullable String name = nameOf(context, matches.get(last));
        if (name == null) {
          // The names couldn't be read, so the emoji is left to the speech engine.
          last = first - 1;
          continue;
        }
        if (repeats > 1) {
          name = context.getString(R.string.character_collapse_template, repeats, name).trim();
        }
        boolean wordBefore = start > 0 && !isSpace(text.charAt(start - 1));
        boolean wordAfter = end < text.length() && !isSpace(text.charAt(end));
        replacement = (wordBefore ? " " : "") + name + (wordAfter ? " " : "");
      } else {
        // A space rather than nothing, so words either side stay apart and the mark has a place.
        replacement = " ";
      }
      replace(text, start, end, replacement);
      changed = true;
      last = first - 1;
    }
    return changed;
  }

  /**
   * Replaces each text {@link TtsSpan} that holds or covers an emoji with its text, its emoji
   * handled too. An engine that reads these spans would say that text anyway; one that doesn't
   * would read the emoji under it. Returns whether any were replaced.
   */
  private static boolean speakTtsSpanText(
      Context context, SpannableStringBuilder text, boolean names) {
    boolean changed = false;
    for (TtsSpan span : text.getSpans(0, text.length(), TtsSpan.class)) {
      if (!TtsSpan.TYPE_TEXT.equals(span.getType())) {
        continue;
      }
      @Nullable String spoken = span.getArgs().getString(TtsSpan.ARG_TEXT);
      int start = text.getSpanStart(span);
      int end = text.getSpanEnd(span);
      if (spoken == null || start < 0 || end <= start) {
        continue;
      }
      SpannableStringBuilder covered = new SpannableStringBuilder(text, start, end);
      if (!EmojiNames.mayHaveEmoji(spoken) && !EmojiNames.mayHaveEmoji(covered)) {
        continue;
      }
      String replacement;
      if (onlyEmoji(context, covered)) {
        if (names) {
          // Such as repeated emoji counted as symbols: the emoji count setting counts them.
          text.removeSpan(span);
          changed = true;
          continue;
        }
        // Without their emoji, counted repeated emoji, "4 grinning face", would say "4".
        replacement = " ";
      } else {
        SpannableStringBuilder spokenText = new SpannableStringBuilder(spoken);
        LocaleSpan[] languages = covered.getSpans(0, covered.length(), LocaleSpan.class);
        if (languages.length > 0) {
          spokenText.setSpan(
              languages[0], 0, spokenText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        replaceEmoji(context, spokenText, names);
        replacement = spokenText.length() > 0 ? spokenText.toString() : " ";
      }
      text.removeSpan(span);
      replace(text, start, end, replacement);
      changed = true;
    }
    return changed;
  }

  /** Returns whether part of {@code start} to {@code end} was replaced already. */
  private static boolean isReplaced(Spanned text, int start, int end) {
    for (OriginalText span : text.getSpans(start, end, OriginalText.class)) {
      // getSpans also gives spans that only touch the range.
      if (text.getSpanStart(span) < end && text.getSpanEnd(span) > start) {
        return true;
      }
    }
    return false;
  }

  /** Replaces part of {@code text}, marking the replacement with the text it replaced. */
  private static void replace(
      SpannableStringBuilder text, int start, int end, String replacement) {
    String original = text.subSequence(start, end).toString();
    text.replace(start, end, replacement);
    text.setSpan(
        new OriginalText(original),
        start,
        start + replacement.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  /** An emoji found in text, and the language the text is spoken in there. */
  private record Found(int start, int end, int emoji, Locale language) {
    boolean isSameEmoji(Found other) {
      return emoji == other.emoji && language.equals(other.language);
    }
  }

  private static List<Found> findAll(Context context, Spanned text) {
    EmojiNames.Index emoji = index(context);
    List<Found> all = new ArrayList<>();
    // Text in one language is matched as one piece.
    for (LanguageSwitch.Run run : LanguageSwitch.runs(text)) {
      Locale language = LanguageSwitch.spokenLanguage(run.locale());
      for (EmojiNames.Match m : emoji.findAll(text.subSequence(run.start(), run.end()))) {
        all.add(new Found(run.start() + m.start(), run.start() + m.end(), m.emoji(), language));
      }
    }
    return all;
  }

  /** Returns the name of an emoji found in text, or null if the names couldn't be read. */
  private static @Nullable String nameOf(Context context, Found found) {
    return namesFor(context, found.language()).name(found.emoji());
  }

  /** Returns whether {@code text} holds nothing but emoji and spaces. */
  private static boolean onlyEmoji(Context context, Spanned text) {
    int i = 0;
    for (Found match : findAll(context, text)) {
      for (; i < match.start(); i++) {
        if (!isSpace(text.charAt(i))) {
          return false;
        }
      }
      i = match.end();
    }
    for (; i < text.length(); i++) {
      if (!isSpace(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isSpace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }

  /** Returns the emoji, loading them the first time. */
  private static EmojiNames.Index index(Context context) {
    EmojiNames.@Nullable Index emoji = index;
    if (emoji != null) {
      return emoji;
    }
    synchronized (indexLock) {
      emoji = index;
      if (emoji == null) {
        emoji = readIndex(context.getAssets());
        index = emoji;
      }
      return emoji;
    }
  }

  /** Reads the emoji. If they can't be read, no emoji are found, and the engine reads them. */
  private static EmojiNames.Index readIndex(AssetManager assets) {
    try (BufferedReader reader = open(assets, EmojiNames.INDEX_FILE)) {
      return EmojiNames.Index.read(reader);
    } catch (Exception e) {
      LogUtils.e(TAG, "Can't read the emoji: %s", e);
      return new EmojiNames.Index();
    }
  }

  /**
   * Returns the names for {@code language}, or English names if there are none for it. Loading
   * them waits only for a load of the same names already under way, not for other languages.
   */
  private static EmojiNames namesFor(Context context, Locale language) {
    AssetManager assets = context.getAssets();
    EmojiNames.Index emoji = index(context);
    FutureTask<EmojiNames> task;
    boolean loadHere = false;
    Set<String> available;
    String file;
    synchronized (loaded) {
      available = availableFiles(assets);
      file = "en";
      for (String candidate : EmojiNames.filesFor(language)) {
        if (available.contains(candidate)) {
          file = candidate;
          break;
        }
      }
      @Nullable EmojiNames names = loaded.get(file);
      if (names != null) {
        return names;
      }
      task = loading.get(file);
      if (task == null) {
        String toLoad = file;
        task = new FutureTask<>(() -> load(assets, toLoad, available, emoji));
        loading.put(file, task);
        loadHere = true;
      }
    }
    if (loadHere) {
      task.run();
    }
    EmojiNames names;
    try {
      names = task.get();
    } catch (ExecutionException | InterruptedException e) {
      LogUtils.e(TAG, "Can't load emoji names %s: %s", file, e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      names = new EmojiNames(emoji);
    }
    if (loadHere) {
      synchronized (loaded) {
        loading.remove(file);
        // Kept unless the emoji were let go of while loading, as when the mode changed.
        if (index == emoji) {
          loaded.put(file, names);
        }
      }
    }
    return names;
  }

  /** Returns the names files in the assets, by language. Called with {@link #loaded} held. */
  private static Set<String> availableFiles(AssetManager assets) {
    if (files == null) {
      Set<String> found = new HashSet<>();
      try {
        @Nullable String[] list = assets.list(EmojiNames.ASSET_FOLDER);
        if (list != null) {
          found.addAll(Arrays.asList(list));
        }
      } catch (Exception e) {
        LogUtils.e(TAG, "Can't list emoji names: %s", e);
      }
      found.remove(EmojiNames.INDEX_FILE);
      found.remove(EmojiNames.LICENSE_FILE);
      files = found;
    }
    return files;
  }

  /**
   * Reads the names in {@code file}, after the names of the languages it builds on. A file that
   * can't be read gives no names, so its emoji are left as they are rather than tried again.
   */
  private static EmojiNames load(
      AssetManager assets, String file, Set<String> available, EmojiNames.Index emoji) {
    // Read the chain of parents, then apply the most general first.
    String[] chain = new String[MAX_PARENTS + 1];
    int length = 0;
    @Nullable String next = file;
    while (next != null && length < chain.length && available.contains(next)) {
      chain[length++] = next;
      next = readParent(assets, next);
    }
    EmojiNames names = new EmojiNames(emoji);
    for (int i = length - 1; i >= 0; i--) {
      read(assets, chain[i], names);
    }
    if (names.isEmpty()) {
      LogUtils.w(TAG, "No emoji names for %s", file);
    }
    return names;
  }

  private static @Nullable String readParent(AssetManager assets, String file) {
    try (BufferedReader reader = open(assets, file)) {
      @Nullable String first = reader.readLine();
      return first != null && first.startsWith(EmojiNames.PARENT)
          ? first.substring(EmojiNames.PARENT.length()).trim()
          : null;
    } catch (Exception e) {
      LogUtils.e(TAG, "Can't read emoji names %s: %s", file, e);
      return null;
    }
  }

  private static void read(AssetManager assets, String file, EmojiNames names) {
    try (BufferedReader reader = open(assets, file)) {
      names.read(reader);
    } catch (Exception e) {
      LogUtils.e(TAG, "Can't read emoji names %s: %s", file, e);
    }
  }

  /** Opens a file of the emoji or names, which the build has unzipped: see EmojiNames.EXTENSION. */
  private static BufferedReader open(AssetManager assets, String file) throws IOException {
    return new BufferedReader(
        new InputStreamReader(
            assets.open(EmojiNames.ASSET_FOLDER + "/" + file), StandardCharsets.UTF_8));
  }
}
