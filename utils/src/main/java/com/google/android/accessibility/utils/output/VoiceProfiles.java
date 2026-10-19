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

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Voice profiles: speech settings the user can switch to from the reading controls, each with its
 * own engine, language, voice, volume, rate, pitch, and sentence sending. Backtalk's default is no
 * profile, and uses the text-to-speech settings. While a profile is in use, all speech uses its
 * engine and voice, so speech never changes language.
 *
 * <p>Each profile's settings are separate preferences, named by {@link #key}, so that settings
 * screens can bind to them directly.
 */
public final class VoiceProfiles {

  /** The ID of the profile in use, or empty for Backtalk's default. */
  public static final String PREF_ACTIVE = "pref_voice_profile";

  /** The IDs of the profiles, in order, separated by commas. */
  public static final String PREF_IDS = "pref_voice_profile_ids";

  /** The ID the next new profile gets, so that a removed profile's ID is never reused. */
  private static final String PREF_NEXT_ID = "pref_voice_profile_next_id";

  /** Starts the name of every voice profile preference. */
  public static final String PREF_PREFIX = "pref_voice_profile";

  /** A setting of one profile, named by {@link #key}. */
  private static final Pattern PROFILE_KEY =
      Pattern.compile(Pattern.quote(PREF_PREFIX) + "_[0-9]+_[a-z]+");

  public static final String NAME = "name";
  public static final String ENGINE = "engine";
  public static final String LANGUAGE = "language";
  public static final String VOICE = "voice";
  public static final String VOLUME = "volume";
  public static final String RATE = "rate";
  public static final String PITCH = "pitch";
  public static final String PHRASES = "phrases";

  public static final String DEFAULT_RATE = "1.0";
  public static final String DEFAULT_PITCH = "1.0";
  public static final String DEFAULT_VOLUME = "100";

  /** The longest name a profile can have. */
  public static final int MAX_NAME_LENGTH = 100;

  // The ranges the settings screens and gestures allow, as in SpeechRateAndPitchActor.
  private static final float MIN_RATE = 0.1f;
  private static final float MAX_RATE = 6.0f;
  private static final float MIN_PITCH = 0.2f;
  private static final float MAX_PITCH = 2.0f;

  /**
   * A profile's settings.
   *
   * @param engine the speech engine's package
   * @param language a language tag, or empty for the engine's default
   * @param voice the voice's name, or empty for the language's default voice
   * @param volume the speech volume, from 0 to 100
   */
  public record VoiceProfile(
      String id,
      String name,
      String engine,
      String language,
      String voice,
      int volume,
      float rate,
      float pitch,
      boolean phrases) {

    /** Whether this profile has the same engine, language and voice as {@code other}. */
    boolean sameVoiceAs(@Nullable VoiceProfile other) {
      return other != null
          && engine.equals(other.engine)
          && language.equals(other.language)
          && voice.equals(other.voice);
    }
  }

  /** The profile in use, or null for Backtalk's default. */
  private static volatile @Nullable VoiceProfile active;

  private VoiceProfiles() {}

  /** Returns the profile in use, or null for Backtalk's default. */
  public static @Nullable VoiceProfile active() {
    return active;
  }

  /** Whether a profile other than Backtalk's default is in use. */
  public static boolean isProfileActive() {
    return active != null;
  }

  static void setActive(@Nullable VoiceProfile profile) {
    active = profile;
  }

  /** Returns the name of the preference holding {@code field} of profile {@code id}. */
  public static String key(String id, String field) {
    return PREF_PREFIX + "_" + id + "_" + field;
  }

  /**
   * Whether {@code key} is one of the voice profile preferences: the profile in use, the list of
   * profiles, or one of a profile's settings. Other settings that start the same way, such as the
   * Backtalk menu's voice profile item, are not.
   */
  public static boolean isProfileKey(@Nullable String key) {
    return key != null
        && (key.equals(PREF_ACTIVE)
            || key.equals(PREF_IDS)
            || PROFILE_KEY.matcher(key).matches());
  }

  /**
   * Returns the IDs of the profiles, in order. A damaged setting can list an ID twice, which counts
   * once, in its first place.
   */
  public static List<String> ids(SharedPreferences prefs) {
    List<String> ids = new ArrayList<>();
    for (String id : getString(prefs, PREF_IDS, "").split(",")) {
      if (!id.isEmpty() && !ids.contains(id)) {
        ids.add(id);
      }
    }
    return ids;
  }

  /** Returns the ID of the profile in use, or empty for Backtalk's default. */
  public static String activeId(SharedPreferences prefs) {
    String id = getString(prefs, PREF_ACTIVE, "");
    return ids(prefs).contains(id) ? id : "";
  }

  /**
   * Returns the ID of the profile before or after the one in use, with Backtalk's default (empty)
   * first, wrapping around at either end.
   */
  public static String adjacentId(SharedPreferences prefs, boolean isNext) {
    List<String> ids = new ArrayList<>();
    ids.add("");
    ids.addAll(ids(prefs));
    int currentIndex = ids.indexOf(activeId(prefs));
    return ids.get(Math.floorMod(currentIndex + (isNext ? 1 : -1), ids.size()));
  }

  /** Returns the profile in use, or null for Backtalk's default. */
  public static @Nullable VoiceProfile readActive(SharedPreferences prefs) {
    String id = activeId(prefs);
    return id.isEmpty() ? null : read(prefs, id);
  }

  /**
   * Returns profile {@code id}. The service reads profiles as it starts, so a damaged setting, such
   * as one restored from a backup, gives its default value rather than stopping the screen reader,
   * and speech settings are kept to the ranges that can be set.
   */
  public static VoiceProfile read(SharedPreferences prefs, String id) {
    String name = getString(prefs, key(id, NAME), "");
    return new VoiceProfile(
        id,
        name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name,
        getString(prefs, key(id, ENGINE), ""),
        getString(prefs, key(id, LANGUAGE), ""),
        getString(prefs, key(id, VOICE), ""),
        Math.max(
            0, Math.min(100, parseInt(getString(prefs, key(id, VOLUME), DEFAULT_VOLUME), 100))),
        clamp(parseFloat(getString(prefs, key(id, RATE), DEFAULT_RATE), 1f), MIN_RATE, MAX_RATE),
        clamp(
            parseFloat(getString(prefs, key(id, PITCH), DEFAULT_PITCH), 1f), MIN_PITCH, MAX_PITCH),
        getBoolean(prefs, key(id, PHRASES), false));
  }

  /**
   * Adds a profile called {@code name}, which must not be blank or another profile's (see {@link
   * #isNameTaken}), speaking with {@code engine} and otherwise like Backtalk's default, and returns
   * its ID.
   */
  public static String create(
      SharedPreferences prefs,
      String name,
      String engine,
      String volume,
      String rate,
      String pitch,
      boolean phrases) {
    List<String> ids = ids(prefs);
    int next = Math.max(1, parseInt(getString(prefs, PREF_NEXT_ID, "1"), 1));
    for (String id : ids) {
      next = Math.max(next, parseInt(id, 0) + 1);
    }
    String id = Integer.toString(next);
    ids.add(id);
    prefs
        .edit()
        .putString(PREF_NEXT_ID, Integer.toString(next + 1))
        .putString(key(id, NAME), name)
        .putString(key(id, ENGINE), engine)
        .putString(key(id, LANGUAGE), "")
        .putString(key(id, VOICE), "")
        .putString(key(id, VOLUME), volume)
        .putString(key(id, RATE), rate)
        .putString(key(id, PITCH), pitch)
        .putBoolean(key(id, PHRASES), phrases)
        .putString(PREF_IDS, String.join(",", ids))
        .apply();
    return id;
  }

  /**
   * Saves the order of the profiles. IDs that aren't profiles are left out, and profiles missing
   * from {@code order} keep their place after the others.
   */
  public static void setOrder(SharedPreferences prefs, List<String> order) {
    List<String> ids = ids(prefs);
    List<String> ordered = new ArrayList<>();
    for (String id : order) {
      if (ids.contains(id) && !ordered.contains(id)) {
        ordered.add(id);
      }
    }
    for (String id : ids) {
      if (!ordered.contains(id)) {
        ordered.add(id);
      }
    }
    prefs.edit().putString(PREF_IDS, String.join(",", ordered)).apply();
  }

  /**
   * Returns each profile's name by ID, in order. Names can't be saved blank or the same as another
   * profile's, but a damaged setting, such as one restored from a backup, can leave them so. A
   * profile with a blank name, or the name of a profile above it or of Backtalk default, gets the
   * first numbered name, such as "Voice profile 2", that no other profile has, as a new profile
   * would. {@link #saveNames} saves those names, so that they stay the same from then on.
   *
   * @param defaultName Backtalk default's name, which no profile can have
   * @param numberedName gives the numbered name for a number
   */
  public static Map<String, String> names(
      SharedPreferences prefs, String defaultName, IntFunction<String> numberedName) {
    Map<String, String> names = new LinkedHashMap<>();
    Set<String> taken = new HashSet<>();
    taken.add(nameKey(defaultName));
    List<String> unnamed = new ArrayList<>();
    for (String id : ids(prefs)) {
      String name = read(prefs, id).name();
      String nameKey = nameKey(name);
      if (nameKey.isEmpty() || taken.contains(nameKey)) {
        unnamed.add(id);
        names.put(id, "");
      } else {
        names.put(id, name);
        taken.add(nameKey);
      }
    }
    for (String id : unnamed) {
      String name = unusedName(taken, numberedName);
      names.put(id, name);
      taken.add(nameKey(name));
    }
    return names;
  }

  /**
   * Saves the names {@link #names} gives profiles whose saved names are blank or another's, if
   * there are any.
   */
  public static void saveNames(
      SharedPreferences prefs, String defaultName, IntFunction<String> numberedName) {
    SharedPreferences.Editor editor = null;
    for (Map.Entry<String, String> entry : names(prefs, defaultName, numberedName).entrySet()) {
      String id = entry.getKey();
      if (!entry.getValue().equals(read(prefs, id).name())) {
        if (editor == null) {
          editor = prefs.edit();
        }
        editor.putString(key(id, NAME), entry.getValue());
      }
    }
    if (editor != null) {
      editor.apply();
    }
  }

  /** Returns the first numbered name, such as "Voice profile 1", that no profile has. */
  public static String unusedName(
      SharedPreferences prefs, String defaultName, IntFunction<String> numberedName) {
    Set<String> taken = new HashSet<>();
    taken.add(nameKey(defaultName));
    for (String name : names(prefs, defaultName, numberedName).values()) {
      taken.add(nameKey(name));
    }
    return unusedName(taken, numberedName);
  }

  private static String unusedName(Set<String> taken, IntFunction<String> numberedName) {
    for (int number = 1; ; number++) {
      String name = numberedName.apply(number);
      if (!taken.contains(nameKey(name))) {
        return name;
      }
    }
  }

  /**
   * Whether a profile other than {@code id} is called {@code name}, or it is Backtalk default's
   * name. Use an empty {@code id} for a new profile.
   */
  public static boolean isNameTaken(
      SharedPreferences prefs, String id, String name, String defaultName) {
    String nameKey = nameKey(name);
    if (nameKey.equals(nameKey(defaultName))) {
      return true;
    }
    for (String other : ids(prefs)) {
      if (!other.equals(id) && nameKey(read(prefs, other).name()).equals(nameKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Names compare without case and spaces at either end, since names that differ only in those
   * sound the same when spoken.
   */
  private static String nameKey(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Renames profile {@code id}, unless {@code name} is blank or another profile's: every profile
   * has a name of its own.
   *
   * @return whether the profile was renamed
   */
  public static boolean rename(
      SharedPreferences prefs, String id, String name, String defaultName) {
    String trimmed = name.trim();
    if (trimmed.length() > MAX_NAME_LENGTH) {
      trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
    }
    if (trimmed.isEmpty() || isNameTaken(prefs, id, trimmed, defaultName)) {
      return false;
    }
    prefs.edit().putString(key(id, NAME), trimmed).apply();
    return true;
  }

  /**
   * Removes profile {@code id}, going back to Backtalk's default if it is in use. The list of
   * profiles is saved without any duplicates a damaged setting left in it.
   */
  public static void delete(SharedPreferences prefs, String id) {
    List<String> ids = ids(prefs);
    ids.remove(id);
    SharedPreferences.Editor editor = prefs.edit();
    for (String field :
        new String[] {NAME, ENGINE, LANGUAGE, VOICE, VOLUME, RATE, PITCH, PHRASES}) {
      editor.remove(key(id, field));
    }
    if (id.equals(getString(prefs, PREF_ACTIVE, ""))) {
      editor.putString(PREF_ACTIVE, "");
    }
    editor.putString(PREF_IDS, String.join(",", ids)).apply();
  }

  private static String getString(SharedPreferences prefs, String key, String fallback) {
    try {
      String value = prefs.getString(key, fallback);
      return value == null ? fallback : value;
    } catch (ClassCastException e) {
      return fallback;
    }
  }

  /**
   * Gives the Voice profile reading control a setting of its own, once. It shared a setting with
   * the Speech engine reading control, which a user may have turned on before voice profiles
   * existed. The setting can't say which control a user meant, so anyone with a voice profile who
   * has it on gets Voice profile on as well and keeps Speech engine on, and has Voice profile
   * still selected if the shared control was. Anyone with no profile keeps Speech engine only.
   *
   * @param doneKey the setting that says this has run
   * @param oldKey the setting that Voice profile and Speech engine shared
   * @param newKey the new setting of Voice profile
   * @param selectedKey the setting of the selected reading control
   * @param oldValue the selected reading control's value that meant Voice profile under {@code
   *     oldKey}
   * @param newValue the selected reading control's value of Voice profile now
   */
  public static void moveReadingControl(
      SharedPreferences prefs,
      String doneKey,
      String oldKey,
      String newKey,
      String selectedKey,
      String oldValue,
      String newValue) {
    if (prefs.contains(doneKey)) {
      return;
    }
    SharedPreferences.Editor editor = prefs.edit().putBoolean(doneKey, true);
    if (!ids(prefs).isEmpty() && getBoolean(prefs, oldKey, false)) {
      editor.putBoolean(newKey, true);
      if (oldValue.equals(getString(prefs, selectedKey, ""))) {
        editor.putString(selectedKey, newValue);
      }
    }
    editor.apply();
  }

  private static boolean getBoolean(SharedPreferences prefs, String key, boolean fallback) {
    try {
      return prefs.getBoolean(key, fallback);
    } catch (ClassCastException e) {
      return fallback;
    }
  }

  /** Returns {@code value} within {@code min} and {@code max}, or 1 if it isn't a number. */
  private static float clamp(float value, float min, float max) {
    return Float.isNaN(value) ? 1f : Math.max(min, Math.min(max, value));
  }

  private static int parseInt(@Nullable String value, int fallback) {
    try {
      return value == null ? fallback : Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static float parseFloat(@Nullable String value, float fallback) {
    try {
      return value == null ? fallback : Float.parseFloat(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
