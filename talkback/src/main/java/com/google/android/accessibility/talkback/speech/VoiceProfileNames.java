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

  /** Gives "Voice profile 1", "Voice profile 2" and so on. */
  private static IntFunction<String> numberedName(Context context) {
    return number -> context.getString(R.string.voice_profile_new_name, number);
  }
}
