/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager.State
import com.google.android.accessibility.talkback.actor.gemini.local.OnDeviceAiSettings.Support
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Settings for running Describe image and Describe screen on a Gemma 4 model on the phone: the
 * switch, the model, and getting the model onto the phone by download or from storage.
 */
class OnDeviceAiFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private lateinit var manager: LocalModelManager
  private lateinit var enablePref: CheckBoxPreference
  private lateinit var modelPref: Preference
  private lateinit var statusPref: Preference
  private lateinit var managePref: Preference
  private lateinit var importPref: Preference
  private lateinit var timeoutPref: Preference
  private lateinit var shortPromptsPref: CheckBoxPreference
  private lateinit var gpuPref: CheckBoxPreference

  private var lastAnnounced = -1
  private var wasDownloading = false

  private val listener: () -> Unit = { if (isAdded) refresh() }

  private val pickModelFile =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        manager.importFrom(uri) { model ->
          if (isAdded) {
            toast(if (model != null) R.string.on_device_ai_import_done else R.string.on_device_ai_import_failed)
          }
        }
      }
    }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_on_device_ai)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    manager = LocalModelManager.get(context)
    val screen = preferenceManager.createPreferenceScreen(context)

    enablePref =
      checkBox(context, OnDeviceAiSettings.PREF_PROVIDER, R.string.pref_on_device_ai_enable) {
        enabled ->
        if (enabled && !manager.isReady()) {
          toast(R.string.pref_on_device_ai_enable_needs_model)
          false
        } else {
          OnDeviceAiSettings.setOnDevice(prefs, enabled)
          true
        }
      }
    modelPref =
      action(context, R.string.pref_on_device_ai_model) { chooseModel() }
    statusPref = action(context, R.string.pref_on_device_ai_download) { onStatusClicked() }
    // No click listener: one that returns true would stop the preference from opening its fragment.
    managePref =
      Preference(context).apply {
        isPersistent = false
        layoutResource = R.layout.listitem_2texts
        setTitle(R.string.pref_on_device_ai_manage)
        fragment = ManageModelsFragment::class.java.name
      }
    importPref =
      action(context, R.string.pref_on_device_ai_import) {
        pickModelFile.launch(arrayOf("application/octet-stream", "*/*"))
      }
    importPref.setSummary(R.string.pref_on_device_ai_import_summary)
    timeoutPref = action(context, R.string.pref_on_device_ai_timeout) { chooseTimeout() }
    shortPromptsPref =
      checkBox(
        context,
        OnDeviceAiSettings.PREF_SHORT_PROMPTS,
        R.string.pref_on_device_ai_short_prompts,
      ) { short ->
        OnDeviceAiSettings.setShortPrompts(prefs, short)
        true
      }
    shortPromptsPref.setSummary(R.string.pref_on_device_ai_short_prompts_summary)
    gpuPref =
      checkBox(context, OnDeviceAiSettings.PREF_GPU, R.string.pref_on_device_ai_gpu) { useGpu ->
        OnDeviceAiSettings.setUseGpu(prefs, useGpu)
        true
      }
    gpuPref.setSummary(R.string.pref_on_device_ai_gpu_summary)

    listOf(enablePref, modelPref, statusPref, managePref, importPref, timeoutPref, shortPromptsPref, gpuPref).forEach {
      screen.addPreference(it)
    }
    preferenceScreen = screen
  }

  override fun onStart() {
    super.onStart()
    manager.addListener(listener)
    refresh()
  }

  override fun onStop() {
    manager.removeListener(listener)
    super.onStop()
  }

  private fun refresh() {
    val context = requireContext()
    val model = manager.preferredModel()
    val state = manager.state
    val busy = state is State.Downloading || state == State.Importing

    val support = support(model)
    enablePref.isChecked = OnDeviceAiSettings.isOnDevice(prefs) && manager.isReady()
    // Without a model there is nothing to turn on, so the summary says to get one first.
    enablePref.isEnabled = support == Support.OK && !busy && manager.isReady()
    enablePref.setSummary(
      when {
        support == Support.UNSUPPORTED_CPU -> R.string.pref_on_device_ai_unsupported_cpu
        support == Support.NOT_ENOUGH_RAM -> R.string.pref_on_device_ai_not_enough_ram
        !manager.isReady() -> R.string.pref_on_device_ai_enable_needs_model
        else -> R.string.pref_on_device_ai_enable_summary
      }
    )

    modelPref.summary =
      context.getString(R.string.on_device_ai_model_summary, model.name, sizeLabel(model))
    modelPref.isEnabled = !busy

    timeoutPref.summary =
      context.getString(
        R.string.pref_on_device_ai_timeout_summary,
        timeoutLabel(OnDeviceAiSettings.timeoutSeconds(prefs)),
      )
    shortPromptsPref.isChecked = OnDeviceAiSettings.shortPrompts(prefs)
    gpuPref.isChecked = OnDeviceAiSettings.useGpu(prefs)

    importPref.isEnabled = !busy

    val installedCount = manager.store.installedModels().size
    managePref.summary =
      if (installedCount == 0) {
        context.getString(R.string.pref_on_device_ai_manage_none)
      } else {
        resources.getQuantityString(
          R.plurals.on_device_ai_models_downloaded,
          installedCount,
          installedCount,
        )
      }

    // Once the model is on the phone there is nothing to download, so the entry goes away.
    statusPref.isVisible = busy || !manager.store.isInstalled(model)

    when {
      state is State.Downloading -> {
        val total = state.model.sizeBytes
        val percent = DownloadProgress.percent(state.bytes, total)
        val sizes =
          context.getString(
            R.string.on_device_ai_progress_sizes,
            Formatter.formatShortFileSize(context, state.bytes),
            Formatter.formatShortFileSize(context, total),
          )
        val seconds = DownloadProgress.secondsLeft(state.bytes, total, state.bytesPerSecond)
        val progress =
          if (seconds == null) sizes
          else
            context.getString(
              R.string.on_device_ai_progress_with_time,
              sizes,
              DateUtils.formatElapsedTime(seconds),
            )
        statusPref.title = context.getString(R.string.pref_on_device_ai_downloading, percent)
        statusPref.summary =
          context.getString(R.string.pref_on_device_ai_downloading_summary, progress)
      }
      state == State.Importing -> {
        statusPref.setTitle(R.string.pref_on_device_ai_importing)
        statusPref.summary = null
      }
      else -> {
        statusPref.title =
          context.getString(R.string.pref_on_device_ai_download, sizeLabel(model))
        statusPref.summary =
          (state as? State.Failed)?.let { context.getString(errorText(it.failure)) }
            ?: context.getString(R.string.pref_on_device_ai_download_summary)
      }
    }
    statusPref.isEnabled = state != State.Importing
    announceProgress(state)
  }

  /** Speaks each quarter of the download and its end, because the numbers change silently. */
  private fun announceProgress(state: State) {
    val view = view ?: return
    if (state is State.Downloading) {
      val milestone = DownloadProgress.milestone(state.bytes, state.model.sizeBytes)
      if (milestone > lastAnnounced) {
        view.announceForAccessibility(getString(R.string.on_device_ai_announce_progress, milestone))
      }
      lastAnnounced = milestone
      wasDownloading = true
    } else {
      if (wasDownloading && manager.isReady()) {
        view.announceForAccessibility(getString(R.string.on_device_ai_announce_done))
      }
      lastAnnounced = -1
      wasDownloading = false
    }
  }

  private fun onStatusClicked() {
    val model = manager.preferredModel()
    when {
      manager.state is State.Downloading -> manager.cancelDownload()
      // Deleting is done in Manage models, so a stray tap here cannot remove a 3 GB download.
      manager.store.isInstalled(model) -> {}
      isMetered() -> confirmMeteredDownload(model)
      else -> manager.startDownload(model)
    }
  }

  private fun confirmMeteredDownload(model: LocalModel) {
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.on_device_ai_metered_title)
      .setMessage(R.string.on_device_ai_metered_message)
      .setPositiveButton(R.string.on_device_ai_metered_download) { _, _ ->
        manager.startDownload(model)
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun chooseModel() {
    val models = manager.availableModels()
    val labels = models.map { modelLabel(it) }.toTypedArray()
    val current = models.indexOf(manager.preferredModel())
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.on_device_ai_choose_model)
      .setSingleChoiceItems(labels, current) { dialog, which ->
        val chosen = models[which]
        if (support(chosen) == Support.NOT_ENOUGH_RAM) {
          toast(R.string.on_device_ai_model_too_big)
        } else {
          OnDeviceAiSettings.setPreferredModel(prefs, chosen)
          refresh()
        }
        dialog.dismiss()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun chooseTimeout() {
    val choices = OnDeviceAiSettings.TIMEOUT_CHOICES_SECONDS
    val labels = choices.map { timeoutLabel(it) }.toTypedArray()
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.on_device_ai_choose_timeout)
      .setSingleChoiceItems(labels, choices.indexOf(OnDeviceAiSettings.timeoutSeconds(prefs))) {
        dialog,
        which ->
        OnDeviceAiSettings.setTimeoutSeconds(prefs, choices[which])
        refresh()
        dialog.dismiss()
      }
      .setNegativeButton(R.string.on_device_ai_cancel, null)
      .show()
  }

  private fun timeoutLabel(seconds: Int): String {
    val minutes = seconds / 60
    return resources.getQuantityString(R.plurals.on_device_ai_timeout_minutes, minutes, minutes)
  }

  private fun isMetered(): Boolean =
    (requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
      .isActiveNetworkMetered

  /** One line for the model picker, such as "SmolVLM2 500M, 0.4 GB (experimental). Tiny and fast." */
  private fun modelLabel(model: LocalModel): String {
    val tags =
      listOfNotNull(
        getString(R.string.on_device_ai_tag_downloaded).takeIf { manager.store.isInstalled(model) },
        getString(R.string.on_device_ai_tag_experimental).takeIf { model.experimental },
        model.license.takeIf { it != "Apache-2.0" },
      )
    val tagText = if (tags.isEmpty()) "" else " (${tags.joinToString(", ")})"
    val memory =
      if (support(model) == Support.NOT_ENOUGH_RAM) {
        getString(R.string.on_device_ai_tag_not_enough_memory)
      } else {
        getString(R.string.on_device_ai_tag_needs_memory, model.minTotalRamGib)
      }
    return getString(
      R.string.on_device_ai_model_entry,
      model.name,
      sizeLabel(model),
      tagText,
      model.note,
      memory,
    )
  }

  private fun sizeLabel(model: LocalModel) = "%.1f GB".format(model.sizeBytes / 1_000_000_000.0)

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

  private fun checkBox(
    context: Context,
    key: String,
    title: Int,
    onChange: (Boolean) -> Boolean,
  ) =
    CheckBoxPreference(context).apply {
      this.key = key
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      setTitle(title)
      setOnPreferenceChangeListener { _, newValue -> onChange(newValue as Boolean) }
    }

  private fun action(context: Context, title: Int, onClick: () -> Unit) =
    Preference(context).apply {
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      setTitle(title)
      setOnPreferenceClickListener {
        onClick()
        true
      }
    }

  /**
   * Whether this phone can run [model]. The developer setting to ignore memory limits lets any
   * model be chosen, to test what happens when Android stops it.
   */
  private fun support(model: LocalModel): Support =
    manager.support(model).let {
      if (it == Support.NOT_ENOUGH_RAM && OnDeviceAiSettings.ignoreMemoryLimits(prefs)) {
        Support.OK
      } else {
        it
      }
    }

  private fun toast(message: Int) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
  }
}
