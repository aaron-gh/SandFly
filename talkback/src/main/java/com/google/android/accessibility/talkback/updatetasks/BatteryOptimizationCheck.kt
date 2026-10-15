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

package com.google.android.accessibility.talkback.updatetasks

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.FormFactorUtils
import java.util.Locale

/**
 * Tells the user, once, when battery optimization could stop Backtalk. Android leaves accessibility
 * services running, but some phone makers' own battery savers stop them anyway, so Backtalk falls
 * silent after the phone sleeps until it's restarted. Only on those phones, and only while Backtalk
 * isn't exempt, a notification opens the system list where the user can exempt it.
 */
object BatteryOptimizationCheck {
  private const val PREF_DONE = "battery_optimization_check_done"
  private const val NOTIFICATION_ID = 0x0BA77E21

  /** Phone makers whose battery savers are known to stop background services, by manufacturer. */
  private val AGGRESSIVE_MAKERS =
    setOf(
      "oneplus",
      "oppo",
      "realme",
      "xiaomi",
      "redmi",
      "poco",
      "huawei",
      "honor",
      "vivo",
      "iqoo",
      "meizu",
      "asus",
      "tecno",
      "infinix",
      "itel",
    )

  /** Whether phones by [manufacturer] are known to stop services that battery optimization covers. */
  @JvmStatic
  fun isAggressiveMaker(manufacturer: String?): Boolean =
    manufacturer?.lowercase(Locale.ROOT)?.trim() in AGGRESSIVE_MAKERS

  @JvmStatic
  fun check(context: Context, prefs: SharedPreferences) {
    if (
      prefs.getBoolean(PREF_DONE, false) ||
        FormFactorUtils.isAndroidWear() ||
        FormFactorUtils.isAndroidTv() ||
        !isAggressiveMaker(Build.MANUFACTURER)
    ) {
      return
    }
    val power = context.getSystemService(PowerManager::class.java)
    if (power == null || power.isIgnoringBatteryOptimizations(context.packageName)) {
      prefs.edit().putBoolean(PREF_DONE, true).apply()
      return
    }
    val intent =
      Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Until notifications are allowed, try again at the next start.
    if (
      intent.resolveActivity(context.packageManager) == null ||
        !NotificationManagerCompat.from(context).areNotificationsEnabled()
    ) {
      return
    }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val title = context.getString(R.string.battery_optimization_notification_title)
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setTicker(title)
        .setContentTitle(title)
        .setContentText(context.getString(R.string.battery_optimization_notification_text))
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(context.getString(R.string.battery_optimization_notification_text))
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    prefs.edit().putBoolean(PREF_DONE, true).apply()
  }
}
