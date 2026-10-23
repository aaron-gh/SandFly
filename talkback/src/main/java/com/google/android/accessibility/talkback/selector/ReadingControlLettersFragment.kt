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

package com.google.android.accessibility.talkback.selector

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferenceFilter
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Reading controls > Handwriting letters: the letter that "Write the letter of a reading control"
 * takes for each reading control, listed in the order of the Reading controls screen.
 */
class ReadingControlLettersFragment : TalkbackBaseFragment() {
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(requireContext()) }

  override fun getTitle(): CharSequence = getString(R.string.title_pref_reading_control_letters)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    // Built in code, so the device-protected settings are set here, as an XML screen does.
    preferenceManager.setStorageDeviceProtected()
    val context = requireContext()
    // The Reading controls screen has each control's name, keyed by its switch's key. It is shown
    // only until this screen replaces it below.
    addPreferencesFromResource(R.xml.selector_menu_preferences)
    val controls = preferenceScreen
    val screen = preferenceManager.createPreferenceScreen(context)
    // Without the controls that the Reading controls screen hides on this device.
    TalkBackPreferenceFilter(context).filterPreferences(controls)
    val settings = SelectorController.SELECTOR_SETTINGS.associateBy { getString(it.prefKeyResId) }
    for (control in switches(controls)) {
      val setting = settings[control.key] ?: continue
      screen.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = ReadingControlLetters.prefKey(setting)
          isPersistent = false
          isIconSpaceReserved = false
          title = control.title
          summary = letterText(setting)
          setOnPreferenceClickListener {
            choose(setting, this)
            true
          }
        }
      )
    }
    preferenceScreen = screen
  }

  private fun switches(group: PreferenceGroup): List<Preference> =
    (0 until group.preferenceCount).flatMap { index ->
      when (val preference = group.getPreference(index)) {
        is PreferenceGroup -> switches(preference)
        is TwoStatePreference -> listOf(preference)
        else -> emptyList()
      }
    }

  private fun letterText(setting: Setting): String =
    ReadingControlLetters.letterOf(prefs, setting)?.toString()
      ?: getString(R.string.reading_control_letter_none)

  private fun choose(setting: Setting, row: Preference) {
    val letters = listOf<Char?>(null) + ('A'..'Z')
    val names = letters.map { it?.toString() ?: getString(R.string.reading_control_letter_none) }
    AlertDialog.Builder(requireContext())
      .setTitle(row.title)
      .setSingleChoiceItems(
        names.toTypedArray(),
        letters.indexOf(ReadingControlLetters.letterOf(prefs, setting)),
      ) { dialog, which ->
        ReadingControlLetters.setLetter(prefs, setting, letters[which])
        row.summary = letterText(setting)
        dialog.dismiss()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
