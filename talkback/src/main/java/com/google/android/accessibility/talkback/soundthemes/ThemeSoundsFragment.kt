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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.controlsounds.ControlSounds
import com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings
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
  private var draftId: String? = null
  private var closing = false
  private var discardDialog: AlertDialog? = null
  private lateinit var prefs: SharedPreferences
  private val soundPreview = SoundPreview()
  internal val executor: ExecutorService = Executors.newSingleThreadExecutor()
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

  private val exportTheme = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
    if (uri != null) {
      val context = requireContext().applicationContext
      val activity = requireActivity()
      val id = SoundThemes.activeId(prefs)
      if (!closing && available()) executor.execute {
        val message = SoundThemeExport.message(context, prefs, id, uri)
        activity.runOnUiThread { if (isAdded && !closing) showMessage(message) }
      }
    }
  }

  public override fun getTitle(): CharSequence {
    val context = requireContext()
    val id = arguments?.getString(ARG_DRAFT)
    val theme = if (id != null) SoundThemes.theme(context, id) else
      SoundThemes.active(context, SharedPreferencesUtils.getSharedPreferences(context))
    // A draft that is gone closes the editor on resume.
    if (theme == null) return getString(R.string.theme_create)
    return getString(R.string.title_pref_theme_sounds_of, SoundThemesFragment.nameOf(context, theme))
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    draftId = arguments?.getString(ARG_DRAFT)
    prefs = draftId?.let { id ->
      context.createDeviceProtectedStorageContext().getSharedPreferences(SoundThemes.draftPreferencesName(id), Context.MODE_PRIVATE).apply {
        edit().putString(SoundThemes.PREF_ACTIVE, id).apply()
      }
    } ?: SharedPreferencesUtils.getSharedPreferences(context)
    choosingFor = savedInstanceState?.getString(STATE_CHOOSING_FOR)
    // Like every settings screen, so that nothing is ever saved outside Backtalk's settings.
    preferenceManager.setStorageDeviceProtected()
    draftId?.let { preferenceManager.sharedPreferencesName = SoundThemes.draftPreferencesName(it) }
    val screen = preferenceManager.createPreferenceScreen(context)
    preferenceScreen = screen

    if (draftId != null) {
      fun action(title: Int, click: () -> Unit) {
        screen.addPreference(AccessibilitySuitePreference(context).apply {
          setTitle(title)
          isPersistent = false
          isIconSpaceReserved = false
          setOnPreferenceClickListener { click(); true }
        })
      }
      requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { confirmDiscard() }
      })
      action(R.string.theme_create_save) { saveDraft() }
      action(R.string.sound_theme_export) {
        try {
          val theme = resolveTheme() ?: return@action
          if (!closing) exportTheme.launch("${theme.manifest.name}.zip")
        } catch (e: ActivityNotFoundException) {
          showMessage(getString(R.string.sound_theme_no_picker))
        }
      }
    }

    if (draftId != null) {
      screen.addPreference(AccessibilitySuiteSwitchPreference(context).apply {
        key = ControlSoundsSettings.PREF_ON
        isPersistent = false
        isChecked = prefs.getBoolean(key, false)
        setOnPreferenceChangeListener { _, value ->
          if (closing || !available()) return@setOnPreferenceChangeListener false
          prefs.edit().putBoolean(key, value as Boolean).apply()
          true
        }
        setTitle(R.string.title_pref_control_sounds)
        setSummary(R.string.summary_pref_control_sounds)
        isIconSpaceReserved = false
      })
      screen.addPreference(AccessibilitySuiteListPreference(context).apply {
        key = ControlSoundsSettings.PREF_3D
        isPersistent = false
        value = prefs.getString(key, ControlSoundsSettings.VALUE_3D_WITH_HEADPHONES)
        setOnPreferenceChangeListener { _, value ->
          if (closing || !available()) return@setOnPreferenceChangeListener false
          prefs.edit().putString(key, value as String).apply()
          true
        }
        setTitle(R.string.title_pref_control_sounds_3d)
        setDialogTitle(R.string.title_pref_control_sounds_3d)
        setEntries(R.array.pref_control_sounds_3d_entries)
        setEntryValues(R.array.pref_control_sounds_3d_values)
        summary = "%s"
        isIconSpaceReserved = false
      })
      // Placed sounds only play with control sounds on.
      findPreference<Preference>(ControlSoundsSettings.PREF_3D)?.dependency =
        ControlSoundsSettings.PREF_ON
    }

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
      val row = SoundRow(context, item, preview = { preview(item) })
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
    discardDialog?.dismiss()
    executor.shutdown()
  }

  override fun onResume() {
    super.onResume()
    if (::prefs.isInitialized) available()
  }

  /**
   * Whether the draft being edited, if any, is still there, and marks it as in use so that it is
   * kept. If it is gone, closes the editor.
   */
  private fun available(): Boolean {
    val context = context ?: return false
    val id = draftId ?: return true
    if (SoundThemes.touchDraft(context, id)) return true
    if (!closing) {
      closing = true
      Toast.makeText(context, R.string.theme_create_draft_missing, Toast.LENGTH_LONG).show()
      requireActivity().finish()
    }
    return false
  }

  /** The theme being edited, or null if it is a draft that is gone. */
  private fun resolveTheme(): SoundTheme? {
    if (!available()) return null
    val context = requireContext()
    val id = draftId ?: return SoundThemes.active(context, prefs)
    return SoundThemes.theme(context, id)
  }

  private fun preview(item: FeedbackItem) {
    if (!closing && available()) soundPreview.play(requireContext(), prefs, item)
  }

  private fun saveDraft() {
    if (closing || !available()) return
    closing = true
    val activity = requireActivity()
    val app = requireContext().applicationContext
    val id = draftId!!
    executor.execute {
      var published = false
      try {
        SoundThemes.saveSettings(app, prefs)
        val saved = SoundThemes.saveDraft(app, id)
        published = true
        SoundThemes.activate(app, SharedPreferencesUtils.getSharedPreferences(app), saved.id)
      } catch (error: IOException) {
        if (!published) activity.runOnUiThread {
          closing = false
          if (isAdded && available()) {
            if (error is SoundThemes.DuplicateThemeException) askForNewName()
            else showMessage(getString(R.string.theme_create_save_failed))
          }
        }
      } catch (error: RuntimeException) {
        if (!published) activity.runOnUiThread {
          closing = false
          if (isAdded) showMessage(getString(R.string.theme_create_save_failed))
        }
      } finally {
        // The draft no longer exists after publishing, even if activation failed.
        if (published) activity.runOnUiThread { activity.finish() }
      }
    }
  }

  private fun askForNewName() {
    val context = requireContext()
    val form = ThemeForm(context)
    val input = form.field(R.string.theme_create_name, SoundThemeManifest.MAX_NAME_LENGTH).apply {
      setText(resolveTheme()?.manifest?.name)
    }
    val dialog = AlertDialog.Builder(context)
      .setTitle(R.string.theme_create_duplicate).setView(form.view)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(android.R.string.ok, null).create()
    var renaming = false
    dialog.setOnShowListener {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        if (renaming || closing || !available()) return@setOnClickListener
        val name = input.text.toString().trim()
        if (name.isEmpty()) {
          input.error = getString(R.string.theme_create_name_required)
          input.requestFocus()
          return@setOnClickListener
        }
        val app = context.applicationContext
        val activity = requireActivity()
        val id = draftId!!
        renaming = true
        executor.execute {
          val renamed =
            try {
              SoundThemes.renameDraft(app, id, name)
              true
            } catch (error: IOException) {
              false
            }
          activity.runOnUiThread {
            if (!isAdded || closing) return@runOnUiThread
            renaming = false
            if (renamed) {
              dialog.dismiss()
              saveDraft()
            } else if (available()) {
              showMessage(getString(R.string.theme_create_save_failed))
            }
          }
        }
      }
    }
    dialog.show()
    input.requestFocus()
  }

  private fun confirmDiscard() {
    if (closing || discardDialog?.isShowing == true) return
    discardDialog = AlertDialog.Builder(requireContext())
      .setTitle(R.string.theme_create_discard_title)
      .setPositiveButton(R.string.theme_create_discard) { _, _ ->
        closing = true
        val app = requireContext().applicationContext
        val activity = requireActivity()
        val id = draftId!!
        // Finish copying any chosen sounds before deleting the draft.
        executor.execute {
          SoundThemes.discardDraft(app, id)
          activity.runOnUiThread { activity.finish() }
        }
      }
      .setNegativeButton(R.string.theme_create_keep_editing, null)
      .show()
  }

  /** Shows which sounds the theme replaces, and offers resetting only when it replaces some. */
  private fun refresh() {
    val context = context ?: return
    val theme = resolveTheme() ?: return
    val custom = SoundThemes.soundFiles(theme)
    if (draftId != null && !prefs.contains(ControlSoundsSettings.PREF_ON)) {
      findPreference<AccessibilitySuiteSwitchPreference>(ControlSoundsSettings.PREF_ON)?.isChecked =
        custom.keys.any { it in ControlSounds.SOUNDS }
    }
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
    if (closing || !available()) return
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
      choices += R.string.theme_sound_preview to { preview(item) }
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
            if (!closing && available()) {
              SoundThemes.removeSound(context, prefs, item)
              refresh()
            }
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
        if (!closing && available()) {
          SoundThemes.removeAllSounds(context, prefs)
          refresh()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun setSound(item: FeedbackItem, uri: Uri) {
    if (closing || !available()) return
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
        if (!isAdded || closing) return@runOnUiThread
        refresh()
        when (result) {
          SoundThemes.Result.OK -> preview(item)
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

  companion object {
    const val ARG_DRAFT = "theme_draft"
    val ACTION_PREVIEW = R.id.accessibility_custom_action_0
    const val STATE_CHOOSING_FOR = "choosing_for"
    const val DEFAULT_EXTENSION = "wav"
    val SOUND_TYPES = arrayOf("audio/*", "application/ogg")

    fun isControlSound(item: FeedbackItem): Boolean = item.key in ControlSounds.SOUNDS

    fun isBrailleTypingSound(item: FeedbackItem): Boolean =
      item.key in SoundThemes.BRAILLE_TYPING_EFFECTS
  }
}
