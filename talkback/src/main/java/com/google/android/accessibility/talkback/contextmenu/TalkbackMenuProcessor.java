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
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.DimScreenActor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.clipboard.ClipboardHistoryOverlay;
import com.google.android.accessibility.talkback.commands.CommandMenus;
import com.google.android.accessibility.talkback.commands.Commands;
import com.google.android.accessibility.talkback.commands.MenuCommands;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem.DeferredType;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.keyboardecho.KeyboardEchoSettings;
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleProcessor;
import com.google.android.accessibility.talkback.scripting.ItemInspector;
import com.google.android.accessibility.talkback.scripting.ScriptMenuItem;
import com.google.android.accessibility.talkback.sandflyai.SandflyAi;
import com.google.android.accessibility.talkback.sandflyai.SandflyAiFeatures;
import com.google.android.accessibility.talkback.scripting.Scripts;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import kotlin.Pair;
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
  // Shares Emoji's order too, and is added between Emoji and Voice profile to match their titles.
  private static final int ORDER_GESTURE_PROFILES = 15;
  // Shares Verbosity's order.
  private static final int ORDER_KEYBOARD_ECHO = 13;
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
  // The switches and item IDs of the items in the menu being made.
  private final Set<String> wantedShowKeys = new HashSet<>();
  private final Set<Integer> wantedItemIds = new HashSet<>();

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
   * Fills {@code target} with the commands and submenus {@code nodes}, in their order.
   *
   * @param target The menu to populate.
   * @param nodes The items of the menu.
   * @return {@code true} if successful, {@code false} otherwise.
   */
  public boolean prepareMenu(ContextMenu target, List<CommandMenus.Node> nodes) {
    // Apply attributes to menu items.
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    List<CommandMenus.Node> shown = CommandMenus.expanded(prefs, nodes);
    Set<String> commands = CommandMenus.commandsIn(shown);
    // The items with behavior of their own, made first and then placed where the menu has them.
    ContextMenu menu = new ContextMenu(service);
    menu.setDefaultListener(target.getDefaultListener());
    wantedShowKeys.clear();
    wantedItemIds.clear();
    Map<Integer, String> commandByItemId = new HashMap<>();
    Set<String> ownItem = new HashSet<>();
    for (MenuCommands.Row row : MenuCommands.rows(service)) {
      ownItem.add(row.getValue());
      if (!commands.contains(row.getValue())) {
        continue;
      }
      if (row.getShowKey() != 0) {
        wantedShowKeys.add(service.getString(row.getShowKey()));
      }
      for (int id : row.getIds()) {
        wantedItemIds.add(id);
        commandByItemId.put(id, row.getValue());
      }
    }

    // Typo suggestions
    addItemOrSubMenuForCurrentNode(
        menu,
        R.id.typo_suggestions_menu,
        R.string.title_edittext_typo_suggestions,
        ORDER_TYPO_SUGGESTIONS);
    // Custom Action
    addItemOrSubMenuForCurrentNode(
        menu, R.id.custom_action_menu, R.string.title_custom_action, ORDER_ACTIONS);
    // Addresses and numbers, after Actions, whose order it shares.
    addItemOrSubMenuForCurrentNode(
        menu, R.id.text_entities_menu, R.string.title_text_entities, ORDER_ACTIONS);
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

    // Keyboard echo, added before context_menu.xml so that it comes before Verbosity, as it does
    // in the menu settings, which sort items with the same order by title.
    addKeyboardEchoMenuIfValid(
        menu,
        TypingInput.ON_SCREEN_KEYBOARD,
        R.id.keyboard_echo_on_screen_menu,
        R.string.pref_show_context_menu_keyboard_echo_on_screen_setting_key);
    addKeyboardEchoMenuIfValid(
        menu,
        TypingInput.PHYSICAL_KEYBOARD,
        R.id.keyboard_echo_physical_menu,
        R.string.pref_show_context_menu_keyboard_echo_physical_setting_key);
    addKeyboardEchoMenuIfValid(
        menu,
        TypingInput.BRAILLE_KEYBOARD,
        R.id.keyboard_echo_braille_keyboard_menu,
        R.string.pref_show_context_menu_keyboard_echo_braille_keyboard_setting_key);
    addKeyboardEchoMenuIfValid(
        menu,
        TypingInput.BRAILLE_DISPLAY,
        R.id.keyboard_echo_braille_display_menu,
        R.string.pref_show_context_menu_keyboard_echo_braille_display_setting_key);
    addKeyboardEchoMenuIfValid(
        menu,
        TypingInput.HANDWRITING_KEYBOARD,
        R.id.keyboard_echo_handwriting_keyboard_menu,
        R.string.pref_show_context_menu_keyboard_echo_handwriting_keyboard_setting_key);
    // Read From & Last Phrase Spoken & screen search at context_menu.xml
    addContextMenuXmlMenu(menu);

    // Show/hide screen
    addDimOrBrightenScreen(menu);
    // Pause Backtalk
    addPauseBacktalk(menu);
    addTurnOff(menu);
    // Tell time
    addTellingTimeActionMenu(menu, prefs);
    // Language
    addLanguageMenuIfValid(menu);
    addEmojiMenuIfValid(menu);
    // Gesture profile and voice profile
    addGestureProfileMenuIfValid(menu);
    addVoiceProfileMenuIfValid(menu);
    addSplitScreenMenuIfValid(menu);
    // System Action
    addWindowActionMenu(menu);
    if (wantedItemIds.contains(R.id.script_command)) {
      addScriptCommands(menu);
    }
    addSandflyProItems(menu);
    addScriptTool(
        menu,
        R.id.inspect_item,
        ORDER_INSPECT_ITEM,
        R.string.pref_show_context_menu_inspect_item_setting_key,
        R.string.shortcut_inspect_item,
        (node, say) -> say.accept(ItemInspector.inspect(service, node)));
    addScriptTool(
        menu,
        R.id.copy_screen_tree,
        ORDER_COPY_SCREEN_TREE,
        R.string.pref_show_context_menu_copy_screen_tree_setting_key,
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

    // The confirmation shows once the menu has closed.
    setMenuItemShowsDialog(menu, R.id.turn_off_sandfly, true);
    setMenuItemDeferredType(menu, R.id.turn_off_sandfly, DeferredType.WINDOWS_STABLE);

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
    setMenuItemShowsDialog(menu, R.id.select_last_spoken_phrase_word_by_word, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.select_last_spoken_phrase_word_by_word, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.audio_ducking, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.sound_feedback, true);
    setSkipRefocusAndWindowAnnounce(menu, R.id.vibration_feedback, true);

    Map<String, List<ContextMenuItem>> items = new HashMap<>();
    for (ContextMenuItem item : menu.getItems()) {
      String command = commandByItemId.get(item.getItemId());
      if (command != null) {
        items.computeIfAbsent(command, unused -> new ArrayList<>()).add(item);
      }
    }
    List<String> plain = new ArrayList<>(commands);
    plain.removeAll(ownItem);
    List<String> labels = Commands.labels(service, plain);
    Map<String, String> plainLabels = new HashMap<>();
    for (int i = 0; i < plain.size(); i++) {
      if (labels.get(i) != null) {
        plainLabels.put(plain.get(i), labels.get(i));
      }
    }
    addNodes(target, shown, items, plainLabels);
    return true;
  }

  /** Adds {@code nodes} to {@code target}: submenus left empty on this screen are left out. */
  private void addNodes(
      ContextMenu target,
      List<CommandMenus.Node> nodes,
      Map<String, List<ContextMenuItem>> items,
      Map<String, String> plainLabels) {
    for (CommandMenus.Node node : nodes) {
      if (node instanceof CommandMenus.Submenu submenu) {
        ListSubMenu subMenu =
            target.addSubMenu(
                /* groupId= */ 0, R.id.command_submenu, Menu.NONE, submenu.getName());
        subMenu.setDefaultListener(target.getDefaultListener());
        subMenu.getItem().setOnMenuItemClickListener(target.getDefaultListener());
        addNodes(subMenu, submenu.getChildren(), items, plainLabels);
        if (subMenu.size() == 0) {
          target.remove(subMenu.getItem());
        }
        continue;
      }
      String command = ((CommandMenus.Command) node).getValue();
      List<ContextMenuItem> own = items.get(command);
      if (own != null) {
        own.forEach(target::add);
      } else if (plainLabels.containsKey(command)) {
        addPlainCommand(target, command, plainLabels.get(command));
      }
    }
  }

  /** Adds an item that runs {@code command} once the menu has closed and focus is back. */
  private void addPlainCommand(ContextMenu target, String command, String label) {
    ContextMenuItem item = target.add(/* groupId= */ 0, R.id.command_item, Menu.NONE, label);
    item.setOnMenuItemClickListener(
        clicked -> {
          service.performCommand(command);
          return true;
        });
    item.setNeedRestoreFocus(true);
    item.setDeferredType(
        currentNode == null
            ? DeferredType.WINDOWS_STABLE
            : DeferredType.ACCESSIBILITY_FOCUS_RECEIVED);
    item.setSkipRefocusEvents(true);
    item.setSkipWindowEvents(true);
  }

  private boolean showMenuItem(@StringRes int prefKeyId) {
    return wantedShowKeys.contains(service.getString(prefKeyId));
  }

  // Adds context_menu.xml, such as read from, last spoken, find on screen sub menu, .., to menu
  private void addContextMenuXmlMenu(ContextMenu menu) {
    new MenuInflater(service).inflate(R.menu.context_menu, menu);

    // Removes talkback TTS settings if phone is locked, Setup Wizard doesn't complete or uncheck
    // show item.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || !showMenuItem(R.string.pref_show_context_menu_tts_settings_setting_key)) {
      menu.removeItem(R.id.tts_settings);
    }

    if (!showMenuItem(R.string.pref_show_context_menu_read_from_top_setting_key)) {
      menu.removeItem(R.id.read_from_top);
    }

    if (!showMenuItem(R.string.pref_show_context_menu_read_from_current_setting_key)) {
      menu.removeItem(R.id.read_from_current);
    }

    if (!showMenuItem(R.string.pref_show_context_menu_copy_last_spoken_phrase_setting_key)) {
      menu.removeItem(R.id.copy_last_utterance_to_clipboard);
    }

    if (!showMenuItem(
        R.string.pref_show_context_menu_select_last_spoken_phrase_word_by_word_setting_key)) {
      menu.removeItem(R.id.select_last_spoken_phrase_word_by_word);
    }

    if (!showMenuItem(R.string.pref_show_context_menu_spell_last_spoken_phrase_setting_key)) {
      menu.removeItem(R.id.spell_last_utterance);
    }

    if (!showMenuItem(R.string.pref_show_context_menu_repeat_last_spoken_phrase_setting_key)) {
      menu.removeItem(R.id.repeat_last_utterance);
    }

    // Removes screen search if this is watch or uncheck show item.
    if (FormFactorUtils.isAndroidWear()
        || !showMenuItem(R.string.pref_show_context_menu_find_on_screen_setting_key)) {
      menu.removeItem(R.id.screen_search);
    }

    // Removes voice command if phone is locked, Setup Wizard doesn't complete or uncheck show item,
    // or in the Wear build.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || FormFactorUtils.isAndroidWear()
        || FormFactorUtils.isAndroidXr()
        || !showMenuItem(R.string.pref_show_context_menu_voice_commands_setting_key)) {
      menu.removeItem(R.id.voice_commands);
    }

    // Removes talkback settings if phone is locked, Setup Wizard does not complete or uncheck show
    // item.
    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || !showMenuItem(R.string.pref_show_context_menu_talkback_settings_setting_key)) {
      menu.removeItem(R.id.talkback_settings);
    }

    // The history can hold private text, so it isn't shown on the lock screen.
    if (ScreenMonitor.isDeviceLocked(service)
        || !showMenuItem(R.string.pref_show_context_menu_clipboard_history_setting_key)) {
      menu.removeItem(R.id.clipboard_history);
    } else {
      @Nullable ContextMenuItem clipboardHistory = menu.findItem(R.id.clipboard_history);
      if (clipboardHistory != null) {
        // Found before the menu opens: the open menu has both focuses.
        @Nullable AccessibilityNodeInfo editBox = ClipboardHistoryOverlay.editBoxWithFocus(service);
        clipboardHistory.setOnMenuItemClickListener(
            clicked -> ClipboardHistoryOverlay.open(service, editBox));
        // Opens once the menu is closed and focus is back on the screen under it.
        clipboardHistory.setDeferredType(DeferredType.WINDOWS_STABLE);
      }
    }

    if (ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || !showMenuItem(R.string.pref_show_context_menu_send_feedback_setting_key)) {
      menu.removeItem(R.id.send_feedback);
    }

    // Removes braille display settings if braille display isn't supported, phone is locked, Setup
    // Wizard isn't completed or the preference of showing item is unchecked.
    if (!FeatureSupport.supportBrailleDisplay(service)
        || ScreenMonitor.isDeviceLocked(service)
        || !SettingsUtils.allowLinksOutOfSettings(service)
        || FormFactorUtils.isAndroidXr()
        || !showMenuItem(R.string.pref_show_context_menu_braille_display_settings_setting_key)) {
      menu.removeItem(R.id.braille_display_settings);
    }

    if (!FeatureFlagReader.enableShowKeyboardShortcutsDialog(service)
        || !showMenuItem(R.string.pref_show_context_menu_show_keyboard_shortcuts_setting_key)) {
      menu.removeItem(R.id.show_keyboard_shortcuts);
    }

    if (ScreenMonitor.isDeviceLocked(service)
        || !showMenuItem(R.string.pref_show_context_menu_verbosity_setting_key)) {
      menu.removeItem(R.id.verbosity);
    }

    if (showMenuItem(R.string.pref_show_context_menu_audio_ducking_setting_key)) {
      computeTitleForMenuItem(
          menu.findItem(R.id.audio_ducking),
          R.string.audio_focus_state,
          R.string.pref_use_audio_focus_key,
          R.bool.pref_use_audio_focus_default);
    } else {
      menu.removeItem(R.id.audio_ducking);
    }

    if (showMenuItem(R.string.pref_show_context_menu_sound_feedback_setting_key)) {
      computeTitleForMenuItem(
          menu.findItem(R.id.sound_feedback),
          R.string.sound_feedback_state,
          R.string.pref_soundback_key,
          R.bool.pref_soundback_default);
    } else {
      menu.removeItem(R.id.sound_feedback);
    }

    if (FeatureSupport.isVibratorSupported(service)
        && showMenuItem(R.string.pref_show_context_menu_vibration_feedback_setting_key)) {
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

    if (!wantedItemIds.contains(itemId)) {
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

    if (!showMenuItem(R.string.pref_show_context_menu_language_setting_key)) {
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

  private void addSplitScreenMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.split_screen_menu);
    if (!showMenuItem(R.string.pref_show_context_menu_split_screen_setting_key)) {
      return;
    }
    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.split_screen_menu,
            ORDER_SYSTEM_ACTIONS,
            service.getString(R.string.title_split_screen));
    if (!SplitScreenMenuProcessor.prepareSplitScreenSubMenu(service, pipeline, subMenu)) {
      menu.removeItem(R.id.split_screen_menu);
    }
  }

  private void addVoiceProfileMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.voice_profile_menu);

    if (!showMenuItem(R.string.pref_show_context_menu_voice_profile_setting_key)) {
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

  private void addGestureProfileMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.gesture_profile_menu);

    if (!showMenuItem(R.string.pref_show_context_menu_gesture_profile_setting_key)) {
      return;
    }

    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            /* itemId= */ R.id.gesture_profile_menu,
            ORDER_GESTURE_PROFILES,
            service.getString(R.string.title_gesture_profile));
    // Shown only once there is a profile to choose besides Default.
    if (!GestureProfileMenuProcessor.prepareGestureProfileSubMenu(service, pipeline, subMenu)) {
      menu.removeItem(R.id.gesture_profile_menu);
    }
  }

  /** Adds a sub menu to choose how emoji are read, titled with the current choice. */
  private void addEmojiMenuIfValid(ContextMenu menu) {
    menu.removeItem(R.id.emoji_menu);
    if (!showMenuItem(R.string.pref_show_context_menu_emoji_setting_key)) {
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

  /**
   * Adds a sub menu to choose the keyboard echo for {@code input}, titled with the current choice.
   * Watches have only the on-screen keyboard's.
   */
  private void addKeyboardEchoMenuIfValid(
      ContextMenu menu, TypingInput input, @IdRes int menuId, @StringRes int prefKeyId) {
    menu.removeItem(menuId);
    if (!showMenuItem(prefKeyId)
        || (FormFactorUtils.isAndroidWear() && input != TypingInput.ON_SCREEN_KEYBOARD)) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(service);
    Resources res = service.getResources();
    ListSubMenu subMenu =
        menu.addSubMenu(
            /* groupId= */ 0,
            menuId,
            ORDER_KEYBOARD_ECHO,
            KeyboardEchoSettings.state(res, input, KeyboardEchoSettings.read(prefs, res, input)));
    for (int echo : KeyboardEchoSettings.choices(res)) {
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              service,
              R.id.group_keyboard_echo,
              Menu.NONE,
              Menu.NONE,
              KeyboardEchoSettings.choiceName(res, echo));
      item.setOnMenuItemClickListener(
          (OnContextMenuItemClickListener)
              clicked -> {
                KeyboardEchoSettings.write(prefs, res, input, echo);
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        KeyboardEchoSettings.state(res, input, echo),
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

    if (!showMenuItem(R.string.pref_show_context_menu_dim_or_brighten_setting_key)) {
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
    if (!showMenuItem(R.string.pref_show_context_menu_pause_backtalk_setting_key)) {
      return;
    }

    menu.add(
        /* groupId= */ 0, R.id.pause_backtalk, ORDER_PAUSE_BACKTALK, R.string.title_pause_backtalk);
  }

  private void addTurnOff(ContextMenu menu) {
    menu.removeItem(R.id.turn_off_sandfly);
    if (!showMenuItem(R.string.pref_show_context_menu_turn_off_setting_key)) {
      return;
    }
    // Shares Pause SandFly's order, and is added after it.
    menu.add(
        /* groupId= */ 0, R.id.turn_off_sandfly, ORDER_PAUSE_BACKTALK, R.string.turn_off_talkback);
  }

  private void addTellingTimeActionMenu(ContextMenu menu, SharedPreferences prefs) {
    menu.removeItem(R.id.disable_telling_time);
    menu.removeItem(R.id.enable_telling_time);

    if (!showMenuItem(R.string.pref_show_context_menu_control_telling_time_setting_key)) {
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

    if (!showMenuItem(R.string.pref_show_context_menu_system_action_setting_key)) {
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

  private void addSandflyProItems(ContextMenu menu) {
    addSandflyProItem(
        menu,
        R.id.recognize_screen_menu,
        R.string.pref_show_context_menu_recognize_screen_setting_key,
        SandflyAi.COMMAND_RECOGNIZE_SCREEN);
    addSandflyProItem(
        menu,
        R.id.screen_recognition_app_menu,
        R.string.pref_show_context_menu_screen_recognition_app_setting_key,
        SandflyAi.COMMAND_SCREEN_RECOGNITION_IN_APP);
  }

  private void addSandflyProItem(
      ContextMenu menu, int itemId, @StringRes int showKey, String command) {
    SandflyAiFeatures features = SandflyAi.getFeatures();
    if (features == null || !showMenuItem(showKey)) {
      return;
    }
    @Nullable Pair<CharSequence, Runnable> item = features.menuItem(service, command);
    if (item != null) {
      addAfterMenuCloses(
          menu, itemId, ORDER_IMAGE_CAPTION, item.getFirst(), node -> item.getSecond().run());
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
      @StringRes int title,
      BiConsumer<@Nullable AccessibilityNodeInfoCompat, Consumer<String>> tool) {
    menu.removeItem(itemId);
    if (showMenuItem(showKey)) {
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
