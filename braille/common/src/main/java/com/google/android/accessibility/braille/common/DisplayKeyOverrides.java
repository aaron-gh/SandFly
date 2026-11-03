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

package com.google.android.accessibility.braille.common;

import androidx.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The braille display keys that the user assigned to commands other than their own.
 *
 * <p>A key is what a braille display reports: either a display command, which stands for every
 * key that the display's key table binds to it ({@code "c<command>"}), or Space pressed with dots
 * that the key table binds to nothing ({@code "s<dots>"}). A target is a display command ({@code
 * "c<command>"}), a screen reader command ({@code "a:<value>"}) or {@link #NONE}.
 *
 * <p>Without an override, a display command key runs its own command and a Space chord runs
 * nothing. Keys moved away from their command are kept as {@link #NONE}.
 */
public final class DisplayKeyOverrides {
  /** Target of a key that runs nothing. */
  public static final String NONE = "-";

  private static final String VERSION = "v1";
  private static final char ENTRY_SEPARATOR = ';';
  private static final char KEY_SEPARATOR = '=';
  private static final String COMMAND_PREFIX = "c";
  private static final String CHORD_PREFIX = "s";
  private static final String SCREEN_READER_PREFIX = "a:";

  /** Longest saved text that is read. */
  static final int MAX_LENGTH = 65536;

  /** Most overrides kept: every display command and every Space chord fit. */
  static final int MAX_ENTRIES = 512;

  /** Longest screen reader command value. */
  static final int MAX_VALUE_LENGTH = 512;

  private static final int MAX_COMMAND = 9999;
  private static final int MAX_DOTS = 0xFF;

  private final LinkedHashMap<String, String> overrides = new LinkedHashMap<>();

  /** Returns the key of a display command. */
  public static String commandKey(int command) {
    return COMMAND_PREFIX + command;
  }

  /** Returns the key of Space pressed with {@code dots}, a bit mask with dot 1 as bit 0. */
  public static String chordKey(int dots) {
    return CHORD_PREFIX + dots;
  }

  /** Returns the target that runs a display command. */
  public static String commandTarget(int command) {
    return COMMAND_PREFIX + command;
  }

  /** Returns the target that runs the screen reader command saved as {@code value}. */
  public static String screenReaderTarget(String value) {
    return SCREEN_READER_PREFIX + value;
  }

  /** Returns the display command of a command key or target, or -1. */
  public static int commandOf(@Nullable String keyOrTarget) {
    return number(keyOrTarget, COMMAND_PREFIX, MAX_COMMAND, 0);
  }

  /** Returns the dots of a Space chord key, or 0. */
  public static int chordDotsOf(@Nullable String key) {
    int dots = number(key, CHORD_PREFIX, MAX_DOTS, 1);
    return dots < 0 ? 0 : dots;
  }

  /** Returns the screen reader command value of a target, or null. */
  @Nullable
  public static String screenReaderValueOf(@Nullable String target) {
    if (target == null
        || !target.startsWith(SCREEN_READER_PREFIX)
        || target.length() == SCREEN_READER_PREFIX.length()
        || target.length() > SCREEN_READER_PREFIX.length() + MAX_VALUE_LENGTH) {
      return null;
    }
    return target.substring(SCREEN_READER_PREFIX.length());
  }

  /** Returns whether {@code key} is a valid key. */
  public static boolean isKey(@Nullable String key) {
    return commandOf(key) >= 0 || chordDotsOf(key) > 0;
  }

  /** Returns whether {@code target} is a valid target. */
  public static boolean isTarget(@Nullable String target) {
    return NONE.equals(target) || commandOf(target) >= 0 || screenReaderValueOf(target) != null;
  }

  /** Returns what {@code key} runs when nothing overrides it. */
  public static String defaultTarget(String key) {
    int command = commandOf(key);
    return command >= 0 ? commandTarget(command) : NONE;
  }

  /** Reads the overrides saved by {@link #serialize}, skipping malformed entries. */
  public static DisplayKeyOverrides parse(@Nullable String saved) {
    DisplayKeyOverrides result = new DisplayKeyOverrides();
    if (saved == null || saved.length() > MAX_LENGTH) {
      return result;
    }
    String[] entries = saved.split(String.valueOf(ENTRY_SEPARATOR));
    if (entries.length == 0 || !VERSION.equals(entries[0])) {
      return result;
    }
    for (int i = 1; i < entries.length && result.overrides.size() < MAX_ENTRIES; i++) {
      String entry = entries[i];
      int separator = entry.indexOf(KEY_SEPARATOR);
      if (separator <= 0) {
        continue;
      }
      String key = entry.substring(0, separator);
      String target = decode(entry.substring(separator + 1));
      if (isKey(key) && isTarget(target) && !result.overrides.containsKey(key)) {
        result.set(key, target);
      }
    }
    return result;
  }

  /** Returns the text to save, which {@link #parse} reads back. */
  public String serialize() {
    StringBuilder builder = new StringBuilder(VERSION);
    for (Map.Entry<String, String> entry : overrides.entrySet()) {
      builder
          .append(ENTRY_SEPARATOR)
          .append(entry.getKey())
          .append(KEY_SEPARATOR)
          .append(encode(entry.getValue()));
    }
    return builder.toString();
  }

  /** Returns whether no key is overridden. */
  public boolean isEmpty() {
    return overrides.isEmpty();
  }

  /** Returns the target that overrides {@code key}, or null when the key keeps its default. */
  @Nullable
  public String get(String key) {
    return overrides.get(key);
  }

  /** Returns what {@code key} runs. */
  public String resolve(String key) {
    String target = overrides.get(key);
    return target != null ? target : defaultTarget(key);
  }

  /**
   * Returns the keys that run {@code target}: its own key unless that key is overridden, then the
   * keys assigned to it.
   */
  public List<String> keysFor(String target) {
    List<String> keys = new ArrayList<>();
    int command = commandOf(target);
    if (command >= 0 && !overrides.containsKey(commandKey(command))) {
      keys.add(commandKey(command));
    }
    for (Map.Entry<String, String> entry : overrides.entrySet()) {
      if (entry.getValue().equals(target)) {
        keys.add(entry.getKey());
      }
    }
    return keys;
  }

  /** Returns whether the keys of {@code target} differ from its default keys. */
  public boolean isChanged(String target) {
    if (overrides.containsValue(target)) {
      return true;
    }
    int command = commandOf(target);
    return command >= 0 && overrides.containsKey(commandKey(command));
  }

  /**
   * Makes {@code key} the only key that runs {@code target}. The target's other keys run nothing;
   * whatever {@code key} ran before loses it.
   */
  public void assign(String target, String key) {
    if (!isKey(key) || !isTarget(target) || NONE.equals(target)) {
      return;
    }
    for (String oldKey : keysFor(target)) {
      if (!oldKey.equals(key)) {
        set(oldKey, NONE);
      }
    }
    set(key, target);
  }

  /**
   * Gives {@code target} its default keys back: its own key runs it again, and keys assigned to it
   * run their own commands again.
   */
  public void reset(String target) {
    Iterator<Map.Entry<String, String>> iterator = overrides.entrySet().iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getValue().equals(target)) {
        iterator.remove();
      }
    }
    int command = commandOf(target);
    if (command >= 0) {
      overrides.remove(commandKey(command));
    }
  }

  /**
   * Returns the target that {@link #reset} takes a key from: the target that the own key of {@code
   * target} was assigned to, or null when resetting takes no key from another target.
   */
  @Nullable
  public String targetLosingKeyOnReset(String target) {
    int command = commandOf(target);
    if (command < 0) {
      return null;
    }
    String current = overrides.get(commandKey(command));
    return current == null || NONE.equals(current) || current.equals(target) ? null : current;
  }

  /** Removes every override. */
  public void clear() {
    overrides.clear();
  }

  private void set(String key, String target) {
    if (target.equals(defaultTarget(key))) {
      overrides.remove(key);
    } else if (overrides.containsKey(key) || overrides.size() < MAX_ENTRIES) {
      overrides.put(key, target);
    }
  }

  private static int number(@Nullable String text, String prefix, int max, int min) {
    if (text == null || !text.startsWith(prefix)) {
      return -1;
    }
    int length = text.length() - prefix.length();
    if (length < 1 || length > 4) {
      return -1;
    }
    int value = 0;
    for (int i = prefix.length(); i < text.length(); i++) {
      char c = text.charAt(i);
      if (c < '0' || c > '9') {
        return -1;
      }
      value = value * 10 + (c - '0');
    }
    if (length > 1 && text.charAt(prefix.length()) == '0') {
      return -1;
    }
    return value >= min && value <= max ? value : -1;
  }

  private static String encode(String text) {
    try {
      return URLEncoder.encode(text, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  @Nullable
  private static String decode(String text) {
    try {
      return URLDecoder.decode(text, "UTF-8");
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      return null;
    }
  }
}
