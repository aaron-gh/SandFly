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

package com.google.android.accessibility.braille.brailledisplay.controller.utils;

import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ANNOUNCE_SCROLL_POSITION;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_EDIT_CUSTOM_LABEL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GLOBAL_BACK;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GLOBAL_HOME;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GLOBAL_NOTIFICATIONS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HELP;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_DEL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_ENTER;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_RETURN;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_SHIFT_TAB;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_TAB;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LONG_PRESS_CURRENT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_BOTTOM;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_BOTTOM_OR_KEY_ACTIVATE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_ITEM_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_ITEM_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_PAN_DOWN;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_PAN_UP;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_STATUS_BAR;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_TOP;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_TOP_OR_KEY_ACTIVATE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NEXT_READING_CONTROL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_OPEN_TALKBACK_MENU;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_PREVIOUS_READING_CONTROL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_QUICK_SETTINGS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_REDO_TYPING;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECT_NEXT_CHARACTER;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECT_PREVIOUS_CHARACTER;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECTION_COPY;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECTION_CUT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECTION_PASTE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SELECTION_SELECT_ALL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SHOW_POPUP_MESSAGE_HISTORY;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_BRAILLE_GRADE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_BROWSE_MODE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_KEYBOARD_VISIBILITY;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_SCREEN_SEARCH;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_VOICE_FEEDBACK;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TRANSLATE_PENDING;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_UNDO_TYPING;

import android.content.Context;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Built-in defaults for refreshable braille displays. */
public final class DisplayCommandProfile {
  private static final Map<String, String> SANDFLY_DEFAULTS = createSandFlyDefaults();
  private static final Map<String, String> IOS_DEFAULTS = createIosDefaults();

  private DisplayCommandProfile() {}

  public static DisplayKeyOverrides readOverrides(Context context) {
    return applyDefaults(context, BrailleUserPreferences.readDisplayKeyOverrides(context));
  }

  public static DisplayKeyOverrides applyDefaults(Context context, DisplayKeyOverrides overrides) {
    return applyDefaults(BrailleUserPreferences.usesIosDisplayCommands(context), overrides);
  }

  static DisplayKeyOverrides applyDefaults(boolean ios, DisplayKeyOverrides overrides) {
    return overrides.withDefaults(ios ? IOS_DEFAULTS : SANDFLY_DEFAULTS);
  }

  /** Whether to capture a driver-bound Space chord by its dots. */
  public static boolean capturesPhysicalChord(Context context, String key) {
    return SANDFLY_DEFAULTS.containsKey(key) || BrailleUserPreferences.usesIosDisplayCommands(context);
  }

  /** Returns the saved key for Space with the given dot numbers. */
  public static String spaceChordKey(String dots) {
    int dotMask = 0;
    for (int i = 0; i < dots.length(); i++) {
      int bit = dots.charAt(i) - '1';
      if (bit < 0 || bit >= 8) {
        throw new IllegalArgumentException("Invalid braille dots: " + dots);
      }
      dotMask |= 1 << bit;
    }
    return DisplayKeyOverrides.chordKey(dotMask);
  }

  private static Map<String, String> createSandFlyDefaults() {
    Map<String, String> defaults = new LinkedHashMap<>();
    bindDisplay(defaults, CMD_OPEN_TALKBACK_MENU, "134", "1348");
    bindDisplay(defaults, CMD_EDIT_CUSTOM_LABEL, "12346");
    bindScreenReader(defaults, "INCREASE_MEDIA_VOLUME", "345");
    bindScreenReader(defaults, "DECREASE_MEDIA_VOLUME", "126");
    return Collections.unmodifiableMap(defaults);
  }

  // https://support.apple.com/en-us/118665
  private static Map<String, String> createIosDefaults() {
    Map<String, String> defaults = new LinkedHashMap<>(SANDFLY_DEFAULTS);
    bindDisplay(defaults, CMD_NAV_ITEM_PREVIOUS, "1");
    bindDisplay(defaults, CMD_NAV_ITEM_NEXT, "4");
    bindDisplay(defaults, CMD_NAV_TOP, "123");
    bindDisplay(defaults, CMD_NAV_BOTTOM, "456");
    defaults.put(DisplayKeyOverrides.commandKey(CMD_NAV_TOP_OR_KEY_ACTIVATE), DisplayKeyOverrides.NONE);
    defaults.put(DisplayKeyOverrides.commandKey(CMD_NAV_BOTTOM_OR_KEY_ACTIVATE), DisplayKeyOverrides.NONE);
    bindDisplay(defaults, CMD_TOGGLE_SCREEN_SEARCH, "24");
    bindDisplay(defaults, CMD_NAV_STATUS_BAR, "234");
    bindDisplay(defaults, CMD_GLOBAL_NOTIFICATIONS, "46");
    bindDisplay(defaults, CMD_QUICK_SETTINGS, "25");
    bindDisplay(defaults, CMD_GLOBAL_BACK, "12");
    bindScreenReader(defaults, "PREVIOUS_WINDOW", "35");
    bindScreenReader(defaults, "NEXT_WINDOW", "26");
    bindScreenReader(defaults, "PREV_CONTAINER", "17");
    bindScreenReader(defaults, "NEXT_CONTAINER", "47");
    bindScreenReader(defaults, "SCROLL_LEFT", "246");
    bindScreenReader(defaults, "SCROLL_RIGHT", "135");
    bindScreenReader(defaults, "SCROLL_UP", "3456", "34568");
    bindScreenReader(defaults, "SCROLL_DOWN", "1456", "14568");
    bindDisplay(defaults, CMD_ANNOUNCE_SCROLL_POSITION, "34");
    bindDisplay(defaults, CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD, "3");
    bindDisplay(defaults, CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD, "6");
    bindDisplay(defaults, CMD_PREVIOUS_READING_CONTROL, "23");
    bindDisplay(defaults, CMD_NEXT_READING_CONTROL, "56");
    bindDisplay(defaults, CMD_KEY_ENTER, "36");
    bindDisplay(defaults, CMD_GLOBAL_HOME, "125");
    bindDisplay(defaults, CMD_TOGGLE_KEYBOARD_VISIBILITY, "146");
    bindDisplay(defaults, CMD_LONG_PRESS_CURRENT, "356", "3678");
    bindScreenReader(defaults, "READ_FROM_CURRENT", "1235");
    bindScreenReader(defaults, "READ_FROM_TOP", "2456");
    bindDisplay(defaults, CMD_SELECTION_SELECT_ALL, "2356");
    bindDisplay(defaults, CMD_SELECT_PREVIOUS_CHARACTER, "235");
    bindDisplay(defaults, CMD_SELECT_NEXT_CHARACTER, "256");
    bindDisplay(defaults, CMD_KEY_TAB, "2345");
    bindDisplay(defaults, CMD_KEY_SHIFT_TAB, "1256");
    bindDisplay(defaults, CMD_SELECTION_CUT, "1346");
    bindDisplay(defaults, CMD_SELECTION_COPY, "14");
    bindDisplay(defaults, CMD_SELECTION_PASTE, "1236");
    bindDisplay(defaults, CMD_KEY_DEL, "145", "7");
    bindDisplay(defaults, CMD_KEY_RETURN, "15", "8");
    bindDisplay(defaults, CMD_UNDO_TYPING, "1356");
    bindDisplay(defaults, CMD_REDO_TYPING, "2346");
    bindScreenReader(defaults, "BRAILLE_FIND_TEXT", "124");
    bindScreenReader(defaults, "DESCRIBE_TEXT_FORMATTING", "23456");
    bindScreenReader(defaults, "DICTATE_OR_MEDIA_CONTROL", "156");
    bindScreenReader(defaults, "SHOW_HIDE SCREEN", "123456");
    bindScreenReader(defaults, "PAUSE_FEEDBACK", "1234");
    bindDisplay(defaults, CMD_TOGGLE_VOICE_FEEDBACK, "134", "1347");
    bindDisplay(defaults, CMD_HELP, "13", "137");
    bindDisplay(defaults, CMD_TOGGLE_BROWSE_MODE, "12345", "123457");
    bindDisplay(defaults, CMD_NAV_PAN_UP, "2");
    bindDisplay(defaults, CMD_NAV_PAN_DOWN, "5");
    bindDisplay(defaults, CMD_SHOW_POPUP_MESSAGE_HISTORY, "1345");
    bindDisplay(defaults, CMD_TRANSLATE_PENDING, "45");
    bindDisplay(defaults, CMD_TOGGLE_BRAILLE_GRADE, "1245");
    bindDisplay(defaults, CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE, "236");
    return Collections.unmodifiableMap(defaults);
  }

  private static void bindDisplay(Map<String, String> defaults, int command, String... chords) {
    // Physical pan and delete keys remain available alongside the profile chords.
    if (command != CMD_NAV_PAN_UP && command != CMD_NAV_PAN_DOWN && command != CMD_KEY_DEL) {
      defaults.put(DisplayKeyOverrides.commandKey(command), DisplayKeyOverrides.NONE);
    }
    bind(defaults, DisplayKeyOverrides.commandTarget(command), chords);
  }

  private static void bindScreenReader(Map<String, String> defaults, String command, String... chords) {
    bind(defaults, DisplayKeyOverrides.screenReaderTarget(command), chords);
  }

  private static void bind(Map<String, String> defaults, String target, String... chords) {
    for (String dots : chords) {
      defaults.put(spaceChordKey(dots), target);
    }
  }
}
