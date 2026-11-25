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

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Asks the user, with a notification, whether an app may speak through SandFly, the first time
 * it tries. Nothing the app sends is said until the user allows it, so that an app the user never
 * chose can't speak in SandFly's voice.
 */
object SpeechControlApproval {
  private const val TAG = "SpeechControlApproval"
  private const val ACTION_ALLOW = "io.github.aaron_gh.sandfly.action.ALLOW_APP_SPEECH"
  private const val ACTION_DENY = "io.github.aaron_gh.sandfly.action.DENY_APP_SPEECH"
  private const val ACTION_DISMISSED = "io.github.aaron_gh.sandfly.action.DISMISS_APP_SPEECH"
  internal const val ACTION_ALLOW_LOCKED =
    "io.github.aaron_gh.sandfly.action.ALLOW_APP_SPEECH_LOCKED"
  private const val ACTION_DENY_LOCKED = "io.github.aaron_gh.sandfly.action.DENY_APP_SPEECH_LOCKED"
  private const val ACTION_DISMISSED_LOCKED =
    "io.github.aaron_gh.sandfly.action.DISMISS_APP_SPEECH_LOCKED"
  internal const val EXTRA_APP = "app"
  private const val NOTIFICATION_ID_BASE = 0x5ec0

  /** How long before an unanswered app is asked about again if its notification isn't showing. */
  private const val RETRY_MS = 60_000L

  // When each unanswered app was last asked about, so that each is asked once at a time. Asked
  // again if its notification isn't showing, as when Android didn't show it or removed it.
  private val asked = HashMap<String, Long>()
  private val askedLocked = HashMap<String, Long>()

  /** Asks whether [app] may speak, unless it was asked already. Called on the main thread. */
  fun ask(context: Context, app: String) {
    // Not counted as asked when it can't be: it is asked once notifications are allowed.
    if (!NotificationUtils.canShowNotifications(context)) {
      LogUtils.w(TAG, "Can't ask about %s, since SandFly notifications are off", app)
      return
    }
    if (!shouldAsk(context, asked, app, notificationId(app))) return
    val label = label(context, app)
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setContentTitle(context.getString(R.string.speech_control_ask_title, label))
        .setContentText(context.getString(R.string.speech_control_ask_text))
        .setTicker(context.getString(R.string.speech_control_ask_title, label))
        .addAction(0, context.getString(R.string.speech_control_allow), answer(context, app, true))
        .addAction(0, context.getString(R.string.speech_control_deny), answer(context, app, false))
        // Dismissed without an answer: asked again the next time the app speaks.
        .setDeleteIntent(dismissed(context, app))
        .setAutoCancel(true)
        .setWhen(0)
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(notificationId(app), notification)
  }

  /**
   * Asks whether [app], which the user already allowed and which declares
   * [SpeechControl.permissionWhenLocked], may also speak while the device is locked. Asked while
   * locked, since that is when the app tries, so Allow needs the device unlocked first: someone
   * holding a locked phone can't give an app that. Called on the main thread.
   */
  fun askWhileLocked(context: Context, app: String) {
    if (!NotificationUtils.canShowNotifications(context)) {
      LogUtils.w(TAG, "Can't ask about %s while locked, since SandFly notifications are off", app)
      return
    }
    if (!shouldAsk(context, askedLocked, app, lockedNotificationId(app))) return
    val label = label(context, app)
    val allow =
      NotificationCompat.Action.Builder(
          0,
          context.getString(R.string.speech_control_allow),
          answer(context, app, ACTION_ALLOW_LOCKED, 3),
        )
        .setAuthenticationRequired(true)
        .build()
    val notification =
      NotificationUtils.createDefaultNotificationBuilder(context)
        .setContentTitle(context.getString(R.string.speech_control_ask_locked_title, label))
        .setContentText(context.getString(R.string.speech_control_ask_locked_text))
        .setTicker(context.getString(R.string.speech_control_ask_locked_title, label))
        .addAction(allow)
        .addAction(
          0,
          context.getString(R.string.speech_control_deny),
          answer(context, app, ACTION_DENY_LOCKED, 2),
        )
        .setDeleteIntent(answer(context, app, ACTION_DISMISSED_LOCKED, 4))
        .setAutoCancel(true)
        .setWhen(0)
        .build()
    context
      .getSystemService(NotificationManager::class.java)
      ?.notify(lockedNotificationId(app), notification)
  }

  /**
   * Removes the questions about [app], once the user answered for it in Let other apps speak.
   * Called on the main thread.
   */
  fun cancel(context: Context, app: String) {
    asked.remove(app)
    askedLocked.remove(app)
    context.getSystemService(NotificationManager::class.java)?.let {
      it.cancel(notificationId(app))
      it.cancel(lockedNotificationId(app))
    }
  }

  /**
   * Whether to post the question with [id] about [app], recording when in [askedAt]: if it was
   * never asked, or asked more than [RETRY_MS] ago and the notification isn't showing.
   */
  private fun shouldAsk(
    context: Context,
    askedAt: HashMap<String, Long>,
    app: String,
    id: Int,
  ): Boolean {
    val now = SystemClock.elapsedRealtime()
    val last = askedAt[app]
    if (last != null && now - last < RETRY_MS) return false
    askedAt[app] = now
    return last == null || !isShowing(context, id)
  }

  private fun isShowing(context: Context, id: Int): Boolean =
    try {
      context
        .getSystemService(NotificationManager::class.java)
        ?.activeNotifications
        ?.any { it.id == id } ?: false
    } catch (e: RuntimeException) {
      // Unknown: treated as showing, so the question isn't posted again.
      true
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

  private fun dismissed(context: Context, app: String): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      notificationId(app) * 2 + 0x10000,
      Intent(context, AnswerReceiver::class.java)
        .setAction(ACTION_DISMISSED)
        .putExtra(EXTRA_APP, app),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  /** The answer from the while-locked question; [slot] keeps each button's intent apart. */
  private fun answer(context: Context, app: String, action: String, slot: Int): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      lockedNotificationId(app) * 8 + slot,
      Intent(context, AnswerReceiver::class.java).setAction(action).putExtra(EXTRA_APP, app),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  private fun notificationId(app: String): Int = NOTIFICATION_ID_BASE + (app.hashCode() and 0xfff)

  private fun lockedNotificationId(app: String): Int = notificationId(app) + 0x1000

  /** Saves the user's answer from the notification. Not exported, so only it can call this. */
  class AnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val app = intent.getStringExtra(EXTRA_APP) ?: return
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      when (intent.action) {
        ACTION_ALLOW_LOCKED,
        ACTION_DENY_LOCKED -> {
          // Before Android 12 the action's authentication requirement has no effect, so Allow
          // reaches here from the lock screen. Ignored while locked; the question stays posted.
          if (
            intent.action == ACTION_ALLOW_LOCKED &&
              Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
              context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true
          ) {
            LogUtils.w(TAG, "Not allowing %s while locked until the device is unlocked", app)
            return
          }
          SpeechControlApps.setAllowedWhileLocked(prefs, app, intent.action == ACTION_ALLOW_LOCKED)
          askedLocked.remove(app)
          context.getSystemService(NotificationManager::class.java)?.cancel(lockedNotificationId(app))
          return
        }
        ACTION_DISMISSED_LOCKED -> {
          askedLocked.remove(app)
          return
        }
      }
      val allow =
        when (intent.action) {
          ACTION_ALLOW -> true
          ACTION_DENY -> false
          ACTION_DISMISSED -> {
            asked.remove(app)
            return
          }
          else -> return
        }
      SpeechControlApps.setAllowed(prefs, app, allow)
      asked.remove(app)
      context.getSystemService(NotificationManager::class.java)?.cancel(notificationId(app))
    }
  }
}
