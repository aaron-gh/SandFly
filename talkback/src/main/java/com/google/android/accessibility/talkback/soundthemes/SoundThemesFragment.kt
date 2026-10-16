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
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The installed sound themes, with the one in use, the settings of the theme in use, and ways to
 * install more. Themes are installed by [SoundThemeInstallActivity], which also opens theme files
 * shared with Backtalk from other apps.
 */
class SoundThemesFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()

  // The theme being saved as a file, kept in case the screen is recreated meanwhile.
  private var exporting: String? = null

  private val installFile: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) startActivity(SoundThemeInstallActivity.fileIntent(requireContext(), uri))
    }

  private val exportFile: ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_ZIP)) { uri ->
      val id = exporting
      exporting = null
      if (uri != null && id != null) export(id, uri)
    }

  // Saves the control sounds and 3D audio settings to the theme in use as soon as they change.
  private val settingsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
      if (key == ControlSoundsSettings.PREF_ON || key == ControlSoundsSettings.PREF_3D) {
        SoundThemes.saveSettings(requireContext(), prefs)
      }
    }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_sound_themes)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    exporting = savedInstanceState?.getString(STATE_EXPORTING)
    // The control sounds and 3D audio switches save to Backtalk's settings, which are in device
    // protected storage. Without this they would save to a second settings file in credential
    // protected storage, which the service moves over the real one when it starts, losing every
    // other setting.
    preferenceManager.setStorageDeviceProtected()
    preferenceScreen = preferenceManager.createPreferenceScreen(context)
  }

  override fun onResume() {
    super.onResume()
    // A theme may have been installed from another app meanwhile.
    rebuild()
    prefs.registerOnSharedPreferenceChangeListener(settingsListener)
  }

  override fun onPause() {
    super.onPause()
    prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_EXPORTING, exporting)
  }

  override fun onDestroy() {
    super.onDestroy()
    executor.shutdown()
  }

  private fun rebuild() {
    val context = requireContext()
    val screen = preferenceScreen
    screen.removeAll()
    val active = SoundThemes.active(context, prefs)

    val installed = category(context, getString(R.string.sound_themes_installed_category))
    screen.addPreference(installed)
    for (theme in SoundThemes.installed(context)) {
      installed.addPreference(
        action(context, nameOf(context, theme), summaryOf(context, theme, theme.id == active.id)) {
          showChoices(theme)
        }
      )
    }

    addSettings(context, screen, active)

    val install = category(context, getString(R.string.sound_themes_install_category))
    screen.addPreference(install)
    install.addPreference(
      action(
        context,
        getString(R.string.title_pref_sound_theme_install_file),
        getString(R.string.summary_pref_sound_theme_install_file),
      ) {
        launch { installFile.launch(PACK_TYPES) }
      }
    )
    install.addPreference(
      action(
        context,
        getString(R.string.title_pref_sound_theme_install_link),
        getString(R.string.summary_pref_sound_theme_install_link),
      ) {
        askForLink()
      }
    )
  }

  /** The settings each theme has its own of, for the theme in use. */
  private fun addSettings(context: Context, screen: PreferenceScreen, active: SoundTheme) {
    val settings =
      category(context, getString(R.string.sound_theme_settings_category, nameOf(context, active)))
    screen.addPreference(settings)
    settings.addPreference(
      AccessibilitySuiteSwitchPreference(context).apply {
        key = ControlSoundsSettings.PREF_ON
        setDefaultValue(false)
        setTitle(R.string.title_pref_control_sounds)
        setSummary(R.string.summary_pref_control_sounds)
        isIconSpaceReserved = false
      }
    )
    settings.addPreference(
      AccessibilitySuiteListPreference(context).apply {
        key = ControlSoundsSettings.PREF_3D
        setDefaultValue(ControlSoundsSettings.VALUE_3D_WITH_HEADPHONES)
        setTitle(R.string.title_pref_control_sounds_3d)
        setDialogTitle(R.string.title_pref_control_sounds_3d)
        setEntries(R.array.pref_control_sounds_3d_entries)
        setEntryValues(R.array.pref_control_sounds_3d_values)
        summary = "%s"
        isIconSpaceReserved = false
      }
    )
    // Placed sounds only play with control sounds on.
    settings.findPreference<Preference>(ControlSoundsSettings.PREF_3D)?.dependency =
      ControlSoundsSettings.PREF_ON
    settings.addPreference(
      AccessibilitySuitePreference(context).apply {
        fragment = ThemeSoundsFragment::class.java.name
        key = "pref_sound_theme_sounds"
        setTitle(R.string.title_pref_theme_sounds)
        setSummary(R.string.summary_pref_theme_sounds)
        isPersistent = false
        isIconSpaceReserved = false
      }
    )
  }

  private fun showChoices(theme: SoundTheme) {
    val context = requireContext()
    val inUse = theme.id == SoundThemes.activeId(prefs)
    val choices = ArrayList<Pair<Int, () -> Unit>>()
    if (!inUse) {
      choices +=
        R.string.sound_theme_use to
          {
            SoundThemes.activate(context, prefs, theme.id)
            rebuild()
          }
    }
    choices += R.string.sound_theme_about to { showAbout(theme) }
    choices +=
      R.string.sound_theme_export to
        {
          exporting = theme.id
          launch { exportFile.launch("${nameOf(context, theme)}.zip") }
        }
    if (!theme.isBuiltIn) {
      choices += R.string.sound_theme_remove to { confirmRemove(theme) }
    }
    AlertDialog.Builder(context)
      .setTitle(nameOf(context, theme))
      .setItems(choices.map { getString(it.first) }.toTypedArray()) { _, which ->
        choices[which].second()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showAbout(theme: SoundTheme) {
    showMessage(describe(requireContext(), theme.manifest, SoundThemes.soundFiles(theme).size))
  }

  private fun confirmRemove(theme: SoundTheme) {
    val context = requireContext()
    AlertDialog.Builder(context)
      .setMessage(getString(R.string.sound_theme_remove_confirm, nameOf(context, theme)))
      .setPositiveButton(R.string.sound_theme_remove_button) { _, _ ->
        SoundThemes.delete(context, prefs, theme.id)
        rebuild()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun askForLink() {
    val context = requireContext()
    val field =
      EditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        setHint(R.string.sound_theme_link_hint)
        isSingleLine = true
      }
    // Lines the field up with the dialog's message.
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    val container =
      FrameLayout(context).apply {
        setPadding(padding, 0, padding, 0)
        addView(field)
      }
    AlertDialog.Builder(context)
      .setTitle(R.string.title_pref_sound_theme_install_link)
      .setMessage(R.string.sound_theme_link_message)
      .setView(container)
      .setPositiveButton(R.string.sound_theme_install) { _, _ ->
        val link = field.text.toString().trim()
        if (link.isNotEmpty()) {
          startActivity(SoundThemeInstallActivity.linkIntent(context, link))
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
    field.requestFocus()
  }

  private fun export(id: String, uri: Uri) {
    val context = requireContext().applicationContext
    val activity = requireActivity()
    executor.execute {
      val count =
        try {
          context.contentResolver.openOutputStream(uri)?.use {
            SoundThemes.export(context, prefs, id, it)
          }
        } catch (e: IOException) {
          null
        } catch (e: SecurityException) {
          null
        }
      activity.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        showMessage(
          if (count == null) getString(R.string.sound_theme_export_failed)
          else resources.getQuantityString(R.plurals.sound_theme_exported, count, count)
        )
      }
    }
  }

  private fun launch(start: () -> Unit) {
    try {
      start()
    } catch (e: ActivityNotFoundException) {
      // Watches have no document picker.
      exporting = null
      showMessage(getString(R.string.sound_theme_no_picker))
    }
  }

  private fun showMessage(message: CharSequence) {
    AlertDialog.Builder(requireContext())
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun category(context: Context, title: CharSequence) =
    PreferenceCategory(context).apply {
      this.title = title
      isIconSpaceReserved = false
    }

  private fun action(
    context: Context,
    title: CharSequence,
    summary: CharSequence?,
    onClick: () -> Unit,
  ): Preference =
    AccessibilitySuitePreference(context).apply {
      this.title = title
      this.summary = summary
      isPersistent = false
      isIconSpaceReserved = false
      setOnPreferenceClickListener {
        onClick()
        true
      }
    }

  companion object {
    private const val STATE_EXPORTING = "exporting"
    private const val MIME_ZIP = "application/zip"

    // File managers do not all call ZIP files by the same type.
    @JvmField
    val PACK_TYPES = arrayOf(MIME_ZIP, "application/x-zip-compressed", "application/octet-stream")

    /** The name to show for [theme]: Backtalk's own is translated. */
    @JvmStatic
    fun nameOf(context: Context, theme: SoundTheme): String =
      if (theme.isBuiltIn) context.getString(R.string.sound_theme_backtalk)
      else theme.manifest.name

    private fun summaryOf(context: Context, theme: SoundTheme, inUse: Boolean): String {
      val parts = ArrayList<String>()
      if (inUse) parts += context.getString(R.string.sound_theme_in_use)
      if (theme.isBuiltIn) {
        parts += context.getString(R.string.sound_theme_backtalk_description)
      } else {
        theme.manifest.author?.let { parts += context.getString(R.string.sound_theme_by, it) }
        theme.manifest.description?.let { parts += it }
      }
      return parts.joinToString("\n")
    }

    /** Describes a theme for the person choosing or installing it. */
    @JvmStatic
    fun describe(context: Context, manifest: SoundThemeManifest, soundCount: Int): String {
      val lines = ArrayList<String>()
      manifest.author?.let { lines += context.getString(R.string.sound_theme_by, it) }
      manifest.description?.let { lines += it }
      val vibrations = manifest.vibrations.size
      lines +=
        context.resources.getQuantityString(R.plurals.sound_theme_sounds, soundCount, soundCount) +
          " " +
          context.resources.getQuantityString(R.plurals.sound_theme_vibrations, vibrations, vibrations)
      manifest.controlSounds?.let {
        lines +=
          context.getString(
            if (it) R.string.sound_theme_control_sounds_on else R.string.sound_theme_control_sounds_off
          )
      }
      manifest.license?.let { lines += context.getString(R.string.sound_theme_license, it) }
      manifest.website?.let { lines += it }
      return lines.joinToString("\n\n")
    }
  }
}
