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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.ListFormatter
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiService
import com.google.android.accessibility.talkback.actor.gemini.AiTextRequests
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText
import java.util.Locale

/**
 * Translation settings: the service, the languages, the service's API key or address, the
 * languages downloaded for on-device translation, and how translations are spoken.
 */
class TranslationFragment : TalkbackBaseFragment(R.xml.translation_preferences) {
  private lateinit var prefs: SharedPreferences
  private val onDevice: OnDeviceTranslation? by lazy { OnDeviceTranslations.get() }
  private var downloaded: Set<String> = emptySet()

  override fun getTitle(): CharSequence = getString(R.string.translation_title)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    onClick("pref_translation_service_choice") { chooseService() }
    onClick("pref_translation_source_choice") { chooseSource() }
    onClick("pref_translation_target_choice") { chooseTarget() }
    onClick("pref_translation_second_target_choice") { chooseSecondTarget() }
    onClick("pref_translation_swap_action") { swap() }
    onClick("pref_translation_address_row") { askAddress() }
    onClick("pref_translation_key_row") { askKey(service()) }
    onClick("pref_translation_region_row") { askRegion() }
    onClick("pref_translation_languages_row") { chooseOnDeviceLanguage() }
  }

  override fun onStart() {
    super.onStart()
    refresh()
    refreshDownloaded()
  }

  private fun service(): TranslationService = TranslationSettings.service(prefs, onDevice != null)

  private fun refresh() {
    if (!isAdded) return
    val service = service()
    val source = TranslationSettings.source(prefs)
    find("pref_translation_service_choice").summary = serviceSummary(service)
    find("pref_translation_source_choice").summary = source?.let { TranslationLanguages.name(it) } ?: getString(R.string.translation_detect)
    find("pref_translation_target_choice").summary = targetLabel()
    find("pref_translation_second_target_choice").apply {
      isVisible = source == null
      summary = TranslationSettings.secondTarget(prefs)?.let { TranslationLanguages.name(it) }
        ?: getString(R.string.translation_second_none)
    }
    find("pref_translation_swap_action").isVisible = source != null

    findPreference<PreferenceCategory>("pref_translation_service_settings")!!.apply {
      isVisible = service != TranslationService.AI
      title = getString(R.string.title_ai_provider_settings, getString(service.label))
    }
    find("pref_translation_address_row").apply {
      isVisible = service == TranslationService.LIBRETRANSLATE
      summary = TranslationSettings.libreTranslateAddress(prefs)
    }
    find("pref_translation_key_row").apply {
      isVisible = service.needsKey || service == TranslationService.LIBRETRANSLATE
      title = getString(R.string.title_pref_gemini_api_key_of, getString(service.label))
      summary =
        getString(
          if (TranslationSettings.apiKey(prefs, service).isEmpty()) R.string.ai_provider_not_set
          else R.string.ai_provider_set
        )
    }
    find("pref_translation_region_row").apply {
      isVisible = service == TranslationService.MICROSOFT
      summary = TranslationSettings.microsoftRegion(prefs).ifEmpty { getString(R.string.translation_region_global) }
    }
    find("pref_translation_languages_row").apply {
      isVisible = service == TranslationService.ON_DEVICE
      summary =
        if (downloaded.isEmpty()) null
        else
          getString(
            R.string.translation_languages_summary,
            ListFormatter.getInstance().format(TranslationLanguages.sorted(downloaded).map { TranslationLanguages.name(it) }),
          )
    }
    findPreference<Preference>(TranslationSettings.PREF_WIFI_ONLY)?.isVisible = service == TranslationService.ON_DEVICE
  }

  private fun refreshDownloaded() {
    onDevice?.downloaded {
      downloaded = it
      refresh()
    }
  }

  private fun serviceSummary(service: TranslationService): String {
    val label = getString(service.label)
    val ready =
      when (service) {
        TranslationService.ON_DEVICE -> true
        TranslationService.AI -> AiTextRequests.isSetUp(requireContext())
        else -> TranslationSettings.missing(prefs, service) == null
      }
    return if (ready) label else getString(R.string.translation_service_not_set_up, label)
  }

  private fun targetLabel(): String =
    if (TranslationSettings.followsDevice(prefs)) {
      getString(R.string.translation_device_language, TranslationLanguages.name(TranslationSettings.target(prefs)))
    } else {
      TranslationLanguages.name(TranslationSettings.target(prefs))
    }

  /** The languages the service in use translates, all of them for the web services. */
  private fun languages(): List<String> =
    TranslationLanguages.sorted(if (service() == TranslationService.ON_DEVICE) onDevice?.languages else null)

  private fun chooseService() {
    val choices = TranslationService.entries.filter { it != TranslationService.ON_DEVICE || onDevice != null }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_service)
      .setSingleChoiceItems(choices.map { getString(it.label) }.toTypedArray(), choices.indexOf(service())) {
        dialog,
        which ->
        dialog.dismiss()
        val chosen = choices[which]
        TranslationSettings.setService(prefs, chosen)
        refresh()
        // A service that needs a key asks for it at once.
        when (TranslationSettings.missing(prefs, chosen)) {
          TranslationSettings.Missing.API_KEY -> askKey(chosen)
          TranslationSettings.Missing.ADDRESS -> askAddress()
          null -> {}
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun chooseLanguage(
    title: Int,
    first: String,
    current: String?,
    choose: (String?) -> Unit,
  ) {
    val languages = languages()
    val labels = listOf(first) + languages.map { TranslationLanguages.name(it) }
    val checked = if (current == null) 0 else languages.indexOf(current) + 1
    AlertDialog.Builder(requireContext())
      .setTitle(title)
      .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
        dialog.dismiss()
        choose(if (which == 0) null else languages[which - 1])
        refresh()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun chooseSource() =
    chooseLanguage(
      R.string.title_pref_translation_source,
      getString(R.string.translation_detect),
      TranslationSettings.source(prefs),
    ) {
      TranslationSettings.setSource(prefs, it)
    }

  private fun chooseTarget() =
    chooseLanguage(
      R.string.title_pref_translation_target,
      getString(R.string.translation_device_language, TranslationLanguages.name(TranslationLanguages.of(Locale.getDefault()))),
      if (TranslationSettings.followsDevice(prefs)) null else TranslationSettings.target(prefs),
    ) {
      TranslationSettings.setTarget(prefs, it)
    }

  private fun chooseSecondTarget() =
    chooseLanguage(
      R.string.title_pref_translation_second_target,
      getString(R.string.translation_second_none),
      TranslationSettings.secondTarget(prefs),
    ) {
      TranslationSettings.setSecondTarget(prefs, it)
    }

  private fun swap() {
    TranslationSettings.swap(prefs)
    refresh()
    val source = TranslationSettings.source(prefs) ?: return
    view?.announceForAccessibility(
      getString(
        R.string.translation_swapped,
        TranslationLanguages.name(source),
        TranslationLanguages.name(TranslationSettings.target(prefs)),
      )
    )
  }

  private fun chooseOnDeviceLanguage() {
    val models = onDevice ?: return
    val languages = TranslationLanguages.sorted(models.languages)
    val labels =
      languages.map {
        val name = TranslationLanguages.name(it)
        when {
          models.isDownloading(it) -> getString(R.string.translation_language_downloading, name)
          it in downloaded -> getString(R.string.translation_language_downloaded, name)
          else -> name
        }
      }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_languages)
      .setItems(labels.toTypedArray()) { _, which ->
        val language = languages[which]
        when {
          models.isDownloading(language) -> {}
          language in downloaded -> confirmDelete(models, language)
          else -> confirmDownload(models, language)
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun confirmDownload(models: OnDeviceTranslation, language: String) {
    val name = TranslationLanguages.name(language)
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.translation_download_title, name))
      .setMessage(R.string.translation_download_message)
      .setPositiveButton(R.string.on_device_ai_metered_download) { _, _ ->
        val appContext = requireContext().applicationContext
        models.download(language, TranslationSettings.wifiOnly(prefs)) { ok ->
          val text = appContext.getString(if (ok) R.string.translation_download_done else R.string.translation_download_failed, name)
          Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
          if (isAdded) refreshDownloaded()
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun confirmDelete(models: OnDeviceTranslation, language: String) {
    val name = TranslationLanguages.name(language)
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.manage_models_delete_title, name))
      .setPositiveButton(R.string.manage_models_delete) { _, _ ->
        models.delete(language) { if (isAdded) refreshDownloaded() }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun askKey(service: TranslationService) {
    // A visible password field, so that the key is not learned by the keyboard or offered as a
    // suggestion, but can still be read back after pasting it.
    val key =
      field(
        R.string.gemini_api_key_hint,
        TranslationSettings.apiKey(prefs, service),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
      )
    val label = getString(service.label)
    val dialog =
      AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.title_pref_gemini_api_key_of, label))
        .setMessage(getString(R.string.translation_key_message, label))
        .setView(padded(key))
        .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
          TranslationSettings.setApiKey(prefs, service, key.text.toString())
          refresh()
        }
        .setNegativeButton(android.R.string.cancel, null)
    if (service.keyUrl.isNotEmpty()) {
      dialog.setNeutralButton(R.string.gemini_api_key_get) { _, _ ->
        try {
          startActivity(Intent(Intent.ACTION_VIEW, service.keyUrl.toUri()))
        } catch (e: ActivityNotFoundException) {
          // No browser.
        }
      }
    }
    dialog.show()
  }

  private fun askRegion() {
    val region = field(R.string.title_pref_translation_region, TranslationSettings.microsoftRegion(prefs), InputType.TYPE_CLASS_TEXT)
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_region)
      .setMessage(R.string.translation_region_message)
      .setView(padded(region))
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        TranslationSettings.setMicrosoftRegion(prefs, region.text.toString())
        refresh()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun askAddress() {
    val address =
      field(
        R.string.translation_address_hint,
        TranslationSettings.libreTranslateAddress(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
      )
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.ai_provider_address)
      .setMessage(R.string.translation_address_message)
      .setView(padded(address))
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        val text = address.text.toString().trim()
        if (text.isNotEmpty() && !AiService.isValidAddress(text)) {
          Toast.makeText(requireContext(), R.string.ai_service_address_invalid, Toast.LENGTH_LONG).show()
        } else {
          TranslationSettings.setLibreTranslateAddress(prefs, text)
          refresh()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun field(hint: Int, value: String, inputType: Int): KeyboardSafeEditText =
    KeyboardSafeEditText(requireContext()).apply {
      this.inputType = inputType
      isSingleLine = true
      setHint(hint)
      setText(value)
      // Android offers Paste only for a field with input focus.
      post { requestFocus() }
    }

  /** [view] lined up with the dialog's message. */
  private fun padded(view: View): FrameLayout {
    val padding = (24 * resources.displayMetrics.density).toInt()
    return FrameLayout(requireContext()).apply {
      setPadding(padding, 0, padding, 0)
      addView(view)
    }
  }

  private fun find(key: String): Preference = findPreference(key)!!

  private fun onClick(key: String, action: () -> Unit) {
    find(key).setOnPreferenceClickListener {
      action()
      true
    }
  }
}
