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

package com.google.android.accessibility.talkback.commands

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.GestureReporter
import com.google.android.accessibility.talkback.actor.ImageCaptioner
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration
import com.google.android.accessibility.talkback.flags.FeatureFlagReader
import com.google.android.accessibility.talkback.gesture.NavigationGestureActions
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut as Key
import com.google.android.accessibility.talkback.sandflyai.SandflyAi
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SettingsUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Every command that a gesture or a keyboard shortcut can run, in one list.
 *
 * A command is identified by its value, the string saved in a gesture preference. Commands that
 * only the keyboard code performs get the value [KEYBOARD_PREFIX] + the keyboard shortcut key, so a
 * gesture can run them too. Commands with a keyboard shortcut key of their own keep it, so their
 * default shortcuts and the shortcuts users set before still apply; any other command gets its
 * keyboard shortcut under [shortcutKey].
 */
object Commands {

  /** Prefix of the value of a command that the keyboard code performs. */
  const val KEYBOARD_PREFIX = "KEYCOMBO:"

  /** Prefix of the key combo model key that holds the keyboard shortcut of a command. */
  const val SHORTCUT_KEY_PREFIX = "command_shortcut_"

  /** A titled group of commands. */
  class Category(@StringRes val titleRes: Int, val commands: List<Command>)

  /**
   * One command. [keyboardAction] is the keyboard action that performs the same thing, if there is
   * one.
   */
  class Command(
    val value: String,
    val label: String,
    val keyboardAction: TalkBackPhysicalKeyboardShortcut?,
  ) {
    /** Returns the key under which the key combo model keeps this command's keyboard shortcut. */
    fun keyboardKey(context: Context): String =
      keyboardAction?.getKey(context.resources) ?: shortcutKey(value)
  }

  /** Returns the key combo model key for the keyboard shortcut of the command saved as [value]. */
  @JvmStatic fun shortcutKey(value: String): String = SHORTCUT_KEY_PREFIX + value

  /** Returns the command value kept under a key combo model [key], or null for other keys. */
  @JvmStatic
  fun valueOfShortcutKey(key: String?): String? =
    key?.takeIf { it.startsWith(SHORTCUT_KEY_PREFIX) }?.substring(SHORTCUT_KEY_PREFIX.length)

  /** Returns the value of the command performed by the keyboard [action]. */
  @JvmStatic
  fun keyboardValue(context: Context, action: TalkBackPhysicalKeyboardShortcut): String =
    KEYBOARD_PREFIX + action.getKey(context.resources)

  /** Returns the keyboard action that the command saved as [value] runs, or null. */
  @JvmStatic
  fun keyboardActionOf(context: Context, value: String?): TalkBackPhysicalKeyboardShortcut? {
    if (value == null || !value.startsWith(KEYBOARD_PREFIX)) {
      return null
    }
    val action =
      TalkBackPhysicalKeyboardShortcut.getActionFromKey(
        context.resources,
        value.substring(KEYBOARD_PREFIX.length),
      )
    return if (action == Key.ACTION_UNKNOWN) null else action
  }

  @Volatile private var keyboardActions: Map<String, TalkBackPhysicalKeyboardShortcut>? = null

  /**
   * Returns the keyboard action that runs the command saved as [value], whether the command has a
   * keyboard action of its own or only the keyboard code performs it, or null.
   */
  @JvmStatic
  fun keyboardActionFor(context: Context, value: String): TalkBackPhysicalKeyboardShortcut? {
    keyboardActionOf(context, value)?.let {
      return it
    }
    val actions =
      keyboardActions
        ?: categoriesWithoutApps(context)
          .flatMap { it.commands }
          .mapNotNull { command -> command.keyboardAction?.let { command.value to it } }
          .toMap()
          .also { keyboardActions = it }
    return actions[value]
  }

  @Volatile private var actionValues: Map<TalkBackPhysicalKeyboardShortcut, String>? = null

  /**
   * Returns the value of the command that the keyboard [action] runs: the command that has it as
   * its keyboard action, otherwise the keyboard action's own command.
   */
  @JvmStatic
  fun valueOfKeyboardAction(context: Context, action: TalkBackPhysicalKeyboardShortcut): String {
    val values =
      actionValues
        ?: categoriesWithoutApps(context)
          .flatMap { it.commands }
          .mapNotNull { command -> command.keyboardAction?.let { it to command.value } }
          .distinctBy { it.first }
          .toMap()
          .also { actionValues = it }
    return values[action] ?: keyboardValue(context, action)
  }

  /** Returns the commands available on this device, by category, ending with Open app. */
  @JvmStatic
  fun categories(context: Context): List<Category> = Builder(context, withApps = true).build()

  /** Returns the commands available on this device, by category, without Open app. */
  private fun categoriesWithoutApps(context: Context): List<Category> =
    Builder(context, withApps = false).build()

  /** Returns the command saved as [value], or null when it isn't one. */
  @JvmStatic
  fun find(context: Context, value: String?): Command? {
    if (value == null) {
      return null
    }
    OpenAppActions.label(context, value)?.let {
      return Command(value, it, keyboardAction = null)
    }
    categoriesWithoutApps(context).forEach { category ->
      category.commands.firstOrNull { it.value == value }?.let {
        return it
      }
    }
    return null
  }

  /** Returns the name of the command saved as [value], or null when it isn't one. */
  @JvmStatic
  fun label(context: Context, value: String?): String? {
    if (value == null) {
      return null
    }
    OpenAppActions.label(context, value)?.let {
      return it
    }
    NavigationGestureActions.label(context, value)?.let {
      return it
    }
    keyboardActionOf(context, value)?.let {
      return it.getDescription(context.resources)
    }
    return find(context, value)?.label
  }

  /**
   * Returns the name of the command whose keyboard shortcut is kept under the key combo model
   * [key], or null when no command has it.
   */
  @JvmStatic
  fun labelOfKeyboardKey(context: Context, key: String): String? {
    valueOfShortcutKey(key)?.let {
      return label(context, it)
    }
    categoriesWithoutApps(context).forEach { category ->
      category.commands.firstOrNull { it.keyboardAction?.getKey(context.resources) == key }?.let {
        return it.label
      }
    }
    val action = TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.resources, key)
    return if (action == Key.ACTION_UNKNOWN) null else action.getDescription(context.resources)
  }

  private class Builder(private val context: Context, private val withApps: Boolean) {
    private val categories = mutableListOf<Category>()
    private var commands = mutableListOf<Command>()

    private val wear = FormFactorUtils.isAndroidWear()

    fun build(): List<Category> {
      navigation()
      activation()
      scrolling()
      textNavigation()
      elementNavigation()
      tables()
      web()
      reading()
      speech()
      readingControls()
      textEditing()
      device()
      media()
      notifications()
      descriptions()
      braille()
      sandFly()
      developer()
      if (withApps) {
        openApp()
      }
      return categories
    }

    private fun category(@StringRes titleRes: Int, add: () -> Unit) {
      commands = mutableListOf()
      add()
      if (commands.isNotEmpty()) {
        categories.add(Category(titleRes, commands))
      }
    }

    /** Adds a gesture command whose value is [valueRes]. */
    private fun gesture(
      @StringRes valueRes: Int,
      @StringRes labelRes: Int,
      keyboard: TalkBackPhysicalKeyboardShortcut? = null,
      available: Boolean = true,
    ) {
      if (available) {
        commands.add(Command(context.getString(valueRes), context.getString(labelRes), keyboard))
      }
    }

    /** Adds a command only the keyboard code performs. */
    private fun keyboard(action: TalkBackPhysicalKeyboardShortcut, available: Boolean = true) {
      if (available) {
        commands.add(
          Command(
            keyboardValue(context, action),
            action.getDescription(context.resources),
            keyboardAction = null,
          )
        )
      }
    }

    /** Adds the navigation actions of [category], with the keyboard actions that match them. */
    private fun navigationActions(category: Int) {
      for (action in NavigationGestureActions.CATEGORIES[category].actions) {
        commands.add(
          Command(action.value, context.getString(action.labelRes), NAVIGATION_KEYS[action.value])
        )
      }
    }

    private fun navigation() =
      category(R.string.title_pref_navigation) {
        gesture(R.string.shortcut_value_next, R.string.shortcut_next, Key.NAVIGATE_NEXT_DEFAULT)
        gesture(
          R.string.shortcut_value_previous,
          R.string.shortcut_previous,
          Key.NAVIGATE_PREVIOUS_DEFAULT,
        )
        keyboard(Key.NAVIGATE_NEXT)
        keyboard(Key.NAVIGATE_PREVIOUS)
        keyboard(Key.NAVIGATE_BELOW)
        keyboard(Key.NAVIGATE_ABOVE)
        gesture(
          R.string.shortcut_value_first_in_screen,
          R.string.shortcut_first_in_screen,
          Key.NAVIGATE_FIRST,
        )
        gesture(
          R.string.shortcut_value_last_in_screen,
          R.string.shortcut_last_in_screen,
          Key.NAVIGATE_LAST,
        )
        gesture(
          R.string.shortcut_value_next_container,
          R.string.shortcut_next_container,
          Key.NAVIGATE_NEXT_CONTAINER,
        )
        gesture(
          R.string.shortcut_value_prev_container,
          R.string.shortcut_prev_container,
          Key.NAVIGATE_PREVIOUS_CONTAINER,
        )
        gesture(
          R.string.shortcut_value_next_window,
          R.string.shortcut_next_window,
          Key.NAVIGATE_NEXT_WINDOW,
        )
        gesture(
          R.string.shortcut_value_previous_window,
          R.string.shortcut_previous_window,
          Key.NAVIGATE_PREVIOUS_WINDOW,
        )
        keyboard(Key.REFOCUS_CURRENT_NODE)
      }

    private fun activation() =
      category(R.string.commands_category_activation) {
        gesture(
          R.string.shortcut_value_perform_click_action,
          R.string.shortcut_perform_click_action,
          Key.PERFORM_CLICK,
        )
        gesture(
          R.string.shortcut_value_perform_long_click_action,
          R.string.shortcut_perform_long_click_action,
          Key.PERFORM_LONG_CLICK,
        )
        gesture(
          R.string.shortcut_value_perform_double_click_action,
          R.string.shortcut_perform_double_click_action,
          Key.PERFORM_DOUBLE_CLICK,
          available = FeatureFlagReader.enableDoubleClickKeyboard(context),
        )
        gesture(
          R.string.shortcut_value_show_custom_actions,
          R.string.shortcut_show_custom_actions,
          Key.SHOW_ACTIONS,
        )
        gesture(
          R.string.shortcut_value_talkback_breakout,
          R.string.shortcut_talkback_breakout,
          Key.SHOW_GLOBAL_CONTEXT_MENU,
        )
        gesture(
          R.string.shortcut_value_pass_through_next_gesture,
          R.string.shortcut_pass_through_next,
          available = FeatureSupport.supportPassthrough(),
        )
        keyboard(Key.TOGGLE_KEY_COMBO_PASS_THROUGH_ONCE)
      }

    private fun scrolling() =
      category(R.string.commands_category_scrolling) {
        gesture(R.string.shortcut_value_scroll_forward, R.string.shortcut_scroll_forward)
        gesture(R.string.shortcut_value_scroll_back, R.string.shortcut_scroll_back)
        val directions = FeatureSupport.isMultiFingerGestureSupported() && !wear
        gesture(R.string.shortcut_value_scroll_up, R.string.shortcut_scroll_up, available = directions)
        gesture(
          R.string.shortcut_value_scroll_down,
          R.string.shortcut_scroll_down,
          available = directions,
        )
        gesture(
          R.string.shortcut_value_scroll_left,
          R.string.shortcut_scroll_left,
          available = directions,
        )
        gesture(
          R.string.shortcut_value_scroll_right,
          R.string.shortcut_scroll_right,
          available = directions,
        )
        gesture(R.string.shortcut_value_scroll_to_top, R.string.shortcut_scroll_to_top)
        gesture(R.string.shortcut_value_scroll_to_bottom, R.string.shortcut_scroll_to_bottom)
        // The pull passes through touch exploration, which needs Android 11.
        gesture(
          R.string.shortcut_value_refresh,
          R.string.shortcut_refresh,
          Key.REFRESH,
          available = FeatureSupport.supportPassthrough(),
        )
      }

    private fun textNavigation() =
      category(R.string.shortcut_title_navigate_by_text) { navigationActions(0) }

    private fun elementNavigation() =
      category(R.string.shortcut_title_navigate_by_element) {
        navigationActions(1)
        navigationActions(2)
      }

    private fun tables() =
      category(R.string.commands_category_tables) {
        val tableGestures = FeatureFlagReader.enableTableNavigation(context)
        gesture(R.string.shortcut_value_row_down, R.string.shortcut_row_down, available = tableGestures)
        gesture(R.string.shortcut_value_row_up, R.string.shortcut_row_up, available = tableGestures)
        gesture(
          R.string.shortcut_value_column_right,
          R.string.shortcut_column_right,
          available = tableGestures,
        )
        gesture(
          R.string.shortcut_value_column_left,
          R.string.shortcut_column_left,
          available = tableGestures,
        )
        keyboard(Key.NAVIGATE_NEXT_ROW)
        keyboard(Key.NAVIGATE_PREVIOUS_ROW)
        keyboard(Key.NAVIGATE_NEXT_COLUMN)
        keyboard(Key.NAVIGATE_PREVIOUS_COLUMN)
        keyboard(Key.NAVIGATE_PREVIOUS_ROW_BOUNDS)
        keyboard(Key.NAVIGATE_NEXT_ROW_BOUNDS)
        keyboard(Key.NAVIGATE_PREVIOUS_COLUMN_BOUNDS)
        keyboard(Key.NAVIGATE_NEXT_COLUMN_BOUNDS)
        keyboard(Key.NAVIGATE_PREVIOUS_TABLE_BOUNDS)
        keyboard(Key.NAVIGATE_NEXT_TABLE_BOUNDS)
        keyboard(Key.ANNOUNCE_ROW_HEADER)
        keyboard(Key.ANNOUNCE_COLUMN_HEADER)
      }

    private fun web() =
      category(R.string.commands_category_web) {
        gesture(
          R.string.shortcut_value_toggle_browse_mode,
          R.string.keycombo_menu_other_toggle_browse_mode,
          Key.TOGGLE_BROWSE_MODE,
        )
        keyboard(Key.TOGGLE_BROWSE_MODE_EXTRA)
        keyboard(Key.SHOW_HEADING_LIST)
        keyboard(Key.SHOW_LANDMARK_LIST)
        keyboard(Key.SHOW_LINK_LIST)
        keyboard(Key.SHOW_CONTROL_LIST)
        keyboard(Key.SHOW_TABLE_LIST)
        keyboard(Key.READ_LINK_URL)
        keyboard(Key.READ_CURRENT_URL)
      }

    private fun reading() =
      category(R.string.shortcut_title_reading_control) {
        gesture(
          R.string.shortcut_value_read_from_top,
          R.string.shortcut_read_from_top,
          Key.READ_FROM_TOP,
        )
        gesture(
          R.string.shortcut_value_read_from_current,
          R.string.shortcut_read_from_current,
          Key.READ_FROM_CURSOR_ITEM,
        )
        gesture(
          R.string.shortcut_value_repeat_last_spoken_phrase,
          R.string.title_repeat_last_spoken_phrase,
        )
        gesture(
          R.string.shortcut_value_spell_last_spoken_phrase,
          R.string.title_spell_last_spoken_phrase,
        )
        gesture(
          R.string.shortcut_value_translate_last_spoken_phrase,
          R.string.title_translate_last_spoken_phrase,
        )
        gesture(
          R.string.shortcut_value_announce_phonetic_pronunciation,
          R.string.shortcut_announce_phonetic_pronunciation,
          Key.ANNOUNCE_PHONETIC_PRONUNCIATION,
          available = FeatureFlagReader.enableAnnouncePhoneticPronunciation(context),
        )
        gesture(
          R.string.shortcut_value_describe_text_formatting,
          R.string.title_text_formatting_menu,
          Key.ANNOUNCE_RICH_TEXT_DESCRIPTION,
          available = FeatureFlagReader.enableTextFormattingSummary(context),
        )
        gesture(R.string.shortcut_value_announce_status, R.string.shortcut_announce_status)
        gesture(
          R.string.shortcut_value_announce_current_time_and_date,
          R.string.shortcut_announce_current_time_and_date,
          Key.ANNOUNCE_CURRENT_TIME_AND_DATE,
          available = FeatureFlagReader.enableAnnounceCurrentTimeAndDate(context),
        )
        gesture(
          R.string.shortcut_value_announce_battery_state,
          R.string.shortcut_announce_battery_state,
          Key.ANNOUNCE_BATTERY_STATE,
          available = FeatureFlagReader.enableAnnounceBatteryState(context),
        )
        gesture(
          R.string.shortcut_value_announce_current_title,
          R.string.shortcut_announce_current_title,
          Key.ANNOUNCE_CURRENT_TITLE,
          available = FeatureFlagReader.enableAnnounceCurrentTitle(context),
        )
        gesture(
          R.string.shortcut_value_control_telling_time,
          R.string.shortcut_control_speak_time,
          available = wear,
        )
      }

    private fun speech() =
      category(R.string.commands_category_speech) {
        gesture(
          R.string.shortcut_value_pause_or_resume_feedback,
          R.string.shortcut_pause_or_resume_feedback,
        )
        gesture(
          R.string.shortcut_value_toggle_voice_feedback,
          R.string.shortcut_toggle_voice_feedback,
          Key.TOGGLE_VOICE_FEEDBACK,
        )
        gesture(
          R.string.shortcut_value_speech_rate_increase,
          R.string.shortcut_speech_rate_increase,
          Key.SPEECH_RATE_INCREASE,
          available = FeatureFlagReader.enableSpeechRateKeyboard(context),
        )
        gesture(
          R.string.shortcut_value_speech_rate_decrease,
          R.string.shortcut_speech_rate_decrease,
          Key.SPEECH_RATE_DECREASE,
          available = FeatureFlagReader.enableSpeechRateKeyboard(context),
        )
        gesture(
          R.string.shortcut_value_speech_pitch_increase,
          R.string.shortcut_speech_pitch_increase,
          Key.SPEECH_PITCH_INCREASE,
          available = FeatureFlagReader.enableSpeechPitchKeyboard(context),
        )
        gesture(
          R.string.shortcut_value_speech_pitch_decrease,
          R.string.shortcut_speech_pitch_decrease,
          Key.SPEECH_PITCH_DECREASE,
          available = FeatureFlagReader.enableSpeechPitchKeyboard(context),
        )
        keyboard(Key.SPEECH_VOLUME_INCREASE)
        keyboard(Key.SPEECH_VOLUME_DECREASE)
        keyboard(Key.RESET_SPEECH_SETTINGS)
        gesture(
          R.string.shortcut_value_show_language_options,
          R.string.shortcut_show_language_options,
          Key.SHOW_LANGUAGES_AVAILABLE,
        )
        gesture(
          R.string.shortcut_value_voice_profiles,
          R.string.shortcut_voice_profiles,
          Key.SHOW_VOICE_PROFILES,
        )
        gesture(
          R.string.shortcut_value_next_voice_profile,
          R.string.shortcut_next_voice_profile,
          Key.NEXT_VOICE_PROFILE,
        )
        gesture(
          R.string.shortcut_value_previous_voice_profile,
          R.string.shortcut_previous_voice_profile,
          Key.PREVIOUS_VOICE_PROFILE,
        )
        keyboard(Key.CYCLE_PUNCTUATION_VERBOSITY)
        keyboard(Key.CYCLE_TYPING_ECHO)
        gesture(
          R.string.shortcut_value_control_text_formatting,
          R.string.title_switch_text_formatting,
          available = FeatureFlagReader.enableTextFormattingInline(context),
        )
        keyboard(Key.TOGGLE_SOUND_FEEDBACK)
        keyboard(Key.OPEN_TTS_SETTINGS)
      }

    private fun readingControls() =
      category(R.string.title_pref_category_selector_menu) {
        gesture(
          R.string.shortcut_value_select_next_setting,
          R.string.shortcut_select_next_setting,
          Key.SELECT_NEXT_READING_MENU,
        )
        gesture(
          R.string.shortcut_value_select_previous_setting,
          R.string.shortcut_select_previous_setting,
          Key.SELECT_PREVIOUS_READING_MENU,
        )
        gesture(
          R.string.shortcut_value_selected_setting_next_action,
          R.string.shortcut_selected_setting_next_action,
          Key.ADJUST_READING_MENU_DOWN,
        )
        gesture(
          R.string.shortcut_value_selected_setting_previous_action,
          R.string.shortcut_selected_setting_previous_action,
          Key.ADJUST_READING_MENU_UP,
        )
        gesture(
          R.string.shortcut_value_write_reading_control,
          R.string.shortcut_write_reading_control,
        )
        keyboard(Key.NEXT_NAVIGATION_SETTING)
        keyboard(Key.PREVIOUS_NAVIGATION_SETTING)
      }

    private fun textEditing() =
      category(R.string.shortcut_title_text_editing) {
        gesture(
          R.string.shortcut_value_start_selection_mode,
          R.string.title_edittext_breakout_start_selection_mode,
        )
        keyboard(Key.TOGGLE_SELECTION)
        gesture(
          R.string.shortcut_value_move_cursor_to_beginning,
          R.string.title_edittext_breakout_move_to_beginning,
        )
        gesture(
          R.string.shortcut_value_move_cursor_to_end,
          R.string.title_edittext_breakout_move_to_end,
        )
        gesture(R.string.shortcut_value_select_all, android.R.string.selectAll)
        gesture(R.string.shortcut_value_clear_selection, R.string.title_clear_selection)
        gesture(R.string.shortcut_value_copy, android.R.string.copy)
        gesture(R.string.shortcut_value_add_to_clipboard, R.string.shortcut_add_to_clipboard)
        gesture(R.string.shortcut_value_clipboard_history, R.string.title_clipboard_history)
        gesture(R.string.shortcut_value_cut, android.R.string.cut)
        gesture(R.string.shortcut_value_paste, android.R.string.paste)
        gesture(
          R.string.shortcut_value_copy_last_spoken_phrase,
          R.string.title_copy_last_spoken_phrase,
          Key.COPY_LAST_SPOKEN_PHRASE,
        )
        gesture(R.string.shortcut_value_granular_editing, R.string.granular_editing_title)
        gesture(R.string.shortcut_value_dictate, R.string.shortcut_dictate)
        gesture(
          R.string.shortcut_value_voice_input,
          R.string.shortcut_voice_input,
          available = FeatureFlagReader.enableVoiceDictationShortcut(context),
        )
      }

    private fun device() =
      category(R.string.shortcut_title_system_actions) {
        gesture(R.string.shortcut_value_home, R.string.shortcut_home, Key.HOME)
        gesture(R.string.shortcut_value_back, R.string.shortcut_back, Key.BACK)
        gesture(R.string.shortcut_value_overview, R.string.shortcut_overview, Key.RECENT_APPS)
        gesture(
          R.string.shortcut_value_notifications,
          R.string.shortcut_notifications,
          Key.NOTIFICATIONS,
        )
        gesture(
          R.string.shortcut_value_system_notifications,
          R.string.shortcut_system_notifications,
        )
        gesture(R.string.shortcut_value_quick_settings, R.string.shortcut_quick_settings)
        gesture(
          R.string.shortcut_value_system_quick_settings,
          R.string.shortcut_system_quick_settings,
        )
        gesture(R.string.shortcut_value_lock_screen, R.string.shortcut_lock_screen)
        gesture(R.string.shortcut_value_power_menu, R.string.shortcut_power_menu)
        val systemActions = FeatureSupport.supportGetSystemActions(context)
        gesture(R.string.shortcut_value_all_apps, R.string.shortcut_all_apps, available = systemActions)
        gesture(
          R.string.shortcut_value_a11y_button,
          R.string.shortcut_a11y_button,
          available = systemActions,
        )
        gesture(
          R.string.shortcut_value_a11y_button_long_press,
          R.string.shortcut_a11y_button_long_press,
          available = systemActions,
        )
        gesture(R.string.shortcut_value_write_app_name, R.string.shortcut_write_app_name)
        gesture(
          R.string.shortcut_value_voice_commands,
          R.string.shortcut_voice_commands,
          Key.OPEN_VOICE_COMMANDS,
          available = TalkbackFeatureSupport.supportSpeechRecognize(),
        )
      }

    private fun media() =
      category(R.string.commands_category_media) {
        val dictation = FeatureFlagReader.enableVoiceDictationShortcut(context)
        if (FeatureSupport.supportMediaControls()) {
          gesture(
            R.string.shortcut_value_media_control_or_voice_input,
            if (dictation) {
              R.string.shortcut_media_control_or_voice_input
            } else {
              R.string.shortcut_media_control
            },
            if (dictation) null else Key.PLAY_PAUSE_MEDIA,
          )
          gesture(
            R.string.shortcut_value_media_control,
            R.string.shortcut_media_control,
            Key.PLAY_PAUSE_MEDIA,
            available = dictation,
          )
        }
        gesture(R.string.shortcut_value_next_track, R.string.shortcut_next_track)
        gesture(R.string.shortcut_value_previous_track, R.string.shortcut_previous_track)
        gesture(R.string.shortcut_value_rewind, R.string.shortcut_rewind)
        gesture(R.string.shortcut_value_fast_forward, R.string.shortcut_fast_forward)
        gesture(R.string.shortcut_value_increase_volume, R.string.shortcut_increase_volume)
        gesture(R.string.shortcut_value_decrease_volume, R.string.shortcut_decrease_volume)
        gesture(
          R.string.shortcut_value_increase_media_volume,
          R.string.shortcut_increase_media_volume,
        )
        gesture(
          R.string.shortcut_value_decrease_media_volume,
          R.string.shortcut_decrease_media_volume,
        )
      }

    private fun notifications() =
      category(R.string.notification_category_title) {
        gesture(R.string.shortcut_value_read_notifications, R.string.shortcut_read_notifications)
        gesture(
          R.string.shortcut_value_read_last_notification,
          R.string.shortcut_read_last_notification,
        )
        gesture(R.string.shortcut_value_stop_notification, R.string.shortcut_stop_notification)
        gesture(
          R.string.shortcut_value_toggle_notification_speech,
          R.string.shortcut_toggle_notification_speech,
        )
      }

    private fun descriptions() =
      category(R.string.title_pref_image_and_screen_descriptions) {
        val captions = ImageCaptioner.supportsImageCaption(context)
        gesture(
          R.string.shortcut_value_describe_image,
          R.string.title_image_caption,
          Key.DESCRIBE_IMAGE,
          available = captions,
        )
        gesture(
          R.string.shortcut_value_screen_overview,
          R.string.title_summarize_view,
          Key.SCREEN_OVERVIEW,
          available = captions && GeminiConfiguration.screenOverviewEnabled(context),
        )
        // SandFly Pro's screen recognition commands, named by the module.
        SandflyAi.features?.let {
          for ((value, label) in it.commands(context)) {
            commands.add(Command(value, label, null))
          }
        }
      }

    private fun braille() =
      category(R.string.title_pref_braille) {
        val display = FeatureSupport.supportBrailleDisplay(context)
        gesture(
          R.string.shortcut_value_braille_keyboard,
          R.string.shortcut_braille_keyboard,
          available = FeatureSupport.supportSwitchToInputMethod() && !wear,
        )
        gesture(
          R.string.shortcut_value_toggle_braille_display,
          R.string.shortcut_toggle_braille_display,
          available = display && FeatureFlagReader.useGestureBrailleDisplayOnOff(context),
        )
        gesture(
          R.string.shortcut_value_braille_display_settings,
          R.string.shortcut_braille_display_settings,
          available = display,
        )
        keyboard(Key.TOGGLE_BRAILLE_CONTRACTED_MODE, available = display)
        keyboard(Key.TOGGLE_BRAILLE_ON_SCREEN_OVERLAY, available = display)
      }

    private fun sandFly() =
      category(R.string.commands_category_sandfly) {
        gesture(
          R.string.shortcut_value_pause_backtalk,
          R.string.title_pause_backtalk,
          Key.PAUSE_OR_RESUME_BACKTALK,
        )
        gesture(
          R.string.shortcut_value_show_hide_screen,
          R.string.title_show_hide_screen,
          Key.HIDE_OR_SHOW_SCREEN,
        )
        gesture(
          R.string.shortcut_value_screen_search,
          R.string.title_show_screen_search,
          Key.SEARCH_SCREEN_FOR_ITEM,
          available = !wear,
        )
        // SandFly AI names its own command, so its label is only in builds that include it.
        SandflyAi.features?.let {
          val value = context.getString(R.string.shortcut_value_ai_agent)
          commands.add(Command(value, it.agentCommandLabel(context), null))
        }
        gesture(R.string.shortcut_value_toggle_arrow_key_nav, R.string.arrow_key_nav_toggle)
        gesture(R.string.shortcut_value_toggle_scripts, R.string.shortcut_toggle_scripts)
        keyboard(Key.OPEN_TALKBACK_SETTINGS)
        gesture(R.string.shortcut_value_tutorial, R.string.shortcut_tutorial, Key.SHOW_TUTORIAL)
        gesture(
          R.string.shortcut_value_practice_gestures,
          R.string.shortcut_practice_gestures,
          available = !wear,
        )
        keyboard(
          Key.SHOW_LEARN_MODE_PAGE,
          available = FeatureFlagReader.enableKeyboardLearnModePage(context),
        )
        keyboard(Key.SHOW_KEYBOARD_SHORTCUTS_DIALOG)
        keyboard(Key.REPORT_ISSUE)
      }

    private fun developer() =
      category(R.string.title_pref_category_developer_settings) {
        val prefs = SharedPreferencesUtils.getSharedPreferences(context)
        gesture(R.string.shortcut_value_inspect_item, R.string.shortcut_inspect_item)
        gesture(R.string.shortcut_value_copy_screen_tree, R.string.shortcut_copy_screen_tree)
        gesture(
          R.string.shortcut_value_print_node_tree,
          R.string.shortcut_print_node_tree,
          available = prefs.getBoolean(context.getString(R.string.pref_tree_debug_key), false),
        )
        gesture(
          R.string.shortcut_value_print_performance_stats,
          R.string.shortcut_print_performance_stats,
          available =
            prefs.getBoolean(context.getString(R.string.pref_performance_stats_key), false),
        )
        gesture(
          R.string.shortcut_value_report_gesture,
          R.string.shortcut_report_gesture,
          available = GestureReporter.ENABLED && SettingsUtils.allowLinksOutOfSettings(context),
        )
      }

    private fun openApp() =
      category(R.string.shortcut_title_open_app) {
        for (app in OpenAppActions.apps(context)) {
          commands.add(Command(app.value, app.label, keyboardAction = null))
        }
      }
  }

  /** The keyboard actions that move to the same items as navigation gesture actions. */
  private val NAVIGATION_KEYS: Map<String, TalkBackPhysicalKeyboardShortcut> =
    buildMap {
      fun pair(name: String, keyName: String = name) {
        put("NEXT_$name", Key.valueOf("NAVIGATE_NEXT_$keyName"))
        put("PREVIOUS_$name", Key.valueOf("NAVIGATE_PREVIOUS_$keyName"))
      }
      listOf(
          "CHARACTER",
          "WORD",
          "LINE",
          "PARAGRAPH",
          "HEADING",
          "LINK",
          "CONTROL",
          "BUTTON",
          "CHECKBOX",
          "RADIO",
          "EDIT_FIELD",
          "COMBOBOX",
          "FOCUSABLE_ITEM",
          "GRAPHIC",
          "LIST",
          "LIST_ITEM",
          "TABLE",
          "VISITED_LINK",
          "UNVISITED_LINK",
          "HEADING_1",
          "HEADING_2",
          "HEADING_3",
          "HEADING_4",
          "HEADING_5",
          "HEADING_6",
        )
        .forEach { pair(it) }
      pair("LANDMARK", "ARIA_LANDMARK")
    }
}
