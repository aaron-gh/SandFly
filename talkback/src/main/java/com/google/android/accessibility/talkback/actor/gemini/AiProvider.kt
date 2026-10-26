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

package com.google.android.accessibility.talkback.actor.gemini

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R

/**
 * The AI services that Describe image and Describe screen can use. Gemini is asked through its own
 * API; the others through their OpenAI-compatible chat API, at [address]. Each keeps its own key
 * and model, so that switching between them loses neither.
 */
enum class AiProvider(
  val id: String,
  @StringRes val label: Int,
  /** The service's OpenAI-compatible API, or empty for Gemini and for an address the user enters. */
  val address: String,
  /** Where to get an API key, or empty. */
  val keyUrl: String,
) {
  GEMINI("gemini", R.string.ai_provider_gemini, "", GeminiApiKey.GET_KEY_URL),
  CLAUDE(
    "claude",
    R.string.ai_provider_claude,
    "https://api.anthropic.com/v1",
    "https://console.anthropic.com/settings/keys",
  ),
  OPENAI(
    "openai",
    R.string.ai_provider_openai,
    "https://api.openai.com/v1",
    "https://platform.openai.com/api-keys",
  ),
  MISTRAL(
    "mistral",
    R.string.ai_provider_mistral,
    "https://api.mistral.ai/v1",
    "https://console.mistral.ai/api-keys",
  ),
  GROQ("groq", R.string.ai_provider_groq, "https://api.groq.com/openai/v1", "https://console.groq.com/keys"),
  OPENROUTER(
    "openrouter",
    R.string.ai_provider_openrouter,
    "https://openrouter.ai/api/v1",
    "https://openrouter.ai/settings/keys",
  ),
  OLLAMA("ollama", R.string.ai_provider_ollama, "https://ollama.com/v1", "https://ollama.com/settings/keys"),
  OTHER("other", R.string.ai_provider_other, "", ""),
  /** No AI service: images and screens are described on the phone, or only their text is read. */
  NONE("none", R.string.ai_provider_none, "", "");

  // "api_key" in the name keeps it out of settings backups, as the Gemini key is.
  val keyPref: String
    get() = "pref_ai_${id}_api_key"

  val modelPref: String
    get() = "pref_ai_${id}_model"

  fun apiKey(prefs: SharedPreferences): String =
    when (this) {
      GEMINI -> GeminiApiKey.userKey(prefs)
      NONE -> ""
      else -> GeminiApiKey.clean(prefs.getString(keyPref, null))
    }

  fun model(prefs: SharedPreferences): String =
    if (this == GEMINI) GeminiModel.chosen(prefs) else prefs.getString(modelPref, null)?.trim().orEmpty()

  /** The address requests go to: [address], or the one the user entered for [OTHER]. */
  fun addressOf(prefs: SharedPreferences): String =
    if (this == OTHER) prefs.getString(PREF_OTHER_ADDRESS, null)?.trim().orEmpty() else address

  fun setApiKey(prefs: SharedPreferences, key: String) {
    if (this == GEMINI) GeminiApiKey.setUserKey(prefs, key) else prefs.putOrRemove(keyPref, GeminiApiKey.clean(key))
  }

  fun setModel(prefs: SharedPreferences, model: String) {
    if (this == GEMINI) GeminiModel.setChosen(prefs, model) else prefs.putOrRemove(modelPref, model)
  }

  /** What the service still needs before it can be used, in the order it is set up. */
  enum class Missing {
    ADDRESS,
    API_KEY,
    MODEL,
  }

  /** The first thing the service still needs, or null when it can be used. */
  fun missing(prefs: SharedPreferences): Missing? =
    when {
      this == NONE -> null
      // A key built into the app, from local.properties, serves as the user's.
      this == GEMINI ->
        if (GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY).isEmpty()) Missing.API_KEY else null
      !AiService.isValidAddress(addressOf(prefs)) -> Missing.ADDRESS
      apiKey(prefs).isEmpty() -> Missing.API_KEY
      model(prefs).isEmpty() -> Missing.MODEL
      else -> null
    }

  /** Whether requests can go to the service: an https address, an API key and a model. */
  fun isReady(prefs: SharedPreferences): Boolean = missing(prefs) == null

  companion object {
    const val PREF_ACTIVE = "pref_ai_provider"
    const val PREF_OTHER_ADDRESS = "pref_ai_other_address"

    // The single "Other AI service" of earlier versions.
    private const val OLD_ADDRESS = "pref_ai_service_address"
    private const val OLD_MODEL = "pref_ai_service_model"
    private const val OLD_API_KEY = "pref_ai_service_api_key"

    fun of(id: String?): AiProvider? = entries.firstOrNull { it.id == id }

    /**
     * The service in use. A chosen service that isn't set up gives way to one that is, such as
     * Gemini, the first choice, with no key while Claude has one: before the Use switches were
     * removed, a service could be set up without being used.
     */
    @JvmStatic
    fun active(prefs: SharedPreferences): AiProvider {
      migrate(prefs)
      val chosen = of(prefs.getString(PREF_ACTIVE, null)) ?: GEMINI
      if (chosen == NONE || chosen.isReady(prefs)) return chosen
      val ready = fallback(prefs)
      if (ready == NONE) return chosen
      setActive(prefs, ready)
      return ready
    }

    fun setActive(prefs: SharedPreferences, provider: AiProvider) =
      prefs.edit { putString(PREF_ACTIVE, provider.id) }

    /** What is used when the service in use stops being set up: the first one that is, or none. */
    @JvmStatic
    fun fallback(prefs: SharedPreferences): AiProvider =
      entries.firstOrNull { it != NONE && it.isReady(prefs) } ?: NONE

    /** The services that can be chosen: those that are set up, and none. */
    fun choices(prefs: SharedPreferences): List<AiProvider> =
      entries.filter { it == NONE || it.isReady(prefs) }

    /**
     * Keeps the service in use set up after [provider]'s settings change: a service that has just
     * been set up is used when the one in use can't be, and one that no longer is set up stops
     * being used.
     */
    @JvmStatic
    fun afterSetUpChanged(prefs: SharedPreferences, provider: AiProvider) {
      val active = active(prefs)
      when {
        provider.isReady(prefs) && (active == NONE || !active.isReady(prefs)) -> setActive(prefs, provider)
        active == provider && !provider.isReady(prefs) -> setActive(prefs, fallback(prefs))
      }
    }

    fun setOtherAddress(prefs: SharedPreferences, address: String) =
      prefs.putOrRemove(PREF_OTHER_ADDRESS, address)

    /**
     * Moves the one other AI service of earlier versions to the service its address belongs to, or
     * to [OTHER], and uses it when it was set up.
     */
    private fun migrate(prefs: SharedPreferences) {
      if (prefs.contains(PREF_ACTIVE)) return
      val address = prefs.getString(OLD_ADDRESS, null)?.trim().orEmpty()
      val model = prefs.getString(OLD_MODEL, null)?.trim().orEmpty()
      val key = prefs.getString(OLD_API_KEY, null)?.trim().orEmpty()
      val provider =
        if (address.isEmpty()) null
        else entries.firstOrNull { it.address.isNotEmpty() && sameService(address, it.address) } ?: OTHER
      prefs.edit {
        if (provider != null) {
          if (provider == OTHER) putString(PREF_OTHER_ADDRESS, address)
          if (model.isNotEmpty()) putString(provider.modelPref, model)
          if (key.isNotEmpty()) putString(provider.keyPref, key)
        }
        remove(OLD_ADDRESS)
        remove(OLD_MODEL)
        remove(OLD_API_KEY)
        val used = provider != null && model.isNotEmpty() && key.isNotEmpty()
        putString(PREF_ACTIVE, if (used) provider!!.id else GEMINI.id)
      }
    }

    private fun sameService(address: String, known: String): Boolean =
      address.trimEnd('/').removeSuffix("/chat/completions").equals(known, ignoreCase = true)

    private fun SharedPreferences.putOrRemove(key: String, value: String) = edit {
      val trimmed = value.trim()
      if (trimmed.isEmpty()) remove(key) else putString(key, trimmed)
    }
  }
}
