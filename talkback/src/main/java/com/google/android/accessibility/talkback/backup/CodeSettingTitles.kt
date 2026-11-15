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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.LanguageVoices
import com.google.android.accessibility.utils.output.SpeechEqualizer
import com.google.android.accessibility.utils.output.VoiceProfiles
import java.util.Locale

/**
 * The titles of settings that screens built in code set, such as voice profiles, which the
 * settings screens' XML doesn't have.
 */
internal object CodeSettingTitles {
  private const val LANGUAGE_VOICE_PREFIX = "pref_language_voice_"
  private const val SCRIPT_PREFIX = "pref_script_language_"
  private const val EQUALIZER_SCREEN_KEY = "pref_speech_equalizer_screen"
  private const val LANGUAGE_VOICES_SCREEN_KEY = "pref_language_voices_screen"

  private val VOICE_PROFILE_FIELD =
    Regex(Regex.escape(VoiceProfiles.PREF_PREFIX) + "_([0-9]+)_([a-z]+)")

  private val FIELD_TITLES =
    mapOf(
      VoiceProfiles.NAME to R.string.title_pref_voice_profile_name,
      VoiceProfiles.ENGINE to R.string.title_pref_tts_engine,
      VoiceProfiles.LANGUAGE to R.string.title_pref_voice_profile_language,
      VoiceProfiles.VOICE to R.string.title_pref_voice_profile_voice,
      VoiceProfiles.VOLUME to R.string.title_pref_speech_volume,
      VoiceProfiles.RATE to R.string.title_pref_talkback_speech_rate,
      VoiceProfiles.PITCH to R.string.title_pref_speech_pitch,
      VoiceProfiles.PHRASES to R.string.title_pref_speak_in_phrases,
      VoiceProfiles.EQUALIZER to R.string.title_pref_speech_equalizer,
      VoiceProfiles.EQUALIZER_ON to R.string.title_pref_speech_equalizer_on,
      VoiceProfiles.KEEP_STARTED to R.string.title_pref_voice_profile_keep_engine_started,
    )

  /**
   * Returns the title of [key] and the titles of the screens that lead to it, or null if no screen
   * built in code sets it. [screens] has the title and path of each setting of the settings
   * screens' XML by key, including the entries that open screens.
   */
  fun of(
    context: Context,
    key: String,
    screens: Map<String, Pair<String, List<String>>>,
  ): Pair<String, List<String>>? {
    fun screen(screenKey: String): List<String> =
      screens[screenKey]?.let { (title, path) -> path + title }.orEmpty()
    fun title(res: Int) = context.getString(res)
    val voiceProfilesKey = context.getString(R.string.pref_voice_profiles_screen_key)
    when (key) {
      VoiceProfiles.PREF_ACTIVE ->
        return title(R.string.title_pref_voice_profile_in_use) to screen(voiceProfilesKey)
      VoiceProfiles.PREF_IDS -> return screens[voiceProfilesKey]
      SpeechEqualizer.PREF_DEFAULT -> return screens[EQUALIZER_SCREEN_KEY]
      SpeechEqualizer.PREF_DEFAULT_ON ->
        return title(R.string.title_pref_speech_equalizer_on) to screen(EQUALIZER_SCREEN_KEY)
      SpeechEqualizer.PREF_PRESETS ->
        return title(R.string.title_pref_speech_equalizer_saved_presets) to
          screen(EQUALIZER_SCREEN_KEY)
      LanguageVoices.PREF_LANGUAGES -> return screens[LANGUAGE_VOICES_SCREEN_KEY]
    }
    VOICE_PROFILE_FIELD.matchEntire(key)?.let { match ->
      val (id, field) = match.destructured
      val titleRes = FIELD_TITLES[field] ?: return null
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      val name = VoiceProfiles.read(prefs, id).name().ifEmpty { id }
      val profile = screen(voiceProfilesKey) + name
      val path =
        if (field == VoiceProfiles.EQUALIZER_ON) profile + title(R.string.title_pref_speech_equalizer)
        else profile
      return title(titleRes) to path
    }
    if (key.startsWith(LANGUAGE_VOICE_PREFIX)) {
      val language = Locale.forLanguageTag(key.removePrefix(LANGUAGE_VOICE_PREFIX)).displayName
      return language.ifEmpty { null }?.let { it to screen(LANGUAGE_VOICES_SCREEN_KEY) }
    }
    if (key.startsWith(SCRIPT_PREFIX)) {
      val script = scriptName(key.removePrefix(SCRIPT_PREFIX))
      return context.getString(R.string.title_pref_script_language, script) to
        screen(LANGUAGE_VOICES_SCREEN_KEY)
    }
    return null
  }

  /** The name of the script with ISO 15924 code [script], as the Language voices screen shows it. */
  private fun scriptName(script: String): String {
    val code =
      when (script) {
        "Kana" -> "Jpan"
        "Hang" -> "Kore"
        else -> script
      }
    return try {
      Locale.Builder().setScript(code).build().displayScript.ifEmpty { script }
    } catch (e: RuntimeException) {
      script
    }
  }
}
