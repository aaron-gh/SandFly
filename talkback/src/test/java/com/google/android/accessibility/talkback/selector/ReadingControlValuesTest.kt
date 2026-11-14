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

import android.media.AudioManager
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.talkback.translation.TranslationSettings
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.accessibility.talkback.keyboardecho.KeyboardEchoSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReadingControlValuesTest {
  private val context = RuntimeEnvironment.getApplication()
  private val res = context.resources
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val on = context.getString(R.string.value_on)
  private val off = context.getString(R.string.value_off)

  @Before
  fun setUp() {
    prefs.edit().clear().commit()
  }

  @Test
  fun withValue_followsTheNameWithACommaOrLeavesTheNameAlone() {
    assertEquals("Speech rate, 60 percent", ReadingControlValues.withValue("Speech rate", "60 percent"))
    assertEquals("Words", ReadingControlValues.withValue("Words", null))
    assertEquals("Words", ReadingControlValues.withValue("Words", ""))
  }

  @Test
  fun switches_sayOnOrOff() {
    assertEquals(off, value(Setting.AUTO_TRANSLATION))
    TranslationSettings.setAuto(prefs, true)
    assertEquals(on, value(Setting.AUTO_TRANSLATION))

    val wrap = context.getString(R.string.pref_wrap_around_key)
    prefs.edit().putBoolean(wrap, true).commit()
    assertEquals(on, value(Setting.WRAP_AROUND))
    prefs.edit().putBoolean(wrap, false).commit()
    assertEquals(off, value(Setting.WRAP_AROUND))

    prefs.edit().putBoolean(context.getString(R.string.pref_use_audio_focus_key), true).commit()
    assertEquals(on, value(Setting.AUDIO_FOCUS))
    prefs.edit().putBoolean(context.getString(R.string.pref_speak_time_key), false).commit()
    assertEquals(off, value(Setting.CONTROL_TELLING_TIME))
    prefs.edit().putBoolean(context.getString(R.string.pref_formatting_inline_key), true).commit()
    assertEquals(on, value(Setting.FORMATTING))
  }

  @Test
  fun choices_sayTheChoiceAsTheSettingNamesIt() {
    val punctuation = res.getStringArray(R.array.pref_punctuation_entries)
    val levels = res.getStringArray(R.array.pref_punctuation_values)
    prefs
      .edit()
      .putString(context.getString(R.string.pref_punctuation_verbosity), levels.last())
      .commit()
    assertEquals(punctuation.last(), value(Setting.PUNCTUATION))

    val emoji = res.getStringArray(R.array.pref_emoji_speech_entries)
    val emojiValues = res.getStringArray(R.array.pref_emoji_speech_values)
    prefs.edit().putString(context.getString(R.string.pref_emoji_speech_key), emojiValues[1]).commit()
    assertEquals(emoji[1], value(Setting.EMOJI))

    val echo = KeyboardEchoSettings.choices(res).last()
    KeyboardEchoSettings.write(prefs, res, TypingInput.PHYSICAL_KEYBOARD, echo)
    assertEquals(KeyboardEchoSettings.choiceName(res, echo), value(Setting.KEYBOARD_ECHO_PHYSICAL))

    val lift = res.getStringArray(R.array.pref_lift_to_activate_entries)
    assertEquals(true, lift.contains(value(Setting.CHANGE_LIFT_TO_ACTIVATE)))

    val streams = res.getStringArray(R.array.pref_speech_stream_entries)
    prefs
      .edit()
      .putString(
        FailoverTextToSpeech.PREF_SPEECH_STREAM_KEY,
        FailoverTextToSpeech.STREAM_VALUES.last(),
      )
      .commit()
    assertEquals(streams[FailoverTextToSpeech.STREAM_VALUES.size - 1], value(Setting.SPEECH_STREAM))
  }

  @Test
  fun soundMode_saysTheModeInUse() {
    context.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_VIBRATE
    assertEquals(context.getString(R.string.quick_settings_vibrate), value(Setting.SOUND_MODE))
  }

  @Test
  fun profiles_sayTheProfileInUse() {
    assertEquals(context.getString(R.string.gesture_profile_default), value(Setting.GESTURE_PROFILE))
    assertEquals(on, value(Setting.EQUALIZER))
  }

  @Test
  fun navigationAndActions_haveNoValue() {
    assertNull(value(Setting.GRANULARITY_WORDS))
    assertNull(value(Setting.GRANULARITY_HEADINGS))
    assertNull(value(Setting.ACTIONS))
    assertNull(value(Setting.AI_ACTIONS))
    assertNull(value(Setting.LIVE_RECOGNITION))
    assertNull(value(Setting.TEXT_ENTITIES))
    assertNull(value(Setting.SCROLLING_SEQUENTIAL))
    assertNull(value(Setting.VERTICAL_NAVIGATION))
  }

  private fun value(setting: Setting) = ReadingControlValues.of(context, prefs, setting)
}
