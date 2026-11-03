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

package com.google.android.accessibility.talkback.distribution

import android.content.Context
import androidx.preference.PreferenceFragmentCompat

/**
 * What differs between the full build, published on GitHub, and the Google Play build. This is the
 * Google Play build's: Google Play updates it, and the Updates screen is hidden.
 */
object Distribution {
  /** Whether this is the Google Play build. */
  const val IS_PLAY = true

  /** Whether an update is downloading or being installed. */
  @JvmStatic val isUpdating: Boolean = false

  @JvmStatic fun onServiceConnected(context: Context) {}

  @JvmStatic fun onServiceDestroyed() {}

  /** Called when Shizuku allows the add-on, after the service starts. */
  fun onShizukuAllowed(context: Context) {}

  /** Sets up the settings of the Updates screen that [fragment] shows, if it does. */
  @JvmStatic fun setUpPreferences(fragment: PreferenceFragmentCompat) {}

  /** Shows the state of the settings of the Updates screen again, when [fragment] resumes. */
  @JvmStatic fun onPreferencesResumed(fragment: PreferenceFragmentCompat) {}
}
