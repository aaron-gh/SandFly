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
      val row =
        SoundRow(
          context,
          item,
          preview = { soundPreview.play(context, prefs, item) },
          setNone = {
            SoundThemes.setNone(context, prefs, item)
            refresh()
          },
          resetDefault = {
            SoundThemes.removeSound(context, prefs, item)
            refresh()
          },
          chooseFile = { launchChooser(item) },
          canPreview = {
            val active = SoundThemes.active(context, prefs)
            !SoundThemes.isNone(active, item) &&
              (SoundThemes.soundFiles(active)[item.key] != null ||
                (!isControlSound(item) && item.key != SoundThemes.SCREEN_ON))
          },
          isNone = {
            val active = SoundThemes.active(context, prefs)
            SoundThemes.isNone(active, item)
          },
          canReset = {
            val active = SoundThemes.active(context, prefs)
            val hasCustom = SoundThemes.soundFiles(active)[item.key] != null
            hasCustom || (SoundThemes.isNone(active, item) && item.key != SoundThemes.SCREEN_ON)
          },
        )
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
    val active = SoundThemes.active(context, prefs)
    val custom = SoundThemes.soundFiles(active)
    for (row in rows) {
      row.summary =
        context.getString(
          when {
            SoundThemes.isNone(active, row.item) -> R.string.theme_sound_summary_no_sound
            row.item.key in custom -> R.string.theme_sound_summary_custom
            isControlSound(row.item) -> R.string.theme_sound_summary_none
            row.item.key == SoundThemes.SCREEN_ON -> R.string.theme_sound_summary_no_sound
            isBrailleTypingSound(row.item) -> R.string.theme_sound_summary_android_keyboard
            else -> R.string.theme_sound_summary_default
          }
        )
    }
    val hasCustomModifications = custom.isNotEmpty() || active.manifest.sounds.isNotEmpty()
    resetPreference?.isEnabled = hasCustomModifications
  }

  private fun showChoices(item: FeedbackItem) {
    val context = requireContext()
    val active = SoundThemes.active(context, prefs)
    val isNone = SoundThemes.isNone(active, item)
    val hasCustom = SoundThemes.soundFiles(active)[item.key] != null
    val choices = ArrayList<Pair<Int, () -> Unit>>()
    choices +=
      R.string.theme_sound_choose to
        {
          launchChooser(item)
        }
    if (!isNone && (hasCustom || (!isControlSound(item) && item.key != SoundThemes.SCREEN_ON))) {
      choices += R.string.theme_sound_preview to { soundPreview.play(context, prefs, item) }
    }
    if (!isNone) {
      choices +=
        R.string.theme_sound_none to
          {
            SoundThemes.setNone(context, prefs, item)
            refresh()
          }
    }
    if (hasCustom || (isNone && item.key != SoundThemes.SCREEN_ON)) {
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

  private fun launchChooser(item: FeedbackItem) {
    choosingFor = item.key
    try {
      chooseSound.launch(SOUND_TYPES)
    } catch (e: ActivityNotFoundException) {
      // Watches have no document picker.
      choosingFor = null
      showMessage(getString(R.string.sound_theme_no_picker))
    }
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

  /** A row for one sound, with accessibility actions. */
  private class SoundRow(
    context: Context,
    val item: FeedbackItem,
    private val preview: () -> Unit,
    private val setNone: () -> Unit,
    private val resetDefault: () -> Unit,
    private val chooseFile: () -> Unit,
    private val canPreview: () -> Boolean,
    private val isNone: () -> Boolean,
    private val canReset: () -> Boolean,
  ) : AccessibilitySuitePreference(context) {
    init {
      key = "pref_theme_sound_${item.key}"
      isPersistent = false
      isIconSpaceReserved = false
      setTitle(item.title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other sounds, so every bind replaces the action.
      if (canPreview()) {
        val previewLabel = context.getString(R.string.theme_sound_preview)
        ViewCompat.replaceAccessibilityAction(
          holder.itemView,
          AccessibilityActionCompat(ACTION_PREVIEW, previewLabel),
          previewLabel,
          AccessibilityViewCommand { _, _ ->
            preview()
            true
          },
        )
      } else {
        ViewCompat.removeAccessibilityAction(holder.itemView, ACTION_PREVIEW)
      }

      if (!isNone()) {
        val noneLabel = context.getString(R.string.theme_sound_action_set_none)
        ViewCompat.replaceAccessibilityAction(
          holder.itemView,
          AccessibilityActionCompat(ACTION_NONE, noneLabel),
          noneLabel,
          AccessibilityViewCommand { _, _ ->
            setNone()
            true
          },
        )
      } else {
        ViewCompat.removeAccessibilityAction(holder.itemView, ACTION_NONE)
      }

      if (canReset()) {
        val resetLabel =
          context.getString(
            when {
              isControlSound(item) -> R.string.theme_sound_remove
              isBrailleTypingSound(item) -> R.string.theme_sound_use_android_keyboard
              else -> R.string.theme_sound_use_default
            }
          )
        ViewCompat.replaceAccessibilityAction(
          holder.itemView,
          AccessibilityActionCompat(ACTION_RESET, resetLabel),
          resetLabel,
          AccessibilityViewCommand { _, _ ->
            resetDefault()
            true
          },
        )
      } else {
        ViewCompat.removeAccessibilityAction(holder.itemView, ACTION_RESET)
      }

      val chooseLabel = context.getString(R.string.theme_sound_choose)
      ViewCompat.replaceAccessibilityAction(
        holder.itemView,
        AccessibilityActionCompat(ACTION_CHOOSE, chooseLabel),
        chooseLabel,
        AccessibilityViewCommand { _, _ ->
          chooseFile()
          true
        },
      )
    }
  }

  private companion object {
    val ACTION_PREVIEW = R.id.accessibility_custom_action_0
    val ACTION_NONE = R.id.accessibility_custom_action_1
    val ACTION_RESET = R.id.accessibility_custom_action_2
    val ACTION_CHOOSE = R.id.accessibility_custom_action_3
    const val STATE_CHOOSING_FOR = "choosing_for"
    const val DEFAULT_EXTENSION = "wav"
    val SOUND_TYPES = arrayOf("audio/*", "application/ogg")

    fun isControlSound(item: FeedbackItem): Boolean = item.key in ControlSounds.SOUNDS

    fun isBrailleTypingSound(item: FeedbackItem): Boolean =
      item.key in SoundThemes.BRAILLE_TYPING_EFFECTS
  }
}
