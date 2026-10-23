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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText
import java.io.IOException
import java.util.concurrent.Executors

/**
 * The settings of one AI service other than Gemini, such as Claude settings: its API key, its
 * model, and for [AiProvider.OTHER] its address. Once it is set up, it can be chosen as the AI
 * service.
 */
class AiProviderFragment : TalkbackBaseFragment() {
  private val provider: AiProvider by lazy {
    AiProvider.of(arguments?.getString(ARG_PROVIDER)) ?: AiProvider.OTHER
  }
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(requireContext()) }

  override fun getTitle(): CharSequence =
    getString(R.string.title_ai_provider_settings, getString(provider.label))

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    // Built in code, so the device-protected settings are set here, as an XML screen does.
    preferenceManager.setStorageDeviceProtected()
    build()
  }

  private fun build() {
    val context = requireContext()
    val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(context)
    if (provider == AiProvider.OTHER) {
      screen.addPreference(
        row(
          getString(R.string.ai_provider_address),
          provider.addressOf(prefs).ifEmpty { getString(R.string.ai_provider_not_set) },
        ) {
          askAddress()
        }
      )
    }
    screen.addPreference(
      row(
        getString(R.string.title_pref_gemini_api_key_of, getString(provider.label)),
        getString(if (provider.apiKey(prefs).isEmpty()) R.string.ai_provider_not_set else R.string.ai_provider_set),
      ) {
        askKey()
      }
    )
    // A model is chosen from the service's list, which needs its address and key.
    val missing = provider.missing(prefs)
    val canChooseModel = missing == null || missing == AiProvider.Missing.MODEL
    screen.addPreference(
      row(
        getString(R.string.title_pref_gemini_model_of, getString(provider.label)),
        if (canChooseModel) provider.model(prefs).ifEmpty { getString(R.string.ai_provider_choose_model) }
        else missingText(missing!!),
      ) {
        chooseModel()
      }.apply { isEnabled = canChooseModel }
    )
    preferenceScreen = screen
  }

  private fun missingText(missing: AiProvider.Missing): String =
    when (missing) {
      AiProvider.Missing.ADDRESS -> getString(R.string.ai_provider_needs_address)
      AiProvider.Missing.API_KEY -> getString(R.string.ai_provider_needs_key, getString(provider.label))
      AiProvider.Missing.MODEL -> getString(R.string.ai_provider_not_ready)
    }

  private fun row(title: CharSequence, summary: CharSequence, onClick: () -> Unit): Preference =
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

  private fun field(hint: Int, value: String, inputType: Int): KeyboardSafeEditText =
    KeyboardSafeEditText(requireContext()).apply {
      this.inputType = inputType
      isSingleLine = true
      setHint(hint)
      setText(value)
    }

  /** [view] lined up with the dialog's message. */
  private fun padded(view: android.view.View): FrameLayout {
    val padding = (24 * resources.displayMetrics.density).toInt()
    return FrameLayout(requireContext()).apply {
      setPadding(padding, 0, padding, 0)
      addView(view)
    }
  }

  private fun askKey() {
    // A visible password field, so that the key is not learned by the keyboard or offered as a
    // suggestion, but can still be read back after pasting it.
    val key =
      field(
        R.string.gemini_api_key_hint,
        provider.apiKey(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
      )
    val dialog =
      AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.title_pref_gemini_api_key_of, getString(provider.label)))
        .setMessage(getString(R.string.ai_provider_key_message, getString(provider.label)))
        .setView(padded(key))
        .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
          provider.setApiKey(prefs, key.text.toString())
          AiProvider.afterSetUpChanged(prefs, provider)
          build()
        }
        .setNegativeButton(android.R.string.cancel, null)
    if (provider.keyUrl.isNotEmpty()) {
      dialog.setNeutralButton(R.string.gemini_api_key_get) { _, _ ->
        try {
          startActivity(Intent(Intent.ACTION_VIEW, provider.keyUrl.toUri()))
        } catch (e: ActivityNotFoundException) {
          // No browser.
        }
      }
    }
    dialog.show()
  }

  private fun askAddress() {
    val address =
      field(
        R.string.ai_service_address_hint,
        provider.addressOf(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
      )
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.ai_provider_address)
      .setMessage(R.string.ai_provider_address_message)
      .setView(padded(address))
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        val text = address.text.toString().trim()
        if (text.isNotEmpty() && !AiService.isValidAddress(text)) {
          Toast.makeText(requireContext(), R.string.ai_service_address_invalid, Toast.LENGTH_LONG).show()
        } else {
          AiProvider.setOtherAddress(prefs, text)
          AiProvider.afterSetUpChanged(prefs, provider)
          build()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  /**
   * Lists the service's models once they are fetched, with a way to type a model name first.
   * Nothing opens until the list is ready; when it can't be fetched, the reason is shown.
   */
  private fun chooseModel() {
    val context = requireContext().applicationContext
    val address = provider.addressOf(prefs)
    if (!AiService.isValidAddress(address)) {
      Toast.makeText(context, R.string.ai_provider_needs_address, Toast.LENGTH_LONG).show()
      return
    }
    val key = provider.apiKey(prefs)
    if (key.isEmpty() && provider != AiProvider.OTHER) {
      Toast.makeText(context, getString(R.string.ai_provider_needs_key, getString(provider.label)), Toast.LENGTH_LONG)
        .show()
      return
    }
    Toast.makeText(context, R.string.gemini_model_loading, Toast.LENGTH_SHORT).show()
    executor.execute {
      val result = runCatching { AiModels.list(provider, address, key) }
      main.post {
        if (!isAdded) return@post
        result.fold(
          { models -> showModels(models) },
          { error ->
            val reason = (error as? IOException)?.message ?: error.javaClass.simpleName
            Toast.makeText(context, getString(R.string.gemini_model_list_failed, reason), Toast.LENGTH_LONG).show()
          },
        )
      }
    }
  }

  private fun showModels(models: List<GeminiModel.Choice>) {
    val chosen = provider.model(prefs)
    val labels =
      listOf(getString(R.string.ai_provider_enter_model)) +
        models.map { if (it.name == it.id) it.id else "${it.name}, ${it.id}" }
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.title_pref_gemini_model_of, getString(provider.label)))
      .setSingleChoiceItems(labels.toTypedArray(), models.indexOfFirst { it.id == chosen } + 1) { dialog, which ->
        dialog.dismiss()
        if (which == 0) {
          askModel()
        } else {
          provider.setModel(prefs, models[which - 1].id)
          build()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun askModel() {
    val model =
      field(
        R.string.ai_service_model_hint,
        provider.model(prefs),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
      )
    AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.title_pref_gemini_model_of, getString(provider.label)))
      .setView(padded(model))
      .setPositiveButton(R.string.gemini_api_key_save) { _, _ ->
        provider.setModel(prefs, model.text.toString())
        AiProvider.afterSetUpChanged(prefs, provider)
        build()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  companion object {
    const val ARG_PROVIDER = "provider"
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
  }
}
