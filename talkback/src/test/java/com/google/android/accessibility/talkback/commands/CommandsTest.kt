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
import android.content.pm.PackageManager
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut
import com.google.android.accessibility.talkback.recognition.LiveRecognition
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.talkback.recognition.RecognitionMode
import com.google.android.accessibility.talkback.selector.ReadingControlCommands
import com.google.android.accessibility.talkback.utils.FocusIndicatorUtils
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommandsTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Other tests may or may not have done this already, depending on the order they run in.
    FormFactorUtils.initialize(context)
  }

  private fun allCommands() = Commands.categories(context).flatMap { it.commands }

  @Test
  fun liveRecognitionModesAreCommandsWithACamera() {
    shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA, true)
    val values = allCommands().map { it.value }
    for (mode in RecognitionMode.entries) {
      val value = LiveRecognition.commandOf(mode)
      assertTrue(value in values)
      assertTrue(LiveRecognition.isCommand(value))
    }
    assertTrue(LiveRecognition.STOP_COMMAND in values)
    assertFalse(LiveRecognition.isCommand("LIVE_RECOGNITION_OTHER"))
  }

  @Test
  fun newFeatureCommandsAreListed() {
    val values = allCommands().map { it.value }
    for (res in
      listOf(
        R.string.shortcut_value_text_entities,
        R.string.shortcut_value_activate_confirm_button,
        R.string.shortcut_value_activate_cancel_button,
        R.string.shortcut_value_toggle_auto_translation,
        R.string.shortcut_value_toggle_equalizer,
        R.string.shortcut_value_toggle_focus_indicator,
        R.string.shortcut_value_repeat_last_spoken_phrase_slower,
        R.string.shortcut_value_toggle_switch_languages,
        R.string.shortcut_value_toggle_identify_languages_from_words,
        R.string.shortcut_value_toggle_focus_text_inside_items,
        R.string.shortcut_value_settings_profiles,
        R.string.shortcut_value_next_settings_profile,
        R.string.shortcut_value_previous_settings_profile,
        R.string.shortcut_value_toggle_time_announcements,
        R.string.shortcut_value_speech_history,
        R.string.shortcut_value_character_count,
        R.string.shortcut_value_read_text_field,
        R.string.shortcut_value_new_line,
        R.string.shortcut_value_switch_keyboard,
        R.string.shortcut_value_app_gestures_for_app,
        R.string.shortcut_value_continuous_copy,
        R.string.shortcut_value_next_reading_control_list,
        R.string.shortcut_value_previous_reading_control_list,
        R.string.shortcut_value_select_first_setting,
        R.string.shortcut_value_share_focused_image,
        R.string.shortcut_value_share_screenshot,
        R.string.shortcut_value_voice_editing,
        R.string.shortcut_value_say_caller,
      )) {
      assertTrue(context.getString(res) in values)
    }
  }

  @Test
  fun focusIndicatorCommandTurnsTheSettingOnAndOff() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val res = context.resources
    prefs.edit().remove(context.getString(R.string.pref_show_focus_indicator_key)).commit()
    assertTrue(FocusIndicatorUtils.isFocusIndicatorShown(prefs, res))
    assertFalse(FocusIndicatorUtils.toggleFocusIndicatorShown(prefs, res))
    assertFalse(FocusIndicatorUtils.isFocusIndicatorShown(prefs, res))
    assertTrue(FocusIndicatorUtils.toggleFocusIndicatorShown(prefs, res))
    assertTrue(FocusIndicatorUtils.isFocusIndicatorShown(prefs, res))
  }

  @Test
  fun readingControlsInTheUsersListHaveSelectionCommands() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val key = context.getString(R.string.pref_selector_punctuation_key)
    val value = "SET_READING_CONTROL:$key"
    prefs.edit().putBoolean(key, true).commit()
    assertTrue(value in allCommands().map { it.value })
    prefs.edit().putBoolean(key, false).commit()
    assertFalse(value in allCommands().map { it.value })
    // A gesture assigned before the control left the list still has a name.
    assertEquals(
      context.getString(
        R.string.shortcut_set_reading_control,
        context.getString(R.string.pref_punctuation_title),
      ),
      Commands.label(context, value),
    )
    prefs.edit().remove(key).commit()
  }

  @Test
  fun selectionCommandsFollowTheOrderOfTheList() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val first = context.getString(R.string.pref_selector_punctuation_key)
    val second = context.getString(R.string.pref_selector_speech_rate_key)
    prefs.edit().putBoolean(first, true).putBoolean(second, true)
      .putString(ItemOrder.READING_CONTROLS_KEY, "$first,$second").commit()
    val values = ReadingControlCommands.selections(context, prefs).map { it.value }
    assertTrue(values.indexOf("SET_READING_CONTROL:$first") < values.indexOf("SET_READING_CONTROL:$second"))
    prefs.edit().remove(first).remove(second).remove(ItemOrder.READING_CONTROLS_KEY).commit()
  }

  @Test
  fun everyCommandIsListedOnce() {
    val values = allCommands().map { it.value }
    assertEquals(values.size, values.toSet().size)
  }

  @Test
  fun noTwoCommandsInACategoryHaveTheSameName() {
    Commands.categories(context).forEach { category ->
      val labels = category.commands.map { it.label }
      assertEquals(labels.groupBy { it }.filter { it.value.size > 1 }.keys, emptySet<String>())
    }
  }

  @Test
  fun everyCategoryHasCommands() {
    Commands.categories(context).forEach { assertTrue(it.commands.isNotEmpty()) }
  }

  @Test
  fun everyDefaultGestureActionIsACommand() {
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    val defaults =
      context.resources.getStringArray(R.array.pref_shortcut_keys).mapNotNull { key ->
        val id = context.resources.getIdentifier(key + "_default", "string", context.packageName)
        if (id == 0) null else context.getString(id)
      }
    for (value in defaults.filter { it != unassigned }) {
      assertNotNull("No command for $value", Commands.label(context, value))
    }
  }

  @Test
  fun keyboardOnlyCommandsRunTheirKeyboardAction() {
    val value = Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.SHOW_HEADING_LIST)
    assertEquals(
      TalkBackPhysicalKeyboardShortcut.SHOW_HEADING_LIST,
      Commands.keyboardActionOf(context, value),
    )
    assertNotNull(Commands.find(context, value))
  }

  @Test
  fun openKeyboardShortcutsSettingsKeepsItsOldValue() {
    val key = TalkBackPhysicalKeyboardShortcut.OPEN_MANAGE_KEYBOARD_SHORTCUTS
    val label = "Open keyboard shortcuts settings"
    assertEquals(label, Commands.label(context, Commands.keyboardValue(context, key)))
    assertEquals(key, Commands.keyboardActionOf(context, Commands.KEYBOARD_PREFIX))
    assertEquals(label, Commands.label(context, Commands.KEYBOARD_PREFIX))
  }

  @Test
  fun gestureCommandsKeepTheirKeyboardAction() {
    val next = Commands.find(context, context.getString(R.string.shortcut_value_next))
    assertEquals(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT, next?.keyboardAction)
    val heading = Commands.find(context, "NEXT_HEADING")
    assertEquals(TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_HEADING, heading?.keyboardAction)
  }

  @Test
  fun shortcutKeysNameTheirCommand() {
    val value = context.getString(R.string.shortcut_value_read_notifications)
    val key = Commands.shortcutKey(value)
    assertEquals(value, Commands.valueOfShortcutKey(key))
    assertNull(Commands.valueOfShortcutKey("keycombo_shortcut_global_back"))
    assertEquals(
      context.getString(R.string.shortcut_read_notifications),
      Commands.labelOfKeyboardKey(context, key),
    )
  }

  @Test
  fun keyboardKeysOfLinkedCommandsNameTheCommand() {
    val key = context.getString(R.string.keycombo_shortcut_navigate_next_default)
    assertEquals(context.getString(R.string.shortcut_next), Commands.labelOfKeyboardKey(context, key))
  }

  @Test
  fun keyboardActionsRunTheCommandThatHasThem() {
    assertEquals(
      context.getString(R.string.shortcut_value_next),
      Commands.valueOfKeyboardAction(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT),
    )
    assertEquals(
      Commands.keyboardValue(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_BELOW),
      Commands.valueOfKeyboardAction(context, TalkBackPhysicalKeyboardShortcut.NAVIGATE_BELOW),
    )
  }

  @Test
  fun gesturesForThisAppIsADefaultMenuItemAndOpensOnlyForAnotherApp() {
    val value = context.getString(R.string.shortcut_value_app_gestures_for_app)
    assertEquals(context.getString(R.string.shortcut_app_gestures_for_app), Commands.label(context, value))
    val row = MenuCommands.rows(context).single { it.value == value }
    assertFalse(row.menuOnly)
    assertTrue(context.resources.getBoolean(row.showDefault))
    assertFalse(AppGestureFragment.canOpen(context, ""))
    assertFalse(AppGestureFragment.canOpen(context, context.packageName))
    assertTrue(AppGestureFragment.canOpen(context, "com.example.player"))
  }
}
