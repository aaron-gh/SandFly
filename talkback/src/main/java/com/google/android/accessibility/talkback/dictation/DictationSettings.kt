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

package com.google.android.accessibility.talkback.dictation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.speech.RecognitionService
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.GeminiApiKey
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/** Dictation settings: how speech is recognized, and which AI service corrects it. */
object DictationSettings {
  private const val TAG = "DictationSettings"
  const val PREF_RECOGNITION = "pref_dictation_recognition"
  const val PREF_AI_SERVICE = "pref_dictation_ai_service"
  const val PREF_WHISPER_MODEL = "pref_dictation_whisper_model"
  const val PREF_WHISPER_GPU = "pref_dictation_whisper_gpu"
  const val PREF_RAMBLER_LIVE = "pref_dictation_rambler_live"
  const val PREF_AUTO_STOP_MS = "pref_dictation_auto_stop_ms"
  const val PREF_SPEAK_TEXT = "pref_dictation_speak_text"
  const val PREF_PUNCTUATION = "pref_dictation_punctuation"
  const val PREF_BLOCK_OFFENSIVE = "pref_dictation_block_offensive"

  /** The AI service of automatic descriptions corrects dictation too. */
  const val AI_SAME = "same"
  /** The on-device AI model corrects dictation. */
  const val AI_DEVICE = "device"

  /** How speech becomes text. */
  enum class Recognition(val id: String) {
    /** The first of these that can be used, in this order. */
    AUTOMATIC("automatic"),
    /**
     * Google's speech recognition on the phone, from the Speech Recognition & Synthesis app, with
     * the language packs downloaded there. Gboard's voice typing, Rambler included, runs on it.
     */
    GOOGLE("google"),
    /** Google's Gemini 3.5 Transcribe through the Gemini API, the model behind Rambler's cleanup. */
    RAMBLER("rambler"),
    /** Whisper on the phone, with a model downloaded in Dictation settings. */
    WHISPER("whisper"),
    /** The speech recognition service chosen in Android's settings. */
    ANDROID("android");

    companion object {
      fun of(id: String?): Recognition? = entries.firstOrNull { it.id == id }
    }
  }

  fun recognition(prefs: SharedPreferences): Recognition =
    Recognition.of(prefs.getString(PREF_RECOGNITION, null)) ?: Recognition.AUTOMATIC

  fun setRecognition(prefs: SharedPreferences, recognition: Recognition) =
    prefs.edit { putString(PREF_RECOGNITION, recognition.id) }

  /** Google's on-device speech recognition service, in the Speech Recognition & Synthesis app. */
  val GOOGLE_SERVICE =
    ComponentName(
      "com.google.android.tts",
      "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService",
    )

  /** Whether Google's on-device speech recognition is installed. */
  fun googleInstalled(context: Context): Boolean =
    context.packageManager
      .queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE).setComponent(GOOGLE_SERVICE), 0)
      .isNotEmpty()

  /** The Gemini API key Rambler uses, or empty when there is none. */
  fun ramblerKey(prefs: SharedPreferences): String =
    GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY)

  /** Whether Rambler transcribes while the user speaks, so that the text is ready sooner. */
  fun ramblerLive(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_RAMBLER_LIVE, true)

  /** Whether Dictate speaks the text it types: Speak dictated text. */
  @JvmStatic
  fun speaksText(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_SPEAK_TEXT, true)

  /** Set once [migrateSpeakTextDefault] has run. */
  const val PREF_SPEAK_TEXT_DEFAULT_ON = "pref_dictation_speak_text_default_on"

  /**
   * Speak dictated text became on by default. The settings screen saved the old default, off, when
   * it was shown, and off could only be chosen by turning the switch on and off again, so a saved
   * off is dropped once and the new default applies. A saved on stays. Runs once.
   */
  @JvmStatic
  fun migrateSpeakTextDefault(prefs: SharedPreferences) {
    if (prefs.getBoolean(PREF_SPEAK_TEXT_DEFAULT_ON, false)) return
    prefs.edit {
      if (prefs.contains(PREF_SPEAK_TEXT) && !prefs.getBoolean(PREF_SPEAK_TEXT, true)) {
        remove(PREF_SPEAK_TEXT)
      }
      putBoolean(PREF_SPEAK_TEXT_DEFAULT_ON, true)
    }
  }

  /** Whether Google speech recognition adds punctuation and capital letters: Automatic punctuation. */
  fun punctuation(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_PUNCTUATION, true)

  /** Whether Google speech recognition masks offensive words: Block offensive words. */
  fun blocksOffensiveWords(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREF_BLOCK_OFFENSIVE, true)

  /** How long after the last speech recording stops by itself, in milliseconds, or 0 for never. */
  fun autoStopMs(prefs: SharedPreferences): Long =
    prefs.getString(PREF_AUTO_STOP_MS, null)?.toLongOrNull()?.coerceAtLeast(0) ?: 0

  /**
   * The language of the keyboard's current input method subtype, as Gboard's language switch sets
   * it, or null when the keyboard has none or it is the phone's language.
   */
  fun keyboardLanguage(context: Context): Locale? {
    val subtype =
      try {
        context.getSystemService(InputMethodManager::class.java)?.currentInputMethodSubtype
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "No keyboard subtype: %s", e)
        null
      } ?: return null
    @Suppress("DEPRECATION") val locale = subtype.locale
    return subtypeLanguage(subtype.languageTag, locale, subtype.mode, Locale.getDefault())
  }

  /**
   * The language of a keyboard subtype with [languageTag] or, without one, [locale] such as uk_UA,
   * in [mode]; null when it has none, isn't a keyboard, or has [phone]'s language.
   */
  fun subtypeLanguage(languageTag: String?, locale: String?, mode: String?, phone: Locale): Locale? {
    if (!mode.isNullOrEmpty() && mode != "keyboard") return null
    val language =
      languageTag?.takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag)
        ?: locale?.takeIf { it.isNotEmpty() }?.let { Locale.forLanguageTag(it.replace('_', '-')) }
        ?: return null
    // "zz" is the subtype of keyboards for no particular language, such as emoji.
    if (language.language.isEmpty() || language.language == "zz" || language.language == "und") {
      return null
    }
    return language.takeIf { it.language != phone.language }
  }

  /** Whether Rambler recognizes [locale]'s language. */
  fun ramblerSupports(locale: Locale): Boolean = locale.language in RAMBLER_LANGUAGES

  /**
   * The way speech is recognized now: the chosen one when it can be used, otherwise the first of
   * Google on-device, Rambler, Whisper and Android speech recognition that can recognize [language],
   * other than [skip]: one that failed, such as Google on-device without a language pack for it, or
   * Rambler for a password.
   */
  fun effectiveRecognition(
    context: Context,
    prefs: SharedPreferences,
    skip: Set<Recognition> = emptySet(),
    language: Locale = Locale.getDefault(),
  ): Recognition {
    val usable = { recognition: Recognition ->
      recognition !in skip &&
        when (recognition) {
          Recognition.GOOGLE ->
            googleInstalled(context) && GoogleSpeechLanguages.hasLanguage(language) != false
          Recognition.RAMBLER -> ramblerKey(prefs).isNotEmpty()
          Recognition.WHISPER -> WhisperModels.get(context).activeWhisper() != null
          Recognition.ANDROID -> true
          Recognition.AUTOMATIC -> false
        }
    }
    val chosen = recognition(prefs)
    if (usable(chosen)) return chosen
    return when {
      usable(Recognition.GOOGLE) -> Recognition.GOOGLE
      usable(Recognition.RAMBLER) && ramblerSupports(language) -> Recognition.RAMBLER
      usable(Recognition.WHISPER) -> Recognition.WHISPER
      else -> Recognition.ANDROID
    }
  }

  /** Whether Whisper runs on the GPU: chosen, on a phone whose GPU it can use. */
  fun whisperOnGpu(prefs: SharedPreferences): Boolean =
    WhisperEngines.gpuAvailable() && prefs.getBoolean(PREF_WHISPER_GPU, false)

  fun setWhisperOnGpu(prefs: SharedPreferences, gpu: Boolean) =
    prefs.edit { putBoolean(PREF_WHISPER_GPU, gpu) }

  fun correctsWithAi(context: Context, prefs: SharedPreferences): Boolean =
    prefs.getBoolean(
      context.getString(R.string.pref_ai_dictation_key),
      context.resources.getBoolean(R.bool.pref_ai_dictation_default),
    )

  /** [AI_SAME], [AI_DEVICE] or an [AiProvider] id. */
  fun aiService(prefs: SharedPreferences): String =
    prefs.getString(PREF_AI_SERVICE, null)?.takeIf { it.isNotEmpty() } ?: AI_SAME

  fun setAiService(prefs: SharedPreferences, service: String) =
    prefs.edit { putString(PREF_AI_SERVICE, service) }

  /**
   * The AI service that corrects dictation: an [AiProvider] that is set up, [AI_DEVICE] when the
   * on-device model is installed, or null when none can.
   */
  fun correctingService(context: Context, prefs: SharedPreferences): String? {
    val chosen = aiService(prefs)
    if (chosen == AI_SAME) {
      if (LocalModelManager.get(context).answersRequests()) return AI_DEVICE
      val active = AiProvider.active(prefs)
      return active.id.takeIf { active != AiProvider.NONE && active.isReady(prefs) }
    }
    if (chosen == AI_DEVICE) return AI_DEVICE.takeIf { LocalModelManager.get(context).isReady() }
    val provider = AiProvider.of(chosen) ?: return null
    return provider.id.takeIf { provider != AiProvider.NONE && provider.isReady(prefs) }
  }

  /** The languages of Gemini 3.5 Transcribe, by ISO 639 code as Java's [Locale] gives them. */
  private val RAMBLER_LANGUAGES =
    setOf(
      "af", "am", "ar", "hy", "as", "az", "be", "bn", "bs", "bg", "my", "yue", "ca", "ceb", "km",
      "hr", "cs", "da", "nl", "en", "et", "fa", "fil", "tl", "fi", "fr", "gl", "ka", "de", "el",
      "gu", "ha", "he", "iw", "hi", "hu", "is", "id", "in", "it", "ja", "jv", "kea", "kn", "kk",
      "ko", "ky", "lv", "ln", "lt", "mk", "ms", "ml", "mt", "zh", "mr", "mn", "ne", "nb", "no",
      "or", "pl", "pt", "pa", "ro", "ru", "sr", "sd", "sk", "sl", "es", "sw", "sv", "tg", "te",
      "th", "tr", "uk", "uz", "vi",
    )
}
