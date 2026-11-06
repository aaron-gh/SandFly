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

package com.google.android.accessibility.talkback.speech;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import java.util.Map;
import java.util.function.IntFunction;

/** The names of voice profiles, with Backtalk default's and numbered names from resources. */
public final class VoiceProfileNames {

  private VoiceProfileNames() {}

  /** Returns Backtalk default's name, which no profile can have. */
  public static String defaultName(Context context) {
    return context.getString(R.string.voice_profile_default);
  }

  /**
   * Returns each profile's name by ID, in order, with a numbered name for any profile a damaged
   * setting left without a name of its own. See {@link VoiceProfiles#names}.
   */
  public static Map<String, String> names(Context context, SharedPreferences prefs) {
    return VoiceProfiles.names(prefs, defaultName(context), numberedName(context));
  }

  /** Returns the name of profile {@code id}, or Backtalk default's for an empty ID. */
  public static String nameOf(Context context, SharedPreferences prefs, String id) {
    if (id.isEmpty()) {
      return defaultName(context);
    }
    String name = names(context, prefs).get(id);
    return (name == null) ? "" : name;
  }

  /** Saves a name of its own for any profile a damaged setting left without one. */
  public static void saveNames(Context context, SharedPreferences prefs) {
    VoiceProfiles.saveNames(prefs, defaultName(context), numberedName(context));
  }

  /** Returns the first numbered name, such as "Voice profile 1", that no profile has. */
  public static String unusedName(Context context, SharedPreferences prefs) {
    return VoiceProfiles.unusedName(prefs, defaultName(context), numberedName(context));
  }

  /** How a command that changes to a voice profile starts, before the profile's ID. */
  private static final String COMMAND_PREFIX = "VOICE_PROFILE:";

  /** Returns the command that changes to profile {@code id}, or to Backtalk default's for "". */
  public static String commandOf(String id) {
    return COMMAND_PREFIX + id;
  }

  /** Returns whether {@code value} is a command that changes to a voice profile. */
  public static boolean isCommand(@Nullable String value) {
    return value != null && value.startsWith(COMMAND_PREFIX);
  }

  /** Returns the ID of the profile that the command {@code value} changes to. */
  public static String idOfCommand(String value) {
    return value.substring(COMMAND_PREFIX.length());
  }

  /**
   * Returns the name of the command {@code value}, or null when it isn't one of these commands or
   * its profile was deleted.
   */
  public static @Nullable String commandLabel(
      Context context, SharedPreferences prefs, @Nullable String value) {
    if (!isCommand(value)) {
      return null;
    }
    String id = idOfCommand(value);
    if (!id.isEmpty() && !VoiceProfiles.ids(prefs).contains(id)) {
      return null;
    }
    return context.getString(R.string.shortcut_use_voice_profile, nameOf(context, prefs, id));
  }

  /** Gives "Voice profile 1", "Voice profile 2" and so on. */
  private static IntFunction<String> numberedName(Context context) {
    return number -> context.getString(R.string.voice_profile_new_name, number);
  }
}
