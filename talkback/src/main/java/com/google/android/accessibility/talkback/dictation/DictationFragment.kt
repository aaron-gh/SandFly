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
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import com.google.android.accessibility.talkback.actor.gemini.local.ModelFiles
import com.google.android.accessibility.talkback.dictation.DictationSettings.Recognition
import com.google.android.accessibility.talkback.dictation.WhisperModels.Model
import com.google.android.accessibility.talkback.dictation.WhisperModels.State
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale

/**
 * Dictation settings: how speech is recognized, the Whisper models for recognition on the phone,
 * and the AI service that corrects dictation.
 */
class DictationFragment : TalkbackBaseFragment(R.xml.dictation_preferences) {
  private lateinit var prefs: SharedPreferences
  private lateinit var models: WhisperModels
  private lateinit var recognitionPref: Preference
  private lateinit var aiServicePref: Preference
  private lateinit var googleDownloadPref: Preference
  private val modelPrefs = mutableMapOf<Model, Preference>()
  private var whisperProcessorPref: Preference? = null
  private var announcedMilestone = -1
  private var downloading: Model? = null

  private val listener: () -> Unit = { if (isAdded) refresh() }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_dictation)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    models = WhisperModels.get(context)
    recognitionPref = onClick("pref_dictation_recognition_choice") { chooseRecognition() }
    aiServicePref = onClick("pref_dictation_ai_service_choice") { chooseAiService() }
    googleDownloadPref =
      onClick("pref_dictation_google_download") {
        GoogleSpeechLanguages.download(requireContext())
      }
    if (!WhisperEngines.isAvailable()) return
    // Made here, since the settings filter removes categories that are empty in the XML.
    val category =
      AccessibilitySuitePreferenceCategory(context).apply {
        key = "pref_dictation_whisper_models"
        order = 3
        setTitle(R.string.title_pref_whisper_models)
      }
    preferenceScreen.addPreference(category)
    for (model in Model.entries) {
      val row =
        AccessibilitySuitePreference(context).apply {
          key = "pref_dictation_whisper_${model.id}"
          isPersistent = false
          title = model.label
          setOnPreferenceClickListener {
            onModelClicked(model)
            true
          }
        }
      category.addPreference(row)
      modelPrefs[model] = row
    }
    // Only phones whose GPU Whisper can use have a choice.
    if (WhisperEngines.gpuAvailable()) {
      whisperProcessorPref =
        AccessibilitySuitePreference(context).apply {
          key = "pref_dictation_whisper_processor"
          isPersistent = false
          setTitle(R.string.pref_on_device_ai_processor)
          setOnPreferenceClickListener {
            chooseWhisperProcessor()
            true
          }
        }
      category.addPreference(whisperProcessorPref!!)
    }
  }

  private fun chooseWhisperProcessor() {
    val labels = arrayOf(getString(R.string.processor_cpu), getString(R.string.processor_gpu))
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.pref_on_device_ai_processor)
      .setSingleChoiceItems(labels, if (DictationSettings.whisperOnGpu(prefs)) 1 else 0) { dialog, which ->
        dialog.dismiss()
        DictationSettings.setWhisperOnGpu(prefs, which == 1)
        refresh()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  override fun onStart() {
    super.onStart()
    models.addListener(listener)
    refresh()
    GoogleSpeechLanguages.refresh(requireContext()) { if (isAdded) refresh() }
  }

  override fun onStop() {
    models.removeListener(listener)
    super.onStop()
  }

  private fun refresh() {
    val context = requireContext()
    recognitionPref.summary = recognitionSummary(context)
    googleDownloadPref.title =
      getString(R.string.dictation_google_download, Locale.getDefault().displayLanguage)
    // Only with a Gemini API key, which Rambler needs.
    findPreference<Preference>(DictationSettings.PREF_RAMBLER_LIVE)?.isVisible =
      DictationSettings.ramblerKey(prefs).isNotEmpty()
    googleDownloadPref.isVisible =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        DictationSettings.googleInstalled(context) &&
        GoogleSpeechLanguages.hasPhoneLanguage() == false
    val active = models.activeModel()
    val state = models.state
    for ((model, row) in modelPrefs) {
      row.summary =
        when {
          state is State.Downloading && state.model == model ->
            getString(R.string.pref_on_device_ai_downloading, models.percent(state))
          state is State.Failed && state.model == model -> getString(errorText(state.failure))
          model == active -> getString(R.string.ai_provider_in_use)
          models.isInstalled(model) ->
            "${size(model)}, ${getString(R.string.on_device_ai_tag_downloaded)}"
          else -> size(model)
        }
    }
    aiServicePref.summary = aiServiceSummary(context)
    whisperProcessorPref?.summary =
      getString(if (DictationSettings.whisperOnGpu(prefs)) R.string.processor_gpu else R.string.processor_cpu)
    announceProgress(state)
  }

  private fun recognitionSummary(context: Context): String {
    val chosen = DictationSettings.recognition(prefs)
    val effective = DictationSettings.effectiveRecognition(context, prefs)
    return when {
      chosen == Recognition.AUTOMATIC ->
        getString(R.string.dictation_recognition_automatic_using, recognitionLabel(effective))
      chosen == effective -> recognitionLabel(chosen)
      chosen == Recognition.GOOGLE -> getString(R.string.dictation_google_unavailable)
      chosen == Recognition.RAMBLER -> getString(R.string.dictation_rambler_needs_key)
      else -> getString(R.string.dictation_whisper_needs_model)
    }
  }

  private fun recognitionLabel(recognition: Recognition): String =
    getString(
      when (recognition) {
        Recognition.AUTOMATIC -> R.string.dictation_recognition_automatic
        Recognition.GOOGLE -> R.string.dictation_recognition_google
        Recognition.RAMBLER -> R.string.dictation_recognition_rambler
        Recognition.WHISPER -> R.string.dictation_recognition_whisper
        Recognition.ANDROID -> R.string.dictation_recognition_android
      }
    )

  private fun chooseRecognition() {
    val choices = Recognition.entries.filter { it != Recognition.WHISPER || WhisperEngines.isAvailable() }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.title_pref_dictation_recognition)
      .setSingleChoiceItems(
        choices.map { recognitionLabel(it) }.toTypedArray(),
        choices.indexOf(DictationSettings.recognition(prefs)),
      ) { dialog, which ->
        dialog.dismiss()
        DictationSettings.setRecognition(prefs, choices[which])
        refresh()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  /** [DictationSettings.AI_SAME], the services that are set up, and on-device AI when installed. */
  private fun aiServiceChoices(context: Context): List<String> =
    buildList {
      add(DictationSettings.AI_SAME)
      AiProvider.entries
        .filter { it != AiProvider.NONE && it.isReady(prefs) }
        .forEach { add(it.id) }
      if (LocalModelManager.get(context).isReady()) add(DictationSettings.AI_DEVICE)
    }

  private fun aiServiceLabel(service: String): String =
    when (service) {
      DictationSettings.AI_SAME -> getString(R.string.dictation_ai_same)
      DictationSettings.AI_DEVICE -> getString(R.string.title_pref_on_device_ai)
      else -> AiProvider.of(service)?.let { getString(it.label) } ?: service
    }

  private fun aiServiceSummary(context: Context): String {
    val label = aiServiceLabel(DictationSettings.aiService(prefs))
    return if (DictationSettings.correctingService(context, prefs) == null) {
      getString(R.string.ai_provider_chosen_not_ready, label)
    } else {
      label
    }
  }

  private fun chooseAiService() {
    val context = requireContext()
    val choices = aiServiceChoices(context)
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_dictation_ai_service)
      .setSingleChoiceItems(
        choices.map { aiServiceLabel(it) }.toTypedArray(),
        choices.indexOf(DictationSettings.aiService(prefs)),
      ) { dialog, which ->
        dialog.dismiss()
        DictationSettings.setAiService(prefs, choices[which])
        refresh()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun onModelClicked(model: Model) {
    val state = models.state
    when {
      state is State.Downloading -> if (state.model == model) models.cancelDownload()
      models.isInstalled(model) -> modelActions(model)
      isMetered() ->
        AlertDialog.Builder(requireContext())
          .setTitle(R.string.on_device_ai_metered_title)
          .setMessage(size(model))
          .setPositiveButton(R.string.on_device_ai_metered_download) { _, _ -> models.startDownload(model) }
          .setNegativeButton(R.string.on_device_ai_cancel, null)
          .show()
      else -> models.startDownload(model)
    }
  }

  /** Offers to use or delete a downloaded model. */
  private fun modelActions(model: Model) {
    val use = getString(R.string.dictation_whisper_use)
    val delete = getString(R.string.manage_models_delete)
    val actions = if (models.activeModel() == model) arrayOf(delete) else arrayOf(use, delete)
    AlertDialog.Builder(requireContext())
      .setTitle(model.label)
      .setItems(actions) { _, which ->
        if (actions[which] == use) {
          models.setActive(model)
        } else {
          confirmDelete(model)
        }
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun confirmDelete(model: Model) {
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.manage_models_delete_title, model.label))
      .setMessage(getString(R.string.manage_models_delete_message, size(model)))
      .setPositiveButton(R.string.manage_models_delete) { _, _ -> models.delete(model) }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  /** Speaks each quarter of the download and its end, because the numbers change silently. */
  private fun announceProgress(state: State) {
    val view = view ?: return
    if (state is State.Downloading) {
      val milestone = models.percent(state) / 25 * 25
      if (milestone > announcedMilestone) {
        view.announceForAccessibility(getString(R.string.on_device_ai_announce_progress, milestone))
      }
      announcedMilestone = milestone
      downloading = state.model
    } else {
      downloading?.let {
        if (models.isInstalled(it)) {
          view.announceForAccessibility(getString(R.string.on_device_ai_announce_done))
        }
      }
      announcedMilestone = -1
      downloading = null
    }
  }

  private fun size(model: Model): String = Formatter.formatShortFileSize(requireContext(), model.sizeBytes)

  private fun isMetered(): Boolean =
    (requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
      .isActiveNetworkMetered

  private fun errorText(failure: ModelFiles.Failure) =
    when (failure) {
      ModelFiles.Failure.NETWORK -> R.string.on_device_ai_error_network
      ModelFiles.Failure.HTTP -> R.string.on_device_ai_error_http
      ModelFiles.Failure.SIZE_MISMATCH,
      ModelFiles.Failure.HASH_MISMATCH -> R.string.on_device_ai_error_corrupt
      ModelFiles.Failure.STORAGE -> R.string.on_device_ai_error_storage
      ModelFiles.Failure.LOW_SPACE -> R.string.on_device_ai_error_low_space
      ModelFiles.Failure.TIME_LIMIT -> R.string.on_device_ai_error_time_limit
    }

  private fun onClick(key: String, action: () -> Unit): Preference =
    findPreference<Preference>(key)!!.apply {
      setOnPreferenceClickListener {
        action()
        true
      }
    }
}
