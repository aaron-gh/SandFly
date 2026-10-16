/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.contextmenu;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuInflater;
import androidx.annotation.BoolRes;
import androidx.annotation.StringRes;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.DimScreenActor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem.DeferredType;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleProcessor;
import com.google.android.accessibility.talkback.scripting.ItemInspector;
import com.google.android.accessibility.talkback.scripting.ScriptMenuItem;
import com.google.android.accessibility.talkback.scripting.Scripts;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Configure dynamic menu items on the talkback context menu. */
public class TalkbackMenuProcessor {

  private static final String TAG = "TalkbackMenuProcessor";

  /*
   * Talkback context menu items order
   * Reserves for context_menu.xml to implement
   * ORDER_READ_FROM_TOP = 8;
   * ORDER_READ_FROM_NEXT_ITEM = 9;
   * ORDER_COPY_LAST_SPOKEN_PHRASE = 10;
   * ORDER_SPELL_LAST_SPOKEN_PHRASE = 11;
   * ORDER_REPEAT_LAST_SPOKEN_PHRASE = 12;
   * ORDER_VERBOSITY = 13;
   * ORDER_AUDIO_DUCKING = 16;
   * ORDER_SOUND_FEEDBACK = 17;
   * ORDER_VIBRATION_FEEDBACK = 18;
   * ORDER_SCREEN_SEARCH = 19;
   * ORDER_VOICE_COMMANDS = 21;
   * ORDER_TALKBACK_SETTINGS = 22;
   * ORDER_TTS_SETTINGS = 23;
   * ORDER_BRAILLE_DISPLAY_SETTINGS = 25;
   */
  private static final int ORDER_TYPO_SUGGESTIONS = 1;
  private static final int ORDER_ACTIONS = 2;
  private static final int ORDER_SCRIPT_COMMANDS = 3;
  private static final int ORDER_PAGE_NAVIGATION = 4;
  public static final int ORDER_LABELS = 5;
  public static final int ORDER_SUMMARIZE_VIEW = 6;
  public static final int ORDER_IMAGE_CAPTION = 6;
  private static final int ORDER_NAVIGATION = 7;
  public static final int ORDER_TEXT_FORMATTING = 13;
  private static final int ORDER_LANGUAGES = 14;
  private static final int ORDER_EMOJI = 15;
  // Every order up to 27 is taken, so this shares Emoji's, as ORDER_SUMMARIZE_VIEW and
  // ORDER_IMAGE_CAPTION share theirs. The menu keeps items with the same order in the order they're
  // added, and the menu settings sort them by title, so Emoji is added first to match.
  private static final int ORDER_VOICE_PROFILES = 15;
  private static final int ORDER_SHOW_HIDE_SCREEN = 20;
  private static final int ORDER_PAUSE_BACKTALK = 21;
  private static final int ORDER_SYSTEM_ACTIONS = 24;
  private static final int ORDER_INSPECT_ITEM = 26;
  private static final int ORDER_COPY_SCREEN_TREE = ORDER_INSPECT_ITEM;
  private static final int ORDER_TELL_TIME = 27;

  private final TalkBackService service;
  private final ActorState actorState;
  private final Pipeline.FeedbackReturner pipeline;
  private final NodeMenuRuleProcessor nodeMenuRuleProcessor;
  private final AccessibilityNodeInfoCompat currentNode;

  // TODO: Reduces dependency on TalkBackService to dependency on Context.
  public TalkbackMenuProcessor(
      TalkBackService service,
      ActorState actorState,
      Pipeline.FeedbackReturner pipeline,
      NodeMenuRuleProcessor nodeMenuRuleProcessor,
      AccessibilityNodeInfoCompat currentNode) {
    this.service = service;
    this.actorState = actorState;
    this.pipeline = pipeline;
    this.nodeMenuRuleProcessor = nodeMenuRuleProcessor;
    this.currentNode = currentNode;
  }

  /**
   * Populates a {@link Menu} with dynamic items relevant to the current global TalkBack state.
   *
   * @param menu The menu to populate.
   * @return {@code true} if successful, {@code false} otherwise.
   */
  public boolean prepareMenu(ContextMenu menu) {
    // Apply attributes to menu items.
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);

    // Typo suggestions
    addItemOrSubMenuForCurrentNode(
        menu,
        R.id.typo_suggestions_menu,
        R.string.title_edittext_typo_suggestions,
        ORDER_TYPO_SUGGESTIONS);
    // Custom Action
    addItemOrSubMenuForCurrentNode(
        menu, R.id.custom_action_menu, R.string.title_custom_action, ORDER_ACTIONS);
    // Page Navigation
    addItemOrSubMenuForCurrentNode(
        menu, R.id.viewpager_menu, R.string.title_viewpager_controls, ORDER_PAGE_NAVIGATION);
    // Labeling
    addItemOrSubMenuForCurrentNode(
        menu, R.id.labeling_breakout_add_label, R.string.title_labeling_controls, ORDER_LABELS);

    // Navigation
    addItemOrSubMenuForCurrentNode(
        menu, R.id.granularity_menu, R.string.title_granularity, ORDER_NAVIGATION);

    // Image caption
    addItemOrSubMenuForCurrentNode(
        menu, R.id.image_caption_menu, R.string.title_image_caption, ORDER_IMAGE_CAPTION);

    if (GeminiConfiguration.screenOverviewEnabled(service)) {
      // Screen overview
      addItemOrSubMenuForCurrentNode(
          menu, R.id.summarize_view_menu, R.string.title_summarize_view, ORDER_SUMMARIZE_VIEW);
    }

    // Text formatting
    addItemOrSubMenuForCurrentNode(
        menu, R.id.text_formatting, R.string.title_text_formatting_menu, ORDER_TEXT_FORMATTING);

    // Read From & Last Phrase Spoken & screen search at context_menu.xml
    addContextMenuXmlMenu(menu);

    // Show/hide screen
    addDimOrBrightenScreen(menu);
    // Pause Backtalk
    addPauseBacktalk(menu);
    // Tell time
    addTellingTimeActionMenu(menu, prefs);
    // Language
    addLanguageMenuIfValid(menu);
    addEmojiMenuIfValid(menu);
    // Voice profile
    addVoiceProfileMenuIfValid(menu);
    // System Action
    addWindowActionMenu(menu);
    addScriptCommands(menu);
    addScriptTool(
        menu,
        R.id.inspect_item,
        ORDER_INSPECT_ITEM,
        R.string.pref_show_context_menu_inspect_item_setting_key,
        R.bool.pref_show_context_menu_inspect_item_default,
        R.string.shortcut_inspect_item,
        (node, say) -> say.accept(ItemInspector.inspect(service, node)));
    addScriptTool(
        menu,
        R.id.copy_screen_tree,
        ORDER_COPY_SCREEN_TREE,
        R.string.pref_show_context_menu_copy_screen_tree_setting_key,
        R.bool.pref_show_context_menu_copy_screen_tree_default,
        R.string.shortcut_copy_screen_tree,
        (node, say) -> ItemInspector.copyScreenTree(service, node, say));

    setMenuItemShowsDialog(
        menu,
        R.id.enable_dimming,
        prefs.getBoolean(
            service.getString(R.string.pref_show_dim_screen_confirmation_dialog), true));

    setMenuItemShowsDialog(
        menu,
        R.id.pause_backtalk,
        prefs.getBoolean(service.getString(R.string.pref_show_pause_confirmation_dialog), true));
    // Pause once the menu has closed, so closing it does not move focus or speak after the pause.
    setMenuItemDeferredType(menu, R.id.pause_backtalk, DeferredType.WINDOWS_STABLE);
    setSkipRefocusAndWindowAnnounce(menu, R.id.pause_backtalk, true);

    setMenuItemShowsDialog(
        menu,
        R.id.read_from_top,
        prefs.getBoolean(
            service.getString(R.string.pref_show_continuous_reading_mode_dialog), true));
    setMenuItemShowsDialog(
        menu,
        R.id.read_from_current,
        prefs.getBoolean(
            service.getString(R.string.pref_show_continuous_reading_mode_dialog), true));

    setMenuItemDeferredType(menu, R.id.screen_search, DeferredType.ACCESSIBILITY_FOCUS_RECEIVED);
    setSkipRefocusAndWindowAnnounce(menu, R.id.screen_search, true);

    setMenuItemDeferredType(menu, R.id.read_from_top, DeferredType.WINDOWS_STABLE);
    setSkipRefocusAndWindowAnnounce(menu, R.id.read_from_top, true);

    setMenuItemDeferredType(
        menu, R.id.read_from_current, DeferredType.ACCESSIBILITY_FOCUS_RECEIVED);
    setSkipRefocusAndWindowAnnounce(menu, R.id.read_from_current, true);

    setSkipRefocusAndWindowAnnounce(menu, R.id.spell_last_utterance, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.repeat_last_utterance, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.copy_last_utterance_to_clipboard, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.audio_ducking, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.sound_feedback, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.vibration_feedback, true);

    menu.sortItemsByOrder();

    return true;
  }

  private boolean showMenuItem(@StringRes int prefKeyId, @BoolRes int defaultValueResId) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    return prefs.getBoolean(
        service.getString(prefKeyId), service.getResources().getBoolean(defaultValueResId));
  }

  // Adds context_menu.xml, such as read from, last spoken, find on screen sub menu, .., to menu
  private void addContextMenuXmlMenu(ContextMenu menu) {
    new MenuInflater(service).inflate(R.menu.context_menu, menu);

    // Removes talkback TTS settings if phone is locked, Setup Wizard doesn't complete or uncheck
    // show item.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || !showMenuItem(
            R.string.pref_show_context_menu_tts_settings_setting_key,
            R.bool.pref_show_context_menu_tts_settings_default)) {
      menu.removeItem(R.id.tts_settings);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_read_from_top_setting_key,
        R.bool.pref_show_context_menu_read_from_top_default)) {
      menu.removeItem(R.id.read_from_top);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_read_from_current_setting_key,
        R.bool.pref_show_context_menu_read_from_current_default)) {
      menu.removeItem(R.id.read_from_current);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_copy_last_spoken_phrase_setting_key,
        R.bool.pref_show_context_menu_copy_last_spoken_phrase_default)) {
      menu.removeItem(R.id.copy_last_utterance_to_clipboard);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_spell_last_spoken_phrase_setting_key,
        R.bool.pref_show_context_menu_spell_last_spoken_phrase_default)) {
      menu.removeItem(R.id.spell_last_utterance);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_repeat_last_spoken_phrase_setting_key,
        R.bool.pref_show_context_menu_repeat_last_spoken_phrase_default)) {
      menu.removeItem(R.id.repeat_last_utterance);
    }

    // Removes screen search if this is watch or uncheck show item.
    if (FormFactorUtils.isAndroidWear()
        || !showMenuItem(
            R.string.pref_show_context_menu_find_on_screen_setting_key,
            R.bool.pref_show_context_menu_find_on_screen_default)) {
      menu.removeItem(R.id.screen_search);
    }

    // Removes voice command if phone is locked, Setup Wizard doesn't complete or uncheck show item,
    // or in the Wear build.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || FormFactorUtils.isAndroidWear()
        || FormFactorUtils.isAndroidXr()
        || !showMenuItem(
            R.string.pref_show_context_menu_voice_commands_setting_key,
            R.bool.pref_show_context_menu_voice_commands_default)) {
      menu.removeItem(R.id.voice_commands);
    }

    // Removes talkback settings if phone is locked, Setup Wizard does not complete or uncheck show
    // item.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || !showMenuItem(
            R.string.pref_show_context_menu_talkback_settings_setting_key,
            R.bool.pref_show_context_menu_talkback_settings_default)) {
      menu.removeItem(R.id.talkback_settings);
    }

    // Removes braille display settings if braille display isn't supported, phone is locked, Setup
    // Wizard isn't completed or the preference of showing item is unchecked.
    if (!FeatureSupport.supportBrailleDisplay(service)
        || ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || FormFactorUtils.isAndroidXr()
        || !showMenuItem(
            R.string.pref_show_context_menu_braille_display_settings_setting_key,
            R.bool.pref_show_context_menu_braille_display_settings_default)) {
      menu.removeItem(R.id.braille_display_settings);
    }

    if (!FeatureFlagReader.enableShowKeyboardShortcutsDialog(service)
        || !showMenuItem(
            R.string.pref_show_context_menu_show_keyboard_shortcuts_setting_key,
            R.bool.pref_show_context_menu_show_keyboard_shortcuts_default)) {
      menu.removeItem(R.id.show_keyboard_shortcuts);
    }

    if (ScreenMonitor.isDeviceLocked(service)
        || !showMenuItem(
            R.string.pref_show_context_menu_verbosity_setting_key,
            R.bool.pref_show_context_menu_verbosity_default)) {
      menu.removeItem(R.id.verbosity);
    }

    if (showMenuItem(
        R.string.pref_show_context_menu_audio_ducking_setting_key,
        R.bool.pref_show_context_menu_audio_ducking_default)) {
      computeTitleForMenuItem(
          menu.findItem(R.id.audio_ducking),
          R.string.audio_focus_state,
          R.string.pref_use_audio_focus_key,
          R.bool.pref_use_audio_focus_default);
    } else {
      menu.removeItem(R.id.audio_ducking);
    }

    if (showMenuItem(
        R.string.pref_show_context_menu_sound_feedback_setting_key,
        R.bool.pref_show_context_menu_sound_feedback_default)) {
      computeTitleForMenuItem(
          menu.findItem(R.id.sound_feedback),
          R.string.sound_feedback_state,
          R.string.pref_soundback_key,
          R.bool.pref_soundback_default);
    } else {
      menu.removeItem(R.id.sound_feedback);
    }

    if (FeatureSupport.isVibratorSupported(service)
        && showMenuItem(
            R.string.pref_show_context_menu_vibration_feedback_setting_key,
            R.bool.pref_show_context_menu_vibration_feedback_default)) {
      computeTitleForMenuItem(
          menu.findItem(R.id.vibration_feedback),
          R.string.vibration_feedback_state,
          R.string.pref_vibration_key,
          R.bool.pref_vibration_default);
    } else {
      menu.removeItem(R.id.vibration_feedback);
    }
  }

  /**
   * Updates title of menu item by current status, on or off, of item.
   *
   * @param item The menu item to update title.
   * @param titleResId The menu title to update.
   * @param prefKeyResId The pref key id of status of feedback that item likes to change.
   * @param defaultPrefKeyResId The default setting of status of feedback.
   */
  private void computeTitleForMenuItem(
      ContextMenuItem item, int titleResId, int prefKeyResId, int defaultPrefKeyResId) {
    boolean preferenceValue =
        SharedPreferencesUtils.getBooleanPref(
            SharedPreferencesUtils.getSharedPreferences(service),
            service.getResources(),
            prefKeyResId,
            defaultPrefKeyResId);

    String newTitle =
        service.getString(
            titleResId,
            preferenceValue
                ? service.getString(R.string.value_on)
                : service.getString(R.string.value_off));

    item.setTitle(newTitle);
  }

  /**
   * Adds sub menu to menu by current node
   *
   * @param menu The menu to add sub menu.
   * @param itemId The item Id on menu.
   * @param titleId The title Id on sub menu.
   */
  private void addItemOrSubMenuForCurrentNode(
      ContextMenu menu, int itemId, int titleId, int itemOrder) {
    menu.removeItem(itemId);

    // If itemId is to add label item, removes edit label item after removes add label,
    if (itemId == R.id.labeling_breakout_add_label) {
      menu.removeItem(R.id.labeling_breakout_edit_label);
    }

    if (!nodeMenuRuleProcessor.isEnabled(itemId)) {
      return;
    }

    if (currentNode == null) {
      return;
    }
    AccessibilityNodeInfoCompat node = currentNode;
    nodeMenuRuleProcessor.prepareTalkbackMenuForNode(menu, node, itemId, titleId, itemOrder);
  }

  private void addLanguageMenuIfValid(ContextMenu menu) {
    // If the menu already has languages menu item, remove it. This will avoid multiple language
    // options appearing in the global context menu. This will dismiss the menu and clears the menu.
    menu.removeItem(R.id.language_menu);

    if (!showMenuItem(
        R.string.pref_show_context_menu_language_setting_key,
        R.bool.pref_show_context_menu_language_default)) {
      return;
    }

    // The Languages menu item will be shown only if there is more than 1 language installed and
    // is populated dynamically.
    boolean shouldShowLanguageMenu = actorState.getLanguageState().allowSelectLanguage();
    LogUtils.v(TAG, "Language menu added: " + shouldShowLanguageMenu);
    if (!shouldShowLanguageMenu) {
      return;
    }
    // Add the latest set of languages in the sub menu.
    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.language_menu,
            ORDER_LANGUAGES,
            service.getString(R.string.spoken_language));
    if (!LanguageMenuProcessor.prepareLanguageSubMenu(service, pipeline, actorState, subMenu)) {
      menu.removeItem(R.id.language_menu);
    }
  }

  private void addVoiceProfileMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.voice_profile_menu);

    if (!showMenuItem(
        R.string.pref_show_context_menu_voice_profile_setting_key,
        R.bool.pref_show_context_menu_voice_profile_default)) {
      return;
    }

    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.voice_profile_menu,
            ORDER_VOICE_PROFILES,
            service.getString(R.string.title_selector_voice_profile));
    // Shown only once there is a profile to choose besides Backtalk default.
    if (!VoiceProfileMenuProcessor.prepareVoiceProfileSubMenu(service, pipeline, subMenu)) {
      menu.removeItem(R.id.voice_profile_menu);
    }
  }

  /** Adds a sub menu to choose how emoji are read, titled with the current choice. */
  private void addEmojiMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.emoji_menu);
    if (!showMenuItem(
        R.string.pref_show_context_menu_emoji_setting_key,
        R.bool.pref_show_context_menu_emoji_default)) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    String[] values = service.getResources().getStringArray(R.array.pref_emoji_speech_values);
    String[] entries = service.getResources().getStringArray(R.array.pref_emoji_speech_entries);
    String current =
        SharedPreferencesUtils.getStringPref(
            prefs,
            service.getResources(),
            R.string.pref_emoji_speech_key,
            R.string.pref_emoji_speech_default);
    int currentIndex = Math.max(0, Arrays.asList(values).indexOf(current));
    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.emoji_menu,
            ORDER_EMOJI,
            service.getString(R.string.emoji_speech_state, entries[currentIndex]));
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      String entry = entries[i];
      ContextMenuItem item =
          ContextMenu.createMenuItem(service, R.id.group_emoji, Menu.NONE, Menu.NONE, entry);
      item.setOnMenuItemClickListener(
          (OnContextMenuItemClickListener)
              clicked -> {
                SharedPreferencesUtils.putStringPref(
                    prefs, service.getResources(), R.string.pref_emoji_speech_key, value);
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        service.getString(R.string.emoji_speech_state, entry),
                        SpeakOptions.create()
                            .setFlags(
                                FeedbackItem.FLAG_NO_HISTORY
                                    | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)));
                return true;
              });
      subMenu.add(item);
    }
  }

  private void addDimOrBrightenScreen(ContextMenu menu) {
    menu.removeItem(R.id.disable_dimming);
    menu.removeItem(R.id.enable_dimming);

    if (FormFactorUtils.isAndroidXr()) {
      return;
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_dim_or_brighten_setting_key,
        R.bool.pref_show_context_menu_dim_or_brighten_default)) {
      return;
    }

    // Add Dim Screen items depending on API level and current device lock state.
    boolean shouldShowDimOrBrightenScreen = DimScreenActor.isSupportedbyPlatform(service);
    LogUtils.d(TAG, "Dim or Brighten screen functionality added: " + shouldShowDimOrBrightenScreen);
    if (!shouldShowDimOrBrightenScreen) {
      return;
    }
    if (actorState.getDimScreen().isDimmingEnabled()) {
      menu.add(
          /* groupId= */ 0,
          R.id.disable_dimming,
          ORDER_SHOW_HIDE_SCREEN,
          R.string.shortcut_disable_dimming);
    } else {
      menu.add(
          /* groupId= */ 0,
          R.id.enable_dimming,
          ORDER_SHOW_HIDE_SCREEN,
          R.string.shortcut_enable_dimming);
    }
  }

  private void addPauseBacktalk(ContextMenu menu) {
    menu.removeItem(R.id.pause_backtalk);

    // A watch has no volume keys or keyboard to resume with, and the paused notification needs a
    // permission watches rarely grant, so pausing could leave the user without a screen reader. On
    // a TV, the volume keys often go to the TV or a soundbar, and there is no notification shade.
    if (FormFactorUtils.isAndroidWear() || FormFactorUtils.isAndroidTv()) {
      return;
    }
    if (!showMenuItem(
        R.string.pref_show_context_menu_pause_backtalk_setting_key,
        R.bool.pref_show_context_menu_pause_backtalk_default)) {
      return;
    }

    menu.add(
        /* groupId= */ 0, R.id.pause_backtalk, ORDER_PAUSE_BACKTALK, R.string.title_pause_backtalk);
  }

  private void addTellingTimeActionMenu(ContextMenu menu, SharedPreferences prefs) {
    menu.removeItem(R.id.disable_telling_time);
    menu.removeItem(R.id.enable_telling_time);

    if (!showMenuItem(
        R.string.pref_show_context_menu_control_telling_time_setting_key,
        R.bool.pref_show_context_menu_control_telling_time_default)) {
      return;
    }

    boolean enabledTellTime =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            service.getResources(),
            R.string.pref_speak_time_key,
            R.bool.pref_tell_time_default);
    if (enabledTellTime) {
      menu.add(
          /* groupId= */ 0,
          R.id.disable_telling_time,
          ORDER_TELL_TIME,
          R.string.shortcut_disable_speak_time);
    } else {
      menu.add(
          /* groupId= */ 0,
          R.id.enable_telling_time,
          ORDER_TELL_TIME,
          R.string.shortcut_enable_speak_time);
    }
  }

  // Adds menu item of window action to menu
  private void addWindowActionMenu(ContextMenu menu) {
    menu.removeItem(R.id.window_menu);

    if (!showMenuItem(
        R.string.pref_show_context_menu_system_action_setting_key,
        R.bool.pref_show_context_menu_system_action_default)) {
      return;
    }

    if (!FeatureSupport.supportGetSystemActions(service)
        || ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)) {
      return;
    }
    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.window_menu,
            ORDER_SYSTEM_ACTIONS,
            /* title= */ service.getString(R.string.system_actions));
    if (!WindowNavigationMenuProcessor.prepareWindowSubMenu(service, subMenu, pipeline)) {
      menu.removeItem(R.id.window_menu);
    }
  }

  private void addScriptCommands(ContextMenu menu) {
    for (ScriptMenuItem scriptItem : Scripts.menuItems()) {
      addAfterMenuCloses(
          menu, R.id.script_command, ORDER_SCRIPT_COMMANDS, scriptItem.getTitle(), scriptItem::run);
    }
  }

  private void addScriptTool(
      ContextMenu menu,
      int itemId,
      int order,
      @StringRes int showKey,
      @BoolRes int showDefault,
      @StringRes int title,
      BiConsumer<@Nullable AccessibilityNodeInfoCompat, Consumer<String>> tool) {
    menu.removeItem(itemId);
    if (showMenuItem(showKey, showDefault)) {
      addAfterMenuCloses(
          menu,
          itemId,
          order,
          service.getString(title),
          node ->
              tool.accept(
                  node,
                  message ->
                      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(message))));
    }
  }

  private void addAfterMenuCloses(
      ContextMenu menu,
      int itemId,
      int order,
      CharSequence title,
      Consumer<@Nullable AccessibilityNodeInfoCompat> action) {
    @Nullable AccessibilityNodeInfoCompat node = currentNode;
    ContextMenuItem item = menu.add(0, itemId, order, title);
    item.setOnMenuItemClickListener(
        clicked -> {
          action.accept(node);
          return true;
        });
    item.setDeferredType(DeferredType.WINDOWS_STABLE);
    item.setSkipRefocusEvents(true);
    item.setSkipWindowEvents(true);
  }

  private static void setMenuItemShowsDialog(ContextMenu menu, int itemId, boolean showsDialog) {
    @Nullable ContextMenuItem item = menu.findItemInMenuOrSubmenus(itemId);
    if (item != null) {
      item.setShowsAlertDialog(showsDialog);
    }
  }

  private static void setMenuItemDeferredType(
      ContextMenu menu, int itemId, DeferredType deferAction) {
    @Nullable ContextMenuItem item = menu.findItemInMenuOrSubmenus(itemId);
    if (item != null) {
      item.setDeferredType(deferAction);
    }
  }

  private static void setSkipRefocusAndWindowAnnounce(ContextMenu menu, int itemId, boolean skip) {
    @Nullable ContextMenuItem item = menu.findItemInMenuOrSubmenus(itemId);
    if (item != null) {
      item.setSkipRefocusEvents(skip);
      item.setSkipWindowEvents(skip);
    }
  }
}
