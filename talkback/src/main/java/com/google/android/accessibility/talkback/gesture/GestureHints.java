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

package com.google.android.accessibility.talkback.gesture;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.WindowUtils;
import java.util.Locale;

/**
 * Text that names the gesture for an action, taken from the user's gesture settings rather than
 * written into a string. Everything it builds comes from strings that are already translated: the
 * action's title, the gesture's name, and the titles of the settings screens.
 */
public final class GestureHints {

  private GestureHints() {}

  /**
   * Whether {@code gestureId}, as a left-to-right text names it, is assigned to the action saved as
   * {@code action}.
   */
  public static boolean isAssigned(
      Context context, GestureShortcutMapping mapping, int gestureId, String action) {
    int laidOut = asLaidOut(gestureId, WindowUtils.isScreenLayoutRTL(context));
    return TextUtils.equals(mapping.getActionKeyFromGestureId(laidOut), action);
  }

  /**
   * Returns the gesture that does what {@code gestureId} does in a left-to-right layout. In a
   * right-to-left layout, the left and right swipes swap settings, and the texts swap them too.
   */
  public static int asLaidOut(int gestureId, boolean isRtl) {
    if (!isRtl) {
      return gestureId;
    }
    return switch (gestureId) {
      case AccessibilityService.GESTURE_SWIPE_LEFT -> AccessibilityService.GESTURE_SWIPE_RIGHT;
      case AccessibilityService.GESTURE_SWIPE_RIGHT -> AccessibilityService.GESTURE_SWIPE_LEFT;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT ->
          AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT ->
          AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT;
      default -> gestureId;
    };
  }

  /**
   * Returns the name of the gesture assigned to {@code action}, starting with a lower-case letter
   * so that it can sit inside a sentence, or null if no gesture is assigned to it.
   */
  public static @Nullable String gestureInSentence(
      Context context, GestureShortcutMapping mapping, String action) {
    String gesture = mapping.getGestureFromActionKey(action);
    return TextUtils.isEmpty(gesture) ? null : lowerCaseFirst(context, gesture);
  }

  /**
   * Returns a line such as "Open Backtalk menu: Tap with 3 fingers", naming the action saved as
   * {@code actionRes} and the gesture assigned to it. If no gesture is, it names the settings screen
   * where gestures are assigned, such as "Open Backtalk menu: Backtalk settings > Gestures".
   */
  public static String actionLine(
      Context context, GestureShortcutMapping mapping, @StringRes int actionRes) {
    String action = context.getString(actionRes);
    return actionLine(
        context,
        GestureShortcutMapping.getActionString(context, action),
        mapping.getGestureFromActionKey(action));
  }

  /**
   * Returns a line naming {@code actionTitle} and {@code gesture}, or the gesture settings screen
   * if {@code gesture} is null.
   */
  public static String actionLine(
      Context context, CharSequence actionTitle, @Nullable String gesture) {
    return context.getString(
        R.string.template_action_and_gesture,
        actionTitle,
        TextUtils.isEmpty(gesture) ? gestureSettingsPath(context) : gesture);
  }

  /**
   * Says that screen search has no keyword yet and how to start one. The translated sentences name
   * a gesture, so one is used only while that gesture starts screen search; otherwise the line
   * names the screen search gesture the user has.
   */
  public static String screenSearchNoKeywordHint(Context context, GestureShortcutMapping mapping) {
    String action = context.getString(R.string.shortcut_value_screen_search);
    if (FeatureSupport.isMultiFingerGestureSupported()
        && isAssigned(
            context, mapping, AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD, action)) {
      return context.getString(R.string.screen_search_no_keyword_hint);
    }
    if (isAssigned(context, mapping, AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN, action)) {
      return context.getString(R.string.screen_search_no_keyword_hint_pre_r);
    }
    return actionLine(context, mapping, R.string.shortcut_value_screen_search);
  }

  /** The path to the gesture settings, such as "Backtalk settings > Gestures". */
  public static String gestureSettingsPath(Context context) {
    return context.getString(
        R.string.template_settings_path,
        context.getString(R.string.talkback_preferences_title),
        context.getString(R.string.title_pref_category_manage_gestures));
  }

  /** Lower-cases the first letter of {@code text}, leaving the rest, which can hold nouns. */
  public static String lowerCaseFirst(Context context, String text) {
    if (text.isEmpty()) {
      return text;
    }
    Locale locale = context.getResources().getConfiguration().getLocales().get(0);
    int first = text.offsetByCodePoints(0, 1);
    return text.substring(0, first).toLowerCase(locale) + text.substring(first);
  }
}
