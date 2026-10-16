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

package com.google.android.accessibility.talkback.speechcontrol

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Turns speech from other apps on or off, and lists the apps that have asked, each of which can be
 * allowed or not.
 */
class SpeechControlFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_speech_control)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
  }

  override fun onResume() {
    super.onResume()
    // Apps can ask while the screen is closed, so the list is built each time it is shown.
    buildScreen()
  }

  private fun buildScreen() {
    val context = requireContext()
    val screen = preferenceManager.createPreferenceScreen(context)
    screen.addPreference(
      checkBox(
        context,
        title = getString(R.string.title_pref_speech_control),
        summary = getString(R.string.summary_pref_speech_control),
        checked = SpeechControl.isEnabled(context),
      ) { checked ->
        prefs.edit().putBoolean(SpeechControl.PREF_ENABLED, checked).apply()
      }
    )
    screen.addPreference(
      checkBox(
        context,
        title = getString(R.string.title_pref_speech_control_sound),
        summary = null,
        checked = SpeechControl.playsSound(context),
      ) { checked ->
        prefs.edit().putBoolean(SpeechControl.PREF_SOUND, checked).apply()
      }
    )
    val category =
      PreferenceCategory(context).apply {
        setTitle(R.string.speech_control_apps_category)
        isIconSpaceReserved = false
      }
    screen.addPreference(category)
    val apps =
      SpeechControlApps.answered(prefs)
        .map { it to SpeechControlApproval.label(context, it).toString() }
        .sortedBy { it.second.lowercase() }
    if (apps.isEmpty()) {
      category.addPreference(
        Preference(context).apply {
          setSummary(R.string.speech_control_no_apps)
          isSelectable = false
          isIconSpaceReserved = false
        }
      )
    }
    for ((app, label) in apps) {
      category.addPreference(
        checkBox(
          context,
          title = label,
          summary = null,
          checked = SpeechControlApps.state(prefs, app) == SpeechControlApps.State.ALLOWED,
        ) { checked ->
          SpeechControlApps.setAllowed(prefs, app, checked)
        }
      )
    }
    preferenceScreen = screen
  }

  private fun checkBox(
    context: Context,
    title: CharSequence,
    summary: CharSequence?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
  ) =
    CheckBoxPreference(context).apply {
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      this.title = title
      summary?.let { this.summary = it }
      isChecked = checked
      setOnPreferenceChangeListener { _, newValue ->
        onChange(newValue as Boolean)
        true
      }
    }
}
