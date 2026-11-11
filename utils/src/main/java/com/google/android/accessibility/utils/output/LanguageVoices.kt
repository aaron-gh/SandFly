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

package com.google.android.accessibility.utils.output

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile
import java.util.Locale

/**
 * The languages the user reads, each spoken with SandFly default's voice for it or with a voice
 * profile, and the language that text in a script shared by several of them is in unless its
 * letters tell. [LanguageDetector] finds them in text.
 */
object LanguageVoices {

  /** The languages added, as language tags separated by commas, in the order added. */
  const val PREF_LANGUAGES = "pref_languages"

  /** Starts the setting of a language's voice profile ID, followed by its tag. Empty: default. */
  private const val PREF_VOICE_PREFIX = "pref_language_voice_"

  /** Starts the setting of a script's language tag, followed by its ISO 15924 code. */
  private const val PREF_SCRIPT_PREFIX = "pref_script_language_"

  /** Whether languages that share a script are told apart by their words, with [guesser]. */
  const val PREF_IDENTIFY_FROM_WORDS = "pref_identify_languages_from_words"

  /** Speech parameter: the ID of the voice profile to say a fragment with. */
  const val SPEECH_PARAM_PROFILE = "sandfly_language_profile"

  /** Speech parameters: the fragment's rate and pitch, as factors of the profile's. */
  const val SPEECH_PARAM_RATE = "sandfly_language_rate"
  const val SPEECH_PARAM_PITCH = "sandfly_language_pitch"

  /** What was read from the settings. Languages are by language code. */
  class State(
    /** The languages added, in order. */
    @JvmField val languages: List<Locale>,
    /** The voice profile of each language that has one, by language code. */
    @JvmField val profiles: Map<String, VoiceProfile>,
    /** The language of each script that has one set, by ISO 15924 code. */
    @JvmField val scriptLanguages: Map<String, Locale>,
    @JvmField val identifyFromWords: Boolean,
  )

  @Volatile private var state = State(emptyList(), emptyMap(), emptyMap(), false)

  /** Tells languages that share a script apart by their words, or null where it can't. */
  @JvmStatic @Volatile var guesser: LanguageGuesser? = null

  @JvmStatic fun state(): State = state

  /** Whether a change to [key] can change the languages or their voices. */
  @JvmStatic
  fun isKey(key: String?): Boolean =
    key != null &&
      (key == PREF_LANGUAGES ||
        key == PREF_IDENTIFY_FROM_WORDS ||
        key.startsWith(PREF_VOICE_PREFIX) ||
        key.startsWith(PREF_SCRIPT_PREFIX) ||
        key == VoiceProfiles.PREF_IDS ||
        VoiceProfiles.isProfileKey(key))

  @JvmStatic fun voiceKey(tag: String): String = PREF_VOICE_PREFIX + tag

  @JvmStatic fun scriptKey(script: String): String = PREF_SCRIPT_PREFIX + script

  /** The tags of the languages added, in order. */
  @JvmStatic
  fun languages(prefs: SharedPreferences): List<String> =
    (prefs.getString(PREF_LANGUAGES, null) ?: "")
      .split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .distinct()

  /** Adds the language [tag], unless it or another form of its language was added. */
  @JvmStatic
  fun add(prefs: SharedPreferences, tag: String) {
    val tags = languages(prefs)
    val language = Locale.forLanguageTag(tag).language
    if (tags.any { Locale.forLanguageTag(it).language == language }) return
    prefs.edit { putString(PREF_LANGUAGES, (tags + tag).joinToString(",")) }
  }

  /** Removes the language [tag] and its voice. */
  @JvmStatic
  fun remove(prefs: SharedPreferences, tag: String) {
    prefs.edit {
      putString(PREF_LANGUAGES, languages(prefs).filter { it != tag }.joinToString(","))
      remove(voiceKey(tag))
    }
  }

  /** The ID of the voice profile of language [tag], or empty for SandFly default. */
  @JvmStatic
  fun profileId(prefs: SharedPreferences, tag: String): String =
    prefs.getString(voiceKey(tag), null) ?: ""

  /** Reads the settings. Call again when [isKey] says a changed setting matters. */
  @JvmStatic
  fun read(prefs: SharedPreferences) {
    val tags = languages(prefs)
    val profileIds = VoiceProfiles.ids(prefs).toSet()
    val languages = ArrayList<Locale>()
    val profiles = HashMap<String, VoiceProfile>()
    for (tag in tags) {
      val locale = Locale.forLanguageTag(tag)
      if (locale.language.isEmpty()) continue
      languages += locale
      val id = profileId(prefs, tag)
      if (id.isNotEmpty() && id in profileIds) profiles[locale.language] = VoiceProfiles.read(prefs, id)
    }
    val scriptLanguages = HashMap<String, Locale>()
    for ((key, value) in prefs.all) {
      if (!key.startsWith(PREF_SCRIPT_PREFIX) || value !is String || value.isEmpty()) continue
      scriptLanguages[key.removePrefix(PREF_SCRIPT_PREFIX)] = Locale.forLanguageTag(value)
    }
    state =
      State(
        languages,
        profiles,
        scriptLanguages,
        prefs.getBoolean(PREF_IDENTIFY_FROM_WORDS, false),
      )
  }

  /**
   * The voice profile to say text in [locale] with, or null for SandFly's speech voice. A language's
   * profile isn't used while it is the voice profile in use.
   */
  @JvmStatic
  fun profileFor(locale: Locale?): VoiceProfile? {
    if (locale == null) return null
    val profile = state.profiles[locale.language] ?: return null
    return if (profile.sameVoiceAs(VoiceProfiles.active())) null else profile
  }

  /** The voice profiles languages are said with, once each. */
  @JvmStatic
  fun profiles(): Collection<VoiceProfile> = state.profiles.values.associateBy { it.id() }.values
}

/** Tells which of several languages written in the same script some text is in. */
fun interface LanguageGuesser {
  /**
   * Returns the language code, one of [candidates], that [text] is most likely in, or null if
   * none is likely enough. Called on the main thread, so it must return quickly.
   */
  fun guess(text: String, candidates: List<String>): String?
}
