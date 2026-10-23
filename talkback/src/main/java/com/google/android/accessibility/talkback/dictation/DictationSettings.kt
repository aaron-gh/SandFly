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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.GeminiApiKey
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import java.util.Locale

/** Dictation settings: how speech is recognized, and which AI service corrects it. */
object DictationSettings {
  const val PREF_RECOGNITION = "pref_dictation_recognition"
  const val PREF_AI_SERVICE = "pref_dictation_ai_service"
  const val PREF_WHISPER_MODEL = "pref_dictation_whisper_model"

  /** The AI service of automatic descriptions corrects dictation too. */
  const val AI_SAME = "same"
  /** The on-device AI model corrects dictation. */
  const val AI_DEVICE = "device"

  /** How speech becomes text. */
  enum class Recognition(val id: String) {
    /** Rambler for languages it supports, then Whisper, then Android speech recognition. */
    AUTOMATIC("automatic"),
    /** Google's Gemini 3.5 Transcribe, the model behind Gboard's Rambler. */
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

  /** The Gemini API key Rambler uses, or empty when there is none. */
  fun ramblerKey(prefs: SharedPreferences): String =
    GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY)

  /** Whether Rambler recognizes [locale]'s language. */
  fun ramblerSupports(locale: Locale): Boolean = locale.language in RAMBLER_LANGUAGES

  /**
   * The way speech is recognized now: the chosen one when it can be used, otherwise the first of
   * Rambler, Whisper and Android speech recognition that can.
   */
  fun effectiveRecognition(context: Context, prefs: SharedPreferences): Recognition {
    val whisperReady = WhisperModels.get(context).activeModel() != null
    val ramblerReady = ramblerKey(prefs).isNotEmpty()
    return when (recognition(prefs)) {
      Recognition.RAMBLER -> if (ramblerReady) Recognition.RAMBLER else null
      Recognition.WHISPER -> if (whisperReady) Recognition.WHISPER else null
      Recognition.ANDROID -> Recognition.ANDROID
      Recognition.AUTOMATIC -> null
    }
      ?: when {
        ramblerReady && ramblerSupports(Locale.getDefault()) -> Recognition.RAMBLER
        whisperReady -> Recognition.WHISPER
        else -> Recognition.ANDROID
      }
  }

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
