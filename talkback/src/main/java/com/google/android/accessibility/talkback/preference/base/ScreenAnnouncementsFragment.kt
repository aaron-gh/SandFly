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

package com.google.android.accessibility.talkback.preference.base

import android.os.Bundle
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.monitor.ScreenAnnouncementSettings
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** What Backtalk says when the screen turns off or on and when the phone is unlocked. */
class ScreenAnnouncementsFragment : TalkbackBaseFragment(R.xml.screen_announcement_preferences) {
  override fun getTitle(): CharSequence = getText(R.string.title_pref_screen_announcements)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    // Before the battery list is created, which would save its default over the old switch's
    // value while the service hasn't migrated it, as after importing old settings.
    ScreenAnnouncementSettings.migrateBattery(
      SharedPreferencesUtils.getSharedPreferences(requireContext())
    )
    super.onCreatePreferences(savedInstanceState, rootKey)
  }
}
