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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters;
import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters.Entry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The custom braille characters of each braille table, as liblouis tables. A braille table with
 * custom characters is translated with the table list "custom table,table", so that liblouis reads
 * the custom characters first, and they take precedence over the table's own rules in both
 * directions.
 *
 * <p>The tables are written again, and liblouis's compiled tables are freed, whenever the entries
 * change, such as after an edit, an import, or a settings profile.
 */
public final class CustomCharacterTables {
  private static final String TAG = "CustomCharacterTables";
  private static final String DIR = "braille_custom_characters";

  @Nullable private static File dir;
  @Nullable private static SharedPreferences prefs;

  // Kept here, as SharedPreferences holds its listeners weakly.
  @Nullable private static OnSharedPreferenceChangeListener listener;

  /** The custom table file of each braille table with entries, by the table's code name. */
  private static volatile Map<String, String> files = Collections.emptyMap();

  /** The cells of the entries of each braille table, by the table's code name. */
  private static volatile Map<String, List<BrailleWord>> cells = Collections.emptyMap();

  /** Whether each table list compiled, so a list that doesn't is replaced by its table alone. */
  private static final Map<String, Boolean> compiled = new HashMap<>();

  /** Counts the updates, so that a check of files written before one isn't kept after it. */
  private static int generation;

  private CustomCharacterTables() {}

  /** Reads the entries and follows their changes, from the first translator on. */
  static synchronized void start(Context context) {
    if (listener != null) {
      return;
    }
    Context storage = ContextCompat.createDeviceProtectedStorageContext(context);
    if (storage == null) {
      storage = context;
    }
    dir = new File(storage.getNoBackupFilesDir(), DIR);
    prefs = CustomBrailleCharacters.prefs(context);
    listener =
        (changed, key) -> {
          if (key == null || CustomBrailleCharacters.KEY.equals(key)) {
            update();
          }
        };
    prefs.registerOnSharedPreferenceChangeListener(listener);
    update();
  }

  /**
   * The table list that translates with [table] in the braille table [code]: its custom table
   * first, if it has entries and the list compiles, or [table] alone.
   */
  static String tableList(@Nullable String code, String table) {
    String file = code == null ? null : files.get(code);
    if (file == null) {
      return table;
    }
    String list = file + "," + table;
    Boolean ok;
    int checkedGeneration;
    synchronized (compiled) {
      ok = compiled.get(list);
      checkedGeneration = generation;
    }
    if (ok == null) {
      // Not while holding compiled, which update() takes inside LouisTranslation.LOCK.
      ok = LouisTranslation.checkTable(list);
      if (!ok) {
        Log.w(TAG, "Custom characters of " + code + " left out");
      }
      synchronized (compiled) {
        if (checkedGeneration == generation) {
          compiled.put(list, ok);
        }
      }
    }
    return ok ? list : table;
  }

  /** Whether [word] holds the cells of one of the custom characters of [code]. */
  static boolean containsCustomCells(@Nullable String code, BrailleWord word) {
    List<BrailleWord> list = code == null ? null : cells.get(code);
    if (list == null) {
      return false;
    }
    for (BrailleWord custom : list) {
      if (word.indexOf(custom) >= 0) {
        return true;
      }
    }
    return false;
  }

  /** Whether [word] is the cells of one of the custom characters of [code]. */
  static boolean isCustomCells(@Nullable String code, BrailleWord word) {
    List<BrailleWord> list = code == null ? null : cells.get(code);
    return list != null && list.contains(word);
  }

  private static synchronized void update() {
    if (prefs == null || dir == null) {
      return;
    }
    Map<String, List<Entry>> byTable = new LinkedHashMap<>();
    for (Entry entry : CustomBrailleCharacters.read(prefs)) {
      List<Entry> list = byTable.get(entry.table);
      if (list == null) {
        list = new ArrayList<>();
        byTable.put(entry.table, list);
      }
      list.add(entry);
    }
    if (byTable.isEmpty() && files.isEmpty()) {
      // Nothing written before or now: liblouis is left as it is, and isn't loaded for this.
      return;
    }
    Map<String, String> newFiles = new HashMap<>();
    Map<String, List<BrailleWord>> newCells = new HashMap<>();
    // Translations wait while the files change and the compiled tables are freed.
    synchronized (LouisTranslation.LOCK) {
      File[] old = dir.listFiles();
      if (old != null) {
        for (File file : old) {
          if (!file.delete()) {
            Log.w(TAG, "Not deleted: " + file.getName());
          }
        }
      }
      if (!byTable.isEmpty() && (dir.isDirectory() || dir.mkdirs())) {
        for (Map.Entry<String, List<Entry>> table : byTable.entrySet()) {
          File file = new File(dir, table.getKey() + ".tbl");
          if (write(file, rules(table.getValue()))) {
            newFiles.put(table.getKey(), file.getAbsolutePath());
            List<BrailleWord> words = new ArrayList<>();
            for (Entry entry : table.getValue()) {
              words.add(new BrailleWord(entry.cells));
            }
            newCells.put(table.getKey(), words);
          }
        }
      }
      files = newFiles;
      cells = newCells;
      synchronized (compiled) {
        compiled.clear();
        generation++;
      }
      LouisTranslation.freeTables();
    }
  }

  private static boolean write(File file, String text) {
    try (OutputStream out = new FileOutputStream(file)) {
      out.write(text.getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (IOException e) {
      Log.w(TAG, "Not written: " + file.getName(), e);
      return false;
    }
  }

  /**
   * The liblouis rules of [entries]. liblouis keeps the first definition of a character and uses
   * the first of the rules for the same cells, so these rules, read first, take precedence. A
   * single character other than a letter, digit or space is also defined as punctuation, a math
   * sign or a sign, which back-translation prefers to the table's other rules for the same cells;
   * every entry has an "always" rule, which translation prefers to the table's rules for the
   * character, such as UEB's rule for a closing parenthesis after a word. Every character is
   * written as \xhhhh, so that no text is read as table syntax.
   */
  @VisibleForTesting
  public static String rules(List<Entry> entries) {
    StringBuilder out = new StringBuilder("# SandFly custom braille characters\n");
    for (Entry entry : entries) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < entry.text.length(); i++) {
        text.append(String.format(Locale.ROOT, "\\x%04x", (int) entry.text.charAt(i)));
      }
      String definition = definitionOpcode(entry.text);
      if (definition != null) {
        out.append(definition).append(' ').append(text).append(' ').append(entry.cells).append('\n');
      }
      out.append("always ").append(text).append(' ').append(entry.cells).append('\n');
    }
    return out.toString();
  }

  /** The opcode that defines [text], if it is one character other than a letter, digit or space. */
  @Nullable
  private static String definitionOpcode(String text) {
    if (text.length() != 1) {
      return null;
    }
    char c = text.charAt(0);
    if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || Character.isSpaceChar(c)) {
      return null;
    }
    switch (Character.getType(c)) {
      case Character.START_PUNCTUATION:
      case Character.END_PUNCTUATION:
      case Character.DASH_PUNCTUATION:
      case Character.CONNECTOR_PUNCTUATION:
      case Character.OTHER_PUNCTUATION:
      case Character.INITIAL_QUOTE_PUNCTUATION:
      case Character.FINAL_QUOTE_PUNCTUATION:
        return "punctuation";
      case Character.MATH_SYMBOL:
        return "math";
      default:
        return "sign";
    }
  }
}
