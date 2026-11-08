/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.talkback.training;

import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_EXIT;
import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.DEFAULT_BUTTONS;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.LinkInfo.NAVIGATION_SETTINGS;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.LinkInfo.BRAILLE_DISPLAY;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.LinkInfo.KEYBOARD_SHORTCUT;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.LinkInfo.TEXT_FORMATTING_OPTIONS;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.LinkInfo.TUTORIAL;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_EXIT_BANNER;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_KEYBOARD_ENHANCED_KEYMAP;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_KEYBOARD_TUTORIAL;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_SPLIT_TAP_EVERYWHERE;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_TEXT_FORMATTING;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_VOICE_DICTATION_WITHOUT_GOOGLE_PLAY;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate.SUPPORT_VOICE_DICTATION_WITH_GOOGLE_PLAY;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_ANNOUNCING_TEXT_FORMATTING;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_KEYBOARD_ENHANCED_KEYMAP;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_KEYBOARD_SMART_BROWSE_MODE;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_KEYBOARD_TUTORIAL;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_NEW_SETTINGS_LAYOUT;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SHORTCUT_FOR_DICTATION;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SHORTCUT_FOR_DICTATION_WITHOUT_GOOGLE_PLAY;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SPLIT_TAP_EVERYWHERE;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_GESTURES;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_SPEECH;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_BRAILLE;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_COMMANDS;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_AI;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_SANDFLY_OTHER;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_UPDATE_WELCOME;

import android.accessibilityservice.AccessibilityService;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.distribution.Distribution;
import com.google.android.accessibility.talkback.sandflyai.SandflyAi;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageAndContentPredicate;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId;
import com.google.android.accessibility.talkback.trainingcommon.TrainingConfig;
import com.google.common.collect.ImmutableList;

// LINT.IfChange(onboarding_update)
final class OnboardingConfigs {

  private OnboardingConfigs() {}

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Pages
  static final PageConfig.Builder welcomeToUpdatedTalkBackForMultiFingerGestures =
      PageConfig.builder(
              PageId.PAGE_ID_WELCOME_TO_UPDATED_TALKBACK_FOR_MULTIFINGER_GESTURES,
              R.string.welcome_to_updated_talkback_title)
          .addText(R.string.welcome_to_android11_text)
          .addNote(R.string.new_shortcut_gesture_note, PageAndContentPredicate.GESTURE_CHANGED)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_pause_or_play_media_text,
              R.string.new_shortcut_gesture_pause_or_play_media_subtext,
              R.drawable.ic_gesture_2fingerdoubletap)
          .captureGesture(
              AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP,
              R.string.new_shortcut_gesture_pause_media_announcement)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_stop_speech_text, R.drawable.ic_gesture_2fingertap)
          .captureGesture(
              AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP,
              R.string.new_shortcut_gesture_stop_speech_announcement)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_reading_menu_text, R.drawable.ic_gesture_3fingerright)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_copy_text_text, R.drawable.ic_gesture_3fingerdoubletap)
          .captureGesture(
              AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP,
              R.string.new_shortcut_gesture_copy_text_announcement)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_paste_text_text, R.drawable.ic_gesture_3fingertripletap)
          .captureGesture(
              AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP,
              R.string.new_shortcut_gesture_paste_text_announcement)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_cut_text_text,
              R.drawable.ic_gesture_3fingerdoubletaphold)
          .captureGesture(
              AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD,
              R.string.new_shortcut_gesture_cut_text_announcement)
          .addTextWithIcon(
              R.string.new_shortcut_gesture_selection_mode_text,
              R.drawable.ic_gesture_2fingerdoubletaphold)
          .captureGesture(
              AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD,
              R.string.new_shortcut_gesture_selection_mode_on_announcement);
  // SandFly 0.4.0: a page for each area, with a heading for each feature. Each release replaces
  // these pages and raises NEW_FEATURE_SHOWN_KEY in OnboardingInitiator, so that they show once
  // after the update.
  static final PageConfig.Builder sandflyGestures =
      PageConfig.builder(PAGE_ID_SANDFLY_GESTURES, R.string.new_features_0_4_0_gestures_title)
          .addHeading(R.string.new_features_0_4_0_voiceover_gestures_heading)
          .addText(R.string.new_features_0_4_0_voiceover_gestures)
          .addHeading(R.string.new_features_0_4_0_back_tap_heading)
          .addText(R.string.new_features_0_4_0_back_tap)
          .addHeading(R.string.new_features_0_4_0_edge_swipes_heading)
          .addText(R.string.new_features_0_4_0_edge_swipes)
          .addHeading(R.string.new_features_0_4_0_edge_menus_heading)
          .addText(R.string.new_features_0_4_0_edge_menus)
          .addHeading(R.string.new_features_0_4_0_volume_keys_heading)
          .addText(R.string.new_features_0_4_0_volume_keys)
          .addHeading(R.string.new_features_0_4_0_hold_to_cancel_heading)
          .addText(R.string.new_features_0_4_0_hold_to_cancel)
          .addHeading(R.string.new_features_0_4_0_tap_to_activate_heading)
          .addText(R.string.new_features_0_4_0_tap_to_activate);

  static final PageConfig.Builder sandflySpeech =
      PageConfig.builder(PAGE_ID_SANDFLY_SPEECH, R.string.new_features_0_4_0_speech_title)
          .addHeading(R.string.new_features_0_4_0_equalizer_heading)
          .addText(R.string.new_features_0_4_0_equalizer)
          .addHeading(R.string.new_features_0_4_0_casting_heading)
          .addText(R.string.new_features_0_4_0_casting)
          .addHeading(R.string.new_features_0_4_0_full_charge_heading)
          .addText(R.string.new_features_0_4_0_full_charge)
          .addHeading(R.string.new_features_0_4_0_time_voice_heading)
          .addText(R.string.new_features_0_4_0_time_voice)
          .addHeading(R.string.new_features_0_4_0_speak_locked_heading)
          .addText(R.string.new_features_0_4_0_speak_locked);

  static final PageConfig.Builder sandflyBraille =
      PageConfig.builder(PAGE_ID_SANDFLY_BRAILLE, R.string.new_features_0_4_0_braille_title)
          .addHeading(R.string.new_features_0_4_0_display_keys_heading)
          .addText(R.string.new_features_0_4_0_display_keys)
          .addHeading(R.string.new_features_0_4_0_braille_dictionary_heading)
          .addText(R.string.new_features_0_4_0_braille_dictionary)
          .addHeading(R.string.new_features_0_4_0_hide_keyboard_heading)
          .addText(R.string.new_features_0_4_0_hide_keyboard)
          .addHeading(R.string.new_features_0_4_0_emoji_search_heading)
          .addText(R.string.new_features_0_4_0_emoji_search)
          .addHeading(R.string.new_features_0_4_0_one_hand_heading)
          .addText(R.string.new_features_0_4_0_one_hand);

  static final PageConfig.Builder sandflyCommands = sandflyCommandsPage();

  /** Turn color inversion on or off needs Shizuku, which the Play build lacks. */
  private static PageConfig.Builder sandflyCommandsPage() {
    PageConfig.Builder page =
        PageConfig.builder(PAGE_ID_SANDFLY_COMMANDS, R.string.new_features_0_4_0_commands_title)
            .addText(R.string.new_features_0_4_0_commands)
            .addHeading(R.string.new_features_0_4_0_app_shortcuts_heading)
            .addText(R.string.new_features_0_4_0_app_shortcuts)
            .addHeading(R.string.new_features_0_4_0_clear_text_heading)
            .addText(R.string.new_features_0_4_0_clear_text);
    if (!Distribution.IS_PLAY) {
      page.addHeading(R.string.new_features_0_4_0_color_inversion_heading)
          .addText(R.string.new_features_0_4_0_color_inversion);
    }
    return page.addHeading(R.string.new_features_0_4_0_voice_profile_heading)
        .addText(R.string.new_features_0_4_0_voice_profile)
        .addHeading(R.string.new_features_0_4_0_keyboard_input_heading)
        .addText(R.string.new_features_0_4_0_keyboard_input)
        .addHeading(R.string.new_features_0_4_0_turn_off_heading)
        .addText(R.string.new_features_0_4_0_turn_off);
  }

  static final PageConfig.Builder sandflyAi = sandflyAiPage();

  /** Remove this device is a SandFly Pro setting, so it shows only in builds with SandFly Pro. */
  private static PageConfig.Builder sandflyAiPage() {
    PageConfig.Builder page =
        PageConfig.builder(PAGE_ID_SANDFLY_AI, R.string.new_features_0_4_0_ai_title)
            .addHeading(R.string.new_features_0_4_0_ai_actions_heading)
            .addText(R.string.new_features_0_4_0_ai_actions)
            .addHeading(R.string.new_features_0_4_0_speak_ask_heading)
            .addText(R.string.new_features_0_4_0_speak_ask)
            .addHeading(R.string.new_features_0_4_0_report_ai_heading)
            .addText(R.string.new_features_0_4_0_report_ai)
            .addHeading(R.string.new_features_0_4_0_model_file_heading)
            .addText(R.string.new_features_0_4_0_model_file)
            .addHeading(R.string.new_features_0_4_0_correction_prompt_heading)
            .addText(R.string.new_features_0_4_0_correction_prompt);
    if (SandflyAi.isAvailable()) {
      page.addHeading(R.string.new_features_0_4_0_remove_device_heading)
          .addText(R.string.new_features_0_4_0_remove_device);
    }
    return page;
  }

  static final PageConfig.Builder sandflyOther =
      PageConfig.builder(PAGE_ID_SANDFLY_OTHER, R.string.new_features_0_4_0_other_title)
          .addHeading(R.string.new_features_0_4_0_search_heading)
          .addText(R.string.new_features_0_4_0_search)
          .addHeading(R.string.new_features_0_4_0_telegram_quote_heading)
          .addText(R.string.new_features_0_4_0_telegram_quote)
          .addHeading(R.string.new_features_0_4_0_focus_indicator_heading)
          .addText(R.string.new_features_0_4_0_focus_indicator)
          .addHeading(R.string.new_features_0_4_0_scripts_heading)
          .addText(R.string.new_features_0_4_0_scripts);

  // For TB 16.2
  static final PageConfig.Builder updateWelcome =
      PageConfig.builder(PAGE_ID_UPDATE_WELCOME, R.string.welcome_to_updated_talkback_title)
          .addExitBanner(SUPPORT_EXIT_BANNER)
          .addTextWithTtsSpan(
              R.string.update_welcome_talkback_16_2, R.string.update_welcome_talkback_16_2_tts);

  static final PageConfig.Builder newSettingsLayout =
      PageConfig.builder(PAGE_ID_NEW_SETTINGS_LAYOUT, R.string.new_settings_layout_title)
          .addText(R.string.new_settings_layout_text);

  static final PageConfig.Builder announcingTextFormatting =
      PageConfig.builder(
              PAGE_ID_ANNOUNCING_TEXT_FORMATTING, R.string.announcing_text_formatting_title)
          .setShowingPredicate(SUPPORT_TEXT_FORMATTING)
          .addText(R.string.announcing_text_formatting_header)
          .addTextWithBullet(R.string.announcing_text_formatting_option1)
          .addTextWithBullet(R.string.announcing_text_formatting_option2, /* subText= */ true)
          .addText(R.string.announcing_text_formatting_middle)
          .addHtml(R.string.announcing_text_formatiing_sample)
          .addTextWithSettingsLinks(
              R.string.announcing_text_formatting_tail,
              ImmutableList.of(R.string.text_formatting_options_settings_path),
              ImmutableList.of(R.string.text_formatting_options_settings_path_tts),
              ImmutableList.of(TEXT_FORMATTING_OPTIONS));

  static final PageConfig.Builder shortcutForDictation =
      PageConfig.builder(PAGE_ID_SHORTCUT_FOR_DICTATION, R.string.shortcut_for_dictation_title)
          .setShowingPredicate(SUPPORT_VOICE_DICTATION_WITH_GOOGLE_PLAY)
          .addHtml(R.string.shortcut_for_dictation_content);

  static final PageConfig.Builder shortcutForDictationWithoutGooglePlay =
      PageConfig.builder(
              PAGE_ID_SHORTCUT_FOR_DICTATION_WITHOUT_GOOGLE_PLAY,
              R.string.shortcut_for_dictation_title)
          .setShowingPredicate(SUPPORT_VOICE_DICTATION_WITHOUT_GOOGLE_PLAY)
          .addHtml(R.string.shortcut_for_dictation_content_without_google_play);

  static final PageConfig.Builder splitTapEverywhere =
      PageConfig.builder(PAGE_ID_SPLIT_TAP_EVERYWHERE, R.string.split_tap_everywhere_title)
          .setShowingPredicate(SUPPORT_SPLIT_TAP_EVERYWHERE)
          .addText(R.string.split_tap_everywhere_text);

  static final PageConfig.Builder enhancedKeymap =
      PageConfig.builder(PAGE_ID_KEYBOARD_ENHANCED_KEYMAP, R.string.keyboard_enhanced_keymap_title)
          .setShowingPredicate(SUPPORT_KEYBOARD_ENHANCED_KEYMAP)
          .addTextWithSettingsLinks(
              R.string.keyboard_enhanced_keymap_text,
              ImmutableList.of(
                  R.string.keyboard_shortcuts_link, R.string.braille_display_settings_link),
              ImmutableList.of(
                  R.string.keyboard_shortcuts_link_tts, R.string.braille_display_settings_link),
              ImmutableList.of(KEYBOARD_SHORTCUT, BRAILLE_DISPLAY));

  static final PageConfig.Builder smartBrowseMode =
      PageConfig.builder(
              PAGE_ID_KEYBOARD_SMART_BROWSE_MODE, R.string.keyboard_smart_browse_mode_title)
          .addTextWithSettingsLinks(
              R.string.keyboard_smart_browse_mode_text,
              ImmutableList.of(R.string.navigation_settings_link),
              ImmutableList.of(R.string.navigation_settings_link_tts),
              ImmutableList.of(NAVIGATION_SETTINGS));

  static final PageConfig.Builder keyboardTutorial =
      PageConfig.builder(PAGE_ID_KEYBOARD_TUTORIAL, R.string.keyboard_tutorial_title)
          .setShowingPredicate(SUPPORT_KEYBOARD_TUTORIAL)
          .addTextWithSettingsLinks(
              R.string.keyboard_tutorial_text,
              ImmutableList.of(R.string.tutorial_link),
              ImmutableList.of(R.string.tutorial_link_tts),
              ImmutableList.of(TUTORIAL));

  // Training
  static TrainingConfig getTalkBackOnBoardingForSettings() {
    return constructOnBoardingConfigBuilder()
        .setSupportNavigateUpArrow(true)
        .setExitButtonOnlyShowOnLastPage(true)
        .build();
  }

  static TrainingConfig getTalkBackOnBoardingForUpdated() {
    return constructOnBoardingConfigBuilder().build();
  }

  static final TrainingConfig ON_BOARDING_FOR_MULTIFINGER_GESTURES =
      TrainingConfig.builder(R.string.welcome_to_updated_talkback_title)
          .setPages(ImmutableList.of(welcomeToUpdatedTalkBackForMultiFingerGestures))
          .setButtons(ImmutableList.of(BUTTON_TYPE_EXIT))
          .build();

  private static TrainingConfig.Builder constructOnBoardingConfigBuilder() {
    return TrainingConfig.builder(R.string.new_feature_in_talkback_title)
        .setPages(
            ImmutableList.of(
                sandflyGestures, sandflySpeech, sandflyBraille, sandflyCommands, sandflyAi))
        .addPageEndOfSection(sandflyOther)
        .setButtons(DEFAULT_BUTTONS);
  }
}
// LINT.ThenChange(
// //depot/google3/java/com/google/android/accessibility/talkback/res/values/donottranslate.xml:onboarding_update,
// //depot/google3/java/com/google/android/accessibility/talkback/training/OnboardingInitiator.java:onboarding_update)
