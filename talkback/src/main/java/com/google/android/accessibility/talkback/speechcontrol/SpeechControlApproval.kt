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

package com.google.android.accessibility.talkback.speechcontrol

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Asks the user, with a notification, whether an app may speak through Backtalk, the first time
 * it tries. Nothing the app sends is said until the user allows it, so that an app the user never
 * chose can't speak in Backtalk's voice.
 */
object SpeechControlApproval {
  private const val TAG = "SpeechControlApproval"
  private const val ACTION_ALLOW = "fyi.quin.backtalk.action.ALLOW_APP_SPEECH"
  private const val ACTION_DENY = "fyi.quin.backtalk.action.DENY_APP_SPEECH"
  private const val EXTRA_APP = "app"
  private const val NOTIFICATION_ID_BASE = 0x5ec0

  // Apps already asked about since Backtalk started, so that each is asked once at a time.
  private val asked = HashSet<String>()

  /** Asks whether [app] may speak, unless it was asked already. Called on the main thread. */
  fun ask(context: Context, app: String) {
    if (!asked.add(app)) return
    if (!NotificationUtils.hasPostNotificationPermission(context)) {
      LogUtils.w(TAG, "Can't ask about %s, since Backtalk can't post notifications", app)
      return
    }
    val label = label(context, app)
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setContentTitle(context.getString(R.string.speech_control_ask_title, label))
        .setContentText(context.getString(R.string.speech_control_ask_text))
        .setTicker(context.getString(R.string.speech_control_ask_title, label))
        .addAction(0, context.getString(R.string.speech_control_allow), answer(context, app, true))
        .addAction(0, context.getString(R.string.speech_control_deny), answer(context, app, false))
        .setAutoCancel(true)
        .setWhen(0)
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(notificationId(app), notification)
  }

  /** The name the user knows the app by, or its package name if it has none. */
  fun label(context: Context, app: String): CharSequence =
    try {
      val pm = context.packageManager
      pm.getApplicationLabel(pm.getApplicationInfo(app, 0))
    } catch (e: PackageManager.NameNotFoundException) {
      app
    }

  private fun answer(context: Context, app: String, allow: Boolean): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      notificationId(app) * 2 + if (allow) 1 else 0,
      Intent(context, AnswerReceiver::class.java)
        .setAction(if (allow) ACTION_ALLOW else ACTION_DENY)
        .putExtra(EXTRA_APP, app),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  private fun notificationId(app: String): Int = NOTIFICATION_ID_BASE + (app.hashCode() and 0xfff)

  /** Saves the user's answer from the notification. Not exported, so only it can call this. */
  class AnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val app = intent.getStringExtra(EXTRA_APP) ?: return
      val allow =
        when (intent.action) {
          ACTION_ALLOW -> true
          ACTION_DENY -> false
          else -> return
        }
      SpeechControlApps.setAllowed(SharedPreferencesUtils.getSharedPreferences(context), app, allow)
      asked.remove(app)
      context.getSystemService(NotificationManager::class.java)?.cancel(notificationId(app))
    }
  }
}
