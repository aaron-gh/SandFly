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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.android.accessibility.talkback.trainingcommon.TutorialShortcuts;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;

/**
 * The keyboard shortcuts that each keyboard tutorial text names. The texts are written for the
 * enhanced keymap with its default keys and the Action key as the modifier; with any other keys,
 * the text is replaced by a line for each shortcut with the user's keys, built from translated
 * shortcut names and key names.
 */
final class KeyboardTutorialKeys {

  private static final int NEXT = R.string.keycombo_shortcut_navigate_next_default;
  private static final int PREVIOUS = R.string.keycombo_shortcut_navigate_previous_default;
  private static final int CLICK = R.string.keycombo_shortcut_perform_click;
  private static final int UP = R.string.keycombo_shortcut_navigate_up;
  private static final int DOWN = R.string.keycombo_shortcut_navigate_down;
  private static final int NEXT_CONTROL =
      R.string.keycombo_shortcut_global_scroll_forward_reading_menu;
  private static final int PREVIOUS_CONTROL =
      R.string.keycombo_shortcut_global_scroll_backward_reading_menu;
  private static final int NEXT_CONTAINER = R.string.keycombo_shortcut_navigate_next_container;
  private static final int PREVIOUS_CONTAINER =
      R.string.keycombo_shortcut_navigate_previous_container;
  private static final int SHORTCUTS_LIST =
      R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog;

  private static final ImmutableMap<Integer, ImmutableList<Integer>> KEYS =
      ImmutableMap.<Integer, ImmutableList<Integer>>builder()
          .put(R.string.keyboard_tutorial_welcome_to_talkback_page_text, ImmutableList.of(NEXT))
          .put(
              R.string.keyboard_tutorial_welcome_to_talkback_next_item_text,
              ImmutableList.of(PREVIOUS, CLICK))
          .put(
              R.string.keyboard_tutorial_chars_words_paragraphs_page_text,
              ImmutableList.of(NEXT_CONTROL, PREVIOUS_CONTROL, UP, DOWN))
          .put(
              R.string.keyboard_tutorial_reading_controls_page_text,
              ImmutableList.of(NEXT_CONTROL, PREVIOUS_CONTROL, UP, DOWN))
          .put(
              R.string.keyboard_tutorial_system_navigation_page_text,
              // Home, Recents, Back and Notifications are named by their system keys, which have no
              // Backtalk shortcut on the default keymap.
              ImmutableList.of(
                  R.string.keycombo_shortcut_navigate_previous_window,
                  R.string.keycombo_shortcut_navigate_next_window))
          .put(
              R.string.keyboard_tutorial_speech_settings_page_text,
              ImmutableList.of(
                  R.string.keycombo_shortcut_global_speech_volume_increase,
                  R.string.keycombo_shortcut_global_speech_volume_decrease,
                  R.string.keycombo_shortcut_global_speech_rate_increase,
                  R.string.keycombo_shortcut_global_speech_rate_decrease))
          .put(
              R.string.keyboard_tutorial_more_keyboard_shortcuts_page_text,
              ImmutableList.of(
                  R.string.keycombo_shortcut_other_talkback_context_menu,
                  SHORTCUTS_LIST,
                  R.string.keycombo_shortcut_global_show_learn_mode_page))
          .put(
              R.string.keyboard_tutorial_browse_mode_page_text,
              ImmutableList.of(
                  R.string.keycombo_shortcut_other_toggle_browse_mode,
                  R.string.keycombo_shortcut_navigate_next_aria_landmark,
                  R.string.keycombo_shortcut_navigate_next_control,
                  R.string.keycombo_shortcut_navigate_next_button,
                  R.string.keycombo_shortcut_navigate_next_link,
                  R.string.keycombo_shortcut_navigate_next_heading,
                  NEXT))
          .put(
              R.string.keyboard_tutorial_heading_levels_page_text,
              ImmutableList.of(
                  R.string.keycombo_shortcut_navigate_next_heading_1,
                  R.string.keycombo_shortcut_navigate_next_heading_2,
                  R.string.keycombo_shortcut_navigate_next_heading_3,
                  R.string.keycombo_shortcut_navigate_next_heading_4,
                  R.string.keycombo_shortcut_navigate_next_heading_5,
                  R.string.keycombo_shortcut_navigate_next_heading_6,
                  NEXT))
          .put(
              R.string.keyboard_tutorial_tables_page_text,
              ImmutableList.of(
                  R.string.keycombo_shortcut_navigate_previous_row,
                  R.string.keycombo_shortcut_navigate_next_row,
                  R.string.keycombo_shortcut_navigate_previous_column,
                  R.string.keycombo_shortcut_navigate_next_column,
                  NEXT))
          .put(R.string.keyboard_tutorial_editing_text_page_text, ImmutableList.of(NEXT))
          .put(
              R.string.keyboard_tutorial_more_browse_mode_page_text,
              ImmutableList.of(SHORTCUTS_LIST))
          .put(
              R.string.keyboard_tutorial_jump_between_controls_page_text,
              ImmutableList.of(NEXT_CONTROL, UP, DOWN))
          .put(
              R.string.keyboard_tutorial_jump_between_links_page_text,
              ImmutableList.of(NEXT_CONTROL, DOWN, NEXT))
          .put(
              R.string.keyboard_tutorial_jump_between_headings_page_text,
              ImmutableList.of(NEXT_CONTROL, DOWN))
          .put(R.string.keyboard_tutorial_find_next_button, ImmutableList.of(NEXT))
          .put(
              R.string.keyboard_tutorial_jump_between_containers_page_text,
              ImmutableList.of(NEXT_CONTAINER, PREVIOUS_CONTAINER, NEXT))
          .put(R.string.keyboard_tutorial_go_to_next_container, ImmutableList.of(NEXT_CONTAINER))
          .put(R.string.keyboard_tutorial_exit_container, ImmutableList.of(NEXT_CONTAINER))
          .put(
              R.string.keyboard_tutorial_add_container_reading_controls,
              ImmutableList.of(NEXT_CONTROL, PREVIOUS_CONTROL, DOWN, UP, NEXT))
          .put(
              R.string.keyboard_tutorial_image_description_page_text,
              ImmutableList.of(R.string.keycombo_shortcut_global_describe_image))
          .put(
              R.string.keyboard_tutorial_image_description_sample_image_content_description,
              ImmutableList.of(R.string.keycombo_shortcut_global_describe_image))
          .put(
              R.string.keyboard_tutorial_voice_commands_page_text,
              ImmutableList.of(R.string.keycombo_shortcut_other_open_voice_commands))
          .put(
              R.string.keyboard_tutorial_screen_search_page_text,
              ImmutableList.of(R.string.keycombo_shortcut_other_toggle_search))
          .put(
              R.string.keyboard_tutorial_navigate_to_screen_search_page_text,
              ImmutableList.of(NEXT_CONTROL, PREVIOUS_CONTROL, DOWN, UP))
          .buildOrThrow();

  private KeyboardTutorialKeys() {}

  /**
   * Returns null if the text {@code textResId} names no keyboard shortcuts, or if the user's keys
   * are the ones it names. Otherwise returns a line for each shortcut it names, with the user's
   * keys.
   */
  static @Nullable String linesIfChanged(
      Context context, @StringRes int textResId, ServiceData data) {
    ImmutableList<Integer> keys = KEYS.get(textResId);
    TutorialShortcuts shortcuts = data.getShortcuts();
    if (keys == null
        || keys.stream().allMatch(key -> shortcuts.isKeyComboAsWritten(context.getString(key)))) {
      return null;
    }
    StringBuilder lines = new StringBuilder();
    for (int keyRes : keys) {
      String key = context.getString(keyRes);
      @Nullable String title = title(context, key);
      if (title == null) {
        // A line needs the shortcut's name. Without one, the text as written is better.
        return null;
      }
      @Nullable String keysText = shortcuts.getKeyComboText(key);
      if (keysText == null || keysText.equals(context.getString(R.string.keycombo_unassigned))) {
        keysText =
            context.getString(
                R.string.template_settings_path,
                context.getString(R.string.talkback_preferences_title),
                context.getString(R.string.title_pref_manage_keyboard_shortcuts));
      }
      if (lines.length() > 0) {
        lines.append('\n');
      }
      lines.append(
          context.getString(R.string.template_action_and_gesture, title, keysText));
    }
    return lines.toString();
  }

  /** The texts that name keyboard shortcuts. */
  static ImmutableSet<Integer> texts() {
    return KEYS.keySet();
  }

  /** The shortcuts that the texts name. */
  @VisibleForTesting
  static ImmutableSet<Integer> keys() {
    return KEYS.values().stream().flatMap(List::stream).collect(toImmutableSet());
  }

  /**
   * The name of the shortcut saved as {@code key}, as the keyboard shortcuts settings show it, or
   * null if the settings don't list it.
   */
  @VisibleForTesting
  static @Nullable String title(Context context, String key) {
    // The navigation shortcuts are saved with "_default" after the names the list knows.
    String listKey = key.endsWith("_default") ? key.substring(0, key.length() - 8) : key;
    TalkBackPhysicalKeyboardShortcut shortcut =
        TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.getResources(), listKey);
    return shortcut == null ? null : shortcut.getDescription(context.getResources());
  }
}
