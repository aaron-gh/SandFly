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

package com.google.android.accessibility.talkback.trainingcommon.content;

import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP;
import static android.accessibilityservice.AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN;
import static android.accessibilityservice.AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT;
import static android.accessibilityservice.AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP;
import static android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureHints;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The gestures that each tutorial text names, with the action each one does there. A text is shown
 * as written only while all its gestures still do those actions; see {@link Text}.
 */
public final class TutorialGestureUses {

  /** A gesture that a text names, and the action that the text says it does. */
  public static final class GestureUse {
    public final int gestureId;
    @StringRes public final int actionKey;

    GestureUse(int gestureId, @StringRes int actionKey) {
      this.gestureId = gestureId;
      this.actionKey = actionKey;
    }
  }

  private static GestureUse use(int gestureId, @StringRes int actionKey) {
    return new GestureUse(gestureId, actionKey);
  }

  private static final GestureUse NEXT = use(GESTURE_SWIPE_RIGHT, R.string.shortcut_value_next);
  private static final GestureUse PREVIOUS =
      use(GESTURE_SWIPE_LEFT, R.string.shortcut_value_previous);
  private static final GestureUse ADJUST_UP =
      use(GESTURE_SWIPE_UP, R.string.shortcut_value_selected_setting_previous_action);
  private static final GestureUse ADJUST_DOWN =
      use(GESTURE_SWIPE_DOWN, R.string.shortcut_value_selected_setting_next_action);
  private static final GestureUse NEXT_CONTROL_3F =
      use(GESTURE_3_FINGER_SWIPE_RIGHT, R.string.shortcut_value_select_next_setting);
  private static final GestureUse PREVIOUS_CONTROL_3F =
      use(GESTURE_3_FINGER_SWIPE_LEFT, R.string.shortcut_value_select_previous_setting);
  private static final GestureUse NEXT_CONTROL_PRE_R =
      use(GESTURE_SWIPE_DOWN_AND_UP, R.string.shortcut_value_select_next_setting);
  private static final GestureUse MENU_3F =
      use(GESTURE_3_FINGER_SINGLE_TAP, R.string.shortcut_value_talkback_breakout);
  private static final GestureUse MENU_PRE_R =
      use(GESTURE_SWIPE_DOWN_AND_RIGHT, R.string.shortcut_value_talkback_breakout);
  private static final GestureUse MEDIA =
      use(GESTURE_2_FINGER_DOUBLE_TAP, R.string.shortcut_value_media_control_or_voice_input);
  private static final GestureUse PAUSE_SPEECH =
      use(GESTURE_2_FINGER_SINGLE_TAP, R.string.shortcut_value_pause_or_resume_feedback);
  private static final GestureUse COPY = use(GESTURE_3_FINGER_DOUBLE_TAP, R.string.shortcut_value_copy);
  private static final GestureUse PASTE =
      use(GESTURE_3_FINGER_TRIPLE_TAP, R.string.shortcut_value_paste);
  private static final GestureUse CUT =
      use(GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD, R.string.shortcut_value_cut);
  private static final GestureUse SELECTION_MODE =
      use(GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD, R.string.shortcut_value_start_selection_mode);
  private static final GestureUse NEXT_CONTAINER =
      use(GESTURE_4_FINGER_SWIPE_RIGHT, R.string.shortcut_value_next_container);
  private static final GestureUse PREVIOUS_CONTAINER =
      use(GESTURE_4_FINGER_SWIPE_LEFT, R.string.shortcut_value_prev_container);
  private static final GestureUse NEXT_WINDOW =
      use(GESTURE_4_FINGER_SWIPE_DOWN, R.string.shortcut_value_next_window);
  private static final GestureUse VOICE_COMMANDS =
      use(GESTURE_SWIPE_RIGHT_AND_UP, R.string.shortcut_value_voice_commands);
  private static final GestureUse VOLUME_UP_WATCH =
      use(GESTURE_SWIPE_RIGHT_AND_UP, R.string.shortcut_value_increase_volume);
  private static final GestureUse VOLUME_DOWN_WATCH =
      use(GESTURE_SWIPE_RIGHT_AND_DOWN, R.string.shortcut_value_decrease_volume);

  private static final ImmutableMap<Integer, ImmutableList<GestureUse>> USES =
      ImmutableMap.<Integer, ImmutableList<GestureUse>>builder()
          // New gestures in onboarding.
          .put(R.string.new_shortcut_gesture_pause_or_play_media_text, ImmutableList.of(MEDIA))
          .put(R.string.new_shortcut_gesture_stop_speech_text, ImmutableList.of(PAUSE_SPEECH))
          .put(R.string.new_shortcut_gesture_copy_text_text, ImmutableList.of(COPY))
          .put(R.string.new_shortcut_gesture_paste_text_text, ImmutableList.of(PASTE))
          .put(R.string.new_shortcut_gesture_cut_text_text, ImmutableList.of(CUT))
          .put(R.string.new_shortcut_gesture_selection_mode_text, ImmutableList.of(SELECTION_MODE))
          .put(
              R.string.new_shortcut_gesture_reading_menu_text,
              ImmutableList.of(PREVIOUS_CONTROL_3F, NEXT_CONTROL_3F))
          // Touch tutorial.
          .put(R.string.welcome_to_talkback_text, ImmutableList.of(NEXT, PREVIOUS))
          .put(R.string.scrolling_text_completed, ImmutableList.of(NEXT))
          .put(R.string.menus_talkback_menu_text, ImmutableList.of(MENU_3F))
          .put(R.string.menus_talkback_menu_text_pre_r, ImmutableList.of(MENU_PRE_R))
          .put(R.string.moving_cursor_text, ImmutableList.of(ADJUST_UP, ADJUST_DOWN))
          .put(R.string.menus_selector_text, ImmutableList.of(PREVIOUS_CONTROL_3F, NEXT_CONTROL_3F))
          .put(R.string.menus_selector_text_pre_r, ImmutableList.of(NEXT_CONTROL_PRE_R))
          .put(R.string.talkback_tutorial_text, ImmutableList.of(NEXT))
          .put(R.string.selecting_text_text, ImmutableList.of(SELECTION_MODE))
          .put(R.string.selecting_text_text_pre_r, ImmutableList.of(MENU_PRE_R))
          .put(R.string.copy_text, ImmutableList.of(COPY))
          .put(R.string.cut_paste_text, ImmutableList.of(CUT, PASTE))
          .put(R.string.copy_text_pre_r, ImmutableList.of(MENU_PRE_R))
          .put(R.string.typo_correction_text, ImmutableList.of(NEXT_CONTROL_3F))
          .put(R.string.typo_correction_text_pre_r, ImmutableList.of(NEXT_CONTROL_PRE_R))
          .put(
              R.string.typo_correction_text_supported_but_not_english,
              ImmutableList.of(NEXT_CONTROL_3F))
          .put(
              R.string.typo_correction_text_supported_but_not_english_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R))
          .put(
              R.string.read_by_character_text,
              ImmutableList.of(NEXT_CONTROL_3F, ADJUST_UP, ADJUST_DOWN))
          .put(
              R.string.read_by_character_text_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R, ADJUST_UP, ADJUST_DOWN))
          .put(
              R.string.jump_between_controls_text,
              ImmutableList.of(NEXT_CONTROL_3F, ADJUST_UP, ADJUST_DOWN))
          .put(
              R.string.jump_between_controls_text_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R, ADJUST_UP, ADJUST_DOWN))
          .put(
              R.string.jump_between_links_text,
              ImmutableList.of(NEXT_CONTROL_3F, ADJUST_DOWN, NEXT))
          .put(
              R.string.jump_between_links_text_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R, ADJUST_DOWN, NEXT))
          .put(R.string.jump_between_headings_text, ImmutableList.of(NEXT_CONTROL_3F, ADJUST_DOWN))
          .put(
              R.string.jump_between_headings_text_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R, ADJUST_DOWN))
          .put(R.string.find_next_button_text, ImmutableList.of(NEXT))
          .put(R.string.container_find_finish_button_text, ImmutableList.of(NEXT))
          .put(
              R.string.jump_between_containers_text,
              ImmutableList.of(PREVIOUS_CONTAINER, NEXT_CONTAINER, NEXT))
          .put(
              R.string.jump_between_containers_text_pre_r,
              ImmutableList.of(NEXT_CONTROL_PRE_R, ADJUST_DOWN))
          .put(R.string.voice_commands_text, ImmutableList.of(VOICE_COMMANDS))
          .put(R.string.using_screen_search_text, ImmutableList.of(MENU_3F))
          .put(
              R.string.screen_search_navigation_text,
              ImmutableList.of(PREVIOUS_CONTROL_3F, NEXT_CONTROL_3F))
          .put(R.string.making_calls_text, ImmutableList.of(MEDIA))
          .put(R.string.checking_notifications_step_one, ImmutableList.of(NEXT_WINDOW))
          .put(R.string.checking_notifications_step_two, ImmutableList.of(PREVIOUS, NEXT))
          .put(R.string.voice_commands_help_description, ImmutableList.of(VOICE_COMMANDS))
          .put(R.string.container_item_subtext, ImmutableList.of(NEXT_CONTAINER))
          .put(R.string.container_item_exit_subtext, ImmutableList.of(NEXT_CONTAINER))
          .put(R.string.container_item_subtext_pre_r, ImmutableList.of(ADJUST_DOWN))
          .put(R.string.container_item_exit_subtext_pre_r, ImmutableList.of(ADJUST_DOWN))
          .put(R.string.welcome_to_talkback_page_idle_announcement, ImmutableList.of(NEXT))
          .put(R.string.image_description_sample_image_content_description, ImmutableList.of(MENU_3F))
          // Watch tutorial.
          .put(R.string.wear_training_welcome_paragraph, ImmutableList.of(NEXT, PREVIOUS))
          .put(R.string.wear_training_volume_up_text, ImmutableList.of(VOLUME_UP_WATCH))
          .put(R.string.wear_training_volume_down_text, ImmutableList.of(VOLUME_DOWN_WATCH))
          .put(R.string.wear_training_open_talkback_menu_text, ImmutableList.of(MENU_PRE_R))
          .buildOrThrow();

  /** The action that each onboarding announcement says its gesture did. */
  private static final ImmutableMap<Integer, Integer> ANNOUNCEMENT_ACTIONS =
      ImmutableMap.<Integer, Integer>builder()
          .put(R.string.new_shortcut_gesture_pause_media_announcement, MEDIA.actionKey)
          .put(R.string.new_shortcut_gesture_stop_speech_announcement, PAUSE_SPEECH.actionKey)
          .put(R.string.new_shortcut_gesture_copy_text_announcement, COPY.actionKey)
          .put(R.string.new_shortcut_gesture_paste_text_announcement, PASTE.actionKey)
          .put(R.string.new_shortcut_gesture_cut_text_announcement, CUT.actionKey)
          .put(
              R.string.new_shortcut_gesture_selection_mode_on_announcement,
              SELECTION_MODE.actionKey)
          .buildOrThrow();

  private TutorialGestureUses() {}

  /**
   * Returns null if the text {@code textResId} names no gestures or keyboard shortcuts, or if all
   * of them still do what it says, so that the translated text is right. Keyboard tutorial texts
   * are checked by {@link KeyboardTutorialKeys}. Otherwise it returns a line for
   * each action the text names, with the gesture the user has for it, built from translated action
   * and gesture names, such as "Open Backtalk menu: Swipe down then right".
   */
  public static @Nullable String gestureLinesIfChanged(
      Context context, @StringRes int textResId, ServiceData data) {
    if (USES.containsKey(textResId) || KeyboardTutorialKeys.texts().contains(textResId)) {
      data.getShortcuts().noteTextShown(textResId);
    }
    @Nullable String keyLines = KeyboardTutorialKeys.linesIfChanged(context, textResId, data);
    if (keyLines != null) {
      return keyLines;
    }
    ImmutableList<GestureUse> uses = forText(textResId);
    if (uses.isEmpty()
        || uses.stream()
            .allMatch(
                use ->
                    data.getShortcuts().isGestureAssigned(context, use.gestureId, use.actionKey))) {
      return null;
    }
    Set<Integer> actions = new LinkedHashSet<>();
    for (GestureUse use : uses) {
      actions.add(use.actionKey);
    }
    StringBuilder lines = new StringBuilder();
    for (int actionKey : actions) {
      if (lines.length() > 0) {
        lines.append('\n');
      }
      lines.append(
          GestureHints.actionLine(
              context,
              GestureShortcutMapping.getActionString(context, context.getString(actionKey)),
              data.getGestureFromActionKey(actionKey)));
    }
    return lines.toString();
  }

  /** Returns the text {@code textResId}, or the lines from {@link #gestureLinesIfChanged}. */
  public static String getText(Context context, @StringRes int textResId, ServiceData data) {
    @Nullable String lines = gestureLinesIfChanged(context, textResId, data);
    return lines != null ? lines : context.getString(textResId);
  }

  /**
   * Returns whether the announcement {@code announcementResId}, spoken when the user tries a
   * gesture in the tutorial, still describes {@code action}, the gesture's action now. Returns true
   * for announcements that don't describe an action.
   */
  public static boolean announcementStillTrue(
      Context context, @StringRes int announcementResId, @Nullable String action) {
    Integer described = ANNOUNCEMENT_ACTIONS.get(announcementResId);
    return described == null || TextUtils.equals(context.getString(described), action);
  }

  /**
   * Whether a text shown before the service sent its gestures and keyboard shortcuts, and so shown
   * as written, is now replaced by lines.
   */
  public static boolean anyEarlyTextReplaced(Context context, ServiceData data) {
    for (int textResId : data.getShortcuts().takeTextsShownEarly()) {
      if (gestureLinesIfChanged(context, textResId, data) != null) {
        return true;
      }
    }
    return false;
  }

  /** The texts that name gestures or keyboard shortcuts. */
  @VisibleForTesting
  static ImmutableSet<Integer> textsNamingShortcuts() {
    return ImmutableSet.<Integer>builder()
        .addAll(USES.keySet())
        .addAll(ANNOUNCEMENT_ACTIONS.keySet())
        .addAll(KeyboardTutorialKeys.texts())
        .build();
  }

  /** Returns the gestures that the text {@code textResId} names, or an empty list. */
  public static ImmutableList<GestureUse> forText(@StringRes int textResId) {
    ImmutableList<GestureUse> uses = USES.get(textResId);
    return uses == null ? ImmutableList.of() : uses;
  }
}
