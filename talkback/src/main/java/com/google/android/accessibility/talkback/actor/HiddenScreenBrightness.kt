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

package com.google.android.accessibility.talkback.actor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Sets the system screen brightness to the minimum while the screen is hidden, and restores the
 * brightness and brightness mode that were set before. The values to restore are saved in a
 * preferences file, so they are restored on the next start if SandFly stopped while the screen
 * was hidden.
 */
object HiddenScreenBrightness {
  private const val TAG = "HiddenScreenBrightness"
  private const val PREFS_NAME = "hidden_screen_brightness"
  private const val KEY_BRIGHTNESS = "saved_brightness"
  private const val KEY_MODE = "saved_mode"
  private const val KEY_PERMISSION_REQUESTED = "permission_requested"

  /** The lowest value of the system brightness slider. */
  private const val MIN_BRIGHTNESS = 1

  @JvmStatic fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

  /**
   * Saves the brightness and brightness mode, unless they are already saved from an earlier hide,
   * and sets manual brightness at the minimum. Does nothing without permission to modify system
   * settings.
   */
  @JvmStatic
  fun lower(context: Context) {
    if (!canWrite(context)) {
      return
    }
    val resolver = context.contentResolver
    val prefs = prefs(context)
    try {
      if (!prefs.contains(KEY_BRIGHTNESS)) {
        val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        val mode =
          Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
          )
        // Saved synchronously, so the values survive SandFly being stopped right after.
        prefs.edit().putInt(KEY_BRIGHTNESS, brightness).putInt(KEY_MODE, mode).commit()
      }
      Settings.System.putInt(
        resolver,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
      )
      Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, MIN_BRIGHTNESS)
    } catch (e: Settings.SettingNotFoundException) {
      LogUtils.w(TAG, "No screen brightness setting: %s", e)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot lower the screen brightness: %s", e)
    }
  }

  /** Restores the saved brightness and brightness mode, if any, and removes them. */
  @JvmStatic
  fun restore(context: Context) {
    val prefs = prefs(context)
    if (!prefs.contains(KEY_BRIGHTNESS)) {
      return
    }
    val brightness = prefs.getInt(KEY_BRIGHTNESS, MIN_BRIGHTNESS)
    val mode = prefs.getInt(KEY_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
    prefs.edit().remove(KEY_BRIGHTNESS).remove(KEY_MODE).commit()
    if (!canWrite(context)) {
      LogUtils.w(TAG, "Cannot restore the screen brightness without permission")
      return
    }
    val resolver = context.contentResolver
    try {
      // The brightness first, so it is not applied to adaptive brightness when that comes back.
      Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
      Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot restore the screen brightness: %s", e)
    }
  }

  /**
   * The first time the screen is hidden without permission to modify system settings, gets it:
   * through Shizuku when it is running, and otherwise from the system's Modify system settings
   * screen for SandFly. [onGranted] runs if Shizuku grants it. Returns true if the system screen
   * was opened.
   */
  @JvmStatic
  fun requestPermissionOnce(context: Context, onGranted: Runnable): Boolean {
    if (canWrite(context)) {
      return false
    }
    val prefs = prefs(context)
    if (prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)) {
      return false
    }
    prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    if (ShizukuGrants.isReady()) {
      ShizukuGrants.request(context, ShizukuGrants.Grant.WRITE_SETTINGS) { granted ->
        if (granted) {
          onGranted.run()
        }
      }
      return false
    }
    return openPermissionScreen(context)
  }

  private fun openPermissionScreen(context: Context): Boolean {
    val intent =
      Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      // Such as on watches and TVs that have no Modify system settings screen.
      LogUtils.w(TAG, "No screen to allow modifying system settings: %s", e)
      false
    }
  }

  private fun prefs(context: Context) =
    SharedPreferencesUtils.getSharedPreferences(context, PREFS_NAME)
}
