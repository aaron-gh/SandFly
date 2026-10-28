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

package com.google.android.accessibility.talkback.translation

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiService
import com.google.android.accessibility.talkback.actor.gemini.GeminiApiKey
import java.util.Locale

/** The services that Translate last spoken phrase can use. */
enum class TranslationService(
  val id: String,
  @StringRes val label: Int,
  /** Where to get an API key, or empty when the service needs none. */
  val keyUrl: String = "",
) {
  /** ML Kit on the phone, with a model per language downloaded from Google when first used. */
  ON_DEVICE("on_device", R.string.translation_service_on_device),
  /** The on-device model or AI service that Automatic descriptions uses. */
  AI("ai", R.string.translation_service_ai),
  GOOGLE_CLOUD(
    "google_cloud",
    R.string.translation_service_google_cloud,
    "https://console.cloud.google.com/apis/credentials",
  ),
  DEEPL("deepl", R.string.translation_service_deepl, "https://www.deepl.com/your-account/keys"),
  MICROSOFT(
    "microsoft",
    R.string.translation_service_microsoft,
    "https://portal.azure.com/#create/Microsoft.CognitiveServicesTextTranslation",
  ),
  LIBRETRANSLATE("libretranslate", R.string.translation_service_libretranslate, "https://portal.libretranslate.com"),
  ;

  val needsKey: Boolean
    get() = this == GOOGLE_CLOUD || this == DEEPL || this == MICROSOFT

  // "api_key" in the name keeps it out of settings backups, as the AI services' keys are.
  val keyPref: String
    get() = "pref_translation_${id}_api_key"

  companion object {
    fun of(id: String?): TranslationService? = entries.firstOrNull { it.id == id }
  }
}

/** Translation settings, kept in SandFly's device-protected settings. */
object TranslationSettings {
  const val PREF_SERVICE = "pref_translation_service"
  const val PREF_SOURCE = "pref_translation_source"
  const val PREF_TARGET = "pref_translation_target"
  const val PREF_SECOND_TARGET = "pref_translation_second_target"
  const val PREF_TARGET_VOICE = "pref_translation_target_voice"
  const val PREF_SAY_SOURCE = "pref_translation_say_source"
  const val PREF_COPY = "pref_translation_copy"
  const val PREF_WIFI_ONLY = "pref_translation_wifi_only"
  const val PREF_MICROSOFT_REGION = "pref_translation_microsoft_region"
  const val PREF_LIBRETRANSLATE_ADDRESS = "pref_translation_libretranslate_address"
  const val PREF_AUTO = "pref_translation_auto"
  const val PREF_NOTIFICATIONS = "pref_translation_notifications"
  const val PREF_AUTO_SERVICE = "pref_translation_auto_service"
  const val PREF_UNTRANSLATED = "pref_translation_untranslated"

  /** The services Automatic translation can use; AI services take too long for every utterance. */
  val AUTO_SERVICES = TranslationService.entries.filter { it != TranslationService.AI }

  /** [PREF_SOURCE] when the language is detected. */
  const val DETECT = "auto"
  /** [PREF_TARGET] for the device's language. */
  const val DEVICE = "device"
  /** [PREF_SECOND_TARGET] when text in the target language is left as it is. */
  const val NONE = "none"

  const val LIBRETRANSLATE_DEFAULT_ADDRESS = "https://libretranslate.com"

  /** The chosen service; on-device translation on phones that have it, else the AI service. */
  fun service(prefs: SharedPreferences, onDeviceAvailable: Boolean): TranslationService {
    val chosen = TranslationService.of(prefs.getString(PREF_SERVICE, null))
    return when {
      chosen == TranslationService.ON_DEVICE && !onDeviceAvailable -> TranslationService.AI
      chosen != null -> chosen
      onDeviceAvailable -> TranslationService.ON_DEVICE
      else -> TranslationService.AI
    }
  }

  fun setService(prefs: SharedPreferences, service: TranslationService) =
    prefs.edit { putString(PREF_SERVICE, service.id) }

  fun auto(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_AUTO, false)

  fun setAuto(prefs: SharedPreferences, on: Boolean) = prefs.edit { putBoolean(PREF_AUTO, on) }

  /** Whether notifications in another language are translated, also with Automatic translation off. */
  fun notifications(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_NOTIFICATIONS, false)

  /**
   * The service of Automatic translation: the chosen one; else the Translation service, unless it
   * is the AI service; else on-device translation, or Google Cloud Translation without it.
   */
  fun autoService(prefs: SharedPreferences, onDeviceAvailable: Boolean): TranslationService {
    val chosen = TranslationService.of(prefs.getString(PREF_AUTO_SERVICE, null))
    val main = service(prefs, onDeviceAvailable)
    return when {
      chosen != null && chosen in AUTO_SERVICES && (chosen != TranslationService.ON_DEVICE || onDeviceAvailable) -> chosen
      main in AUTO_SERVICES -> main
      onDeviceAvailable -> TranslationService.ON_DEVICE
      else -> TranslationService.GOOGLE_CLOUD
    }
  }

  fun setAutoService(prefs: SharedPreferences, service: TranslationService) =
    prefs.edit { putString(PREF_AUTO_SERVICE, service.id) }

  /** Languages that Automatic translation leaves as they are, besides the target language. */
  fun untranslated(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(PREF_UNTRANSLATED, null).orEmpty().mapNotNull { TranslationLanguages.normalize(it) }.toSet()

  fun setUntranslated(prefs: SharedPreferences, languages: Set<String>) =
    prefs.edit { putStringSet(PREF_UNTRANSLATED, languages) }

  /** The language text is in, or null to detect it. */
  fun source(prefs: SharedPreferences): String? =
    TranslationLanguages.normalize(prefs.getString(PREF_SOURCE, DETECT))

  /** The language to translate into: the chosen one, or the device's. */
  fun target(prefs: SharedPreferences): String =
    TranslationLanguages.normalize(prefs.getString(PREF_TARGET, DEVICE))
      ?: TranslationLanguages.of(Locale.getDefault())

  fun followsDevice(prefs: SharedPreferences): Boolean =
    TranslationLanguages.normalize(prefs.getString(PREF_TARGET, DEVICE)) == null

  /**
   * The language that text detected to be in the target language is translated into, or null to
   * leave it. Only used while the language is detected.
   */
  fun secondTarget(prefs: SharedPreferences): String? =
    TranslationLanguages.normalize(prefs.getString(PREF_SECOND_TARGET, NONE))

  fun setSource(prefs: SharedPreferences, code: String?) =
    prefs.edit { putString(PREF_SOURCE, code ?: DETECT) }

  fun setTarget(prefs: SharedPreferences, code: String?) =
    prefs.edit { putString(PREF_TARGET, code ?: DEVICE) }

  fun setSecondTarget(prefs: SharedPreferences, code: String?) =
    prefs.edit { putString(PREF_SECOND_TARGET, code ?: NONE) }

  /** Translates from the target language into the source language from now on. */
  fun swap(prefs: SharedPreferences) {
    val source = source(prefs) ?: return
    val target = target(prefs)
    prefs.edit {
      putString(PREF_SOURCE, target)
      putString(PREF_TARGET, source)
    }
  }

  fun targetVoice(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_TARGET_VOICE, true)

  fun saySource(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_SAY_SOURCE, false)

  fun copy(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_COPY, false)

  fun wifiOnly(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_WIFI_ONLY, false)

  fun apiKey(prefs: SharedPreferences, service: TranslationService): String =
    GeminiApiKey.clean(prefs.getString(service.keyPref, null))

  fun setApiKey(prefs: SharedPreferences, service: TranslationService, key: String) =
    prefs.putOrRemove(service.keyPref, GeminiApiKey.clean(key))

  /** The Azure region of the Microsoft Translator resource, such as westeurope, or empty for global. */
  fun microsoftRegion(prefs: SharedPreferences): String =
    prefs.getString(PREF_MICROSOFT_REGION, null)?.trim().orEmpty()

  fun setMicrosoftRegion(prefs: SharedPreferences, region: String) =
    prefs.putOrRemove(PREF_MICROSOFT_REGION, region)

  fun libreTranslateAddress(prefs: SharedPreferences): String =
    prefs.getString(PREF_LIBRETRANSLATE_ADDRESS, null)?.trim().orEmpty().ifEmpty { LIBRETRANSLATE_DEFAULT_ADDRESS }

  fun setLibreTranslateAddress(prefs: SharedPreferences, address: String) =
    prefs.putOrRemove(PREF_LIBRETRANSLATE_ADDRESS, address)

  /**
   * What [service] still needs before it can translate, or null when it can. The AI service and
   * on-device translation are checked by [Translator], which knows the AI service in use.
   */
  fun missing(prefs: SharedPreferences, service: TranslationService): Missing? =
    when {
      service.needsKey && apiKey(prefs, service).isEmpty() -> Missing.API_KEY
      service == TranslationService.LIBRETRANSLATE && !AiService.isValidAddress(libreTranslateAddress(prefs)) ->
        Missing.ADDRESS
      else -> null
    }

  enum class Missing {
    API_KEY,
    ADDRESS,
  }

  private fun SharedPreferences.putOrRemove(key: String, value: String) = edit {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) remove(key) else putString(key, trimmed)
  }
}
