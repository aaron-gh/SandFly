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

package com.google.android.accessibility.talkback.soundthemes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.controlsounds.ControlSounds
import com.google.android.accessibility.talkback.individualfeedback.FeedbackItem
import com.google.android.accessibility.talkback.individualfeedback.SoundPreview
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A row for each of Backtalk's sounds in the theme in use, where the user can choose a sound file
 * to play in its place. Files are copied in on a background thread.
 */
class ThemeSoundsFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private val soundPreview = SoundPreview()
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val rows = ArrayList<SoundRow>()
  private var resetPreference: Preference? = null

  // The item a sound file is being chosen for, kept in case the screen is recreated meanwhile.
  private var choosingFor: String? = null

  private val chooseSound: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      val item = SoundThemes.SOUNDS.firstOrNull { it.key == choosingFor }
      choosingFor = null
      if (uri != null && item != null) setSound(item, uri)
    }

  public override fun getTitle(): CharSequence {
    val context = requireContext()
    val theme = SoundThemes.active(context, SharedPreferencesUtils.getSharedPreferences(context))
    return getString(R.string.title_pref_theme_sounds_of, SoundThemesFragment.nameOf(context, theme))
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    choosingFor = savedInstanceState?.getString(STATE_CHOOSING_FOR)
    // Like every settings screen, so that nothing is ever saved outside Backtalk's settings.
    preferenceManager.setStorageDeviceProtected()
    val screen = preferenceManager.createPreferenceScreen(context)
    preferenceScreen = screen

    val reset =
      AccessibilitySuitePreference(context).apply {
        setTitle(R.string.title_pref_theme_sounds_reset)
        isPersistent = false
        isIconSpaceReserved = false
        setOnPreferenceClickListener {
          confirmReset()
          true
        }
      }
    screen.addPreference(reset)
    resetPreference = reset

    val sounds =
      PreferenceCategory(context).apply {
        setTitle(R.string.theme_sounds_category)
        isIconSpaceReserved = false
      }
    screen.addPreference(sounds)
    rows.clear()
    for (item in SoundThemes.SOUNDS) {
      val row = SoundRow(context, item, preview = { soundPreview.play(context, prefs, item) })
      row.setOnPreferenceClickListener {
        showChoices(item)
        true
      }
      sounds.addPreference(row)
      rows += row
    }
    refresh()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_CHOOSING_FOR, choosingFor)
  }

  override fun onPause() {
    super.onPause()
    soundPreview.stop()
  }

  override fun onDestroy() {
    super.onDestroy()
    executor.shutdown()
  }

  /** Shows which sounds the theme replaces, and offers resetting only when it replaces some. */
  private fun refresh() {
    val context = context ?: return
    val custom = SoundThemes.soundFiles(SoundThemes.active(context, prefs))
    for (row in rows) {
      row.summary =
        context.getString(
          when {
            row.item.key in custom -> R.string.theme_sound_summary_custom
            isControlSound(row.item) -> R.string.theme_sound_summary_none
            isBrailleTypingSound(row.item) -> R.string.theme_sound_summary_android_keyboard
            else -> R.string.theme_sound_summary_default
          }
        )
    }
    resetPreference?.isEnabled = custom.isNotEmpty()
  }

  private fun showChoices(item: FeedbackItem) {
    val context = requireContext()
    val hasCustom = SoundThemes.soundFile(context, prefs, item) != null
    val choices = ArrayList<Pair<Int, () -> Unit>>()
    choices +=
      R.string.theme_sound_choose to
        {
          choosingFor = item.key
          try {
            chooseSound.launch(SOUND_TYPES)
          } catch (e: ActivityNotFoundException) {
            // Watches have no document picker.
            choosingFor = null
            showMessage(getString(R.string.sound_theme_no_picker))
          }
        }
    if (hasCustom || !isControlSound(item)) {
      choices += R.string.theme_sound_preview to { soundPreview.play(context, prefs, item) }
    }
    if (hasCustom) {
      val label =
        when {
          isControlSound(item) -> R.string.theme_sound_remove
          isBrailleTypingSound(item) -> R.string.theme_sound_use_android_keyboard
          else -> R.string.theme_sound_use_default
        }
      choices +=
        label to
          {
            SoundThemes.removeSound(context, prefs, item)
            refresh()
          }
    }
    AlertDialog.Builder(context)
      .setTitle(getString(R.string.theme_sound_dialog_title, getString(item.title), item.key))
      .setItems(choices.map { getString(it.first) }.toTypedArray()) { _, which ->
        choices[which].second()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun confirmReset() {
    val context = requireContext()
    AlertDialog.Builder(context)
      .setMessage(R.string.theme_sounds_reset_confirm)
      .setPositiveButton(R.string.theme_sounds_reset_button) { _, _ ->
        SoundThemes.removeAllSounds(context, prefs)
        refresh()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun setSound(item: FeedbackItem, uri: Uri) {
    val context = requireContext().applicationContext
    val extension = extensionOf(context, uri)
    val activity = requireActivity()
    executor.execute {
      val result =
        try {
          context.contentResolver.openInputStream(uri)?.use {
            SoundThemes.setSound(context, prefs, item, it, extension)
          } ?: SoundThemes.Result.FAILED
        } catch (e: IOException) {
          SoundThemes.Result.FAILED
        } catch (e: SecurityException) {
          SoundThemes.Result.FAILED
        }
      activity.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        refresh()
        when (result) {
          SoundThemes.Result.OK -> soundPreview.play(context, prefs, item)
          SoundThemes.Result.TOO_LARGE -> showMessage(getString(R.string.theme_sound_too_large))
          SoundThemes.Result.NOT_AUDIO -> showMessage(getString(R.string.theme_sound_not_audio))
          SoundThemes.Result.FAILED -> showMessage(getString(R.string.theme_sound_failed))
        }
      }
    }
  }

  private fun showMessage(message: CharSequence) {
    AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  /** Returns the extension of a chosen file, from its name, or else from its type. */
  private fun extensionOf(context: Context, uri: Uri): String {
    val name =
      try {
        context.contentResolver
          .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
          ?.use { if (it.moveToFirst()) it.getString(0) else null }
      } catch (e: RuntimeException) {
        null
      }
    SoundThemes.extensionOf(name)?.let {
      return it
    }
    val type = context.contentResolver.getType(uri)
    val fromType = type?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    return SoundThemes.extensionOf("sound.$fromType") ?: DEFAULT_EXTENSION
  }

  /** A row for one sound, with a Preview action that plays it. */
  private class SoundRow(context: Context, val item: FeedbackItem, private val preview: () -> Unit) :
    AccessibilitySuitePreference(context) {
    init {
      key = "pref_theme_sound_${item.key}"
      isPersistent = false
      isIconSpaceReserved = false
      setTitle(item.title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other sounds, so every bind replaces the action.
      val label = context.getString(R.string.theme_sound_preview)
      ViewCompat.replaceAccessibilityAction(
        holder.itemView,
        AccessibilityActionCompat(ACTION_PREVIEW, label),
        label,
        AccessibilityViewCommand { _, _ ->
          preview()
          true
        },
      )
    }
  }

  private companion object {
    val ACTION_PREVIEW = R.id.accessibility_custom_action_0
    const val STATE_CHOOSING_FOR = "choosing_for"
    const val DEFAULT_EXTENSION = "wav"
    val SOUND_TYPES = arrayOf("audio/*", "application/ogg")

    fun isControlSound(item: FeedbackItem): Boolean = item.key in ControlSounds.SOUNDS

    fun isBrailleTypingSound(item: FeedbackItem): Boolean =
      item.key in SoundThemes.BRAILLE_TYPING_EFFECTS
  }
}
