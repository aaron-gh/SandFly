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
import androidx.annotation.BoolRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut as Key
import com.google.android.accessibility.talkback.sandflyai.SandflyAi

/**
 * The commands that have their own item in a menu, built by TalkbackMenuProcessor, in the default
 * order of the SandFly menu. Such an item can show a current state, hold a submenu or depend on the
 * focused item. Every other command gets a plain item that runs it.
 */
object MenuCommands {
  /**
   * One command with a menu item of its own. [key] names it in the saved order of the SandFly menu.
   * [showKey] is the switch that held whether it was in the SandFly menu, 0 when there is none.
   * [ids] are the IDs of the menu items that perform it.
   */
  class Row(
    val key: String,
    @StringRes val showKey: Int,
    @BoolRes val showDefault: Int,
    val ids: IntArray,
    val value: String,
    val menuOnly: Boolean,
  )

  private const val SCRIPT_COMMANDS_KEY = "script_commands"

  @Volatile private var cached: List<Row>? = null

  /** Returns every row, in the default order of the SandFly menu. */
  @JvmStatic fun rows(context: Context): List<Row> = cached ?: build(context).also { cached = it }

  /** Returns the keys of the rows, in the default order of the SandFly menu. */
  @JvmStatic fun defaultOrderKeys(context: Context): List<String> = rows(context).map { it.key }

  /** Returns whether only a menu item performs the command saved as [value]. */
  @JvmStatic
  fun isMenuOnly(context: Context, value: String): Boolean =
    rows(context).any { it.menuOnly && it.value == value }

  /** Returns the value of the command whose switch was [showKey], or null. */
  fun valueOfShowKey(context: Context, showKey: String): String? =
    rows(context).firstOrNull { it.showKey != 0 && context.getString(it.showKey) == showKey }?.value

  private fun build(context: Context): List<Row> {
    val rows = ArrayList<Row>()
    fun add(
      @StringRes showKey: Int,
      @BoolRes showDefault: Int,
      value: String,
      menuOnly: Boolean,
      @IdRes vararg ids: Int,
    ) {
      rows += Row(context.getString(showKey), showKey, showDefault, ids, value, menuOnly)
    }
    fun command(
      @StringRes showKey: Int,
      @BoolRes showDefault: Int,
      @StringRes value: Int,
      @IdRes vararg ids: Int,
    ) = add(showKey, showDefault, context.getString(value), false, *ids)
    fun menuOnly(
      @StringRes showKey: Int,
      @BoolRes showDefault: Int,
      @StringRes value: Int,
      @IdRes vararg ids: Int,
    ) = add(showKey, showDefault, context.getString(value), true, *ids)
    fun keyboard(@StringRes showKey: Int, @BoolRes showDefault: Int, action: Key, @IdRes id: Int) =
      add(showKey, showDefault, Commands.keyboardValue(context, action), false, id)

    menuOnly(
      R.string.pref_show_context_menu_typo_suggestions_setting_key,
      R.bool.pref_show_context_menu_typo_suggestions_default,
      R.string.shortcut_value_typo_suggestions,
      R.id.typo_suggestions_menu,
    )
    command(
      R.string.pref_show_context_menu_custom_action_setting_key,
      R.bool.pref_show_context_menu_custom_action_default,
      R.string.shortcut_value_show_custom_actions,
      R.id.custom_action_menu,
    )
    command(
      R.string.pref_show_context_menu_text_entities_setting_key,
      R.bool.pref_show_context_menu_text_entities_default,
      R.string.shortcut_value_text_entities,
      R.id.text_entities_menu,
    )
    rows +=
      Row(
        SCRIPT_COMMANDS_KEY,
        0,
        0,
        intArrayOf(R.id.script_command),
        context.getString(R.string.shortcut_value_script_commands),
        true,
      )
    menuOnly(
      R.string.pref_show_context_menu_page_navigation_setting_key,
      R.bool.pref_show_context_menu_page_navigation_default,
      R.string.shortcut_value_page_navigation,
      R.id.viewpager_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_labeling_setting_key,
      R.bool.pref_show_context_menu_labeling_default,
      R.string.shortcut_value_label,
      R.id.labeling_breakout_add_label,
      R.id.labeling_breakout_edit_label,
    )
    command(
      R.string.pref_show_context_menu_image_caption_setting_key,
      R.bool.pref_show_context_menu_image_caption_default,
      R.string.shortcut_value_describe_image,
      R.id.image_caption_menu,
    )
    command(
      R.string.pref_show_context_menu_summarize_view_setting_key,
      R.bool.pref_show_context_menu_summarize_view_default,
      R.string.shortcut_value_screen_overview,
      R.id.summarize_view_menu,
    )
    add(
      R.string.pref_show_context_menu_recognize_screen_setting_key,
      R.bool.pref_show_context_menu_recognize_screen_default,
      SandflyAi.COMMAND_RECOGNIZE_SCREEN,
      false,
      R.id.recognize_screen_menu,
    )
    add(
      R.string.pref_show_context_menu_screen_recognition_app_setting_key,
      R.bool.pref_show_context_menu_screen_recognition_app_default,
      SandflyAi.COMMAND_SCREEN_RECOGNITION_IN_APP,
      false,
      R.id.screen_recognition_app_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_granularity_setting_key,
      R.bool.pref_show_context_menu_granularity_default,
      R.string.shortcut_value_granularity_menu,
      R.id.granularity_menu,
    )
    command(
      R.string.pref_show_context_menu_read_from_top_setting_key,
      R.bool.pref_show_context_menu_read_from_top_default,
      R.string.shortcut_value_read_from_top,
      R.id.read_from_top,
    )
    command(
      R.string.pref_show_context_menu_read_from_current_setting_key,
      R.bool.pref_show_context_menu_read_from_current_default,
      R.string.shortcut_value_read_from_current,
      R.id.read_from_current,
    )
    command(
      R.string.pref_show_context_menu_copy_last_spoken_phrase_setting_key,
      R.bool.pref_show_context_menu_copy_last_spoken_phrase_default,
      R.string.shortcut_value_copy_last_spoken_phrase,
      R.id.copy_last_utterance_to_clipboard,
    )
    command(
      R.string.pref_show_context_menu_spell_last_spoken_phrase_setting_key,
      R.bool.pref_show_context_menu_spell_last_spoken_phrase_default,
      R.string.shortcut_value_spell_last_spoken_phrase,
      R.id.spell_last_utterance,
    )
    command(
      R.string.pref_show_context_menu_repeat_last_spoken_phrase_setting_key,
      R.bool.pref_show_context_menu_repeat_last_spoken_phrase_default,
      R.string.shortcut_value_repeat_last_spoken_phrase,
      R.id.repeat_last_utterance,
    )
    command(
      R.string.pref_show_context_menu_text_formatting_setting_key,
      R.bool.pref_show_context_menu_text_formatting_default,
      R.string.shortcut_value_describe_text_formatting,
      R.id.text_formatting,
    )
    menuOnly(
      R.string.pref_show_context_menu_keyboard_echo_on_screen_setting_key,
      R.bool.pref_show_context_menu_keyboard_echo_default,
      R.string.shortcut_value_keyboard_echo_on_screen,
      R.id.keyboard_echo_on_screen_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_keyboard_echo_physical_setting_key,
      R.bool.pref_show_context_menu_keyboard_echo_default,
      R.string.shortcut_value_keyboard_echo_physical,
      R.id.keyboard_echo_physical_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_keyboard_echo_braille_keyboard_setting_key,
      R.bool.pref_show_context_menu_keyboard_echo_default,
      R.string.shortcut_value_keyboard_echo_braille_keyboard,
      R.id.keyboard_echo_braille_keyboard_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_keyboard_echo_braille_display_setting_key,
      R.bool.pref_show_context_menu_keyboard_echo_default,
      R.string.shortcut_value_keyboard_echo_braille_display,
      R.id.keyboard_echo_braille_display_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_keyboard_echo_handwriting_keyboard_setting_key,
      R.bool.pref_show_context_menu_keyboard_echo_default,
      R.string.shortcut_value_keyboard_echo_handwriting,
      R.id.keyboard_echo_handwriting_keyboard_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_verbosity_setting_key,
      R.bool.pref_show_context_menu_verbosity_default,
      R.string.shortcut_value_verbosity_settings,
      R.id.verbosity,
    )
    command(
      R.string.pref_show_context_menu_language_setting_key,
      R.bool.pref_show_context_menu_language_default,
      R.string.shortcut_value_show_language_options,
      R.id.language_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_emoji_setting_key,
      R.bool.pref_show_context_menu_emoji_default,
      R.string.shortcut_value_emoji_menu,
      R.id.emoji_menu,
    )
    command(
      R.string.pref_show_context_menu_gesture_profile_setting_key,
      R.bool.pref_show_context_menu_gesture_profile_default,
      R.string.shortcut_value_gesture_profiles,
      R.id.gesture_profile_menu,
    )
    command(
      R.string.pref_show_context_menu_voice_profile_setting_key,
      R.bool.pref_show_context_menu_voice_profile_default,
      R.string.shortcut_value_voice_profiles,
      R.id.voice_profile_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_audio_ducking_setting_key,
      R.bool.pref_show_context_menu_audio_ducking_default,
      R.string.shortcut_value_toggle_audio_ducking,
      R.id.audio_ducking,
    )
    keyboard(
      R.string.pref_show_context_menu_sound_feedback_setting_key,
      R.bool.pref_show_context_menu_sound_feedback_default,
      Key.TOGGLE_SOUND_FEEDBACK,
      R.id.sound_feedback,
    )
    menuOnly(
      R.string.pref_show_context_menu_vibration_feedback_setting_key,
      R.bool.pref_show_context_menu_vibration_feedback_default,
      R.string.shortcut_value_toggle_vibration,
      R.id.vibration_feedback,
    )
    command(
      R.string.pref_show_context_menu_find_on_screen_setting_key,
      R.bool.pref_show_context_menu_find_on_screen_default,
      R.string.shortcut_value_screen_search,
      R.id.screen_search,
    )
    command(
      R.string.pref_show_context_menu_dim_or_brighten_setting_key,
      R.bool.pref_show_context_menu_dim_or_brighten_default,
      R.string.shortcut_value_show_hide_screen,
      R.id.enable_dimming,
      R.id.disable_dimming,
    )
    command(
      R.string.pref_show_context_menu_voice_commands_setting_key,
      R.bool.pref_show_context_menu_voice_commands_default,
      R.string.shortcut_value_voice_commands,
      R.id.voice_commands,
    )
    command(
      R.string.pref_show_context_menu_pause_backtalk_setting_key,
      R.bool.pref_show_context_menu_pause_backtalk_default,
      R.string.shortcut_value_pause_backtalk,
      R.id.pause_backtalk,
    )
    command(
      R.string.pref_show_context_menu_turn_off_setting_key,
      R.bool.pref_show_context_menu_turn_off_default,
      R.string.shortcut_value_turn_off_sandfly,
      R.id.turn_off_sandfly,
    )
    keyboard(
      R.string.pref_show_context_menu_show_keyboard_shortcuts_setting_key,
      R.bool.pref_show_context_menu_show_keyboard_shortcuts_default,
      Key.SHOW_KEYBOARD_SHORTCUTS_DIALOG,
      R.id.show_keyboard_shortcuts,
    )
    command(
      R.string.pref_show_context_menu_clipboard_history_setting_key,
      R.bool.pref_show_context_menu_clipboard_history_default,
      R.string.shortcut_value_clipboard_history,
      R.id.clipboard_history,
    )
    keyboard(
      R.string.pref_show_context_menu_talkback_settings_setting_key,
      R.bool.pref_show_context_menu_talkback_settings_default,
      Key.OPEN_TALKBACK_SETTINGS,
      R.id.talkback_settings,
    )
    keyboard(
      R.string.pref_show_context_menu_send_feedback_setting_key,
      R.bool.pref_show_context_menu_send_feedback_default,
      Key.REPORT_ISSUE,
      R.id.send_feedback,
    )
    keyboard(
      R.string.pref_show_context_menu_tts_settings_setting_key,
      R.bool.pref_show_context_menu_tts_settings_default,
      Key.OPEN_TTS_SETTINGS,
      R.id.tts_settings,
    )
    command(
      R.string.pref_show_context_menu_split_screen_setting_key,
      R.bool.pref_show_context_menu_split_screen_default,
      R.string.shortcut_value_split_screen,
      R.id.split_screen_menu,
    )
    menuOnly(
      R.string.pref_show_context_menu_system_action_setting_key,
      R.bool.pref_show_context_menu_system_action_default,
      R.string.shortcut_value_system_actions_menu,
      R.id.window_menu,
    )
    command(
      R.string.pref_show_context_menu_braille_display_settings_setting_key,
      R.bool.pref_show_context_menu_braille_display_settings_default,
      R.string.shortcut_value_braille_display_settings,
      R.id.braille_display_settings,
    )
    command(
      R.string.pref_show_context_menu_inspect_item_setting_key,
      R.bool.pref_show_context_menu_inspect_item_default,
      R.string.shortcut_value_inspect_item,
      R.id.inspect_item,
    )
    command(
      R.string.pref_show_context_menu_copy_screen_tree_setting_key,
      R.bool.pref_show_context_menu_copy_screen_tree_default,
      R.string.shortcut_value_copy_screen_tree,
      R.id.copy_screen_tree,
    )
    command(
      R.string.pref_show_context_menu_control_telling_time_setting_key,
      R.bool.pref_show_context_menu_control_telling_time_default,
      R.string.shortcut_value_control_telling_time,
      R.id.enable_telling_time,
      R.id.disable_telling_time,
    )
    return rows
  }
}
