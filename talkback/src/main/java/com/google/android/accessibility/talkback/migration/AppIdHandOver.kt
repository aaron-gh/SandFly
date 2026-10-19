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

package com.google.android.accessibility.talkback.migration

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Takes over from Backtalk Aaron when SandFly turns on.
 *
 * The old app is told to turn itself off. If the user granted WRITE_SECURE_SETTINGS with adb, the
 * old screen reader also leaves the enabled services, and the accessibility shortcuts and the
 * braille keyboard move to this app. Then a notification offers to uninstall the old app, once.
 */
object AppIdHandOver {
  private const val TAG = "AppIdHandOver"
  private const val NOTIFICATION_ID = 0x0BAC7A1D
  private const val KEY_UNINSTALL_OFFERED = "uninstall_offered"
  private const val BRAILLE_IME_CLASS = "com.google.android.accessibility.brailleime.BrailleIme"

  /** How long after the old service leaves the enabled services to set up gestures again. */
  private const val REREGISTER_DELAY_MS = 500L

  /** How long to wait for the old service to leave the enabled services. */
  private const val WATCH_TIMEOUT_MS = 30_000L

  // Secure settings that name accessibility services as shortcut targets. Some of them are only on
  // newer Android versions, and missing ones are skipped.
  private val SHORTCUT_TARGET_SETTINGS =
    listOf(
      "accessibility_shortcut_target_service",
      "accessibility_button_targets",
      "accessibility_qs_targets",
      "accessibility_gesture_targets",
    )

  /**
   * Call when the screen reader service connects. When the old service has left the enabled
   * services, [onOldServiceGone] runs on the main thread: Android sets up touch handling again after
   * that change, which drops this service's own gesture detection, so it must be set up again.
   */
  @JvmStatic
  fun onServiceConnected(context: Context, onOldServiceGone: Runnable) {
    if (context.packageName == AppIdMove.OLD_PACKAGE) {
      return
    }
    try {
      handOver(context, onOldServiceGone)
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Cannot take over from the old app: %s", e)
    }
  }

  private fun handOver(context: Context, onOldServiceGone: Runnable) {
    if (!SettingsImporter.isOldAppInstalled(context)) {
      return
    }
    val oldService = AppIdMove.serviceName(AppIdMove.OLD_PACKAGE)
    val newService = AppIdMove.serviceName(context.packageName)
    val resolver = context.contentResolver
    val canWrite =
      context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED

    val enabled = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    if (ComponentLists.contains(enabled, oldService)) {
      watchForServiceGone(context, oldService, onOldServiceGone)
      if (canWrite) {
        ComponentLists.remove(enabled, oldService)?.let {
          putSecure(context, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, it)
        }
      }
      LogUtils.i(TAG, "Telling the old app to turn itself off")
      context.sendBroadcast(
        Intent(AppIdMove.ACTION_HAND_OVER)
          .setComponent(ComponentName(AppIdMove.OLD_PACKAGE, HandOverReceiver::class.java.name))
      )
    }

    if (canWrite) {
      for (key in SHORTCUT_TARGET_SETTINGS) {
        swap(context, key, oldService, newService)
      }
      val oldIme = "${AppIdMove.OLD_PACKAGE}/$BRAILLE_IME_CLASS"
      val newIme = "${context.packageName}/$BRAILLE_IME_CLASS"
      swap(context, Settings.Secure.ENABLED_INPUT_METHODS, oldIme, newIme)
      swap(context, Settings.Secure.DEFAULT_INPUT_METHOD, oldIme, newIme)
    }

    offerUninstall(context)
  }

  /** Runs [onGone] shortly after [service] leaves the enabled accessibility services. */
  private fun watchForServiceGone(context: Context, service: String, onGone: Runnable) {
    val resolver = context.contentResolver
    val handler = Handler(Looper.getMainLooper())
    val observer =
      object : ContentObserver(handler) {
        var watching = true

        override fun onChange(selfChange: Boolean) {
          val enabled =
            Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
          if (watching && !ComponentLists.contains(enabled, service)) {
            stop()
            LogUtils.i(TAG, "The old app is off, setting up gestures again")
            handler.postDelayed(onGone, REREGISTER_DELAY_MS)
          }
        }

        fun stop() {
          watching = false
          resolver.unregisterContentObserver(this)
        }
      }
    resolver.registerContentObserver(
      Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
      false,
      observer,
    )
    handler.postDelayed({ if (observer.watching) observer.stop() }, WATCH_TIMEOUT_MS)
  }

  private fun swap(context: Context, key: String, oldComponent: String, newComponent: String) {
    val value =
      try {
        Settings.Secure.getString(context.contentResolver, key)
      } catch (e: SecurityException) {
        // Some of the shortcut settings are hidden from apps on Android 12 and later.
        LogUtils.w(TAG, "Cannot read %s: %s", key, e)
        return
      }
    ComponentLists.replace(value, oldComponent, newComponent)?.let { putSecure(context, key, it) }
  }

  private fun putSecure(context: Context, key: String, value: String) {
    try {
      Settings.Secure.putString(context.contentResolver, key, value)
      LogUtils.i(TAG, "Moved %s to the new app", key)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot change %s: %s", key, e)
    }
  }

  /** Posts a notification that opens the uninstall prompt for the old app, once. */
  private fun offerUninstall(context: Context) {
    val state = SharedPreferencesUtils.getSharedPreferences(context, MigrationFiles.STATE_PREFS)
    if (state.getBoolean(KEY_UNINSTALL_OFFERED, false)) {
      return
    }
    val intent =
      Intent(Intent.ACTION_DELETE, Uri.fromParts("package", AppIdMove.OLD_PACKAGE, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val title = context.getString(R.string.migration_moved_title)
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setTicker(title)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.migration_moved_text))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    state.edit().putBoolean(KEY_UNINSTALL_OFFERED, true).apply()
  }
}
