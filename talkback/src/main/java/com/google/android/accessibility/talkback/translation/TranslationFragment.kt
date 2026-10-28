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
import android.text.format.Formatter
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.preference.Preference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference
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
  private val serviceCategories = mutableListOf<String>()

  override fun getTitle(): CharSequence = getString(R.string.translation_title)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    onClick("pref_translation_service_choice") { chooseService() }
    onClick("pref_translation_source_choice") { chooseSource() }
    onClick("pref_translation_target_choice") { chooseTarget() }
    onClick("pref_translation_second_target_choice") { chooseSecondTarget() }
    onClick("pref_translation_swap_action") { swap() }
    onClick("pref_translation_auto_service_choice") { chooseAutoService() }
    onClick("pref_translation_untranslated_choice") { chooseUntranslated() }
    for (key in listOf(TranslationSettings.PREF_AUTO, TranslationSettings.PREF_NOTIFICATIONS)) {
      findPreference<Preference>(key)!!.setOnPreferenceChangeListener { _, _ ->
        // After the switch has saved its value.
        view?.post { refresh() }
        true
      }
    }
  }

  override fun onStart() {
    super.onStart()
    AllLanguageDownloads.addListener(downloadsChanged)
    refresh()
    refreshDownloaded()
  }

  override fun onStop() {
    AllLanguageDownloads.removeListener(downloadsChanged)
    super.onStop()
  }

  /** Lists the downloaded languages again as each downloads, and says each quarter of the way. */
  private val downloadsChanged: () -> Unit = {
    if (isAdded) {
      refreshDownloaded()
      val progress = AllLanguageDownloads.progress
      val quarter = progress?.let { it.done * 4 / it.total } ?: -1
      if (progress != null && quarter > announcedQuarter && quarter in 1..3) {
        view?.announceForAccessibility(
          getString(R.string.translation_download_all_progress, progress.done, progress.total)
        )
      }
      announcedQuarter = quarter
    }
  }
  private var announcedQuarter = -1

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

    findPreference<Preference>("pref_translation_auto_service_choice")!!.apply {
      isEnabled = autoServiceUsed()
      summary = serviceSummary(autoService())
    }
    findPreference<Preference>("pref_translation_untranslated_choice")!!.apply {
      isEnabled = autoServiceUsed()
      val untranslated = TranslationSettings.untranslated(prefs)
      summary =
        if (untranslated.isEmpty()) getString(R.string.translation_untranslated_none)
        else ListFormatter.getInstance().format(TranslationLanguages.sorted(untranslated).map { TranslationLanguages.name(it) })
    }
    refreshServiceSettings()
  }

  private fun autoService(): TranslationService = TranslationSettings.autoService(prefs, onDevice != null)

  /** Whether Automatic translation's service is used: for all speech, or for notifications. */
  private fun autoServiceUsed(): Boolean = TranslationSettings.auto(prefs) || TranslationSettings.notifications(prefs)

  /**
   * A category of settings for each service in use, the Translation service and Automatic
   * translation's, for those that have settings.
   */
  private fun refreshServiceSettings() {
    for (key in serviceCategories) findPreference<Preference>(key)?.let { preferenceScreen.removePreference(it) }
    serviceCategories.clear()
    val services = buildList {
      add(service())
      if (autoServiceUsed()) add(autoService())
    }.distinct().filter { it != TranslationService.AI }
    for ((index, service) in services.withIndex()) {
      val category =
        AccessibilitySuitePreferenceCategory(requireContext()).apply {
          key = "pref_translation_${service.id}_settings"
          order = SERVICE_CATEGORY_ORDER + index
          title = getString(R.string.title_ai_provider_settings, getString(service.label))
        }
      preferenceScreen.addPreference(category)
      serviceCategories += category.key
      if (service == TranslationService.LIBRETRANSLATE) {
        category.addPreference(row(getString(R.string.ai_provider_address), TranslationSettings.libreTranslateAddress(prefs)) { askAddress() })
      }
      if (service.needsKey || service == TranslationService.LIBRETRANSLATE) {
        val set = TranslationSettings.apiKey(prefs, service).isNotEmpty()
        category.addPreference(
          row(
            getString(R.string.title_pref_gemini_api_key_of, getString(service.label)),
            getString(if (set) R.string.ai_provider_set else R.string.ai_provider_not_set),
          ) {
            askKey(service)
          }
        )
      }
      if (service == TranslationService.MICROSOFT) {
        category.addPreference(
          row(
            getString(R.string.title_pref_translation_region),
            TranslationSettings.microsoftRegion(prefs).ifEmpty { getString(R.string.translation_region_global) },
          ) {
            askRegion()
          }
        )
      }
      if (service == TranslationService.ON_DEVICE) {
        val summary =
          if (downloaded.isEmpty()) null
          else
            getString(
              R.string.translation_languages_summary,
              ListFormatter.getInstance().format(TranslationLanguages.sorted(downloaded).map { TranslationLanguages.name(it) }),
            )
        category.addPreference(row(getString(R.string.title_pref_translation_languages), summary) { chooseOnDeviceLanguage() })
        category.addPreference(downloadAllRow())
        category.addPreference(
          AccessibilitySuiteSwitchPreference(requireContext()).apply {
            key = TranslationSettings.PREF_WIFI_ONLY
            setDefaultValue(false)
            isIconSpaceReserved = false
            setTitle(R.string.title_pref_translation_wifi_only)
          }
        )
      }
    }
  }

  /** Download all languages: what is left to download, or how far the downloads are. */
  private fun downloadAllRow(): Preference {
    val models = onDevice
    val missing = missingLanguages()
    val progress = AllLanguageDownloads.progress
    val summary =
      when {
        progress != null -> getString(R.string.translation_download_all_progress, progress.done, progress.total)
        downloaded.isNotEmpty() && missing.isEmpty() -> getString(R.string.translation_all_downloaded)
        else -> null
      }
    return row(getString(R.string.title_pref_translation_download_all), summary) {
      if (models == null) return@row
      if (AllLanguageDownloads.isRunning) confirmStopDownloads() else confirmDownloadAll(models)
    }.apply { isEnabled = progress != null || (downloaded.isNotEmpty() && missing.isNotEmpty()) }
  }

  /** The on-device languages that aren't downloaded, known once the downloaded ones are listed. */
  private fun missingLanguages(): List<String> =
    if (downloaded.isEmpty()) emptyList()
    else TranslationLanguages.sorted(onDevice?.languages.orEmpty() - downloaded).filter { onDevice?.isDownloading(it) != true }

  private fun confirmDownloadAll(models: OnDeviceTranslation) {
    val missing = missingLanguages()
    if (missing.isEmpty()) return
    val size = Formatter.formatShortFileSize(requireContext(), missing.size * LANGUAGE_BYTES)
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_download_all)
      .setMessage(resources.getQuantityString(R.plurals.translation_download_all_message, missing.size, missing.size, size))
      .setPositiveButton(R.string.on_device_ai_metered_download) { _, _ ->
        val appContext = requireContext().applicationContext
        AllLanguageDownloads.start(models, missing, TranslationSettings.wifiOnly(prefs)) { progress ->
          val text =
            if (progress.failed == 0) appContext.getString(R.string.translation_all_downloaded)
            else
              appContext.resources.getQuantityString(
                R.plurals.translation_download_all_failed,
                progress.failed,
                progress.failed,
              )
          Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun confirmStopDownloads() {
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.translation_download_all_stop_title)
      .setPositiveButton(R.string.translation_download_all_stop) { _, _ -> AllLanguageDownloads.stop() }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun row(title: String, summary: String?, onClick: () -> Unit): Preference =
    AccessibilitySuitePreference(requireContext()).apply {
      isPersistent = false
      isIconSpaceReserved = false
      this.title = title
      this.summary = summary
      setOnPreferenceClickListener {
        onClick()
        true
      }
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

  private fun chooseAutoService() {
    val choices = TranslationSettings.AUTO_SERVICES.filter { it != TranslationService.ON_DEVICE || onDevice != null }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_auto_service)
      .setSingleChoiceItems(choices.map { getString(it.label) }.toTypedArray(), choices.indexOf(autoService())) {
        dialog,
        which ->
        dialog.dismiss()
        val chosen = choices[which]
        TranslationSettings.setAutoService(prefs, chosen)
        refresh()
        when (TranslationSettings.missing(prefs, chosen)) {
          TranslationSettings.Missing.API_KEY -> askKey(chosen)
          TranslationSettings.Missing.ADDRESS -> askAddress()
          null -> {}
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  /** Chooses the languages Automatic translation leaves as they are, from all languages. */
  private fun chooseUntranslated() {
    val languages = TranslationLanguages.sorted()
    val chosen = TranslationSettings.untranslated(prefs).toMutableSet()
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_translation_untranslated)
      .setMultiChoiceItems(
        languages.map { TranslationLanguages.name(it) }.toTypedArray(),
        languages.map { it in chosen }.toBooleanArray(),
      ) { _, which, checked ->
        if (checked) chosen += languages[which] else chosen -= languages[which]
      }
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        TranslationSettings.setUntranslated(prefs, chosen)
        refresh()
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

  private companion object {
    /** After Automatic translation and before Speech. */
    const val SERVICE_CATEGORY_ORDER = 60
    /** About the size of one language's model. */
    const val LANGUAGE_BYTES = 30L * 1000 * 1000
  }

  private fun onClick(key: String, action: () -> Unit) {
    find(key).setOnPreferenceClickListener {
      action()
      true
    }
  }
}
