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

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Vibrator
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.quicksettings.SoundMode
import com.google.android.libraries.accessibility.utils.log.LogUtils

/** Moves the ringer between sound, vibrate and mute for the Sound mode reading control. */
internal object SoundModeControl {
  private const val TAG = "SoundModeControl"

  /**
   * Sets the next or previous sound mode, wrapping around, and returns its name to speak. Vibrate
   * is left out without a vibrator. Turning Mute on or off needs Do Not Disturb access; without
   * it, the setting for it opens.
   */
  @JvmStatic
  fun change(context: Context, next: Boolean): String {
    val audio = context.getSystemService(AudioManager::class.java)
    val modes =
      SoundMode.entries.filter {
        it != SoundMode.VIBRATE ||
          context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
      }
    val index = modes.indexOfFirst { it.ringerMode == audio.ringerMode }
    val mode = modes[Math.floorMod(index + if (next) 1 else -1, modes.size)]
    val togglesMute =
      mode == SoundMode.MUTE || audio.ringerMode == AudioManager.RINGER_MODE_SILENT
    val hasAccess =
      context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    if (togglesMute && !hasAccess) {
      return needsAccess(context)
    }
    try {
      audio.ringerMode = mode.ringerMode
    } catch (e: SecurityException) {
      LogUtils.w(TAG, "Cannot change the sound mode: %s", e)
      return needsAccess(context)
    }
    return context.getString(mode.title)
  }

  private fun needsAccess(context: Context): String {
    try {
      context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "No Do Not Disturb access settings: %s", e)
    }
    return context.getString(R.string.sound_mode_needs_access)
  }
}
