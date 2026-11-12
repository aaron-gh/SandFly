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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandMenus
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * Commands that act on one reading control. Value commands change a reading control without
 * selecting it, for those no other command changes: a [toggle] has one command, the others have next
 * and previous. Selection commands make a reading control in the user's list the selected one.
 */
object ReadingControlCommands {
  private const val PREFIX = "READING_CONTROL_VALUE:"
  private const val NEXT = ":NEXT"
  private const val PREVIOUS = ":PREVIOUS"
  private const val SELECT_PREFIX = "SET_READING_CONTROL:"
  private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

  class Entry(val setting: Setting, @StringRes val title: Int, val toggle: Boolean = false)

  @JvmField
  val ENTRIES: List<Entry> =
    listOf(
      Entry(Setting.LANGUAGE, R.string.spoken_language),
      Entry(Setting.VERBOSITY, R.string.title_pref_selector_verbosity),
      Entry(Setting.PUNCTUATION, R.string.title_pref_selector_punctuation),
      Entry(Setting.EMOJI, R.string.title_pref_emoji_speech),
      Entry(Setting.KEYBOARD_ECHO_ON_SCREEN, R.string.title_keyboard_echo_on_screen),
      Entry(Setting.KEYBOARD_ECHO_PHYSICAL, R.string.title_keyboard_echo_physical),
      Entry(Setting.KEYBOARD_ECHO_BRAILLE_KEYBOARD, R.string.title_keyboard_echo_braille_keyboard),
      Entry(Setting.KEYBOARD_ECHO_BRAILLE_DISPLAY, R.string.title_keyboard_echo_braille_display),
      Entry(Setting.KEYBOARD_ECHO_HANDWRITING_KEYBOARD, R.string.title_keyboard_echo_handwriting_keyboard),
      Entry(Setting.CALL_VOLUME, R.string.quick_settings_call_volume),
      Entry(Setting.RING_VOLUME, R.string.quick_settings_ring_volume),
      Entry(Setting.NOTIFICATION_VOLUME, R.string.quick_settings_notification_volume),
      Entry(Setting.ALARM_VOLUME, R.string.quick_settings_alarm_volume),
      Entry(Setting.SOUND_MODE, R.string.quick_settings_sound_mode),
      Entry(Setting.CHANGE_BRIGHTNESS, R.string.title_pref_brightness),
      Entry(Setting.CHANGE_TOUCH_FOCUS_LATENCY, R.string.title_pref_touch_focus_latency),
      Entry(Setting.CHANGE_TYPING_FOCUS_LATENCY, R.string.title_pref_touch_explore_latency),
      Entry(Setting.CHANGE_LIFT_TO_ACTIVATE, R.string.title_pref_lift_to_activate),
      Entry(Setting.WRAP_AROUND, R.string.title_pref_wrap_around, toggle = true),
      Entry(Setting.TABLET_HELD_UP_FACES_AWAY, R.string.title_selector_tablet_held_up_faces_away, toggle = true),
      Entry(Setting.SWITCH_TTS_ENGINE, R.string.title_selector_switch_tts_engine),
      Entry(Setting.SPEECH_STREAM, R.string.title_pref_speech_stream),
      Entry(Setting.AUDIO_OUTPUT, R.string.title_pref_audio_output_device),
      Entry(Setting.EQUALIZER_32, R.string.title_selector_equalizer_32),
      Entry(Setting.EQUALIZER_64, R.string.title_selector_equalizer_64),
      Entry(Setting.EQUALIZER_125, R.string.title_selector_equalizer_125),
      Entry(Setting.EQUALIZER_250, R.string.title_selector_equalizer_250),
      Entry(Setting.EQUALIZER_500, R.string.title_selector_equalizer_500),
      Entry(Setting.EQUALIZER_1K, R.string.title_selector_equalizer_1k),
      Entry(Setting.EQUALIZER_2K, R.string.title_selector_equalizer_2k),
      Entry(Setting.EQUALIZER_3K, R.string.title_selector_equalizer_3k),
      Entry(Setting.EQUALIZER_4K, R.string.title_selector_equalizer_4k),
      Entry(Setting.EQUALIZER_6K, R.string.title_selector_equalizer_6k),
      Entry(Setting.EQUALIZER_8K, R.string.title_selector_equalizer_8k),
      Entry(Setting.EQUALIZER_16K, R.string.title_selector_equalizer_16k),
    )

  private val SETTINGS = ENTRIES.associateBy { it.setting.name }

  @JvmStatic fun valueOf(setting: Setting, next: Boolean): String = PREFIX + setting.name + if (next) NEXT else PREVIOUS

  /** The reading control the command saved as [value] changes, or null for other commands. */
  @JvmStatic
  fun settingOf(value: String): Setting? {
    if (!value.startsWith(PREFIX)) return null
    return SETTINGS[value.removePrefix(PREFIX).removeSuffix(NEXT).removeSuffix(PREVIOUS)]?.setting
  }

  @JvmStatic fun isNext(value: String): Boolean = value.endsWith(NEXT)

  /** A command that makes a reading control the selected one. */
  class Selection(val value: String, val label: String)

  /** Returns whether [value] is a command that selects a reading control. */
  @JvmStatic fun isSelection(value: String?): Boolean = value != null && value.startsWith(SELECT_PREFIX)

  /** The preference key or script key of the reading control the command [value] selects. */
  @JvmStatic fun selectedKey(value: String): String = value.removePrefix(SELECT_PREFIX)

  /** The value of the command that selects the reading control with the key [key]. */
  fun selectionValue(key: String): String = SELECT_PREFIX + key

  /** Returns a command for each reading control in the user's list, in its order. */
  fun selections(context: Context, prefs: SharedPreferences): List<Selection> {
    val titles = titles(context)
    val settings = SelectorController.SELECTOR_SETTINGS.associateBy { context.getString(it.prefKeyResId) }
    val custom =
      CommandMenus.controls(prefs).filter { it.items.isNotEmpty() && prefs.getBoolean(it.id, true) }.associateBy { it.id }
    val defaults = SelectorController.defaultOrderKeys(context) + custom.keys
    val listed =
      ItemOrder.ordered(prefs, ItemOrder.READING_CONTROLS_KEY, defaults).mapNotNull { key ->
        val setting = settings[key]
        when {
          key in custom -> selection(context, key, custom.getValue(key).name)
          setting != null &&
            prefs.getBoolean(key, context.resources.getBoolean(setting.defaultValueResId)) ->
            titles[key]?.let { selection(context, key, it) }
          else -> null
        }
      }
    return listed + Scripts.readingControls().map { selection(context, it.key, it.title) }
  }

  /** The name of the selection command [value], even when its control has left the list. */
  fun selectionLabel(context: Context, value: String): String? {
    if (!isSelection(value)) return null
    val key = selectedKey(value)
    val title =
      Scripts.readingControls().firstOrNull { it.key == key }?.title
        ?: CommandMenus.find(SharedPreferencesUtils.getSharedPreferences(context), key)?.name
        ?: titles(context)[key]
        ?: return null
    return context.getString(R.string.shortcut_set_reading_control, title)
  }

  private fun selection(context: Context, key: String, title: String) =
    Selection(SELECT_PREFIX + key, context.getString(R.string.shortcut_set_reading_control, title))

  @Volatile private var cachedTitles: Pair<Locale?, Map<String, String>>? = null

  /** The title of each reading control on the Reading controls screen, by preference key. */
  private fun titles(context: Context): Map<String, String> {
    val locale = context.resources.configuration.locales.get(0)
    cachedTitles?.takeIf { it.first == locale }?.let {
      return it.second
    }
    val titles = HashMap<String, String>()
    context.resources.getXml(R.xml.selector_menu_preferences).use { parser ->
      var event = parser.next()
      while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
          val key = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "key", 0)
          val title = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "title", 0)
          if (key != 0 && title != 0) {
            titles[context.getString(key)] = context.getString(title)
          }
        }
        event = parser.next()
      }
    }
    cachedTitles = locale to titles
    return titles
  }
}
