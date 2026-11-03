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

package com.google.android.accessibility.talkback.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Clears the output device that Android keeps for accessibility audio.
 *
 * During a call, Android sends accessibility audio to the call's communication device. Some phones
 * keep that device after the call: on a Galaxy Z Fold, Telecom kept the earpiece after a WhatsApp
 * call. Speech then plays on the earpiece, and other audio on the
 * same output stops while it plays. Only the shell user can clear it, as it needs
 * MODIFY_AUDIO_ROUTING: SandFly does it through Shizuku, and the user with adb, which runs
 * [AccessibilityAudioOutput] from SandFly's APK.
 */
object AccessibilityAudioReset {
  const val PREF_ENABLED = "pref_reset_accessibility_audio"
  private const val ENABLED_DEFAULT = false

  fun isEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_ENABLED, ENABLED_DEFAULT)

  /**
   * The adb command that resets the output once, which runs [AccessibilityAudioOutput] from the
   * installed APK. The APK's path changes when SandFly is updated.
   */
  fun command(context: Context): String =
    "adb shell CLASSPATH=${context.applicationInfo.sourceDir} app_process / " +
      AccessibilityAudioOutput::class.java.name
}
