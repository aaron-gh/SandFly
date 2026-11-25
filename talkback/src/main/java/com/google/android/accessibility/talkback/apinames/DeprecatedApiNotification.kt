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

package com.google.android.accessibility.talkback.apinames

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.DeprecatedApiNames

/**
 * Posts a notification when a debuggable app, which is almost always a developer's own build, uses
 * a deprecated API name, so that its developer sees it without reading SandFly's log. Release builds
 * of apps get only the log warning. One notification per app, listing each name it used while
 * SandFly runs.
 */
object DeprecatedApiNotification {
  private const val NOTIFICATION_ID_BASE = 0x0DE9_0000

  private val mainHandler = Handler(Looper.getMainLooper())

  // The names each app used, as shown in its notification. Main thread only.
  private val namesByApp = HashMap<String, LinkedHashSet<Pair<String, String>>>()

  @JvmStatic
  fun install(context: Context) {
    val appContext = context.applicationContext
    DeprecatedApiNames.listener =
      DeprecatedApiNames.Listener { app, legacyName, name ->
        if (isDebuggable(appContext, app)) {
          mainHandler.post { post(appContext, app, legacyName, name) }
        }
      }
  }

  private fun isDebuggable(context: Context, app: String): Boolean =
    try {
      context.packageManager.getApplicationInfo(app, 0).flags and
        ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (_: PackageManager.NameNotFoundException) {
      false
    }

  private fun post(context: Context, app: String, legacyName: String, name: String) {
    if (!NotificationUtils.canShowNotifications(context)) {
      return
    }
    val names = namesByApp.getOrPut(app) { LinkedHashSet() }
    names.add(legacyName to name)
    val label = label(context, app)
    val title = context.getString(R.string.deprecated_api_title, label)
    val lines =
      names.map { (legacy, current) ->
        context.getString(R.string.deprecated_api_replace, legacy, current)
      }
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setContentTitle(title)
        .setContentText(lines.first())
        .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
        .setTicker(title)
        .setOnlyAlertOnce(true)
        .setAutoCancel(true)
        .build()
    context
      .getSystemService(NotificationManager::class.java)
      ?.notify(NOTIFICATION_ID_BASE + (app.hashCode() and 0xFFFF), notification)
  }

  private fun label(context: Context, app: String): CharSequence =
    try {
      val pm = context.packageManager
      pm.getApplicationLabel(pm.getApplicationInfo(app, 0))
    } catch (_: PackageManager.NameNotFoundException) {
      app
    }
}
