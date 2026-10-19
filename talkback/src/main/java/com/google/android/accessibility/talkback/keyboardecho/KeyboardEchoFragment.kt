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

package com.google.android.accessibility.talkback.keyboardecho

import android.content.SharedPreferences
import androidx.preference.ListPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Keyboard echo for each way of typing: the on-screen and physical keyboards, the braille keyboard
 * and braille displays.
 */
class KeyboardEchoFragment : TalkbackBaseFragment(R.xml.keyboard_echo_preferences) {
  private lateinit var prefs: SharedPreferences

  // The typing echo reading control changes the physical keyboard's echo while this screen is open.
  private val listener =
    SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
      val preference =
        key?.let { findPreference<ListPreference>(it) }
          ?: return@OnSharedPreferenceChangeListener
      val value = sharedPrefs.getString(key, null)
      if (value != null && value != preference.value) {
        preference.value = value
      }
    }

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_keyboard_echo)

  override fun onResume() {
    super.onResume()
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  override fun onPause() {
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
    super.onPause()
  }
}
