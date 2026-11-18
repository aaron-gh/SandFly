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

package com.google.android.accessibility.braille.interfaces;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Braille keyboard settings > Custom braille characters: text typed with braille cells of the
 * user's choice, in one braille table. An entry takes precedence over the table's own rules, when
 * typing and when braille is shown. The braille translator reads the entries of a table as a
 * liblouis table placed before the table's own.
 */
public final class CustomBrailleCharacters {

  /** The preference file of the braille keyboard, which holds the entries. */
  public static final String PREFS_FILE = "braille_keyboard";

  public static final String KEY = "pref_braille_custom_characters";

  /** The most entries kept. */
  public static final int MAX_ENTRIES = 500;

  /** The longest text of an entry. */
  public static final int MAX_TEXT = 50;

  /** The most cells of an entry. */
  public static final int MAX_CELLS = 20;

  /** The name of a braille table, as the braille keyboard's languages are named in code. */
  private static final Pattern TABLE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

  /** An entry: [text] is typed with [cells] in the braille table [table]. */
  public static final class Entry {
    public final String text;

    /** The cells, as dot numbers with cells separated by hyphens, such as "456-2356". */
    public final String cells;

    /** The braille table, such as "POLISH". */
    public final String table;

    public Entry(String text, String cells, String table) {
      this.text = text;
      this.cells = cells;
      this.table = table;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry other = (Entry) o;
      return text.equals(other.text) && cells.equals(other.cells) && table.equals(other.table);
    }

    @Override
    public int hashCode() {
      return Objects.hash(text, cells, table);
    }

    @Override
    public String toString() {
      return text + " " + cells + " " + table;
    }
  }

  private CustomBrailleCharacters() {}

  public static SharedPreferences prefs(Context context) {
    return SharedPreferencesUtils.getSharedPreferences(context, PREFS_FILE);
  }

  public static List<Entry> read(SharedPreferences prefs) {
    return fromJson(prefs.getString(KEY, null));
  }

  public static void write(SharedPreferences prefs, List<Entry> entries) {
    if (entries.isEmpty()) {
      prefs.edit().remove(KEY).apply();
    } else {
      prefs.edit().putString(KEY, toJson(entries)).apply();
    }
  }

  /** The entries in [json], leaving out those that can't be used, at most [MAX_ENTRIES]. */
  public static List<Entry> fromJson(@Nullable String json) {
    if (json == null || json.isEmpty()) {
      return Collections.emptyList();
    }
    List<Entry> entries = new ArrayList<>();
    try {
      JSONArray array = new JSONArray(json);
      for (int i = 0; i < array.length() && entries.size() < MAX_ENTRIES; i++) {
        JSONObject item = array.optJSONObject(i);
        if (item == null) {
          continue;
        }
        Entry entry =
            entry(item.optString("text"), item.optString("cells"), item.optString("table"));
        if (entry != null) {
          entries.add(entry);
        }
      }
    } catch (JSONException e) {
      return Collections.emptyList();
    }
    return entries;
  }

  public static String toJson(List<Entry> entries) {
    JSONArray array = new JSONArray();
    try {
      for (Entry entry : entries) {
        array.put(
            new JSONObject()
                .put("text", entry.text)
                .put("cells", entry.cells)
                .put("table", entry.table));
      }
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    return array.toString();
  }

  /**
   * An entry of [text], [cells] in any form [normalizeCells] reads, and [table], or null if one of
   * them can't be used.
   */
  @Nullable
  public static Entry entry(String text, String cells, String table) {
    String normalized = normalizeCells(cells);
    if (!isValidText(text) || normalized == null || !isValidTable(table)) {
      return null;
    }
    return new Entry(text, normalized, table);
  }

  /** Whether [text] can be an entry's text: 1 to [MAX_TEXT] characters, none of them controls. */
  public static boolean isValidText(@Nullable String text) {
    if (text == null || text.isEmpty() || text.length() > MAX_TEXT) {
      return false;
    }
    for (int i = 0; i < text.length(); i++) {
      if (Character.isISOControl(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isValidTable(@Nullable String table) {
    return table != null && TABLE.matcher(table).matches();
  }

  /**
   * [cells] as dot numbers in order with cells separated by hyphens, such as "456-2356", or null if
   * it isn't 1 to [MAX_CELLS] cells of dots 1 to 8. Cells can be separated by hyphens, spaces or
   * slashes, and the dots of a cell can be in any order.
   */
  @Nullable
  public static String normalizeCells(@Nullable String cells) {
    if (cells == null) {
      return null;
    }
    String[] parts = cells.trim().split("[-\\s/]+", -1);
    if (parts.length == 0 || parts.length > MAX_CELLS) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty() || part.length() > 8) {
        return null;
      }
      boolean[] dots = new boolean[9];
      for (int i = 0; i < part.length(); i++) {
        char c = part.charAt(i);
        if (c < '1' || c > '8' || dots[c - '0']) {
          return null;
        }
        dots[c - '0'] = true;
      }
      if (out.length() > 0) {
        out.append('-');
      }
      for (int dot = 1; dot <= 8; dot++) {
        if (dots[dot]) {
          out.append(dot);
        }
      }
    }
    return out.toString();
  }
}
