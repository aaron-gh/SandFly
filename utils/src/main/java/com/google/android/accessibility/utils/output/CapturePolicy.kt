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

package com.google.android.accessibility.utils.output

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Whether other apps may capture SandFly's speech and sounds, such as screen casting and
 * mirroring, which then play them on the TV or speaker. With Keep speech on the phone while
 * casting, no app may capture them; screen recordings then have no SandFly audio either.
 */
object CapturePolicy {
  private const val TAG = "CapturePolicy"
  const val PREF_KEEP_OFF_CASTS = "pref_keep_speech_off_casts"

  @JvmStatic
  fun keepsOffCasts(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_KEEP_OFF_CASTS, false)

  /** The capture policy for SandFly's audio. */
  @JvmStatic
  fun policy(prefs: SharedPreferences): Int =
    if (keepsOffCasts(prefs)) AudioAttributes.ALLOW_CAPTURE_BY_NONE
    else AudioAttributes.ALLOW_CAPTURE_BY_ALL

  /** Applies the policy to everything SandFly's own process plays. */
  @JvmStatic
  fun applyToProcess(context: Context, prefs: SharedPreferences) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    try {
      context.getSystemService(AudioManager::class.java)?.setAllowedCapturePolicy(policy(prefs))
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot set the capture policy: %s", e)
    }
  }

  /**
   * Applies the policy to [builder], for speech that a speech engine plays in its own process with
   * SandFly's attributes.
   */
  @JvmStatic
  fun applyTo(builder: AudioAttributes.Builder, prefs: SharedPreferences): AudioAttributes.Builder {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      builder.setAllowedCapturePolicy(policy(prefs))
    }
    return builder
  }
}
