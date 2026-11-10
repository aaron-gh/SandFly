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

import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.selector.SelectorController.Setting

/**
 * Commands that change the value of a reading control without selecting it, for the reading
 * controls no other command changes. A [toggle] has one command, the others have next and previous.
 */
object ReadingControlCommands {
  private const val PREFIX = "READING_CONTROL_VALUE:"
  private const val NEXT = ":NEXT"
  private const val PREVIOUS = ":PREVIOUS"

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
      Entry(Setting.SOUND_MODE, R.string.quick_settings_sound_mode),
      Entry(Setting.CHANGE_BRIGHTNESS, R.string.title_pref_brightness),
      Entry(Setting.CHANGE_TOUCH_FOCUS_LATENCY, R.string.title_pref_touch_focus_latency),
      Entry(Setting.CHANGE_TYPING_FOCUS_LATENCY, R.string.title_pref_touch_explore_latency),
      Entry(Setting.CHANGE_LIFT_TO_ACTIVATE, R.string.title_pref_lift_to_activate),
      Entry(Setting.WRAP_AROUND, R.string.title_pref_wrap_around, toggle = true),
      Entry(Setting.TABLET_HELD_UP_FACES_AWAY, R.string.title_selector_tablet_held_up_faces_away, toggle = true),
      Entry(Setting.SWITCH_TTS_ENGINE, R.string.title_selector_switch_tts_engine),
      Entry(Setting.SPEECH_STREAM, R.string.title_pref_speech_stream),
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
}
