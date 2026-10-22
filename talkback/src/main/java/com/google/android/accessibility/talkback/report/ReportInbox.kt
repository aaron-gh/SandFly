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

package com.google.android.accessibility.talkback.report

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.preference.BasePreferencesActivity
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executors

/**
 * Checks sent reports for replies and status changes, every hour while the service runs and when
 * the reports are opened, and posts a notification, with sound, for each report that has news.
 */
object ReportInbox {
  private const val TAG = "ReportInbox"
  private const val CHANNEL = "problem_reports"
  private const val CHECK_INTERVAL_MS = 60L * 60 * 1000
  private const val FIRST_CHECK_DELAY_MS = 2L * 60 * 1000

  private val executor = Executors.newSingleThreadExecutor { Thread(it, "ReportInbox") }
  private val mainHandler = Handler(Looper.getMainLooper())
  private var appContext: Context? = null
  private val periodicCheck =
    object : Runnable {
      override fun run() {
        appContext?.let { checkNow(it) }
        mainHandler.postDelayed(this, CHECK_INTERVAL_MS)
      }
    }

  /** Starts the hourly checks. Called as the service starts. */
  @JvmStatic
  @MainThread
  fun startFollowing(context: Context) {
    appContext = context.applicationContext
    mainHandler.removeCallbacks(periodicCheck)
    mainHandler.postDelayed(periodicCheck, FIRST_CHECK_DELAY_MS)
  }

  /** Stops the hourly checks. Called as the service stops. */
  @JvmStatic
  @MainThread
  fun stopFollowing() {
    mainHandler.removeCallbacks(periodicCheck)
    appContext = null
  }

  /** Checks the followed reports now, and calls [done] on the main thread with whether it could. */
  @JvmStatic
  fun checkNow(context: Context, done: ((Boolean) -> Unit)? = null) {
    val app = context.applicationContext
    executor.execute {
      val checked =
        try {
          check(app)
        } catch (e: RuntimeException) {
          LogUtils.e(TAG, "Reports not checked: %s", e)
          false
        }
      done?.let { mainHandler.post { it(checked) } }
    }
  }

  @WorkerThread
  private fun check(context: Context): Boolean {
    val store = ReportStore.get(context)
    val now = System.currentTimeMillis()
    val followed = store.sentReports().filter { it.isFollowed(now) }
    if (followed.isEmpty()) return true
    val threads = ProblemReportSender.threads(context, followed.map { it.token }) ?: return false
    for (thread in threads) {
      val report = followed.firstOrNull { it.token == thread.token } ?: continue
      var news: News? = null
      store.updateSent(report.id) { current ->
        val merged = merge(current, thread, now)
        news = newsOf(merged)
        merged.withNotified()
      }
      news?.let { notify(context, report.id, it) }
    }
    return true
  }

  /** [report] with the status and messages of [thread], which the report server sent at [now]. */
  @VisibleForTesting
  fun merge(report: SentReport, thread: ProblemReportSender.ReportThread, now: Long): SentReport =
    report.copy(
      status = thread.status,
      statusChangedAt = if (thread.status != report.status) now else report.statusChangedAt,
      messages = thread.messages,
    )

  /** What a notification says about a report: its newest reply, or its new status. */
  @VisibleForTesting
  data class News(val title: String, val reply: String?, val status: String?)

  /** What [report] has that no notification has been posted for, or null. */
  @VisibleForTesting
  fun newsOf(report: SentReport): News? {
    val newReply =
      report.messages.filter { it.fromDeveloper && it.id > report.notifiedMessageId }.lastOrNull()
    val newStatus =
      report.status.takeIf {
        it != report.notifiedStatus && it != SentReport.STATUS_UNKNOWN
      }
    if (newReply == null && newStatus == null) return null
    return News(report.title, newReply?.body, newStatus)
  }

  /** [this] once a notification is posted for what it has now. */
  private fun SentReport.withNotified(): SentReport =
    copy(
      notifiedMessageId =
        maxOf(notifiedMessageId, messages.filter { it.fromDeveloper }.maxOfOrNull { it.id } ?: 0),
      notifiedStatus = status,
    )

  private fun notify(context: Context, reportId: String, news: News) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL,
        context.getString(R.string.notification_channel_problem_reports),
        NotificationManager.IMPORTANCE_DEFAULT,
      )
    )
    // A new status is in the title, and a reply, if there is one, is the text.
    val title =
      if (news.status != null)
        context.getString(
          R.string.report_notification_status,
          news.title,
          context.getString(statusName(news.status)),
        )
      else context.getString(R.string.report_notification_reply, news.title)
    val text = news.reply ?: context.getString(statusName(news.status))
    val notification =
      NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.quantum_gm_ic_accessibility_new_vd_theme_24)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(openIntent(context, reportId))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .build()
    try {
      manager.notify(reportId, NOTIFICATION_ID, notification)
    } catch (e: SecurityException) {
      LogUtils.w(TAG, "Cannot show the report notification: %s", e)
    }
  }

  /** Removes the notification for the report with [reportId], once the report is opened. */
  @JvmStatic
  fun cancelNotification(context: Context, reportId: String) {
    context.getSystemService(NotificationManager::class.java)?.cancel(reportId, NOTIFICATION_ID)
  }

  private const val NOTIFICATION_ID = 7301

  private fun openIntent(context: Context, reportId: String): PendingIntent {
    val intent =
      Intent(context, TalkBackPreferencesActivity.TalkBackSubSettings::class.java)
        .putExtra(BasePreferencesActivity.FRAGMENT_NAME, ReportThreadFragment::class.java.name)
        .putExtra(
          BasePreferencesActivity.FRAGMENT_ARGS,
          Bundle().apply { putString(ReportThreadFragment.ARG_REPORT_ID, reportId) },
        )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return PendingIntent.getActivity(
      context,
      reportId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  /** When [millis] was, such as "5 minutes ago", or "just now" within a minute. */
  @JvmStatic
  fun timeAgo(context: Context, millis: Long): CharSequence {
    val now = System.currentTimeMillis()
    return if (now - millis < DateUtils.MINUTE_IN_MILLIS) context.getString(R.string.report_just_now)
    else DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.MINUTE_IN_MILLIS)
  }

  /** The name of a report's status, as the reporter sees it. */
  @JvmStatic
  @StringRes
  fun statusName(status: String?): Int =
    when (status) {
      SentReport.STATUS_COMPLETED -> R.string.report_status_completed
      SentReport.STATUS_NOT_PLANNED -> R.string.report_status_not_planned
      SentReport.STATUS_DUPLICATE -> R.string.report_status_duplicate
      SentReport.STATUS_UNKNOWN -> R.string.report_status_unknown
      else -> R.string.report_status_open
    }
}
