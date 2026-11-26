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

package com.google.android.accessibility.talkback.textrecognition

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.local.ModelFiles
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.textrecognition.CyrillicTextModels.Model
import com.google.android.accessibility.talkback.textrecognition.CyrillicTextModels.State
import java.text.Collator
import java.util.Locale

/**
 * Cyrillic text recognition: a row for each language's Tesseract model, which downloads it, stops
 * its download, or deletes it.
 */
class CyrillicTextRecognitionFragment :
  TalkbackBaseFragment(R.xml.cyrillic_text_recognition_preferences) {
  private lateinit var models: CyrillicTextModels
  private val modelPrefs = LinkedHashMap<Model, Preference>()
  private var announcedMilestone = -1
  private var downloading: Model? = null
  private val listener: () -> Unit = { if (isAdded) refresh() }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_cyrillic_text_recognition)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    val context = requireContext()
    models = CyrillicTextModels.get(context)
    val collator = Collator.getInstance(Locale.getDefault())
    val sorted = Model.entries.sortedWith(compareBy(collator) { name(it) })
    for ((index, model) in sorted.withIndex()) {
      val row =
        AccessibilitySuitePreference(context).apply {
          key = "pref_cyrillic_text_model_${model.file.code}"
          isPersistent = false
          order = index
          title = name(model)
          setOnPreferenceClickListener {
            onModelClicked(model)
            true
          }
        }
      preferenceScreen.addPreference(row)
      modelPrefs[model] = row
    }
  }

  override fun onStart() {
    super.onStart()
    models.addListener(listener)
    refresh()
  }

  override fun onStop() {
    models.removeListener(listener)
    super.onStop()
  }

  private fun name(model: Model): String = model.locale.getDisplayName(Locale.getDefault())

  private fun refresh() {
    val state = models.state
    for ((model, row) in modelPrefs) {
      row.summary =
        when {
          state is State.Downloading && state.model == model ->
            getString(R.string.pref_on_device_ai_downloading, models.percent(state))
          state is State.Failed && state.model == model -> getString(errorText(state.failure))
          models.isInstalled(model) ->
            "${size(model.file.sizeBytes)}, ${getString(R.string.on_device_ai_tag_downloaded)}"
          else -> size(models.downloadBytes(model))
        }
    }
    announceProgress(state)
  }

  private fun onModelClicked(model: Model) {
    val state = models.state
    when {
      state is State.Downloading -> if (state.model == model) models.cancelDownload()
      models.isInstalled(model) -> confirmDelete(model)
      isMetered() ->
        AlertDialog.Builder(requireContext())
          .setTitle(R.string.on_device_ai_metered_title)
          .setMessage(size(models.downloadBytes(model)))
          .setPositiveButton(R.string.on_device_ai_metered_download) { _, _ ->
            models.startDownload(model)
          }
          .setNegativeButton(R.string.on_device_ai_cancel, null)
          .show()
      else -> models.startDownload(model)
    }
  }

  private fun confirmDelete(model: Model) {
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.manage_models_delete_title, name(model)))
      .setMessage(getString(R.string.manage_models_delete_message, size(models.deleteBytes(model))))
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

  private fun size(bytes: Long): String = Formatter.formatShortFileSize(requireContext(), bytes)

  private fun isMetered(): Boolean =
    (requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
      .isActiveNetworkMetered

  private fun errorText(failure: ModelFiles.Failure) =
    when (failure) {
      ModelFiles.Failure.NETWORK -> R.string.on_device_ai_error_network
      ModelFiles.Failure.HTTP -> R.string.cyrillic_text_model_error_http
      ModelFiles.Failure.SIZE_MISMATCH,
      ModelFiles.Failure.HASH_MISMATCH -> R.string.on_device_ai_error_corrupt
      ModelFiles.Failure.STORAGE -> R.string.on_device_ai_error_storage
      ModelFiles.Failure.LOW_SPACE -> R.string.on_device_ai_error_low_space
      ModelFiles.Failure.TIME_LIMIT -> R.string.on_device_ai_error_time_limit
    }
}
